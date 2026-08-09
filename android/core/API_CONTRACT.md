# API contract — Android's proposal

`backend/README.md` commits to a shape ("one cursor-paginated API for memos,
likes, and skip/next", Firebase Auth, report queue with appeals) but names no
endpoints, and the backend is still design-stage with no implementation.

`:core` had to pick something concrete to build against. **This document is the
Android client's proposal, not an agreed contract.** It needs a backend author
to accept, amend or reject it before either side treats it as settled.

Where this is wrong, the cost is contained: wire types live in
`net/Dto.kt`, separate from the domain models in `model/`, so a rename or
reshape changes one file and no feature-module code.

## Conventions

- Base URL is supplied by `:app` via `TewApiConfig` — nothing is hard-coded.
- `Authorization: Bearer <token>` on every request, from `AuthSession`.
- Snake case on the wire, camel case in Kotlin, mapped in `Dto.kt`.
- Unknown JSON fields are ignored, so the backend can add fields without
  breaking deployed clients.

## Endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/v1/me` | Signed-in user's id and username |
| `GET` | `/v1/feed?cursor=&limit=` | One page of the feed |
| `POST` | `/v1/memos` | Upload a memo (multipart, `audio`) |
| `POST` | `/v1/memos/{id}/like` | Like |
| `DELETE` | `/v1/memos/{id}/like` | Unlike |
| `POST` | `/v1/memos/{id}/skip` | Record a skip |
| `GET` | `/v1/memos/{id}/comments` | Voice replies to a memo |
| `POST` | `/v1/memos/{id}/comments` | Post a voice reply (multipart, `audio`) |
| `POST` | `/v1/memos/{id}/report` | Report a memo |
| `GET` | `/v1/me/memos` | Own memos, **including removed ones** |
| `POST` | `/v1/memos/{id}/appeal` | Appeal a removal |
| `DELETE` | `/v1/memos/{id}` | Delete your own memo and its audio |
| `DELETE` | `/v1/me` | Delete your account and everything you recorded |

## Retention

Audio exists until the person deletes the memo or deletes their account, and
then it is gone. No time-based expiry, no soft delete. `DELETE /v1/memos/{id}`
returns 404 for both "no such memo" and "not yours" — telling those apart would
confirm another person's memo exists to a stranger.

**The client does not call either endpoint yet.** There is no delete control in
the app. Until there is, the retention rule is only half deliverable.

## Two things that are load-bearing

**`next_cursor` must be null or absent at the end of the feed.** Non-negotiable
#3 is *the stream ends*. The client reads a null cursor as the end and says so
out loud. A backend that always returns a cursor turns TEW into an infinite
feed, which is the specific thing the product exists not to be. There is a test
for this (`DtoMappingTest`), and it will not be relaxed.

**No counts, anywhere.** No like totals, play counts, comment counts or
follower numbers — non-negotiable #4, *no engagement machinery*. `liked_by_me`
is a boolean because a user needs to know their own like registered. If the
backend starts sending totals the client will ignore them, and they should not
be sent.

## Deliberately absent

- **Direct messages.** libsignal stays backend-only this phase; no DM screen
  ships, so no DM endpoints are consumed.
- **Moderator review queue.** Reports are submitted from the app; reviewing
  happens outside it. `android/README.md` cuts the admin UI, not the reporting.
- **Anything ranked or personalised.** The feed is ordered, not scored.

## Open questions for the backend author

*(Answered — see `backend/README.md`. Kept for the record.)*

1. Is cursor pagination opaque-string based, as assumed here?
2. Does `/v1/me/memos` return removed memos with a human-readable `reason`?
   The appeal screen is built on the assumption that it does — a reason code
   the client has to translate would move that copy into the app, where it is
   harder to change than on the server.
3. What audio container and codec should the client upload? `audio/mp4` is
   assumed; whisper.cpp's expectations should decide this, not the client.
4. Is there a rate limit the client should back off against, and does it come
   back as `429` with `Retry-After`?
