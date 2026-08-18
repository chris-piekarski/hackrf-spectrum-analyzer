#!/bin/sh
# Install persistent udev rules so HackRF usbfs nodes stay writable
# after usbipd attach / unplug. Requires sudo once.
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SRC="$ROOT/scripts/53-hackrf.rules"
DEST=/etc/udev/rules.d/53-hackrf.rules

[ -f "$SRC" ] || { echo "missing $SRC" >&2; exit 1; }
[ "$(id -u)" -eq 0 ] || exec sudo "$0" "$@"

cp -f "$SRC" "$DEST"
chmod 644 "$DEST"
if command -v udevadm >/dev/null 2>&1; then
	udevadm control --reload-rules
	udevadm trigger --subsystem-match=usb --action=add || true
fi
echo "installed $DEST"
echo "Re-plug or: usbipd attach --wsl --busid <id>   then: make info"
echo "WSL auto-reattach: usbipd attach --wsl --auto-attach --hardware-id 1d50:6089"
exit 0
