#!/usr/bin/env python3
"""Lightweight APK string scanner for Mi Fitness reverse engineering.

Uses only Python stdlib so it works before JADX/APKTool are installed.
It scans DEX/resources for targeted identifiers and literals.
"""

import hashlib
import json
import os
import re
import sys
import zipfile

TERMS = [
    "M2552B1",
    "M2558B1",
    "p67",
    "p67gl",
    "band 10 pro",
    "smart band 10 pro",
    "ThirdAppDebug",
    "ThirdPartyApp",
    "THIRDPARTY_APP",
    "prepareInstallApp",
    "installRpk",
    "quickapp",
    "rpk",
    "developer",
    "debug",
    "watchface",
    "WATCH_FACE",
    "MASS",
    "WearDebug",
    "FirmwareDebug",
    "DeviceDebug",
]


def ascii_strings(data: bytes, minlen: int = 4):
    pattern = rb"[\x20-\x7e]{%d,}" % minlen
    return [m.group().decode("utf-8", "ignore") for m in re.finditer(pattern, data)]


def sha256_file(path: str):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def scan(path: str):
    out = {
        "path": path,
        "size": os.path.getsize(path),
        "sha256": sha256_file(path),
        "matches": {},
        "dex_files": [],
    }

    with zipfile.ZipFile(path) as z:
        names = z.namelist()
        out["entries"] = len(names)

        for name in names:
            should_scan = name.endswith(".dex") or name.endswith(
                (".xml", ".json", ".txt", ".js", ".lua", ".html", ".properties", ".conf")
            )
            if not should_scan:
                continue

            if name.endswith(".dex"):
                out["dex_files"].append(name)

            try:
                data = z.read(name)
            except Exception:
                continue

            strings = ascii_strings(data)
            lowered = [s.lower() for s in strings]

            for term in TERMS:
                term_lower = term.lower()
                hits = []
                for value, value_lower in zip(strings, lowered):
                    if term_lower in value_lower:
                        hits.append(value[:500])
                        if len(hits) >= 25:
                            break
                if hits:
                    out["matches"].setdefault(term, []).extend(
                        {"file": name, "string": hit} for hit in hits
                    )

    return out


def main():
    if len(sys.argv) < 2:
        raise SystemExit("usage: scan_apk.py <apk> [apk ...]")
    print(json.dumps([scan(path) for path in sys.argv[1:]], ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
