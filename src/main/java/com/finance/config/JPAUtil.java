package com.finance.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JPAUtil {

    private static final EntityManagerFactory emf = buildEmf();

    private static EntityManagerFactory buildEmf() {
        java.nio.file.Path dataDir = resolveDataDir();
        try {
            java.nio.file.Files.createDirectories(dataDir);
        } catch (java.io.IOException e) {
            throw new RuntimeException("Não foi possível criar o diretório de dados: " + dataDir, e);
        }
        String dbUrl = "jdbc:h2:file:" + dataDir.resolve("finance") + ";AUTO_SERVER=TRUE";
        java.util.Map<String, String> props = new java.util.HashMap<>();
        props.put("jakarta.persistence.jdbc.url", dbUrl);
        return Persistence.createEntityManagerFactory("personal-finance", props);
    }

    private static java.nio.file.Path resolveDataDir() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");
        if (os.contains("mac")) {
            return java.nio.file.Paths.get(home, "Library", "Application Support", "FinancasPessoais");
        } else if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            return java.nio.file.Paths.get(appData != null ? appData : home, "FinancasPessoais");
        } else {
            return java.nio.file.Paths.get(home, ".financas-pessoais");
        }
    }

    public static EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public static void close() {
        if (emf != null && emf.isOpen()) emf.close();
    }
}
