package com.gap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import com.gap.banco.ConexaoSQLite;
import com.gap.service.ControlIdService;

public class Main {

    public static void main(String[] args) {
        System.out.println("Sistema de Controle de Acesso");

        ConexaoSQLite.criarTabela();

        ControlIdService api = new ControlIdService();

        try {

            boolean logado = api.fazerLogin("admin", "admin");

            if (logado) {
                String nomeFuncionario = "Gabriel Pedroso";
                String matricula = "2026";

                long idGerado = api.criarUsuario(nomeFuncionario, matricula);

                ConexaoSQLite.salvarUsuarioLocal(nomeFuncionario, matricula, idGerado);

                Path caminhoFoto = Path.of("C:\\Users\\HP\\Downloads\\gabriel_pedroso.jpg");

                if (Files.exists(caminhoFoto)) {
                    byte[] bytesDaFoto = Files.readAllBytes(caminhoFoto);
                    String fotoBase64 = Base64.getEncoder().encodeToString(bytesDaFoto);

                    api.enviarFoto(idGerado, fotoBase64);
                    System.out.println("Cadastro Finalizado com sucesso!");
                } else {
                    System.out.println("Arquivo de foto não encontrado: " + caminhoFoto.toString());
                }

            }

        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}
