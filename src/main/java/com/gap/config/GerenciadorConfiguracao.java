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

        if (arquivo.exists()) {
            try (FileInputStream fis = new FileInputStream(ARQUIVO_CONFIG)) {
                props.load(fis);
            } catch (IOException e) {
                System.err.println("Erro ao ler arquivo de configuração: " + e.getMessage());
            }
        }

        // Garante um valor padrão pra quem ainda não configurou nada
        if (!props.containsKey("banco.caminho")) {
            props.setProperty("banco.caminho", "gap.db");
        }

        return props;
    }

    /**
     * Salva/atualiza uma única chave sem apagar as demais já configuradas.
     * (Antes, salvarCaminhoBanco recriava o arquivo do zero e perdia
     * qualquer outra configuração já salva, como a pasta de backup.)
     */
    public static void salvarPropriedade(String chave, String valor) {
        Properties props = carregarConfiguracoes();
        props.setProperty(chave, valor);

        try (FileOutputStream fos = new FileOutputStream(ARQUIVO_CONFIG)) {
            props.store(fos, "Configuracoes GAP");
        } catch (IOException e) {
            System.err.println("Erro ao salvar configuracao: " + e.getMessage());
        }
    }

    public static void salvarCaminhoBanco(String caminhoOuIp) {
        salvarPropriedade("banco.caminho", caminhoOuIp);
    }
}