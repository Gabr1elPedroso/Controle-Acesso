package com.gap.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class DashboardController {

    @FXML 
    private StackPane areaCentral;

    @FXML
    public void initialize() {
        // Ao abrir o Dashboard, já carrega a tela de Pessoas por padrão
        abrirTelaPessoas();
    }

    @FXML
    public void abrirTelaPessoas() {
        // Lembre-se de renomear seu arquivo layout_principal.fxml para tela_pessoas.fxml!
        carregarTela("/tela_pessoas.fxml");
    }

    @FXML
    public void abrirTelaEquipamentos() {
        carregarTela("/tela_equipamentos.fxml");
    }

    @FXML
    public void abrirTelaSincronizacao() {
        carregarTela("/tela_sincronizacao.fxml");
    }

    @FXML
    public void abrirTelaConfiguracoes() {
        carregarTela("/tela_configuracoes.fxml");
    }

    @FXML
    public void fazerLogout() {
        try {
            // Pega a janela atual
            Stage janela = (Stage) areaCentral.getScene().getWindow();
            
            // Volta para a tela de Login
            Parent telaLogin = FXMLLoader.load(getClass().getResource("/login.fxml"));
            janela.setScene(new Scene(telaLogin, 400, 400));
            janela.centerOnScreen();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Método genérico que carrega um FXML e joga dentro do StackPane (Área Central)
     */
    private void carregarTela(String caminhoFxml) {
        try {
            Parent tela = FXMLLoader.load(getClass().getResource(caminhoFxml));
            
            // Limpa o que tiver no centro e adiciona a nova tela
            areaCentral.getChildren().clear();
            areaCentral.getChildren().add(tela);
            
        } catch (Exception e) {
            System.out.println("Erro ao carregar a tela: " + caminhoFxml);
            e.printStackTrace();
        }
    }
}