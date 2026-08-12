#!/usr/bin/env bash
#
# Generate the offline seed content that ships inside the APK.
#
# The app has two modes. With a server address set it talks to the backend;
# with the field empty it runs on built-in memos so it is testable with nothing
# reachable. Those built-in memos used to point at a bell recording on
# Wikimedia — which meant "offline mode" needed the internet, and every memo in
# the feed sounded identical. Both defeat the point: this app is audio, and a
# tester comparing the radio timeline against the card deck cannot tell one
# memo from the next if they all play the same bell.
#
# So the memos are synthesised here and committed as raw resources. Each person
# gets their own espeak-ng voice, and the transcripts are the same ones
# backend/src/db/seed.ts uses, so the feed looks the same whether it came from
# Postgres or from the APK.
#
# Synthetic on purpose. Non-negotiable #7 makes a voice biometric data, and
# seeding a public repo with real recordings of real people would be exactly
# the thing this project promises not to do.
#
# Encoding matches what the app records and what the backend stores: AAC in
# MP4, 32 kbit/s mono at 22.05 kHz. If seed audio decoded differently from real
# audio, playback bugs would hide behind the fake.
#
# Usage:  android/tools/generate-seed-audio.sh
# Needs:  espeak-ng, ffmpeg, ffprobe
#
# Writes:  core/src/main/res/raw/*.m4a
#          core/src/main/java/org/teww/tew/core/repo/SeedContent.kt
#
# Re-run after editing MEMOS below. Do not hand-edit SeedContent.kt — the
# durations in it are measured from the files, and a hand-typed duration that
# disagrees with the audio makes the app announce the wrong length.

set -euo pipefail

here="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
raw="$here/../core/src/main/res/raw"
out="$here/../core/src/main/java/org/teww/tew/core/repo/SeedContent.kt"

for tool in espeak-ng ffmpeg ffprobe; do
    command -v "$tool" >/dev/null || { echo "missing: $tool" >&2; exit 1; }
done

# person|espeak voice
USERS=(
    "amina|en-gb+f3"
    "joseph|en-gb+m3"
    "fatima|en-us+f4"
    "daniel|en-us+m2"
    "grace|en-gb+f2"
    "samuel|en+m5"
    "you|en+f1"
)

# id|author|minutes ago|transcript
MEMOS=(
    "m1|amina|4|Good morning everyone. I walked to the market on my own today for the first time since I moved here. I am still shaking a bit, but I did it."
    "m2|joseph|19|Does anyone else find the bus announcements too quiet? I keep missing my stop and having to walk back."
    "m3|fatima|47|My daughter recorded her school song for me this morning. I have listened to it about nine times now. I am not sorry."
    "m4|daniel|96|Question for the group. What is your trick for telling shampoo and conditioner apart? I have tried elastic bands and I keep getting it wrong."
    "m5|grace|140|I just wanted to say that hearing other people voices in the evening helps more than I expected it to. That is all. Goodnight."
    "m6|samuel|200|Rain on a tin roof. That is the whole memo. Enjoy it."
    "m7|joseph|260|Following up on the bus thing. Someone suggested asking the driver to call out my stop, and it worked. Thank you whoever that was."
    "m8|amina|320|Small thing. The tactile paving outside the library has been repaired. It has been broken for two years."
)

# The tester's own memos, so the profile screen is not empty and the appeal
# flow has something real to open.
OWN=(
    "own1|you|30|Testing this out. Hello from my kitchen."
    "own2|you|400|This memo was taken down, so the appeal screen has something to open."
)

# id|memo|author|minutes ago|transcript
COMMENTS=(
    "c1|m2|grace|12|I have the same problem on the number nine. You are not imagining it."
    "c2|m2|daniel|8|Ask the driver when you get on. Mine has never once minded."
    "c3|m4|fatima|60|Rubber band on the conditioner. One band, one bottle, never two."
    "c4|m1|samuel|3|That is a big thing, not a small one. Well done."
)

voice_for() {
    local want="$1"
    for u in "${USERS[@]}"; do
        [[ "${u%%|*}" == "$want" ]] && { echo "${u#*|}"; return; }
    done
    echo "en"
}

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT

mkdir -p "$raw"
rm -f "$raw"/seed_*.m4a

# resource name -> duration in ms, filled in as each clip is rendered.
declare -A DURATION

render() {
    local res="$1" voice="$2" text="$3"
    espeak-ng -v "$voice" -s 150 -w "$tmp/$res.wav" "$text"
    ffmpeg -hide_banner -loglevel error -y \
        -i "$tmp/$res.wav" \
        -c:a aac -b:a 32k -ar 22050 -ac 1 \
        "$raw/$res.m4a"
    local seconds
    seconds="$(ffprobe -v error -show_entries format=duration -of csv=p=0 "$raw/$res.m4a")"
    DURATION["$res"]="$(printf '%.0f' "$(echo "$seconds * 1000" | bc -l)")"
    echo "  $res.m4a  ${DURATION[$res]}ms"
}

