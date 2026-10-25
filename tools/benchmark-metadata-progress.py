#!/usr/bin/env python3
"""Execute actual Java scope SQL against synthetic SQLite data, never production."""
import base64, hashlib, json, pathlib, re, sqlite3, statistics, subprocess, tempfile, time
ROOT=pathlib.Path(__file__).resolve().parents[1]
SCOPES=[(s,k,"") for s in ("","s0","s19","missing","x' OR 1=1 --") for k in ("","movie","series")]
SCOPES += [("s0",k,g) for k in ("","movie","series") for g in ("Drama","Missing","Drama' OR 1=1 --")]

def java_scopes():
    # Execute the production Java helper, rather than reimplement its branching in Python.
    with tempfile.TemporaryDirectory(prefix="metadata-scope-") as d:
        d=pathlib.Path(d)
        probe=d/"ScopeProbe.java"
        probe.write_text("""package com.drbep.tvplayer;
import java.util.Base64;
import java.nio.charset.StandardCharsets;
public class ScopeProbe {
 static String b(String s){return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));}
 static String a(String[] a){return java.util.Arrays.stream(a).map(ScopeProbe::b).collect(java.util.stream.Collectors.joining(","));}
 public static void main(String[] args){
  for(int i=0;i<args.length;i+=3){MetadataIndexSelection s=new MetadataIndexSelection(args[i],args[i+1],args[i+2]);
   System.out.println(b(s.where())+";"+a(s.args())+";"+b(s.countWhere())+";"+a(s.countArgs()));}
 }
}""")
        subprocess.run(["javac","-d",str(d),str(ROOT/"app/src/main/java/com/drbep/tvplayer/MetadataIndexSelection.java"),str(probe)],check=True)
        out=subprocess.check_output(["java","-cp",str(d),"com.drbep.tvplayer.ScopeProbe",*[v for row in SCOPES for v in row]],text=True)
        decode=lambda v:base64.b64decode(v).decode()
        return [(decode(w),[decode(v) for v in a.split(",")] if a else [],decode(c),[decode(v) for v in ca.split(",")] if ca else []) for w,a,c,ca in (line.split(";") for line in out.splitlines())]

def fixture(n):
    # Baseline v8 indexes; the series-group benchmark adds v9 to a separate copy.
    db=sqlite3.connect(":memory:")
    schema=(ROOT/"app/src/main/java/com/drbep/tvplayer/playlists/PlaylistDatabase.java").read_text()
    # These CREATE statements and indexes are taken directly from the current schema.
    for name in ("sources","entries","entries_page","entries_group","entries_series","preferences","metadata_index","metadata_index_state"):
        sql=re.search(r'"(CREATE (?:TABLE|INDEX) '+name+r'\b[^"\n]*)"',schema).group(1)
        db.execute(sql)
    db.execute("ALTER TABLE sources ADD COLUMN preview TEXT")
    db.executemany("INSERT INTO sources(id,name,origin,active,preview) VALUES(?,?,'file',?,?)",[("s"+str(i),"List "+str(i),None if i==19 else "g1","g1") for i in range(20)])
    rows=[];entries=[];hidden=[]
    for i in range(n):
        source="s"+str(i%20);kind="movie" if (i//20)%2==0 else "series"
        generation="g0" if i%137==0 else "g1";identity="item"+str(i);series="show"+str(i//80) if kind=="series" else ""
        state=("pending","done","error","unmatched","skipped")[(i//113)%5]
        rows.append((kind,source,generation,identity,i%2,series,state))
        entries.append((source,generation,identity,i%2,i,kind,identity,series,1,1,"Drama" if i%3 else "Other","","https://example.invalid/video","{}",""))
        if i%97==0:hidden.append((source,identity,i%2))
    db.executemany("INSERT INTO metadata_index VALUES(?,?,?,?,?,?,?)",rows)
    db.executemany("INSERT INTO entries VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",entries)
    db.executemany("INSERT INTO preferences(source,identity,occurrence,hidden) VALUES(?,?,?,1)",hidden)
    db.commit();return db

def measure_pair(db,before,oldargs,after,args):
    pairs=((before,oldargs),(after,args));values=[[],[]]
    for sql,params in pairs:
        for _ in range(2):db.execute(sql,params).fetchall()
    for i in range(9):
        for variant in ((0,1) if i%2==0 else (1,0)):
            sql,params=pairs[variant]
            t=time.perf_counter_ns();db.execute(sql,params).fetchall()
            values[variant].append((time.perf_counter_ns()-t)/1e6)
    return [round(statistics.median(v),4) for v in values]

def main():
    actual=java_scopes();report={"sqlite":sqlite3.sqlite_version,"equivalences":0,"scenarios":[],"timings":[]}
    text=(ROOT/"app/src/main/java/com/drbep/tvplayer/MetadataIndexSelection.java").read_text()
    group=re.search(r'GROUP_SQL="([^"]+)"',text).group(1).replace("@","metadata_index")
    queries={"counts":"SELECT state,COUNT(*) FROM metadata_index WHERE {} GROUP BY state", "next":"SELECT rowid,kind,source,generation,identity,occurrence FROM metadata_index WHERE state='pending' AND {} ORDER BY rowid LIMIT 1", "exists":"SELECT 1 FROM metadata_index WHERE state='pending' AND {} LIMIT 1", "retry":"SELECT rowid FROM metadata_index WHERE state='error' AND {} ORDER BY rowid"}
    for n,groups in ((120000,False),(12000,True)):
        db=fixture(n)
        for phase in ("mixed","nearly_complete","complete","retried"):
            if phase=="nearly_complete":db.execute("UPDATE metadata_index SET state='done' WHERE state='pending' AND rowid<?",(n-80,))
            if phase=="complete":db.execute("UPDATE metadata_index SET state='done' WHERE state='pending'")
            if phase=="retried":db.execute("UPDATE metadata_index SET state='pending' WHERE state='error' AND source='s0'")
            digest=hashlib.sha256()
            for (source,kind,g),(where,args,countwhere,countargs) in zip(SCOPES,actual):
                if bool(g)!=groups:continue
                old="(?='' OR source=?) AND (?='' OR kind=?)"+(" AND "+group if g else "")
                oldargs=[source,source,kind,kind]+([g] if g else [])
                for name,template in queries.items():
                    newargs=countargs if name=="counts" else args
                    before=template.format(old);after=template.format(countwhere if name=="counts" else where)
                    a=db.execute(before,oldargs).fetchall();b=db.execute(after,newargs).fetchall()
                    assert a==b,(n,phase,source,kind,g,name,a[:5],b[:5])
                    digest.update(repr((source,kind,g,name,b)).encode());report["equivalences"]+=1
                    # Time meaningful positive scopes, and empty pending queues separately.
                    if source in ("","s0","s19") and g in ("","Drama") and name in ("counts","next") and phase in ("mixed","complete"):
                        bm,am=measure_pair(db,before,oldargs,after,newargs)
                        report["timings"].append(dict(rows=n,phase=phase,source=source,kind=kind,group=g,query=name,before_ms=bm,after_ms=am,plan=[r[3] for r in db.execute("EXPLAIN QUERY PLAN "+after,newargs)]))
            report["scenarios"].append(dict(rows=n,phase=phase,sha256=digest.hexdigest()))
        db.close()
    print(json.dumps(report,indent=2))
if __name__=="__main__":main()
