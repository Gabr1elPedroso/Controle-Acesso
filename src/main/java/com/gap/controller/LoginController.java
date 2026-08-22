package com.gap.controller;

import com.gap.banco.ConexaoSQLite;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField campoUsuario;
    @FXML private PasswordField campoSenha;
    @FXML private Button btnLogin;
    @FXML private Label labelErro;

    @FXML
    public void initialize() {
        btnLogin.setOnAction(event -> fazerLogin());
        
        // Permite apertar a tecla "Enter" no campo de senha para logar
        campoSenha.setOnAction(event -> fazerLogin());
    }

    private void fazerLogin() {
        String usuario = campoUsuario.getText();
        String senha = campoSenha.getText();
        
        // Verifica no banco de dados se as credenciais batem
        if (ConexaoSQLite.autenticarSistema(usuario, senha)) {
            abrirSistema();
        } else {
            labelErro.setVisible(true);
            campoSenha.clear();
        }
    }

    private void abrirSistema() {
        try {
            // Pega a janela atual (a janela de login)
            Stage janela = (Stage) btnLogin.getScene().getWindow();
            
            // Carrega a tela do sistema (por enquanto nossa tela de pessoas)
            Parent layoutPrincipal = FXMLLoader.load(getClass().getResource("/dashboard.fxml"));
            
            // Troca a cena atual pela nova tela do sistema e ajusta o tamanho
            janela.setScene(new Scene(layoutPrincipal, 1024, 768));
            janela.centerOnScreen();
            
        } catch (Exception e) {
            e.printStackTrace();
            labelErro.setText("Erro ao carregar o sistema.");
            labelErro.setVisible(true);
        }
    }
}