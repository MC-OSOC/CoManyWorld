#!/usr/bin/env bash
# Starts a real Minecraft server (Paper or Spigot) with the plugin installed, types the plugin's
# commands into the server console and checks the answers. Used by .github/workflows/server-test.yml
#
#   .github/scripts/server-smoke-test.sh <server.jar> <plugin.jar> <empty work folder> [<older server.jar>]
#
# With an older server jar, a world group is first created on that server and must still be there,
# with the same blocks, after the upgrade.
#
# A diamond block is placed in a world and looked for again after a restart and after an import,
# so a world that was silently generated again from scratch is caught.
set -euo pipefail

SERVER_JAR=$(realpath "$1")
PLUGIN_JAR=$(realpath "$2")
DIR=$3
OLD_SERVER_JAR=${4:+$(realpath "$4")}
START_TIMEOUT=${START_TIMEOUT:-600}
STEP_TIMEOUT=${STEP_TIMEOUT:-120}

mkdir -p "$DIR/plugins"
cd "$DIR"
cp "$PLUGIN_JAR" plugins/
echo "eula=true" > eula.txt
cat > server.properties <<'EOF'
online-mode=false
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

# Writes to stderr, so it is still shown when called inside $(...) or with stdout redirected.
fail() {
  {
    echo "::error::$*"
    echo "----- last 80 lines of the server console -----"
    plain_log | tail -n 80
  } >&2
  exit 1
}

# The console output without colour codes.
plain_log() { sed -e 's/\x1b\[[0-9;]*[A-Za-z]//g' "$LOG"; }
lines() { plain_log | wc -l; }

# wait_for <first line to search from> <timeout in seconds> <grep -E pattern>
# Prints the first matching line.
wait_for() {
  local from=$1 timeout=$2 pattern=$3 i match
  for ((i = 0; i < timeout; i++)); do
    # No grep -q / -m: with pipefail they can make the pipeline fail with SIGPIPE.
    match=$(plain_log | tail -n "+$from" | grep -E -- "$pattern" || true)
    if [ -n "$match" ]; then
      head -n 1 <<< "$match"
      return 0
    fi
    if ! kill -0 "$SERVER_PID" 2>/dev/null; then
      fail "The server stopped while waiting for: $pattern"
    fi
    sleep 1
  done
  fail "Timed out after ${timeout}s waiting for: $pattern"
}

# Regex-escapes a fixed text.
fixed() { sed -e 's/[][\.*^$+?(){}|]/\\&/g' <<< "$1"; }

# expect_since_start <exact text>  (must already be in the output of the current server start)
expect_since_start() {
  plain_log | tail -n "+$START_LINE" | grep -F -- "$1" > /dev/null || fail "Missing from the server output: $1"
  echo "  ok: $1"
}

start_server() {
  local jar=$1
  echo "::group::Start $(basename "$jar")"
  rm -f console.in
  mkfifo console.in
  START_LINE=$(( $(lines) + 1 ))
  java -Xms1G -Xmx2G -jar "$jar" --nogui < console.in >> "$LOG" 2>&1 &
  SERVER_PID=$!
  exec 3> console.in
  wait_for "$START_LINE" "$START_TIMEOUT" 'Done \(' > /dev/null
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

# send <console command> <grep -E pattern of the answer>  -> prints the matching line
send() {
  local from
  from=$(( $(lines) + 1 ))
  echo "> $1" >&2
  echo "$1" >&3
  wait_for "$from" "$STEP_TIMEOUT" "$2"
}

# Answers of the plugin that mean a command failed.
PLUGIN_ERRORS='Failed|failed|not found|Invalid|Skipped|Could not|can only be imported|Not enough|does not exist'

# run <console command> <exact text that must appear in the answer> [<grep -E pattern that means it failed>]
run() {
  local answer
  if [ -n "${3:-}" ]; then
    answer=$(send "$1" "$(fixed "$2")|$3")
    case "$answer" in
      *"$2"*) ;;
      *) fail "'$1' failed: $answer" ;;
    esac
  else
    send "$1" "$(fixed "$2")" > /dev/null
  fi
  echo "  ok: $2"
}

