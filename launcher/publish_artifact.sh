#!/usr/bin/env bash
# OJ MUSIC - CI artifact bridge.
# After a successful cloud build this helper copies the assembled APK into a
# Git branch so it can be fetched from any network that can reach github.com
# (GitHub's artifact/blob CDNs are not reachable from every sandbox).
set -uo pipefail

[ -z "${GITHUB_ACTIONS:-}" ] && exit 0
WS="${GITHUB_WORKSPACE:-$(pwd)}"
cd "$WS" || exit 0

APK=$(find "$WS/app/build/outputs/apk" -name '*.apk' -type f 2>/dev/null | head -n 1)
if [ -z "${APK:-}" ]; then
    echo "[oj-publish] no APK found - nothing to publish"
    exit 0
fi

REF="oj-artifact-${GITHUB_RUN_ID:-manual}"
echo "[oj-publish] found APK: $APK ($(du -h "$APK" | cut -f1))"

# Use the bot token that GitHub provides to the workflow run
TOKEN="${GITHUB_TOKEN:-}"
if [ -z "$TOKEN" ]; then
    echo "[oj-publish] no GITHUB_TOKEN available"
    exit 0
fi

git config user.name  "OJ MUSIC CI"
git config user.email "ci@oj-music.local"

# Use a separate clone/branch technique: push to a dedicated orphan ref
REPO="https://x-access-token:${TOKEN}@github.com/${GITHUB_REPOSITORY}.git"

WORK=$(mktemp -d)
git clone --depth=1 --branch="${GITHUB_REF_NAME}" "$REPO" "$WORK/repo" 2>/dev/null || \
  git clone --depth=1 "$REPO" "$WORK/repo" 2>/dev/null
cd "$WORK/repo" || exit 0
git checkout --orphan "$REF"
git rm -rf --cached . >/dev/null 2>&1 || true
# Clean working tree but preserve .git
find . -mindepth 1 -maxdepth 1 ! -name .git -exec rm -rf {} + 2>/dev/null || true
cp -f "$APK" "./OJ-MUSIC.apk"
cat > README.txt << EOF
OJ MUSIC - build artifact
Run ID: ${GITHUB_RUN_ID}
Commit: ${GITHUB_SHA}
Branch: ${GITHUB_REF_NAME}
EOF
git add -f OJ-MUSIC.apk README.txt
git commit -q -m "OJ MUSIC APK (run ${GITHUB_RUN_ID:-manual})"
git push --force origin "HEAD:refs/heads/$REF" 2>&1 | tail -3
echo "[oj-publish] pushed APK to branch $REF"
rm -rf "$WORK"
exit 0
