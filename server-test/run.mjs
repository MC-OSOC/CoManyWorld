// Real-server test for CoManyWorld.
//
// Starts a real Paper or Spigot server with the plugin, drives it from the console and with a
// mineflayer bot that joins as an ordinary player, then restarts the server to check that the
// world types are applied again when the worlds are loaded on startup.
//
//   node run.mjs --server-jar paper.jar --plugin-jar CoManyWorld.jar --mc 1.21.11 [--dir work] [--java java]
//
// Every check is reported at the end (and in the GitHub step summary); the exit code is 1 if a
// non-"soft" check failed. Soft checks are things that depend on the client library (tab completion).
import { spawn } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { parseArgs } from 'node:util';
import mineflayer from 'mineflayer';

const { values: opts } = parseArgs({
  options: {
    'server-jar': { type: 'string' },
    'plugin-jar': { type: 'string' },
    mc: { type: 'string' },
    dir: { type: 'string', default: 'work' },
    java: { type: 'string', default: 'java' },
    port: { type: 'string', default: '25565' },
    label: { type: 'string', default: 'server' },
  },
});
for (const required of ['server-jar', 'plugin-jar', 'mc']) {
  if (!opts[required]) {
    console.error(`Missing --${required}`);
    process.exit(2);
  }
}

