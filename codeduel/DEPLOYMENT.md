# CodeDuel Deployment: Vercel, Render, Neon, Upstash

This guide deploys the split production stack:

- Frontend: Vercel
- Backend: Render
- PostgreSQL: Neon
- Redis: Upstash
- Code execution: public or paid Judge0
- Domain: Get.Tech or provider subdomains

## Recommended final domains

- `codeduel.tech` for the frontend
- `api.codeduel.tech` for the backend
- Neon and Upstash use their own hostnames

If the custom domain is not available yet, use:

- Vercel URL for the frontend
- Render URL such as `codeduel-backend.onrender.com`

The custom-domain setup is preferable because the frontend and backend share
the `codeduel.tech` site and browser cookie behavior is less restrictive.

## 1. Create the Neon database

1. Create a Neon account.
2. Create a project.
3. Create a database named `codeduel`.
4. Copy the connection settings.

Neon provides a JDBC URL similar to:

```text
jdbc:postgresql://ep-example.region.aws.neon.tech/codeduel?sslmode=require
```

Copy it exactly as an environment variable. Neon credentials must not be
committed to Git.

## 2. Create the Upstash Redis database

1. Create an Upstash account.
2. Create a Redis database.
3. Select the region closest to Render.
4. Copy:
   - Host
   - Port
   - Password
   - Username, usually `default`
5. Confirm that TLS is enabled.

Upstash values will become the Render Redis environment variables.

## 3. Choose Judge0

### Initial testing

Use the public CE endpoint:

```text
https://ce.judge0.com
```

This is suitable for limited testing. It may return rate-limit errors and must
not be considered a permanent production service.

### Stable deployment

Use RapidAPI Judge0 or a hosted Judge0 instance. Set:

```text
JUDGE0_URL=https://your-judge0-host
JUDGE0_API_KEY=your-provider-api-key
```

The backend accepts `JUDGE0_API_KEY` and sends it as `X-Auth-Token`.

## 4. Configure the backend on Render

