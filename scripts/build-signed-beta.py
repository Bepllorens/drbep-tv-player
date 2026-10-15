#!/usr/bin/env python3
import os, subprocess, argparse
from pathlib import Path
parser=argparse.ArgumentParser(description='Build a signed beta without publishing')
parser.add_argument('--universal', action='store_true', help='Include ARM32 and ARM64')
args=parser.parse_args()
p=Path('/srv/drbep-dev/secrets/release.properties')
values={}
for line in p.read_text().splitlines():
 if '=' in line and not line.lstrip().startswith('#'):
  k,v=line.split('=',1); values[k.strip()]=v.strip()
required=['DRBEP_RELEASE_STORE_FILE','DRBEP_RELEASE_STORE_PASSWORD','DRBEP_RELEASE_KEY_ALIAS','DRBEP_RELEASE_KEY_PASSWORD']
if not all(values.get(k) for k in required): raise SystemExit('Faltan propiedades de firma')
values['DRBEP_RELEASE_STORE_FILE']='/srv/drbep-dev/secrets/release.jks'
env=os.environ.copy();env.update(values)
subprocess.run(['bash','./gradlew','--no-daemon','--no-configuration-cache','--max-workers=1',':app:assembleRelease','-PupdateChannel=beta','-PfireAbiOnly=' + ('false' if args.universal else 'true'),'-PprofileBeta=true'],cwd='/srv/drbep-dev/repos/drbep-tv-player',env=env,check=True)
