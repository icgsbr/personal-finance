package com.finance.ui.controller;

import com.finance.service.FinanceService;
import com.finance.service.MonthSummary;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.List;

public class ProjectionController {

    private final FinanceService service;
    private final VBox view = new VBox(12);
    private final VBox cardsContainer = new VBox(10);

    public ProjectionController(FinanceService service) {
        this.service = service;
        view.setPadding(new Insets(16));
        buildView();
    }

    private void buildView() {
        Label title = new Label("Projeções dos Próximos Meses");
        title.getStyleClass().add("title");

        HBox controls = new HBox(10);
        Spinner<Integer> monthsSpinner = new Spinner<>(1, 24, 6);
        monthsSpinner.setPrefWidth(80);
        Button refreshBtn = new Button("Atualizar");
        refreshBtn.getStyleClass().add("btn-primary");
        refreshBtn.setOnAction(e -> loadProjections(monthsSpinner.getValue()));
        controls.getChildren().addAll(new Label("Meses à frente:"), monthsSpinner, refreshBtn);

        ScrollPane scroll = new ScrollPane(cardsContainer);
        scroll.setFitToWidth(true);
        VBox.setVgrow(scroll, Priority.ALWAYS);

        view.getChildren().addAll(title, controls, scroll);
        loadProjections(6);
    }

    public void refresh() {
        loadProjections(6);
    }

    private void loadProjections(int months) {
        cardsContainer.getChildren().clear();
        List<MonthSummary> projections = service.getProjections(months);

        for (MonthSummary s : projections) {
            cardsContainer.getChildren().add(buildMonthCard(s));
        }
    }

    private HBox buildMonthCard(MonthSummary s) {
        HBox card = new HBox(20);
        card.getStyleClass().add("projection-card");
        card.setPadding(new Insets(12));
        card.setAlignment(Pos.CENTER_LEFT);

        Label monthLbl = new Label(s.getMonthLabel());
        monthLbl.getStyleClass().add("projection-month");
        monthLbl.setPrefWidth(70);

        VBox details = new VBox(4);
        details.getChildren().addAll(
                infoRow("💰 Entradas reais:", fmt(s.totalIncome())),
                infoRow("💸 Saídas reais:", fmt(s.totalExpense())),
                infoRow("🔄 Renda recorrente:", fmt(s.recurringIncome())),
                infoRow("🔁 Gasto recorrente:", fmt(s.recurringExpense())),
                infoRow("💳 Parcelas:", fmt(s.debtInstallments()))
        );

        VBox balances = new VBox(4);
        Label realBalance = new Label("Balanço Real: " + fmt(s.balance()));
        realBalance.getStyleClass().add(s.balance() >= 0 ? "text-income" : "text-expense");

        Label projBalance = new Label("Projeção: " + fmt(s.projectedBalance()));
        projBalance.getStyleClass().add(s.projectedBalance() >= 0 ? "text-income" : "text-expense");

        Label debtCount = new Label("Dívidas ativas: " + s.activeDebts().size());
        debtCount.getStyleClass().add("text-neutral");

        balances.getChildren().addAll(realBalance, projBalance, debtCount);

        card.getChildren().addAll(monthLbl, details, balances);
        return card;
    }

    private HBox infoRow(String label, String value) {
        Label lbl = new Label(label);
        lbl.setPrefWidth(160);
        Label val = new Label(value);
        HBox row = new HBox(8, lbl, val);
        return row;
    }

    private String fmt(double value) {
        return String.format("R$ %,.2f", value);
    }

    public VBox getView() { return view; }
}
