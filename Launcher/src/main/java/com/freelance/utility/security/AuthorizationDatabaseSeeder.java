package com.freelance.utility.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freelance.system.SystemHardcoding;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuthorizationDatabaseSeeder {

    private static final Logger logger = Logger.getLogger(AuthorizationDatabaseSeeder.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final UUID SYSTEM_USER_ID = SystemHardcoding.SYSTEM_USER_ID.asUuid();

    public void syncAuthorizationsToDb(Connection connection) {
        List<JsonNode> jsonEntries = loadAllAuthorizationsFromClasspath();

        if (jsonEntries.isEmpty()) {
            logger.info("[AuthorizationSeeder] No authorizations found on classpath.");
            return;
        }

        try {
            connection.setAutoCommit(false);

            // Step 1: Nuclear delete of all system roles and their mappings
            deleteSystemRolesAndMappings(connection);

            // Step 2: Upsert Authorizations and map keys to DB IDs
            Map<String, UUID> authKeyToIdMap = upsertAuthorizations(connection, jsonEntries);

            // Step 3: Insert active System Roles with deterministic UUIDs
            Map<String, UUID> roleNameToIdMap = createActiveSystemRoles(connection, jsonEntries);

            // Step 4: Map role_authorization links
            createRoleAuthorizationMappings(connection, jsonEntries, authKeyToIdMap, roleNameToIdMap);

            connection.commit();
            logger.info("[AuthorizationSeeder] Successfully re-synced system roles and authorizations!");

        } catch (Exception e) {
            logger.log(Level.SEVERE, "[AuthorizationSeeder] Sync failed! Rolling back...", e);
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                logger.log(Level.SEVERE, "Failed to rollback", rollbackEx);
            }
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException ignored) {}
        }
    }

    private void deleteSystemRolesAndMappings(Connection conn) throws SQLException {
        String deleteMappingsSql =
                "DELETE FROM freelance.FL_ROLE_AUTHORIZATION " +
                        "WHERE role_id IN (SELECT id FROM freelance.FL_ROLE WHERE is_system = TRUE)";
        try (PreparedStatement stmt = conn.prepareStatement(deleteMappingsSql)) {
            stmt.executeUpdate();
        }

        String deleteRolesSql = "DELETE FROM freelance.FL_ROLE WHERE is_system = TRUE";
        try (PreparedStatement stmt = conn.prepareStatement(deleteRolesSql)) {
            stmt.executeUpdate();
        }
    }

    private Map<String, UUID> upsertAuthorizations(Connection conn, List<JsonNode> nodes) throws SQLException {
        String upsertAuthSql =
            """
            INSERT INTO freelance.FL_AUTHORIZATION (id, key, name, description, group_path, created_date, last_updated) 
            VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP) 
            ON CONFLICT (key) DO UPDATE SET 
                name = EXCLUDED.name, 
                description = EXCLUDED.description, 
                group_path = EXCLUDED.group_path, 
                last_updated = CURRENT_TIMESTAMP
            """;

        try (PreparedStatement stmt = conn.prepareStatement(upsertAuthSql)) {
            for (JsonNode node : nodes) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setString(2, node.path("key").asText());
                stmt.setString(3, node.path("name").asText());
                stmt.setString(4, node.path("description").asText(null));
                stmt.setString(5, node.path("group").asText());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }

        Map<String, UUID> authMap = new HashMap<>();
        String selectSql = "SELECT id, key FROM freelance.FL_AUTHORIZATION";
        try (PreparedStatement stmt = conn.prepareStatement(selectSql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                authMap.put(rs.getString("key"), (UUID) rs.getObject("id"));
            }
        }
        return authMap;
    }

    private Map<String, UUID> createActiveSystemRoles(Connection conn, List<JsonNode> nodes) throws SQLException {
        Set<String> distinctRoles = new HashSet<>();
        for (JsonNode node : nodes) {
            String role = node.path("role").asText("");
            if (!role.isBlank()) {
                distinctRoles.add(role.trim());
            }
        }

        Map<String, UUID> roleMap = new HashMap<>();
        if (distinctRoles.isEmpty()) return roleMap;

        String insertRoleSql = """
            INSERT INTO freelance.FL_ROLE (id, name, description, is_system, created_by, last_updated_by) 
            VALUES (?, ?, ?, TRUE, ?, ?)
            """;

        try (PreparedStatement stmt = conn.prepareStatement(insertRoleSql)) {
            for (String roleName : distinctRoles) {
                // Deterministic UUID derived from role name — stays identical as long as roleName stays same
                UUID roleId = UUID.nameUUIDFromBytes(("role:" + roleName).getBytes(StandardCharsets.UTF_8));

                stmt.setObject(1, roleId);
                stmt.setString(2, roleName);
                stmt.setString(3, "System generated role for " + roleName);
                stmt.setObject(4, SYSTEM_USER_ID);
                stmt.setObject(5, SYSTEM_USER_ID);
                stmt.addBatch();

                roleMap.put(roleName, roleId);
            }
            stmt.executeBatch();
        }

        return roleMap;
    }

    private void createRoleAuthorizationMappings(
            Connection conn,
            List<JsonNode> nodes,
            Map<String, UUID> authMap,
            Map<String, UUID> roleMap
    ) throws SQLException {

        String insertMappingSql =
        """
        INSERT INTO freelance.FL_ROLE_AUTHORIZATION (role_id, authorization_id, created_date) 
        VALUES (?, ?, CURRENT_TIMESTAMP) 
        ON CONFLICT (role_id, authorization_id) DO NOTHING
        """;

        int mappingsCreated = 0;
        try (PreparedStatement stmt = conn.prepareStatement(insertMappingSql)) {
            for (JsonNode node : nodes) {
                String roleName = node.path("role").asText("");
                String authKey = node.path("key").asText();

                if (!roleName.isBlank()) {
                    UUID roleId = roleMap.get(roleName.trim());
                    UUID authId = authMap.get(authKey);

                    if (roleId != null && authId != null) {
                        stmt.setObject(1, roleId);
                        stmt.setObject(2, authId);
                        stmt.addBatch();
                        mappingsCreated++;
                    }
                }
            }
            if (mappingsCreated > 0) {
                stmt.executeBatch();
            }
        }
    }

    private List<JsonNode> loadAllAuthorizationsFromClasspath() {
        List<JsonNode> allEntries = new ArrayList<>();
        try {
            Enumeration<URL> resources = Thread.currentThread()
                    .getContextClassLoader()
                    .getResources("authorizations.json");

            while (resources.hasMoreElements()) {
                URL resourceUrl = resources.nextElement();
                try (InputStream inputStream = resourceUrl.openStream()) {
                    JsonNode rootArray = objectMapper.readTree(inputStream);
                    if (rootArray.isArray()) {
                        for (JsonNode item : rootArray) {
                            allEntries.add(item);
                        }
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Failed parsing " + resourceUrl, e);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error scanning classpath", e);
        }
        return allEntries;
    }
}