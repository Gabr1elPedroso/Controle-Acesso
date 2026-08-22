package com.gap.banco;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.gap.model.Equipamento;

public class ConexaoSQLite {

    private static final String CAMINHO_BANCO = "gap.db";
    private static final String URL = "jdbc:sqlite:" + CAMINHO_BANCO;

    public static Connection conectar() {
        try {
            return DriverManager.getConnection(URL);
        } catch (Exception e) {
            System.out.println("Erro ao conectar no banco de dados: " + e.getMessage());
            return null;
        }
    }

    /**
     * Cria toda a estrutura de tabelas necessárias para o sistema
     */
    public static void criarTabelas() {
        try (Connection conn = conectar(); Statement stmt = conn.createStatement()) {

            // 1. Tabela de Categorias (ex: Diretoria, Funcionário, Terceirizado)
            String sqlCategorias = "CREATE TABLE IF NOT EXISTS categorias ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "nome TEXT NOT NULL UNIQUE"
                    + ");";
            stmt.execute(sqlCategorias);

            // Inserir categorias padrão caso a tabela esteja vazia
            stmt.execute("INSERT OR IGNORE INTO categorias (id, nome) VALUES (1, 'Funcionário'), (2, 'Diretoria'), (3, 'Visitante');");

            // 2. Tabela de Equipamentos (Leitores Faciais iDFace)
            String sqlEquipamentos = "CREATE TABLE IF NOT EXISTS equipamentos ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "nome TEXT NOT NULL, "
                    + "ip TEXT NOT NULL UNIQUE, "
                    + "usuario TEXT NOT NULL DEFAULT 'admin', "
                    + "senha TEXT NOT NULL DEFAULT 'admin', "
                    + "status TEXT DEFAULT 'Desconhecido'"
                    + ");";
            stmt.execute(sqlEquipamentos);

            // 3. Tabela de Usuários (Funcionários)
            String sqlUsuarios = "CREATE TABLE IF NOT EXISTS usuarios ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "nome TEXT NOT NULL, "
                    + "matricula TEXT NOT NULL UNIQUE, "
                    + "categoria_id INTEGER, "
                    + "foto_base64 TEXT, "
                    + "FOREIGN KEY (categoria_id) REFERENCES categorias(id)"
                    + ");";
            stmt.execute(sqlUsuarios);

            // Criação da tabela de acesso ao sistema
            String sqlAdmins = "CREATE TABLE IF NOT EXISTS usuarios_sistema ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "login TEXT NOT NULL UNIQUE, "
                    + "senha TEXT NOT NULL"
                    + ");";
            stmt.execute(sqlAdmins);

            // Insere o usuário padrão (admin / admin123) se a tabela estiver vazia
            String verificaAdmin = "SELECT COUNT(*) AS total FROM usuarios_sistema";
            java.sql.ResultSet rs = stmt.executeQuery(verificaAdmin);
            if (rs.next() && rs.getInt("total") == 0) {
                stmt.execute("INSERT INTO usuarios_sistema (login, senha) VALUES ('Gap', '2215')");
                System.out.println("Usuário administrador padrão criado com sucesso.");
            }

            // 4. Tabela de Mapeamento (Relaciona o Usuário ao ID específico de cada Equipamento)
            String sqlEquipamentoUsers = "CREATE TABLE IF NOT EXISTS equipamento_usuarios ("
                    + "usuario_id INTEGER, "
                    + "equipamento_id INTEGER, "
                    + "id_no_equipamento INTEGER, "
                    + "PRIMARY KEY (usuario_id, equipamento_id), "
                    + "FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (equipamento_id) REFERENCES equipamentos(id) ON DELETE CASCADE"
                    + ");";
            stmt.execute(sqlEquipamentoUsers);

            // 5. Tabela de Permissões de Acesso (Determina se o usuário pode acessar determinada porta)
            String sqlPermissoes = "CREATE TABLE IF NOT EXISTS permissoes ("
                    + "usuario_id INTEGER, "
                    + "equipamento_id INTEGER, "
                    + "PRIMARY KEY (usuario_id, equipamento_id), "
                    + "FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (equipamento_id) REFERENCES equipamentos(id) ON DELETE CASCADE"
                    + ");";
            stmt.execute(sqlPermissoes);

            System.out.println("Estrutura completa do banco de dados carregada com sucesso!");

        } catch (Exception e) {
            System.out.println("Erro ao criar estrutura do banco: " + e.getMessage());
        }
    }

