import subprocess,time,pathlib,sys
adb=['adb','-s','127.0.0.1:15555']
subprocess.run(adb+['shell','dumpsys','gfxinfo','com.drbep.tvplayer.offline','reset'],stdout=subprocess.DEVNULL)
for key in [22]*4+[21]*4+[22]*4+[21]*4:
 subprocess.run(adb+['shell','input','keyevent',str(key)],check=True)
 time.sleep(.35)
pathlib.Path(sys.argv[1]).write_bytes(subprocess.check_output(adb+['shell','dumpsys','gfxinfo','com.drbep.tvplayer.offline']))