1. Push this repository to GitHub.
2. Sign in to Render.
3. Select **New > Blueprint**.
4. Connect the GitHub repository.
5. Select `render.yaml`.
6. Review the `codeduel-backend` service.
7. Set these environment variables:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://NEON_HOST/codeduel?sslmode=require
SPRING_DATASOURCE_USERNAME=NEON_USERNAME
SPRING_DATASOURCE_PASSWORD=NEON_PASSWORD
SPRING_DATA_REDIS_HOST=UPSTASH_HOST
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=UPSTASH_PASSWORD
SPRING_DATA_REDIS_USERNAME=default
SPRING_DATA_REDIS_SSL_ENABLED=true
JUDGE0_URL=https://ce.judge0.com
JUDGE0_API_KEY=
APP_CORS_ALLOWED_ORIGINS=https://codeduel.tech,https://www.codeduel.tech
COOKIE_SECURE=true
COOKIE_SAME_SITE=none
SERVER_FORWARD_HEADERS_STRATEGY=framework
CODEDUEL_SUBMISSION_TIMEOUT_SECONDS=75
OPENROUTER_API_KEY=
OPENROUTER_MODEL=openai/gpt-4o-mini
```

For a temporary Render-only frontend test, set CORS to its Vercel URL:

```text
APP_CORS_ALLOWED_ORIGINS=https://YOUR_PROJECT.vercel.app
```

The backend Docker context is `codeduel/backend`, and the Dockerfile is
`codeduel/backend/Dockerfile`.

Create the Render service and wait for deployment.

Test:

```text
https://YOUR_RENDER_SERVICE.onrender.com/api/health
https://YOUR_RENDER_SERVICE.onrender.com/api/auth/me
```

The health endpoint must return `{"status":"UP"}`.
The auth endpoint should return HTTP 401 while logged out.

## 5. Deploy the frontend on Vercel

1. Import the GitHub repository into Vercel.
2. Set the project root directory to `codeduel/frontend`.
3. Framework preset: Vite.
4. Build command:

```text
npm run build
```

5. Output directory:

```text
dist
```

6. Add environment variables:

```text
VITE_API_URL=https://YOUR_RENDER_SERVICE.onrender.com/api
VITE_WS_URL=https://YOUR_RENDER_SERVICE.onrender.com/ws
```

Deploy the project.

Test:

```text
https://YOUR_PROJECT.vercel.app
https://YOUR_PROJECT.vercel.app/login
```

Do not place API keys in `VITE_*` variables. Vite exposes those values to every
browser.

## 6. Add the custom frontend domain

Use a Get.Tech `.tech` domain or a domain that you already own.

In Vercel:

1. Open **Project Settings > Domains**.
2. Add `codeduel.tech`.
3. Add `www.codeduel.tech`.
4. Copy Vercel's DNS target.

In the Get.Tech DNS panel, add:

```text
Type: CNAME
Name: www
Value: cname.vercel-dns.com
```

For an apex `codeduel.tech` domain, use the DNS record recommended by Vercel,
usually `A 76.76.21.21`.

Wait for Vercel's HTTPS certificate.

## 7. Add the backend custom domain

In Render:

1. Open the backend service.
2. Select **Custom Domains**.
3. Add `api.codeduel.tech`.

Render provides the required DNS record, normally a CNAME.

In Get.Tech DNS, add:

```text
Type: CNAME
Name: api
Value: YOUR_RENDER_HOSTNAME
```

Wait for DNS and Render TLS to complete.

Then update the Vercel environment variables:

```text
VITE_API_URL=https://api.codeduel.tech/api
VITE_WS_URL=https://api.codeduel.tech/ws
```

Redeploy Vercel.

Update the Render CORS value:

```text
APP_CORS_ALLOWED_ORIGINS=https://codeduel.tech,https://www.codeduel.tech
```

Restart or redeploy the Render backend.

## 8. Verify authentication cookies

The login flow uses an HTTP session cookie.

With custom domains:

```text
COOKIE_SECURE=true
COOKIE_SAME_SITE=none
```

The browser must receive HTTPS cookies from `api.codeduel.tech`.

Test signup, login, `/auth/me`, `/dashboard`, logout, and login again.
In browser DevTools:

1. Open **Application > Cookies**.
2. Confirm a `JSESSIONID` cookie exists for `api.codeduel.tech`.
3. Confirm it is `Secure`, `HttpOnly`, and `SameSite=None`.

If cookies are blocked, keep the frontend and backend under one root domain
and do not use both a bare provider subdomain and a custom frontend domain.

## 9. Seed and verify PostgreSQL

The [DataSeeder.java](codeduel/backend/src/main/java/com/example/CodeDuel/config/DataSeeder.java)
creates five problems if the `problems` table is empty.

Verify with Neon SQL editor:

```sql
SELECT id, title, difficulty FROM problems;
```

If the table was previously created or has stale records, update data manually
or with a migration before testing.

## 10. Test Judge0

Without login, protected submission testing is not possible.

After login:

1. Open a battle.
2. Run sample code.
3. Submit accepted code.
4. Submit wrong code.
5. Check backend logs for Judge0 HTTP results.
6. Repeat three times.

If Judge0 returns 429, it is rate-limited. Do not continue retrying rapidly.
Use a paid provider or host Judge0.

## 11. Test matchmaking and WebSockets

Use two browsers or one normal browser plus one private browser:

1. Log in as two users.
2. Open the lobby.
3. Start matchmaking from each user.
4. Confirm both users receive the match.
5. Confirm both enter `/battle/{battleId}`.
6. Submit code in one browser.
7. Confirm opponent progress updates in the other browser.
8. Leave a battle and confirm the finished state.

Check Render logs if WebSockets disconnect.

## 12. Free-tier limitations

- Render free services sleep when idle.
- The first request after sleep can take 30-60 seconds.
- Neon free limits may pause or restrict usage.
- Upstash free limits can be reached.
- Public Judge0 CE is not a reliable production service.
- The backend stores session state in server memory. Render instance restarts
  can invalidate sessions. For horizontal scaling, use Redis-backed sessions.

## 13. Ongoing updates

Push to GitHub:

```bash
git add .
git commit -m "Update CodeDuel"
git push origin main
```

Vercel redeploys automatically.
Render redeploys automatically if GitHub auto-deploy is enabled.
