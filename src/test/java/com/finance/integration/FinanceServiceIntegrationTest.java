package com.finance.integration;

import com.finance.model.*;
import com.finance.service.FinanceService;
import com.finance.service.MonthSummary;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FinanceServiceIntegrationTest {

    private static final FinanceService service = new FinanceService();

    @Test @Order(1)
    void saveAndRetrieveRecurring() {
        RecurringEntry r = service.saveRecurring(new RecurringEntry("Salário IT", 8000.0, EntryType.INCOME, 5));
        assertNotNull(r.getId());

        List<RecurringEntry> all = service.getAllRecurring();
        assertTrue(all.stream().anyMatch(e -> e.getDescription().equals("Salário IT")));
    }

    @Test @Order(2)
    void deleteRecurring_removesFromList() {
        RecurringEntry r = service.saveRecurring(new RecurringEntry("Temp IT", 100.0, EntryType.EXPENSE, null));
        Long id = r.getId();
        service.deleteRecurring(id);

        assertTrue(service.getAllRecurring().stream().noneMatch(e -> e.getId().equals(id)));
    }

    @Test @Order(3)
    void saveAndRetrieveTransaction() {
        Transaction t = service.saveTransaction(new Transaction(
                "Supermercado IT", 450.0, EntryType.EXPENSE,
                PaymentMethod.CARTAO_DEBITO, LocalDate.of(2025, 5, 10), "Alimentação"));
        assertNotNull(t.getId());

        List<Transaction> found = service.getTransactionsByMonth(2025, 5);
        assertTrue(found.stream().anyMatch(tx -> tx.getDescription().equals("Supermercado IT")));
    }

    @Test @Order(4)
    void deleteTransaction_removesFromMonth() {
        Transaction t = service.saveTransaction(new Transaction(
                "Del IT", 50.0, EntryType.EXPENSE, PaymentMethod.DINHEIRO,
                LocalDate.of(2025, 5, 15), null));
        service.deleteTransaction(t.getId());

        List<Transaction> found = service.getTransactionsByMonth(2025, 5);
        assertTrue(found.stream().noneMatch(tx -> tx.getId().equals(t.getId())));
    }

    @Test @Order(5)
    void saveAndRetrieveDebt() {
        Debt d = service.saveDebt(new Debt("Financiamento IT", 500.0, 24,
                LocalDate.of(2025, 1, 1), PaymentMethod.CARTAO_CREDITO));
        assertNotNull(d.getId());
        assertEquals(12000.0, d.getTotalAmount(), 0.001);

        assertTrue(service.getAllDebts().stream().anyMatch(debt -> debt.getDescription().equals("Financiamento IT")));
    }

    @Test @Order(6)
    void saveAndRetrieveInvestment() {
        Investment inv = service.saveInvestment(new Investment(
                "Tesouro IT", "Tesouro Direto", 2000.0, LocalDate.of(2025, 3, 1), PaymentMethod.PIX));
        assertNotNull(inv.getId());

        assertTrue(service.getAllInvestments().stream().anyMatch(i -> i.getDescription().equals("Tesouro IT")));
    }

    @Test @Order(7)
    void getTotalInvested_sumsAll() {
        double before = service.getTotalInvested();
        service.saveInvestment(new Investment("Extra IT", "CDB", 1000.0, LocalDate.now(), PaymentMethod.PIX));
        assertEquals(before + 1000.0, service.getTotalInvested(), 0.001);
    }

    @Test @Order(8)
    void getMonthSummary_reflectsRealData() {
        // Limpa o mês 6/2025 salvando transações específicas
        service.saveTransaction(new Transaction("Entrada IT", 3000.0, EntryType.INCOME,
                PaymentMethod.PIX, LocalDate.of(2025, 6, 1), null));
        service.saveTransaction(new Transaction("Saída IT", 1000.0, EntryType.EXPENSE,
                PaymentMethod.PIX, LocalDate.of(2025, 6, 15), null));

        MonthSummary summary = service.getMonthSummary(2025, 6);

        assertTrue(summary.totalIncome() >= 3000.0);
        assertTrue(summary.totalExpense() >= 1000.0);
        assertTrue(summary.balance() >= 2000.0);
    }

    @Test @Order(9)
    void getMonthSummary_debtActiveInCorrectMonths() {
        Debt debt = service.saveDebt(new Debt("Parcela IT", 300.0, 3,
                LocalDate.of(2025, 7, 1), PaymentMethod.PIX));

        MonthSummary jul = service.getMonthSummary(2025, 7);
        MonthSummary oct = service.getMonthSummary(2025, 10);

        assertTrue(jul.activeDebts().stream().anyMatch(d -> d.getId().equals(debt.getId())));
        assertTrue(oct.activeDebts().stream().noneMatch(d -> d.getId().equals(debt.getId())));
    }

    @Test @Order(10)
    void getProjections_returnsRequestedMonths() {
        List<MonthSummary> projections = service.getProjections(4);
        assertEquals(4, projections.size());

        LocalDate now = LocalDate.now();
        assertEquals(now.getYear(), projections.get(0).year());
        assertEquals(now.getMonthValue(), projections.get(0).month());
    }

    @Test @Order(11)
    void recurringTotals_reflectActiveEntries() {
        double incomeBefore = service.getMonthlyRecurringIncomeTotal();
        double expenseBefore = service.getMonthlyRecurringExpenseTotal();

        service.saveRecurring(new RecurringEntry("Renda Extra IT", 1000.0, EntryType.INCOME, null));
        service.saveRecurring(new RecurringEntry("Conta IT", 200.0, EntryType.EXPENSE, null));

        assertEquals(incomeBefore + 1000.0, service.getMonthlyRecurringIncomeTotal(), 0.001);
        assertEquals(expenseBefore + 200.0, service.getMonthlyRecurringExpenseTotal(), 0.001);
    }

    @Test @Order(12)
    void debt_markPaidAndCheckFinished() {
        Debt d = service.saveDebt(new Debt("Quitação IT", 100.0, 2,
                LocalDate.of(2025, 8, 1), PaymentMethod.PIX));
        assertFalse(d.isFinished());

        d.setPaidInstallments(1);
        service.saveDebt(d);
        assertFalse(service.findDebtById(d.getId()).orElseThrow().isFinished());

        d.setPaidInstallments(2);
        service.saveDebt(d);
        assertTrue(service.findDebtById(d.getId()).orElseThrow().isFinished());
    }

    @Test @Order(13)
    void getActiveDebts_excludesFinished() {
        Debt finished = service.saveDebt(new Debt("Quitada IT", 50.0, 1,
                LocalDate.of(2025, 1, 1), PaymentMethod.DINHEIRO));
        finished.setPaidInstallments(1);
        service.saveDebt(finished);

        List<Debt> active = service.getActiveDebts();
        assertTrue(active.stream().noneMatch(d -> d.getId().equals(finished.getId())));
    }
}
