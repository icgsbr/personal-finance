package com.finance.ui.controller;

import com.finance.service.BackupService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupController {

    private final BackupService backupService = new BackupService();
    private final VBox view = new VBox(20);
    private final Runnable onImportDone;

    public BackupController(Runnable onImportDone) {
        this.onImportDone = onImportDone;
        view.setPadding(new Insets(24));
        view.setMaxWidth(600);
        buildView();
    }

    private void buildView() {
        Label title = new Label("Backup e Importação");
        title.getStyleClass().add("title");

        view.getChildren().addAll(title, buildExportCard(), buildImportCard());
    }

    private VBox buildExportCard() {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("card", "card-neutral");
        card.setPadding(new Insets(16));

        Label cardTitle = new Label("📤 Exportar Backup");
        cardTitle.getStyleClass().add("section-title");

        Label desc = new Label("Gera um arquivo .json com todos os seus dados: lançamentos, recorrentes, dívidas e investimentos.");
        desc.setWrapText(true);
        desc.setStyle("-fx-opacity: 0.8;");

        Button exportBtn = new Button("Baixar Backup");
        exportBtn.getStyleClass().add("btn-primary");
        exportBtn.setOnAction(e -> doExport());

        card.getChildren().addAll(cardTitle, desc, exportBtn);
        return card;
    }

    private VBox buildImportCard() {
        VBox card = new VBox(10);
        card.getStyleClass().addAll("card", "card-income");
        card.setPadding(new Insets(16));

        Label cardTitle = new Label("📥 Importar Backup");
        cardTitle.getStyleClass().add("section-title");

        Label desc = new Label("Importa dados de um arquivo de backup. Registros já existentes serão ignorados automaticamente — nenhum dado atual será sobrescrito.");
        desc.setWrapText(true);
        desc.setStyle("-fx-opacity: 0.8;");

        Label warningLbl = new Label("ℹ️ A deduplicação é feita por conteúdo (descrição, valor, data). Registros idênticos não serão duplicados.");
        warningLbl.setWrapText(true);
        warningLbl.getStyleClass().add("text-neutral");
        warningLbl.setStyle("-fx-font-size: 11px;");

        Button importBtn = new Button("Selecionar Arquivo e Importar");
        importBtn.getStyleClass().add("btn-primary");
        importBtn.setOnAction(e -> doImport());

        card.getChildren().addAll(cardTitle, desc, warningLbl, importBtn);
        return card;
    }

    private void doExport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salvar Backup");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Backup", "*.json"));
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"));
        chooser.setInitialFileName("financas-backup-" + timestamp + ".json");

        Window window = view.getScene() != null ? view.getScene().getWindow() : null;
        File file = chooser.showSaveDialog(window);
        if (file == null) return;

        try {
            backupService.export(file);
            showInfo("✅ Backup exportado com sucesso!\n\nArquivo salvo em:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            showError("Erro ao exportar backup:\n" + ex.getMessage());
        }
    }

    private void doImport() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Selecionar Backup");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Backup", "*.json"));

        Window window = view.getScene() != null ? view.getScene().getWindow() : null;
        File file = chooser.showOpenDialog(window);
        if (file == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Importação");
        confirm.setHeaderText("Importar dados de: " + file.getName());
        confirm.setContentText("Registros já existentes serão ignorados.\nDeseja continuar?");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        try {
            BackupService.ImportResult result = backupService.importFrom(file);
            showInfo(result.summary());
            if (result.totalImported() > 0 && onImportDone != null) {
                onImportDone.run();
            }
        } catch (Exception ex) {
            showError("Erro ao importar backup:\n" + ex.getMessage());
        }
    }

    private void showInfo(String msg) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Backup");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    public VBox getView() { return view; }
}
