package com.drbep.maintenance;
import android.app.*;import android.content.*;import android.os.*;import android.database.*;import android.database.sqlite.*;
import org.json.*;import java.io.*;import java.security.*;import java.util.*;

/** Target-private, same-signature, explicitly invoked maintenance. Never exports row values. */
public class IndexMaintenance extends Instrumentation {
 static final String INDEX="entries_series_group";
 static final String CREATE="CREATE INDEX IF NOT EXISTS entries_series_group ON entries(source,generation,kind,series,group_name)";
 Bundle args;Context ctx;File live,backup,export;
 @Override public void onCreate(Bundle bundle){super.onCreate(bundle);args=bundle;start();}
 @Override public void onStart(){Bundle out=new Bundle();try{
  ctx=getTargetContext();require(ctx.getPackageName().equals("com.drbep.tvplayer.offline"),"Wrong target");
  require(!ctx.getSharedPreferences("metadata-index",0).getBoolean("enabled",false),"Active metadata job");
  live=ctx.getDatabasePath("personal-playlists.db");require(live.isFile(),"Missing existing database");
  String snapshot=args.getString("snapshot","20261024");require(snapshot.matches("[0-9]{8}(-[a-z0-9]+)?"),"Invalid snapshot name");
  backup=new File(ctx.getNoBackupFilesDir(),"personal-playlists-index-base-"+snapshot+".db");
  File folder=new File(ctx.getExternalFilesDir(null),"index-adoption-"+snapshot);
  export=new File(folder,"private-backup.db");
  String action=args.getString("action","");JSONObject result;
  if(action.equals("backup"))result=backup();
  else if(action.equals("clonecheck"))result=clonecheck();
  else if(action.equals("releasecheck"))result=releasecheck();
  else if(action.equals("return8"))result=return8();
  else if(action.equals("export"))result=exportBackup();
  else if(action.equals("bench"))result=IndexPlanProbe.run(ctx,backup);
  else if(action.equals("apply"))result=modify(true);
  else if(action.equals("rollback"))result=modify(false);
  else if(action.equals("inspect"))result=inspect();
  else throw new IllegalArgumentException("Unknown action");
  out.putString("stream",result.toString()+"\n");finish(Activity.RESULT_OK,out);
 }catch(Throwable e){out.putString("stream","{\"ok\":false,\"reason\":\""+e.getClass().getSimpleName()+":"+String.valueOf(e.getMessage()).replace("\"", "'").replace("\n"," ")+"\"}\n");finish(Activity.RESULT_CANCELED,out);}}
 static void require(boolean test,String why){if(!test)throw new IllegalStateException(why);}
 SQLiteDatabase open(File file,int flags){return SQLiteDatabase.openDatabase(file.getAbsolutePath(),null,flags);}
 static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){require(c.moveToFirst(),"Empty scalar");return c.getLong(0);}}
 static boolean index(SQLiteDatabase db){return scalar(db,"SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='entries_series_group'")>0;}
 static void verifyIndex(SQLiteDatabase db){List<String> cols=new ArrayList<>();try(Cursor c=db.rawQuery("PRAGMA index_info(entries_series_group)",null)){while(c.moveToNext())cols.add(c.getString(2));}require(cols.equals(Arrays.asList("source","generation","kind","series","group_name")),"Unexpected index definition");}
 static void integrity(SQLiteDatabase db){try(Cursor c=db.rawQuery("PRAGMA quick_check",null)){require(c.moveToFirst()&&c.getString(0).equals("ok")&&!c.moveToNext(),"Integrity check failed");}}
 static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.ROOT,"%02x",x&255));return s.toString();}
 static String fileHash(File file)throws Exception{MessageDigest h=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)h.update(b,0,n);}return hex(h.digest());}
 static String quote(String name){return "\""+name.replace("\"","\"\"")+"\"";}
 static String digest(SQLiteDatabase db)throws Exception {
  MessageDigest hash=MessageDigest.getInstance("SHA-256");
  try(DataOutputStream out=new DataOutputStream(new DigestOutputStream(new OutputStream(){public void write(int b){}public void write(byte[] b,int o,int n){}},hash))){
   List<String> tables=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name",null)){while(c.moveToNext())tables.add(c.getString(0));}
   for(String table:tables){out.writeUTF(table);try(Cursor c=db.rawQuery("SELECT rowid,* FROM "+quote(table)+" ORDER BY rowid",null)){
    out.writeInt(c.getColumnCount());for(String name:c.getColumnNames())out.writeUTF(name);
    while(c.moveToNext()){out.writeByte(1);for(int i=0;i<c.getColumnCount();i++){int type=c.getType(i);out.writeByte(type);if(type==Cursor.FIELD_TYPE_INTEGER)out.writeLong(c.getLong(i));else if(type==Cursor.FIELD_TYPE_FLOAT)out.writeDouble(c.getDouble(i));else if(type==Cursor.FIELD_TYPE_STRING||type==Cursor.FIELD_TYPE_BLOB){byte[] b=type==Cursor.FIELD_TYPE_BLOB?c.getBlob(i):c.getString(i).getBytes(java.nio.charset.StandardCharsets.UTF_8);out.writeInt(b.length);out.write(b);}}}out.writeByte(0);
   }}
  }return hex(hash.digest());
 }
 static String schema(SQLiteDatabase db)throws Exception {MessageDigest h=MessageDigest.getInstance("SHA-256");try(Cursor c=db.rawQuery("SELECT type,name,tbl_name,COALESCE(sql,'') FROM sqlite_master WHERE name<>'entries_series_group' ORDER BY type,name",null)){while(c.moveToNext())for(int i=0;i<4;i++)h.update((c.getString(i)+"\n").getBytes("UTF-8"));}return hex(h.digest());}
 static void copy(File from,File to)throws Exception {File tmp=new File(to.getPath()+".tmp");require(to.getParentFile().isDirectory()||to.getParentFile().mkdirs(),"Backup folder unavailable");try(InputStream in=new FileInputStream(from);FileOutputStream out=new FileOutputStream(tmp)){byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);out.getFD().sync();}require(tmp.renameTo(to),"Backup rename failed");}
 JSONObject inspect()throws Exception {try(SQLiteDatabase db=open(live,SQLiteDatabase.OPEN_READONLY)){integrity(db);return new JSONObject().put("ok",true).put("version",db.getVersion()).put("index",index(db)).put("entries",scalar(db,"SELECT COUNT(*) FROM entries")).put("series",scalar(db,"SELECT COUNT(*) FROM entries WHERE kind='series'")).put("metadata",scalar(db,"SELECT COUNT(*) FROM metadata_index"));}}
 JSONObject backup()throws Exception {
  require(!backup.exists(),"Backup already exists; do not overwrite");
  require(new StatFs(live.getParent()).getAvailableBytes()>live.length()*3+150L*1024*1024,"Insufficient backup space");
  try(SQLiteDatabase db=open(live,0)){
   require(db.getVersion()==8,"Expected live schema 8");if(index(db))verifyIndex(db);
   require(scalar(db,"SELECT COUNT(*) FROM import_jobs WHERE state IN ('queued','running','retry')")==0,"Active import job");
   try(Cursor c=db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null)){require(c.moveToFirst()&&c.getInt(0)==0,"WAL checkpoint busy");}
   db.beginTransaction();try{
    require(!new File(live.getPath()+"-wal").exists()||new File(live.getPath()+"-wal").length()==0,"WAL not empty");
    String data=digest(db),ddl=schema(db);copy(live,backup);String sha=fileHash(backup);require(sha.equals(fileHash(live)),"Backup bytes differ");
    try(SQLiteDatabase test=open(backup,SQLiteDatabase.OPEN_READONLY)){integrity(test);require(data.equals(digest(test))&&ddl.equals(schema(test))&&test.getVersion()==8,"Backup content differs");}
    copy(backup,export);require(sha.equals(fileHash(export)),"Export differs");
    return new JSONObject().put("ok",true).put("version",8).put("data_sha256",data).put("schema_sha256",ddl).put("backup_sha256",sha).put("backup_bytes",backup.length()).put("export_path",export.getPath());
   }finally{db.endTransaction();}
  }
 }
 JSONObject exportBackup()throws Exception {
  require(backup.isFile(),"Backup required");
  java.net.ServerSocket server=new java.net.ServerSocket(0,1,java.net.InetAddress.getByName("127.0.0.1"));server.setSoTimeout(45000);
  byte[] random=new byte[32];new SecureRandom().nextBytes(random);String token=android.util.Base64.encodeToString(random,android.util.Base64.NO_WRAP);
  Bundle info=new Bundle();info.putString("stream","EXPORT "+new JSONObject().put("port",server.getLocalPort()).put("token",token).toString()+"\n");sendStatus(1,info);
  try(java.net.Socket socket=server.accept()){
   socket.setSoTimeout(10000);BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream(),"US-ASCII"));String first=in.readLine();require("GET /backup HTTP/1.1".equals(first),"Invalid backup request");String supplied="";int total=0;
   for(String line;(line=in.readLine())!=null&&!line.isEmpty();){total+=line.length();require(total<8192,"Headers too large");if(line.toLowerCase(Locale.ROOT).startsWith("x-backup-token:"))supplied=line.substring(line.indexOf(':')+1).trim();}
   require(MessageDigest.isEqual(token.getBytes("US-ASCII"),supplied.getBytes("US-ASCII")),"Backup authorization failed");
   OutputStream out=socket.getOutputStream();out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/octet-stream\r\nContent-Length: "+backup.length()+"\r\nConnection: close\r\n\r\n").getBytes("US-ASCII"));
   try(InputStream file=new FileInputStream(backup)){byte[] b=new byte[65536];int n;while((n=file.read(b))!=-1)out.write(b,0,n);}out.flush();
   if(export.exists())require(export.delete(),"Cannot remove private external temporary");
   return new JSONObject().put("ok",true).put("bytes",backup.length()).put("sha256",fileHash(backup));
  }finally{server.close();}
 }
 JSONObject clonecheck()throws Exception {
  require(backup.isFile(),"Backup required");File clone=new File(ctx.getNoBackupFilesDir(),"index-return-test.db");require(!clone.exists(),"Clone already exists");copy(backup,clone);
  try {String before,ddl;long createMs,dropMs;try(SQLiteDatabase db=open(clone,0)){before=digest(db);ddl=schema(db);require(db.getVersion()==8&&!index(db),"Unexpected clone base");long t=System.nanoTime();db.execSQL(CREATE);createMs=(System.nanoTime()-t)/1000000;verifyIndex(db);require(before.equals(digest(db))&&ddl.equals(schema(db))&&db.getVersion()==8,"Apply changed clone data");db.execSQL(CREATE);require(before.equals(digest(db)),"Repeated create changed data");t=System.nanoTime();db.execSQL("DROP INDEX entries_series_group");dropMs=(System.nanoTime()-t)/1000000;require(!index(db)&&before.equals(digest(db))&&ddl.equals(schema(db))&&db.getVersion()==8,"Return changed clone");integrity(db);}
   try(SQLiteOpenHelper old=new SQLiteOpenHelper(ctx,clone.getPath(),null,8){public void onCreate(SQLiteDatabase d){throw new AssertionError();}public void onUpgrade(SQLiteDatabase d,int a,int b){throw new AssertionError();}}){require(old.getReadableDatabase().getVersion()==8,"Old helper cannot open returned clone");}
   return new JSONObject().put("ok",true).put("clone_apply_return_preserved_data",true).put("helper8_opened",true).put("create_ms",createMs).put("drop_ms",dropMs);
  }finally{clone.delete();new File(clone+"-journal").delete();new File(clone+"-wal").delete();new File(clone+"-shm").delete();}
 }

 // Only v9's additive index is reversible this way. Never restore stale rows.
 JSONObject releasecheck()throws Exception {
  require(backup.isFile(),"Backup required");File clone=new File(ctx.getNoBackupFilesDir(),"release-return-test.db");require(!clone.exists(),"Clone exists");copy(backup,clone);
  try {
   String before,ddl;
   try(SQLiteDatabase db=open(clone,0)) {
    require(db.getVersion()==8,"Expected backup8");before=digest(db);ddl=schema(db);
    db.beginTransaction();try{db.execSQL(CREATE);db.setVersion(9);verifyIndex(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
    require(before.equals(digest(db))&&ddl.equals(schema(db)),"Upgrade changed data/schema");
    db.beginTransaction();try{db.setVersion(8);db.setTransactionSuccessful();}finally{db.endTransaction();}
    require(before.equals(digest(db))&&ddl.equals(schema(db)),"Return changed data/schema");integrity(db);
   }
   try(SQLiteOpenHelper old=new SQLiteOpenHelper(ctx,clone.getPath(),null,8){public void onCreate(SQLiteDatabase d){throw new AssertionError();}public void onUpgrade(SQLiteDatabase d,int a,int b){throw new AssertionError();}}){require(old.getReadableDatabase().getVersion()==8,"Helper8 return failed");}
   return new JSONObject().put("ok",true).put("upgrade9_return8_preserves_rows",true).put("helper8_opened",true);
  }finally{clone.delete();new File(clone+"-journal").delete();new File(clone+"-wal").delete();new File(clone+"-shm").delete();}
 }
 JSONObject return8()throws Exception {
  require(backup.isFile(),"Verified schema8 backup required");
  try(SQLiteDatabase db=open(live,0);SQLiteDatabase base=open(backup,SQLiteDatabase.OPEN_READONLY)) {
   require(base.getVersion()==8&&db.getVersion()==9,"Only verified v9 to8 allowed");integrity(base);
   db.beginTransaction();try {
    require(schema(base).equals(schema(db)),"Schema differs beyond owned index; abort return");verifyIndex(db);
    String before=digest(db);db.setVersion(8);require(before.equals(digest(db)),"Return changed current data");integrity(db);db.setTransactionSuccessful();
    return new JSONObject().put("ok",true).put("version",8).put("current_progress_preserved",true).put("data_sha256",before);
   }finally{db.endTransaction();}
  }
 }
 JSONObject modify(boolean apply)throws Exception {
  require(backup.isFile(),"Verified backup required");
  try(SQLiteDatabase db=open(live,0);SQLiteDatabase base=open(backup,SQLiteDatabase.OPEN_READONLY)){
   require(db.getVersion()==8&&base.getVersion()==8,"Keep version 8");integrity(base);db.beginTransaction();try{
    String before=digest(db),ddl=schema(db);require(ddl.equals(schema(base)),"Unexpected schema changed");
    if(apply){require(!index(db),"Index already present");require(before.equals(digest(base)),"Live data changed since backup; abort apply");db.execSQL(CREATE);verifyIndex(db);}else{require(index(db),"Index missing");verifyIndex(db);db.execSQL("DROP INDEX entries_series_group");}
    require(before.equals(digest(db))&&ddl.equals(schema(db))&&db.getVersion()==8,"Data or version changed");integrity(db);db.setTransactionSuccessful();
    return new JSONObject().put("ok",true).put("action",apply?"apply":"rollback").put("version",8).put("index",apply).put("data_sha256",before).put("schema_sha256",ddl);
   }finally{db.endTransaction();}
  }
 }
}
