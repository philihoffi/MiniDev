# MiniDev - Fullstack Start Guide

This project consists of a Spring Boot backend and an Angular frontend.
The frontend is configured so that its build is copied directly into the static resources of the backend.

## GitHub Actions Pipeline

The project includes a comprehensive CI/CD pipeline (`.github/workflows/build.yml`):
- **Frontend Job:** Runs security audits, linting, unit tests (Headless Chrome), and builds the Angular application in parallel.
- **Backend Job:** Parallelized Maven build using all available CPU cores. It downloads the pre-built frontend and skips redundant build steps to minimize execution time.
- **Performance:** Optimized with caching, parallel execution, and decoupled build/test stages.
- **Artifacts:** The final `.war` file is stored as a build artifact in GitHub Actions.

## Integrated Start (Backend + Frontend together)

To integrate the frontend into the backend and run both together on one port (8080):

1. **Full Build:**
   Simply run Maven in the `minidev-backend` directory (it will automatically build the frontend):
   ```powershell
   cd minidev-backend
   ./mvnw clean install
   ```
   The frontend files will be copied to `minidev-backend/src/main/resources/static/browser`.

2. **Start Application:**
   ```powershell
   ./mvnw spring-boot:run
   ```

3. **Open Application:**
   Navigate to `http://localhost:8080` in your browser.

## Development (Separate Servers)

For active development, it is recommended to start both servers separately:

- **Backend:** `cd minidev-backend; ./mvnw spring-boot:run` (Port 8080)
- **Frontend:** `cd minidev-frontend; npm start` (Port 4200)

The frontend automatically forwards API requests to port 8080.
