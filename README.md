# CodeCompass

CodeCompass is a developer support chatbot built with Java 25 and Spring Boot. It provides a web chat interface, Gemini-backed answers when configured, and offline troubleshooting guidance when no AI key is available. The same application includes account authentication, projects, saved conversations, and developer workspace tools.

## Features

- Registration, email verification, login, logout, password reset, and profile settings.
- BCrypt password hashing, server-side sessions, CSRF protection, one-time email tokens, and login attempt throttling.
- Projects, conversation history, search, branches, checkpoints, pinned context, and collaborators.
- Code analysis endpoints for AST summaries, diffs, and multi-model responses.
- User-managed API keys, preferences, and usage summaries.
- Gemini assistance with offline troubleshooting fallback.
- H2 file database for local development, with MySQL and PostgreSQL/Supabase driver support.

## Requirements

- JDK 25
- Maven 3.9 or newer

This repository does not include a Maven Wrapper (`mvnw`). Install Maven and make `mvn` available on `PATH`, or invoke an existing `mvn.cmd` by its full path.

## Run on Windows

Open PowerShell in the project directory. If you installed Temurin JDK 25 in the default location, set Java for this terminal:

```powershell
$jdk = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-25*' | Select-Object -First 1
if (-not $jdk) { throw 'JDK 25 was not found under C:\Program Files\Eclipse Adoptium' }
$env:JAVA_HOME = $jdk.FullName
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
mvn -version
```

Both Java and Maven should report Java 25. If `mvn` is not on `PATH`, locate an existing Maven command in the local Maven Wrapper cache:

```powershell
$mvn = Get-ChildItem "$env:USERPROFILE\.m2\wrapper\dists" -Filter 'mvn.cmd' -Recurse | Select-Object -First 1 -ExpandProperty FullName
if (-not $mvn) { throw 'Maven was not found; install Maven 3.9 or newer.' }
```

Set any environment variables described below in this same PowerShell window, then start the server:

```powershell
mvn spring-boot:run
```

If using the cached Maven command instead:

```powershell
& $mvn spring-boot:run
```

Open <http://localhost:8080>. Keep the terminal open while using CodeCompass; press `Ctrl+C` there to stop it.

## Gemini configuration (optional)

Without an API key, CodeCompass uses its offline troubleshooting guidance. To enable Gemini, set the key before starting the app:

```powershell
$env:GEMINI_API_KEY = 'YOUR_GEMINI_API_KEY'
$env:GEMINI_MODEL = 'gemini-2.5-flash'
```

Create or manage a key in [Google AI Studio](https://aistudio.google.com/app/apikey). Keep real keys in environment variables or a secret manager; do not put them in this README or commit them.

## Email and SMTP configuration

Email is used for account verification and password reset. SMTP settings are read from environment variables when the app starts. For Gmail, use an App Password; a normal Google account password will not work. Google requires 2-Step Verification for App Passwords.

In PowerShell, set the non-secret settings (replace the example address):

```powershell
$env:MAIL_HOST = 'smtp.gmail.com'
$env:MAIL_PORT = '587'
$env:MAIL_USERNAME = 'your-address@gmail.com'
$env:MAIL_FROM = $env:MAIL_USERNAME
$env:MAIL_SMTP_AUTH = 'true'
$env:MAIL_SMTP_STARTTLS = 'true'
$env:APP_BASE_URL = 'http://localhost:8080'
```

Enter the App Password at the secure prompt. Do not append it to the command or place it in `application.properties`:

```powershell
$securePassword = Read-Host -Prompt 'SMTP App Password' -AsSecureString
$env:MAIL_PASSWORD = [System.Net.NetworkCredential]::new('', $securePassword).Password.Replace(' ', '')
Remove-Variable securePassword
```

The default `MAIL_DEV_FALLBACK=true` prints verification or reset links to the server console if SMTP is not configured or sending fails. This is for local development. Set `$env:MAIL_DEV_FALLBACK = 'false'` before starting the app to require real SMTP delivery and return categorized SMTP errors. Use a fresh App Password if a credential has been exposed; revoke exposed passwords in your Google Account first.

For SMTP providers other than Gmail, replace the host, port, username, password, sender, and TLS settings with that provider's values.

## Database configuration

By default, the app stores data in a local H2 file under `./data/`. That directory is intentionally ignored by Git and should not be committed.

To use MySQL, set `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` before starting. For example:

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/codecompass'
$env:DB_USERNAME = 'your-database-user'
$env:DB_PASSWORD = 'your-database-password'
```

The optional `supabase` profile uses PostgreSQL. Activate it with `$env:SPRING_PROFILES_ACTIVE = 'supabase'` and configure `SUPABASE_DB_URL`, `SUPABASE_DB_USERNAME`, and `SUPABASE_DB_PASSWORD`. Optional Supabase API integration settings are `SUPABASE_URL`, `SUPABASE_ANON_KEY`, `SUPABASE_PUBLISHABLE_KEY`, and `SUPABASE_SECRET_KEY`.

Flyway applies migrations from `src/main/resources/db/migration` at application startup.

## API overview

- Authentication and account actions: `/api/auth/*`
- Projects and conversations: `/api/projects/*`, `/api/conversations/*`
- Chat: `POST /api/chat`, `GET|DELETE /api/chat/{conversationId}`
- Settings, API keys, preferences, and usage: `/api/settings/*`
- Workspace tools: `/api/conversations/{id}/branches`, `/api/conversations/{id}/checkpoints`, `/api/context`, and `/api/analysis/*`

State-changing browser requests use CSRF protection. Protected records are scoped to the signed-in account.

## Build and test

```powershell
mvn test
mvn package
```

The tests use an in-memory database and mock external AI and email providers; they do not send real email or require a Gemini key.
