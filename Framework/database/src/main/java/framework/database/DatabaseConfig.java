package framework.database;

public record DatabaseConfig(
        String url,
        String username,
        String password
) {
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