    /**
     * Realiza a cópia de segurança (Backup) do banco de dados
     * @param pastaDestino Caminho da pasta onde o backup será salvo (ex: "C:/Backups_GAP")
     * @return boolean se o backup teve sucesso
     */
    public static boolean realizarBackup(String pastaDestino) {
        File arquivoOriginal = new File(CAMINHO_BANCO);
        if (!arquivoOriginal.exists()) {
            System.out.println("Arquivo de banco de dados não encontrado para backup.");
            return false;
        }

        try {
            // Garante que a pasta de destino existe
            File diretorioDestino = new File(pastaDestino);
            if (!diretorioDestino.exists()) {
                diretorioDestino.mkdirs();
            }

            // Gera um nome único baseado na data e hora: backup_gap_20260822_104500.db
            String dataHora = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String nomeArquivoBackup = "backup_gap_" + dataHora + ".db";
            File arquivoBackup = new File(diretorioDestino, nomeArquivoBackup);

            // Executa a cópia do arquivo
            Files.copy(arquivoOriginal.toPath(), arquivoBackup.toPath(), StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Backup realizado com sucesso em: " + arquivoBackup.getAbsolutePath());
            return true;

        } catch (IOException e) {
            System.out.println("Erro ao realizar backup do banco: " + e.getMessage());
            return false;
        }
    }

    public static boolean salvarUsuario(String nome, String matricula, int idCategoria, String fotoBase64){
        String sql = "INSERT INTO usuarios (nome, matricula, categoria_id, foto_base64) VALUES (?, ?, ?, ?)";

        try (Connection conn = conectar();
        PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, nome);
            pstmt.setString(2, matricula);
            pstmt.setInt(3, idCategoria);
            pstmt.setString(4, fotoBase64);

            pstmt.executeUpdate();
            return true;
        } catch (Exception e){
            System.out.println("Erro ao salvar usuário: " + e.getMessage());
            return false;
        }
    }

    /**
     * Busca todos os usuários cadastrados no banco
     */
    public static java.util.List<com.gap.model.Pessoa> listarUsuarios() {
        java.util.List<com.gap.model.Pessoa> lista = new java.util.ArrayList<>();
        String sql = "SELECT id, nome, matricula, categoria_id, foto_base64 FROM usuarios ORDER BY nome ASC";

        try (java.sql.Connection conn = conectar();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                com.gap.model.Pessoa p = new com.gap.model.Pessoa(
                    rs.getInt("id"),
                    rs.getString("nome"),
                    rs.getString("matricula"),
                    rs.getInt("categoria_id"),
                    rs.getString("foto_base64")
                );
                lista.add(p);
            }
        } catch (Exception e) {
            System.out.println("Erro ao listar usuários: " + e.getMessage());
        }

        return lista;
    }

    /**
     * Remove um usuário do banco local pelo ID
     */
    public static boolean excluirUsuario(int idUsuario) {
        String sql = "DELETE FROM usuarios WHERE id = ?";

        try (Connection conn = conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idUsuario);
            int linhasAfetadas = pstmt.executeUpdate();

            return linhasAfetadas > 0;

        } catch (Exception e) {
            System.out.println("Erro ao excluir usuário no banco: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica se o login e senha do administrador estão corretos
     */
    public static boolean autenticarSistema(String login, String senha) {
        String sql = "SELECT id FROM usuarios_sistema WHERE login = ? AND senha = ?";
        try (Connection conn = conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, login);
            pstmt.setString(2, senha);
            
            // Se retornar algum resultado, a senha está correta
            return pstmt.executeQuery().next();
            
        } catch (Exception e) {
            System.out.println("Erro ao autenticar: " + e.getMessage());
            return false;
        }
    }

    /**
     * Atualiza um usuário existente no banco local pelo ID
     */
    public static boolean atualizarUsuario(int id, String nome, String matricula, int idCategoria, String fotoBase64) {
        String sql = "UPDATE usuarios SET nome = ?, matricula = ?, categoria_id = ?, foto_base64 = ? WHERE id = ?";
        
        try (Connection conn = conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, nome);
            pstmt.setString(2, matricula);
            pstmt.setInt(3, idCategoria);
            pstmt.setString(4, fotoBase64);
            pstmt.setInt(5, id);
            
            return pstmt.executeUpdate() > 0;
            
        } catch (Exception e) {
            System.out.println("Erro ao atualizar usuário: " + e.getMessage());
            return false;
        }
    }

    // ==========================================
    // CRUD DE EQUIPAMENTOS (iDFace)
    // ==========================================

    public static boolean salvarEquipamento(String nome, String ip, String usuario, String senha) {
        String sql = "INSERT INTO equipamentos (nome, ip, usuario, senha, status) VALUES (?, ?, ?, ?, 'Desconhecido')";
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nome);
            pstmt.setString(2, ip);
            pstmt.setString(3, usuario);
            pstmt.setString(4, senha);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erro ao salvar equipamento: " + e.getMessage());
            return false;
        }
    }

    public static List<Equipamento> listarEquipamentos() {
        List<Equipamento> lista = new ArrayList<>();
        String sql = "SELECT * FROM equipamentos";
        try (Connection conn = conectar(); 
             Statement stmt = conn.createStatement(); 
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                lista.add(new Equipamento(
                        rs.getInt("id"),
                        rs.getString("nome"),
                        rs.getString("ip"),
                        rs.getString("usuario"),
                        rs.getString("senha"),
                        rs.getString("status")
                ));
            }
        } catch (Exception e) {
            System.out.println("Erro ao listar equipamentos: " + e.getMessage());
        }
        return lista;
    }

    public static boolean atualizarEquipamento(int id, String nome, String ip, String usuario, String senha) {
        String sql = "UPDATE equipamentos SET nome = ?, ip = ?, usuario = ?, senha = ? WHERE id = ?";
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nome);
            pstmt.setString(2, ip);
            pstmt.setString(3, usuario);
            pstmt.setString(4, senha);
            pstmt.setInt(5, id);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erro ao atualizar equipamento: " + e.getMessage());
            return false;
        }
    }

    public static boolean excluirEquipamento(int id) {
        String sql = "DELETE FROM equipamentos WHERE id = ?";
        try (Connection conn = conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Erro ao excluir equipamento: " + e.getMessage());
            return false;
        }
    }

}