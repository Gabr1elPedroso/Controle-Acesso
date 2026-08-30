package com.gap;

import com.gap.banco.ConexaoSQLite;

public class Main {
    public static void main(String[] args) {
        System.out.println("Iniciando Sistema de Controle de Acesso...");

        // 1. Cria/Carrega todas as tabelas (Categorias, Equipamentos, Usuários, Permissões)
        ConexaoSQLite.criarTabelas();

        // 2. Teste da função de Backup (cria uma pasta 'backups' dentro do seu projeto)
        System.out.println("Executando rotina de backup...");
        boolean sucessoBackup = ConexaoSQLite.realizarBackupConfigurado();

        if (sucessoBackup) {
            System.out.println("Rotina de validação concluída sem erros!");
        }
    }
}