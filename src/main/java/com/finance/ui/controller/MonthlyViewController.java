package com.finance.ui.controller;

import com.finance.model.EntryType;
import com.finance.model.Transaction;
import com.finance.service.FinanceService;
import com.finance.service.MonthSummary;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;

public class MonthlyViewController {

    private final FinanceService service;
    private final VBox view = new VBox(0);
    private YearMonth selectedMonth = YearMonth.now();

    private HBox timeline;
    private VBox contentArea;

    public MonthlyViewController(FinanceService service) {
        this.service = service;
        view.setPadding(new Insets(16));
        buildView();
    }

    private void buildView() {
        // Cabeçalho com navegação
        HBox header = buildHeader();

        // Timeline de meses
        timeline = new HBox(6);
        timeline.setPadding(new Insets(10, 0, 14, 0));
        buildTimeline();

        // Área de conteúdo do mês selecionado
        contentArea = new VBox(14);

        ScrollPane scroll = new ScrollPane(contentArea);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(header, timeline, scroll);
        loadMonth();
    }

    private HBox buildHeader() {
        Button prev = new Button("◀");
        Button next = new Button("▶");
        Button today = new Button("Hoje");
        today.getStyleClass().add("btn-primary");

        Label monthLabel = new Label();
        monthLabel.getStyleClass().add("title");
        monthLabel.setMinWidth(260);
        monthLabel.setAlignment(Pos.CENTER);

        Runnable updateLabel = () -> {
            String name = selectedMonth.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
            monthLabel.setText(name + " " + selectedMonth.getYear());
        };
        updateLabel.run();

        prev.setOnAction(e -> { selectedMonth = selectedMonth.minusMonths(1); updateLabel.run(); buildTimeline(); loadMonth(); });
        next.setOnAction(e -> { selectedMonth = selectedMonth.plusMonths(1); updateLabel.run(); buildTimeline(); loadMonth(); });
        today.setOnAction(e -> { selectedMonth = YearMonth.now(); updateLabel.run(); buildTimeline(); loadMonth(); });

        HBox box = new HBox(10, prev, monthLabel, next, today);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void buildTimeline() {
        timeline.getChildren().clear();
        YearMonth now = YearMonth.now();
        // Mostra 3 meses antes, atual e 4 depois centrado no selecionado
        YearMonth start = selectedMonth.minusMonths(3);
        for (int i = 0; i < 8; i++) {
            YearMonth m = start.plusMonths(i);
            Button btn = buildTimelineBtn(m, now);
            timeline.getChildren().add(btn);
        }
    }

    private Button buildTimelineBtn(YearMonth m, YearMonth now) {
        String rawName = m.getMonth().getDisplayName(TextStyle.SHORT, new Locale("pt", "BR"));
        String shortName = Character.toUpperCase(rawName.charAt(0)) + rawName.substring(1).replace(".", "");
        Button btn = new Button(shortName + "\n" + m.getYear());
        btn.setStyle("-fx-font-size: 11px; -fx-text-alignment: center;");
        btn.setPrefWidth(72);
        btn.setPrefHeight(44);

        if (m.equals(selectedMonth)) {
            btn.getStyleClass().add("btn-primary");
        } else if (m.isBefore(now)) {
            btn.setStyle(btn.getStyle() + "-fx-opacity: 0.7;");
        }

        btn.setOnAction(e -> {
            selectedMonth = m;
            String rawMonthName = m.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
            String finalName = Character.toUpperCase(rawMonthName.charAt(0)) + rawMonthName.substring(1);
            if (!view.getChildren().isEmpty() && view.getChildren().get(0) instanceof HBox header) {
                header.getChildren().stream()
                        .filter(n -> n instanceof Label)
                        .map(n -> (Label) n)
                        .findFirst()
                        .ifPresent(lbl -> lbl.setText(finalName + " " + m.getYear()));
            }
            buildTimeline();
            loadMonth();
        });
        return btn;
    }

    private void loadMonth() {
        contentArea.getChildren().clear();
        YearMonth now = YearMonth.now();
        MonthSummary summary = service.getMonthSummary(selectedMonth.getYear(), selectedMonth.getMonthValue());

        boolean isPast = selectedMonth.isBefore(now);
        boolean isFuture = selectedMonth.isAfter(now);

        // Badge de contexto
        Label badge = new Label(isPast ? "📅 Mês passado" : isFuture ? "🔮 Projeção futura" : "📍 Mês atual");
        badge.getStyleClass().add(isPast ? "text-neutral" : isFuture ? "text-invest" : "text-income");
        badge.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        // Cards de resumo
        GridPane cards = buildCards(summary, isFuture);

        // Barra de saúde financeira
        VBox healthBar = buildHealthBar(summary);

        // Dívidas ativas no mês
        VBox debtSection = buildDebtSection(summary);

        // Transações do mês (só para passado/atual)
        VBox txSection = buildTransactionSection(summary, isFuture);

        contentArea.getChildren().addAll(badge, cards, healthBar);
        if (!summary.activeDebts().isEmpty()) contentArea.getChildren().add(debtSection);
        contentArea.getChildren().add(txSection);
    }

    private GridPane buildCards(MonthSummary s, boolean isFuture) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        if (isFuture) {
            // Mês futuro: mostra projeção baseada em recorrentes
            grid.add(card("🔄 Entrada Recorrente", fmt(s.recurringIncome()), "card-income"), 0, 0);
            grid.add(card("🔁 Saída Recorrente", fmt(s.recurringExpense()), "card-expense"), 1, 0);
            grid.add(card("💳 Parcelas Previstas", fmt(s.debtInstallments()), "card-neutral"), 2, 0);
            grid.add(card("📈 Investimentos Prev.", fmt(s.investmentAmount()), "card-invest"), 0, 1);
            double proj = s.recurringIncome() - s.recurringExpense() - s.debtInstallments();
            grid.add(card("🎯 Sobra Estimada", fmt(proj), proj >= 0 ? "card-income" : "card-expense"), 1, 1);
            grid.add(card("💳 Dívidas Ativas", String.valueOf(s.activeDebts().size()), "card-neutral"), 2, 1);
        } else {
            // Mês passado ou atual: mostra real + projeção
            grid.add(card("💰 Entradas Reais", fmt(s.totalIncome()), "card-income"), 0, 0);
            grid.add(card("💸 Saídas Reais", fmt(s.totalExpense()), "card-expense"), 1, 0);
            grid.add(card("📊 Balanço Real", fmt(s.balance()), s.balance() >= 0 ? "card-income" : "card-expense"), 2, 0);
            grid.add(card("🔄 Renda Recorrente", fmt(s.recurringIncome()), "card-neutral"), 0, 1);
            grid.add(card("🔁 Gasto Recorrente", fmt(s.recurringExpense()), "card-neutral"), 1, 1);
            grid.add(card("💳 Parcelas", fmt(s.debtInstallments()), "card-neutral"), 2, 1);
            grid.add(card("📈 Investimentos", fmt(s.investmentAmount()), "card-invest"), 0, 2);
            double proj = s.recurringIncome() - s.recurringExpense() - s.debtInstallments();
            grid.add(card("🎯 Sobra Recorrente", fmt(proj), proj >= 0 ? "card-income" : "card-expense"), 1, 2);
            grid.add(card("💼 Total Investido", fmt(service.getTotalInvested()), "card-invest"), 2, 2);
        }

        return grid;
    }

