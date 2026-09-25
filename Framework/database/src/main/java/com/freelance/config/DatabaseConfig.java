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
        if (args.length < 3) {
            throw new IllegalArgumentException(
                    "Expected arguments: <db-url> <db-username> <db-password> [PROD | TEST]"
            );
        }

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
}