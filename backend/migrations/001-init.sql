-- TEW initial schema.
--
-- Two things worth reading before changing anything here.
--
-- 1. There are no counter columns. No like_count, no play_count, no
--    follower_count. Non-negotiable #4 is "no engagement machinery", and a
--    denormalised counter is how that decision gets quietly reversed: once the
--    number exists somebody will render it. Likes are rows; nobody totals them.
--
-- 2. There is no follow table and no score column. BRIEF.md rules out a follow
--    graph and a ranking algorithm, and the schema is the cheapest place to
--    make that structurally true rather than a rule someone has to remember.

CREATE TABLE IF NOT EXISTS users (
  id           TEXT PRIMARY KEY,
  username     TEXT NOT NULL UNIQUE,
  created_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- No deleted_at, and that is the retention policy rather than an omission.
-- Audio exists until the person deletes the memo or deletes their account,
-- and then it is gone: the row is removed, ON DELETE CASCADE takes the
-- memos, comments, likes and heard-markers with it, and the audio object is
-- deleted from storage. There is no time-based expiry and no soft delete.
-- A "deleted" recording still sitting on disk has not been deleted, and
-- non-negotiable #7 does not leave room for that distinction.

CREATE TABLE IF NOT EXISTS memos (
  id            TEXT PRIMARY KEY,
  author_id     TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  -- Object-store key or path. Not a public URL: the API signs one per request
  -- so audio is not world-readable by anyone who guesses an id. Voice is
  -- biometric data (non-negotiable #7).
  audio_key     TEXT NOT NULL,
  duration_ms   INTEGER NOT NULL CHECK (duration_ms >= 0),
  posted_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  -- Filled in by the ASR pass. Null means not transcribed yet, which the
  -- client already handles.
  transcript    TEXT,

  moderation_state TEXT NOT NULL DEFAULT 'visible'
    CHECK (moderation_state IN ('visible', 'under_review', 'removed')),
  -- Plain language, written to be read aloud. Lives here rather than as a
  -- code the client translates, so moderation wording can change without an
  -- app release.
  moderation_reason TEXT,
  appeal_state  TEXT NOT NULL DEFAULT 'not_applicable'
    CHECK (appeal_state IN ('not_applicable', 'available', 'submitted', 'upheld', 'denied'))
);

-- The feed's access path: newest first, within the window, visible only.
CREATE INDEX IF NOT EXISTS memos_feed_idx
  ON memos (posted_at DESC, id DESC)
  WHERE moderation_state = 'visible';

CREATE INDEX IF NOT EXISTS memos_author_idx ON memos (author_id, posted_at DESC);

CREATE TABLE IF NOT EXISTS comments (
  id          TEXT PRIMARY KEY,
  memo_id     TEXT NOT NULL REFERENCES memos(id) ON DELETE CASCADE,
  author_id   TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  audio_key   TEXT NOT NULL,
  duration_ms INTEGER NOT NULL CHECK (duration_ms >= 0),
  posted_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  transcript  TEXT
);

CREATE INDEX IF NOT EXISTS comments_memo_idx ON comments (memo_id, posted_at ASC);

CREATE TABLE IF NOT EXISTS likes (
  user_id   TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  memo_id   TEXT NOT NULL REFERENCES memos(id) ON DELETE CASCADE,
  liked_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (user_id, memo_id)
);

-- Which memos a listener has already been served. This is what makes the
-- stream end: the feed serves what you have not heard, and eventually that set
-- is empty. It is NOT an engagement signal and must never feed ranking.
CREATE TABLE IF NOT EXISTS heard (
  user_id   TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  memo_id   TEXT NOT NULL REFERENCES memos(id) ON DELETE CASCADE,
  heard_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (user_id, memo_id)
);

CREATE TABLE IF NOT EXISTS reports (
  id          TEXT PRIMARY KEY,
  memo_id     TEXT NOT NULL REFERENCES memos(id) ON DELETE CASCADE,
  reporter_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  reason      TEXT NOT NULL
    CHECK (reason IN ('harassment', 'hate_speech', 'sexual_content', 'spam', 'other')),
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  resolved_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS reports_open_idx ON reports (created_at DESC)
  WHERE resolved_at IS NULL;

CREATE TABLE IF NOT EXISTS appeals (
  id         TEXT PRIMARY KEY,
  memo_id    TEXT NOT NULL REFERENCES memos(id) ON DELETE CASCADE,
  author_id  TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  text       TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  decided_at TIMESTAMPTZ
);
