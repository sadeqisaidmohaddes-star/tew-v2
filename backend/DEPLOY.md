# Deploying the TEW API

Written for whoever brings this up on the VPS. It has **not been run on that
box** — this is a runbook, not a report. Anything that does not match what you
find, trust the box and correct this file.

## What this is

A Node/Fastify API on Postgres. It is the backend for an internal prototype
test of the Android app, not a public launch.

**It does not yet do everything the app will eventually need.** Audio upload,
object storage, ASR (whisper.cpp) and rate limiting are not implemented. The
API accepts memo metadata and serves the feed; the audio path is the next
piece of work.

## Prerequisites

- Docker with the Compose plugin
- A reverse proxy terminating TLS (aaPanel's nginx is already there)

## Bring it up

```bash
cd backend
cp .env.example .env
# Set POSTGRES_PASSWORD to something long and random. The compose file
# refuses to start without it rather than defaulting to something guessable.
$EDITOR .env

docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml exec api node dist/src/db/migrate.js
curl -s localhost:8080/health   # {"ok":true}
```

Then point the reverse proxy at `127.0.0.1:8080`.

## Three things that will stop it booting, on purpose

Each is a refusal to start rather than a warning, because all three fail
silently and expensively if allowed through:

| Symptom | Cause | Fix |
| --- | --- | --- |
| `Refusing to start in production with the stub token verifier` | No Firebase credentials | Expected for the prototype test — see below |
| `DATABASE_URL is not set. Refusing to start in production...` | Missing database URL | Set it in `.env` |
| `set POSTGRES_PASSWORD in .env` | Compose variable unset | Set it |

### The auth one needs a decision before you deploy

Said has said Firebase is **not needed for the internal prototype test**. But
the API refuses to run with `NODE_ENV=production` and the stub verifier,
because the stub **accepts any token and signs the caller in as whoever they
claim to be**.

So pick one, knowingly:

- **Run with `NODE_ENV=development`** — the stub is allowed, and anyone who
  finds the URL can read and post as anyone. Acceptable only if the API is not
  reachable from the public internet: bind it to a VPN, an IP allowlist, or
  HTTP basic auth at the nginx layer. **Do not put it on a public hostname
  in this mode.**
- **Or implement `FirebaseTokenVerifier`** against the existing
  `TokenVerifier` interface. It is one file and no other code changes.

There is no third option where the stub is safe on an open port. Voice is
biometric data under non-negotiable #7, and an open API would let anyone
download every memo in the system.

## Networking

Postgres is **not** published to the host. It is reachable only on the compose
network. The VPS runs other sites, and an exposed database port on a shared box
is a standing invitation.

The API binds to `127.0.0.1` only. Terminate TLS in front of it. Serving 8080
straight to the internet would put voice recordings on plaintext HTTP.

## Backups

Nothing here backs anything up. Before the test involves real people:

```bash
docker compose -f docker-compose.prod.yml exec postgres \
  pg_dump -U tew tew | gzip > tew-$(date +%F).sql.gz
```

Note what a backup means under the retention rule: **audio exists until the
person deletes it or deletes their account.** A backup taken before a deletion
still contains their recording. If someone asks to be forgotten, the backups
have to be dealt with too, or the deletion is only partly real.

## Updating

```bash
git pull
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml exec api node dist/src/db/migrate.js
```

Migrations are applied once each, in filename order, one transaction apiece —
a half-applied schema on a hand-managed box is a much worse afternoon than a
failed migration.

## Logs

```bash
docker compose -f docker-compose.prod.yml logs -f api
```

Request logging is at BASIC level and never logs bodies. That is deliberate:
bodies would put memo audio and transcripts into the log where anything on the
box can read them.
