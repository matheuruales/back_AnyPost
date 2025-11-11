# Automatizacion

Small Spring Boot project for managing announcements. This repo includes a simple REST API built with Spring Boot, JPA (H2 for local dev), and DTO-based controllers.

## What I added for GitHub readiness
- `src/main/resources/application.properties.example` — template showing configuration keys and placeholders (do not store secrets in the repo).
- `.gitignore` — updated to ignore build artifacts, IDE metadata, OS files, and local secrets.
- `.github/workflows/maven.yml` — GitHub Actions workflow to build and run tests on push/PR.

## Quick start (local development)
Prerequisites:
- Java 17 (or the version configured in `pom.xml`)
- Maven (or use the included maven wrapper)

Run locally using the maven wrapper (Windows PowerShell):

```powershell
.\\mvnw.cmd spring-boot:run
```

Or build and run the jar:

```powershell
.\\mvnw.cmd -DskipTests package
java -jar target\\*.jar
```

The default configuration uses an in-memory H2 database and exposes the H2 console at `/h2-console`.

## Configuration & secrets
- Keep non-sensitive defaults in `application.properties` or `application.properties.example`.
- For production credentials (DB passwords, API keys), use environment variables or a secret manager.
- Use the `SPRING_PROFILES_ACTIVE` environment variable to switch profiles (for example `prod`).

Example env vars (PowerShell):

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://db-host:5432/anuncios"
$env:SPRING_DATASOURCE_USERNAME = "dbuser"
$env:SPRING_DATASOURCE_PASSWORD = "secret"
.\\mvnw.cmd spring-boot:run
```

## CI / GitHub Actions
A simple CI workflow is included that builds the project and runs tests on pushes and pull requests to `main`. Expand it to add linting, code scanning, or deployment steps.

## Deploying to Azure
Recommended path for small Spring Boot apps: Azure App Service (Linux) using the packaged JAR, or containerize and push to Azure Container Registry + Web App for Containers.

Basic flow:
1. Build package locally or in CI (`mvn -DskipTests package`).
2. In Azure, create a Resource Group, App Service Plan, and a Web App configured to run Java 17.
3. Configure production `SPRING_DATASOURCE_*` env vars in the App Service Configuration.
4. Deploy the JAR via GitHub Actions or `az webapp deploy`.

I can add a sample GitHub Actions deploy step for App Service or a Dockerfile + ACR workflow if you want container deployment.

## Contributing
1. Fork or branch.
2. Run tests locally: `mvn test`.
3. Open a pull request.

## Notes
- Do not commit production secrets. If secrets were accidentally committed, seek help to remove them from the repo history.
# AnyPost_Backendd
