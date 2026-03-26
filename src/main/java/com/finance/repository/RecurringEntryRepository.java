package com.finance.repository;

import com.finance.config.JPAUtil;
import com.finance.model.RecurringEntry;
import com.finance.model.EntryType;
import jakarta.persistence.EntityManager;

import java.util.List;

public class RecurringEntryRepository extends BaseRepository<RecurringEntry, Long> {

    public RecurringEntryRepository() {
        super(RecurringEntry.class);
    }

    public List<RecurringEntry> findAllActive() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("FROM RecurringEntry WHERE active = true", RecurringEntry.class).getResultList();
        } finally {
            em.close();
        }
    }

    public List<RecurringEntry> findActiveByType(EntryType type) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("FROM RecurringEntry WHERE active = true AND type = :type", RecurringEntry.class)
                    .setParameter("type", type)
                    .getResultList();
        } finally {
            em.close();
        }
    }
}
