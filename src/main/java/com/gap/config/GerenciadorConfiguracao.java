package com.gap.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class GerenciadorConfiguracao {

    private static final String ARQUIVO_CONFIG = "config.properties";

    public static Properties carregarConfiguracoes() {
        Properties props = new Properties();
        File arquivo = new File(ARQUIVO_CONFIG);

        if (!arquivo.exists()) {
            // Por padrão, aponta para o banco local gap.db que já estamos usando
            salvarCaminhoBanco("gap.db");
        }

        try (FileInputStream fis = new FileInputStream(ARQUIVO_CONFIG)) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("Erro ao ler arquivo de configuração: " + e.getMessage());
        }

        return props;
    }

    public static void salvarCaminhoBanco(String caminhoOuIp) {
        Properties props = new Properties();
        props.setProperty("banco.caminho", caminhoOuIp);

        try (FileOutputStream fos = new FileOutputStream(ARQUIVO_CONFIG)) {
            props.store(fos, "Configuracoes de Conexao GAP");
        } catch (IOException e) {
            System.err.println("Erro ao salvar configuracao: " + e.getMessage());
        }
    }
}