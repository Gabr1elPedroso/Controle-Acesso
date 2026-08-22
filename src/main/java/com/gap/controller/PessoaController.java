package com.gap.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Base64;

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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

public class PessoaController {

    // Lado Esquerdo - Tabela
    @FXML private TextField campoBusca;
    @FXML private TableView<Pessoa> tabelaPessoas; 
    @FXML private TableColumn<Pessoa, String> colunaNome;
    @FXML private TableColumn<Pessoa, String> colunaMatricula;
    @FXML private Button btnNovo;

    // Lado Direito - Contêiner e Formulário
    @FXML private VBox painelFormulario;
    @FXML private ImageView fotoPerfil;
    @FXML private Button btnEscolherFoto;
    @FXML private TextField campoNome;
    @FXML private TextField campoMatricula;
    @FXML private ComboBox<String> comboCategoria;
    @FXML private VBox listaEquipamentos;
    
    @FXML private Button btnExcluir;
    @FXML private Button btnSalvar;

    private ObservableList<Pessoa> listaPessoas = FXCollections.observableArrayList();
    private String fotoBase64Atual = null;
    
    // Guarda a pessoa sendo editada (se for null, trata-se de um novo cadastro)
    private Pessoa pessoaSelecionada = null;

    @FXML
    public void initialize() {
        painelFormulario.managedProperty().bind(painelFormulario.visibleProperty());
        colunaNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colunaMatricula.setCellValueFactory(new PropertyValueFactory<>("matricula"));

        comboCategoria.setItems(FXCollections.observableArrayList(
            "1 - Funcionário",
            "2 - Visitante",
            "3 - Terceirizado",
            "4 - Diretoria"
        ));

        // Inicia com o formulário oculto
        painelFormulario.setVisible(false);

        carregarTabela();
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
            pessoaSelecionada = null; // Modo inclusão
            tabelaPessoas.getSelectionModel().clearSelection();
            painelFormulario.setVisible(true); // Exibe o formulário limpo
        });

        btnEscolherFoto.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Selecione a Foto do Usuário");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagens (JPG, PNG)", "*.jpg", "*.jpeg", "*.png")
            );

            Stage janelaAtual = (Stage) btnEscolherFoto.getScene().getWindow();
            File arquivoSelecionado = fileChooser.showOpenDialog(janelaAtual);

            if (arquivoSelecionado != null) {
                try {
                    byte[] bytesArquivo = Files.readAllBytes(arquivoSelecionado.toPath());
                    fotoBase64Atual = Base64.getEncoder().encodeToString(bytesArquivo);
                    Image imagemCarregada = new Image(arquivoSelecionado.toURI().toString());
                    fotoPerfil.setImage(imagemCarregada);
                } catch (Exception e) {
                    mostrarAlerta("Erro", "Falha na Imagem", "Não foi possível carregar a imagem.", Alert.AlertType.ERROR);
                }
            }
        });

        // --- SALVAR / ATUALIZAR CADASTRO ---
        btnSalvar.setOnAction(event -> {
            String nome = campoNome.getText().trim();
            String matricula = campoMatricula.getText().trim();
            String catSelecionada = comboCategoria.getValue();

            if (nome.isEmpty() || matricula.isEmpty() || catSelecionada == null) {
                mostrarAlerta("Atenção", "Preenchimento Obrigatório", "Preencha Nome, Matrícula e Categoria.", Alert.AlertType.WARNING);
                return;
            }

            int idCategoria = Integer.parseInt(catSelecionada.split(" - ")[0]);
            boolean sucesso;

            if (pessoaSelecionada == null) {
                // INSERT: Novo cadastro
                sucesso = ConexaoSQLite.salvarUsuario(nome, matricula, idCategoria, fotoBase64Atual);
            } else {
                // UPDATE: Edição de registro existente
                sucesso = ConexaoSQLite.atualizarUsuario(pessoaSelecionada.getId(), nome, matricula, idCategoria, fotoBase64Atual);
            }

            if (sucesso) {
                mostrarAlerta("Sucesso", "Operação Realizada", "Dados salvos com sucesso!", Alert.AlertType.INFORMATION);
                limparFormulario();
                painelFormulario.setVisible(false);
                carregarTabela(); 
            } else {
                mostrarAlerta("Erro", "Falha ao Salvar", "Não foi possível salvar as alterações.", Alert.AlertType.ERROR);
            }
        });

        // --- EXCLUIR CADASTRO ---
        btnExcluir.setOnAction(event -> {
            if (pessoaSelecionada == null) return;

            Alert confirmacao = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacao.setTitle("Confirmar Exclusão");
            confirmacao.setHeaderText("Excluir Usuário");
            confirmacao.setContentText("Deseja realmente excluir " + pessoaSelecionada.getNome() + "?");

            confirmacao.showAndWait().ifPresent(resposta -> {
                if (resposta == javafx.scene.control.ButtonType.OK) {
                    if (ConexaoSQLite.excluirUsuario(pessoaSelecionada.getId())) {
                        mostrarAlerta("Sucesso", "Usuário Removido", "Registro excluído com sucesso.", Alert.AlertType.INFORMATION);
                        limparFormulario();
                        painelFormulario.setVisible(false);
                        carregarTabela();
                    }
                }
            });
        });
    }

    private void configurarSelecaoTabela() {
        tabelaPessoas.getSelectionModel().selectedItemProperty().addListener((obs, antigo, selecionado) -> {
            pessoaSelecionada = selecionado;

            if (selecionado != null) {
                painelFormulario.setVisible(true); // Exibe o formulário ao selecionar
                
                campoNome.setText(selecionado.getNome());
                campoMatricula.setText(selecionado.getMatricula());

                for (String cat : comboCategoria.getItems()) {
                    if (cat.startsWith(selecionado.getCategoriaId() + " -")) {
                        comboCategoria.setValue(cat);
                        break;
                    }
                }

                fotoBase64Atual = selecionado.getFotoBase64();
                if (fotoBase64Atual != null && !fotoBase64Atual.isEmpty()) {
                    try {
                        byte[] bytesImagem = Base64.getDecoder().decode(fotoBase64Atual);
                        fotoPerfil.setImage(new Image(new ByteArrayInputStream(bytesImagem)));
                    } catch (Exception e) {
                        fotoPerfil.setImage(null);
                    }
                } else {
                    fotoPerfil.setImage(null);
                }
            }
        });
    }

    private void limparFormulario() {
        campoNome.clear();
        campoMatricula.clear();
        fotoBase64Atual = null;
        pessoaSelecionada = null;
        
        if (comboCategoria != null) {
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