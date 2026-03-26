package com.finance.ui.controller;

import com.finance.config.ThemePreference;
import com.finance.service.FinanceService;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public class MainController {

    private final BorderPane view = new BorderPane();
    private final FinanceService service = new FinanceService();
    private boolean darkMode;

    public MainController() {
        darkMode = ThemePreference.loadDarkMode();

        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        MonthlyViewController monthly = new MonthlyViewController(service);
        DashboardController dashboard = new DashboardController(service);
        TransactionController transactions = new TransactionController(service);
        RecurringController recurring = new RecurringController(service);
        DebtController debts = new DebtController(service);
        InvestmentController investments = new InvestmentController(service);
        BackupController backup = new BackupController(() -> {
            monthly.refresh();
            dashboard.refresh();
            transactions.refresh();
            recurring.refresh();
            debts.refresh();
            investments.refresh();
        });

        Tab tabDashboard    = new Tab("🏠 Dashboard",       dashboard.getView());
        Tab tabMonthly      = new Tab("📅 Visão Mensal",   monthly.getView());
        Tab tabTransactions = new Tab("💸 Lançamentos",    transactions.getView());
        Tab tabRecurring    = new Tab("🔄 Recorrentes",    recurring.getView());
        Tab tabDebts        = new Tab("💳 Dívidas",        debts.getView());
        Tab tabInvestments  = new Tab("📈 Investimentos",  investments.getView());
        Tab tabBackup       = new Tab("🗄 Backup",         backup.getView());

        tabPane.getTabs().addAll(tabDashboard, tabMonthly, tabTransactions, tabRecurring, tabDebts, tabInvestments, tabBackup);

        tabPane.getSelectionModel().selectedItemProperty().addListener((obs, old, newTab) -> {
            if (newTab == tabDashboard)    dashboard.refresh();
            if (newTab == tabMonthly)      monthly.refresh();
            if (newTab == tabInvestments)  investments.refresh();
        });

        Button themeBtn = new Button();
        themeBtn.getStyleClass().add("theme-toggle");
        applyTheme(themeBtn);

        themeBtn.setOnAction(e -> {
            darkMode = !darkMode;
            applyTheme(themeBtn);
            ThemePreference.save(darkMode);
        });

        HBox topBar = new HBox(themeBtn);
        topBar.getStyleClass().add("top-bar");
        topBar.setAlignment(Pos.CENTER_RIGHT);

        view.setTop(topBar);
        view.setCenter(tabPane);
    }

    private void applyTheme(Button themeBtn) {
        if (darkMode) {
            view.getStyleClass().remove("light");
            themeBtn.setText("☀️ Modo Claro");
        } else {
            if (!view.getStyleClass().contains("light")) {
                view.getStyleClass().add("light");
            }
            themeBtn.setText("🌙 Modo Escuro");
        }
    }

    public BorderPane getView() {
        return view;
    }
}