# Folder of a world: <server>/<name> (Spigot, Paper 1.21) or <server>/world/dimensions/minecraft/<name> (Paper 26.x)
world_dir() {
  local candidate
  for candidate in "$1" "world/dimensions/minecraft/$1"; do
    if [ -d "$candidate" ]; then
      echo "$candidate"
      return 0
    fi
  done
  return 1
}
expect_world() {
  local dir
  dir=$(world_dir "$1") || fail "No folder for world $1"
  echo "  ok: $1 is in $dir"
}
expect_world_gone() {
  local dir
  if dir=$(world_dir "$1"); then
    fail "Should have been deleted: $dir"
  fi
  echo "  ok: $1 was deleted"
}

show_worlds() {
  echo "World folders:"
  find . -path ./plugins -prune -o -type d \( -path '*many_world*' -o -name dimensions \) -print \
    | grep -vE '/(region|entities|poi|data|DIM-?1|datapacks|stats|advancements|playerdata)(/|$)' \
    | sort | sed 's/^/  /' || true
}

# A diamond block at 0 100 0 marks a world, so we can tell it apart from a newly generated one.
set_marker() {
  send "execute in minecraft:$1 run forceload add 0 0" 'force load' > /dev/null
  sleep 2
  run "execute in minecraft:$1 run setblock 0 100 0 minecraft:diamond_block" "Changed the block"
}
# expect_marker <world> passed|failed
expect_marker() {
  local answer
  send "execute in minecraft:$1 run forceload add 0 0" 'force load' > /dev/null
  sleep 2
  answer=$(send "execute in minecraft:$1 if block 0 100 0 minecraft:diamond_block" 'Test (passed|failed)')
  case "$answer" in
    *"Test $2"*) echo "  ok: marker block in $1: $2" ;;
    *) fail "Marker block in $1: expected '$2', the server answered: $answer" ;;
  esac
}

if [ -n "$OLD_SERVER_JAR" ]; then
  ######## On the older server: a world group that has to survive the upgrade ########
  start_server "$OLD_SERVER_JAR"
  expect_since_start "[CoManyWorld] Running on"
  run "co-many create legacy -all" \
      "World legacy created: many_world/legacy/world, many_world/legacy/world_nether, many_world/legacy/world_the_end"
  set_marker many_world/legacy/world
  # A backup made on the old server, imported on the new one below
  run "co-many backup many_world/legacy/world" "backed up successfully" "$PLUGIN_ERRORS"
  cp "$(find plugins/CoManyWorld/many_world_backup -name '*many_world_legacy_world*.zip' | head -n 1)" old-server-backup.zip
  show_worlds
  stop_server
fi

######## 1st start: create, list, backup, import ########
start_server "$SERVER_JAR"
expect_since_start "[CoManyWorld] Running on"
if [ -n "$OLD_SERVER_JAR" ]; then
  expect_since_start "Loading world: many_world/legacy/world with environment: NORMAL"
  expect_since_start "Loading world: many_world/legacy/world_nether with environment: NETHER"
  expect_since_start "Loading world: many_world/legacy/world_the_end with environment: THE_END"
  expect_marker many_world/legacy/world passed
  expect_world many_world/legacy/world
  cp old-server-backup.zip plugins/CoManyWorld/import/oldbackup.zip
  run "co-many import oldbackup.zip" "World oldbackup has been successfully imported to many_world/oldbackup/world" "$PLUGIN_ERRORS"
  expect_marker many_world/oldbackup/world passed
fi

run "co-many about" "World folders >>>"

run "co-many create smoke -all" \
    "World smoke created: many_world/smoke/world, many_world/smoke/world_nether, many_world/smoke/world_the_end" "$PLUGIN_ERRORS"
