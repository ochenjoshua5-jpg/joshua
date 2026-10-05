# OJ MUSIC — Windows launcher (.exe)

`OJ-MUSIC.exe` is a single self-contained Windows program that **installs and starts the
full OJ MUSIC Android app** on a phone, tablet or Android emulator.

Made by **Ochen Joshua**.

## What it does

1. Unpacks the Android app (APK) that is bundled **inside the .exe** to
   `%LOCALAPPDATA%\OJ MUSIC\OJ-MUSIC.apk`.
2. Finds `adb` (next to the .exe, in `platform-tools\`, in the Android SDK, or on `PATH`).
3. Waits for a device. If none is connected it will try to boot an Android emulator.
4. Installs the app (`adb install -r -d`) and launches it
   (`com.ochenjoshua.ojmusicplayer/.MainActivity`).
5. If `adb` is missing, the **Save APK…** button writes the APK anywhere you like so you can
   copy it to the phone and tap it to install manually.

### Requirements

* Windows 10/11 (x64)
* A phone with **USB debugging** enabled, or an Android emulator
* [Android Platform Tools](https://developer.android.com/tools/releases/platform-tools) (for `adb`)

## How it is built

The launcher is a plain Win32 C program (no runtime dependencies).

```bash
# 1. build the Android app (GitHub Actions does this automatically)
./gradlew :app:assembleGmsMobileUniversalDebug

# 2. bundle that APK into the launcher executable
python3 launcher/build_launcher.py \
    --apk app/build/outputs/apk/gmsMobileUniversal/debug/app-gms-mobile-universal-debug.apk \
    --out dist/OJ-MUSIC.exe
```

`build_launcher.py` compiles `ojmusic_launcher.c` with the first compiler it finds
(`x86_64-w64-mingw32-gcc`, `zig cc`, `python3 -m ziglang cc`) and appends the APK plus a small
footer (`[exe][apk][8-byte size]["OJMUSIC1"]`) so the result is one self-contained file.

## Cloud build

`ci-workflow-improved.yml` is a drop-in replacement for `.github/workflows/build.yml` that
builds **both** the APK and this Windows launcher and uploads them as workflow artifacts.
Copy it in from a machine that has *workflows* permission:

```bash
cp launcher/ci-workflow-improved.yml .github/workflows/build.yml
git commit -am "CI: build APK + Windows launcher" && git push
```
