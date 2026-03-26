package com.finance.repository;

import com.finance.config.JPAUtil;
import com.finance.model.Debt;
import jakarta.persistence.EntityManager;

import java.util.List;

public class DebtRepository extends BaseRepository<Debt, Long> {

    public DebtRepository() {
        super(Debt.class);
    }

    public List<Debt> findAllActive() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery(
                    "FROM Debt WHERE paidInstallments < totalInstallments", Debt.class)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
