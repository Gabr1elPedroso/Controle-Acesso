package com.gap.banco;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

import com.gap.model.Equipamento;
import com.gap.model.Operador;

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

            // Bancos criados antes de existirem as colunas nome/nivel
            // precisam de ALTER TABLE — CREATE TABLE IF NOT EXISTS não
            // altera uma tabela que já existe.
            migrarColunasUsuarioSistema(conn);
            migrarColunasEquipamentos(conn);

            // --------------------------------------------------
            // Usuário administrador padrão
            // --------------------------------------------------

            String verificaAdmin =
                    "SELECT COUNT(*) AS total FROM usuarios_sistema";

            ResultSet rs = stmt.executeQuery(verificaAdmin);

            if (rs.next() && rs.getInt("total") == 0) {

                stmt.execute(
                    "INSERT INTO usuarios_sistema "
                    + "(login, senha, nome, nivel) "
                    + "VALUES ('Gap', '2215', 'Administrador Padrão', 'Administrador')"
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

    /**
     * Adiciona as colunas nome/nivel em bases já existentes (criadas antes
     * dessas colunas existirem). CREATE TABLE IF NOT EXISTS não altera uma
     * tabela que já existe, então isso precisa ser feito à parte.
     */
    private static void migrarColunasUsuarioSistema(Connection conn) throws Exception {

        java.util.Set<String> colunas = new java.util.HashSet<>();

        try (
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(usuarios_sistema)")
        ) {
            while (rs.next()) {
                colunas.add(rs.getString("name"));
            }
        }

        try (Statement stmt = conn.createStatement()) {
            if (!colunas.contains("nome")) {
                stmt.execute("ALTER TABLE usuarios_sistema ADD COLUMN nome TEXT");
            }
            if (!colunas.contains("nivel")) {
                stmt.execute(
                    "ALTER TABLE usuarios_sistema "
                    + "ADD COLUMN nivel TEXT NOT NULL DEFAULT 'Administrador'"
                );
            }
        }
    }

    /**
     * Adiciona as colunas porta/status/usuario/senha em bases criadas
     * antes dessas colunas existirem. Mesmo padrão da migração de
     * usuarios_sistema.
     */
    private static void migrarColunasEquipamentos(Connection conn) throws Exception {

        java.util.Set<String> colunas = new java.util.HashSet<>();

        try (
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(equipamentos)")
        ) {
            while (rs.next()) {
                colunas.add(rs.getString("name"));
            }
        }

        try (Statement stmt = conn.createStatement()) {
            if (!colunas.contains("porta")) {
                stmt.execute(
                    "ALTER TABLE equipamentos "
                    + "ADD COLUMN porta INTEGER NOT NULL DEFAULT 80"
                );
            }
            if (!colunas.contains("status")) {
                stmt.execute(
                    "ALTER TABLE equipamentos "
                    + "ADD COLUMN status TEXT DEFAULT 'Desconhecido'"
                );
            }
            if (!colunas.contains("usuario")) {
                stmt.execute(
                    "ALTER TABLE equipamentos "
                    + "ADD COLUMN usuario TEXT NOT NULL DEFAULT 'admin'"
                );
            }
            if (!colunas.contains("senha")) {
                stmt.execute(
                    "ALTER TABLE equipamentos "
                    + "ADD COLUMN senha TEXT NOT NULL DEFAULT 'admin'"
                );
            }
        }
    }

    // ==========================================================
    // BACKUP
    // ==========================================================

    public static boolean realizarBackup(String pastaDestino) {

        String caminhoBanco = com.gap.config.GerenciadorConfiguracao
                .carregarConfiguracoes()
                .getProperty("banco.caminho", "gap.db");

        File arquivoOriginal = new File(caminhoBanco);

        if (!arquivoOriginal.exists()) {

            System.out.println(
                "Arquivo de banco de dados não encontrado para backup: "
                + caminhoBanco
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

            // VACUUM INTO faz uma cópia consistente pelo próprio SQLite,
            // diferente de copiar o arquivo bruto (Files.copy) enquanto
            // o banco pode estar em uso.
            try (
                Connection conn = conectar();
                Statement stmt = conn.createStatement()
            ) {

                if (conn == null) {
                    System.out.println(
                        "Não foi possível conectar ao banco para o backup."
                    );
                    return false;
                }

                String caminhoEscapado =
                        arquivoBackup.getAbsolutePath().replace("'", "''");

                stmt.execute("VACUUM INTO '" + caminhoEscapado + "'");
            }

            System.out.println(
                "Backup realizado com sucesso em: "
                + arquivoBackup.getAbsolutePath()
            );

            limparBackupsAntigos(diretorioDestino, 30);

            return true;

        } catch (Exception e) {

            System.out.println(
                "Erro ao realizar backup do banco: " + e.getMessage()
            );

            return false;
        }
    }

    /**
     * Lê a pasta de backup configurada na tela de Configurações e roda o
     * backup nela. É o método chamado tanto pelo backup automático ao abrir
     * o app quanto pela rotina headless (BackupJob) agendada no SO.
     */
    public static boolean realizarBackupConfigurado() {
        String pastaDestino = com.gap.config.GerenciadorConfiguracao
                .carregarConfiguracoes()
                .getProperty("backup.pasta", "backups");

        return realizarBackup(pastaDestino);
    }

    /**
     * Mantém só os N backups mais recentes na pasta, apagando o resto.
     * Sem isso, os arquivos só se acumulam (o backup diário deixaria
     * uma cópia nova por dia, pra sempre).
     */
    private static void limparBackupsAntigos(File diretorio, int manterUltimos) {

        File[] arquivos = diretorio.listFiles((dir, nome) ->
                nome.startsWith("backup_gap_") && nome.endsWith(".db"));

        if (arquivos == null || arquivos.length <= manterUltimos) {
            return;
        }

        java.util.Arrays.sort(
            arquivos,
            java.util.Comparator.comparingLong(File::lastModified)
        );

        int totalParaRemover = arquivos.length - manterUltimos;

        for (int i = 0; i < totalParaRemover; i++) {
            if (arquivos[i].delete()) {
                System.out.println(
                    "Backup antigo removido: " + arquivos[i].getName()
                );
            }
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
                "SELECT id, senha FROM usuarios_sistema "
                + "WHERE login = ?";

        try (
            Connection conn = conectar();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, login);

            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                return false;
            }

            int id = rs.getInt("id");
            String senhaArmazenada = rs.getString("senha");

            if (senhaArmazenada == null) {
                return false;
            }

            // Hash BCrypt (formato começa com $2a$, $2b$ ou $2y$)
            if (senhaArmazenada.startsWith("$2")) {
                return BCrypt.checkpw(senha, senhaArmazenada);
            }

            // Compatibilidade: senha antiga em texto puro (ex.: admin
            // padrão 'Gap'/'2215'). Se bater, migra pra hash na hora,
            // sem exigir nenhuma ação manual.
            if (senhaArmazenada.equals(senha)) {
                atualizarSenhaOperador(id, senha);
                return true;
            }

            return false;

        } catch (Exception e) {

            System.out.println(
                "Erro ao autenticar: " + e.getMessage()
            );

            return false;
        }
    }

    // ==========================================================
    // OPERADORES (USUÁRIOS DO SISTEMA)
    // ==========================================================

    public static boolean criarOperador(
            String nome,
            String login,
            String senhaPlana) {

        String sql =
                "INSERT INTO usuarios_sistema "
                + "(nome, login, senha, nivel) "
                + "VALUES (?, ?, ?, 'Administrador')";

        try (
            Connection conn = conectar();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            String senhaHash = BCrypt.hashpw(senhaPlana, BCrypt.gensalt());

            pstmt.setString(1, nome);
            pstmt.setString(2, login);
            pstmt.setString(3, senhaHash);

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {

            // Cai aqui, por exemplo, quando o login já existe
            // (constraint UNIQUE).
            System.out.println(
                "Erro ao criar operador: " + e.getMessage()
            );

            return false;
        }
    }

    public static List<Operador> listarOperadores() {

        List<Operador> lista = new ArrayList<>();

        String sql =
                "SELECT id, nome, login, nivel "
                + "FROM usuarios_sistema ORDER BY nome";

        try (
            Connection conn = conectar();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql)
        ) {

            while (rs.next()) {
                lista.add(new Operador(
                    rs.getInt("id"),
                    rs.getString("nome"),
                    rs.getString("login"),
                    rs.getString("nivel")
                ));
            }

        } catch (Exception e) {

            System.out.println(
                "Erro ao listar operadores: " + e.getMessage()
            );

            e.printStackTrace();
        }

        return lista;
    }

    public static boolean atualizarSenhaOperador(
            int id,
            String novaSenhaPlana) {

        String sql =
                "UPDATE usuarios_sistema SET senha = ? WHERE id = ?";

        try (
            Connection conn = conectar();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setString(1, BCrypt.hashpw(novaSenhaPlana, BCrypt.gensalt()));
            pstmt.setInt(2, id);

            return pstmt.executeUpdate() > 0;

        } catch (Exception e) {

            System.out.println(
                "Erro ao atualizar senha do operador: " + e.getMessage()
            );

            return false;
        }
    }

    public static boolean testarConexao(String caminho) {

        File arquivo = new File(caminho);

        if (!arquivo.exists()) {
            System.out.println(
                "Arquivo de banco não encontrado em: " + caminho
            );
            return false;
        }

        String url = "jdbc:sqlite:" + caminho;

        try (Connection conn = DriverManager.getConnection(url)) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            System.out.println(
                "Erro ao testar conexão: " + e.getMessage()
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

            String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

            if (msg.contains("unique") || msg.contains("unique constraint")) {
                System.out.println(
                    "Equipamento duplicado (IP ou Nome já existe): " + e.getMessage()
                );
            } else {
                System.out.println(
                    "Erro ao salvar equipamento: " + e.getMessage()
                );
                e.printStackTrace();
            }

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

    // ==========================================================
    // MAPEAMENTO PESSOA ↔ EQUIPAMENTO
    // ==========================================================

    /**
     * Retorna o ID que o equipamento atribuiu a uma pessoa na última
     * sincronização, ou null se essa pessoa nunca foi enviada para
     * aquele equipamento.
     */
    public static Long buscarIdNoEquipamento(int usuarioId, int equipamentoId) {

        String sql =
                "SELECT id_no_equipamento "
                + "FROM equipamento_usuarios "
                + "WHERE usuario_id = ? AND equipamento_id = ?";

        try (
            Connection conn = conectar();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, equipamentoId);

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getLong("id_no_equipamento");
            }

        } catch (Exception e) {
            System.out.println(
                "Erro ao buscar ID no equipamento: " + e.getMessage()
            );
        }

        return null;
    }

    /**
     * Grava (ou atualiza) o ID que o equipamento atribuiu a uma pessoa.
     * Usado após cada criação bem-sucedida durante a sincronização.
     */
    public static void salvarMapeamentoEquipamento(
            int usuarioId,
            int equipamentoId,
            long idNoEquipamento) {

        // INSERT OR REPLACE respeita a PRIMARY KEY (usuario_id, equipamento_id)
        // e atualiza o id_no_equipamento se o par já existir.
        String sql =
                "INSERT OR REPLACE INTO equipamento_usuarios "
                + "(usuario_id, equipamento_id, id_no_equipamento) "
                + "VALUES (?, ?, ?)";

        try (
            Connection conn = conectar();
            PreparedStatement pstmt = conn.prepareStatement(sql)
        ) {

            pstmt.setInt(1, usuarioId);
            pstmt.setInt(2, equipamentoId);
            pstmt.setLong(3, idNoEquipamento);
            pstmt.executeUpdate();

        } catch (Exception e) {
            System.out.println(
                "Erro ao salvar mapeamento equipamento: " + e.getMessage()
            );
        }
    }
}