package com.finance.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "recurring_entries")
public class RecurringEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EntryType type;

    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    private boolean active = true;

    public RecurringEntry(String description, Double amount, EntryType type, Integer dayOfMonth) {
        this.description = description;
        this.amount = amount;
        this.type = type;
        this.dayOfMonth = dayOfMonth;
        this.active = true;
    }
}
