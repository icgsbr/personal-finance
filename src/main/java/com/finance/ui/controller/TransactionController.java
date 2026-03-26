package com.finance.ui.controller;

import com.finance.model.EntryType;
import com.finance.model.PaymentMethod;
import com.finance.model.Transaction;
import com.finance.service.FinanceService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.YearMonth;

public class TransactionController {

    private final FinanceService service;
    private final VBox view = new VBox(12);
    private TableView<Transaction> table;
    private TitledPane formPane;
    private YearMonth currentMonth = YearMonth.now();

    private TextField descField, amountField, categoryField;
    private ComboBox<EntryType> typeCombo;
    private ComboBox<PaymentMethod> paymentCombo;
    private DatePicker datePicker;
    private CheckBox investmentCheck;
    private Button saveBtn, cancelBtn;
    private Long editingId = null;

    public TransactionController(FinanceService service) {
        this.service = service;
        view.setPadding(new Insets(16));
        buildView();
    }

    private void buildView() {
        Label title = new Label("Lançamentos");
        title.getStyleClass().add("title");

        formPane = new TitledPane("Novo Lançamento", buildForm());
        formPane.setExpanded(false);
        formPane.skinProperty().addListener((obs, o, n) -> applyFormPaneStyle());

        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        view.getChildren().addAll(title, buildMonthNav(), formPane, table);
        refreshTable();
    }

    private HBox buildMonthNav() {
        Button prev = new Button("◀");
        Button next = new Button("▶");
        Label monthLabel = new Label();
        monthLabel.getStyleClass().add("month-label");

        Runnable updateLabel = () -> monthLabel.setText(
                String.format("%02d/%d", currentMonth.getMonthValue(), currentMonth.getYear()));
        updateLabel.run();

        prev.setOnAction(e -> { currentMonth = currentMonth.minusMonths(1); updateLabel.run(); refreshTable(); });
        next.setOnAction(e -> { currentMonth = currentMonth.plusMonths(1); updateLabel.run(); refreshTable(); });

        return new HBox(8, prev, monthLabel, next);
    }

    private GridPane buildForm() {
        descField = new TextField(); descField.setPromptText("Descrição");
        amountField = new TextField(); amountField.setPromptText("Valor");
        categoryField = new TextField(); categoryField.setPromptText("Categoria (opcional)");
        typeCombo = new ComboBox<>(FXCollections.observableArrayList(EntryType.values()));
        typeCombo.setPromptText("Tipo");
        paymentCombo = new ComboBox<>(FXCollections.observableArrayList(PaymentMethod.values()));
        paymentCombo.setPromptText("Forma de pagamento");
        datePicker = new DatePicker(LocalDate.now());
        investmentCheck = new CheckBox("É investimento?");

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
        grid.add(new Label("Valor:"), 2, 0); grid.add(amountField, 3, 0);
        grid.add(new Label("Tipo:"), 0, 1); grid.add(typeCombo, 1, 1);
        grid.add(new Label("Pagamento:"), 2, 1); grid.add(paymentCombo, 3, 1);
        grid.add(new Label("Data:"), 0, 2); grid.add(datePicker, 1, 2);
        grid.add(new Label("Categoria:"), 2, 2); grid.add(categoryField, 3, 2);
        grid.add(investmentCheck, 1, 3);
        grid.add(new HBox(8, saveBtn, cancelBtn), 3, 3);

        return grid;
    }

    private void loadForEdit(Transaction t) {
        editingId = t.getId();
        descField.setText(t.getDescription());
        amountField.setText(String.valueOf(t.getAmount()));
        categoryField.setText(t.getCategory() != null ? t.getCategory() : "");
        typeCombo.setValue(t.getType());
        paymentCombo.setValue(t.getPaymentMethod());
        datePicker.setValue(t.getDate());
        investmentCheck.setSelected(t.isInvestment());
        saveBtn.setText("Atualizar");
        cancelBtn.setVisible(true);
        formPane.setText("Editando Lançamento");
        applyFormPaneStyle();
        formPane.setExpanded(true);
    }

    private void save() {
        try {
            String desc = descField.getText().trim();
            double amount = Double.parseDouble(amountField.getText().replace(",", "."));
            EntryType type = typeCombo.getValue();
            PaymentMethod payment = paymentCombo.getValue();
            LocalDate date = datePicker.getValue();

            if (desc.isEmpty() || type == null || payment == null || date == null) {
                new Alert(Alert.AlertType.WARNING, "Preencha todos os campos obrigatórios.", ButtonType.OK).showAndWait();
                return;
            }

            Transaction t = new Transaction(desc, amount, type, payment, date, categoryField.getText().trim());
            t.setInvestment(investmentCheck.isSelected());
            if (editingId != null) t.setId(editingId);
            service.saveTransaction(t);
            cancelEdit();
            refreshTable();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Valor inválido.", ButtonType.OK).showAndWait();
        }
    }

    private void cancelEdit() {
        editingId = null;
        descField.clear(); amountField.clear(); categoryField.clear();
        typeCombo.setValue(null); paymentCombo.setValue(null);
        datePicker.setValue(LocalDate.now()); investmentCheck.setSelected(false);
        saveBtn.setText("Salvar");
        cancelBtn.setVisible(false);
        formPane.setText("Novo Lançamento");
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
    private TableView<Transaction> buildTable() {
        TableView<Transaction> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Transaction, LocalDate> dateCol = new TableColumn<>("Data");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(90);

        TableColumn<Transaction, String> descCol = new TableColumn<>("Descrição");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<Transaction, String> catCol = new TableColumn<>("Categoria");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCol.setPrefWidth(110);

        TableColumn<Transaction, EntryType> typeCol = new TableColumn<>("Tipo");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(80);

        TableColumn<Transaction, PaymentMethod> payCol = new TableColumn<>("Pagamento");
        payCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        payCol.setPrefWidth(120);

        TableColumn<Transaction, Double> amtCol = new TableColumn<>("Valor");
        amtCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amtCol.setPrefWidth(100);
        amtCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                Transaction t = getTableView().getItems().get(getIndex());
                setText(String.format("R$ %,.2f", item));
                setStyle(t.getType() == EntryType.INCOME ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: #e74c3c;");
            }
        });

        TableColumn<Transaction, Void> actionsCol = new TableColumn<>("");
        actionsCol.setPrefWidth(100);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Editar");
            private final Button delBtn = new Button("Excluir");
            { editBtn.getStyleClass().add("btn-edit"); delBtn.getStyleClass().add("btn-delete"); }
            private final HBox box = new HBox(4, editBtn, delBtn);
            {
                editBtn.setOnAction(e -> loadForEdit(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e -> {
                    Transaction t = getTableView().getItems().get(getIndex());
                    service.deleteTransaction(t.getId());
                    if (editingId != null && editingId.equals(t.getId())) cancelEdit();
                    refreshTable();
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tv.getColumns().addAll(dateCol, descCol, catCol, typeCol, payCol, amtCol, actionsCol);
        return tv;
    }

    private void refreshTable() {
        table.setItems(FXCollections.observableArrayList(
                service.getTransactionsByMonth(currentMonth.getYear(), currentMonth.getMonthValue())));
    }

    public void refresh() { refreshTable(); }

    public VBox getView() { return view; }
}
