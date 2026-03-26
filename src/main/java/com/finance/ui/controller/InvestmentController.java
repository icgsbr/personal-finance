package com.finance.ui.controller;

import com.finance.model.Investment;
import com.finance.model.PaymentMethod;
import com.finance.service.FinanceService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class InvestmentController {

    private final FinanceService service;
    private final VBox view = new VBox(12);
    private TableView<Investment> table;
    private TitledPane formPane;
    private final HBox summaryBox = new HBox(16);

    private TextField descField, typeField, amountField, notesField;
    private ComboBox<PaymentMethod> paymentCombo;
    private DatePicker datePicker;
    private Button saveBtn, cancelBtn;
    private Long editingId = null;

    public InvestmentController(FinanceService service) {
        this.service = service;
        view.setPadding(new Insets(16));
        buildView();
    }

    private void buildView() {
        Label title = new Label("Investimentos");
        title.getStyleClass().add("title");

        formPane = new TitledPane("Novo Investimento", buildForm());
        formPane.setExpanded(false);
        formPane.skinProperty().addListener((obs, o, n) -> applyFormPaneStyle());

        summaryBox.setPadding(new Insets(4, 0, 4, 0));

        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        view.getChildren().addAll(title, formPane, summaryBox, table);
        refresh();
    }

    private GridPane buildForm() {
        descField = new TextField(); descField.setPromptText("Descrição");
        typeField = new TextField(); typeField.setPromptText("Tipo (ex: Tesouro Direto, CDB, Ações)");
        amountField = new TextField(); amountField.setPromptText("Valor");
        notesField = new TextField(); notesField.setPromptText("Observações (opcional)");
        paymentCombo = new ComboBox<>(FXCollections.observableArrayList(PaymentMethod.values()));
        paymentCombo.setPromptText("Forma de pagamento");
        datePicker = new DatePicker(LocalDate.now());

        saveBtn = new Button("Salvar");
        saveBtn.getStyleClass().add("btn-primary");
        saveBtn.setOnAction(e -> save());

        cancelBtn = new Button("Cancelar");
        cancelBtn.setVisible(false);
        cancelBtn.setOnAction(e -> cancelEdit());

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(8);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Descrição:"), 0, 0); grid.add(descField, 1, 0);
        grid.add(new Label("Tipo:"), 2, 0); grid.add(typeField, 3, 0);
        grid.add(new Label("Valor:"), 0, 1); grid.add(amountField, 1, 1);
        grid.add(new Label("Pagamento:"), 2, 1); grid.add(paymentCombo, 3, 1);
        grid.add(new Label("Data:"), 0, 2); grid.add(datePicker, 1, 2);
        grid.add(new Label("Observações:"), 2, 2); grid.add(notesField, 3, 2);
        grid.add(new HBox(8, saveBtn, cancelBtn), 3, 3);

        return grid;
    }

    private void loadForEdit(Investment inv) {
        editingId = inv.getId();
        descField.setText(inv.getDescription());
        typeField.setText(inv.getType());
        amountField.setText(String.valueOf(inv.getAmount()));
        notesField.setText(inv.getNotes() != null ? inv.getNotes() : "");
        paymentCombo.setValue(inv.getPaymentMethod());
        datePicker.setValue(inv.getDate());
        saveBtn.setText("Atualizar");
        cancelBtn.setVisible(true);
        formPane.setText("Editando Investimento");
        applyFormPaneStyle();
        formPane.setExpanded(true);
    }

    private void save() {
        try {
            String desc = descField.getText().trim();
            String type = typeField.getText().trim();
            double amount = Double.parseDouble(amountField.getText().replace(",", "."));
            PaymentMethod payment = paymentCombo.getValue();
            LocalDate date = datePicker.getValue();

            if (desc.isEmpty() || type.isEmpty() || payment == null || date == null) {
                new Alert(Alert.AlertType.WARNING, "Preencha todos os campos obrigatórios.", ButtonType.OK).showAndWait();
                return;
            }

            Investment inv = new Investment(desc, type, amount, date, payment);
            inv.setNotes(notesField.getText().trim());
            if (editingId != null) inv.setId(editingId);
            service.saveInvestment(inv);
            cancelEdit();
            refresh();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Valor inválido.", ButtonType.OK).showAndWait();
        }
    }

    private void cancelEdit() {
        editingId = null;
        descField.clear(); typeField.clear(); amountField.clear(); notesField.clear();
        paymentCombo.setValue(null); datePicker.setValue(LocalDate.now());
        saveBtn.setText("Salvar");
        cancelBtn.setVisible(false);
        formPane.setText("Novo Investimento");
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

    @SuppressWarnings("unchecked")
    private TableView<Investment> buildTable() {
        TableView<Investment> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Investment, LocalDate> dateCol = new TableColumn<>("Data");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(90);

        TableColumn<Investment, String> descCol = new TableColumn<>("Descrição");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<Investment, String> typeCol = new TableColumn<>("Tipo");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(130);

        TableColumn<Investment, Double> amtCol = new TableColumn<>("Valor");
        amtCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amtCol.setPrefWidth(110);
        amtCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("R$ %,.2f", item));
                setStyle("-fx-text-fill: #2980b9;");
            }
        });

        TableColumn<Investment, PaymentMethod> payCol = new TableColumn<>("Pagamento");
        payCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        payCol.setPrefWidth(120);

        TableColumn<Investment, String> notesCol = new TableColumn<>("Observações");
        notesCol.setCellValueFactory(new PropertyValueFactory<>("notes"));

        TableColumn<Investment, Void> actionsCol = new TableColumn<>("");
        actionsCol.setPrefWidth(100);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Editar");
            private final Button delBtn = new Button("Excluir");
            { editBtn.getStyleClass().add("btn-edit"); delBtn.getStyleClass().add("btn-delete"); }
            private final HBox box = new HBox(4, editBtn, delBtn);
            {
                editBtn.setOnAction(e -> loadForEdit(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e -> {
                    Investment inv = getTableView().getItems().get(getIndex());
                    service.deleteInvestment(inv.getId());
                    if (editingId != null && editingId.equals(inv.getId())) cancelEdit();
                    refresh();
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tv.getColumns().addAll(dateCol, descCol, typeCol, amtCol, payCol, notesCol, actionsCol);
        return tv;
    }

    public void refresh() {
        List<Investment> all = service.getAllInvestments();
        table.setItems(FXCollections.observableArrayList(all));
        refreshSummary(all);
    }

    private void refreshSummary(List<Investment> all) {
        summaryBox.getChildren().clear();
        double total = all.stream().mapToDouble(Investment::getAmount).sum();
        Label totalLbl = new Label("Total Investido: " + String.format("R$ %,.2f", total));
        totalLbl.getStyleClass().add("text-invest");
        summaryBox.getChildren().add(totalLbl);

        Map<String, Double> byType = all.stream()
                .collect(Collectors.groupingBy(Investment::getType, Collectors.summingDouble(Investment::getAmount)));
        byType.forEach((type, amount) -> {
            Label lbl = new Label(type + ": " + String.format("R$ %,.2f", amount));
            lbl.getStyleClass().add("text-neutral");
            summaryBox.getChildren().add(lbl);
        });
    }

    public VBox getView() { return view; }
}
