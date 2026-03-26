package com.finance.ui.controller;

import com.finance.model.Debt;
import com.finance.model.PaymentMethod;
import com.finance.service.FinanceService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class DebtController {

    private final FinanceService service;
    private final VBox view = new VBox(12);
    private TitledPane formPane;
    private VBox debtCardsBox;

    private TextField descField, installmentField, installmentsField;
    private ComboBox<PaymentMethod> paymentCombo;
    private DatePicker firstChargeDatePicker;
    private Button saveBtn, cancelBtn;
    private Long editingId = null;

    public DebtController(FinanceService service) {
        this.service = service;
        view.setPadding(new Insets(16));
        buildView();
    }

    private void buildView() {
        Label title = new Label("Dívidas e Parcelamentos");
        title.getStyleClass().add("title");

        formPane = new TitledPane("Nova Dívida / Parcelamento", buildForm());
        formPane.setExpanded(false);
        formPane.skinProperty().addListener((obs, o, n) -> applyFormPaneStyle());

        debtCardsBox = new VBox(10);
        ScrollPane scroll = new ScrollPane(debtCardsBox);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(title, formPane, scroll);
        refreshCards();
    }

    private GridPane buildForm() {
        descField = new TextField(); descField.setPromptText("Descrição (ex: Financiamento TV)");
        installmentField = new TextField(); installmentField.setPromptText("Valor da parcela");
        installmentsField = new TextField(); installmentsField.setPromptText("Nº de parcelas");
        paymentCombo = new ComboBox<>(FXCollections.observableArrayList(PaymentMethod.values()));
        paymentCombo.setPromptText("Forma de pagamento");
        firstChargeDatePicker = new DatePicker(LocalDate.now());

        // Preview do total ao digitar
        Runnable updatePreview = () -> {
            try {
                double inst = Double.parseDouble(installmentField.getText().replace(",", "."));
                int n = Integer.parseInt(installmentsField.getText().trim());
                installmentField.setStyle("-fx-border-color: #a6e3a1;");
                installmentsField.setStyle("-fx-border-color: #a6e3a1;");
            } catch (NumberFormatException ignored) {
                installmentField.setStyle("");
                installmentsField.setStyle("");
            }
        };
        installmentField.textProperty().addListener((o, old, v) -> updatePreview.run());
        installmentsField.textProperty().addListener((o, old, v) -> updatePreview.run());

        Label totalPreviewLabel = new Label("");
        totalPreviewLabel.getStyleClass().add("text-neutral");
        installmentField.textProperty().addListener((o, old, v) -> updateTotalPreview(totalPreviewLabel));
        installmentsField.textProperty().addListener((o, old, v) -> updateTotalPreview(totalPreviewLabel));

        saveBtn = new Button("Salvar");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> save());

        cancelBtn = new Button("Cancelar");
        cancelBtn.setVisible(false);
        cancelBtn.setOnAction(e -> cancelEdit());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Descrição:"), 0, 0);          grid.add(descField, 1, 0);
        grid.add(new Label("Valor da Parcela:"), 2, 0);   grid.add(installmentField, 3, 0);
        grid.add(new Label("Nº de Parcelas:"), 0, 1);     grid.add(installmentsField, 1, 1);
        grid.add(new Label("Pagamento:"), 2, 1);          grid.add(paymentCombo, 3, 1);
        grid.add(new Label("1ª Cobrança:"), 0, 2);        grid.add(firstChargeDatePicker, 1, 2);
        grid.add(totalPreviewLabel, 2, 2);
        grid.add(new HBox(8, saveBtn, cancelBtn), 3, 2);

        return grid;
    }

    private void updateTotalPreview(Label label) {
        try {
            double inst = Double.parseDouble(installmentField.getText().replace(",", "."));
            int n = Integer.parseInt(installmentsField.getText().trim());
            label.setText(String.format("Total: R$ %,.2f", inst * n));
        } catch (NumberFormatException e) {
            label.setText("");
        }
    }

    private void loadForEdit(Debt d) {
        editingId = d.getId();
        descField.setText(d.getDescription());
        installmentField.setText(String.valueOf(d.getInstallmentAmount()));
        installmentsField.setText(String.valueOf(d.getTotalInstallments()));
        paymentCombo.setValue(d.getPaymentMethod());
        firstChargeDatePicker.setValue(d.getStartDate());
        saveBtn.setText("Atualizar");
        cancelBtn.setVisible(true);
        formPane.setText("Editando Dívida");
        applyFormPaneStyle();
        formPane.setExpanded(true);
    }

    private void save() {
        try {
            String desc = descField.getText().trim();
            double installment = Double.parseDouble(installmentField.getText().replace(",", "."));
            int installments = Integer.parseInt(installmentsField.getText().trim());
            PaymentMethod payment = paymentCombo.getValue();
            LocalDate firstCharge = firstChargeDatePicker.getValue();

            if (desc.isEmpty() || payment == null || firstCharge == null) {
                new Alert(Alert.AlertType.WARNING, "Preencha todos os campos.", ButtonType.OK).showAndWait();
                return;
            }

            Debt debt = new Debt(desc, installment, installments, firstCharge, payment);
            if (editingId != null) {
                debt.setId(editingId);
                // Preserva parcelas pagas ao editar
                service.findDebtById(editingId).ifPresent(old -> debt.setPaidInstallments(old.getPaidInstallments()));
            }
            service.saveDebt(debt);
            cancelEdit();
            refreshCards();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Valor ou parcelas inválidos.", ButtonType.OK).showAndWait();
        }
    }

    private void cancelEdit() {
        editingId = null;
        descField.clear(); installmentField.clear(); installmentsField.clear();
        paymentCombo.setValue(null); firstChargeDatePicker.setValue(LocalDate.now());
        saveBtn.setText("Salvar");
        cancelBtn.setVisible(false);
        formPane.setText("Nova Dívida / Parcelamento");
        applyFormPaneStyle();
        formPane.setExpanded(false);
    }

    private void applyFormPaneStyle() {
        javafx.scene.Node title = formPane.lookup(".title");
        if (title != null) title.setStyle("-fx-background-color: #313244;");
        javafx.scene.Node label = formPane.lookup(".title > .text");
        if (label == null) label = formPane.lookup(".title .label");
        if (label instanceof javafx.scene.control.Label l) l.setStyle("-fx-text-fill: #cdd6f4; -fx-font-weight: bold;");
        if (label instanceof javafx.scene.text.Text t) t.setStyle("-fx-fill: #cdd6f4; -fx-font-weight: bold;");
    }

    private void refreshCards() {
        debtCardsBox.getChildren().clear();
        List<Debt> debts = service.getAllDebts();

        if (debts.isEmpty()) {
            Label empty = new Label("Nenhuma dívida cadastrada.");
            empty.setStyle("-fx-opacity: 0.6;");
            debtCardsBox.getChildren().add(empty);
            return;
        }

        // Resumo geral
        double totalRemaining = debts.stream()
                .filter(d -> !d.isFinished())
                .mapToDouble(d -> d.getInstallmentAmount() * d.getRemainingInstallments())
                .sum();
        Label summary = new Label(String.format("Total restante em dívidas: R$ %,.2f", totalRemaining));
        summary.getStyleClass().add("text-expense");
        summary.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        debtCardsBox.getChildren().add(summary);

        // Card por dívida
        for (Debt d : debts) {
            debtCardsBox.getChildren().add(buildDebtCard(d));
        }
    }

    private VBox buildDebtCard(Debt d) {
        VBox card = new VBox(8);
        card.getStyleClass().add(d.isFinished() ? "card-neutral" : "card-expense");
        card.getStyleClass().add("card");
        card.setPadding(new Insets(12));

        // Linha 1: descrição + status + ações
        Label descLbl = new Label(d.getDescription());
        descLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label statusLbl = new Label(d.isFinished() ? "✅ Quitada" : "⏳ Ativa");
        statusLbl.getStyleClass().add(d.isFinished() ? "text-income" : "text-expense");

        Button editBtn = new Button("Editar");
        editBtn.getStyleClass().add("btn-edit");
        Button delBtn = new Button("Excluir");
        delBtn.getStyleClass().add("btn-delete");
        editBtn.setOnAction(e -> loadForEdit(d));
        delBtn.setOnAction(e -> {
            service.deleteDebt(d.getId());
            if (editingId != null && editingId.equals(d.getId())) cancelEdit();
            refreshCards();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(8, descLbl, statusLbl, spacer, editBtn, delBtn);
        header.setAlignment(Pos.CENTER_LEFT);

        // Linha 2: infos financeiras
        int remaining = d.getRemainingInstallments();
        double remainingAmount = d.getInstallmentAmount() * remaining;
        double paidAmount = d.getInstallmentAmount() * d.getPaidInstallments();

        Label infoLbl = new Label(String.format(
                "Parcela: R$ %,.2f   |   %d/%d pagas   |   Pago: R$ %,.2f   |   Restante: R$ %,.2f",
                d.getInstallmentAmount(), d.getPaidInstallments(), d.getTotalInstallments(),
                paidAmount, remainingAmount));
        infoLbl.setStyle("-fx-font-size: 12px;");

        // Linha 3: data e pagamento
        String nextCharge = calcNextCharge(d);
        Label dateLbl = new Label(String.format("1ª cobrança: %s   |   %s   |   %s",
                d.getStartDate(), d.getPaymentMethod(), nextCharge));
        dateLbl.setStyle("-fx-font-size: 11px; -fx-opacity: 0.75;");

        // Barra de progresso
        double progress = d.getTotalInstallments() > 0
                ? (double) d.getPaidInstallments() / d.getTotalInstallments() : 0;

        HBox progressBar = buildProgressBar(progress, d.getPaidInstallments(), d.getTotalInstallments());

        // Botão de marcar parcela paga (só se não quitada)
        HBox actions = new HBox(8);
        if (!d.isFinished()) {
            Button markPaidBtn = new Button("✔ Marcar parcela paga");
            markPaidBtn.getStyleClass().add("btn-primary");
            markPaidBtn.setOnAction(e -> {
                d.setPaidInstallments(d.getPaidInstallments() + 1);
                service.saveDebt(d);
                refreshCards();
            });

            if (d.getPaidInstallments() > 0) {
                Button undoBtn = new Button("↩ Desfazer");
                undoBtn.setOnAction(e -> {
                    d.setPaidInstallments(d.getPaidInstallments() - 1);
                    service.saveDebt(d);
                    refreshCards();
                });
                actions.getChildren().addAll(markPaidBtn, undoBtn);
            } else {
                actions.getChildren().add(markPaidBtn);
            }
        }

        card.getChildren().addAll(header, infoLbl, dateLbl, progressBar);
        if (!actions.getChildren().isEmpty()) card.getChildren().add(actions);

        return card;
    }

    private HBox buildProgressBar(double progress, int paid, int total) {
        HBox container = new HBox(8);
        container.setAlignment(Pos.CENTER_LEFT);

        StackPane barBg = new StackPane();
        barBg.setPrefHeight(12);
        barBg.setPrefWidth(400);
        barBg.setStyle("-fx-background-color: #45475a; -fx-background-radius: 6;");

        Region fill = new Region();
        fill.setPrefHeight(12);
        fill.setPrefWidth(Math.max(progress * 400, progress > 0 ? 8 : 0));
        fill.setStyle((progress >= 1.0 ? "-fx-background-color: #a6e3a1;" : "-fx-background-color: #89b4fa;")
                + " -fx-background-radius: 6;");
        fill.setMaxWidth(400);

        StackPane.setAlignment(fill, Pos.CENTER_LEFT);
        barBg.getChildren().add(fill);

        Label pctLbl = new Label(String.format("%.0f%%  (%d de %d parcelas)", progress * 100, paid, total));
        pctLbl.setStyle("-fx-font-size: 11px;");

        container.getChildren().addAll(barBg, pctLbl);
        return container;
    }

    private String calcNextCharge(Debt d) {
        if (d.isFinished()) return "Quitada";
        YearMonth firstMonth = YearMonth.from(d.getStartDate());
        YearMonth nextMonth = firstMonth.plusMonths(d.getPaidInstallments());
        String monthName = nextMonth.getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt", "BR"));
        return "Próxima: " + monthName + "/" + nextMonth.getYear();
    }

    public void refresh() { refreshCards(); }

    public VBox getView() { return view; }
}
