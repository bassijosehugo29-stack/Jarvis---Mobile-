#!/usr/bin/env python3
"""Verify the built artifact, signing certificate and 16 KiB native-library alignment."""
import hashlib
import json
import os
from pathlib import Path
import re
import struct
import subprocess
import zipfile

apk = Path("app/build/outputs/apk/debug/app-debug.apk")
sdk = Path(os.environ.get("ANDROID_HOME", os.environ.get("ANDROID_SDK_ROOT", "")))
tools = sdk / "build-tools" / "35.0.0"
cert = subprocess.check_output([str(tools / "apksigner"), "verify", "--verbose", "--print-certs", str(apk)], text=True)
badging = subprocess.check_output([str(tools / "aapt"), "dump", "badging", str(apk)], text=True)
permissions = subprocess.check_output([str(tools / "aapt"), "dump", "permissions", str(apk)], text=True)
subprocess.run([str(tools / "zipalign"), "-c", "-P", "16", "-v", "4", str(apk)], check=True, stdout=subprocess.DEVNULL)
libraries = []
with zipfile.ZipFile(apk) as archive, apk.open("rb") as raw:
    assert archive.testzip() is None, "APK CRC mismatch"
    for info in archive.infolist():
        if not info.filename.endswith(".so"):
            continue
        body = archive.read(info)
        assert body[:6] == b"\x7fELF\x02\x01", "Expected 64-bit little-endian ELF"
        phoff, = struct.unpack_from("<Q", body, 32)
        entsize, count = struct.unpack_from("<HH", body, 54)
        alignments = []
        for index in range(count):
            offset = phoff + index * entsize
            if struct.unpack_from("<I", body, offset)[0] == 1:
                alignment, = struct.unpack_from("<Q", body, offset + 48)
                assert alignment >= 16384, f"ELF not 16KiB aligned: {info.filename}"
                alignments.append(alignment)
        raw.seek(info.header_offset + 26)
        name_len, extra_len = struct.unpack("<HH", raw.read(4))
        offset = info.header_offset + 30 + name_len + extra_len
        if info.compress_type == zipfile.ZIP_STORED:
            assert offset % 16384 == 0, f"ZIP not 16KiB aligned: {info.filename}"
        libraries.append({"path": info.filename, "elf_load_alignment": alignments, "zip_offset": offset})
result = {
    "apk": str(apk), "sha256": hashlib.sha256(apk.read_bytes()).hexdigest(), "bytes": apk.stat().st_size,
    "package": next(line for line in badging.splitlines() if line.startswith("package:")),
    "certificate_sha1": re.search(r"certificate SHA-1 digest: (\w+)", cert).group(1),
    "certificate_sha256": re.search(r"certificate SHA-256 digest: (\w+)", cert).group(1),
    "signing": "fixed_private_secret" if os.environ.get("JARVIS_KEYSTORE_PATH") else "temporary_debug_key",
    "signature_verified": True, "zipalign_16k": True, "libraries": libraries,
    "permissions": permissions.splitlines(),
}
output = Path("build/verification/apk.json")
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(result, indent=2), encoding="utf-8")
print(f"APK verified: {result['bytes']} bytes, SHA-256 {result['sha256']}. Signing: {result['signing']}.")
