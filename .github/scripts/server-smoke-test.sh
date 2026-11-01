#!/usr/bin/env bash
# Starts a real Minecraft server (Paper or Spigot) with the plugin installed, types the plugin's
# commands into the server console and checks the answers. Used by .github/workflows/server-test.yml
#
#   .github/scripts/server-smoke-test.sh <server.jar> <plugin.jar> <empty work folder>
#
# The server is started twice: the second start checks that the worlds saved in config.yml load again.
set -euo pipefail

SERVER_JAR=$(realpath "$1")
PLUGIN_JAR=$(realpath "$2")
DIR=$3
START_TIMEOUT=${START_TIMEOUT:-600}
STEP_TIMEOUT=${STEP_TIMEOUT:-300}

mkdir -p "$DIR/plugins"
cd "$DIR"
cp "$PLUGIN_JAR" plugins/
echo "eula=true" > eula.txt
cat > server.properties <<'EOF'
online-mode=false
level-type=minecraft\:flat
generate-structures=false
spawn-protection=0
view-distance=4
simulation-distance=4
EOF

LOG=console.log
: > "$LOG"
SERVER_PID=
START_LINE=1

cleanup() {
  if [ -n "$SERVER_PID" ] && kill -0 "$SERVER_PID" 2>/dev/null; then
    kill -9 "$SERVER_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT

fail() {
  echo "::error::$*"
  echo "----- last 80 lines of the server console -----"
  plain_log | tail -n 80
  exit 1
}

# The console output without colour codes.
plain_log() { sed -e 's/\x1b\[[0-9;]*[A-Za-z]//g' "$LOG"; }
lines() { plain_log | wc -l; }

# wait_for <first line to search from> <timeout in seconds> <exact text>
wait_for() {
  local from=$1 timeout=$2 text=$3 i
  for ((i = 0; i < timeout; i++)); do
    # No grep -q: with pipefail it can make the pipeline fail with SIGPIPE.
    if plain_log | tail -n "+$from" | grep -F -- "$text" > /dev/null; then
      return 0
    fi
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
      fail "The server stopped while waiting for: $text"
    fi
    sleep 1
  done
  fail "Timed out after ${timeout}s waiting for: $text"
}

# expect_since_start <exact text>  (must already be in the output of the current server start)
expect_since_start() {
  plain_log | tail -n "+$START_LINE" | grep -F -- "$1" > /dev/null || fail "Missing from the server output: $1"
  echo "  ok: $1"
}

start_server() {
  echo "::group::Start server"
  rm -f console.in
  mkfifo console.in
  START_LINE=$(( $(lines) + 1 ))
  java -Xms1G -Xmx2G -jar "$SERVER_JAR" --nogui < console.in >> "$LOG" 2>&1 &
  SERVER_PID=$!
  exec 3> console.in
  wait_for "$START_LINE" "$START_TIMEOUT" 'Done ('
  echo "Server is up."
  echo "::endgroup::"
}

stop_server() {
  echo "> stop"
  echo "stop" >&3
  local i
  for ((i = 0; i < 180; i++)); do
    kill -0 "$SERVER_PID" 2>/dev/null || break
    sleep 1
  done
  if kill -0 "$SERVER_PID" 2>/dev/null; then
    fail "The server did not stop within 180s"
  fi
  exec 3>&-
  local code=0
  wait "$SERVER_PID" || code=$?
  [ "$code" -eq 0 ] || fail "The server exited with code $code"
  SERVER_PID=
}

# run <console command> <exact text that must appear in the answer>
run() {
  local command=$1 expected=$2 from
  from=$(( $(lines) + 1 ))
  echo "> $command"
  echo "$command" >&3
  wait_for "$from" "$STEP_TIMEOUT" "$expected"
  echo "  ok: $expected"
}

expect_dir() { [ -d "$1" ] || fail "Folder missing: $1"; echo "  ok: folder $1 exists"; }
expect_gone() { [ ! -e "$1" ] || fail "Should have been deleted: $1"; echo "  ok: $1 was deleted"; }

show_worlds() {
  echo "World folders:"
  find many_world -maxdepth 3 2>/dev/null | sort | sed 's/^/  /' || true
}

######## 1st start: create, list, backup, import ########
start_server
expect_since_start "[CoManyWorld] Running on"

run "co-many about" "Minecraft Server Version >>>"

run "co-many create smoke -all" \
    "World smoke created: many_world/smoke/world, many_world/smoke/world_nether, many_world/smoke/world_the_end"
show_worlds
expect_dir many_world/smoke/world
expect_dir many_world/smoke/world_nether
expect_dir many_world/smoke/world_the_end

run "co-many list" "many_world/smoke/world_the_end"

run "co-many backup many_world/smoke/world" "backed up successfully"
BACKUP=$(find plugins/CoManyWorld/many_world_backup -name '*.zip' | head -n 1)
[ -n "$BACKUP" ] || fail "No backup zip in plugins/CoManyWorld/many_world_backup"
echo "  ok: $BACKUP"

# Import the backup again under a new name.
mkdir -p plugins/CoManyWorld/import
cp "$BACKUP" plugins/CoManyWorld/import/restored.zip
run "co-many import restored.zip" "World restored has been successfully imported to many_world/restored/world"
expect_dir many_world/restored/world

stop_server

######## 2nd start: the worlds come back, then delete and clear ########
start_server
expect_since_start "Loading world: many_world/smoke/world with environment: NORMAL"
expect_since_start "Loading world: many_world/smoke/world_nether with environment: NETHER"
expect_since_start "Loading world: many_world/smoke/world_the_end with environment: THE_END"
expect_since_start "Loading world: many_world/restored/world with environment: NORMAL"
if plain_log | tail -n "+$START_LINE" | grep -F -e "Skipping world" -e "Failed to load world" > /dev/null; then
  fail "Some worlds from config.yml did not load"
fi

run "co-many list" "many_world/restored/world"

run "co-many delete many_world/smoke/" "Use /co-many clear confirm to permanently delete these worlds."
run "co-many clear confirm" "Deleted world folder: many_world/smoke/world_the_end"
expect_gone many_world/smoke
expect_dir many_world/restored/world
show_worlds

stop_server

######## Anything thrown by the plugin, or API it could not find? ########
if plain_log | grep -nE 'at org\.cakedek\.|NoSuchMethodError|NoSuchFieldError|NoClassDefFoundError|AbstractMethodError|IncompatibleClassChangeError|Error occurred while enabling CoManyWorld|Could not load .*CoManyWorld'; then
  fail "The plugin threw errors (see the lines above)"
fi
echo "Warnings from the plugin:"
plain_log | grep -F "[CoManyWorld]" | grep -E 'WARN|SEVERE|ERROR' || echo "  (none)"

echo "All checks passed."