    private VBox buildHealthBar(MonthSummary s) {
        double income = s.recurringIncome() > 0 ? s.recurringIncome() : 1;
        double expenseRatio = Math.min((s.recurringExpense() + s.debtInstallments()) / income, 1.0);
        double investRatio = Math.min(s.investmentAmount() / income, 1.0 - expenseRatio);
        double freeRatio = Math.max(1.0 - expenseRatio - investRatio, 0);

        Label title = new Label("Distribuição da Renda Recorrente");
        title.getStyleClass().add("section-title");

        HBox bar = new HBox();
        bar.setPrefHeight(18);
        bar.setMaxWidth(Double.MAX_VALUE);

        Region expenseBar = new Region();
        expenseBar.setPrefWidth(expenseRatio * 700);
        expenseBar.setStyle("-fx-background-color: #f38ba8; -fx-background-radius: 4 0 0 4;");

        Region investBar = new Region();
        investBar.setPrefWidth(investRatio * 700);
        investBar.setStyle("-fx-background-color: #f9e2af;");

        Region freeBar = new Region();
        freeBar.setPrefWidth(freeRatio * 700);
        freeBar.setStyle("-fx-background-color: #a6e3a1; -fx-background-radius: 0 4 4 0;");

        bar.getChildren().addAll(expenseBar, investBar, freeBar);

        HBox legend = new HBox(16);
        legend.getChildren().addAll(
                legendItem("🔴 Gastos/Parcelas", String.format("%.0f%%", expenseRatio * 100), "#f38ba8"),
                legendItem("🟡 Investimentos", String.format("%.0f%%", investRatio * 100), "#f9e2af"),
                legendItem("🟢 Livre", String.format("%.0f%%", freeRatio * 100), "#a6e3a1")
        );

        VBox box = new VBox(6, title, bar, legend);
        box.setPadding(new Insets(4, 0, 4, 0));
        return box;
    }

