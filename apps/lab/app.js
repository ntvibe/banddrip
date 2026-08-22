const $ = id => document.getElementById(id)
const controls = ['device','glucose','units','trend','delta','age','iob','showIob'].reduce((a,k)=>(a[k]=$(k),a),{})
let devices = []
let selected = null
let realEvidence = null

async function loadDevices(){
  const index = await fetch('./devices/index.json').then(r=>r.json())
  devices = await Promise.all(index.devices.map(file=>fetch(`./devices/${file}`).then(r=>r.json())))
  controls.device.innerHTML = devices.map(d=>`<option value="${d.id}">${d.name}</option>`).join('')
  controls.device.value = 'band10'
  selectDevice()
}

function selectDevice(){
  selected = devices.find(d=>d.id===controls.device.value) || devices[0]
  renderApproximation()
}

function signed(v,units){
  if(v==='' || v===null || Number.isNaN(Number(v))) return 'Δ —'
  const n = Number(v)
  const decimals = units==='mmol/L' ? 1 : 0
  return `${n>0?'+':''}${n.toFixed(decimals)}`
}

function renderApproximation(){
  if(!selected) return
  const screen = $('screen')
  const maxW = selected.shape==='capsule' ? 212 : 336
  const maxH = selected.shape==='capsule' ? 520 : 480
  const scale = Math.min(300/maxW, 500/maxH)
  screen.style.width = `${selected.width}px`
  screen.style.height = `${selected.height}px`
  screen.style.borderRadius = `${selected.cornerRadius}px`
  screen.style.transform = `scale(${scale})`

  const widthFactor = selected.width / 212
  const heightFactor = selected.height / 520
  const typeScale = Math.min(Math.max(widthFactor*.82, .9), 1.42)
  screen.style.paddingTop = `${Math.max(58, 92*heightFactor)}px`
  document.querySelector('.glucose-row').style.fontSize=`${76*typeScale}px`
  $('trendOut').style.fontSize=`${46*typeScale}px`
  $('trendOut').style.marginLeft=`${8*Math.min(typeScale,1.3)}px`
  document.querySelector('.meta-row').style.fontSize=`${31*Math.min(typeScale,1.22)}px`
  document.querySelector('.meta-row').style.marginTop=`${18*Math.max(.8,heightFactor)}px`
  document.querySelector('.meta-row .dot').style.margin=`0 ${9*Math.min(typeScale,1.2)}px`
  $('iobOut').style.fontSize=`${25*Math.min(typeScale,1.22)}px`
  $('iobOut').style.marginTop=`${28*Math.max(.75,heightFactor)}px`

  const units = controls.units.value
  const glucose = Number(controls.glucose.value)
  $('glucoseOut').textContent = units==='mmol/L' ? glucose.toFixed(1) : Math.round(glucose)
  $('trendOut').textContent = controls.trend.value
  $('deltaOut').textContent = signed(controls.delta.value, units)
  $('ageOut').textContent = `${controls.age.value}m ago`
  $('ageValue').textContent = `${controls.age.value}m`
  $('iobOut').textContent = `IOB ${Number(controls.iob.value || 0).toFixed(3)} U`
  $('iobOut').style.display = controls.showIob.checked ? 'block' : 'none'
  screen.classList.toggle('stale', Number(controls.age.value)>=10)

  const c = selected.capabilities
  $('deviceInfo').innerHTML = `<strong>${selected.width}×${selected.height}</strong> · browser approximation · ${selected.banddripTarget.replaceAll('-',' ')}<br>${selected.notes}<br>Vela app: <strong>${String(c.velaQuickApp)}</strong> · Lua face research: <strong>${String(c.luaWatchFaceResearch)}</strong>`
}

function showRealFixture(fixture){
  if(!realEvidence || !fixture) return
  document.querySelectorAll('[data-real-fixture]').forEach(button=>button.classList.toggle('active',button.dataset.realFixture===fixture.id))
  const img = $('realFrame')
  img.src = `./${fixture.image}?commit=${encodeURIComponent(realEvidence.commit || '')}`
  img.style.display = 'block'
  $('realMissing').style.display = 'none'
  $('realCaption').innerHTML = `<strong>${fixture.label}</strong> · ${fixture.summary}<br>Captured from <strong>${realEvidence.evidence}</strong> · ${realEvidence.device} · ${realEvidence.width}×${realEvidence.height} · commit ${String(realEvidence.commit || '').slice(0,7)}`
}

async function loadRealEvidence(){
  const badge = $('firmwareBadge')
  try{
    const result = await fetch('./results/real-firmware.json',{cache:'no-store'}).then(r=>{
      if(!r.ok) throw new Error(`HTTP ${r.status}`)
      return r.json()
    })
    realEvidence = result
    badge.textContent = `Real Vela firmware: ${result.status} · ${result.device}`
    badge.className = result.status==='passed' ? 'badge good' : 'badge warn'
    $('realMeta').innerHTML = `<strong>${result.device}</strong> · ${result.width}×${result.height}<br>${result.image}<br>Generated ${new Date(result.generatedAt).toLocaleString()}`
    const holder = $('realFixtures')
    holder.innerHTML = ''
    for(const fixture of result.fixtures || []){
      const button = document.createElement('button')
      button.type = 'button'
      button.dataset.realFixture = fixture.id
      button.textContent = fixture.label
      button.addEventListener('click',()=>showRealFixture(fixture))
      holder.appendChild(button)
    }
    if(result.fixtures?.length) showRealFixture(result.fixtures[0])
  }catch(error){
    badge.textContent = 'Real Vela firmware: no published snapshot yet'
    badge.className = 'badge warn'
    $('realMeta').textContent = 'The real-firmware Action has not published a usable snapshot bundle yet.'
    $('realMissing').style.display = 'block'
    $('realFrame').style.display = 'none'
  }
}

controls.device.addEventListener('change',selectDevice)
Object.values(controls).forEach(el=>el?.addEventListener?.('input',renderApproximation))

await Promise.all([loadDevices(),loadRealEvidence()])
