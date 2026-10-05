#!/usr/bin/env python3
"""
Build the OJ MUSIC Windows launcher.

The Android package (APK) is appended to the compiled launcher together with a
small footer, so the resulting .exe is a single self contained file:

    [ launcher.exe ][ apk bytes ][ 8 byte little endian size ][ "OJMUSIC1" ]

Usage:
    python3 launcher/build_launcher.py --apk app-release.apk --out dist/OJ-MUSIC.exe
    python3 launcher/build_launcher.py --cc x86_64-w64-mingw32-gcc --apk x.apk --out y.exe

Compiler selection order: --cc, $CC, x86_64-w64-mingw32-gcc, `zig cc`, `python3 -m ziglang cc`.

Made by Ochen Joshua.
"""

import argparse
import importlib.util
import os
import shutil
import struct
import subprocess
import sys
import tempfile
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parent
SRC = HERE / "ojmusic_launcher.c"
MAGIC = b"OJMUSIC1"
LINK_LIBS = ["-lshell32", "-lcomdlg32", "-lole32", "-luser32", "-lgdi32"]
ZIG_DIR = Path(tempfile.gettempdir()) / "oj-music-ziglang"


def candidate_compilers():
    """Yield (label, command) pairs for every usable Windows cross compiler."""
    cc = os.environ.get("CC", "").strip()
    if cc:
        base = cc.split()
        yield ("$CC", base + (["-target", "x86_64-windows-gnu"] if "zig" in base[0] else []))

    if shutil.which("x86_64-w64-mingw32-gcc"):
        yield ("mingw-w64", ["x86_64-w64-mingw32-gcc"])

    if shutil.which("zig"):
        yield ("zig", ["zig", "cc", "-target", "x86_64-windows-gnu"])

    if (ZIG_DIR / "ziglang").is_dir():
        yield ("ziglang package", [sys.executable, "-m", "ziglang", "cc", "-target", "x86_64-windows-gnu"])


def install_ziglang() -> bool:
    """Fetch the portable zig toolchain into a scratch directory (no system changes)."""
    print("No Windows cross compiler found - fetching the portable zig toolchain...")
    try:
        subprocess.run(
            [sys.executable, "-m", "pip", "install", "--quiet", "--disable-pip-version-check",
             "--target", str(ZIG_DIR), "ziglang"],
            check=True, timeout=900,
        )
    except Exception as exc:
        print(f"  zig toolchain download failed: {exc}")
        return False
    ok = (ZIG_DIR / "ziglang").is_dir()
    print("  zig toolchain ready." if ok else "  zig toolchain still unavailable.")
    return ok


def zig_env() -> dict:
    env = dict(os.environ)
    env["PYTHONPATH"] = str(ZIG_DIR) + os.pathsep + env.get("PYTHONPATH", "")
    return env


def compile_launcher(exe_out: Path, base: list[str], env: dict | None = None) -> None:
    cmd = [
        *base,
        "-O2",
        "-Wl,--subsystem,windows",
        str(SRC),
        "-o",
        str(exe_out),
        *LINK_LIBS,
    ]
    print("+", " ".join(cmd))
    subprocess.run(cmd, check=True, cwd=ROOT, env=env)


def compile_any(exe_out: Path, cc_cmd: list[str] | None) -> None:
    if cc_cmd:
        compile_launcher(exe_out, cc_cmd)
        return

    errors = []
    for label, cmd in candidate_compilers():
        try:
            compile_launcher(exe_out, cmd)
            return
        except Exception as exc:
            errors.append(f"{label}: {exc}")

    if install_ziglang():
        try:
            compile_launcher(exe_out, [sys.executable, "-m", "ziglang", "cc", "-target", "x86_64-windows-gnu"],
                             env=zig_env())
            return
        except Exception as exc:
            errors.append(f"zig (after install): {exc}")

    sys.exit("Could not compile the launcher. Install mingw-w64 (apt install gcc-mingw-w64-x86-64)\n"
             "or a zig toolchain, then retry. Attempts:\n  " + "\n  ".join(errors or ["none"]))


def build(apk: Path, out: Path, cc_cmd: list[str] | None) -> None:
    if not apk.is_file():
        sys.exit(f"APK not found: {apk}")
    apk_bytes = apk.read_bytes()
    if len(apk_bytes) < 4 or apk_bytes[:2] != b"PK":
        print(f"warning: {apk} does not start with a ZIP/APK signature", file=sys.stderr)

    out.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        exe = Path(tmp) / "OJMUSIC-launcher.exe"
        compile_any(exe, cc_cmd)
        payload = exe.read_bytes() + apk_bytes + struct.pack("<Q", len(apk_bytes)) + MAGIC
        out.write_bytes(payload)

    print(f"OK  {out}  ({out.stat().st_size / (1024 * 1024):.1f} MB, APK {len(apk_bytes) / (1024 * 1024):.1f} MB)")


def main() -> None:
    p = argparse.ArgumentParser(description="Build the OJ MUSIC Windows launcher (APK bundled inside).")
    p.add_argument("--apk", required=True, type=Path, help="path to the built APK")
    p.add_argument("--out", default=ROOT / "dist" / "OJ-MUSIC.exe", type=Path, help="output .exe path")
    p.add_argument("--cc", default=None, help="compiler command, e.g. 'x86_64-w64-mingw32-gcc'")
    args = p.parse_args()
    build(args.apk, args.out, args.cc.split() if args.cc else None)


if __name__ == "__main__":
    main()
