package com.finance.ui.controller;

import com.finance.model.EntryType;
import com.finance.model.RecurringEntry;
import com.finance.service.FinanceService;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

public class RecurringController {

    private final FinanceService service;
    private final VBox view = new VBox(12);
    private TableView<RecurringEntry> table;
    private TitledPane formPane;

    private TextField descField, amountField, dayField;
    private ComboBox<EntryType> typeCombo;
    private Button saveBtn, cancelBtn;
    private Long editingId = null;

    public RecurringController(FinanceService service) {
        this.service = service;
        view.setPadding(new Insets(16));
        buildView();
    }

    private void buildView() {
        Label title = new Label("Entradas e Saídas Recorrentes");
        title.getStyleClass().add("title");

        formPane = new TitledPane("Nova Recorrente", buildForm());
        formPane.setExpanded(false);
        formPane.skinProperty().addListener((obs, o, n) -> applyFormPaneStyle());

        HBox totals = buildTotals();
        table = buildTable();
        VBox.setVgrow(table, Priority.ALWAYS);

        view.getChildren().addAll(title, formPane, totals, table);
        refreshTable();
    }

    private HBox buildTotals() {
        HBox box = new HBox(24);
        box.setPadding(new Insets(4, 0, 4, 0));
        refreshTotals(box);
        return box;
    }

    private void refreshTotals(HBox box) {
        box.getChildren().clear();
        double income = service.getMonthlyRecurringIncomeTotal();
        double expense = service.getMonthlyRecurringExpenseTotal();
        Label inc = new Label("Total Entradas: " + String.format("R$ %,.2f", income));
        inc.getStyleClass().add("text-income");
        Label exp = new Label("Total Saídas: " + String.format("R$ %,.2f", expense));
        exp.getStyleClass().add("text-expense");
        Label net = new Label("Saldo: " + String.format("R$ %,.2f", income - expense));
        net.getStyleClass().add((income - expense) >= 0 ? "text-income" : "text-expense");
        box.getChildren().addAll(inc, exp, net);
    }

    private GridPane buildForm() {
        descField = new TextField(); descField.setPromptText("Descrição");
        amountField = new TextField(); amountField.setPromptText("Valor");
        dayField = new TextField(); dayField.setPromptText("Dia do mês (ex: 5)");
        typeCombo = new ComboBox<>(FXCollections.observableArrayList(EntryType.values()));
        typeCombo.setPromptText("Tipo");

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
        grid.add(new Label("Dia do mês:"), 2, 1); grid.add(dayField, 3, 1);
        grid.add(new HBox(8, saveBtn, cancelBtn), 3, 2);

        return grid;
    }

    private void loadForEdit(RecurringEntry r) {
        editingId = r.getId();
        descField.setText(r.getDescription());
        amountField.setText(String.valueOf(r.getAmount()));
        dayField.setText(r.getDayOfMonth() != null ? String.valueOf(r.getDayOfMonth()) : "");
        typeCombo.setValue(r.getType());
        saveBtn.setText("Atualizar");
        cancelBtn.setVisible(true);
        formPane.setText("Editando Recorrente");
        applyFormPaneStyle();
        formPane.setExpanded(true);
    }

    private void save() {
        try {
            String desc = descField.getText().trim();
            double amount = Double.parseDouble(amountField.getText().replace(",", "."));
            EntryType type = typeCombo.getValue();
            Integer day = dayField.getText().isBlank() ? null : Integer.parseInt(dayField.getText().trim());

            if (desc.isEmpty() || type == null) {
                new Alert(Alert.AlertType.WARNING, "Preencha descrição e tipo.", ButtonType.OK).showAndWait();
                return;
            }

            RecurringEntry entry = new RecurringEntry(desc, amount, type, day);
            if (editingId != null) entry.setId(editingId);
            service.saveRecurring(entry);
            cancelEdit();
            refreshTable();
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.WARNING, "Valor ou dia inválido.", ButtonType.OK).showAndWait();
        }
    }

    private void cancelEdit() {
        editingId = null;
        descField.clear(); amountField.clear(); dayField.clear();
        typeCombo.setValue(null);
        saveBtn.setText("Salvar");
        cancelBtn.setVisible(false);
        formPane.setText("Nova Recorrente");
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
    private TableView<RecurringEntry> buildTable() {
        TableView<RecurringEntry> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<RecurringEntry, String> descCol = new TableColumn<>("Descrição");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<RecurringEntry, EntryType> typeCol = new TableColumn<>("Tipo");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(90);

        TableColumn<RecurringEntry, Double> amtCol = new TableColumn<>("Valor");
        amtCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amtCol.setPrefWidth(110);
        amtCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                RecurringEntry r = getTableView().getItems().get(getIndex());
                setText(String.format("R$ %,.2f", item));
                setStyle(r.getType() == EntryType.INCOME ? "-fx-text-fill: #27ae60;" : "-fx-text-fill: #e74c3c;");
            }
        });

        TableColumn<RecurringEntry, Integer> dayCol = new TableColumn<>("Dia");
        dayCol.setCellValueFactory(new PropertyValueFactory<>("dayOfMonth"));
        dayCol.setPrefWidth(60);

        TableColumn<RecurringEntry, Boolean> activeCol = new TableColumn<>("Ativo");
        activeCol.setCellValueFactory(new PropertyValueFactory<>("active"));
        activeCol.setPrefWidth(60);

        TableColumn<RecurringEntry, Void> actionsCol = new TableColumn<>("");
        actionsCol.setPrefWidth(100);
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Editar");
            private final Button delBtn = new Button("Excluir");
            { editBtn.getStyleClass().add("btn-edit"); delBtn.getStyleClass().add("btn-delete"); }
            private final HBox box = new HBox(4, editBtn, delBtn);
            {
                editBtn.setOnAction(e -> loadForEdit(getTableView().getItems().get(getIndex())));
                delBtn.setOnAction(e -> {
                    RecurringEntry r = getTableView().getItems().get(getIndex());
                    service.deleteRecurring(r.getId());
                    if (editingId != null && editingId.equals(r.getId())) cancelEdit();
                    refreshTable();
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tv.getColumns().addAll(descCol, typeCol, amtCol, dayCol, activeCol, actionsCol);
        return tv;
    }

    private void refreshTable() {
        table.setItems(FXCollections.observableArrayList(service.getAllRecurring()));
        if (view.getChildren().size() > 2) {
            HBox totals = (HBox) view.getChildren().get(2);
            refreshTotals(totals);
        }
    }

    public void refresh() { refreshTable(); }

    public VBox getView() { return view; }
}
