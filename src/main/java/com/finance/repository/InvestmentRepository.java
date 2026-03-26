package com.finance.repository;

import com.finance.config.JPAUtil;
import com.finance.model.Investment;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

public class InvestmentRepository extends BaseRepository<Investment, Long> {

    public InvestmentRepository() {
        super(Investment.class);
    }

    public List<Investment> findByMonth(int year, int month) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.withDayOfMonth(start.lengthOfMonth());
            return em.createQuery(
                    "FROM Investment WHERE date >= :start AND date <= :end ORDER BY date",
                    Investment.class)
                    .setParameter("start", start)
                    .setParameter("end", end)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
