package com.authmodule.auth;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {

    private final EntityManager em;

    public UserRepository(EntityManager em) {
        this.em = em;
    }

    public UserEntity createUser(String passwordHash) {
        UserEntity user = new UserEntity(UUID.randomUUID(), passwordHash, Instant.now());
        em.persist(user);
        return user;
    }

    public Optional<UserEntity> findUserById(UUID id) {
        return Optional.ofNullable(em.find(UserEntity.class, id));
    }

    public Optional<UserEntity> findUserByLogin(AuthModels.IdentifierType type, String normalizedValue) {
        try {
            LoginIdentifierEntity identifier = em.createQuery(
                            """
                                    SELECT i FROM LoginIdentifierEntity i
                                    WHERE i.type = :type
                                      AND i.normalizedValue = :normalizedValue
                                      AND i.active = true
                                    """,
                            LoginIdentifierEntity.class
                    )
                    .setParameter("type", type)
                    .setParameter("normalizedValue", normalizedValue)
                    .getSingleResult();
            return findUserById(identifier.getUserId());
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public LoginIdentifierEntity saveIdentifier(LoginIdentifierEntity identifier) {
        em.persist(identifier);
        return identifier;
    }

    public Optional<LoginIdentifierEntity> findIdentifier(
            UUID userId,
            AuthModels.IdentifierType type
    ) {
        try {
            return Optional.of(
                    em.createQuery(
                                    """
                                            SELECT i FROM LoginIdentifierEntity i
                                            WHERE i.userId = :userId AND i.type = :type
                                            """,
                                    LoginIdentifierEntity.class
                            )
                            .setParameter("userId", userId)
                            .setParameter("type", type)
                            .getSingleResult()
            );
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public List<LoginIdentifierEntity> findIdentifiersByUserId(UUID userId) {
        return em.createQuery(
                        """
                                SELECT i FROM LoginIdentifierEntity i
                                WHERE i.userId = :userId
                                ORDER BY i.type
                                """,
                        LoginIdentifierEntity.class
                )
                .setParameter("userId", userId)
                .getResultList();
    }

    public void updateIdentifier(LoginIdentifierEntity identifier) {
        em.merge(identifier);
    }

    public void deleteIdentifier(LoginIdentifierEntity identifier) {
        LoginIdentifierEntity managed = em.contains(identifier) ? identifier : em.merge(identifier);
        em.remove(managed);
    }

    public Optional<UsernameReservationEntity> findUsernameReservation(String normalizedUsername) {
        return Optional.ofNullable(em.find(UsernameReservationEntity.class, normalizedUsername));
    }

    public void reserveUsername(String normalizedUsername, UUID ownerUserId) {
        if (em.find(UsernameReservationEntity.class, normalizedUsername) == null) {
            em.persist(new UsernameReservationEntity(normalizedUsername, ownerUserId));
        }
    }

    public PasswordResetTokenEntity createPasswordResetToken(
            UUID userId,
            String tokenHash,
            Instant expiresAt
    ) {
        PasswordResetTokenEntity token = new PasswordResetTokenEntity(
                UUID.randomUUID(),
                userId,
                tokenHash,
                expiresAt,
                false
        );
        em.persist(token);
        return token;
    }

    public Optional<PasswordResetTokenEntity> findPasswordResetTokenByHash(String tokenHash) {
        try {
            return Optional.of(
                    em.createQuery(
                                    """
                                            SELECT t FROM PasswordResetTokenEntity t
                                            WHERE t.tokenHash = :tokenHash
                                            """,
                                    PasswordResetTokenEntity.class
                            )
                            .setParameter("tokenHash", tokenHash)
                            .getSingleResult()
            );
        } catch (NoResultException e) {
            return Optional.empty();
        }
    }

    public void updatePassword(UserEntity user, String passwordHash) {
        user.setPasswordHash(passwordHash);
        em.merge(user);
    }

    public boolean identifierExists(AuthModels.IdentifierType type, String normalizedValue) {
        Long count = em.createQuery(
                        """
                                SELECT COUNT(i) FROM LoginIdentifierEntity i
                                WHERE i.type = :type AND i.normalizedValue = :normalizedValue
                                """,
                        Long.class
                )
                .setParameter("type", type)
                .setParameter("normalizedValue", normalizedValue)
                .getSingleResult();
        return count != null && count > 0;
    }
}
