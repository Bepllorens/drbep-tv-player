import subprocess,time,pathlib,sys,re,json
out=pathlib.Path(sys.argv[1]);out.mkdir(parents=True,exist_ok=True)
def adb(*a):return subprocess.check_output(['adb','-s','127.0.0.1:15555',*a],text=True)
adb('shell','atrace','--async_start','-b','16384','-a','com.drbep.tvplayer.offline','gfx','view')
try:
 for direction in [22,22,22,21,21,21]*2:
  adb('shell','input','keyevent',str(direction),'23');time.sleep(3)
finally:
 trace=adb('shell','atrace','--async_stop');(out/'trace.txt').write_text(trace)
requests={}
for line in trace.splitlines():
 m=re.search(r'(\d+\.\d+): tracing_mark_write: B\|\d+\|Archive.page.(\d+).(\d+).(\w+)',line)
 if m:
  t,q,r,stage=m.groups();requests.setdefault(q,{}).setdefault(stage,[]).append((r,float(t)*1000))
rows=[]
for q,stages in requests.items():
 if 'input' not in stages or 'drawn' not in stages or 'published' not in stages:continue
 end=stages['drawn'][0][1];pubs=[p for p in stages['published'] if p[1]<=end];rev,pub=pubs[-1]
 times={'input':stages['input'][0][1],'drawn':end,'published':pub}
 for name in ['requested','scheduled','queryStart','pageReady','stripReady','genresReady','dataReady','modelsReady']:
  candidates=[t for r,t in stages.get(name,[]) if (r==rev or name=='requested') and t<=pub]
  if candidates:times[name]=candidates[-1]
 row={'request':q,'total_ms':round(end-times['input'],2)}
 order=['input','requested','scheduled','queryStart','pageReady','stripReady','genresReady','dataReady','modelsReady','published','drawn']
 for a,b in zip(order,order[1:]):
  if a in times and b in times:row[a+'_'+b]=round(times[b]-times[a],2)
 for line in trace.splitlines():
  m=re.search(r'(\d+\.\d+): tracing_mark_write: B\|\d+\|Archive.cost.(\w+).(\d+)',line)
  if m and 'queryStart' in times and 'pageReady' in times:
   timestamp,stage,millis=m.groups()
   if times['queryStart']<=float(timestamp)*1000<=times['pageReady']:
    row['db_'+stage+'_ms']=row.get('db_'+stage+'_ms',0)+int(millis)
 rows.append(row)
(out/'latency.json').write_text(json.dumps(rows,indent=2));print(json.dumps(rows,indent=2))
