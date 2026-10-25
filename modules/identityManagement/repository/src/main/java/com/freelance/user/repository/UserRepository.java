package com.freelance.user.repository;

import com.freelance.user.entity.User;
import com.freelance.rest.app.repository.AbstractRepository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class UserRepository extends AbstractRepository<User, UUID> {

    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return executeSingleResultSearch(User.QUERY_FIND_BY_USERNAME, Map.of("username", username));
    }

    public Optional<User> findById(UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(get(id));
    }

    public boolean existsByUsername(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return findByUsername(username).isPresent();
    }

    public long countUsers() {
        Long count = em().createNamedQuery(User.QUERY_COUNT_ALL, Long.class).getSingleResult();
        return count != null ? count : 0L;
    }

    public java.util.List<User> findAllUsers() {
        return em().createQuery("SELECT u FROM User u ORDER BY u.createdDate DESC", User.class).getResultList();
    }

    public java.util.List<User> findByIds(java.util.List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return em().createQuery("SELECT u FROM User u WHERE u.id IN :ids ORDER BY u.createdDate DESC", User.class)
                .setParameter("ids", ids)
                .getResultList();
    }

    public void ensureUserExists(UUID userId, String phone) {
        ensureUserExists(userId, phone, null);
    }

    public void ensureUserExists(UUID userId, String phone, String customerName) {
        if (userId == null) return;
        try {
            Number count = (Number) em()
                    .createNativeQuery("SELECT count(*) FROM fl_users WHERE id = :userId")
                    .setParameter("userId", userId)
                    .getSingleResult();
            java.time.Instant now = java.time.Instant.now();
            String firstName = "WhatsApp";
            String lastName = phone != null ? phone : "Lead";
            if (customerName != null && !customerName.isBlank()) {
                String[] parts = customerName.trim().split("\\s+", 2);
                firstName = parts[0];
                lastName = parts.length > 1 ? parts[1] : "";
            }

            if (count != null && count.intValue() == 0) {
                String safeUsername = "mem_" + (phone != null ? phone.replaceAll("[^0-9]", "") : userId.toString().substring(0, 8));
                em().createNativeQuery("INSERT INTO fl_users (id, username, password, first_name, last_name, created_by, created_date) VALUES (:id, :username, :password, :firstName, :lastName, :createdBy, :createdDate)")
                        .setParameter("id", userId)
                        .setParameter("username", safeUsername)
                        .setParameter("password", "NOPASSWORD")
                        .setParameter("firstName", firstName)
                        .setParameter("lastName", lastName)
                        .setParameter("createdBy", userId)
                        .setParameter("createdDate", now)
                        .executeUpdate();
            } else if (customerName != null && !customerName.isBlank()) {
                em().createNativeQuery("UPDATE fl_users SET first_name = :firstName, last_name = :lastName WHERE id = :userId AND (first_name = 'WhatsApp' OR first_name IS NULL)")
                        .setParameter("firstName", firstName)
                        .setParameter("lastName", lastName)
                        .setParameter("userId", userId)
                        .executeUpdate();
            }

            // Always ensure the user has the 'MEMBER' user type association so they appear in gym members directory as a Prospect
            try {
                em().createNativeQuery("INSERT INTO fl_user_user_type (id, user_id, user_type_id, created_by, created_date) " +
                                "SELECT :id, :userId, id, :createdBy, :createdDate FROM fl_user_type WHERE code = 'MEMBER' " +
                                "ON CONFLICT (user_id, user_type_id) DO NOTHING")
                        .setParameter("id", UUID.randomUUID())
                        .setParameter("userId", userId)
                        .setParameter("createdBy", userId)
                        .setParameter("createdDate", now)
                        .executeUpdate();
            } catch (Exception ignored) {
            }
        } catch (Exception ignored) {
        }
    }
}

