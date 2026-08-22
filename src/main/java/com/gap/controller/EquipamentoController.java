package com.gap.controller;

import com.gap.banco.ConexaoSQLite;
import com.gap.model.Equipamento;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

public class EquipamentoController {

    @FXML private Button btnNovoEquipamento;
    @FXML private TableView<Equipamento> tabelaEquipamentos;
    @FXML private TableColumn<Equipamento, String> colunaNomeEquip;
    @FXML private TableColumn<Equipamento, String> colunaIp;
    
    @FXML private VBox painelFormularioEquip;
    @FXML private TextField campoNomeEquip;
    @FXML private TextField campoIp;
    @FXML private TextField campoUsuario;
    @FXML private PasswordField campoSenha;
    @FXML private TextField campoPorta;
    
    @FXML private Button btnExcluirEquip;
    @FXML private Button btnSalvarEquip;

    private ObservableList<Equipamento> listaEquipamentos = FXCollections.observableArrayList();
    private Equipamento equipamentoSelecionado = null;

    @FXML
    public void initialize() {
        // Mágica do painel retrátil
        painelFormularioEquip.managedProperty().bind(painelFormularioEquip.visibleProperty());
        painelFormularioEquip.setVisible(false);

        colunaNomeEquip.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colunaIp.setCellValueFactory(new PropertyValueFactory<>("ip"));

        carregarTabela();
        configurarBotoes();
        configurarSelecaoTabela();
    }

    private void carregarTabela() {
        listaEquipamentos.clear();
        listaEquipamentos.addAll(ConexaoSQLite.listarEquipamentos());
        tabelaEquipamentos.setItems(listaEquipamentos);
    }

    private void configurarBotoes() {
        btnNovoEquipamento.setOnAction(event -> {
            limparFormulario();
            equipamentoSelecionado = null;
            tabelaEquipamentos.getSelectionModel().clearSelection();
            painelFormularioEquip.setVisible(true);
        });

        btnSalvarEquip.setOnAction(event -> {
            String nome = campoNomeEquip.getText().trim();
            String ip = campoIp.getText().trim();
            String usuario = campoUsuario.getText().trim();
            String senha = campoSenha.getText().trim();
            int porta;

            try {
                porta = Integer.parseInt(campoPorta.getText().trim());
            } catch (NumberFormatException e) {
                mostrarAlerta("Atenção", "Porta Inválida", "A porta deve ser um número inteiro.", Alert.AlertType.WARNING);
                return;
            }

            if (nome.isEmpty() || ip.isEmpty()) {
                mostrarAlerta("Atenção", "Preenchimento Obrigatório", "Nome e IP são obrigatórios.", Alert.AlertType.WARNING);
                return;
            }

            boolean sucesso;
            if (equipamentoSelecionado == null) {
                sucesso = ConexaoSQLite.salvarEquipamento(nome, ip, porta, usuario, senha);
            } else {
                sucesso = ConexaoSQLite.atualizarEquipamento(equipamentoSelecionado.getId(), nome, ip, porta, usuario, senha);
            }

            if (sucesso) {
                mostrarAlerta("Sucesso", "Operação Realizada", "Equipamento salvo com sucesso!", Alert.AlertType.INFORMATION);
                limparFormulario();
                painelFormularioEquip.setVisible(false);
                carregarTabela();
            } else {
                mostrarAlerta("Erro", "Falha ao Salvar", "Verifique se o IP já está cadastrado.", Alert.AlertType.ERROR);
            }
        });

        btnExcluirEquip.setOnAction(event -> {
            if (equipamentoSelecionado == null) return;

            Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacao.setTitle("Confirmar Exclusão");
            confirmacao.setHeaderText("Remover Equipamento");
            confirmacao.setContentText("Remover " + equipamentoSelecionado.getNome() + "?");

            confirmacao.showAndWait().ifPresent(resposta -> {
                if (resposta == ButtonType.OK) {
                    if (ConexaoSQLite.excluirEquipamento(equipamentoSelecionado.getId())) {
                        limparFormulario();
                        painelFormularioEquip.setVisible(false);
                        carregarTabela();
                    }
                }
            });
        });
    }

    private void configurarSelecaoTabela() {
        tabelaEquipamentos.getSelectionModel().selectedItemProperty().addListener((obs, antigo, selecionado) -> {
            equipamentoSelecionado = selecionado;
            if (selecionado != null) {
                painelFormularioEquip.setVisible(true);
                campoNomeEquip.setText(selecionado.getNome());
                campoIp.setText(selecionado.getIp());
                campoUsuario.setText(selecionado.getUsuario());
                campoSenha.setText(selecionado.getSenha());
            }
        });
    }

    private void limparFormulario() {
        campoNomeEquip.clear();
        campoIp.clear();
        campoUsuario.setText("admin");
        campoSenha.setText("admin");
    }

    private void mostrarAlerta(String titulo, String cabecalho, String mensagem, Alert.AlertType tipo) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(cabecalho);
        alerta.setContentText(mensagem);
        alerta.showAndWait();
    }
}