#!/bin/sh
# Extract ```mermaid fences from first-party Markdown and parse them.
# Uses mermaid-cli (mmdc) when available — same major as current GitHub Mermaid.
# Always applies GitHub-known pitfalls even without mmdc.
# Invoked by `make mermaid`.

set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

python3 - "$ROOT" "$TMP" <<'PY'
import re, sys
from pathlib import Path

root = Path(sys.argv[1])
tmp = Path(sys.argv[2])
fence = re.compile(r"^```mermaid\n(.*?)```", re.M | re.S)
skip_parts = {".git", "target", "build", "obj", "node_modules", "lib"}
files = []
for p in [root / "README.md", root / "AGENTS.md", root / "CONTRIBUTING.md", root / "CHANGELOG.md"]:
    if p.is_file():
        files.append(p)
files.extend(sorted((root / "docs").rglob("*.md")))

pitfalls = []
index = []
n = 0
for path in files:
    if any(part in skip_parts for part in path.parts):
        continue
    text = path.read_text(encoding="utf-8", errors="replace")
    rel = path.relative_to(root).as_posix()
    for i, block in enumerate(fence.findall(text), 1):
        body = block.strip() + "\n"
        n += 1
        dest = tmp / f"{n:02d}_{rel.replace('/', '_')}_{i}.mmd"
        dest.write_text(body, encoding="utf-8")
        kind = body.splitlines()[0].split()[0] if body.strip() else "?"
        index.append((dest.name, rel, i, kind))
        if kind == "deploymentDiagram":
            pitfalls.append(
                f"{rel} #{i}: deploymentDiagram is not in Mermaid 11 / current GitHub. Use a flowchart."
            )
        if kind == "sequenceDiagram":
            for ln, line in enumerate(body.splitlines(), 1):
                if line.lstrip().startswith("Note ") and ">" in line and '"' not in line:
                    pitfalls.append(
                        f"{rel} #{i} line {ln}: unquoted '>' in a sequence Note is parsed as an arrow."
                    )

manifest = tmp / "index.txt"
manifest.write_text(
    "\n".join(f"{name}\t{rel}\t{i}\t{kind}" for name, rel, i, kind in index) + "\n",
    encoding="utf-8",
)
print(f"extracted {n} mermaid fence(s)")
if pitfalls:
    print("PITFALLS")
    for p in pitfalls:
        print("  " + p)
    sys.exit(2)
PY

if [ ! -s "$TMP/index.txt" ]; then
	echo "no mermaid fences found"
	exit 0
fi

if ! command -v mmdc >/dev/null 2>&1; then
	echo "mmdc not on PATH — skipped render parse. Pitfall checks passed."
	echo "Install @mermaid-js/mermaid-cli for a full GitHub-equivalent parse."
	exit 0
fi

fail=0
while IFS="$(printf '\t')" read -r name rel i kind; do
	[ -n "$name" ] || continue
	err="$TMP/$name.err"
	if mmdc -i "$TMP/$name" -o "$TMP/$name.svg" -q 2>"$err"; then
		printf 'OK   %s #%s (%s)\n' "$rel" "$i" "$kind"
	else
		printf 'FAIL %s #%s (%s)\n' "$rel" "$i" "$kind"
		sed 's/^/     /' "$err" | tail -8
		fail=1
	fi
done < "$TMP/index.txt"

if [ "$fail" -ne 0 ]; then
	echo "mermaid parse failed (mermaid-cli $(mmdc --version 2>/dev/null || echo '?'))"
	exit 1
fi
echo "all mermaid diagrams parsed"
