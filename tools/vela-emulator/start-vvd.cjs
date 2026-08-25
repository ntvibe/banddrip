const { VvdManager, defaultSDKHome, defaultVvdHome } = require('@aiot-toolkit/emulator');
const name=process.env.BANDDRIP_VVD_NAME || 'BandDrip_ProLab';
(async()=>{
  const manager=new VvdManager({sdkHome:defaultSDKHome,vvdHome:defaultVvdHome});
  const result=await manager.startVvd({vvdName:name,verbose:true,stdoutCallback:m=>process.stdout.write(`[vela] ${m}`),stderrCallback:m=>process.stderr.write(`[vela] ${m}`)});
  console.log(`\nVVD start requested; coldBoot=${result.coldBoot}`);
})().catch(e=>{console.error(e);process.exit(1)});
