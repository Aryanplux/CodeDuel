# How to Start CodeDuel

1. Install Docker Desktop, Node.js 18 or newer, and Java 17 or newer.

2. Open Docker Desktop and wait until Docker reports that it is running.

3. Open PowerShell in the project folder:

   ```powershell
   cd "C:\Users\aryan\OneDrive\Desktop\Projects\Final Year Project\codeduel"
   ```

4. Start PostgreSQL, Redis, and Judge0:

   ```powershell
   docker compose up -d
   ```

5. Check that the containers are running:

   ```powershell
   docker compose ps
   ```

   Confirm that `codeduel-judge0` and `codeduel-judge0-worker` are both running.

6. Open a second PowerShell window and start the backend:

   ```powershell
   cd "C:\Users\aryan\OneDrive\Desktop\Projects\Final Year Project\codeduel\backend"
   .\mvnw.cmd spring-boot:run
   ```

   Optional OpenRouter free-tier AI review configuration:

   ```powershell
   Copy-Item .env.example .env
   notepad .env
   ```

   Put the new API key and a free model ID in `backend\.env`, based on
   `backend\.env.example`. This file is ignored by Git and is loaded
   automatically when the backend starts from the backend directory. Never put
   the API key in `howToStart.md`, frontend files, or committed source code.
   OpenRouter review is requested only after Judge0 accepts a solution. Judge0
   remains authoritative for correctness. If the variables are not set,
   submissions continue to work and show the review as unavailable. The default
   winner weights are 70% execution speed and 30% AI quality; override them with
   `CODEDUEL_WINNER_EXECUTION_WEIGHT` and `CODEDUEL_WINNER_AI_WEIGHT` if needed.
   Once a player submits, the other player has 75 seconds by default to submit.
   The server broadcasts lock/status events over WebSocket and automatically
   marks an inactive player as not submitted when the deadline expires. Override
   this with `CODEDUEL_SUBMISSION_TIMEOUT_SECONDS` if needed.

7. Open a third PowerShell window and install frontend dependencies:
      
   ```powershell
   cd "C:\Users\aryan\OneDrive\Desktop\Projects\Final Year Project\codeduel\frontend"
   npm install
   ```

8. Start the frontend:

   ```powershell
   npm run dev
   ```

9. Open the URL printed by Vite, usually `http://localhost:5173`.

10. Stop the website by pressing `Ctrl+C` in the backend and frontend windows.

11. Stop the Docker services when finished:

   ```powershell
   cd "C:\Users\aryan\OneDrive\Desktop\Projects\Final Year Project\codeduel"
   docker compose down
   ```
