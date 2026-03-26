package com.finance.integration;

import com.finance.model.*;
import com.finance.service.BackupService;
import com.finance.service.FinanceService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class BackupIntegrationTest {

    private static final FinanceService financeService = new FinanceService();
    private static final BackupService backupService = new BackupService();

    @TempDir
    static Path tempDir;

    private static File backupFile;

    @BeforeAll
    static void seedData() {
        financeService.saveRecurring(new RecurringEntry("Salário BK", 6000.0, EntryType.INCOME, 5));
        financeService.saveRecurring(new RecurringEntry("Aluguel BK", 1200.0, EntryType.EXPENSE, 10));
        financeService.saveTransaction(new Transaction("Mercado BK", 300.0, EntryType.EXPENSE,
                PaymentMethod.CARTAO_DEBITO, LocalDate.of(2025, 9, 10), "Alimentação"));
        financeService.saveDebt(new Debt("Parcela BK", 400.0, 6,
                LocalDate.of(2025, 9, 1), PaymentMethod.CARTAO_CREDITO));
        financeService.saveInvestment(new Investment("CDB BK", "Renda Fixa", 3000.0,
                LocalDate.of(2025, 9, 1), PaymentMethod.PIX));
    }

    @Test @Order(1)
    void export_createsFile(@TempDir Path dir) throws Exception {
        backupFile = dir.resolve("test-backup.json").toFile();
        backupService.export(backupFile);

        assertTrue(backupFile.exists());
        assertTrue(backupFile.length() > 100);
    }

    @Test @Order(2)
    void export_fileContainsSeededData(@TempDir Path dir) throws Exception {
        File file = dir.resolve("check-backup.json").toFile();
        backupService.export(file);

        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
        assertTrue(content.contains("Salário BK"));
        assertTrue(content.contains("Mercado BK"));
        assertTrue(content.contains("Parcela BK"));
        assertTrue(content.contains("CDB BK"));
    }

    @Test @Order(3)
    void import_duplicates_areSkipped(@TempDir Path dir) throws Exception {
        File file = dir.resolve("dup-backup.json").toFile();
        backupService.export(file);

        // Importa o mesmo arquivo — tudo deve ser ignorado
        BackupService.ImportResult result = backupService.importFrom(file);

        assertEquals(0, result.importedRecurring());
        assertEquals(0, result.importedTransactions());
        assertEquals(0, result.importedDebts());
        assertEquals(0, result.importedInvestments());
        assertTrue(result.totalSkipped() > 0);
    }

    @Test @Order(4)
    void import_newData_isAdded(@TempDir Path dir) throws Exception {
        // Cria um backup com dados novos que não existem no banco
        com.finance.service.BackupData newData = new com.finance.service.BackupData(
                java.util.List.of(new RecurringEntry("Nova Renda BK", 500.0, EntryType.INCOME, 15)),
                java.util.List.of(new Transaction("Nova TX BK", 100.0, EntryType.EXPENSE,
                        PaymentMethod.PIX, LocalDate.of(2024, 1, 1), "Teste")),
                java.util.List.of(),
                java.util.List.of()
        );

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        File file = dir.resolve("new-data.json").toFile();
        mapper.writeValue(file, newData);

        BackupService.ImportResult result = backupService.importFrom(file);

        assertEquals(1, result.importedRecurring());
        assertEquals(1, result.importedTransactions());
        assertEquals(0, result.skippedRecurring());
        assertEquals(0, result.skippedTransactions());
    }

    @Test @Order(5)
    void import_mixedData_correctCounts(@TempDir Path dir) throws Exception {
        // Exporta estado atual (todos duplicados) + adiciona 1 novo
        File exportFile = dir.resolve("mixed.json").toFile();
        backupService.export(exportFile);

        // Lê o JSON e adiciona um registro novo manualmente
        String json = new String(java.nio.file.Files.readAllBytes(exportFile.toPath()));
        String newEntry = "{\"description\":\"Único BK\",\"amount\":999.0,\"type\":\"INCOME\",\"dayOfMonth\":1,\"active\":true}";
        // Injeta no array de recurringEntries
        json = json.replace("\"recurringEntries\" : [ ", "\"recurringEntries\" : [ " + newEntry + ",");

        File mixedFile = dir.resolve("mixed-modified.json").toFile();
        java.nio.file.Files.writeString(mixedFile.toPath(), json);

        BackupService.ImportResult result = backupService.importFrom(mixedFile);

        assertEquals(1, result.importedRecurring());
        assertTrue(result.skippedRecurring() > 0);
    }
}
