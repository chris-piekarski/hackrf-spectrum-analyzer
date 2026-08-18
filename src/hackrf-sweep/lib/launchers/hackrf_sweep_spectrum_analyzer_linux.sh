#!/bin/bash
set -euo pipefail
DIRECTORY=$(cd -- "$(dirname -- "$0")" && pwd)
MIN_JAVA=21

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
	JAVA_BIN="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
	JAVA_BIN=$(command -v java)
else
	echo "Java $MIN_JAVA or newer is required but 'java' was not found." >&2
	echo "Install a headful JDK, e.g.: sudo apt install openjdk-21-jdk" >&2
	exit 1
fi

JAVA_VER=$("$JAVA_BIN" -version 2>&1 | awk -F[\".] '/version/ {print ($2=="1"?$3:$2); exit}')
if [ -z "${JAVA_VER:-}" ] || [ "$JAVA_VER" -lt "$MIN_JAVA" ]; then
	echo "Java $MIN_JAVA or newer is required (found: $("$JAVA_BIN" -version 2>&1 | head -1))." >&2
	echo "Install a headful JDK, e.g.: sudo apt install openjdk-21-jdk" >&2
	exit 1
fi

if echo "${JAVA_TOOL_OPTIONS:-} ${JDK_JAVA_OPTIONS:-}" | grep -q 'java.awt.headless=true'; then
	echo "This UI needs a headful JDK (java.awt.headless is set)." >&2
	exit 1
fi

JAVA_HOME_DIR=$("$JAVA_BIN" -XshowSettings:properties -version 2>&1 | awk -F= '/java.home/ {gsub(/^[ \t]+|[ \t]+$/,"",$2); print $2; exit}')
if [ -n "$JAVA_HOME_DIR" ] && [ ! -e "$JAVA_HOME_DIR/lib/libawt_xawt.so" ] && [ ! -e "$JAVA_HOME_DIR/lib/amd64/libawt_xawt.so" ]; then
	echo "This UI needs a headful JDK (no libawt_xawt in $JAVA_HOME_DIR)." >&2
	echo "Install e.g.: sudo apt install openjdk-21-jdk   (not openjdk-21-jdk-headless)" >&2
	exit 1
fi

cd "$DIRECTORY"
exec "$JAVA_BIN" -Djna.platform.library.path=lib/linux-x86-64 -jar "$DIRECTORY/lib/hackrf_sweep_spectrum_analyzer.jar" "$@"
