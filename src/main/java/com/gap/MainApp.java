package com.gap;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override

    public void start(Stage palcoPrincipal) {
        Label texto = new Label("Sistema de Controle de Acesso");
        StackPane layout = new StackPane(texto);

        Scene cena = new Scene(layout, 800, 600);

        palcoPrincipal.setTitle("GAP - Controle de Acesso");
        palcoPrincipal.setScene(cena);
        palcoPrincipal.show();

    }

    public static void main(String[] args) {

        com.gap.banco.ConexaoSQLite.criarTabelas();
        com.gap.banco.ConexaoSQLite.realizarBackup("backups");
        launch(args);
    }
}


