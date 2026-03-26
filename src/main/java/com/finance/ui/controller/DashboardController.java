package com.finance.ui.controller;

import com.finance.model.Debt;
import com.finance.model.EntryType;
import com.finance.model.Investment;
import com.finance.model.RecurringEntry;
import com.finance.model.Transaction;
import com.finance.service.FinanceService;
import com.finance.service.MonthSummary;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class DashboardController {

    private final FinanceService service;
    private final VBox root = new VBox(0);
    private final VBox content = new VBox(20);
    private YearMonth selectedMonth = YearMonth.now();

    public DashboardController(FinanceService service) {
        this.service = service;

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        content.setPadding(new Insets(20));

        root.getChildren().add(scroll);
        VBox.setVgrow(root, Priority.ALWAYS);

        refresh();
    }

    public void refresh() {
        content.getChildren().clear();
        MonthSummary s = service.getMonthSummary(selectedMonth.getYear(), selectedMonth.getMonthValue());

        content.getChildren().addAll(
                buildHeader(),
                buildSummaryCards(s),
                buildHealthBar(s),
                buildIncomeSection(s),
                buildExpenseSection(s),
                buildRecurringSection(),
                buildDebtSection(s),
                buildInvestmentSection()
        );
    }

    // ── Cabeçalho com navegação de mês ──────────────────────────────────────

    private HBox buildHeader() {
        Button prev = new Button("◀");
        Button next = new Button("▶");
        Button today = new Button("Hoje");
        today.getStyleClass().add("btn-primary");

        Label monthLabel = new Label(formatMonthLabel());
        monthLabel.getStyleClass().add("title");
        monthLabel.setMinWidth(280);
        monthLabel.setAlignment(Pos.CENTER);

        prev.setOnAction(e -> { selectedMonth = selectedMonth.minusMonths(1); monthLabel.setText(formatMonthLabel()); refresh(); });
        next.setOnAction(e -> { selectedMonth = selectedMonth.plusMonths(1); monthLabel.setText(formatMonthLabel()); refresh(); });
        today.setOnAction(e -> { selectedMonth = YearMonth.now(); monthLabel.setText(formatMonthLabel()); refresh(); });

        HBox box = new HBox(10, prev, monthLabel, next, today);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private String formatMonthLabel() {
        String name = selectedMonth.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
        return Character.toUpperCase(name.charAt(0)) + name.substring(1) + " " + selectedMonth.getYear();
    }

    // ── Cards de resumo ──────────────────────────────────────────────────────

    private GridPane buildSummaryCards(MonthSummary s) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        double proj = s.recurringIncome() - s.recurringExpense() - s.debtInstallments();

        grid.add(card("💰 Entradas Reais",      fmt(s.totalIncome()),       "card-income"),  0, 0);
        grid.add(card("💸 Saídas Reais",        fmt(s.totalExpense()),      "card-expense"), 1, 0);
        grid.add(card("📊 Balanço Real",        fmt(s.balance()),           s.balance() >= 0 ? "card-income" : "card-expense"), 2, 0);
        grid.add(card("🔄 Renda Recorrente",    fmt(s.recurringIncome()),   "card-neutral"), 0, 1);
        grid.add(card("🔁 Gasto Recorrente",    fmt(s.recurringExpense()),  "card-neutral"), 1, 1);
        grid.add(card("💳 Parcelas do Mês",     fmt(s.debtInstallments()),  "card-neutral"), 2, 1);
        grid.add(card("📈 Investimentos",       fmt(s.investmentAmount()),  "card-invest"),  0, 2);
        grid.add(card("🎯 Sobra Recorrente",    fmt(proj),                  proj >= 0 ? "card-income" : "card-expense"), 1, 2);
        grid.add(card("💼 Total Investido",     fmt(service.getTotalInvested()), "card-invest"), 2, 2);

        return grid;
    }

    // ── Barra de saúde financeira ────────────────────────────────────────────

    private VBox buildHealthBar(MonthSummary s) {
        double income = s.recurringIncome() > 0 ? s.recurringIncome() : 1;
        double expRatio  = Math.min((s.recurringExpense() + s.debtInstallments()) / income, 1.0);
        double invRatio  = Math.min(s.investmentAmount() / income, Math.max(1.0 - expRatio, 0));
        double freeRatio = Math.max(1.0 - expRatio - invRatio, 0);

        Label title = new Label("Distribuição da Renda Recorrente");
        title.getStyleClass().add("section-title");

        Region expBar  = barRegion(expRatio,  "#f38ba8", "4 0 0 4");
        Region invBar  = barRegion(invRatio,  "#fab387", "0 0 0 0");
        Region freeBar = barRegion(freeRatio, "#a6e3a1", "0 4 4 0");

        HBox bar = new HBox(expBar, invBar, freeBar);
        bar.setPrefHeight(18);
        bar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(expBar,  Priority.SOMETIMES);
        HBox.setHgrow(invBar,  Priority.SOMETIMES);
        HBox.setHgrow(freeBar, Priority.SOMETIMES);

        HBox legend = new HBox(20,
                legendDot("#f38ba8", String.format("Gastos/Parcelas %.0f%%", expRatio * 100)),
                legendDot("#fab387", String.format("Investimentos %.0f%%",   invRatio * 100)),
                legendDot("#a6e3a1", String.format("Livre %.0f%%",           freeRatio * 100)));

        VBox box = new VBox(6, title, bar, legend);
        box.setPadding(new Insets(0, 0, 4, 0));
        return box;
    }

    private Region barRegion(double ratio, String color, String radius) {
        Region r = new Region();
        r.setPrefHeight(18);
        r.setPrefWidth(ratio * 600);
        r.setMinWidth(ratio > 0 ? 4 : 0);
        r.setStyle("-fx-background-color: " + color + "; -fx-background-radius: " + radius + ";");
        return r;
    }

    private HBox legendDot(String color, String text) {
        Region dot = new Region();
        dot.setPrefSize(10, 10);
        dot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 5;");
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11px;");
        HBox box = new HBox(6, dot, lbl);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    // ── Seção de Entradas ────────────────────────────────────────────────────

    private VBox buildIncomeSection(MonthSummary s) {
        List<Transaction> incomes = s.transactions().stream()
                .filter(t -> t.getType() == EntryType.INCOME)
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .toList();

        double total = incomes.stream().mapToDouble(Transaction::getAmount).sum();

        Label title = new Label("💰 Entradas do Mês — " + fmt(total));
        title.getStyleClass().add("section-title");

        if (incomes.isEmpty()) {
            Label empty = new Label("Nenhuma entrada registrada.");
            empty.setStyle("-fx-opacity: 0.6;");
            return new VBox(6, title, empty);
        }

        // Agrupamento por categoria
        VBox byCategory = buildCategoryBreakdown(incomes, total, "#a6e3a1");

        // Tabela detalhada
        TableView<Transaction> table = buildTransactionTable(incomes, EntryType.INCOME);

        TitledPane detail = new TitledPane("Ver lançamentos detalhados (" + incomes.size() + ")", table);
        detail.setExpanded(false);
        detail.skinProperty().addListener((obs, o, n) -> styleTitledPane(detail));

        return new VBox(10, title, byCategory, detail);
    }

    // ── Seção de Saídas ──────────────────────────────────────────────────────

    private VBox buildExpenseSection(MonthSummary s) {
        List<Transaction> expenses = s.transactions().stream()
                .filter(t -> t.getType() == EntryType.EXPENSE)
                .sorted((a, b) -> b.getDate().compareTo(a.getDate()))
                .toList();

        double total = expenses.stream().mapToDouble(Transaction::getAmount).sum();

        Label title = new Label("💸 Saídas do Mês — " + fmt(total));
        title.getStyleClass().add("section-title");

        if (expenses.isEmpty()) {
            Label empty = new Label("Nenhuma saída registrada.");
            empty.setStyle("-fx-opacity: 0.6;");
            return new VBox(6, title, empty);
        }

        VBox byCategory = buildCategoryBreakdown(expenses, total, "#f38ba8");

        TableView<Transaction> table = buildTransactionTable(expenses, EntryType.EXPENSE);

        TitledPane detail = new TitledPane("Ver lançamentos detalhados (" + expenses.size() + ")", table);
        detail.setExpanded(false);
        detail.skinProperty().addListener((obs, o, n) -> styleTitledPane(detail));

        return new VBox(10, title, byCategory, detail);
    }

    // ── Breakdown por categoria com barra proporcional ───────────────────────

    private VBox buildCategoryBreakdown(List<Transaction> txs, double total, String barColor) {
        Map<String, Double> byCategory = txs.stream().collect(
                Collectors.groupingBy(
                        t -> t.getCategory() != null && !t.getCategory().isBlank() ? t.getCategory() : "Sem categoria",
                        Collectors.summingDouble(Transaction::getAmount)));

        VBox box = new VBox(6);
        byCategory.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .forEach(e -> {
                    double ratio = total > 0 ? e.getValue() / total : 0;

                    Label catLabel = new Label(e.getKey());
                    catLabel.setMinWidth(160);
                    catLabel.setStyle("-fx-font-size: 12px;");

                    Region fill = new Region();
                    fill.setPrefHeight(14);
                    fill.setPrefWidth(Math.max(ratio * 340, 4));
                    fill.setStyle("-fx-background-color: " + barColor + "; -fx-background-radius: 3; -fx-opacity: 0.85;");

                    Label valLabel = new Label(String.format("%s  (%.0f%%)", fmt(e.getValue()), ratio * 100));
                    valLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

                    HBox row = new HBox(10, catLabel, fill, valLabel);
                    row.setAlignment(Pos.CENTER_LEFT);
                    box.getChildren().add(row);
                });
        return box;
    }

    // ── Tabela de transações ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private TableView<Transaction> buildTransactionTable(List<Transaction> txs, EntryType type) {
        TableView<Transaction> tv = new TableView<>(FXCollections.observableArrayList(txs));
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tv.setPrefHeight(Math.min(txs.size() * 36 + 36, 280));

        TableColumn<Transaction, LocalDate> dateCol = new TableColumn<>("Data");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(90);

        TableColumn<Transaction, String> descCol = new TableColumn<>("Descrição");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<Transaction, String> catCol = new TableColumn<>("Categoria");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCol.setPrefWidth(120);

        TableColumn<Transaction, String> payCol = new TableColumn<>("Pagamento");
        payCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        payCol.setPrefWidth(120);

        String color = type == EntryType.INCOME ? "#a6e3a1" : "#f38ba8";
        TableColumn<Transaction, Double> amtCol = new TableColumn<>("Valor");
        amtCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amtCol.setPrefWidth(110);
        amtCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(fmt(item));
                setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
            }
        });

        tv.getColumns().addAll(dateCol, descCol, catCol, payCol, amtCol);
        return tv;
    }

    // ── Seção de Recorrentes ─────────────────────────────────────────────────

    private VBox buildRecurringSection() {
        List<RecurringEntry> all = service.getAllRecurring().stream()
                .filter(RecurringEntry::isActive).toList();

        double totalInc = all.stream().filter(r -> r.getType() == EntryType.INCOME).mapToDouble(RecurringEntry::getAmount).sum();
        double totalExp = all.stream().filter(r -> r.getType() == EntryType.EXPENSE).mapToDouble(RecurringEntry::getAmount).sum();

        Label title = new Label("🔄 Recorrentes — Entradas: " + fmt(totalInc) + "  |  Saídas: " + fmt(totalExp));
        title.getStyleClass().add("section-title");

        VBox incBox = new VBox(4);
        VBox expBox = new VBox(4);

        Label incTitle = new Label("Entradas recorrentes");
        incTitle.getStyleClass().add("text-income");
        incTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        incBox.getChildren().add(incTitle);

        Label expTitle = new Label("Saídas recorrentes");
        expTitle.getStyleClass().add("text-expense");
        expTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        expBox.getChildren().add(expTitle);

        all.stream().filter(r -> r.getType() == EntryType.INCOME).forEach(r -> {
            Label lbl = new Label(String.format("• %s — %s%s",
                    r.getDescription(), fmt(r.getAmount()),
                    r.getDayOfMonth() != null ? "  (dia " + r.getDayOfMonth() + ")" : ""));
            lbl.setStyle("-fx-font-size: 12px;");
            incBox.getChildren().add(lbl);
        });

        all.stream().filter(r -> r.getType() == EntryType.EXPENSE).forEach(r -> {
            Label lbl = new Label(String.format("• %s — %s%s",
                    r.getDescription(), fmt(r.getAmount()),
                    r.getDayOfMonth() != null ? "  (dia " + r.getDayOfMonth() + ")" : ""));
            lbl.setStyle("-fx-font-size: 12px;");
            expBox.getChildren().add(lbl);
        });

        HBox columns = new HBox(40, incBox, expBox);

        TitledPane pane = new TitledPane("Ver detalhes", columns);
        pane.setExpanded(false);
        pane.skinProperty().addListener((obs, o, n) -> styleTitledPane(pane));

        return new VBox(8, title, pane);
    }

    // ── Seção de Dívidas ─────────────────────────────────────────────────────

    private VBox buildDebtSection(MonthSummary s) {
        List<Debt> active = s.activeDebts();
        double total = active.stream().mapToDouble(Debt::getInstallmentAmount).sum();

        Label title = new Label("💳 Dívidas Ativas neste Mês (" + active.size() + ") — " + fmt(total));
        title.getStyleClass().add("section-title");

        if (active.isEmpty()) {
            Label empty = new Label("Nenhuma dívida ativa neste mês.");
            empty.setStyle("-fx-opacity: 0.6;");
            return new VBox(6, title, empty);
        }

        VBox list = new VBox(6);
        active.forEach(d -> {
            int currentInstallment = (int) java.time.temporal.ChronoUnit.MONTHS.between(
                    d.getStartDate().withDayOfMonth(1),
                    LocalDate.of(selectedMonth.getYear(), selectedMonth.getMonthValue(), 1)) + 1;
            double remaining = d.getInstallmentAmount() * d.getRemainingInstallments();

            Label lbl = new Label(String.format("• %s   parcela %d/%d   %s/mês   restante total: %s   %s",
                    d.getDescription(), currentInstallment, d.getTotalInstallments(),
                    fmt(d.getInstallmentAmount()), fmt(remaining), d.getPaymentMethod()));
            lbl.setStyle("-fx-font-size: 12px;");
            list.getChildren().add(lbl);
        });

        TitledPane pane = new TitledPane("Ver detalhes", list);
        pane.setExpanded(false);
        pane.skinProperty().addListener((obs, o, n) -> styleTitledPane(pane));

        return new VBox(8, title, pane);
    }

    // ── Seção de Investimentos ───────────────────────────────────────────────

    private VBox buildInvestmentSection() {
        List<Investment> monthInv = service.getInvestmentsByMonth(selectedMonth.getYear(), selectedMonth.getMonthValue());
        double monthTotal = monthInv.stream().mapToDouble(Investment::getAmount).sum();
        double allTotal   = service.getTotalInvested();

        Label title = new Label(String.format("📈 Investimentos — Mês: %s  |  Total acumulado: %s", fmt(monthTotal), fmt(allTotal)));
        title.getStyleClass().add("section-title");

        if (monthInv.isEmpty()) {
            Label empty = new Label("Nenhum investimento registrado neste mês.");
            empty.setStyle("-fx-opacity: 0.6;");
            return new VBox(6, title, empty);
        }

        // Breakdown por tipo
        Map<String, Double> byType = monthInv.stream().collect(
                Collectors.groupingBy(Investment::getType, Collectors.summingDouble(Investment::getAmount)));

        VBox breakdown = new VBox(4);
        byType.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .forEach(e -> {
                    double ratio = monthTotal > 0 ? e.getValue() / monthTotal : 0;
                    Label lbl = new Label(String.format("• %s — %s  (%.0f%%)", e.getKey(), fmt(e.getValue()), ratio * 100));
                    lbl.setStyle("-fx-font-size: 12px;");
                    breakdown.getChildren().add(lbl);
                });

        TitledPane pane = new TitledPane("Ver detalhes (" + monthInv.size() + " lançamentos)", breakdown);
        pane.setExpanded(false);
        pane.skinProperty().addListener((obs, o, n) -> styleTitledPane(pane));

        return new VBox(8, title, pane);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private VBox card(String label, String value, String styleClass) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("card-label");
        Label val = new Label(value);
        val.getStyleClass().add("card-value");
        VBox box = new VBox(4, lbl, val);
        box.getStyleClass().addAll("card", styleClass);
        box.setPadding(new Insets(14));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(240);
        return box;
    }

    private void styleTitledPane(TitledPane pane) {
        javafx.scene.Node titleNode = pane.lookup(".title");
        if (titleNode != null) titleNode.setStyle("-fx-background-color: #313244;");
        javafx.scene.Node label = pane.lookup(".title > .text");
        if (label == null) label = pane.lookup(".title .label");
        if (label instanceof Label l) l.setStyle("-fx-text-fill: #cdd6f4; -fx-font-weight: bold;");
        if (label instanceof Text t) t.setStyle("-fx-fill: #cdd6f4; -fx-font-weight: bold;");
    }

    private String fmt(double value) {
        return String.format("R$ %,.2f", value);
    }

    public VBox getView() { return root; }
}
