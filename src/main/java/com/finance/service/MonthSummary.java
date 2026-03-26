package com.finance.service;

import com.finance.model.Debt;
import com.finance.model.Transaction;

import java.util.List;

public record MonthSummary(
        int year,
        int month,
        double totalIncome,
        double totalExpense,
        double recurringIncome,
        double recurringExpense,
        double debtInstallments,
        double investmentAmount,
        double balance,
        double projectedBalance,
        List<Transaction> transactions,
        List<Debt> activeDebts
) {
    public String getMonthLabel() {
        return String.format("%02d/%d", month, year);
    }

    public double getNetRecurring() {
        return recurringIncome - recurringExpense - debtInstallments;
    }
}
