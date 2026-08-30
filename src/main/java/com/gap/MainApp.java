package com.gap;

import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage palcoPrincipal) throws Exception {
        // Busca o arquivo FXML na pasta resources
        URL arquivoFXML = getClass().getResource("/login.fxml");
        
        if (arquivoFXML == null) {
            System.out.println("Erro: Arquivo layout_principal.fxml não encontrado!");
            return;
        }

        Parent layout = FXMLLoader.load(arquivoFXML);
        
        // Define a cena inicial
        Scene cena = new Scene(layout, 400, 400); // Resolução padrão de sistemas
        
        palcoPrincipal.setTitle("GAP - Controle de Acesso");
        palcoPrincipal.setScene(cena);
        palcoPrincipal.setResizable(false);
        palcoPrincipal.show();
    }

    public static void main(String[] args) {
        // Inicia o banco de dados e backup antes de abrir a tela
        com.gap.banco.ConexaoSQLite.criarTabelas();
        com.gap.banco.ConexaoSQLite.realizarBackupConfigurado();
        
        launch(args);
    }
}