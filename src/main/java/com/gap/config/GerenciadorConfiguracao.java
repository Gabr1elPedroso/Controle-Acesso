package com.gap.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class GerenciadorConfiguracao {

    // Nome/Caminho do arquivo de configuração
    private static final String ARQUIVO_CONFIG = "config.properties";

    /**
     * Carrega as configurações do arquivo.
     * Se o arquivo não existir, cria um com valores padrão.
     */
    public static Properties carregarConfiguracoes() {
        Properties props = new Properties();
        File arquivo = new File(ARQUIVO_CONFIG);

        // Se o arquivo não existe, cria um padrão (apontando para a própria máquina)
        if (!arquivo.exists()) {
            System.out.println("Arquivo de configuração não encontrado. Criando padrão...");
            salvarConfiguracoes("localhost", "3306", "gap_db", "root", "");
        }

        // Lê o arquivo
        try (FileInputStream fis = new FileInputStream(ARQUIVO_CONFIG)) {
            props.load(fis);
        } catch (IOException e) {
            System.err.println("Erro ao ler o arquivo de configuração: " + e.getMessage());
        }

        return props;
    }

    /**
     * Salva as novas configurações de rede no arquivo config.properties.
     */
    public static void salvarConfiguracoes(String ip, String porta, String banco, String usuario, String senha) {
        Properties props = new Properties();
        
        // Define as chaves e os valores
        props.setProperty("servidor.ip", ip);
        props.setProperty("servidor.porta", porta);
        props.setProperty("banco.nome", banco);
        props.setProperty("banco.usuario", usuario);
        props.setProperty("banco.senha", senha); // Em um cenário real, você pode ofuscar essa senha depois

        // Grava no arquivo
        try (FileOutputStream fos = new FileOutputStream(ARQUIVO_CONFIG)) {
            // O segundo parâmetro é um comentário que vai no topo do arquivo
            props.store(fos, "Configurações de Conexão com o Servidor GAP");
            System.out.println("Configurações salvas com sucesso!");
        } catch (IOException e) {
            System.err.println("Erro ao salvar o arquivo de configuração: " + e.getMessage());
        }
    }
}