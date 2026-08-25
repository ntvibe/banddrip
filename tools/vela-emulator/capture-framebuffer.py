#!/usr/bin/env python3
import argparse
import pathlib
import subprocess
import sys

from PIL import Image


def adb_bytes(serial: str, command: list[str], timeout: int = 15) -> bytes:
    proc = subprocess.run(
        ["adb", "-s", serial, "exec-out", *command],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        timeout=timeout,
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError(proc.stderr.decode("utf-8", "replace"))
    return proc.stdout


def main() -> int:
    parser = argparse.ArgumentParser(description="Capture Xiaomi Vela/NuttX /dev/fb0 via ADB")
    parser.add_argument("--serial", default="emulator-5554")
    parser.add_argument("--width", type=int, default=336)
    parser.add_argument("--height", type=int, default=480)
    parser.add_argument("--output-dir", default="emulator-evidence")
    args = parser.parse_args()

    out = pathlib.Path(args.output_dir)
    out.mkdir(parents=True, exist_ok=True)
    frame_bytes = args.width * args.height * 4

    raw = b""
    errors: list[str] = []
    for command in (["dd", "if=/dev/fb0", f"bs={frame_bytes}", "count=2"], ["cat", "/dev/fb0"]):
        try:
            raw = adb_bytes(args.serial, list(command))
            if len(raw) >= frame_bytes:
                break
            errors.append(f"{' '.join(command)} returned only {len(raw)} bytes")
        except Exception as exc:  # evidence collection should explain, not hide, failures
            errors.append(f"{' '.join(command)}: {exc}")

    (out / "framebuffer-errors.txt").write_text("\n".join(errors), encoding="utf-8")
    if len(raw) < frame_bytes:
        print(f"Framebuffer capture failed: got {len(raw)} bytes, need at least {frame_bytes}", file=sys.stderr)
        return 1

    (out / "framebuffer.raw").write_bytes(raw)
    buffers = min(2, len(raw) // frame_bytes)
    for index in range(buffers):
        frame = raw[index * frame_bytes : (index + 1) * frame_bytes]
        # NuttX FB_FMT_RGB32 is a 32-bit RGB integer. On little-endian emulator hosts
        # its bytes are typically B,G,R,X/A, so decode as BGRA and discard alpha.
        image = Image.frombytes("RGBA", (args.width, args.height), frame, "raw", "BGRA").convert("RGB")
        path = out / f"screen-buffer{index}.png"
        image.save(path)
        print(f"Saved {path} ({args.width}x{args.height})")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
