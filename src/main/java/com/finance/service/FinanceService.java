package com.finance.service;

import com.finance.model.*;
import com.finance.repository.*;

import java.time.LocalDate;
import java.util.List;

public class FinanceService {

    private final RecurringEntryRepository recurringRepo;
    private final TransactionRepository transactionRepo;
    private final DebtRepository debtRepo;
    private final InvestmentRepository investmentRepo;

    public FinanceService() {
        this.recurringRepo = new RecurringEntryRepository();
        this.transactionRepo = new TransactionRepository();
        this.debtRepo = new DebtRepository();
        this.investmentRepo = new InvestmentRepository();
    }

    public FinanceService(RecurringEntryRepository recurringRepo, TransactionRepository transactionRepo,
                          DebtRepository debtRepo, InvestmentRepository investmentRepo) {
        this.recurringRepo = recurringRepo;
        this.transactionRepo = transactionRepo;
        this.debtRepo = debtRepo;
        this.investmentRepo = investmentRepo;
    }

    // ---- Recorrentes ----

    public RecurringEntry saveRecurring(RecurringEntry entry) {
        return recurringRepo.save(entry);
    }

    public void deleteRecurring(Long id) {
        recurringRepo.delete(id);
    }

    public List<RecurringEntry> getAllRecurring() {
        return recurringRepo.findAll();
    }

    public List<RecurringEntry> getActiveRecurring() {
        return recurringRepo.findAllActive();
    }

    // ---- Transações ----

    public Transaction saveTransaction(Transaction t) {
        return transactionRepo.save(t);
    }

    public void deleteTransaction(Long id) {
        transactionRepo.delete(id);
    }

    public List<Transaction> getTransactionsByMonth(int year, int month) {
        return transactionRepo.findByMonth(year, month);
    }

    // ---- Dívidas ----

    public Debt saveDebt(Debt debt) {
        return debtRepo.save(debt);
    }

    public void deleteDebt(Long id) {
        debtRepo.delete(id);
    }

    public List<Debt> getAllDebts() {
        return debtRepo.findAll();
    }

    public List<Debt> getActiveDebts() {
        return debtRepo.findAllActive();
    }

    public java.util.Optional<Debt> findDebtById(Long id) {
        return debtRepo.findById(id);
    }

    // ---- Investimentos ----

    public Investment saveInvestment(Investment inv) {
        return investmentRepo.save(inv);
    }

    public void deleteInvestment(Long id) {
        investmentRepo.delete(id);
    }

    public List<Investment> getAllInvestments() {
        return investmentRepo.findAll();
    }

    public List<Investment> getInvestmentsByMonth(int year, int month) {
        return investmentRepo.findByMonth(year, month);
    }

    public double getTotalInvested() {
        return investmentRepo.findAll().stream().mapToDouble(Investment::getAmount).sum();
    }

    // ---- Cálculos mensais ----

    public MonthSummary getMonthSummary(int year, int month) {
        List<Transaction> transactions = transactionRepo.findByMonth(year, month);

        double totalIncome = transactions.stream()
                .filter(t -> t.getType() == EntryType.INCOME)
                .mapToDouble(Transaction::getAmount).sum();

        double totalExpense = transactions.stream()
                .filter(t -> t.getType() == EntryType.EXPENSE)
                .mapToDouble(Transaction::getAmount).sum();

        List<RecurringEntry> activeRecurring = recurringRepo.findAllActive();

        double recurringIncome = activeRecurring.stream()
                .filter(r -> r.getType() == EntryType.INCOME)
                .mapToDouble(RecurringEntry::getAmount).sum();

        double recurringExpense = activeRecurring.stream()
                .filter(r -> r.getType() == EntryType.EXPENSE)
                .mapToDouble(RecurringEntry::getAmount).sum();

        List<Debt> allDebts = debtRepo.findAll();
        List<Debt> activeDebts = allDebts.stream()
                .filter(d -> d.isActiveOnMonth(year, month))
                .toList();

        double debtInstallments = activeDebts.stream()
                .mapToDouble(Debt::getInstallmentAmount).sum();

        List<Investment> investments = investmentRepo.findByMonth(year, month);
        double investmentAmount = investments.stream().mapToDouble(Investment::getAmount).sum();

        double balance = totalIncome - totalExpense;
        double projectedBalance = recurringIncome - recurringExpense - debtInstallments - investmentAmount;

        return new MonthSummary(year, month, totalIncome, totalExpense,
                recurringIncome, recurringExpense, debtInstallments,
                investmentAmount, balance, projectedBalance, transactions, activeDebts);
    }

    public List<MonthSummary> getProjections(int monthsAhead) {
        LocalDate now = LocalDate.now();
        return java.util.stream.IntStream.range(0, monthsAhead)
                .mapToObj(i -> {
                    LocalDate target = now.plusMonths(i);
                    return getMonthSummary(target.getYear(), target.getMonthValue());
                })
                .toList();
    }

    public double getMonthlyRecurringExpenseTotal() {
        return recurringRepo.findAllActive().stream()
                .filter(r -> r.getType() == EntryType.EXPENSE)
                .mapToDouble(RecurringEntry::getAmount).sum();
    }

    public double getMonthlyRecurringIncomeTotal() {
        return recurringRepo.findAllActive().stream()
                .filter(r -> r.getType() == EntryType.INCOME)
                .mapToDouble(RecurringEntry::getAmount).sum();
    }
}