const SERVER_DIR = path.resolve(opts.dir);
const PORT = Number(opts.port);
const PLAYER = 'Steve';
const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
// eslint-disable-next-line no-control-regex
const stripAnsi = (text) => text.replace(/\x1b\[[0-9;?]*[ -/]*[@-~]/g, '').replace(/\r/g, '');

// ------------------------------------------------------------------ results

const results = [];

async function check(name, fn, { soft = false } = {}) {
  try {
    const detail = await fn();
    if (detail === false) {
      throw new Error('condition was false');
    }
    results.push({ name, ok: true, soft, detail: typeof detail === 'string' ? detail : '' });
    console.log(`\n  [PASS] ${name}\n`);
  } catch (error) {
    results.push({ name, ok: false, soft, detail: String(error.message ?? error).slice(0, 1500) });
    console.log(`\n  [${soft ? 'WARN' : 'FAIL'}] ${name}: ${error.message ?? error}\n`);
  }
}

function expect(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

// ------------------------------------------------------------------ server process

class Server {
  constructor(label) {
    this.label = label;
    this.log = '';
    this.exited = false;
    this.exitCode = null;
  }

  start() {
    this.log = '';
    this.exited = false;
    this.proc = spawn(opts.java, ['-Xmx2G', '-jar', path.basename(opts['server-jar']), '--nogui'], {
      cwd: SERVER_DIR,
      stdio: ['pipe', 'pipe', 'pipe'],
    });
    const onData = (data) => {
      const text = stripAnsi(data.toString());
      this.log += text;
      process.stdout.write(text);
    };
    this.proc.stdout.on('data', onData);
    this.proc.stderr.on('data', onData);
    this.proc.on('exit', (code) => {
      this.exited = true;
      this.exitCode = code;
    });
  }

  send(command) {
    console.log(`\n> ${command}`);
    this.proc.stdin.write(command + '\n');
  }

  /** Console output printed after position `from` of the log. */
  since(from) {
    return this.log.slice(from);
  }

  /** Waits until the output after `from` matches, and returns that output. */
  async waitFor(regex, timeoutMs = 30000, from = 0) {
    const end = Date.now() + timeoutMs;
    while (Date.now() < end) {
      const out = this.since(from);
      if (regex.test(out)) {
        return out;
      }
      if (this.exited) {
        throw new Error(`server exited (code ${this.exitCode}) while waiting for ${regex}`);
      }
      await sleep(100);
    }
    throw new Error(`timed out after ${timeoutMs / 1000}s waiting for ${regex}\n--- output since the command ---\n${this.since(from).slice(-1500)}`);
  }

  /** Runs a console command and returns the output once `regex` shows up. */
  async run(command, regex, timeoutMs = 30000) {
    const mark = this.log.length;
    this.send(command);
    return this.waitFor(regex, timeoutMs, mark);
  }

  /** Runs a console command and returns what it printed after a short pause. */
  async runQuiet(command, waitMs = 1000) {
    const mark = this.log.length;
    this.send(command);
    await sleep(waitMs);
    return this.since(mark);
  }

  async boot() {
    this.start();
    await this.waitFor(/Done \([0-9.,]+s\)!/, 360000);
  }

  async stop() {
    if (this.exited) {
      return;
    }
    this.send('stop');
    const end = Date.now() + 120000;
    while (!this.exited && Date.now() < end) {
      await sleep(200);
    }
    if (!this.exited) {
      this.proc.kill('SIGKILL');
      throw new Error('server did not stop within 120s');
    }
  }
}

// ------------------------------------------------------------------ bot (a real player)

class Player {
  constructor(name) {
    this.name = name;
    this.messages = [];
  }

  async join() {
    this.bot = mineflayer.createBot({
      host: '127.0.0.1',
      port: PORT,
      username: this.name,
      version: opts.mc,
      auth: 'offline',
      checkTimeoutInterval: 120000,
    });
    this.bot.on('messagestr', (message) => {
      this.messages.push(message);
      console.log(`[${this.name} <-] ${message}`);
    });
    this.bot.on('kicked', (reason) => console.log(`[${this.name}] kicked: ${JSON.stringify(reason)}`));
    this.bot.on('error', (error) => console.log(`[${this.name}] error: ${error.message}`));
    await new Promise((resolve, reject) => {
      const timer = setTimeout(() => reject(new Error(`${this.name} did not spawn within 60s`)), 60000);
      this.bot.once('spawn', () => {
        clearTimeout(timer);
        resolve();
      });
      this.bot.once('end', (reason) => {
        clearTimeout(timer);
        reject(new Error(`${this.name} was disconnected: ${reason}`));
      });
    });
    await sleep(1500);
  }

  /** Sends a chat command and returns the messages the server answered with. */
  async command(text, waitMs = 1500) {
    const from = this.messages.length;
    console.log(`[${this.name} ->] ${text}`);
    this.bot.chat(text);
    await sleep(waitMs);
    return this.messages.slice(from).join('\n');
  }

  async waitMessage(regex, from, timeoutMs = 15000) {
    const end = Date.now() + timeoutMs;
    while (Date.now() < end) {
      const out = this.messages.slice(from).join('\n');
      if (regex.test(out)) {
        return out;
      }
      await sleep(100);
    }
    throw new Error(`${this.name} did not receive ${regex}; got:\n${this.messages.slice(from).join('\n')}`);
  }

  async leave() {
    try {
      this.bot.quit();
    } catch {
      // already gone
    }
    await sleep(1000);
  }
}

// ------------------------------------------------------------------ helpers that ask the server

let marker = 0;
const dimension = (world) => `minecraft:${world}`;

/** True if the block at x y z of a world is `block` (the chunk is force-loaded first). */
async function blockIs(server, world, x, y, z, block, timeoutMs = 60000) {
  const end = Date.now() + timeoutMs;
  let last = '';
  while (Date.now() < end) {
    const tag = `MARK${++marker}`;
    const mark = server.log.length;
    server.send(`execute in ${dimension(world)} if block ${x} ${y} ${z} ${block} run say ${tag}`);
    await sleep(800);
    const out = server.since(mark);
    if (out.includes(`[Server] ${tag}`)) {
      return true;
    }
    if (/not loaded/i.test(out)) {
      last = out;
      await sleep(500);
      continue;
    }
    if (/Unknown dimension|Incorrect argument|Unknown or incomplete/i.test(out)) {
      throw new Error(`the server did not understand the command: ${out.trim()}`);
    }
    return false;
  }
  throw new Error(`the chunk never loaded: ${last.trim()}`);
}

async function forceload(server, world, x, z) {
  const out = await server.runQuiet(`execute in ${dimension(world)} run forceload add ${x} ${z}`, 1500);
  expect(!/Unknown dimension|Incorrect argument/i.test(out), `forceload failed: ${out.trim()}`);
}

/** True if `player` is standing in `world`, according to the server. */
async function playerIn(server, player, world, timeoutMs = 15000) {
  const end = Date.now() + timeoutMs;
  while (Date.now() < end) {
    const tag = `IN${++marker}`;
    const mark = server.log.length;
    server.send(`execute as ${player} at @s if dimension ${dimension(world)} run say ${tag}`);
    await sleep(700);
    // "say" run as a player is printed as "[<player>] <tag>"
    if (new RegExp(`\\[${player}\\] ${tag}\\s*$`, 'm').test(server.since(mark))) {
      return true;
    }
  }
  return false;
}

/** Output of /co-many list, read after a short pause so the whole multi-line message is there. */
async function getList(server) {
  const mark = server.log.length;
  server.send('co-many list');
  await server.waitFor(/Many World > List/, 15000, mark);
  await sleep(700);
  return server.since(mark);
}

const configPath = () => path.join(SERVER_DIR, 'plugins', 'CoManyWorld', 'config.yml');
const readConfig = () => fs.readFileSync(configPath(), 'utf8');

// ------------------------------------------------------------------ set up the server folder

function prepare() {
  fs.rmSync(SERVER_DIR, { recursive: true, force: true });
  fs.mkdirSync(path.join(SERVER_DIR, 'plugins'), { recursive: true });
  fs.copyFileSync(opts['server-jar'], path.join(SERVER_DIR, path.basename(opts['server-jar'])));
  fs.copyFileSync(opts['plugin-jar'], path.join(SERVER_DIR, 'plugins', 'CoManyWorld.jar'));
  fs.writeFileSync(path.join(SERVER_DIR, 'eula.txt'), 'eula=true\n');
  fs.writeFileSync(
    path.join(SERVER_DIR, 'server.properties'),
    [
      'online-mode=false',
      `server-port=${PORT}`,
      'enforce-secure-profile=false',
      'view-distance=4',
      'simulation-distance=4',
      'spawn-protection=0',
      'motd=CoManyWorld CI',
      'level-seed=1',
      // The bot must not be killed by mobs (flat worlds spawn slimes)
      'difficulty=peaceful',
      'spawn-monsters=false',
      'spawn-animals=false',
      'max-tick-time=-1',
      '',
    ].join('\n'),
  );
  // Every player may use /co-many tp to the overworld of "combo" (but not to its Nether):
  // this is how a permissions plugin would give co.many.worlds.tp.<world>.
  fs.writeFileSync(
    path.join(SERVER_DIR, 'permissions.yml'),
    ['co.many.worlds.tp.many_world/combo/world:', '  default: true', ''].join('\n'),
  );
}

// ------------------------------------------------------------------ the test

const WORLDS = {
  flat: 'many_world/flat1/world',
  void: 'many_world/void1/world',
  large: 'many_world/large1/world',
  amplified: 'many_world/amplified1/world',
  combo: 'many_world/combo/world',
  comboNether: 'many_world/combo/world_nether',
  comboEnd: 'many_world/combo/world_the_end',
  gen: 'many_world/gen/world',
};

const server = new Server(opts.label);
let player;

async function phaseOne() {
  await check('server boots and the plugin loads', async () => {
    await server.boot();
    expect(/\[CoManyWorld\] Running on/.test(server.log), 'the plugin did not print its "Running on" line');
    expect(!/Could not load 'plugins\/CoManyWorld/.test(server.log), 'the server could not load the plugin');
    expect(!/Error occurred while enabling CoManyWorld/.test(server.log), 'the plugin failed to enable');
    return (server.log.match(/Running on ([^\n]*)/) ?? [])[1];
  });

  await check('/co-many about shows the version', async () => {
    const out = await server.run('co-many about', /Minecraft Server Version >>>/);
    expect(/Version >>> \S+/.test(out), 'no version in the output');
  });

  // ---- the new world types
  for (const [kind, name, flag] of [
    ['flat', 'flat1', '-flat'],
    ['void', 'void1', '-void'],
    ['large', 'large1', '-large'],
    ['amplified', 'amplified1', '-amplified'],
  ]) {
    await check(`create ${flag} world`, async () => {
      const out = await server.run(`co-many create ${name} -11 ${flag}`, new RegExp(`World ${name} created|Failed to create`), 120000);
      expect(new RegExp(`World ${name} created: ${WORLDS[kind]} Type: ${kind}`).test(out), `unexpected output: ${out.trim()}`);
    });
  }

  await check('create -all with a seed (type only changes the overworld)', async () => {
    const out = await server.run('co-many create combo -all -s12345 -flat', /World combo created|Failed to create/, 180000);
    expect(/created: many_world\/combo\/world, many_world\/combo\/world_nether, many_world\/combo\/world_the_end/.test(out), out.trim());
    expect(/Seed: 12345/.test(out) && /Type: flat/.test(out), out.trim());
  });

  await check('create with a generator from a plugin (-gCoManyWorld:x)', async () => {
    const out = await server.run('co-many create gen -11 -gCoManyWorld:x', /World gen created|Failed to create/, 120000);
    expect(/World gen created/.test(out), out.trim());
  });

  await check('bad create arguments are rejected and create nothing', async () => {
    const unknownPlugin = await server.runQuiet('co-many create bad1 -11 -gNoSuchPlugin');
    expect(/not installed or not enabled/.test(unknownPlugin), unknownPlugin.trim());
    const twoTypes = await server.runQuiet('co-many create bad2 -11 -flat -void');
    expect(/only one/.test(twoTypes), twoTypes.trim());
    const netherFlat = await server.runQuiet('co-many create bad3 -12 -flat');
    expect(/overworld/.test(netherFlat), netherFlat.trim());
    await sleep(1000);
    const list = await getList(server);
    expect(!/many_world\/bad[123]/.test(list), `a rejected world exists:\n${list}`);
  });

  await check('/co-many list shows the new worlds', async () => {
    const out = await getList(server);
    for (const world of Object.values(WORLDS)) {
      expect(out.includes(world), `${world} is missing from the list:\n${out}`);
    }
  });

  await check('config.yml keeps the world types', async () => {
    const config = readConfig();
    expect(/many_world\/void1\/world:\s*\n\s+type: void/.test(config), `void1 missing:\n${config}`);
    expect(/many_world\/flat1\/world:\s*\n\s+type: flat/.test(config), 'flat1 missing');
    expect(/many_world\/large1\/world:\s*\n\s+type: large/.test(config), 'large1 missing');
    expect(/many_world\/amplified1\/world:\s*\n\s+type: amplified/.test(config), 'amplified1 missing');
    expect(/many_world\/combo\/world:\s*\n\s+type: flat/.test(config), 'combo missing');
    expect(/many_world\/gen\/world:\s*\n\s+generator: ['"]?CoManyWorld:x/.test(config), 'gen missing');
    expect(!/combo\/world_nether:\s*\n\s+type/.test(config), 'the Nether of combo must not have a type');
  });

  // ---- the terrain really is what was asked for
  await check('void world: stone platform at the spawn, nothing below it', async () => {
    await forceload(server, WORLDS.void, 8, 8);
    expect(await blockIs(server, WORLDS.void, 8, 63, 8, 'minecraft:stone'), 'no stone platform at 8 63 8');
    expect(await blockIs(server, WORLDS.void, 8, 62, 8, 'minecraft:air'), 'there is something below the platform');
  });

  await check('flat world: bedrock, dirt and grass layers', async () => {
    await forceload(server, WORLDS.flat, 0, 0);
    expect(await blockIs(server, WORLDS.flat, 0, -64, 0, 'minecraft:bedrock'), 'no bedrock at y=-64');
    expect(await blockIs(server, WORLDS.flat, 0, -61, 0, 'minecraft:grass_block'), 'no grass at y=-61');
    expect(await blockIs(server, WORLDS.flat, 0, -60, 0, 'minecraft:air'), 'not flat: something above the grass');
  });

  await check('large biomes and amplified worlds generate terrain', async () => {
    for (const world of [WORLDS.large, WORLDS.amplified]) {
      await forceload(server, world, 0, 0);
      expect(await blockIs(server, world, 0, -64, 0, 'minecraft:bedrock'), `no bedrock at the bottom of ${world}`);
    }
  });

  // ---- a real player
  player = new Player(PLAYER);
  await check('a player can join', () => player.join());

  await check('player without permission: can tp to the world they were given, not to others', async () => {
    const denied = await player.command(`/co-many tp ${WORLDS.comboNether}`);
    expect(/permission to teleport/.test(denied), `expected a refusal, got: ${denied}`);
    expect(!(await playerIn(server, PLAYER, WORLDS.comboNether, 3000)), 'the player was teleported anyway');

    const tpFrom = player.messages.length;
    player.bot.chat(`/co-many tp ${WORLDS.combo}`);
    await player.waitMessage(/Teleported to/, tpFrom, 20000);
    expect(await playerIn(server, PLAYER, WORLDS.combo), 'the player is not in the combo world');
  });

  await check('player without permission cannot use admin commands', async () => {
    for (const command of ['/co-many create hacked -11', '/co-many list', '/co-many delete many_world/gen/world', '/co-many backup world', '/co-many about']) {
      const out = await player.command(command, 800);
      expect(/permission/.test(out), `${command} answered: ${out || '(nothing)'}`);
    }
    const help = await player.command('/co-many', 800);
    expect(/\/co-many tp/.test(help) && !/\/co-many create/.test(help), `help was: ${help}`);
    const list = await getList(server);
    expect(!list.includes('hacked'), 'a world was created by a player without permission');
    expect(fs.existsSync(configPath()) && !/hacked/.test(readConfig()), 'config.yml mentions the hacked world');
  });

  await check('tab completion only offers what the player may use', async () => {
    const raw = await player.bot.tabComplete('/co-many ', true, false, 8000);
    const matches = raw.map((m) => (typeof m === 'string' ? m : m.match));
    expect(matches.includes('tp') && !matches.includes('create') && !matches.includes('delete'), `offered: ${matches.join(', ')}`);
    const worlds = (await player.bot.tabComplete('/co-many tp many', true, false, 8000)).map((m) => (typeof m === 'string' ? m : m.match));
    expect(worlds.includes(WORLDS.combo) && !worlds.includes(WORLDS.comboNether), `worlds offered: ${worlds.join(', ')}`);
  }, { soft: true });

  await check('after /op the same player can create worlds and tp anywhere', async () => {
    await server.run(`op ${PLAYER}`, /Made .* a server operator/);
    await sleep(500);
    // The answer goes to the player who typed the command, not to the console
    const from = player.messages.length;
    player.bot.chat('/co-many create fromplayer -11 -flat');
    await player.waitMessage(/World fromplayer created: many_world\/fromplayer\/world Type: flat/, from, 120000);
    const tpFrom = player.messages.length;
    player.bot.chat(`/co-many tp ${WORLDS.void}`);
    await player.waitMessage(/Teleported to/, tpFrom, 20000);
    expect(await playerIn(server, PLAYER, WORLDS.void), 'the player is not in the void world');
  });

  await check('void world: the player lands on the platform at the world spawn', async () => {
    const end = Date.now() + 15000;
    let position;
    while (Date.now() < end) {
      position = player.bot.entity?.position;
      if (position && Math.abs(position.x - 8) < 2 && Math.abs(position.z - 8) < 2) {
        break;
      }
      await sleep(250);
    }
    expect(position && Math.abs(position.x - 8) < 2 && Math.abs(position.z - 8) < 2, `position is ${position}`);
    expect(position.y >= 63.5 && position.y <= 65.5, `y is ${position.y}: the player is not standing on the platform`);
  });

  await check('/co-many tp back to the default world', async () => {
    const tpFrom = player.messages.length;
    player.bot.chat('/co-many tp world');
    await player.waitMessage(/Teleported to/, tpFrom, 20000);
    expect(await playerIn(server, PLAYER, 'world'), 'the player did not return to "world"');
  });

  await player.leave();

  // ---- delete forgets the world type
  await check('deleting a world removes its world-settings', async () => {
    expect(/many_world\/gen\/world:/.test(readConfig()), 'gen is not in config.yml before the delete');
    const out = await server.run(`co-many delete ${WORLDS.gen}`, /marked for deletion|Could not unload|World not found/);
    expect(/marked for deletion/.test(out), out.trim());
    expect(!/many_world\/gen\/world/.test(readConfig()), 'gen is still in config.yml');
  });

  await check('no plugin stack traces in the log so far', async () => {
    expect(stackTraces(server.log) === '', stackTraces(server.log).slice(0, 1200));
  });

  await check('server stops cleanly', () => server.stop());
}

/** Lines of the log that look like a stack trace through the plugin. */
function stackTraces(log) {
  return log
    .split('\n')
    .filter((line) => /^\s+at org\.cakedek\.comanyworld\./.test(line) || /\[CoManyWorld\].*(Exception|SEVERE)/.test(line))
    .join('\n');
}

async function phaseTwo() {
  await check('restart: the server boots again', () => server.boot());

  await check('restart: the plugin loads the saved worlds again', async () => {
    for (const world of [WORLDS.flat, WORLDS.void, WORLDS.large, WORLDS.amplified, WORLDS.combo, WORLDS.comboNether, WORLDS.comboEnd]) {
      expect(server.log.includes(`Loading world: ${world}`), `${world} was not loaded on startup`);
    }
    expect(!server.log.includes('Loading world: many_world/gen/world'), 'the deleted world was loaded');
    expect(!/Skipping world/.test(server.log), 'a world was skipped on startup');
    expect(!server.log.includes('Failed to load world'), 'a world failed to load');
  });

  await check('restart: /co-many list still shows them', async () => {
    const out = await getList(server);
    for (const world of [WORLDS.flat, WORLDS.void, WORLDS.combo]) {
      expect(out.includes(world), `${world} is missing:\n${out}`);
    }
  });

  // These chunks did not exist before the restart, so they are generated by the generator the
  // plugin chose when it loaded the world: the world type must have been applied again.
  await check('restart: the void world is still void far away', async () => {
    await forceload(server, WORLDS.void, 1000, 1000);
    for (const y of [-50, 0, 40, 63]) {
      expect(await blockIs(server, WORLDS.void, 1000, y, 1000, 'minecraft:air'), `block at 1000 ${y} 1000 is not air: the world is no longer void`);
    }
  });

  await check('restart: the flat world is still flat far away', async () => {
    await forceload(server, WORLDS.flat, 1000, 1000);
    expect(await blockIs(server, WORLDS.flat, 1000, -61, 1000, 'minecraft:grass_block'), 'no grass at y=-61: the world is no longer flat');
    expect(await blockIs(server, WORLDS.flat, 1000, -60, 1000, 'minecraft:air'), 'something above the grass: the world is no longer flat');
  });

  await check('restart: the player can still join and tp', async () => {
    player = new Player(PLAYER);
    await player.join();
    const tpFrom = player.messages.length;
    player.bot.chat(`/co-many tp ${WORLDS.void}`);
    await player.waitMessage(/Teleported to/, tpFrom, 20000);
    expect(await playerIn(server, PLAYER, WORLDS.void), 'the player is not in the void world');
    await player.leave();
  });

  await check('no plugin stack traces in the log after the restart', async () => {
    expect(stackTraces(server.log) === '', stackTraces(server.log).slice(0, 1200));
  });

  await check('server stops cleanly after the restart', () => server.stop());
}

// ------------------------------------------------------------------ main

function report() {
  const failed = results.filter((r) => !r.ok && !r.soft);
  const warned = results.filter((r) => !r.ok && r.soft);
  const lines = [
    `### ${opts.label} (Minecraft ${opts.mc})`,
    '',
    '| | Check | Details |',
    '|---|---|---|',
    ...results.map((r) => {
      const icon = r.ok ? 'PASS' : r.soft ? 'WARN' : 'FAIL';
      const detail = (r.detail || '').split('\n')[0].replace(/\|/g, '\\|').slice(0, 200);
      return `| ${icon} | ${r.name} | ${detail} |`;
    }),
    '',
    `${results.length - failed.length - warned.length} passed, ${failed.length} failed, ${warned.length} warnings`,
    '',
  ];
  console.log('\n' + lines.join('\n'));
  if (process.env.GITHUB_STEP_SUMMARY) {
    fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, lines.join('\n') + '\n');
  }
  return failed.length;
}

let failures = 1;
try {
  prepare();
  await phaseOne();
  // Without a successful first boot there is nothing to restart
  if (results.some((r) => r.name === 'server boots and the plugin loads' && r.ok)) {
    if (!server.exited) {
      await server.stop().catch(() => {});
    }
    await phaseTwo();
  }
} catch (error) {
  console.error('Unexpected error:', error);
  results.push({ name: 'test run', ok: false, soft: false, detail: String(error.stack ?? error) });
} finally {
  try {
    player?.bot?.quit();
  } catch {
    // ignore
  }
  if (server.proc && !server.exited) {
    server.proc.kill('SIGKILL');
  }
  failures = report();
}
process.exit(failures === 0 ? 0 : 1);
