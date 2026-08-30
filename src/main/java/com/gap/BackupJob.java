package com.gap;

import com.gap.banco.ConexaoSQLite;

/**
 * Rotina de backup headless — sem JavaFX. Feita pra ser chamada pelo
 * Agendador de Tarefas do Windows (ou cron), rodando no horário
 * configurado independente do aplicativo gráfico estar aberto ou não.
 *
 * Uso (depois de "mvn package"):
 *   java -cp controle-acesso-1.0-SNAPSHOT.jar com.gap.BackupJob
 */
public class BackupJob {

    public static void main(String[] args) {

        System.out.println("Iniciando rotina agendada de backup...");

        // Garante que o schema (incluindo colunas novas) existe,
        // mesmo se essa rotina rodar antes do app gráfico ter sido aberto.
        ConexaoSQLite.criarTabelas();

        boolean sucesso = ConexaoSQLite.realizarBackupConfigurado();

        if (!sucesso) {
            System.out.println("Falha ao executar o backup agendado.");
            System.exit(1);
        }

        System.out.println("Backup agendado executado com sucesso.");
        System.exit(0);
    }
}