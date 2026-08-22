package com.gap.banco;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;

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
}