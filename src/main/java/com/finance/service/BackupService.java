package com.finance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.finance.model.*;
import com.finance.repository.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class BackupService {

    private final RecurringEntryRepository recurringRepo = new RecurringEntryRepository();
    private final TransactionRepository transactionRepo = new TransactionRepository();
    private final DebtRepository debtRepo = new DebtRepository();
    private final InvestmentRepository investmentRepo = new InvestmentRepository();

    private final ObjectMapper mapper;

    public BackupService() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public void export(File file) throws IOException {
        BackupData data = new BackupData(
                recurringRepo.findAll(),
                transactionRepo.findAll(),
                debtRepo.findAll(),
                investmentRepo.findAll()
        );
        mapper.writeValue(file, data);
    }

    public ImportResult importFrom(File file) throws IOException {
        BackupData data = mapper.readValue(file, BackupData.class);

        int importedRecurring = 0, skippedRecurring = 0;
        int importedTransactions = 0, skippedTransactions = 0;
        int importedDebts = 0, skippedDebts = 0;
        int importedInvestments = 0, skippedInvestments = 0;

        List<RecurringEntry> existingRecurring = recurringRepo.findAll();
        if (data.recurringEntries != null) {
            for (RecurringEntry r : data.recurringEntries) {
                if (isDuplicateRecurring(r, existingRecurring)) {
                    skippedRecurring++;
                } else {
                    r.setId(null);
                    recurringRepo.save(r);
                    importedRecurring++;
                }
            }
        }

        List<Transaction> existingTx = transactionRepo.findAll();
        if (data.transactions != null) {
            for (Transaction t : data.transactions) {
                if (isDuplicateTransaction(t, existingTx)) {
                    skippedTransactions++;
                } else {
                    t.setId(null);
                    transactionRepo.save(t);
                    importedTransactions++;
                }
            }
        }

        List<Debt> existingDebts = debtRepo.findAll();
        if (data.debts != null) {
            for (Debt d : data.debts) {
                if (isDuplicateDebt(d, existingDebts)) {
                    skippedDebts++;
                } else {
                    d.setId(null);
                    debtRepo.save(d);
                    importedDebts++;
                }
            }
        }

        List<Investment> existingInv = investmentRepo.findAll();
        if (data.investments != null) {
            for (Investment i : data.investments) {
                if (isDuplicateInvestment(i, existingInv)) {
                    skippedInvestments++;
                } else {
                    i.setId(null);
                    investmentRepo.save(i);
                    importedInvestments++;
                }
            }
        }

        return new ImportResult(
                importedRecurring, skippedRecurring,
                importedTransactions, skippedTransactions,
                importedDebts, skippedDebts,
                importedInvestments, skippedInvestments
        );
    }

    // Deduplicação por conteúdo — ignora ID

    private boolean isDuplicateRecurring(RecurringEntry r, List<RecurringEntry> existing) {
        return existing.stream().anyMatch(e ->
                Objects.equals(e.getDescription(), r.getDescription()) &&
                Objects.equals(e.getAmount(), r.getAmount()) &&
                e.getType() == r.getType() &&
                Objects.equals(e.getDayOfMonth(), r.getDayOfMonth())
        );
    }

    private boolean isDuplicateTransaction(Transaction t, List<Transaction> existing) {
        return existing.stream().anyMatch(e ->
                Objects.equals(e.getDescription(), t.getDescription()) &&
                Objects.equals(e.getAmount(), t.getAmount()) &&
                e.getType() == t.getType() &&
                Objects.equals(e.getDate(), t.getDate()) &&
                e.getPaymentMethod() == t.getPaymentMethod()
        );
    }

    private boolean isDuplicateDebt(Debt d, List<Debt> existing) {
        return existing.stream().anyMatch(e ->
                Objects.equals(e.getDescription(), d.getDescription()) &&
                Objects.equals(e.getInstallmentAmount(), d.getInstallmentAmount()) &&
                Objects.equals(e.getTotalInstallments(), d.getTotalInstallments()) &&
                Objects.equals(e.getStartDate(), d.getStartDate())
        );
    }

    private boolean isDuplicateInvestment(Investment i, List<Investment> existing) {
        return existing.stream().anyMatch(e ->
                Objects.equals(e.getDescription(), i.getDescription()) &&
                Objects.equals(e.getAmount(), i.getAmount()) &&
                Objects.equals(e.getDate(), i.getDate()) &&
                Objects.equals(e.getType(), i.getType())
        );
    }

    public record ImportResult(
            int importedRecurring, int skippedRecurring,
            int importedTransactions, int skippedTransactions,
            int importedDebts, int skippedDebts,
            int importedInvestments, int skippedInvestments
    ) {
        public int totalImported() {
            return importedRecurring + importedTransactions + importedDebts + importedInvestments;
        }

        public int totalSkipped() {
            return skippedRecurring + skippedTransactions + skippedDebts + skippedInvestments;
        }

        public String summary() {
            return String.format("""
                    ✅ Importados: %d registros
                    ⏭ Ignorados (já existiam): %d registros
                    
                    Detalhes:
                    • Recorrentes: %d importados, %d ignorados
                    • Lançamentos: %d importados, %d ignorados
                    • Dívidas: %d importados, %d ignorados
                    • Investimentos: %d importados, %d ignorados
                    """,
                    totalImported(), totalSkipped(),
                    importedRecurring, skippedRecurring,
                    importedTransactions, skippedTransactions,
                    importedDebts, skippedDebts,
                    importedInvestments, skippedInvestments
            );
        }
    }
}
