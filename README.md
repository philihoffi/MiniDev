# MiniDev

MiniDev besteht aus einem Spring-Boot-Backend und einem Angular-Frontend.
Das Frontend wird beim Build in die statischen Ressourcen des Backends geschrieben, sodass die App auch als ein gemeinsames Deployable gestartet werden kann.

## Projektstruktur

- `minidev-backend`: Java/Spring Boot API + Hosting der gebauten Frontend-Dateien
- `minidev-frontend`: Angular App
- `logs/`: Laufzeit-Logs

## Voraussetzungen

- Java 25+
- Maven 3.9+
- Node.js (für lokale Frontend-Entwicklung)
- PostgreSQL (für lokalen Backend-Betrieb)

## Schnellstart (integriert auf Port 8080)

Im integrierten Modus baut Maven das Frontend automatisch und legt die Artefakte unter `minidev-backend/src/main/resources/static` ab.

```powershell
Set-Location .\minidev-backend
.\mvnw.cmd clean install
.\mvnw.cmd spring-boot:run
```

Danach ist die App unter `http://localhost:8080` erreichbar.

## Entwicklung mit getrennten Servern

Backend (Port 8080):

```powershell
Set-Location .\minidev-backend
.\mvnw.cmd spring-boot:run
```

Frontend (Port 4200):

```powershell
Set-Location .\minidev-frontend
npm install
npm start
```

API-Aufrufe auf `/api` werden im Frontend per `minidev-frontend/proxy.conf.json` an `http://localhost:8080` weitergeleitet.

## Wichtige Umgebungsvariablen

Konfiguriert über `minidev-backend/src/main/resources/application.properties`:

- `POSTGRES_HOST`
- `POSTGRES_PORT`
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `OPENROUTER_API_KEY`

## Nützliche Build-Optionen

- Frontend-Build im Maven-Lauf überspringen:

```powershell
Set-Location .\minidev-backend
.\mvnw.cmd clean verify -DskipFrontendBuild=true
```

## Weitere Dokumentation

- `minidev-backend/README.md`
- `minidev-frontend/README.md`
