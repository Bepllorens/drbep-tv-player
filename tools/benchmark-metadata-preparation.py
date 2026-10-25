#!/usr/bin/env python3
"""Compare actual queue Java before/current SQL with synthetic data only."""
import base64,json,pathlib,subprocess,tempfile,statistics,time,hashlib
ROOT=pathlib.Path(__file__).resolve().parents[1]
JAVA=ROOT/"app/src/main/java/com/drbep/tvplayer"
# Reference behavior: optional movie source filter; series source pushdown already active.
SCOPES=[(k,s,g) for k in ("movie","series") for s in ("","a","b","d","absent","x' OR 1=1 --") for g in (("","Drama","Drama's","absent") if s else ("",))]
def queries():
 with tempfile.TemporaryDirectory(prefix="metadata-preparation-") as name:
  d=pathlib.Path(name)
  current=(JAVA/"MetadataIndexQueue.java").read_text()
  # Build a reference helper by keeping the established optional-source method;
  # only the movie branch is under comparison. No regex SQL reconstruction.
  reference=current.replace("class MetadataIndexQueue {","class MetadataIndexQueueBefore {").replace("if (!series) {","if (!series) return sql(series, withGroup);\n        if (false) {",1).replace("if (series || source.isEmpty()) values.add(source);","values.add(source);")
  (d/"MetadataIndexQueueBefore.java").write_text(reference)
  (d/"QueueProbe.java").write_text("""package com.drbep.tvplayer;
import java.util.*;import java.nio.charset.StandardCharsets;
public class QueueProbe {
 static String b(String x){return Base64.getEncoder().encodeToString(x.getBytes(StandardCharsets.UTF_8));}
 static String a(String[] a){return Arrays.stream(a).map(QueueProbe::b).collect(java.util.stream.Collectors.joining(","));}
 public static void main(String[] args){for(int i=0;i<args.length;i+=3){boolean series=args[i].equals("series");String s=args[i+1],g=args[i+2];
 System.out.println(b(MetadataIndexQueueBefore.sql(series,!g.isEmpty(),!s.isEmpty()))+";"+a(MetadataIndexQueueBefore.args(series,args[i],s,g))+";"+b(MetadataIndexQueue.sql(series,!g.isEmpty(),!s.isEmpty()))+";"+a(MetadataIndexQueue.args(series,args[i],s,g)));}}
}""")
  subprocess.run(["javac","-d",str(d),str(JAVA/"MetadataIndexSelection.java"),str(JAVA/"MetadataIndexQueue.java"),str(d/"MetadataIndexQueueBefore.java"),str(d/"QueueProbe.java")],check=True)
  out=subprocess.check_output(["java","-cp",str(d),"com.drbep.tvplayer.QueueProbe",*[v for row in SCOPES for v in row]],text=True)
  b=lambda x:base64.b64decode(x).decode()
  return [(b(o),[b(x) for x in oa.split(",")],b(n),[b(x) for x in na.split(",")]) for o,oa,n,na in (line.split(";") for line in out.splitlines())]
def main():
 # Reuse the established synthetic fixture, without executing its benchmark.
 p=ROOT/"tools/benchmark-metadata-queue.py";ns={"__file__":str(p)}
 exec(p.read_text().split("comparisons=0")[0].replace("24000","120000"),ns)
 db=ns["db"];db.execute("CREATE INDEX metadata_index_state ON metadata_index(state,kind)")
 pairs=queries();equal=0;timings=[];digests=[];order_changes=[]
 def run(sql,args,saved=()):
  db.execute("DELETE FROM metadata_index")
  db.executemany("INSERT INTO metadata_index VALUES(?,?,?,?,?,?,?)",saved)
  db.execute(sql,args)
  rows=db.execute("SELECT * FROM metadata_index ORDER BY kind,source,generation,identity,occurrence").fetchall()
  order=db.execute("SELECT kind,source,generation,identity,occurrence FROM metadata_index ORDER BY rowid").fetchall()
  return rows,order
 for (kind,source,group),(old,oa,new,na) in zip(SCOPES,pairs):
  a,ao=run(old,oa);b,bo=run(new,na);assert a==b,(kind,source,group);equal+=1
  if ao!=bo:order_changes.append([kind,source,group])
  saved=[tuple(list(row[:-1])+[state]) for row,state in zip(a,("done","error","unmatched","skipped"))]
  aa,_=run(old,oa,saved);bb,_=run(new,na,saved);assert aa==bb;equal+=1
  db.execute(new,na);assert bb==db.execute("SELECT * FROM metadata_index ORDER BY kind,source,generation,identity,occurrence").fetchall();equal+=1
  digests.append(dict(kind=kind,source=source,group=group,candidates=len(b),sha256=hashlib.sha256(repr(bb).encode()).hexdigest()))
  if kind=="movie" and source in ("","a","b","absent") and group in ("","Drama"):
   times=[[],[]]
   for i in range(9):
    for v in ((0,1) if i%2==0 else (1,0)):
     sql,args=(old,oa) if v==0 else (new,na);db.execute("DELETE FROM metadata_index");t=time.perf_counter_ns();db.execute(sql,args)
     if i>=2:times[v].append((time.perf_counter_ns()-t)/1e6)
   timings.append(dict(kind=kind,source=source,group=group,before_ms=statistics.median(times[0]),after_ms=statistics.median(times[1]),before_plan=[r[3] for r in db.execute("EXPLAIN QUERY PLAN "+old,oa)],after_plan=[r[3] for r in db.execute("EXPLAIN QUERY PLAN "+new,na)]))
 print(json.dumps(dict(rows=120000,equivalences=equal,order_changes=order_changes,timings=timings,scopes=digests),indent=2))
if __name__=="__main__":main()
