#!/usr/bin/env node
const fs = require('fs');
const os = require('os');
const path = require('path');
const { execSync } = require('child_process');
const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');

function findProto() {
  const explicit = process.env.VELA_EMULATOR_PROTO;
  const candidates = [
    explicit,
    path.join(os.homedir(), '.vela/sdk/emulator/linux-x86_64/lib/emulator_controller.proto'),
    path.join(os.homedir(), '.vela/sdk/emulator/linux-x86_64/lib64/emulator_controller.proto'),
  ].filter(Boolean);
  for (const candidate of candidates) if (fs.existsSync(candidate)) return candidate;

  const root = path.join(os.homedir(), '.vela/sdk/emulator');
  if (fs.existsSync(root)) {
    try {
      const found = execSync(`find ${JSON.stringify(root)} -name emulator_controller.proto -type f | head -n 1`, {encoding:'utf8'}).trim();
      if (found && fs.existsSync(found)) return found;
    } catch {}
  }
  throw new Error('Could not locate emulator_controller.proto inside the Xiaomi Vela emulator SDK');
}

function listeningPorts() {
  try {
    const text = execSync('lsof -nP -iTCP -sTCP:LISTEN 2>/dev/null || true', {encoding:'utf8'});
    const lines = text.split('\n').filter(line => /qemu|emulator/i.test(line));
    const ports = [];
    for (const line of lines) {
      const match = line.match(/:(\d+)\s+\(LISTEN\)/);
      if (match) ports.push(Number(match[1]));
    }
    return [...new Set(ports)];
  } catch { return []; }
}

function protoIncludeDirs(protoPath) {
  const dirs = [path.dirname(protoPath)];
  try {
    const googleProtoFiles = require('google-proto-files');
    if (googleProtoFiles && typeof googleProtoFiles.getProtoPath === 'function') {
      dirs.push(path.dirname(googleProtoFiles.getProtoPath('empty.proto')));
      dirs.push(path.dirname(path.dirname(googleProtoFiles.getProtoPath('empty.proto'))));
    }
  } catch {}
  return [...new Set(dirs)];
}

function clientFor(port, protoPath) {
  const def = protoLoader.loadSync(protoPath, {
    keepCase: true,
    longs: String,
    enums: String,
    defaults: true,
    oneofs: true,
    includeDirs: protoIncludeDirs(protoPath),
  });
  const loaded = grpc.loadPackageDefinition(def);
  const Controller = loaded.android.emulation.control.EmulatorController;
  if (!Controller) throw new Error('EmulatorController service missing from loaded proto');
  return new Controller(`127.0.0.1:${port}`, grpc.credentials.createInsecure());
}

function screenshot(client, output) {
  return new Promise((resolve, reject) => {
    client.getScreenshot({format:'PNG', width:0, height:0, display:0}, {deadline: Date.now()+5000}, (err, response) => {
      if (err) return reject(err);
      const image = response?.image;
      if (!image || image.length === 0) return reject(new Error('emulator returned an empty screenshot'));
      fs.writeFileSync(output, image);
      resolve({bytes:image.length, format:response.format});
    });
  });
}

async function main() {
  const output = process.argv[2] || 'emulator-evidence/screen-grpc.png';
  fs.mkdirSync(path.dirname(output), {recursive:true});
  const proto = findProto();
  const explicit = process.env.VELA_GRPC_PORT ? Number(process.env.VELA_GRPC_PORT) : null;
  const detected = listeningPorts().filter(p => p >= 8000);
  const ports = [...new Set([explicit, 8554, ...detected].filter(Boolean))];
  fs.writeFileSync(path.join(path.dirname(output), 'grpc-probe.json'), JSON.stringify({proto, ports, allListening:listeningPorts()}, null, 2));

  const errors = [];
  for (const port of ports) {
    const client = clientFor(port, proto);
    try {
      for (let attempt=1; attempt<=4; attempt++) {
        try {
          const result = await screenshot(client, output);
          console.log(JSON.stringify({status:'ok', port, proto, output, ...result}));
          client.close();
          return;
        } catch (err) {
          errors.push({port, attempt, error:String(err.message || err)});
          await new Promise(r => setTimeout(r, 750));
        }
      }
    } finally {
      try { client.close(); } catch {}
    }
  }
  fs.writeFileSync(path.join(path.dirname(output), 'grpc-errors.json'), JSON.stringify(errors, null, 2));
  throw new Error(`gRPC screenshot failed on ports: ${ports.join(', ')}`);
}

main().catch(err => { console.error(err.stack || err); process.exit(1); });
