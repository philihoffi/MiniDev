# MiniDev

MiniDev kombiniert ein Spring-Boot-Backend mit einem Angular-Frontend.
Beim Backend-Build wird das Frontend automatisch gebaut und in die statischen Ressourcen eingebettet, sodass die App als ein gemeinsames Deployable auf `:8080` laufen kann.

## Kurzuberblick

- `minidev-backend`: API, Persistenz, Security, Hosting der gebauten Frontend-Artefakte
- `minidev-frontend`: Angular-App fur UI und lokale Entwicklung auf `:4200`
- `docker-compose.yml`: Startet App + PostgreSQL fur den containerisierten Betrieb
- `logs/`: Laufzeit-Logs im lokalen Betrieb (in Docker primar uber `docker compose logs`)

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

## Wichtige Umgebungsvariablen

Aus `docker-compose.yml` und der Backend-Konfiguration:

- `MINIDEV_PORT`
- `POSTGRES_HOST`
- `POSTGRES_PORT`
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `SPRING_PROFILES_ACTIVE`
- `OPENROUTER_API_KEY`
- `SESSION_COOKIE_SECURE`
- `JAVA_OPTS`

Beispielwerte findest du in `.env.example`.
