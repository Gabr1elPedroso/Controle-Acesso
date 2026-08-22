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
    
    // Variável para guardar a foto atual em formato Base64
    private String fotoBase64Atual = null;

    @FXML
    public void initialize() {
        // Configura as colunas
        colunaNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colunaMatricula.setCellValueFactory(new PropertyValueFactory<>("matricula"));

        // Popula as opções de Categoria no ComboBox
        comboCategoria.setItems(FXCollections.observableArrayList(
            "1 - Funcionário",
            "2 - Visitante",
            "3 - Terceirizado",
            "4 - Diretoria"
        ));

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
            tabelaPessoas.getSelectionModel().clearSelection();
        });

        // --- SELEÇÃO DE FOTO ---
        btnEscolherFoto.setOnAction(event -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Selecione a Foto do Usuário");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagens (JPG, PNG)", "*.jpg", "*.jpeg", "*.png")
            );

            // Obtém a janela atual para abrir o explorador de arquivos nela
            Stage janelaAtual = (Stage) btnEscolherFoto.getScene().getWindow();
            File arquivoSelecionado = fileChooser.showOpenDialog(janelaAtual);

            if (arquivoSelecionado != null) {
                try {
                    // 1. Converte o arquivo para Base64 (Para o SQLite e Control iD)
                    byte[] bytesArquivo = Files.readAllBytes(arquivoSelecionado.toPath());
                    fotoBase64Atual = Base64.getEncoder().encodeToString(bytesArquivo);

                    // 2. Exibe a imagem na tela usando o arquivo local
                    Image imagemCarregada = new Image(arquivoSelecionado.toURI().toString());
                    fotoPerfil.setImage(imagemCarregada);
                    
                } catch (Exception e) {
                    mostrarAlerta("Erro", "Falha na Imagem", "Não foi possível carregar a imagem selecionada.", Alert.AlertType.ERROR);
                }
            }
        });

        // --- SALVAR CADASTRO ---
        btnSalvar.setOnAction(event -> {
            String nome = campoNome.getText().trim();
            String matricula = campoMatricula.getText().trim();
            String catSelecionada = comboCategoria.getValue();

            if (nome.isEmpty() || matricula.isEmpty() || catSelecionada == null) {
                mostrarAlerta("Atenção", "Preenchimento Obrigatório", "Preencha Nome, Matrícula e selecione uma Categoria.", Alert.AlertType.WARNING);
                return;
            }

            // Extrai o número do ID da categoria selecionada (Ex: "1 - Funcionário" vira 1)
            int idCategoria = Integer.parseInt(catSelecionada.split(" - ")[0]);

            boolean salvoComSucesso = ConexaoSQLite.salvarUsuario(nome, matricula, idCategoria, fotoBase64Atual);

            if (salvoComSucesso) {
                mostrarAlerta("Sucesso", "Cadastro Realizado", "Usuário salvo com sucesso!", Alert.AlertType.INFORMATION);
                limparFormulario();
                carregarTabela(); 
            } else {
                mostrarAlerta("Erro", "Falha ao Salvar", "A matrícula pode já existir no sistema.", Alert.AlertType.ERROR);
            }
        });
    }

    private void configurarSelecaoTabela() {
        tabelaPessoas.getSelectionModel().selectedItemProperty().addListener((obs, antigo, selecionado) -> {
            if (selecionado != null) {
                // Preenche os campos de texto
                campoNome.setText(selecionado.getNome());
                campoMatricula.setText(selecionado.getMatricula());
                
                // Seleciona a categoria correta no ComboBox
                for (String cat : comboCategoria.getItems()) {
                    if (cat.startsWith(selecionado.getCategoriaId() + " -")) {
                        comboCategoria.setValue(cat);
                        break;
                    }
                }

                // Carrega a foto convertendo o Base64 de volta para Imagem
                fotoBase64Atual = selecionado.getFotoBase64();
                if (fotoBase64Atual != null && !fotoBase64Atual.isEmpty()) {
                    try {
                        byte[] bytesImagem = Base64.getDecoder().decode(fotoBase64Atual);
                        Image img = new Image(new ByteArrayInputStream(bytesImagem));
                        fotoPerfil.setImage(img);
                    } catch (Exception e) {
                        System.out.println("Erro ao decodificar imagem da matrícula " + selecionado.getMatricula());
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