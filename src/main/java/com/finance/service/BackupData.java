package com.finance.service;

import com.finance.model.Debt;
import com.finance.model.Investment;
import com.finance.model.RecurringEntry;
import com.finance.model.Transaction;

import java.time.LocalDateTime;
import java.util.List;

public class BackupData {
    public String version = "1.0";
    public String exportedAt = LocalDateTime.now().toString();
    public List<RecurringEntry> recurringEntries;
    public List<Transaction> transactions;
    public List<Debt> debts;
    public List<Investment> investments;

    public BackupData() {}

    public BackupData(List<RecurringEntry> recurringEntries, List<Transaction> transactions,
                      List<Debt> debts, List<Investment> investments) {
        this.recurringEntries = recurringEntries;
        this.transactions = transactions;
        this.debts = debts;
        this.investments = investments;
    }
}
