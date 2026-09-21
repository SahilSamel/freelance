# Freelance Backend API Server

Modular Java & JAX-RS backend application with PostgreSQL, Flyway migrations, Hibernate/JPA, and RBAC authentication.

---

## 1. Database Configuration & Prerequisites

Before starting the server, ensure PostgreSQL is running with the target database:

- **Host**: `localhost`
- **Port**: `5432`
- **Database**: `freelance_db`
- **Schema**: `freelance` (Created automatically via Flyway migrations)
- **User**: `postgres` (or your configured user)
- **Password**: `<your_postgres_password>`

---

## 2. Program Arguments for `Main`

The entry point [`com.freelance.launcher.Main`](Launcher/src/main/java/com/freelance/launcher/Main.java) accepts command-line arguments in the following order:

```text
<db-url> <db-username> <db-password> [DEV | TEST | PROD]
```

### Argument Reference

| Index | Argument | Example Value | Description |
| :--- | :--- | :--- | :--- |
| **0** | `db-url` | `jdbc:postgresql://localhost:5432/freelance_db` | PostgreSQL JDBC connection URL pointing to `freelance_db` |
| **1** | `db-username` | `postgres` | Database username |
| **2** | `db-password` | `welcome1` | Database password |
| **3** | `mode` *(optional)* | `DEV` *(default)*, `TEST`, `PROD` | Deployment mode (`DEV` preserves data; `TEST` wipes & rebuilds clean Day-0 DB) |

---

## 3. Starting the Application

### Option A: Testing Mode (`TEST` — Clean Slate / Recommended for Integration Tests)
Wipes and rebuilds the database schema fresh with Flyway, executes the Day-0 Bootstrap Ceremony, and starts the HTTPS server on port `8080`.

**PowerShell (Windows)**:
```powershell
.\gradlew :Launcher:run --args="jdbc:postgresql://localhost:5432/freelance_db postgres welcome1 TEST"
```

**Bash (Linux / macOS / WSL)**:
```bash
./gradlew :Launcher:run --args="jdbc:postgresql://localhost:5432/freelance_db postgres welcome1 TEST"
```

---

### Option B: Development Mode (`DEV` — Preserves Existing Data)
Applies incremental migrations without dropping existing database data.

**PowerShell (Windows)**:
```powershell
.\gradlew :Launcher:run --args="jdbc:postgresql://localhost:5432/freelance_db postgres welcome1 DEV"
```

**Bash (Linux / macOS / WSL)**:
```bash
./gradlew :Launcher:run --args="jdbc:postgresql://localhost:5432/freelance_db postgres welcome1 DEV"
```

---

## 4. Running the Test Suite & Coverage

While the application server is running, open a separate terminal in the project root to run the test suite:

### Run Full Test Suite (Unit + Integration + Unified JaCoCo Report)

**PowerShell (Windows)**:
```powershell
.\gradlew test :testing:integrationTest :testing:jacocoIntegrationReport --no-daemon
```

**Bash (Linux / macOS / WSL)**:
```bash
./gradlew test :testing:integrationTest :testing:jacocoIntegrationReport --no-daemon
```

### Targeted Test Commands

| Command | Description |
| :--- | :--- |
| `.\gradlew test` | Runs **Unit Tests only** across all 15 submodules |
| `.\gradlew :testing:integrationTest` | Runs **Integration Tests only** (against running server) |
| `.\gradlew :testing:jacocoIntegrationReport` | Generates the **Unified JaCoCo Code Coverage Report** |
| `.\gradlew clean test :testing:integrationTest :testing:jacocoIntegrationReport` | **Clean & Re-run Everything Fresh** (clears report & execution caches) |

---

## 5. Test & Coverage Reports

After running the tests, view the generated HTML reports:

| Report | File Location |
| :--- | :--- |
| **JaCoCo Unified Coverage Report** | `testing/build/reports/coverage/html/index.html` |
| **Integration Test Execution Report** | `testing/build/reports/tests/integrationTest/index.html` |
| **Submodule Unit Test Reports** | `<submodule>/build/reports/tests/test/index.html` |

---

## 6. Default Pre-Seeded Super Admin Credentials

When the database is migrated on startup:
- **Username**: `superadmin`
- **Password**: `Welcome@1`
- **Assigned Role**: `SUPER_ADMIN`

---

## 7. Server & SSL Info

- **Server URL**: `https://localhost:8080`
- **KeyStore**: `Launcher/src/main/resources/keystore/keystore.jks` (Password: `changeit`)
- **Key Modules**:
  - `Authentication` (`/auth/login`, `/auth/refresh`, `/auth/logout`)
  - `Identity Management` (`/users/create`, `/users/read`, `/users/update`, `/users/delete`)
  - `Authorization & RBAC` (`/authorizations/list`, `/roles/*`)
  - `Configurations` (`/configurations/system/*`, `/configurations/error/*`)
  - `Alerts & Templates` (`/alerts/templates/*`, `/alerts/attributes/*`, `/alerts/emit`)
  - `Audit Logging` (`/audit/list`)