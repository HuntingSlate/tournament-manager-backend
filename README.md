# Tournament Management Backend

This is the backend for a tournament management system, built with Spring Boot and using Microsoft SQL Server as its database. The application provides a RESTful API for managing tournaments, teams, matches, player statistics, and user authorization (JWT).

## Requirements

To run the project locally using Docker, you will need:

* **Docker Desktop** (includes Docker Engine and Docker Compose)
    * Download and install from: [https://www.docker.com/products/docker-desktop/](https://www.docker.com/products/docker-desktop/)

## Running the Project with Docker (Recommended)

**Frontend developers (or any developer) should use this method.** It eliminates the need for manual installation of Java, Maven, SQL Server, and database configuration on their local machine.

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/HuntingSlate/tournament-manager-backend.git](https://github.com/HuntingSlate/tournament-manager-backend.git)
    cd tournament-manager-backend
    ```

2.  **Configure environment variables:**
    * In the root directory of your project, you'll find an example file (you can create one from the example below if it's missing).
    * **Create a `.env` file** in the same directory as `docker-compose.yml` and `Dockerfile`.
    * Populate it with the required variables:
        ```dotenv
        # File: .env
        DB_USERNAME=sa                     # Database user for connecting (e.g., sa)
        DB_PASSWORD=YourStrongPassword!    # Password for the SA user (must be strong: min. 8 chars, uppercase, lowercase, number, special char)
        JWT_SECRET=InsertHereALongRandomJWTKeyAtLeast64CharsLongAndKeepItSecurelyStored    # JWT secret key (at least 64 Base64 characters long)
        JWT_EXPIRATION=86400000            # JWT token expiration time in milliseconds (e.g., 24 hours)
        ```
3.  **Start the containers using Docker Compose:**
    * Open your terminal (e.g., Git Bash on Windows, PowerShell, CMD).
    * Navigate to the root directory of your project (where `docker-compose.yml` is located).
    * Execute the command:
        ```bash
        docker compose up --build -d
        ```
        * `up`: Starts the services defined in `docker-compose.yml`.
        * `--build`: Forces a rebuild of the backend image if it doesn't exist or if the source code has changed.
        * `-d`: Runs the containers in detached mode (in the background).

4.  **Wait for the application to start:**
    * The first time you run it, it might take a few minutes as Docker needs to pull the SQL Server image and build your backend image. SQL Server also needs time for initialization.
    * You can monitor the container logs to check their status:
        ```bash
        docker compose logs -f
        ```
      (Press `Ctrl+C` to exit the log view).

5.  **Verify the application is running:**
    * Once the logs indicate that the `backend` container has started successfully (look for a message like "Started TournamentManagerBackendApplication..."), open your browser and navigate to:
        * **API Documentation (Swagger UI):** `http://localhost:8080/swagger-ui.html`
        * (If that doesn't work, try `http://localhost:8080/swagger-ui/index.html`)

## API Access

The backend application will be accessible at: `http://localhost:8080`.

**Key Endpoints (test via Swagger UI):**

* **Registration:** `POST /api/auth/register` (public)
* **Login:** `POST /api/auth/login` (public)
* **Create Tournament:** `POST /api/tournaments` (requires JWT token)
* **Create Team:** `POST /api/teams` (requires JWT token)

## Stopping Containers

To stop and remove the running containers:

```bash
docker compose down