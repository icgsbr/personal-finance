package com.finance.repository;

import com.finance.config.JPAUtil;
import com.finance.model.Transaction;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

public class TransactionRepository extends BaseRepository<Transaction, Long> {

    public TransactionRepository() {
        super(Transaction.class);
    }

    public List<Transaction> findByMonth(int year, int month) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            return em.createQuery(
                    "FROM Transaction WHERE date >= :start AND date <= :end AND investment = false ORDER BY date",
                    Transaction.class)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Transaction> findInvestmentsByMonth(int year, int month) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            return em.createQuery(
                    "FROM Transaction WHERE date >= :start AND date <= :end AND investment = true ORDER BY date",
                    Transaction.class)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    public List<Transaction> findAllInvestments() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("FROM Transaction WHERE investment = true ORDER BY date DESC", Transaction.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
