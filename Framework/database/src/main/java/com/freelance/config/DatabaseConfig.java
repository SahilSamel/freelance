package com.freelance.config;

import com.freelance.enums.DeploymentMode;

public record DatabaseConfig(
        String url,
        String username,
        String password,
        DeploymentMode mode
) {
    public DatabaseConfig(String url, String username, String password) {
        this(url, username, password, DeploymentMode.PROD);
    }

    public static DatabaseConfig fromArgs(String[] args) {
        if (args == null) {
            throw new NullPointerException("Arguments array cannot be null");
        }

        if (args.length >= 3) {
            DeploymentMode mode = (args.length >= 4)
                    ? DeploymentMode.fromString(args[3])
                    : DeploymentMode.PROD;

            return new DatabaseConfig(
                    args[0],
                    args[1],
                    args[2],
                    mode
            );
        }

        return fromEnv();
    }

    public static DatabaseConfig fromEnv() {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String pass = System.getenv("DB_PASSWORD");
        String modeStr = System.getenv("DEPLOYMENT_MODE");

        if (url != null && !url.isBlank() && user != null && !user.isBlank() && pass != null) {
            DeploymentMode mode = (modeStr != null && !modeStr.isBlank())
                    ? DeploymentMode.fromString(modeStr)
                    : DeploymentMode.PROD;

            return new DatabaseConfig(url, user, pass, mode);
        }

        throw new IllegalArgumentException(
                "Database configuration missing! Provide CLI arguments: <db-url> <db-username> <db-password> [PROD | TEST] " +
                "OR set environment variables: DB_URL, DB_USER, DB_PASSWORD, [DEPLOYMENT_MODE]"
        );
    }
}