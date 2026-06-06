# MiniDev

[![Build](https://github.com/philihoffi/MiniDev/actions/workflows/build.yml/badge.svg)](https://github.com/philihoffi/MiniDev/actions/workflows/build.yml)
[![Lint](https://github.com/philihoffi/MiniDev/actions/workflows/lint.yml/badge.svg)](https://github.com/philihoffi/MiniDev/actions/workflows/lint.yml)
[![CodeQL](https://github.com/philihoffi/MiniDev/actions/workflows/codeql.yml/badge.svg)](https://github.com/philihoffi/MiniDev/actions/workflows/codeql.yml)
![Java 25](https://img.shields.io/badge/Java-25-blue)
![Angular 19](https://img.shields.io/badge/Angular-19-red)
![License](https://img.shields.io/badge/License-MIT-green)

MiniDev kombiniert ein Spring-Boot-Backend mit einem Angular-Frontend.
Beim Backend-Build wird das Frontend automatisch gebaut und in die statischen Ressourcen eingebettet, sodass die App als ein gemeinsames Deployable auf `:8080` laufen kann.

## Kurzuberblick

- `minidev-backend`: API, Persistenz, Security, Hosting der gebauten Frontend-Artefakte
- `minidev-frontend`: Angular-App fur UI und lokale Entwicklung auf `:4200`
- `docker-compose.yml`: Startet App + PostgreSQL fur den containerisierten Betrieb

## Tech Stack

- **Backend:** Java 25+, Spring Boot 4.0.5, Spring Data JPA, Spring Security
- **Frontend:** Angular 19, Tailwind CSS 3, TypeScript 5.7
- **Datenbank:** PostgreSQL 17
- **Build:** Maven (Backend), npm/Angular CLI (Frontend)
- **CI/CD:** GitHub Actions
- **Containerisierung:** Docker + Docker Compose
- **LLM-Integration:** OpenRouter API

## Voraussetzungen

- Docker Desktop mit Docker Compose (empfohlen)
- Java 25+
- Maven 3.9+
- Node.js
- PostgreSQL

## Schnellstart mit Docker (empfohlen)

1. Konfiguration anlegen:

```powershell
Copy-Item .env.example .env
```

2. Container bauen und starten:

```powershell
docker compose up --build
```

3. Anwendung offnen:

- App: `http://localhost:8080`
- Datenbank: intern als Host `postgres` im Compose-Netz
- PostgreSQL hat bewusst keinen veroffentlichten Host-Port

## Lokale Entwicklung

### Backend

```bash
cd minidev-backend
./mvnw clean install
./mvnw spring-boot:run
```

### Frontend

```bash
cd minidev-frontend
npm install
ng serve
```

Die Frontend-Entwicklungslauft auf `http://localhost:4200` und proxied API-Aufrufe an `localhost:8080`.

## Projektstruktur

```
MiniDev/
├── minidev-backend/
│   └── src/main/java/org/philipp/fun/minidev/
│       ├── audit/           # Request-Logging, ApiRequestLog-Entitten
│       ├── config/          # Spring-Konfiguration (Security, SPA-Routing, Init)
│       ├── controller/      # REST-Controller (Auth, LLM, SSE, User, Wallpaper)
│       ├── dto/             # Data Transfer Objects
│       │   └── llm/         # LLM-spezifische DTOs (Request, Response, Schema)
│       ├── exception/       # Globale Exception-Behandlung
│       ├── llm/             # LLM-Client (OpenRouter-Integration)
│       │   └── openrouter/  # OpenRouter-spezifische Implementierung
│       ├── logging/         # Custom Logback-Appender
│       ├── mapper/          # Object-Mapper (z.B. WallpaperMapper)
│       ├── model/           # JPA-Entitten (BaseEntity, User, Wallpaper, Role)
│       ├── pipeline/        # Pipeline-Framework
│       │   └── wallpaper/   # Wallpaper-Generierungspipeline
│       │       └── stages/  # Pipeline-Stages (Theme, CodeGen, Validate, Cache)
│       ├── repository/      # Spring Data Repositories
│       ├── service/         # Business Logic
│       └── sse/             # Server-Sent Events (AbstractSseService, SseEventName)
├── minidev-frontend/
│   └── src/app/
│       ├── components/      # Wiederverwendbare Komponenten
│       ├── core/            # Guards, Models, Services
│       └── pages/           # Seiten-Komponenten
├── docker-compose.yml
├── Dockerfile
└── .github/workflows/build.yml
```

## CI/CD

Das GitHub Actions Setup umfasst mehrere Workflows:

| Workflow | Trigger | Aufgabe |
|---|---|---|
| **build.yml** | Push/PR auf `dev`, `master` | Frontend + Backend bauen, Tests ausführen |
| **lint.yml** | Push/PR auf `dev`, `master` | ESLint (Frontend) + Checkstyle (Backend) |
| **docker.yml** | Push auf `master` | Docker-Image bauen + nach GHCR pushen |
| **release.yml** | Git-Tag `v*` | Release erstellen (Docker-Image + GitHub Release + WAR) |
| **codeql.yml** | Push/PR + wöchentlich | Security-Analyse (Java + TypeScript) |
| **stale.yml** | Wöchentlich | Inaktive Issues schließen |
| **issue-label.yml** | Issue-Eröffnung | Automatisch `status:discussion` setzen |

### Branch-Strategie

```
feature/xyz → dev (PR) → master (PR + Review)
```

- `master` – nur über PR mit mind. 1 Approval
- `dev` – Integration Branch
- `feature/*` – Entwicklung

## Umgebungsvariablen

Aus `docker-compose.yml` und der Backend-Konfiguration:

| Variable | Beschreibung |
|---|---|
| `MINIDEV_PORT` | Externer Port der Anwendung |
| `POSTGRES_HOST` | PostgreSQL-Host |
| `POSTGRES_PORT` | PostgreSQL-Port |
| `POSTGRES_DB` | Datenbankname |
| `POSTGRES_USER` | Datenbank-Benutzer |
| `POSTGRES_PASSWORD` | Datenbank-Passwort |
| `SPRING_PROFILES_ACTIVE` | Aktives Spring-Profil (z.B. `dev`, `prod`) |
| `OPENROUTER_API_KEY` | API-Key fur OpenRouter LLM-Zugang |
| `SESSION_COOKIE_SECURE` | Sichere Cookies (fur HTTPS) |
| `JAVA_OPTS` | JVM-Optionen |

## Contributing

Beitrage sind wilkommen! Forke das Repository und sende einen Pull Request.
