#!/usr/bin/env python3
"""
GraphWalker CLI (offline/check) evaluates edge guards as JavaScript. MBT models use
guard strings like "gwGuard_loginCredentialsOk" which in Java are method names;
the CLI has no such functions → ReferenceError.

Fix: set model-level `actions` to assign each unique gwGuard_* to true, so the guard
expression (identifier) is truthy. Maven/Selenium still uses BaseImpl which invokes
Java boolean guards and ignores these JS bindings.

Run from repo:  python mbt-gw/scripts/inject_gw_cli_guard_stubs.py
"""
from __future__ import annotations

import json
import re
from pathlib import Path

GW_GUARD = re.compile(r'"guard":\s*"(gwGuard_[a-zA-Z0-9_]+)"')


def main() -> None:
    root = Path(__file__).resolve().parent.parent / "src" / "test" / "resources" / "com" / "eduplatform" / "mbt" / "models"
    for path in sorted(root.glob("*.json")):
        with path.open(encoding="utf-8") as f:
            text = f.read()
        data = json.loads(text)
        guards = set(GW_GUARD.findall(text))
        if not guards:
            print(f"skip (no gwGuard) {path.name}")
            continue
        for m in data.get("models", []):
            stmts = [f"{g} = true;" for g in sorted(guards)]
            m["actions"] = stmts
        with path.open("w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
        print(f"OK {path.name}  ({len(guards)} guards)")


if __name__ == "__main__":
    main()