show_worlds
expect_world many_world/smoke/world
expect_world many_world/smoke/world_nether
expect_world many_world/smoke/world_the_end
run "co-many create smoke -all" "already exists"

run "co-many list" "many_world/smoke/world_the_end"
set_marker many_world/smoke/world

run "co-many backup many_world/smoke/world" "backed up successfully" "$PLUGIN_ERRORS"
BACKUP=$(find plugins/CoManyWorld/many_world_backup -name '*many_world_smoke_world*.zip' | head -n 1)
[ -n "$BACKUP" ] || fail "No backup zip in plugins/CoManyWorld/many_world_backup"
echo "  ok: $BACKUP"
echo "Backup contents:"
unzip -Z1 "$BACKUP" | grep -vE '/(region|entities|poi)/.' | head -n 40 | sed 's/^/  /' || true

# Import the backup again under a new name; it must contain the marker block.
cp "$BACKUP" plugins/CoManyWorld/import/restored.zip
run "co-many import restored.zip" "World restored has been successfully imported to many_world/restored/world" "$PLUGIN_ERRORS"
expect_world many_world/restored/world
expect_marker many_world/restored/world passed

stop_server

######## 2nd start: the worlds come back with their blocks, then delete and clear ########
start_server "$SERVER_JAR"
expect_since_start "Loading world: many_world/smoke/world with environment: NORMAL"
expect_since_start "Loading world: many_world/smoke/world_nether with environment: NETHER"
expect_since_start "Loading world: many_world/smoke/world_the_end with environment: THE_END"
expect_since_start "Loading world: many_world/restored/world with environment: NORMAL"
if [ -n "$OLD_SERVER_JAR" ]; then
  # Paper 26.x moved these out of <server>/many_world on the first start; they must still be found.
  expect_since_start "Loading world: many_world/legacy/world with environment: NORMAL"
  expect_since_start "Loading world: many_world/oldbackup/world with environment: NORMAL"
fi
if plain_log | tail -n "+$START_LINE" | grep -F -e "Skipping world" -e "Failed to load world" > /dev/null; then
  fail "Some worlds from config.yml did not load"
fi
expect_marker many_world/smoke/world passed
expect_marker many_world/restored/world passed
if [ -n "$OLD_SERVER_JAR" ]; then
  expect_marker many_world/legacy/world passed
fi

run "co-many list" "many_world/restored/world"

run "co-many delete many_world/smoke/" "Use /co-many clear confirm to permanently delete these worlds."
run "co-many clear confirm" "Deleted world folder: many_world/smoke/world_the_end" "$PLUGIN_ERRORS"
expect_world_gone many_world/smoke/world
expect_world_gone many_world/smoke/world_nether
expect_world_gone many_world/smoke/world_the_end
expect_world many_world/restored/world

# The name can be used again, and the new world really is new.
run "co-many create smoke" "World smoke created: many_world/smoke/world" "$PLUGIN_ERRORS"
expect_marker many_world/smoke/world failed

if [ -n "$OLD_SERVER_JAR" ]; then
  run "co-many delete many_world/legacy/" "Use /co-many clear confirm to permanently delete these worlds."
  run "co-many clear confirm" "Deleted world folder: many_world/legacy/world_the_end" "$PLUGIN_ERRORS"
  expect_world_gone many_world/legacy/world
  expect_world_gone many_world/legacy/world_nether
  expect_world_gone many_world/legacy/world_the_end
fi
show_worlds

stop_server

######## Anything thrown by the plugin, or API it could not find? ########
if plain_log | grep -nE 'at org\.cakedek\.|NoSuchMethodError|NoSuchFieldError|NoClassDefFoundError|AbstractMethodError|IncompatibleClassChangeError|Error occurred while enabling CoManyWorld|Could not load .*CoManyWorld'; then
  fail "The plugin threw errors (see the lines above)"
fi
echo "Warnings from the plugin:"
plain_log | grep -F "[CoManyWorld]" | grep -E 'WARN|SEVERE|ERROR' || echo "  (none)"

echo "All checks passed."
