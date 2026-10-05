#!/usr/bin/env bash
# OJ MUSIC - CI artifact bridge (diagnostic build).
set -uo pipefail

note() { echo "::notice title=OJ-PUBLISH::$1"; echo "[oj-publish] $1"; }
warn() { echo "::warning title=OJ-PUBLISH::$1"; echo "[oj-publish] $1"; }

[ -z "${GITHUB_ACTIONS:-}" ] && exit 0
WS="${GITHUB_WORKSPACE:-$(pwd)}"
cd "$WS" || exit 0

APK=$(find "$WS/app/build/outputs/apk" -name '*.apk' -type f 2>/dev/null | head -n 1)
if [ -z "${APK:-}" ]; then warn "hook ran but no APK found"; exit 0; fi

SIZE=$(du -h "$APK" | cut -f1)
SUM=$(sha256sum "$APK" | cut -c1-16)
note "hook ran; apk=$SIZE sha=$SUM found in app/build/outputs/apk"

git config user.name  "OJ MUSIC CI"  >/dev/null 2>&1
git config user.email "ci@oj-music.local" >/dev/null 2>&1

cp -f "$APK" "$WS/OJ-MUSIC.apk" || { warn "copy failed"; exit 0; }
git add -f "$WS/OJ-MUSIC.apk" >/dev/null 2>&1
git commit -q -m "OJ MUSIC build artifact (run ${GITHUB_RUN_ID:-manual})" >/dev/null 2>&1

REF="oj-artifact-${GITHUB_RUN_ID:-manual}"
ERR=$(git push --force origin "HEAD:refs/heads/$REF" 2>&1)
if [ $? -eq 0 ]; then
    note "pushed artifact branch $REF"
else
    warn "push failed: $(printf '%s' "$ERR" | tail -c 300 | tr '\n' ' ')"
fi
exit 0
