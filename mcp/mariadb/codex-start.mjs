import { spawn } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { parseEnv } from 'node:util';
import { fileURLToPath } from 'node:url';

const directory = dirname(fileURLToPath(import.meta.url));
const envFile = join(directory, '.env.codex');
const required = ['MYSQL_HOST', 'MYSQL_USER', 'MYSQL_PASSWORD', 'MYSQL_DATABASE'];

function fail(message) {
  console.error(`[ultary_mariadb] ${message}`);
  process.exit(1);
}

if (!existsSync(envFile)) {
  fail(`Create ${envFile} from .env.codex.example and enter your DB credentials.`);
}

let settings;
try {
  settings = parseEnv(readFileSync(envFile, 'utf8'));
} catch {
  fail('Unable to read .env.codex. Check the file format and permissions.');
}

const missing = required.filter((name) => !settings[name]?.trim());
if (missing.length) {
  fail(`Fill these fields in .env.codex: ${missing.join(', ')}. No DB connection was attempted.`);
}

const port = Number(settings.MYSQL_PORT || '3306');
const limit = Number(settings.MYSQL_CONNECTION_LIMIT || '2');
if (!Number.isInteger(port) || port < 1 || port > 65535) {
  fail('MYSQL_PORT must be an integer between 1 and 65535.');
}
if (!Number.isInteger(limit) || limit < 1 || limit > 10) {
  fail('MYSQL_CONNECTION_LIMIT must be an integer between 1 and 10.');
}
if (!['true', 'false'].includes(settings.MYSQL_SSL || 'false')) {
  fail('MYSQL_SSL must be true or false.');
}

const npxCli = join(dirname(process.execPath), 'node_modules', 'npm', 'bin', 'npx-cli.js');
if (!existsSync(npxCli)) {
  fail('Cannot find npx-cli.js next to Node.js. Install Node.js with npm.');
}

if (process.argv.includes('--check')) {
  console.error('[ultary_mariadb] Configuration format and Node/npm paths are valid. DB connectivity was not tested.');
  process.exit(0);
}

// Never pass credentials through shell arguments or write protocol logs to stdout.
// Keep the Cursor .env and its MCP configuration independent.
const env = { ...process.env };
for (const name of Object.keys(env)) {
  if (name.startsWith('MYSQL_')) delete env[name];
}
for (const [name, value] of Object.entries(settings)) {
  if (name.startsWith('MYSQL_')) env[name] = value;
}
env.MYSQL_PORT = String(port);
env.MYSQL_SSL = settings.MYSQL_SSL || 'false';
env.MYSQL_CONNECTION_LIMIT = String(limit);

const child = spawn(process.execPath, [npxCli, '--yes', '@ashenud/mcp-mysql-server@1.0.0'], {
  cwd: directory,
  env,
  stdio: 'inherit',
  windowsHide: true,
});

child.on('error', () => fail('Could not start the MCP server. Check Node/npm and network access.'));
child.on('exit', (code) => process.exit(code ?? 1));
for (const signal of ['SIGINT', 'SIGTERM']) {
  process.on(signal, () => child.kill(signal));
}
