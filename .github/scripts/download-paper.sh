#!/usr/bin/env bash
# Downloads the newest Paper build of a Minecraft version and checks its SHA-256.
#
#   .github/scripts/download-paper.sh <minecraft version> <output jar>
#
# Prints "Paper <version> build <n> (<channel>)".
set -euo pipefail

VERSION=$1
OUT=$2
UA="CoManyWorld-server-test (https://github.com/${GITHUB_REPOSITORY:-MC-OSOC/CoManyWorld})"

BUILDS=$(curl -fsSL -A "$UA" "https://fill.papermc.io/v3/projects/paper/versions/$VERSION/builds")
# Newest build of any channel: new Minecraft versions may only have BETA builds.
IFS=$'\t' read -r BUILD CHANNEL URL SHA256 < <(jq -r '
  sort_by(.id) | last
  | [.id, .channel, .downloads["server:default"].url, .downloads["server:default"].checksums.sha256]
  | @tsv' <<< "$BUILDS")

mkdir -p "$(dirname "$OUT")"
curl -fsSL -A "$UA" -o "$OUT" "$URL"
echo "$SHA256  $OUT" | sha256sum -c - > /dev/null
echo "Paper $VERSION build $BUILD ($CHANNEL)"
