package com.gap.controller;

import java.io.File;

import com.gap.banco.ConexaoSQLite;
import com.gap.config.GerenciadorConfiguracao;
import com.gap.model.Operador;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class TelaConfiguracoesController {

    // Aba 1 - Conexão com o Servidor
    @FXML private TextField txtCaminhoBanco;
    @FXML private Button btnProcurar;
    @FXML private Label lblStatusConexao;

    // Aba 2 - Operadores do Sistema
    @FXML private TextField txtNomeOperador;
    @FXML private TextField txtLoginOperador;
    @FXML private PasswordField txtSenhaOperador;
    @FXML private ComboBox<String> cmbNivelAcesso;
    @FXML private TableView<Operador> tblOperadores;
    @FXML private TableColumn<Operador, Integer> colId;
    @FXML private TableColumn<Operador, String> colNome;
    @FXML private TableColumn<Operador, String> colLogin;
    @FXML private TableColumn<Operador, String> colNivel;

    private final ObservableList<Operador> listaOperadores = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        // --- Aba 1: carrega o caminho já configurado ---
        String caminhoAtual = GerenciadorConfiguracao.carregarConfiguracoes()
                .getProperty("banco.caminho", "gap.db");
        txtCaminhoBanco.setText(caminhoAtual);

        // --- Aba 2: nível único por enquanto, sem lógica de permissão ---
        cmbNivelAcesso.setItems(FXCollections.observableArrayList("Administrador"));
        cmbNivelAcesso.getSelectionModel().selectFirst();

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colLogin.setCellValueFactory(new PropertyValueFactory<>("login"));
        colNivel.setCellValueFactory(new PropertyValueFactory<>("nivel"));
        tblOperadores.setItems(listaOperadores);

        carregarOperadores();
    }

    // ==========================================================
    // ABA 1 - CONEXÃO COM O SERVIDOR
    // ==========================================================

    @FXML
    public void handleProcurarArquivo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecione o arquivo do banco de dados (gap.db)");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Banco SQLite (*.db)", "*.db")
        );

        Stage janelaAtual = (Stage) btnProcurar.getScene().getWindow();
        File arquivo = fileChooser.showOpenDialog(janelaAtual);

        if (arquivo != null) {
            txtCaminhoBanco.setText(arquivo.getAbsolutePath());
        }
    }

    @FXML
    public void handleTestarConexao() {
        String caminho = txtCaminhoBanco.getText().trim();

        if (caminho.isEmpty()) {
            lblStatusConexao.setText("Informe um caminho antes de testar.");
            lblStatusConexao.setTextFill(Color.web("#c62828"));
            return;
        }

        boolean ok = ConexaoSQLite.testarConexao(caminho);

        if (ok) {
            lblStatusConexao.setText("Conexão bem-sucedida!");
            lblStatusConexao.setTextFill(Color.web("#2e7d32"));
        } else {
            lblStatusConexao.setText("Não foi possível conectar nesse caminho.");
            lblStatusConexao.setTextFill(Color.web("#c62828"));
        }
    }

    @FXML
    public void handleSalvarConfiguracao() {
        String caminho = txtCaminhoBanco.getText().trim();

        if (caminho.isEmpty()) {
            mostrarAlerta(
                "Atenção", "Caminho obrigatório",
                "Informe o caminho do banco antes de salvar.",
                Alert.AlertType.WARNING
            );
            return;
        }

        GerenciadorConfiguracao.salvarCaminhoBanco(caminho);

        mostrarAlerta(
            "Sucesso", "Configuração Salva",
            "O novo caminho do banco será usado a partir da próxima conexão.",
            Alert.AlertType.INFORMATION
        );
    }

    // ==========================================================
    // ABA 2 - OPERADORES DO SISTEMA
    // ==========================================================

    private void carregarOperadores() {
        listaOperadores.clear();
        listaOperadores.addAll(ConexaoSQLite.listarOperadores());
    }

    @FXML
    public void handleCadastrarOperador() {
        String nome = txtNomeOperador.getText().trim();
        String login = txtLoginOperador.getText().trim();
        String senha = txtSenhaOperador.getText();

        if (nome.isEmpty() || login.isEmpty() || senha.isEmpty()) {
            mostrarAlerta(
                "Atenção", "Preenchimento Obrigatório",
                "Preencha Nome, Usuário e Senha.",
                Alert.AlertType.WARNING
            );
            return;
        }

        if (senha.length() < 4) {
            mostrarAlerta(
                "Atenção", "Senha muito curta",
                "Use uma senha com pelo menos 4 caracteres.",
                Alert.AlertType.WARNING
            );
            return;
        }

        boolean sucesso = ConexaoSQLite.criarOperador(nome, login, senha);

        if (sucesso) {
            mostrarAlerta(
                "Sucesso", "Operador Cadastrado",
                "Operador criado com sucesso!",
                Alert.AlertType.INFORMATION
            );
            handleLimparCampos();
            carregarOperadores();
        } else {
            mostrarAlerta(
                "Erro", "Falha ao Cadastrar",
                "Não foi possível criar o operador. O login informado já pode estar em uso.",
                Alert.AlertType.ERROR
            );
        }
    }

    @FXML
    public void handleLimparCampos() {
        txtNomeOperador.clear();
        txtLoginOperador.clear();
        txtSenhaOperador.clear();
        cmbNivelAcesso.getSelectionModel().selectFirst();
    }

    private void mostrarAlerta(String titulo, String cabecalho, String mensagem, Alert.AlertType tipo) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(cabecalho);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }
}