#!/usr/bin/env python3
"""Compare actual Java mixed-group scopes against the legacy EXISTS on synthetic DBs."""
import pathlib,importlib.util,subprocess,tempfile,base64,json,time,statistics,re,hashlib,sys
ROOT=pathlib.Path(__file__).resolve().parents[1];JAVA=ROOT/'app/src/main/java/com/drbep/tvplayer'
spec=importlib.util.spec_from_file_location('progress',ROOT/'tools/benchmark-metadata-progress.py');progress=importlib.util.module_from_spec(spec);spec.loader.exec_module(progress)
SCOPES=[(s,'',g) for s in ('s0','s19','absent',"x' OR 1=1 --") for g in ('Drama','Other','Missing',"Drama' OR 1=1 --")]
SCOPES += [('s0',k,g) for k in ('movie','series') for g in ('Drama','Missing')]+[(s,k,'') for s in ('','s0') for k in ('','movie','series')]
def scopes():
 with tempfile.TemporaryDirectory(prefix='mixed-metadata-') as name:
  d=pathlib.Path(name);current=(JAVA/'MetadataIndexSelection.java').read_text()
  reference=current.replace('MetadataIndexSelection','MetadataIndexSelectionBefore').replace('if(kind.isEmpty())values.add(group);','').replace('sql=GROUP_MIXED_SQL;','sql=GROUP_SQL;')
  (d/'MetadataIndexSelectionBefore.java').write_text(reference)
  (d/'MixedScopeProbe.java').write_text("""package com.drbep.tvplayer;
import java.util.*;import java.nio.charset.StandardCharsets;
public class MixedScopeProbe {
 static String b(String x){return Base64.getEncoder().encodeToString(x.getBytes(StandardCharsets.UTF_8));}
 static String a(String[] a){return Arrays.stream(a).map(MixedScopeProbe::b).collect(java.util.stream.Collectors.joining(","));}
 public static void main(String[] args){for(int i=0;i<args.length;i+=3){MetadataIndexSelectionBefore o=new MetadataIndexSelectionBefore(args[i],args[i+1],args[i+2]);MetadataIndexSelection n=new MetadataIndexSelection(args[i],args[i+1],args[i+2]);System.out.println(b(o.where())+";"+a(o.args())+";"+b(o.countWhere())+";"+a(o.countArgs())+";"+b(n.where())+";"+a(n.args())+";"+b(n.countWhere())+";"+a(n.countArgs()));}}
}""")
  subprocess.run(['javac','-d',str(d),str(JAVA/'MetadataIndexSelection.java'),str(d/'MetadataIndexSelectionBefore.java'),str(d/'MixedScopeProbe.java')],check=True)
  out=subprocess.check_output(['java','-cp',str(d),'com.drbep.tvplayer.MixedScopeProbe',*[v for row in SCOPES for v in row]],text=True)
  decode=lambda x:base64.b64decode(x).decode();result=[]
  for line in out.splitlines():
   fields=line.split(';');result.append(tuple(decode(v) if i%2==0 else [decode(a) for a in v.split(',')] if v else [] for i,v in enumerate(fields)))
  return result
review=(JAVA/'MetadataIndexReview.java').read_text();PREFIX=re.search(r'PREFIX="([^"]+)"',review).group(1);SUFFIX=re.search(r'SUFFIX="([^"]+)"',review).group(1)
def queries(scope):
 where,args,cwhere,cargs=scope
 return [('counts','SELECT state,COUNT(*) FROM metadata_index WHERE '+cwhere+' GROUP BY state',cargs),('next',"SELECT rowid,kind,source,generation,identity,occurrence FROM metadata_index WHERE state='pending' AND "+where+' ORDER BY rowid LIMIT 1',args),('retry',"SELECT rowid FROM metadata_index WHERE state='error' AND "+where+' ORDER BY rowid',args),('review',PREFIX+where+SUFFIX,args+['unmatched',0])]
def pair(db,old,oa,new,na):
 for q,a in ((old,oa),(new,na)):
  for _ in range(2):db.execute(q,a).fetchall()
 vals=[[],[]]
 for i in range(9):
  for v in ((0,1) if i%2==0 else (1,0)):
   q,a=(old,oa) if v==0 else (new,na);t=time.perf_counter_ns();db.execute(q,a).fetchall();vals[v].append((time.perf_counter_ns()-t)/1e6)
 return [round(statistics.median(v),4) for v in vals]
def state_digest(db):
 h=hashlib.sha256()
 for row in db.execute('SELECT rowid,state FROM metadata_index ORDER BY rowid'):h.update(repr(row).encode())
 return h.hexdigest()
def main():
 compiled=scopes();report=dict(sqlite=progress.sqlite3.sqlite_version,equivalences=0,unchanged=0,retry_equivalences=0,timings=[])
 for n in (12000,120000):
  for version in (8,9):
   print('fixture',n,'indexes',version,file=sys.stderr,flush=True);db=progress.fixture(n)
   if version==9:db.execute('CREATE INDEX entries_series_group ON entries(source,generation,kind,series,group_name)')
   for phase in ('visible','hidden','old_generation','complete'):
    if n==120000 and phase in ('hidden','old_generation'):continue
    if phase=='hidden':db.execute("UPDATE preferences SET hidden=1 WHERE source='s0'")
    if phase=='old_generation':db.execute("UPDATE sources SET active='g0' WHERE id='s0'")
    if phase=='complete':db.execute("UPDATE metadata_index SET state='done' WHERE state='pending'")
    for selection,compiled_scope in zip(SCOPES,compiled):
     source,kind,group=selection;old=compiled_scope[:4];new=compiled_scope[4:]
     if n==120000 and not(source=='s0' and kind=='' and group in ('Drama','Missing')):continue
     if not group or kind:
      assert old==new;report['unchanged']+=1
     for (name,oq,oa),(_,nq,na) in zip(queries(old),queries(new)):
      a=db.execute(oq,oa).fetchall();b=db.execute(nq,na).fetchall();assert a==b,(n,version,phase,selection,name);report['equivalences']+=1
      if name=='review' and len(a)>50:
       cursor=a[49][-1];aa=oa[:-1]+[cursor];bb=na[:-1]+[cursor];assert db.execute(oq,aa).fetchall()==db.execute(nq,bb).fetchall();report['equivalences']+=1
      if phase=='visible' and source=='s0' and not kind and group in ('Drama','Missing') and name in ('counts','next'):
       bm,am=pair(db,oq,oa,nq,na);report['timings'].append(dict(rows=n,schema=version,group=group,query=name,before_ms=bm,after_ms=am,after_plan=[r[3] for r in db.execute('EXPLAIN QUERY PLAN '+nq,na)]))
     if n==12000 and phase=='visible':
      states=[]
      for where,args,*_ in (old,new):
       db.execute('SAVEPOINT retry_scope');db.execute("UPDATE metadata_index SET state='pending' WHERE state='error' AND "+where,args);states.append(state_digest(db));db.execute('ROLLBACK TO retry_scope');db.execute('RELEASE retry_scope')
      assert states[0]==states[1];report['retry_equivalences']+=1
   db.close()
 print(json.dumps(report,indent=2))
if __name__=='__main__':main()
