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


def candidate_compilers():
    if os.environ.get("CC"):
        yield os.environ["CC"].split() + ["-target", "x86_64-windows-gnu"]
    if shutil.which("x86_64-w64-mingw32-gcc"):
        yield ["x86_64-w64-mingw32-gcc"]
    if shutil.which("zig"):
        yield ["zig", "cc", "-target", "x86_64-windows-gnu"]
    try:
        import ziglang  # noqa: F401  (installed via: pip install ziglang)

        yield [sys.executable, "-m", "ziglang", "cc", "-target", "x86_64-windows-gnu"]
    except ImportError:
        pass


def compile_launcher(exe_out: Path, cc_cmd: list[str] | None) -> None:
    base = cc_cmd or ["x86_64-w64-mingw32-gcc"]
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
    subprocess.run(cmd, check=True, cwd=ROOT)


def build(apk: Path, out: Path, cc_cmd: list[str] | None) -> None:
    if not apk.is_file():
        sys.exit(f"APK not found: {apk}")
    apk_bytes = apk.read_bytes()
    if len(apk_bytes) < 4 or apk_bytes[:2] != b"PK":
        print(f"warning: {apk} does not start with a ZIP/APK signature", file=sys.stderr)

    out.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        exe = Path(tmp) / "OJMUSIC-launcher.exe"
        errors = []
        for cc in ([cc_cmd] if cc_cmd else list(candidate_compilers())):
            try:
                compile_launcher(exe, cc)
                break
            except Exception as exc:  # try the next compiler
                errors.append(f"{cc}: {exc}")
        else:
            sys.exit("Could not compile the launcher:\n  " + "\n  ".join(errors))

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
