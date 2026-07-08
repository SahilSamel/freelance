package com.freelance.config;

/**
 * Immutable database connection settings, typically supplied at
 * application boot via command-line arguments.
 */
public record DatabaseConfig(
        String url,
        String username,
        String password
) {
    /**
     * Parses database connection settings from launcher arguments.
     *
     * @param args expected as {@code <db-url> <db-username> <db-password>}
     * @return the parsed DatabaseConfig
     * @throws IllegalArgumentException if fewer than 3 arguments are provided
     */
    public static DatabaseConfig fromArgs(String[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException(
                    "Expected arguments: <db-url> <db-username> <db-password>"
            );
        }

        return new DatabaseConfig(
                args[0],
                args[1],
                args[2]
        );
    }
}