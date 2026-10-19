import re,sys,statistics,json
from collections import defaultdict
stack=defaultdict(list); values=defaultdict(list)
for line in open(sys.argv[1],errors='replace'):
 m=re.search(r'-(\d+)\s+.*?\s(\d+\.\d+): tracing_mark_write: ([BE])(?:\|\d+\|([^\n]+))?',line)
 if not m:continue
 tid,ts,kind,name=m.groups();ts=float(ts)
 if kind=='B': stack[tid].append((ts,name))
 elif stack[tid]:
  start,label=stack[tid].pop()
  if label and (label.startswith('Archive.') or label in ('AndroidOwner:measureAndLayout','Recomposer:recompose')):values[label].append((ts-start)*1000)
for label,ds in sorted(values.items()):
 ds.sort();print(label,json.dumps(dict(n=len(ds),median_ms=round(statistics.median(ds),3),p95_ms=round(ds[min(len(ds)-1,int(len(ds)*.95))],3),max_ms=round(max(ds),3),sum_ms=round(sum(ds),3))))
