package com.gap.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class PessoaController {
    
    @FXML private TextField campoBusca;
    @FXML private TableView<?> tabelaPessoas;
    @FXML private TableColumn<?, ?> colunaNome;
    @FXML private TableColumn<?, ?> colunaMatricula;
    @FXML private Button btnNovo;


    @FXML private ImageView fotoPerfil;
    @FXML private Button btnEscolherFoto;
    @FXML private TextField campoNome;
    @FXML private TextField campoMatricula;
    @FXML private ComboBox<?> comboCategoria;
    @FXML private VBox listaEquipamentos;

    @FXML private Button btnExcluir;
    @FXML private Button btnSalvar;

    @FXML
    public void initialize(){
        System.out.println("Tela de Pessoas Carregada com Sucesso!");

        configurarBotoes();
    }

    private void configurarBotoes(){
        btnNovo.setOnAction(event -> {
            limparFormulario();
            System.out.println("Preparando formulario para novo usuario");

        });

        btnEscolherFoto.setOnAction(event -> {
            System.out.println("Abrindo Janela para escolher foto");
        });
    }

    private void limparFormulario(){
        campoNome.clear();
        campoMatricula.clear();
        comboCategoria.getSelectionModel().clearSelection();
        fotoPerfil.setImage(null);
    }

}
