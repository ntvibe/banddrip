import fs from 'node:fs';
import path from 'node:path';
const root=process.cwd();
const dir=path.join(root,'packages/devices');
const index=JSON.parse(fs.readFileSync(path.join(dir,'index.json'),'utf8'));
const seen=new Set();
for(const file of index.devices){
  const p=path.join(dir,file);
  if(!fs.existsSync(p)) throw new Error(`Missing device profile: ${file}`);
  const d=JSON.parse(fs.readFileSync(p,'utf8'));
  for(const key of ['id','name','width','height','shape','capabilities']) if(d[key]===undefined) throw new Error(`${file}: missing ${key}`);
  if(seen.has(d.id)) throw new Error(`Duplicate device id: ${d.id}`);
  seen.add(d.id);
  if(!Number.isFinite(d.width)||!Number.isFinite(d.height)||d.width<=0||d.height<=0) throw new Error(`${file}: invalid geometry`);
  if(!['capsule','rounded-rectangle'].includes(d.shape)) throw new Error(`${file}: unsupported shape ${d.shape}`);
}
console.log(`Validated ${seen.size} BandDrip device profiles: ${[...seen].join(', ')}`);
