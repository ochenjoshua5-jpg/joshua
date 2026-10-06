#!/usr/bin/env bash
# OJ MUSIC - CI artifact bridge (GitHub API based).
set -uo pipefail

[ -z "${GITHUB_ACTIONS:-}" ] && exit 0
WS="${GITHUB_WORKSPACE:-$(pwd)}"
cd "$WS" || exit 0

command -v jq >/dev/null 2>&1 || { echo "[oj-publish] jq missing"; exit 0; }

APK=$(find "$WS/app/build/outputs/apk" -name '*.apk' -type f 2>/dev/null | head -n 1)
if [ -z "${APK:-}" ]; then
    echo "[oj-publish] no APK found"
    exit 0
fi
SIZE=$(wc -c < "$APK")
echo "[oj-publish] APK: $APK (${SIZE} bytes)"

TOKEN="${GITHUB_TOKEN:-}"
REPO="${GITHUB_REPOSITORY:-}"
[ -z "$TOKEN" ] || [ -z "$REPO" ] && { echo "[oj-publish] missing TOKEN/REPO"; exit 0; }

RUN_ID="${GITHUB_RUN_ID:-manual}"
REF="oj-artifact-${RUN_ID}"
API="https://api.github.com/repos/${REPO}"
AUTH=(-H "Authorization: Bearer ${TOKEN}" -H "Accept: application/vnd.github+json" -H "Content-Type: application/json")
TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT

# Test permission first by writing a tiny marker
MARKER_BODY="$TMPDIR/marker.json"
echo '{"content":"dGVzdA==","encoding":"base64"}' > "$MARKER_BODY"
MARKER_SHA=$(curl -sS -X POST "${API}/git/blobs" "${AUTH[@]}" --data-binary "@$MARKER_BODY" | jq -r '.sha // empty')
echo "[oj-publish] marker blob: $MARKER_SHA"
if [ -z "$MARKER_SHA" ]; then
    echo "[oj-publish] NO WRITE PERMISSION (blob create failed)"
    curl -sS -X POST "${API}/git/blobs" "${AUTH[@]}" --data-binary "@$MARKER_BODY" | head -c 500
    echo
    exit 0
fi

# Encode APK as base64 (split-safe file output via python so argv isn't overflowed)
B64_FILE="$TMPDIR/apk.b64"
BODY="$TMPDIR/blob.json"
echo "[oj-publish] encoding APK..."
python3 - "$APK" "$B64_FILE" "$BODY" <<'PY'
import base64, json, sys
src, b64path, dst = sys.argv[1], sys.argv[2], sys.argv[3]
with open(src, "rb") as f:
    data = f.read()
enc = base64.b64encode(data).decode("ascii")
with open(b64path, "w") as f:
    f.write(enc)
with open(dst, "w") as f:
    json.dump({"content": enc, "encoding": "base64"}, f)
print("b64 bytes:", len(enc))
PY

if [ "$(wc -c < "$BODY")" -gt 110000000 ]; then
    echo "[oj-publish] payload too large for GitHub blob API"
    exit 0
fi

echo "[oj-publish] uploading APK blob..."
BLOB_SHA=$(curl -sS -X POST "${API}/git/blobs" "${AUTH[@]}" --data-binary "@$BODY" | jq -r '.sha // empty')
[ -z "$BLOB_SHA" ] && { echo "[oj-publish] APK blob upload failed"; curl -sS -X POST "${API}/git/blobs" "${AUTH[@]}" --data-binary "@$BODY" | head -c 500; echo; exit 0; }
echo "[oj-publish] apk blob: $BLOB_SHA"

# README
README_BODY="$TMPDIR/readme-blob.json"
python3 - "$README_BODY" "$RUN_ID" "${GITHUB_SHA:-}" "${GITHUB_REF:-}" "$BLOB_SHA" <<'PY'
import json, sys
dst, run, sha, ref, apk = sys.argv[1:]
text = f"OJ MUSIC build artifact\nRun: {run}\nCommit: {sha}\nRef: {ref}\nAPK: {apk}\n"
json.dump({"content": text, "encoding": "utf-8"}, open(dst,"w"))
PY
README_SHA=$(curl -sS -X POST "${API}/git/blobs" "${AUTH[@]}" --data-binary "@$README_BODY" | jq -r '.sha // empty')

# Base tree
BASE_SHA="${GITHUB_SHA:-}"
BASE_TREE=$(curl -sS "${API}/git/commits/${BASE_SHA}" "${AUTH[@]}" | jq -r '.tree.sha // empty')
[ -z "$BASE_TREE" ] && { echo "[oj-publish] base tree failed"; exit 0; }

# New tree
TREE_BODY="$TMPDIR/tree.json"
jq -nc --arg base "$BASE_TREE" --arg apk "$BLOB_SHA" --arg readme "$README_SHA" \
   '{base_tree:$base, tree:[
        {path:"OJ-MUSIC.apk", mode:"100644", type:"blob", sha:$apk},
        {path:"README.txt",  mode:"100644", type:"blob", sha:$readme}
    ]}' > "$TREE_BODY"
TREE_SHA=$(curl -sS -X POST "${API}/git/trees" "${AUTH[@]}" --data-binary "@$TREE_BODY" | jq -r '.sha // empty')
[ -z "$TREE_SHA" ] && { echo "[oj-publish] tree failed"; exit 0; }

# Commit
COMMIT_BODY="$TMPDIR/commit.json"
jq -nc --arg tree "$TREE_SHA" --arg parent "$BASE_SHA" \
   --arg msg "OJ MUSIC APK (run ${RUN_ID})" \
   '{message:$msg, tree:$tree, parents:[$parent]}' > "$COMMIT_BODY"
COMMIT_SHA=$(curl -sS -X POST "${API}/git/commits" "${AUTH[@]}" --data-binary "@$COMMIT_BODY" | jq -r '.sha // empty')
[ -z "$COMMIT_SHA" ] && { echo "[oj-publish] commit failed"; exit 0; }
echo "[oj-publish] commit: $COMMIT_SHA"

# Create or update ref
REF_BODY="$TMPDIR/ref.json"
jq -nc --arg sha "$COMMIT_SHA" '{sha:$sha}' > "$REF_BODY"
EXIST=$(curl -sS -o /dev/null -w "%{http_code}" "${API}/git/refs/heads/${REF}" "${AUTH[@]}")
if [ "$EXIST" = "200" ]; then
    curl -sS -X PATCH "${API}/git/refs/heads/${REF}" "${AUTH[@]}" --data-binary "$(jq -nc --arg sha "$COMMIT_SHA" '{sha:$sha, force:true}')" >/dev/null
else
    curl -sS -X POST "${API}/git/refs" "${AUTH[@]}" --data-binary "$(jq -nc --arg ref "refs/heads/${REF}" --arg sha "$COMMIT_SHA" '{ref:$ref, sha:$sha}')" >/dev/null
fi
echo "[oj-publish] DONE branch=$REF commit=$COMMIT_SHA"
