package com.authmodule.social;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Idempotent default-profile creation for registration and lazy repair paths.
 * Does not depend on {@code AuthService} (avoids circular wiring).
 */
@Repository
public class ProfileProvisioningRepository {

    private final EntityManager em;

    public ProfileProvisioningRepository(EntityManager em) {
        this.em = em;
    }

    /**
     * Returns the existing profile, or persists {@link Profile#empty(UUID)} when missing.
     * Concurrent creates are resolved via the {@code profiles.user_id} primary key.
     */
    @Transactional
    public Profile createIfMissing(UUID userId) {
        Profile existing = em.find(Profile.class, userId);
        if (existing != null) {
            return existing;
        }
        try {
            Profile created = Profile.empty(userId);
            em.persist(created);
            em.flush();
            return created;
        } catch (PersistenceException | DataIntegrityViolationException ex) {
            if (!isDuplicateKey(ex)) {
                throw ex;
            }
            em.clear();
            Profile raced = em.find(Profile.class, userId);
            if (raced != null) {
                return raced;
            }
            throw ex;
        }
    }

    private static boolean isDuplicateKey(RuntimeException ex) {
        Throwable cursor = ex;
        while (cursor != null) {
            if (cursor instanceof ConstraintViolationException
                    || cursor instanceof org.springframework.dao.DuplicateKeyException
                    || cursor instanceof jakarta.persistence.EntityExistsException) {
                return true;
            }
            String message = cursor.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("duplicate")
                        || lower.contains("unique")
                        || lower.contains("primary key")
                        || lower.contains("already exists")) {
                    return true;
                }
            }
            cursor = cursor.getCause();
        }
        return false;
    }
}
