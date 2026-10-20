#!/usr/bin/env python3
"""Offline playback evidence, numeric output only. Never contacts a device."""
import argparse,bisect,collections,json,zipfile
from pathlib import Path
MAX_BYTES=8*1024*1024
MAX_LINE=65536
KINDS={'PLAYBACK_SAMPLE','AUDIO_COUNTERS','VIDEO_COUNTERS'}
def read_lines(paths):
 total=0
 def bounded(stream):
  nonlocal total
  while True:
   line=stream.readline(MAX_LINE+1)
   if not line:return
   total+=len(line)
   if len(line)>MAX_LINE or total>MAX_BYTES:raise ValueError('Input limit exceeded')
   yield line
 for path in paths:
  if path.suffix.lower()=='.zip':
   with zipfile.ZipFile(path) as z:
    members=[m for m in z.infolist() if Path(m.filename).name.startswith('events-') and m.filename.endswith('.jsonl')]
    if len(members)>64 or sum(m.file_size for m in members)>MAX_BYTES:raise ValueError('Archive limit exceeded')
    for member in members:
     with z.open(member) as stream:yield from bounded(stream)
  else:
   with path.open('rb') as stream:yield from bounded(stream)
def summarize(lines):
 groups={};seen=set();multi=set();rejected=duplicates=0
 for line in lines:
  try:
   e=json.loads(line)
   if not isinstance(e,dict):raise ValueError()
   kind=e.get('event')
   if kind in ('MULTI_OPEN','MULTI_STATE','MULTI_COUNTERS'):
    if isinstance(e.get('session'),str) and type(e.get('version')) is int:multi.add((e['session'],e['version']))
    continue
   if kind not in KINDS:continue
   session=e['session'];nums=[e[k] for k in ('version','op','at','a','b','c')]
   if not isinstance(session,str) or not 1<=len(session)<=128 or any(type(v)is not int for v in nums):raise ValueError()
   version,op,at,a,b,c=nums
   if min(nums)<0 or op==0 or max(nums)>2**63-1:raise ValueError()
   if kind=='PLAYBACK_SAMPLE' and (c>127 or (c&7) not in (1,2,3,4)):raise ValueError()
   clock='monotonic' if 'elapsed' in e else 'wall'
   if clock=='monotonic':
    if type(e['elapsed']) is not int or not 0<=e['elapsed']<=2**63-1:raise ValueError()
    at=e['elapsed']
   key=(session,version,op,clock)
   if key not in groups and len(groups)>=512:raise ValueError('Group limit')
   identity=(key,kind,at,a,b,c)
   if identity in seen:duplicates+=1;continue
   if len(seen)>=50000:raise ValueError('Event limit')
   seen.add(identity);groups.setdefault(key,[]).append((at,kind,a,b,c))
  except (ValueError,KeyError,TypeError,UnicodeDecodeError):rejected+=1
 output=[]
 for (session,version,_,clock),events in groups.items():
  events.sort()
  samples=[e for e in events if e[1]=='PLAYBACK_SAMPLE']
  counter={}
  for kind in ('AUDIO_COUNTERS','VIDEO_COUNTERS'):
   values=[e for e in events if e[1]==kind];counter[kind]=([e[0] for e in values],values)
  def near(kind,at):
   times,values=counter[kind];i=bisect.bisect_left(times,at)
   candidates=values[max(0,i-1):i+1]
   if not candidates:return None
   closest=min(candidates,key=lambda e:abs(e[0]-at))
   return closest if abs(closest[0]-at)<=1000 else None
  def delta(kind,old,new):
   a,b=near(kind,old),near(kind,new)
   if a is None or b is None or a[0]>=b[0] or b[2]<a[2] or b[3]<a[3]:return None
   return {'rendered':b[2]-a[2],'skipped_or_dropped':b[3]-a[3]}
  counts=collections.Counter();details=[];omitted=0
  if (session,version) in multi:
   output.append({'group':len(output)+1,'version':version,'clock':clock,'samples':len(samples),
                  'unclassified':'multiview present; attempt identifiers may overlap'})
   continue
  for prev,cur in zip(samples,samples[1:]):
   dt=cur[0]-prev[0];dp=cur[2]-prev[2];f0,f1=prev[4],cur[4]
   if not 1000<=dt<=90000:kind='gap_or_ambiguous_time'
   elif not(f0&8) or not(f1&8):kind='pause_requested'
   elif (f0|f1)&64:kind='suppressed'
   elif dp<0:kind='position_rewind'
   elif dp>dt*4+5000:kind='position_jump'
   elif dp>0:kind='position_advancing'
   elif (f0&7)!=3 or (f1&7)!=3:kind='stationary_not_ready'
   elif min(prev[3],cur[3])>=3000:kind='stationary_with_buffer'
   elif max(prev[3],cur[3])<=500:kind='stationary_low_buffer'
   else:kind='stationary_mixed_buffer'
   counts[kind]+=1
   if kind.startswith('stationary'):
    detail={'from_ms':prev[0]-samples[0][0],'span_ms':dt,'observation':kind,
            'buffer_before_ms':prev[3],'buffer_after_ms':cur[3],
            'audio_delta':delta('AUDIO_COUNTERS',prev[0],cur[0]),
            'video_delta':delta('VIDEO_COUNTERS',prev[0],cur[0])}
    if len(details)<100:details.append(detail)
    else:omitted+=1
  output.append({'group':len(output)+1,'version':version,'clock':clock,'samples':len(samples),
                 'intervals':dict(counts),'stationary_intervals':details,'omitted_details':omitted})
 return {'groups':output,'rejected':rejected,'duplicates':duplicates,
         'limits':'Sampled observations, not a diagnosis. Pauses/seeks between samples may be invisible. Counter reset or missing data yields null. No claim of audible audio or visible video.'}
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('files',nargs='+',type=Path);a=p.parse_args()
 print(json.dumps(summarize(read_lines(a.files)),indent=2))
if __name__=='__main__':main()
