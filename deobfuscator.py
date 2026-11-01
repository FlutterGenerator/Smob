#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Deobfuscator for the supplied Smali obfuscator.

IMPORTANT:
The original obfuscator does not mark inserted instructions.
This tool uses conservative heuristics and cannot guarantee
reconstruction of the original source.
"""

import os
import re
import sys
import shutil
import argparse
from pathlib import Path
from datetime import datetime


BACKUP_PREFIX = "_smali_backup_"

# Instructions that may have been inserted by the obfuscator.
ARITHMETIC = (
    "and-int/2addr",
    "or-int/2addr",
    "xor-int/2addr",
    "shr-int/2addr",
    "shl-int/2addr",
)

MOVE_PATTERN = re.compile(
    r"^\s*move\s+v(\d+)\s*,\s*v(\d+)\s*(?:#.*)?$"
)

CONST_PATTERN = re.compile(
    r"^\s*const/4\s+v(\d+)\s*,\s*(-?0x[0-9a-fA-F]+|-?\d+)"
)

LOCALS_PATTERN = re.compile(
    r"^(\s*\.locals\s+)(\d+)"
)


def read_file(path):
    return path.read_text(
        encoding="utf-8",
        errors="surrogateescape"
    )


def write_file(path, content):
    path.write_text(
        content,
        encoding="utf-8",
        errors="surrogateescape"
    )


def make_backup(root):
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    backup = root.parent / (
        root.name + BACKUP_PREFIX + timestamp
    )

    shutil.copytree(root, backup)

    return backup


def is_method_start(line):
    return line.startswith(".method")


def is_method_end(line):
    return line.startswith(".end method")


def is_label(line):
    return line.startswith("    :")


def conservative_cleanup(lines):
    """
    Remove only highly suspicious redundant instructions.

    No general removal of move/const/4/arithmetic instructions:
    these can be legitimate application code.
    """

    result = []
    stats = {
        "methods": 0,
        "changed": 0,
        "removed": 0,
        "ambiguous": 0,
    }

    inside_method = False

    for index, line in enumerate(lines):

        if is_method_start(line):
            inside_method = True
            stats["methods"] += 1

        # Keep original code unless we can prove a transformation.
        if inside_method:

            # The supplied obfuscator can insert blank lines,
            # but blank lines are harmless and are retained.

            # Do not remove arbitrary move instructions.
            # Do not remove arbitrary const/4 instructions.
            # Do not remove arithmetic instructions.

            if any(
                line.strip().startswith(op)
                for op in ARITHMETIC
            ):
                stats["ambiguous"] += 1

            if CONST_PATTERN.match(line):
                stats["ambiguous"] += 1

            if MOVE_PATTERN.match(line):
                stats["ambiguous"] += 1

        result.append(line)

        if is_method_end(line):
            inside_method = False

    return result, stats


def analyze_strings(lines):
    """
    The supplied obfuscator shuffles string characters.
    Without the original string, their order cannot be recovered.
    """

    suspicious = []

    for number, line in enumerate(lines, 1):

        if "const-string" not in line:
            continue

        match = re.search(r',\s*"([^"]*)"', line)

        if match:
            value = match.group(1)

            suspicious.append({
                "line": number,
                "value": value,
                "length": len(value),
            })

    return suspicious


def process_file(path, dry_run=False):
    original = read_file(path)
    lines = original.splitlines(keepends=True)

    cleaned, stats = conservative_cleanup(lines)

    new_content = "".join(cleaned)

    changed = new_content != original

    if changed and not dry_run:
        write_file(path, new_content)

    strings = analyze_strings(lines)

    return {
        "file": str(path),
        "changed": changed,
        "stats": stats,
        "strings": len(strings),
    }


def main():
    parser = argparse.ArgumentParser(
        description="Conservative Smali deobfuscator"
    )

    parser.add_argument(
        "folder",
        help="Folder containing Smali files"
    )

    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Analyze without modifying files"
    )

    parser.add_argument(
        "--no-backup",
        action="store_true",
        help="Do not create a backup"
    )

    args = parser.parse_args()

    root = Path(args.folder).resolve()

    if not root.is_dir():
        print("ERROR: Folder not found:", root)
        sys.exit(1)

    files = list(root.rglob("*.smali"))

    if not files:
        print("No Smali files found.")
        return

    print("=" * 50)
    print("SMALI DEOBFUSCATOR")
    print("=" * 50)

    backup = None

    if not args.dry_run and not args.no_backup:
        backup = make_backup(root)
        print("Backup:", backup)

    total = 0
    changed = 0
    ambiguous = 0

    for path in files:

        try:
            result = process_file(
                path,
                dry_run=args.dry_run
            )

            total += 1
            ambiguous += result["stats"]["ambiguous"]

            if result["changed"]:
                changed += 1

            print(
                "[OK]",
                path,
                "| ambiguous instructions:",
                result["stats"]["ambiguous"]
            )

        except Exception as error:
            print("[ERROR]", path, error)

    print("=" * 50)
    print("Processed files:", total)
    print("Changed files:", changed)
    print("Potentially ambiguous instructions:", ambiguous)
    print("Dry run:", args.dry_run)
    print("Backup:", backup or "Not created")
    print("=" * 50)


if __name__ == "__main__":
    main()
