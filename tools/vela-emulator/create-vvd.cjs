const path = require('path');
const { VvdManager, defaultSDKHome, defaultVvdHome, IVvdArchType } = require('@aiot-toolkit/emulator');
const name = process.env.BANDDRIP_VVD_NAME || 'BandDrip_ProLab';
const skinName = process.env.BANDDRIP_VELA_SKIN || 'xiaomi_band_pro';
const imageType = process.env.BANDDRIP_VELA_IMAGE || 'vela-miwear-watch-5.0';
(async()=>{
  const manager=new VvdManager({sdkHome:defaultSDKHome,vvdHome:defaultVvdHome});
  const skins=await manager.getVelaSkinList();
  const skin=skins.find(s=>s.name===skinName);
  if(!skin) throw new Error(`Vela skin ${skinName} not found. Available: ${skins.map(s=>s.name).join(', ')}`);
  const imageDir=path.resolve(defaultSDKHome,'system-images',imageType);
  manager.createVvd({name,skin:skin.name,'skin.path':skin.path,arch:IVvdArchType.arm,imageDir,imageType,customLcdRadius:''});
  console.log(`Created ${name} using ${skinName} / ${imageType}`);
})().catch(e=>{console.error(e);process.exit(1)});
