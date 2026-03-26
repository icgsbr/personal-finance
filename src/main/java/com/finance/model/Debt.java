package com.finance.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@Entity
@Table(name = "debts")
public class Debt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "installment_amount", nullable = false)
    private Double installmentAmount;

    @Column(name = "total_installments", nullable = false)
    private Integer totalInstallments;

    @Column(name = "paid_installments", nullable = false)
    private Integer paidInstallments = 0;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    public Debt(String description, Double installmentAmount, Integer totalInstallments, LocalDate startDate, PaymentMethod paymentMethod) {
        this.description = description;
        this.installmentAmount = installmentAmount;
        this.totalInstallments = totalInstallments;
        this.totalAmount = installmentAmount * totalInstallments;
        this.paidInstallments = 0;
        this.startDate = startDate;
        this.paymentMethod = paymentMethod;
    }

    public boolean isActiveOnMonth(int year, int month) {
        LocalDate start = startDate.withDayOfMonth(1);
        LocalDate end = start.plusMonths(totalInstallments - 1);
        LocalDate target = LocalDate.of(year, month, 1);
        return !target.isBefore(start) && !target.isAfter(end);
    }

    @JsonIgnore
    public int getRemainingInstallments() {
        return totalInstallments - paidInstallments;
    }

    @JsonIgnore
    public boolean isFinished() {
        return paidInstallments >= totalInstallments;
    }
}
