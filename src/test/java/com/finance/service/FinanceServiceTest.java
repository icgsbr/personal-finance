package com.finance.service;

import com.finance.model.*;
import com.finance.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock private RecurringEntryRepository recurringRepo;
    @Mock private TransactionRepository transactionRepo;
    @Mock private DebtRepository debtRepo;
    @Mock private InvestmentRepository investmentRepo;

    private FinanceService service;

    private final int YEAR = 2025;
    private final int MONTH = 3;

    @BeforeEach
    void setUp() {
        service = new FinanceService(recurringRepo, transactionRepo, debtRepo, investmentRepo);
        lenient().when(recurringRepo.findAllActive()).thenReturn(List.of());
        lenient().when(transactionRepo.findByMonth(anyInt(), anyInt())).thenReturn(List.of());
        lenient().when(debtRepo.findAll()).thenReturn(List.of());
        lenient().when(investmentRepo.findByMonth(anyInt(), anyInt())).thenReturn(List.of());
    }

    // ---- MonthSummary ----

    @Test
    void getMonthSummary_emptyData_returnsZeroBalance() {
        MonthSummary summary = service.getMonthSummary(YEAR, MONTH);
        assertEquals(0.0, summary.balance(), 0.001);
        assertEquals(0.0, summary.totalIncome(), 0.001);
        assertEquals(0.0, summary.totalExpense(), 0.001);
    }

    @Test
    void getMonthSummary_calculatesBalanceFromTransactions() {
        List<Transaction> txs = List.of(
                tx(3000.0, EntryType.INCOME),
                tx(1200.0, EntryType.EXPENSE),
                tx(300.0, EntryType.EXPENSE)
        );
        when(transactionRepo.findByMonth(YEAR, MONTH)).thenReturn(txs);

        MonthSummary summary = service.getMonthSummary(YEAR, MONTH);

        assertEquals(3000.0, summary.totalIncome(), 0.001);
        assertEquals(1500.0, summary.totalExpense(), 0.001);
        assertEquals(1500.0, summary.balance(), 0.001);
    }

    @Test
    void getMonthSummary_includesRecurringInProjection() {
        when(recurringRepo.findAllActive()).thenReturn(List.of(
                recurring(5000.0, EntryType.INCOME),
                recurring(1500.0, EntryType.EXPENSE),
                recurring(800.0, EntryType.EXPENSE)
        ));

        MonthSummary summary = service.getMonthSummary(YEAR, MONTH);

        assertEquals(5000.0, summary.recurringIncome(), 0.001);
        assertEquals(2300.0, summary.recurringExpense(), 0.001);
        assertEquals(2700.0, summary.projectedBalance(), 0.001);
    }

    @Test
    void getMonthSummary_debtInstallmentsReduceProjection() {
        when(recurringRepo.findAllActive()).thenReturn(List.of(recurring(5000.0, EntryType.INCOME)));
        Debt debt = new Debt("Carro", 800.0, 24, LocalDate.of(YEAR, MONTH, 1), PaymentMethod.PIX);
        when(debtRepo.findAll()).thenReturn(List.of(debt));

        MonthSummary summary = service.getMonthSummary(YEAR, MONTH);

        assertEquals(800.0, summary.debtInstallments(), 0.001);
        assertEquals(4200.0, summary.projectedBalance(), 0.001);
        assertEquals(1, summary.activeDebts().size());
    }

    @Test
    void getMonthSummary_debtNotActiveInMonth_notIncluded() {
        when(recurringRepo.findAllActive()).thenReturn(List.of(recurring(5000.0, EntryType.INCOME)));
        // Dívida que começa em mês futuro
        Debt debt = new Debt("Notebook", 500.0, 6, LocalDate.of(YEAR, MONTH + 2, 1), PaymentMethod.CARTAO_CREDITO);
        when(debtRepo.findAll()).thenReturn(List.of(debt));

        MonthSummary summary = service.getMonthSummary(YEAR, MONTH);

        assertEquals(0.0, summary.debtInstallments(), 0.001);
        assertTrue(summary.activeDebts().isEmpty());
    }

    @Test
    void getMonthSummary_investmentsIncluded() {
        Investment inv = new Investment("Tesouro", "TD", 1000.0, LocalDate.of(YEAR, MONTH, 10), PaymentMethod.PIX);
        when(investmentRepo.findByMonth(YEAR, MONTH)).thenReturn(List.of(inv));

        MonthSummary summary = service.getMonthSummary(YEAR, MONTH);

        assertEquals(1000.0, summary.investmentAmount(), 0.001);
    }

    @Test
    void getMonthSummary_netRecurring() {
        when(recurringRepo.findAllActive()).thenReturn(List.of(
                recurring(6000.0, EntryType.INCOME),
                recurring(2000.0, EntryType.EXPENSE)
        ));
        Debt debt = new Debt("Parcela", 500.0, 12, LocalDate.of(YEAR, MONTH, 1), PaymentMethod.PIX);
        when(debtRepo.findAll()).thenReturn(List.of(debt));

        MonthSummary summary = service.getMonthSummary(YEAR, MONTH);

        assertEquals(3500.0, summary.getNetRecurring(), 0.001);
    }

    // ---- Recorrentes ----

    @Test
    void getMonthlyRecurringIncomeTotal_onlyActiveIncome() {
        when(recurringRepo.findAllActive()).thenReturn(List.of(
                recurring(4000.0, EntryType.INCOME),
                recurring(1000.0, EntryType.INCOME),
                recurring(500.0, EntryType.EXPENSE)
        ));
        assertEquals(5000.0, service.getMonthlyRecurringIncomeTotal(), 0.001);
    }

    @Test
    void getMonthlyRecurringExpenseTotal_onlyActiveExpense() {
        when(recurringRepo.findAllActive()).thenReturn(List.of(
                recurring(4000.0, EntryType.INCOME),
                recurring(800.0, EntryType.EXPENSE),
                recurring(200.0, EntryType.EXPENSE)
        ));
        assertEquals(1000.0, service.getMonthlyRecurringExpenseTotal(), 0.001);
    }

    // ---- Investimentos ----

    @Test
    void getTotalInvested_sumsAllInvestments() {
        when(investmentRepo.findAll()).thenReturn(List.of(
                inv(1000.0), inv(2500.0), inv(500.0)
        ));
        assertEquals(4000.0, service.getTotalInvested(), 0.001);
    }

    @Test
    void getTotalInvested_emptyReturnsZero() {
        when(investmentRepo.findAll()).thenReturn(List.of());
        assertEquals(0.0, service.getTotalInvested(), 0.001);
    }

    // ---- Projeções ----

    @Test
    void getProjections_returnsCorrectNumberOfMonths() {
        List<MonthSummary> projections = service.getProjections(6);
        assertEquals(6, projections.size());
    }

    @Test
    void getProjections_firstMonthIsCurrentMonth() {
        List<MonthSummary> projections = service.getProjections(3);
        LocalDate now = LocalDate.now();
        assertEquals(now.getYear(), projections.get(0).year());
        assertEquals(now.getMonthValue(), projections.get(0).month());
    }

    // ---- CRUD delegations ----

    @Test
    void saveRecurring_delegatesToRepo() {
        RecurringEntry entry = recurring(1000.0, EntryType.INCOME);
        when(recurringRepo.save(entry)).thenReturn(entry);
        service.saveRecurring(entry);
        verify(recurringRepo).save(entry);
    }

    @Test
    void deleteTransaction_delegatesToRepo() {
        service.deleteTransaction(42L);
        verify(transactionRepo).delete(42L);
    }

    @Test
    void findDebtById_delegatesToRepo() {
        Debt debt = new Debt("Test", 100.0, 3, LocalDate.now(), PaymentMethod.PIX);
        when(debtRepo.findById(1L)).thenReturn(Optional.of(debt));
        assertTrue(service.findDebtById(1L).isPresent());
    }
    // ---- Helpers ----

    private Transaction tx(double amount, EntryType type) {
        return new Transaction("desc", amount, type, PaymentMethod.PIX, LocalDate.of(YEAR, MONTH, 1), "cat");
    }

    private RecurringEntry recurring(double amount, EntryType type) {
        return new RecurringEntry("desc", amount, type, 1);
    }

    private Investment inv(double amount) {
        return new Investment("inv", "CDB", amount, LocalDate.now(), PaymentMethod.PIX);
    }
}
