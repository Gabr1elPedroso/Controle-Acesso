package com.gap.controller;

import com.gap.service.SincronizacaoService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;

public class SincronizacaoController {

    @FXML private Label lblProgresso;
    @FXML private ProgressBar barraProgresso;
    @FXML private TextArea areaLog;
    @FXML private Button btnSincronizar;
    @FXML private Button btnFechar;

    private final SincronizacaoService service = new SincronizacaoService();

    @FXML
    public void handleSincronizar() {

        // Limpa o log anterior e desabilita o botão durante a execução
        areaLog.clear();
        btnSincronizar.setDisable(true);
        barraProgresso.setProgress(0);
        lblProgresso.setText("Executando...");

        // Roda em thread separada — nunca bloqueia a UI
        Thread thread = new Thread(() -> {

            service.sincronizarTudo(

                // Callback de log — atualiza o TextArea na thread da UI
                (mensagem, erro) -> Platform.runLater(() -> {
                    areaLog.appendText(mensagem + "\n");

                    // Rola o log automaticamente para o final
                    areaLog.setScrollTop(Double.MAX_VALUE);
                }),

                // Callback de progresso — atualiza a barra na thread da UI
                pct -> Platform.runLater(() -> {
                    barraProgresso.setProgress(pct);
                    int percentual = (int) (pct * 100);
                    lblProgresso.setText(percentual + "% concluído");
                })
            );

            // Garante que a barra chegue a 100% e o botão volte
            Platform.runLater(() -> {
                barraProgresso.setProgress(1.0);
                lblProgresso.setText("Concluído.");
                btnSincronizar.setDisable(false);
            });
        });

        thread.setDaemon(true); // Não impede o app de fechar
        thread.start();
    }

    @FXML
    public void handleFechar() {
        // Remove a tela da área central voltando para Pessoas
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/tela_pessoas.fxml"));
            javafx.scene.Parent telaPessoas = loader.load();

            javafx.scene.layout.StackPane areaCentral =
                    (javafx.scene.layout.StackPane) areaLog.getScene()
                    .lookup("#areaCentral");

            if (areaCentral != null) {
                areaCentral.getChildren().clear();
                areaCentral.getChildren().add(telaPessoas);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}