echo "Rendering seed audio into $raw"
for row in "${MEMOS[@]}" "${OWN[@]}" "${COMMENTS[@]}"; do
    IFS='|' read -r id author _ text <<<"$row"
    render "seed_$id" "$(voice_for "$author")" "$text"
done

# --- Emit the Kotlin ----------------------------------------------------

kt_escape() { printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g; s/\$/\\$/g'; }

{
cat <<'HEADER'
package org.teww.tew.core.repo

import org.teww.tew.core.R
import org.teww.tew.core.model.AppealState
import org.teww.tew.core.model.Comment
import org.teww.tew.core.model.Memo
import org.teww.tew.core.model.Moderation
import org.teww.tew.core.model.ModerationState

// GENERATED by android/tools/generate-seed-audio.sh — do not edit by hand.
//
// Every duration here was measured from the file it names. Editing one by hand
// makes the app announce a length the audio does not have, and there is no test
// that can catch that because the audio is the ground truth.
//
// Change the transcripts in the script and re-run it.

/**
 * The memos the app shows when no server address is set.
 *
 * Audio is bundled in the APK as raw resources and addressed with
 * `rawresource://`, which Media3 resolves without a network. That is the whole
 * point: a tester with a phone, no server and no signal can still open the app
 * and hear a feed.
 *
 * These transcripts are the same ones the backend seeds, so switching between
 * built-in memos and a real server does not change what the tester is looking
 * at — only where it came from.
 */
HEADER

emit_memo() {
    local id="$1" author="$2" mins="$3" text="$4" removed="${5:-}"
    local res="seed_$id"
    echo "    Memo("
    echo "        id = \"$id\","
    echo "        authorUsername = \"$(kt_escape "$author")\","
    echo "        audioUrl = rawResourceUri(R.raw.$res),"
    echo "        durationMs = ${DURATION[$res]},"
    echo "        postedAtEpochSeconds = minutesAgo($mins),"
    echo "        transcript = \"$(kt_escape "$text")\","
    echo "        likedByMe = false,"
    if [[ -n "$removed" ]]; then
        echo "        moderation = Moderation("
        echo "            state = ModerationState.REMOVED,"
        echo "            reason = \"$(kt_escape "$removed")\","
        echo "            appeal = AppealState.AVAILABLE,"
        echo "        ),"
    else
        echo "        moderation = Moderation.visible,"
    fi
    echo "    ),"
}

echo "fun sampleMemos(): List<Memo> = listOf("
for row in "${MEMOS[@]}"; do
    IFS='|' read -r id author mins text <<<"$row"
    emit_memo "$id" "$author" "$mins" "$text"
done
echo ")"
echo

cat <<'OWNDOC'
/**
 * What the profile screen shows. The second one is removed on purpose, so the
 * moderation reason and the appeal flow have something real to open rather
 * than an empty state nobody can test.
 */
OWNDOC
echo "fun sampleOwnMemos(): List<Memo> = listOf("
for row in "${OWN[@]}"; do
    IFS='|' read -r id author mins text <<<"$row"
    if [[ "$id" == "own2" ]]; then
        emit_memo "$id" "$author" "$mins" "$text" \
            "A listener reported this memo for harassment, and a moderator agreed it broke the community rules. If you think that was wrong, you can ask for another look."
    else
        emit_memo "$id" "$author" "$mins" "$text"
    fi
done
echo ")"
echo

cat <<'CDOC'
/**
 * Replies, keyed by the memo they answer.
 *
 * Without these the comments screen opens empty in offline mode, and "listen to
 * the replies" — one of the three things a tester is asked to try — cannot be
 * tried at all.
 */
CDOC
echo "fun sampleComments(): Map<String, List<Comment>> = mapOf("
for memo in $(printf '%s\n' "${COMMENTS[@]}" | cut -d'|' -f2 | awk '!seen[$0]++'); do
    echo "    \"$memo\" to listOf("
    for row in "${COMMENTS[@]}"; do
        IFS='|' read -r id m author mins text <<<"$row"
        [[ "$m" == "$memo" ]] || continue
        echo "        Comment("
        echo "            id = \"$id\","
        echo "            memoId = \"$m\","
        echo "            authorUsername = \"$(kt_escape "$author")\","
        echo "            audioUrl = rawResourceUri(R.raw.seed_$id),"
        echo "            durationMs = ${DURATION[seed_$id]},"
        echo "            postedAtEpochSeconds = minutesAgo($mins),"
        echo "            transcript = \"$(kt_escape "$text")\","
        echo "        ),"
    done
    echo "    ),"
done
echo ")"
} > "$out"

echo "Wrote $out"
