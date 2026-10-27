# CodeCompass — Developer Support Chatbot

CodeCompass is a Java 25 and Spring Boot developer support application. It combines a Gemini-backed assistant and offline troubleshooting guidance with account authentication, project workspaces, and persistent conversation history. The browser UI is served by Spring Boot; the Gemini key stays on the server.

## Requirements

- JDK 25
- Maven 3.9+
- Optional: a Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey)
- SMTP configuration for account verification and password reset emails

The application uses a persistent H2 file database by default (`./data/codecompass`). Configure a MySQL database using `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD` when deploying or sharing data across instances. Flyway applies schema migrations from `src/main/resources/db/migration`.

## Run on Windows PowerShell

If Maven is not on `PATH`, use the Maven installation directly. This searches the Maven Wrapper cache used by the project setup. Set `JAVA_HOME` to the actual JDK installation directory (not a placeholder):

```powershell
$jdk = Get-ChildItem 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-25*' | Select-Object -First 1
$env:JAVA_HOME = $jdk.FullName
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
$mvn = Get-ChildItem "$env:USERPROFILE\.m2\wrapper\dists" -Filter 'mvn.cmd' -Recurse | Select-Object -First 1
```

Optionally enable Gemini responses:

```powershell
$env:GEMINI_API_KEY = 'YOUR_GEMINI_API_KEY'
$env:GEMINI_MODEL = 'gemini-2.5-flash'
```

### Email & SMTP Configuration

CodeCompass includes a **development fallback mode** by default (`codecompass.mail.dev-fallback=true`). If you do not have SMTP credentials configured or if email delivery fails, the registration verification links and password reset links are printed directly to the terminal/console so you can click them immediately and never get blocked during local development.

To send real emails using **Gmail SMTP**:
1. Enable 2-Step Verification on your Google Account: <https://myaccount.google.com/security>
2. Generate a 16-character **App Password**: <https://myaccount.google.com/apppasswords> (Select app name e.g. "CodeCompass").
3. Set your credentials in PowerShell before starting the app, or in `application.properties`:

```powershell
$env:MAIL_HOST = 'smtp.gmail.com'
$env:MAIL_PORT = '587'
$env:MAIL_USERNAME = 'your-email@gmail.com'
$env:MAIL_PASSWORD = 'xxxx xxxx xxxx xxxx'  # 16-character Google App Password (not your Gmail account password)
$env:MAIL_FROM = 'your-email@gmail.com'
$env:APP_BASE_URL = 'http://localhost:8080'
```

For strict production deployments where email delivery failures must reject registration, set `$env:MAIL_DEV_FALLBACK = 'false'`.

Start the application:

```powershell
& $mvn.FullName spring-boot:run
```

Open <http://localhost:8080>. Do not commit SMTP passwords or API keys to version control. Set `SESSION_COOKIE_SECURE=true` when serving the application over HTTPS. The H2 file is local development storage; use MySQL and an HTTPS URL for a multi-user deployment.

## Features

- Email and password registration with email verification; login, logout, forgot/reset password, and profile/password settings.
- New passwords must be 6–18 characters. Existing BCrypt password hashes remain compatible; new password input is also checked against BCrypt's 72-byte limit to prevent truncation-based matches.
- Server-side sessions with HttpOnly cookies, CSRF protection, BCrypt password hashes, one-time hashed email tokens, and login attempt throttling.
- Projects with descriptions and bounded project instructions, searchable conversations, move between projects, and owner-only access.
- Persistent conversations and messages. Deleting a project detaches its conversations; deleting a conversation removes its messages.
- Gemini answers when configured and offline troubleshooting guidance otherwise.

## HTTP API

Authentication endpoints are under `/api/auth`: `GET /csrf`, `POST /register`, `POST /login`, `POST /logout`, `GET /me`, `POST /verify-email`, `POST /resend-verification`, `POST /forgot-password`, `POST /reset-password`, `PATCH /profile`, and `POST /change-password`.

Protected project and conversation endpoints:

- `GET|POST /api/projects`
- `GET|PATCH|DELETE /api/projects/{projectId}`
- `GET /api/conversations?search=...` and `GET /api/projects/{projectId}/conversations?search=...`
- `PATCH|DELETE /api/conversations/{conversationId}`
- `POST /api/chat` with `{ "sessionId": "optional-conversation-id", "message": "...", "projectId": "optional-project-id" }`
- `GET|DELETE /api/chat/{conversationId}` to read or clear messages while retaining the conversation

The browser obtains a CSRF token from `/api/auth/csrf` and sends it in `X-XSRF-TOKEN` for state-changing requests. Protected resources are scoped to the signed-in account. Chat request context is limited to recent messages and a bounded character count; message size is capped at 12,000 characters.

## Build and test

```powershell
& $mvn.FullName test
& $mvn.FullName package
```

The integration tests use an in-memory H2 database and mock email/AI providers. They do not require live SMTP or a Gemini key.
