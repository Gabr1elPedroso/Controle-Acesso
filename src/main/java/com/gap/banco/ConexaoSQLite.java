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

    // ==========================================================
    // CONEXÃO
    // ==========================================================
public static Connection conectar() {
        try {
            java.util.Properties config = com.gap.config.GerenciadorConfiguracao.carregarConfiguracoes();
            String caminhoBanco = config.getProperty("banco.caminho", "gap.db");
            
            String url = "jdbc:sqlite:" + caminhoBanco;
            return DriverManager.getConnection(url);

        } catch (Exception e) {
            System.out.println("Erro ao conectar no banco de dados: " + e.getMessage());
            return null;
        }
    }

    // ==========================================================
    // CRIAÇÃO DAS TABELAS
    // ==========================================================

    public static void criarTabelas() {

        try (
            Connection conn = conectar();
            Statement stmt = conn.createStatement()
        ) {

            // --------------------------------------------------
            // 1. Tabela de categorias
            // --------------------------------------------------

            String sqlCategorias =
                    "CREATE TABLE IF NOT EXISTS categorias ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "nome TEXT NOT NULL UNIQUE"
                    + ");";

            stmt.execute(sqlCategorias);

            // Categorias padrão
            stmt.execute(
                "INSERT OR IGNORE INTO categorias "
                + "(id, nome) VALUES "
                + "(1, 'Funcionário'), "
                + "(2, 'Diretoria'), "
                + "(3, 'Visitante');"
            );

            // --------------------------------------------------
            // 2. Tabela de equipamentos
            // --------------------------------------------------

            String sqlEquipamentos =
                    "CREATE TABLE IF NOT EXISTS equipamentos ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "nome TEXT NOT NULL, "
                    + "ip TEXT NOT NULL UNIQUE, "
                    + "usuario TEXT NOT NULL DEFAULT 'admin', "
                    + "senha TEXT NOT NULL DEFAULT 'admin', "
                    + "status TEXT DEFAULT 'Desconhecido', "
                    + "porta INTEGER NOT NULL DEFAULT 80"
                    + ");";

            stmt.execute(sqlEquipamentos);

            // --------------------------------------------------
            // 3. Tabela de usuários
            // --------------------------------------------------

            String sqlUsuarios =
                    "CREATE TABLE IF NOT EXISTS usuarios ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "nome TEXT NOT NULL, "
                    + "matricula TEXT NOT NULL UNIQUE, "
                    + "categoria_id INTEGER, "
                    + "foto_base64 TEXT, "
                    + "FOREIGN KEY (categoria_id) "
                    + "REFERENCES categorias(id)"
                    + ");";

            stmt.execute(sqlUsuarios);

            // --------------------------------------------------
            // 4. Tabela de usuários do sistema
            // --------------------------------------------------

            String sqlAdmins =
                    "CREATE TABLE IF NOT EXISTS usuarios_sistema ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "login TEXT NOT NULL UNIQUE, "
                    + "senha TEXT NOT NULL"
                    + ");";

            stmt.execute(sqlAdmins);

            // --------------------------------------------------
            // Usuário administrador padrão
            // --------------------------------------------------

            String verificaAdmin =
                    "SELECT COUNT(*) AS total FROM usuarios_sistema";

            ResultSet rs = stmt.executeQuery(verificaAdmin);

            if (rs.next() && rs.getInt("total") == 0) {

                stmt.execute(
                    "INSERT INTO usuarios_sistema "
                    + "(login, senha) "
                    + "VALUES ('Gap', '2215')"
                );

                System.out.println(
                    "Usuário administrador padrão criado com sucesso."
                );
            }

            // --------------------------------------------------
            // 5. Tabela equipamento_usuarios
            // --------------------------------------------------

            String sqlEquipamentoUsers =
                    "CREATE TABLE IF NOT EXISTS equipamento_usuarios ("
                    + "usuario_id INTEGER, "
                    + "equipamento_id INTEGER, "
                    + "id_no_equipamento INTEGER, "
                    + "PRIMARY KEY (usuario_id, equipamento_id), "
                    + "FOREIGN KEY (usuario_id) "
                    + "REFERENCES usuarios(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (equipamento_id) "
                    + "REFERENCES equipamentos(id) ON DELETE CASCADE"
                    + ");";

            stmt.execute(sqlEquipamentoUsers);

            // --------------------------------------------------
            // 6. Tabela de permissões
            // --------------------------------------------------

            String sqlPermissoes =
                    "CREATE TABLE IF NOT EXISTS permissoes ("
                    + "usuario_id INTEGER, "
                    + "equipamento_id INTEGER, "
                    + "PRIMARY KEY (usuario_id, equipamento_id), "
                    + "FOREIGN KEY (usuario_id) "
                    + "REFERENCES usuarios(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (equipamento_id) "
                    + "REFERENCES equipamentos(id) ON DELETE CASCADE"
                    + ");";

            stmt.execute(sqlPermissoes);

            System.out.println(
                "Estrutura completa do banco de dados carregada com sucesso!"
            );

        } catch (Exception e) {

            System.out.println(
                "Erro ao criar estrutura do banco: " + e.getMessage()
            );

            e.printStackTrace();
        }
    }

    // ==========================================================
    // BACKUP
    // ==========================================================

    public static boolean realizarBackup(String pastaDestino) {

        File arquivoOriginal = new File(CAMINHO_BANCO);

        if (!arquivoOriginal.exists()) {

            System.out.println(
                "Arquivo de banco de dados não encontrado para backup."
            );

            return false;
        }

        try {

            // Garante que a pasta exista
            File diretorioDestino = new File(pastaDestino);

            if (!diretorioDestino.exists()) {
                diretorioDestino.mkdirs();
            }

            // Nome do backup
            String dataHora =
                    new SimpleDateFormat("yyyyMMdd_HHmmss")
                    .format(new Date());

            String nomeArquivoBackup =
                    "backup_gap_" + dataHora + ".db";

            File arquivoBackup =
                    new File(diretorioDestino, nomeArquivoBackup);

            // Copia o banco
            Files.copy(
                arquivoOriginal.toPath(),
                arquivoBackup.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            );

            System.out.println(
                "Backup realizado com sucesso em: "
                + arquivoBackup.getAbsolutePath()
            );

            return true;

        } catch (IOException e) {

            System.out.println(
                "Erro ao realizar backup do banco: " + e.getMessage()
            );

            return false;
        }
    }

    // ==========================================================
    // USUÁRIOS
    // ==========================================================

    public static boolean salvarUsuario(
            String nome,
            String matricula,
            int idCategoria,
            String fotoBase64) {

        String sql =
                "INSERT INTO usuarios "
                + "(nome, matricula, categoria_id, foto_base64) "
                + "VALUES (?, ?, ?, ?)";

        try (
            Connection conn = conectar();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, nome);
            pstmt.setString(2, matricula);
            pstmt.setInt(3, idCategoria);
            pstmt.setString(4, fotoBase64);

            pstmt.executeUpdate();

            return true;

        } catch (Exception e) {

            System.out.println(
                "Erro ao salvar usuário: " + e.getMessage()
            );

            return false;
        }
    }

    public static List<com.gap.model.Pessoa> listarUsuarios() {

        List<com.gap.model.Pessoa> lista =
                new ArrayList<>();

        String sql =
                "SELECT id, nome, matricula, categoria_id, foto_base64 "
                + "FROM usuarios ORDER BY nome ASC";

        try (
            Connection conn = conectar();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)
        ) {

            while (rs.next()) {

                com.gap.model.Pessoa p =
                        new com.gap.model.Pessoa(
                            rs.getInt("id"),
                            rs.getString("nome"),
                            rs.getString("matricula"),
                            rs.getInt("categoria_id"),
                            rs.getString("foto_base64")
                        );

                lista.add(p);
            }

        } catch (Exception e) {

            System.out.println(
                "Erro ao listar usuários: " + e.getMessage()
            );

            e.printStackTrace();
        }

        return lista;
    }

    public static boolean excluirUsuario(int idUsuario) {

        String sql =
                "DELETE FROM usuarios WHERE id = ?";

        try (
            Connection conn = conectar();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setInt(1, idUsuario);

            int linhasAfetadas =
                    pstmt.executeUpdate();

            return linhasAfetadas > 0;

        } catch (Exception e) {

            System.out.println(
                "Erro ao excluir usuário no banco: "
                + e.getMessage()
            );

            return false;
        }
    }

    public static boolean autenticarSistema(
            String login,
            String senha) {

        String sql =
                "SELECT id FROM usuarios_sistema "
                + "WHERE login = ? AND senha = ?";

        try (
            Connection conn = conectar();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, login);
            pstmt.setString(2, senha);

            return pstmt.executeQuery().next();

        } catch (Exception e) {

            System.out.println(
                "Erro ao autenticar: " + e.getMessage()
            );

            return false;
        }
    }

    public static boolean atualizarUsuario(
            int id,
            String nome,
            String matricula,
            int idCategoria,
            String fotoBase64) {

        String sql =
                "UPDATE usuarios SET "
                + "nome = ?, "
                + "matricula = ?, "
                + "categoria_id = ?, "
                + "foto_base64 = ? "
                + "WHERE id = ?";

        try (
            Connection conn = conectar();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, nome);
            pstmt.setString(2, matricula);
            pstmt.setInt(3, idCategoria);
            pstmt.setString(4, fotoBase64);
            pstmt.setInt(5, id);

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {

            System.out.println(
                "Erro ao atualizar usuário: "
                + e.getMessage()
            );

            return false;
        }
    }

    // ==========================================================
    // EQUIPAMENTOS
    // ==========================================================

    public static boolean salvarEquipamento(
            String nome,
            String ip,
            int porta,
            String usuario,
            String senha) {

        String sql =
                "INSERT INTO equipamentos "
                + "(nome, ip, porta, usuario, senha) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (
            Connection conn = conectar();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, nome);
            pstmt.setString(2, ip);
            pstmt.setInt(3, porta);
            pstmt.setString(4, usuario);
            pstmt.setString(5, senha);

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {

            System.out.println(
                "Erro ao salvar equipamento: "
                + e.getMessage()
            );

            return false;
        }
    }

  public static List<Equipamento> listarEquipamentos() {

    List<Equipamento> lista = new ArrayList<>();

    String sql = "SELECT * FROM equipamentos";

    try (
        Connection conn = conectar();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)
    ) {

        while (rs.next()) {

            lista.add(
                new Equipamento(
                    rs.getInt("id"),
                    rs.getString("nome"),
                    rs.getString("ip"),
                    rs.getInt("porta"),
                    rs.getString("usuario"),
                    rs.getString("senha"),
                    rs.getString("status")
                )
            );
        }

    } catch (Exception e) {

        System.out.println(
            "Erro ao listar equipamentos: " + e.getMessage()
        );

        e.printStackTrace();
    }

    return lista;
}

    public static boolean atualizarEquipamento(
            int id,
            String nome,
            String ip,
            int porta,
            String usuario,
            String senha) {

        String sql =
                "UPDATE equipamentos SET "
                + "nome = ?, "
                + "ip = ?, "
                + "porta = ?, "
                + "usuario = ?, "
                + "senha = ? "
                + "WHERE id = ?";

        try (
            Connection conn = conectar();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, nome);
            pstmt.setString(2, ip);
            pstmt.setInt(3, porta);
            pstmt.setString(4, usuario);
            pstmt.setString(5, senha);
            pstmt.setInt(6, id);

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {

            System.out.println(
                "Erro ao atualizar equipamento: "
                + e.getMessage()
            );

            return false;
        }
    }

    public static boolean excluirEquipamento(int id) {

        String sql =
                "DELETE FROM equipamentos WHERE id = ?";

        try (
            Connection conn = conectar();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setInt(1, id);

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {

            System.out.println(
                "Erro ao excluir equipamento: "
                + e.getMessage()
            );

            return false;
        }
    }
}