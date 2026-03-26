package com.finance.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ModelTest {

    @Test
    void recurringEntry_defaultActiveTrue() {
        RecurringEntry r = new RecurringEntry("Salário", 5000.0, EntryType.INCOME, 5);
        assertTrue(r.isActive());
        assertEquals("Salário", r.getDescription());
        assertEquals(5000.0, r.getAmount(), 0.001);
        assertEquals(EntryType.INCOME, r.getType());
        assertEquals(5, r.getDayOfMonth());
    }

    @Test
    void recurringEntry_noArgsConstructor() {
        RecurringEntry r = new RecurringEntry();
        assertNull(r.getId());
        assertNull(r.getDescription());
    }

    @Test
    void transaction_fieldsSetCorrectly() {
        LocalDate date = LocalDate.of(2025, 3, 15);
        Transaction t = new Transaction("Mercado", 350.0, EntryType.EXPENSE,
                PaymentMethod.CARTAO_DEBITO, date, "Alimentação");
        assertEquals("Mercado", t.getDescription());
        assertEquals(350.0, t.getAmount(), 0.001);
        assertEquals(EntryType.EXPENSE, t.getType());
        assertEquals(PaymentMethod.CARTAO_DEBITO, t.getPaymentMethod());
        assertEquals(date, t.getDate());
        assertEquals("Alimentação", t.getCategory());
        assertFalse(t.isInvestment());
    }

    @Test
    void transaction_investmentFlag() {
        Transaction t = new Transaction("Tesouro", 1000.0, EntryType.EXPENSE,
                PaymentMethod.PIX, LocalDate.now(), "Investimento");
        t.setInvestment(true);
        assertTrue(t.isInvestment());
    }

    @Test
    void investment_fieldsSetCorrectly() {
        LocalDate date = LocalDate.of(2025, 1, 10);
        Investment inv = new Investment("CDB Banco X", "CDB", 2000.0, date, PaymentMethod.PIX);
        assertEquals("CDB Banco X", inv.getDescription());
        assertEquals("CDB", inv.getType());
        assertEquals(2000.0, inv.getAmount(), 0.001);
        assertEquals(date, inv.getDate());
        assertEquals(PaymentMethod.PIX, inv.getPaymentMethod());
        assertNull(inv.getNotes());
    }

    @Test
    void investment_notes() {
        Investment inv = new Investment("Ações PETR4", "Ações", 500.0, LocalDate.now(), PaymentMethod.TRANSFERENCIA);
        inv.setNotes("Compra mensal");
        assertEquals("Compra mensal", inv.getNotes());
    }
}
