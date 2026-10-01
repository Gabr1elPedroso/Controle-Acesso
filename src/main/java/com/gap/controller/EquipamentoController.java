package com.gap.controller;

import com.gap.banco.ConexaoSQLite;
import com.gap.model.Categoria;
import com.gap.model.Equipamento;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
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

    @FXML private CheckBox chkRestringirCategoria;
    @FXML private VBox painelCategorias;
    private final Map<CheckBox, Categoria> checkBoxesCategorias = new HashMap<>();
    
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

        // Monta um CheckBox por categoria cadastrada (Funcionário,
        // Diretoria, Visitante, e quaisquer outras que existirem)
        painelCategorias.managedProperty().bind(painelCategorias.visibleProperty());
        painelCategorias.setVisible(false);

        for (Categoria categoria : ConexaoSQLite.listarCategorias()) {
            CheckBox cb = new CheckBox(categoria.getNome());
            checkBoxesCategorias.put(cb, categoria);
            painelCategorias.getChildren().add(cb);
        }

        chkRestringirCategoria.setOnAction(e ->
                painelCategorias.setVisible(chkRestringirCategoria.isSelected()));
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
                carregarTabela();

                int equipamentoId = (equipamentoSelecionado != null)
                        ? equipamentoSelecionado.getId()
                        : listaEquipamentos.stream()
                                .filter(e -> e.getIp().equalsIgnoreCase(ip))
                                .findFirst()
                                .map(Equipamento::getId)
                                .orElse(-1);

                if (equipamentoId != -1) {
                    Set<Integer> categoriasSelecionadas = new HashSet<>();
                    for (Map.Entry<CheckBox, Categoria> entry : checkBoxesCategorias.entrySet()) {
                        if (entry.getKey().isSelected()) {
                            categoriasSelecionadas.add(entry.getValue().getId());
                        }
                    }
                    ConexaoSQLite.salvarRestricaoCategorias(
                            equipamentoId, chkRestringirCategoria.isSelected(), categoriasSelecionadas);
                }

                mostrarAlerta("Sucesso", "Operação Realizada", "Equipamento salvo com sucesso!", Alert.AlertType.INFORMATION);
                limparFormulario();
                painelFormularioEquip.setVisible(false);
            } else {
                // Verifica se já existe equipamento com esse IP ou Nome
                // para dar uma mensagem mais precisa ao usuário
                boolean ipDuplicado = ConexaoSQLite.listarEquipamentos().stream()
                        .anyMatch(e -> e.getIp().equalsIgnoreCase(ip)
                                && (equipamentoSelecionado == null || e.getId() != equipamentoSelecionado.getId()));

                if (ipDuplicado) {
                    mostrarAlerta("Erro", "IP já cadastrado",
                            "Já existe um equipamento com o IP " + ip + ". Use um IP diferente.",
                            Alert.AlertType.ERROR);
                } else {
                    mostrarAlerta("Erro", "Falha ao Salvar",
                            "Não foi possível salvar o equipamento. Verifique o console para mais detalhes.",
                            Alert.AlertType.ERROR);
                }
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
                campoPorta.setText(String.valueOf(selecionado.getporta()));
                campoUsuario.setText(selecionado.getUsuario());
                campoSenha.setText(selecionado.getSenha());

                boolean restringe = ConexaoSQLite.restringePorCategoria(selecionado.getId());
                Set<Integer> permitidas = ConexaoSQLite.categoriasPermitidas(selecionado.getId());

                chkRestringirCategoria.setSelected(restringe);
                painelCategorias.setVisible(restringe);

                for (Map.Entry<CheckBox, Categoria> entry : checkBoxesCategorias.entrySet()) {
                    boolean marcado = restringe && permitidas != null && permitidas.contains(entry.getValue().getId());
                    entry.getKey().setSelected(marcado);
                }
            }
        });
    }

    private void limparFormulario() {
        campoNomeEquip.clear();
        campoIp.clear();
        campoPorta.clear();
        campoUsuario.setText("admin");
        campoSenha.setText("admin");

        chkRestringirCategoria.setSelected(false);
        painelCategorias.setVisible(false);
        for (CheckBox cb : checkBoxesCategorias.keySet()) {
            cb.setSelected(false);
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