    private HBox legendItem(String label, String pct, String color) {
        Label lbl = new Label(label + " " + pct);
        lbl.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        return new HBox(lbl);
    }

    private VBox buildDebtSection(MonthSummary s) {
        Label title = new Label("💳 Dívidas Ativas neste Mês (" + s.activeDebts().size() + ")");
        title.getStyleClass().add("section-title");

        VBox box = new VBox(4, title);
        s.activeDebts().forEach(d -> {
            int currentInstallment = (int) java.time.temporal.ChronoUnit.MONTHS.between(
                    d.getStartDate().withDayOfMonth(1),
                    LocalDate.of(selectedMonth.getYear(), selectedMonth.getMonthValue(), 1)) + 1;
            Label lbl = new Label(String.format("• %s — parcela %d/%d — %s",
                    d.getDescription(), currentInstallment, d.getTotalInstallments(), fmt(d.getInstallmentAmount())));
            lbl.getStyleClass().add("text-neutral");
            box.getChildren().add(lbl);
        });
        return box;
    }

    @SuppressWarnings("unchecked")
    private VBox buildTransactionSection(MonthSummary s, boolean isFuture) {
        Label title = new Label(isFuture ? "📋 Sem lançamentos (mês futuro)" : "📋 Lançamentos do Mês");
        title.getStyleClass().add("section-title");

        if (isFuture || s.transactions().isEmpty()) {
            Label empty = new Label(isFuture ? "Os lançamentos aparecerão quando o mês chegar." : "Nenhum lançamento registrado.");
            empty.setStyle("-fx-opacity: 0.6;");
            return new VBox(6, title, empty);
        }

        TableView<Transaction> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefHeight(200);
        table.setItems(FXCollections.observableArrayList(s.transactions()));

        TableColumn<Transaction, LocalDate> dateCol = new TableColumn<>("Data");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(90);

        TableColumn<Transaction, String> descCol = new TableColumn<>("Descrição");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        TableColumn<Transaction, String> catCol = new TableColumn<>("Categoria");
        catCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        catCol.setPrefWidth(110);

        TableColumn<Transaction, String> payCol = new TableColumn<>("Pagamento");
        payCol.setCellValueFactory(new PropertyValueFactory<>("paymentMethod"));
        payCol.setPrefWidth(120);

        TableColumn<Transaction, Double> amtCol = new TableColumn<>("Valor");
        amtCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amtCol.setPrefWidth(110);
        amtCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                Transaction t = getTableView().getItems().get(getIndex());
                setText(fmt(item));
                setStyle(t.getType() == EntryType.INCOME ? "-fx-text-fill: #a6e3a1;" : "-fx-text-fill: #f38ba8;");
            }
        });

        table.getColumns().addAll(dateCol, descCol, catCol, payCol, amtCol);

        // Totais
        double totalIn = s.transactions().stream().filter(t -> t.getType() == EntryType.INCOME).mapToDouble(Transaction::getAmount).sum();
        double totalOut = s.transactions().stream().filter(t -> t.getType() == EntryType.EXPENSE).mapToDouble(Transaction::getAmount).sum();
        Label totals = new Label(String.format("Entradas: %s   |   Saídas: %s   |   Saldo: %s",
                fmt(totalIn), fmt(totalOut), fmt(totalIn - totalOut)));
        totals.setStyle("-fx-font-size: 12px;");
        totals.getStyleClass().add((totalIn - totalOut) >= 0 ? "text-income" : "text-expense");

        ScrollPane scroll = new ScrollPane(table);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(220);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        return new VBox(6, title, totals, scroll);
    }

    private VBox card(String label, String value, String styleClass) {
        Label lbl = new Label(label);
        lbl.getStyleClass().add("card-label");
        Label val = new Label(value);
        val.getStyleClass().add("card-value");
        VBox box = new VBox(4, lbl, val);
        box.getStyleClass().addAll("card", styleClass);
        box.setPadding(new Insets(12));
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(220);
        return box;
    }

    private String fmt(double value) {
        return String.format("R$ %,.2f", value);
    }

    public void refresh() {
        loadMonth();
        buildTimeline();
    }

    public VBox getView() { return view; }
}
