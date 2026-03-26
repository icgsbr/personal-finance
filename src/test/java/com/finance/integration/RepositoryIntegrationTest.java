package com.finance.integration;

import com.finance.model.*;
import com.finance.repository.*;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RepositoryIntegrationTest {

    private static final RecurringEntryRepository recurringRepo = new RecurringEntryRepository();
    private static final TransactionRepository transactionRepo = new TransactionRepository();
    private static final DebtRepository debtRepo = new DebtRepository();
    private static final InvestmentRepository investmentRepo = new InvestmentRepository();

    // ---- RecurringEntry ----

    @Test @Order(1)
    void recurring_saveAndFindAll() {
        RecurringEntry r = new RecurringEntry("Salário", 5000.0, EntryType.INCOME, 5);
        RecurringEntry saved = recurringRepo.save(r);

        assertNotNull(saved.getId());
        List<RecurringEntry> all = recurringRepo.findAll();
        assertTrue(all.stream().anyMatch(e -> e.getDescription().equals("Salário")));
    }

    @Test @Order(2)
    void recurring_findAllActive_returnsOnlyActive() {
        RecurringEntry active = new RecurringEntry("Aluguel", 1500.0, EntryType.EXPENSE, 10);
        RecurringEntry inactive = new RecurringEntry("Extinto", 100.0, EntryType.EXPENSE, 1);
        inactive.setActive(false);

        recurringRepo.save(active);
        recurringRepo.save(inactive);

        List<RecurringEntry> actives = recurringRepo.findAllActive();
        assertTrue(actives.stream().allMatch(RecurringEntry::isActive));
        assertTrue(actives.stream().noneMatch(e -> e.getDescription().equals("Extinto")));
    }

    @Test @Order(3)
    void recurring_findActiveByType() {
        List<RecurringEntry> incomes = recurringRepo.findActiveByType(EntryType.INCOME);
        List<RecurringEntry> expenses = recurringRepo.findActiveByType(EntryType.EXPENSE);

        assertTrue(incomes.stream().allMatch(r -> r.getType() == EntryType.INCOME));
        assertTrue(expenses.stream().allMatch(r -> r.getType() == EntryType.EXPENSE));
    }

    @Test @Order(4)
    void recurring_delete() {
        RecurringEntry r = recurringRepo.save(new RecurringEntry("Temp", 50.0, EntryType.EXPENSE, null));
        Long id = r.getId();
        recurringRepo.delete(id);
        Optional<RecurringEntry> found = recurringRepo.findById(id);
        assertTrue(found.isEmpty());
    }

    @Test @Order(5)
    void recurring_update() {
        RecurringEntry r = recurringRepo.save(new RecurringEntry("Original", 100.0, EntryType.INCOME, 1));
        r.setDescription("Atualizado");
        r.setAmount(200.0);
        recurringRepo.save(r);

        RecurringEntry updated = recurringRepo.findById(r.getId()).orElseThrow();
        assertEquals("Atualizado", updated.getDescription());
        assertEquals(200.0, updated.getAmount(), 0.001);
    }

    // ---- Transaction ----

    @Test @Order(10)
    void transaction_saveAndFindByMonth() {
        Transaction t = new Transaction("Mercado", 350.0, EntryType.EXPENSE,
                PaymentMethod.CARTAO_DEBITO, LocalDate.of(2025, 3, 15), "Alimentação");
        transactionRepo.save(t);

        List<Transaction> found = transactionRepo.findByMonth(2025, 3);
        assertTrue(found.stream().anyMatch(tx -> tx.getDescription().equals("Mercado")));
    }

    @Test @Order(11)
    void transaction_findByMonth_excludesOtherMonths() {
        transactionRepo.save(new Transaction("Janeiro", 100.0, EntryType.INCOME,
                PaymentMethod.PIX, LocalDate.of(2025, 1, 10), null));
        transactionRepo.save(new Transaction("Fevereiro", 200.0, EntryType.INCOME,
                PaymentMethod.PIX, LocalDate.of(2025, 2, 10), null));

        List<Transaction> jan = transactionRepo.findByMonth(2025, 1);
        assertTrue(jan.stream().anyMatch(t -> t.getDescription().equals("Janeiro")));
        assertTrue(jan.stream().noneMatch(t -> t.getDescription().equals("Fevereiro")));
    }

    @Test @Order(12)
    void transaction_findByMonth_excludesInvestments() {
        Transaction inv = new Transaction("Tesouro", 1000.0, EntryType.EXPENSE,
                PaymentMethod.PIX, LocalDate.of(2025, 3, 5), "Investimento");
        inv.setInvestment(true);
        transactionRepo.save(inv);

        List<Transaction> txs = transactionRepo.findByMonth(2025, 3);
        assertTrue(txs.stream().noneMatch(t -> t.isInvestment()));
    }

    @Test @Order(13)
    void transaction_delete() {
        Transaction t = transactionRepo.save(new Transaction("Del", 10.0, EntryType.EXPENSE,
                PaymentMethod.DINHEIRO, LocalDate.of(2025, 3, 1), null));
        transactionRepo.delete(t.getId());
        assertTrue(transactionRepo.findById(t.getId()).isEmpty());
    }

    @Test @Order(14)
    void transaction_update_preservesId() {
        Transaction t = transactionRepo.save(new Transaction("Antes", 100.0, EntryType.INCOME,
                PaymentMethod.PIX, LocalDate.of(2025, 4, 1), null));
        Long id = t.getId();
        t.setDescription("Depois");
        t.setAmount(999.0);
        transactionRepo.save(t);

        Transaction updated = transactionRepo.findById(id).orElseThrow();
        assertEquals("Depois", updated.getDescription());
        assertEquals(999.0, updated.getAmount(), 0.001);
    }

    // ---- Debt ----

    @Test @Order(20)
    void debt_saveAndFindAll() {
        Debt d = new Debt("Notebook", 300.0, 10, LocalDate.of(2025, 1, 1), PaymentMethod.CARTAO_CREDITO);
        debtRepo.save(d);

        List<Debt> all = debtRepo.findAll();
        assertTrue(all.stream().anyMatch(debt -> debt.getDescription().equals("Notebook")));
    }

    @Test @Order(21)
    void debt_totalAmountCalculatedCorrectly() {
        Debt d = new Debt("TV", 250.0, 12, LocalDate.of(2025, 1, 1), PaymentMethod.CARTAO_CREDITO);
        Debt saved = debtRepo.save(d);
        assertEquals(3000.0, saved.getTotalAmount(), 0.001);
    }

    @Test @Order(22)
    void debt_findAllActive_excludesFinished() {
        Debt active = new Debt("Ativo", 100.0, 6, LocalDate.of(2025, 1, 1), PaymentMethod.PIX);
        Debt finished = new Debt("Quitado", 100.0, 3, LocalDate.of(2025, 1, 1), PaymentMethod.PIX);
        finished.setPaidInstallments(3);

        debtRepo.save(active);
        debtRepo.save(finished);

        List<Debt> actives = debtRepo.findAllActive();
        assertTrue(actives.stream().noneMatch(Debt::isFinished));
    }

    @Test @Order(23)
    void debt_markPaidInstallments() {
        Debt d = debtRepo.save(new Debt("Parcela", 200.0, 5, LocalDate.of(2025, 2, 1), PaymentMethod.PIX));
        d.setPaidInstallments(2);
        debtRepo.save(d);

        Debt updated = debtRepo.findById(d.getId()).orElseThrow();
        assertEquals(2, updated.getPaidInstallments());
        assertEquals(3, updated.getRemainingInstallments());
        assertFalse(updated.isFinished());
    }

    @Test @Order(24)
    void debt_delete() {
        Debt d = debtRepo.save(new Debt("Del", 100.0, 2, LocalDate.now(), PaymentMethod.DINHEIRO));
        debtRepo.delete(d.getId());
        assertTrue(debtRepo.findById(d.getId()).isEmpty());
    }

    // ---- Investment ----

    @Test @Order(30)
    void investment_saveAndFindAll() {
        Investment inv = new Investment("CDB", "Renda Fixa", 5000.0,
                LocalDate.of(2025, 3, 1), PaymentMethod.PIX);
        investmentRepo.save(inv);

        List<Investment> all = investmentRepo.findAll();
        assertTrue(all.stream().anyMatch(i -> i.getDescription().equals("CDB")));
    }

    @Test @Order(31)
    void investment_findByMonth() {
        investmentRepo.save(new Investment("Março", "Ações", 1000.0,
                LocalDate.of(2025, 3, 10), PaymentMethod.PIX));
        investmentRepo.save(new Investment("Abril", "Ações", 2000.0,
                LocalDate.of(2025, 4, 10), PaymentMethod.PIX));

        List<Investment> march = investmentRepo.findByMonth(2025, 3);
        assertTrue(march.stream().anyMatch(i -> i.getDescription().equals("Março")));
        assertTrue(march.stream().noneMatch(i -> i.getDescription().equals("Abril")));
    }

    @Test @Order(32)
    void investment_delete() {
        Investment inv = investmentRepo.save(new Investment("Del", "CDB", 100.0,
                LocalDate.now(), PaymentMethod.PIX));
        investmentRepo.delete(inv.getId());
        assertTrue(investmentRepo.findById(inv.getId()).isEmpty());
    }
}
