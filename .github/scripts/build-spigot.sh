#!/usr/bin/env bash
# Builds a Spigot server jar with BuildTools (Spigot has no download).
#
#   .github/scripts/build-spigot.sh <minecraft version> <output jar> [<java>]
#
# Prints "Spigot <version> (BuildTools)". On failure the end of the BuildTools log is printed.
set -euo pipefail

VERSION=$1
OUT=$(realpath -m "$2")
JAVA=${3:-java}
WORK=$(mktemp -d "${RUNNER_TEMP:-/tmp}/buildtools-XXXXXX")

curl -fsSL -o "$WORK/BuildTools.jar" \
  https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
mkdir -p "$WORK/out" "$(dirname "$OUT")"
if ! (cd "$WORK" && "$JAVA" -jar BuildTools.jar --rev "$VERSION" --output-dir out > buildtools.log 2>&1); then
  tail -n 100 "$WORK/buildtools.log" >&2
  mkdir -p buildtools && cp "$WORK/buildtools.log" "buildtools/buildtools-$VERSION.log"
  exit 1
fi
mv "$WORK"/out/spigot-*.jar "$OUT"
rm -rf "$WORK"
echo "Spigot $VERSION (BuildTools)"
