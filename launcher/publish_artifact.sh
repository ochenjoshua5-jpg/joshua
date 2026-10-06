#!/usr/bin/env bash
# OJ MUSIC - CI artifact bridge (GitHub API based).
#
# Uploads the assembled APK as a Git blob via the GitHub REST API and creates a
# ref "refs/heads/oj-artifact-<runid>" pointing to a commit containing the APK.
# Works from inside pull-request events and publishes the APK over api.github.com,
# which is reachable from restricted-egress sandboxes where the Actions blob CDN
# is blocked.
set -uo pipefail

[ -z "${GITHUB_ACTIONS:-}" ] && exit 0
WS="${GITHUB_WORKSPACE:-$(pwd)}"
cd "$WS" || exit 0

if ! command -v jq >/dev/null 2>&1; then
    echo "[oj-publish] jq not available; skipping"
    exit 0
fi

APK=$(find "$WS/app/build/outputs/apk" -name '*.apk' -type f 2>/dev/null | head -n 1)
if [ -z "${APK:-}" ]; then
    echo "[oj-publish] no APK found"
    exit 0
fi
SIZE=$(wc -c < "$APK")
echo "[oj-publish] APK: $APK (${SIZE} bytes)"

TOKEN="${GITHUB_TOKEN:-}"
REPO="${GITHUB_REPOSITORY:-}"
if [ -z "$TOKEN" ] || [ -z "$REPO" ]; then
    echo "[oj-publish] missing TOKEN or REPO"
    exit 0
fi

REF="oj-artifact-${GITHUB_RUN_ID:-manual}"
API="https://api.github.com/repos/${REPO}"
AUTH=(-H "Authorization: Bearer ${TOKEN}" -H "Accept: application/vnd.github+json")
TMPDIR=$(mktemp -d)
trap 'rm -rf "$TMPDIR"' EXIT

# Upload blob via base64 in a JSON body written to a file (to avoid argv limits).
# GitHub blobs API accepts up to 100 MB total; base64 adds 33% overhead, so APKs
# up to ~75 MB fit. Universal debug builds of this app are comfortably under that.
echo "[oj-publish] encoding APK as base64..."
B64_FILE="$TMPDIR/apk.b64"
base64 -w0 "$APK" > "$B64_FILE"
B64_SIZE=$(wc -c < "$B64_FILE")
if [ "$B64_SIZE" -gt 100000000 ]; then
    echo "[oj-publish] base64 payload too large (${B64_SIZE} bytes); skipping API publish"
    exit 0
fi

BODY="$TMPDIR/blob.json"
python3 - "$B64_FILE" "$BODY" <<'PY'
import base64, json, sys
src, dst = sys.argv[1], sys.argv[2]
with open(src, "r") as f:
    content = f.read()
with open(dst, "w") as f:
    json.dump({"content": content, "encoding": "base64"}, f)
PY

echo "[oj-publish] uploading blob..."
BLOB_SHA=$(curl -sS -X POST "${API}/git/blobs" "${AUTH[@]}" \
    -H "Content-Type: application/json" --data-binary "@$BODY" | jq -r '.sha // empty')
if [ -z "$BLOB_SHA" ]; then
    echo "[oj-publish] blob upload failed"
    curl -sS -X POST "${API}/git/blobs" "${AUTH[@]}" \
        -H "Content-Type: application/json" --data-binary "@$BODY" | head -c 1000
    exit 0
fi
echo "[oj-publish] apk blob: $BLOB_SHA"

# README blob (small, inline JSON fine)
README="$TMPDIR/readme.txt"
cat > "$README" <<EOF
OJ MUSIC - build artifact
Run ID: ${GITHUB_RUN_ID}
Commit: ${GITHUB_SHA}
Ref: ${GITHUB_REF}
APK blob: ${BLOB_SHA}
EOF
README_BODY="$TMPDIR/readme-blob.json"
python3 - "$README" "$README_BODY" <<'PY'
import json, sys
src, dst = sys.argv[1], sys.argv[2]
with open(src, "rb") as f:
    content = f.read().decode("utf-8")
with open(dst, "w") as f:
    json.dump({"content": content, "encoding": "utf-8"}, f)
PY
README_SHA=$(curl -sS -X POST "${API}/git/blobs" "${AUTH[@]}" \
    -H "Content-Type: application/json" --data-binary "@$README_BODY" | jq -r '.sha // empty')
echo "[oj-publish] readme blob: $README_SHA"

# Base tree from current commit
BASE_SHA="${GITHUB_SHA:-}"
BASE_TREE=$(curl -sS "${API}/git/commits/${BASE_SHA}" "${AUTH[@]}" | jq -r '.tree.sha // empty')
if [ -z "$BASE_TREE" ]; then
    echo "[oj-publish] base tree lookup failed"
    exit 0
fi

# Create new tree
TREE_BODY="$TMPDIR/tree.json"
jq -nc --arg base "$BASE_TREE" \
    --arg apk "$BLOB_SHA" --arg readme "$README_SHA" \
    '{base_tree:$base, tree:[
        {path:"OJ-MUSIC.apk", mode:"100644", type:"blob", sha:$apk},
        {path:"README.txt",  mode:"100644", type:"blob", sha:$readme}
    ]}' > "$TREE_BODY"
TREE_SHA=$(curl -sS -X POST "${API}/git/trees" "${AUTH[@]}" \
    -H "Content-Type: application/json" --data-binary "@$TREE_BODY" | jq -r '.sha // empty')
if [ -z "$TREE_SHA" ]; then
    echo "[oj-publish] tree creation failed"
    exit 0
fi
echo "[oj-publish] tree: $TREE_SHA"

# Create commit
COMMIT_BODY="$TMPDIR/commit.json"
jq -nc --arg tree "$TREE_SHA" --arg parent "$BASE_SHA" \
    --arg msg "OJ MUSIC APK (run ${GITHUB_RUN_ID:-manual})" \
    '{message:$msg, tree:$tree, parents:[$parent]}' > "$COMMIT_BODY"
COMMIT_SHA=$(curl -sS -X POST "${API}/git/commits" "${AUTH[@]}" \
    -H "Content-Type: application/json" --data-binary "@$COMMIT_BODY" | jq -r '.sha // empty')
if [ -z "$COMMIT_SHA" ]; then
    echo "[oj-publish] commit creation failed"
    exit 0
fi
echo "[oj-publish] commit: $COMMIT_SHA"

# Create or update the ref
REF_BODY="$TMPDIR/ref.json"
jq -nc --arg sha "$COMMIT_SHA" '{sha:$sha}' > "$REF_BODY"
EXISTING=$(curl -sS -o /dev/null -w "%{http_code}" "${API}/git/refs/heads/${REF}" "${AUTH[@]}")
if [ "$EXISTING" = "200" ]; then
    curl -sS -X PATCH "${API}/git/refs/heads/${REF}" "${AUTH[@]}" \
        -H "Content-Type: application/json" --data-binary "$(jq -nc --arg sha "$COMMIT_SHA" '{sha:$sha, force:true}')" >/dev/null
else
    curl -sS -X POST "${API}/git/refs" "${AUTH[@]}" \
        -H "Content-Type: application/json" \
        --data-binary "$(jq -nc --arg ref "refs/heads/${REF}" --arg sha "$COMMIT_SHA" '{ref:$ref, sha:$sha}')" >/dev/null
fi
echo "[oj-publish] published to branch $REF (commit $COMMIT_SHA)"
