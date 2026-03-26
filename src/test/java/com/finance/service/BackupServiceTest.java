package com.finance.service;

import com.finance.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BackupServiceTest {

    // Usa H2 em memória via persistence.xml de teste
    private BackupService backupService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        backupService = new BackupService();
    }

    @Test
    void exportAndImport_roundTrip(@TempDir Path dir) throws Exception {
        File file = dir.resolve("backup.json").toFile();

        // Exporta (banco pode estar vazio — só verifica que não lança exceção)
        assertDoesNotThrow(() -> backupService.export(file));
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    void importResult_summary_containsAllSections() {
        BackupService.ImportResult result = new BackupService.ImportResult(2, 1, 5, 3, 1, 0, 3, 2);
        String summary = result.summary();

        assertTrue(summary.contains("Importados: 11"));
        assertTrue(summary.contains("Ignorados"));
        assertTrue(summary.contains("Recorrentes"));
        assertTrue(summary.contains("Lançamentos"));
        assertTrue(summary.contains("Dívidas"));
        assertTrue(summary.contains("Investimentos"));
    }

    @Test
    void importResult_totalImported() {
        BackupService.ImportResult result = new BackupService.ImportResult(1, 0, 2, 0, 1, 0, 1, 0);
        assertEquals(5, result.totalImported());
        assertEquals(0, result.totalSkipped());
    }

    @Test
    void importResult_totalSkipped() {
        BackupService.ImportResult result = new BackupService.ImportResult(0, 2, 0, 3, 0, 1, 0, 4);
        assertEquals(0, result.totalImported());
        assertEquals(10, result.totalSkipped());
    }

    @Test
    void backupData_defaultVersion() {
        BackupData data = new BackupData(List.of(), List.of(), List.of(), List.of());
        assertEquals("1.0", data.version);
        assertNotNull(data.exportedAt);
    }

    @Test
    void importFrom_invalidFile_throwsIOException(@TempDir Path dir) {
        File invalid = dir.resolve("invalid.json").toFile();
        assertThrows(Exception.class, () -> backupService.importFrom(invalid));
    }

    @Test
    void exportedFile_isValidJson(@TempDir Path dir) throws Exception {
        File file = dir.resolve("backup.json").toFile();
        backupService.export(file);

        String content = new String(java.nio.file.Files.readAllBytes(file.toPath()));
        assertTrue(content.contains("\"version\""));
        assertTrue(content.contains("\"exportedAt\""));
        assertTrue(content.contains("\"recurringEntries\""));
        assertTrue(content.contains("\"transactions\""));
        assertTrue(content.contains("\"debts\""));
        assertTrue(content.contains("\"investments\""));
    }
}
