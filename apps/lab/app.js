const $ = id => document.getElementById(id);
const controls = ['device','glucose','units','trend','delta','age','iob','showIob'].reduce((a,k)=>(a[k]=$(k),a),{});
let devices = [];
let selected = null;

const fixtureMap = {
  fresh:{glucose:112,units:'mg/dL',trend:'→',delta:6,age:3,iob:.250,showIob:true},
  stale:{glucose:112,units:'mg/dL',trend:'↘',delta:-4,age:12,iob:.250,showIob:true},
  high:{glucose:350,units:'mg/dL',trend:'↑',delta:18,age:2,iob:1.375,showIob:true},
  mmol:{glucose:6.2,units:'mmol/L',trend:'→',delta:.3,age:4,iob:.250,showIob:true},
  missingDelta:{glucose:104,units:'mg/dL',trend:'→',delta:'',age:1,iob:.125,showIob:true}
};

async function loadDevices(){
  const index = await fetch('./devices/index.json').then(r=>r.json());
  devices = await Promise.all(index.devices.map(file=>fetch(`./devices/${file}`).then(r=>r.json())));
  controls.device.innerHTML = devices.map(d=>`<option value="${d.id}">${d.name}</option>`).join('');
  controls.device.value = 'band10';
  selectDevice();
}

function selectDevice(){
  selected = devices.find(d=>d.id===controls.device.value) || devices[0];
  render();
}

function signed(v,units){
  if(v==='' || v===null || Number.isNaN(Number(v))) return 'Δ —';
  const n = Number(v);
  const decimals = units==='mmol/L' ? 1 : 0;
  return `${n>0?'+':''}${n.toFixed(decimals)}`;
}

function render(){
  if(!selected) return;
  const screen = $('screen');
  const maxW = selected.shape==='capsule' ? 212 : 336;
  const maxH = selected.shape==='capsule' ? 520 : 480;
  const scale = Math.min(300/maxW, 500/maxH);
  screen.style.width = `${selected.width}px`;
  screen.style.height = `${selected.height}px`;
  screen.style.borderRadius = selected.shape==='capsule' ? `${selected.cornerRadius}px` : `${selected.cornerRadius}px`;
  screen.style.transform = `scale(${scale})`;

  const widthFactor = selected.width / 212;
  const heightFactor = selected.height / 520;
  const typeScale = Math.min(Math.max(widthFactor*.82, .9), 1.42);
  const top = Math.max(58, 92*heightFactor);
  $('.unused');
  screen.style.paddingTop = `${top}px`;
  const glucoseSize = 76*typeScale;
  const trendSize = 46*typeScale;
  const metaSize = 31*Math.min(typeScale,1.22);
  const iobSize = 25*Math.min(typeScale,1.22);
  document.querySelector('.glucose-row').style.fontSize=`${glucoseSize}px`;
  $('trendOut').style.fontSize=`${trendSize}px`;
  $('trendOut').style.marginLeft=`${8*Math.min(typeScale,1.3)}px`;
  document.querySelector('.meta-row').style.fontSize=`${metaSize}px`;
  document.querySelector('.meta-row').style.marginTop=`${18*Math.max(.8,heightFactor)}px`;
  document.querySelector('.meta-row .dot').style.margin=`0 ${9*Math.min(typeScale,1.2)}px`;
  $('iobOut').style.fontSize=`${iobSize}px`;
  $('iobOut').style.marginTop=`${28*Math.max(.75,heightFactor)}px`;

  const units = controls.units.value;
  const glucose = Number(controls.glucose.value);
  $('glucoseOut').textContent = units==='mmol/L' ? glucose.toFixed(1) : Math.round(glucose);
  $('trendOut').textContent = controls.trend.value;
  $('deltaOut').textContent = signed(controls.delta.value, units);
  $('ageOut').textContent = `${controls.age.value}m ago`;
  $('ageValue').textContent = `${controls.age.value}m`;
  $('iobOut').textContent = `IOB ${Number(controls.iob.value || 0).toFixed(3)} U`;
  $('iobOut').style.display = controls.showIob.checked ? 'block' : 'none';
  screen.classList.toggle('stale', Number(controls.age.value)>=10);

  const c = selected.capabilities;
  const target = selected.banddripTarget.replaceAll('-',' ');
  $('deviceInfo').innerHTML = `<strong>${selected.width}×${selected.height}</strong> · ${target}<br>${selected.notes}<br>Vela app: <strong>${String(c.velaQuickApp)}</strong> · Lua face research: <strong>${String(c.luaWatchFaceResearch)}</strong>`;
}

function applyFixture(name){
  const f=fixtureMap[name];
  for(const [key,val] of Object.entries(f)){
    if(key==='showIob') controls.showIob.checked=val;
    else controls[key].value=val;
  }
  render();
}

async function loadEvidence(){
  try{
    const result=await fetch('./results/latest.json',{cache:'no-store'}).then(r=>r.json());
    const badge=$('firmwareBadge');
    if(result.status==='passed') { badge.textContent=`Vela emulator: passed · ${result.device}`; badge.className='badge good'; }
    else if(result.status==='failed') { badge.textContent=`Vela emulator: failed · ${result.device}`; badge.className='badge warn'; }
  } catch {}
}

controls.device.addEventListener('change',selectDevice);
Object.values(controls).forEach(el=>el?.addEventListener?.('input',render));
document.querySelectorAll('[data-fixture]').forEach(b=>b.addEventListener('click',()=>applyFixture(b.dataset.fixture)));

await loadDevices();
await loadEvidence();
