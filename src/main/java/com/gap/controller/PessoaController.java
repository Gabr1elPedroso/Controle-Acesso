package com.gap.controller;

import com.gap.banco.ConexaoSQLite;
import com.gap.model.Pessoa;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class PessoaController {

    // Lado Esquerdo - Tabela
    @FXML private TextField campoBusca;
    @FXML private TableView<Pessoa> tabelaPessoas; 
    @FXML private TableColumn<Pessoa, String> colunaNome;
    @FXML private TableColumn<Pessoa, String> colunaMatricula;
    @FXML private Button btnNovo;

    // Lado Direito - Formulário
    @FXML private ImageView fotoPerfil;
    @FXML private Button btnEscolherFoto;
    @FXML private TextField campoNome;
    @FXML private TextField campoMatricula;
    @FXML private ComboBox<String> comboCategoria;
    @FXML private VBox listaEquipamentos;
    
    @FXML private Button btnExcluir;
    @FXML private Button btnSalvar;

    private ObservableList<Pessoa> listaPessoas = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Configura as colunas para lerem as propriedades 'nome' e 'matricula' da classe Pessoa
        colunaNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colunaMatricula.setCellValueFactory(new PropertyValueFactory<>("matricula"));

        // Carrega os dados na tabela
        carregarTabela();

        // Configura ações de clique nos botões e na tabela
        configurarBotoes();
        configurarSelecaoTabela();
    }

    private void carregarTabela() {
        listaPessoas.clear();
        listaPessoas.addAll(ConexaoSQLite.listarUsuarios());
        tabelaPessoas.setItems(listaPessoas);
    }

    private void configurarBotoes() {
        btnNovo.setOnAction(event -> {
            limparFormulario();
            tabelaPessoas.getSelectionModel().clearSelection();
        });

        btnSalvar.setOnAction(event -> {
            String nome = campoNome.getText().trim();
            String matricula = campoMatricula.getText().trim();

            if (nome.isEmpty() || matricula.isEmpty()) {
                mostrarAlerta("Atenção", "Preenchimento Obrigatório", "Por favor, preencha o Nome e a Matrícula.", Alert.AlertType.WARNING);
                return;
            }

            boolean salvoComSucesso = ConexaoSQLite.salvarUsuario(nome, matricula, 1, null);

            if (salvoComSucesso) {
                mostrarAlerta("Sucesso", "Cadastro Realizado", "O usuário " + nome + " foi salvo com sucesso!", Alert.AlertType.INFORMATION);
                limparFormulario();
                carregarTabela(); // Atualiza a tabela na hora!
            } else {
                mostrarAlerta("Erro", "Falha ao Salvar", "Verifique se a matrícula já está cadastrada.", Alert.AlertType.ERROR);
            }
        });

        btnEscolherFoto.setOnAction(event -> {
            mostrarAlerta("Em breve", "Funcionalidade", "A seleção de fotos será configurada a seguir!", Alert.AlertType.INFORMATION);
        });
    }

    private void configurarSelecaoTabela() {
        // Preenche o formulário automaticamente ao clicar em uma pessoa da tabela
        tabelaPessoas.getSelectionModel().selectedItemProperty().addListener((obs, antigo, selecionado) -> {
            if (selecionado != null) {
                campoNome.setText(selecionado.getNome());
                campoMatricula.setText(selecionado.getMatricula());
            }
        });
    }

    private void limparFormulario() {
        campoNome.clear();
        campoMatricula.clear();
        if (comboCategoria != null && comboCategoria.getSelectionModel() != null) {
            comboCategoria.getSelectionModel().clearSelection();
        }
        if (fotoPerfil != null) {
            fotoPerfil.setImage(null);
        }
    }

    private void mostrarAlerta(String titulo, String cabecalho, String mensagem, Alert.AlertType tipo) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(cabecalho);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }
}