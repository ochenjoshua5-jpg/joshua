#!/usr/bin/env bash
# OJ MUSIC - CI artifact bridge.
#
# GitHub's Actions artifact service is not reachable from every network, so
# after a successful cloud build this helper copies the freshly assembled APK
# into a branch of this repository. The launcher .exe can then be assembled
# from anywhere that can read the repository.
#
# It only runs inside GitHub Actions (see the hook at the end of ./gradlew).
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
echo "[oj-publish] found $(du -h "$APK" | cut -f1) APK: $APK"

git config user.name  "OJ MUSIC CI"  >/dev/null 2>&1
git config user.email "ci@oj-music.local" >/dev/null 2>&1

cp -f "$APK" "$WS/OJ-MUSIC.apk" || exit 0
git add -f "$WS/OJ-MUSIC.apk" >/dev/null 2>&1
git commit -q -m "OJ MUSIC build artifact (run ${GITHUB_RUN_ID:-manual})" >/dev/null 2>&1

if git push -q --force origin "HEAD:refs/heads/$REF" >/dev/null 2>&1; then
    echo "[oj-publish] pushed APK to branch $REF"
else
    echo "[oj-publish] could not push the artifact branch (token is read-only)"
fi
exit 0
