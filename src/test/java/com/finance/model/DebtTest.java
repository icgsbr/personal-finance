package com.finance.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DebtTest {

    private Debt debt(int startYear, int startMonth, int installments) {
        return new Debt("Test", 100.0, installments,
                LocalDate.of(startYear, startMonth, 1), PaymentMethod.PIX);
    }

    @Test
    void constructorCalculatesTotalAmountCorrectly() {
        Debt d = new Debt("TV", 250.0, 12, LocalDate.now(), PaymentMethod.CARTAO_CREDITO);
        assertEquals(3000.0, d.getTotalAmount(), 0.001);
        assertEquals(250.0, d.getInstallmentAmount(), 0.001);
        assertEquals(12, d.getTotalInstallments());
        assertEquals(0, d.getPaidInstallments());
    }

    @ParameterizedTest
    @CsvSource({
        "2025,1, 2025,1,  true",   // primeiro mês
        "2025,1, 2025,6,  true",   // mês do meio
        "2025,1, 2025,12, true",   // último mês
        "2025,1, 2024,12, false",  // antes do início
        "2025,1, 2026,1,  false",  // depois do fim
    })
    void isActiveOnMonth(int startYear, int startMonth, int targetYear, int targetMonth, boolean expected) {
        Debt d = debt(startYear, startMonth, 12);
        assertEquals(expected, d.isActiveOnMonth(targetYear, targetMonth));
    }

    @Test
    void isActiveOnMonth_singleInstallment() {
        Debt d = debt(2025, 3, 1);
        assertTrue(d.isActiveOnMonth(2025, 3));
        assertFalse(d.isActiveOnMonth(2025, 4));
        assertFalse(d.isActiveOnMonth(2025, 2));
    }

    @Test
    void getRemainingInstallments() {
        Debt d = debt(2025, 1, 10);
        assertEquals(10, d.getRemainingInstallments());
        d.setPaidInstallments(4);
        assertEquals(6, d.getRemainingInstallments());
        d.setPaidInstallments(10);
        assertEquals(0, d.getRemainingInstallments());
    }

    @Test
    void isFinished() {
        Debt d = debt(2025, 1, 3);
        assertFalse(d.isFinished());
        d.setPaidInstallments(2);
        assertFalse(d.isFinished());
        d.setPaidInstallments(3);
        assertTrue(d.isFinished());
    }
}
