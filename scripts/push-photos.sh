#!/bin/sh
# Mirror a folder of photographs onto the television.
# HEIC files are converted to JPEG first. The long edge is kept at 2560 or below.
#
# Usage: TV=<ip>:5555 push-photos.sh <folder>

set -eu

if [ "$#" -ne 1 ] || [ ! -d "$1" ] || [ -z "${TV:-}" ]; then
  echo "usage: TV=<ip>:5555 push-photos.sh <folder>" >&2
  exit 1
fi
DEST="/sdcard/Android/data/app.cleanlaunch.tv/files/photos"
STAGE=$(mktemp -d)
trap 'rm -rf "$STAGE"' EXIT

find "$1" -maxdepth 1 -type f | while IFS= read -r file; do
  base=$(basename "$file")
  ext=$(printf '%s' "${base##*.}" | tr '[:upper:]' '[:lower:]')
  case "$ext" in
    heic|heif)
      out="$STAGE/${base%.*}.jpg"
      sips -s format jpeg "$file" --out "$out" >/dev/null
      sips -Z 2560 "$out" >/dev/null
      ;;
    jpg|jpeg|png|webp)
      cp "$file" "$STAGE/$base"
      sips -Z 2560 "$STAGE/$base" >/dev/null || true
      ;;
  esac
done

count=$(find "$STAGE" -maxdepth 1 -type f | wc -l | tr -d ' ')
if [ "$count" -eq 0 ]; then
  echo "no jpg, png, webp, or heic files in $1" >&2
  exit 1
fi

adb -s "$TV" shell "mkdir -p '$DEST'"
adb -s "$TV" shell "find '$DEST' -maxdepth 1 -type f -delete"
for file in "$STAGE"/*; do
  [ -f "$file" ] || continue
  adb -s "$TV" push "$file" "$DEST/$(basename "$file")"
done

echo "pushed $count to $TV:$DEST"
