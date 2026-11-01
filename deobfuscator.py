
#!/usr/bin/env python3
# Smob v1.4 Deobfuscator
# Python 3.6+

import os
import re
import sys
import shutil
import argparse

BANNER = "Smob - Mod obfuscation tool v1.4 by Kirlif"

STATS = {
    "files": 0,
    "changed": 0,
    "garbage": 0,
    "banners": 0,
    "errors": 0
}

STRING_RE = re.compile(
    r'^\s*const-string(?:/jumbo)?\s+([vp]\d+)\s*,\s*"(.*)"\s*$'
)


def is_smob_garbage(line):
    match = STRING_RE.match(line)

    if not match:
        return False

    value = match.group(2)

    tilde = value.count("~")
    at = value.count("@")

    if tilde + at < 5:
        return False

    if not value:
        return False

    density = (tilde + at) / float(len(value))

    return density >= 0.08 and tilde >= 2 and at >= 2


def process_file(path, dry_run=False, no_backup=False):

    try:
        with open(
            path, "r",
            encoding="utf-8",
            errors="surrogateescape"
        ) as f:
            original = f.readlines()

    except OSError as e:
        STATS["errors"] += 1
        print("[ERROR] {}: {}".format(path, e))
        return

    STATS["files"] += 1

    result = []

    for line in original:

        if BANNER in line:
            STATS["banners"] += 1
            continue

        if is_smob_garbage(line):
            STATS["garbage"] += 1
            continue

        result.append(line)

    if result == original:
        return

    STATS["changed"] += 1

    if dry_run:
        print("[FOUND]", path)
        return

    if not no_backup:
        backup = path + ".deobf.bak"

        try:
            if not os.path.exists(backup):
                shutil.copy2(path, backup)
        except OSError as e:
            STATS["errors"] += 1
            print("[BACKUP ERROR]", path, e)
            return

    try:
        with open(
            path, "w",
            encoding="utf-8",
            errors="surrogateescape"
        ) as f:
            f.writelines(result)

        print("[CLEANED]", path)

    except OSError as e:
        STATS["errors"] += 1
        print("[ERROR]", path, e)


def main():

    parser = argparse.ArgumentParser(
        description="Smob v1.4 Recursive Deobfuscator"
    )

    parser.add_argument(
        "folder",
        help="Root APK source folder"
    )

    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Check without modifying files"
    )

    parser.add_argument(
        "--no-backup",
        action="store_true",
        help="Disable backup"
    )

    args = parser.parse_args()

    folder = os.path.realpath(args.folder)

    if not os.path.isdir(folder):
        print("[ERROR] Folder not found:", folder)
        return 1

    print("#############################")
    print(" Smob v1.4 Deobfuscator")
    print(" Recursive mode")
    print("#############################")

    if args.dry_run:
        print("[DRY RUN] No files will be modified.")

    # Scan every subdirectory automatically.
    for root, dirs, files in os.walk(folder):

        for filename in files:

            if filename.endswith(".smali"):
                path = os.path.join(root, filename)

                process_file(
                    path,
                    args.dry_run,
                    args.no_backup
                )

    print("\n#############################")
    print(" Job completed")
    print("#############################")

    print("- Scanned files:", STATS["files"])
    print("- Changed files:", STATS["changed"])
    print("- Removed garbage strings:", STATS["garbage"])
    print("- Removed banners:", STATS["banners"])
    print("- Errors:", STATS["errors"])

    if args.dry_run:
        print("- DRY RUN: files unchanged")
    else:
        print("- Backup: .deobf.bak")

    return 0


if __name__ == "__main__":
    sys.exit(main())
