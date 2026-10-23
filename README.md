# CodeCompass — Developer Support Chatbot

CodeCompass is a Java 25 / Spring Boot application with a responsive chat interface, an in-memory conversation history, and a Gemini-backed developer support assistant. It uses Spring AI's Google GenAI integration and keeps the Gemini key on the server.

## Requirements

- JDK 25
- Maven 3.6.3 or later
- A Gemini API key from [Google AI Studio](https://aistudio.google.com/app/apikey) to enable AI answers

Without `GEMINI_API_KEY`, the app still starts and labels its built-in troubleshooting replies as offline guidance. With a key configured, provider failures return an error instead of a misleading offline answer.

## Configure and run on Windows PowerShell

Set the key in the current PowerShell session (replace the example with your own key):

```powershell
$env:GEMINI_API_KEY = "YOUR_GEMINI_API_KEY"
```

The default model is `gemini-2.5-flash`. To select a different model available to your key, set `GEMINI_MODEL` before starting the app:

```powershell
$env:GEMINI_MODEL = "gemini-2.5-flash"
mvn spring-boot:run
```

Open <http://localhost:8080>. Do not paste the key into the frontend or commit it to a properties file. For a persistent Windows user environment variable, use `setx GEMINI_API_KEY "YOUR_GEMINI_API_KEY"` in PowerShell, then open a new terminal. Keep the key private; rotate it in Google AI Studio if it has been exposed.

## API

- `POST /api/chat` — request: `{"sessionId":"optional-id","message":"How do I fix this Spring error?"}`. Response shape: `{"sessionId":"...","reply":{"role":"assistant","content":"...","timestamp":123},"messages":[...]}`. A new UUID session is generated when the ID is omitted.
- `GET /api/chat/{sessionId}` — read stored messages.
- `DELETE /api/chat/{sessionId}` — clear that session.

Example:

```powershell
$body = @{ message = "Explain dependency injection in Spring Boot" } | ConvertTo-Json
Invoke-RestMethod -Uri http://localhost:8080/api/chat -Method Post -ContentType "application/json" -Body $body
```

Empty messages are rejected with HTTP 400; messages over 12,000 characters receive HTTP 413. Gemini provider errors return a safe HTTP 502 or 503 JSON error. Request context is limited to 12 prior messages and 24,000 characters. Each conversation is kept in memory and is lost when the application restarts; session IDs are not an authentication mechanism.

## Build and test

```powershell
mvn test
mvn package
```

Automated tests mock the AI response and do not need a Gemini key or consume model quota.
