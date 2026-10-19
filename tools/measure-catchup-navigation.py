#!/usr/bin/env python3
"""Run from Catchup with focus on Today. Never opens or modifies a programme."""
import argparse, subprocess, time, json, pathlib
p=argparse.ArgumentParser();p.add_argument('--serial',required=True);p.add_argument('--output',required=True);a=p.parse_args()
def adb(*args):return subprocess.check_output(['adb','-s',a.serial,*args],text=True)
out=pathlib.Path(a.output);out.mkdir(parents=True,exist_ok=True)
adb('shell','dumpsys','gfxinfo','com.drbep.tvplayer.offline','reset')
steps=[]
for direction in [22,22,22,21,21,21]:
 t=time.monotonic()
 adb('shell','input','keyevent',str(direction),'23')
 time.sleep(2)
 steps.append({'direction':direction,'host_elapsed_ms':round((time.monotonic()-t)*1000)})
(out/'gfx.txt').write_text(adb('shell','dumpsys','gfxinfo','com.drbep.tvplayer.offline'))
(out/'steps.json').write_text(json.dumps(steps,indent=2))
print('Saved navigation measurements; host_elapsed_ms includes ADB and fixed waits, not UI latency.')
