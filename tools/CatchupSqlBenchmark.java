import android.database.sqlite.*;
import android.database.Cursor;
import java.util.*;
/** Shell-only synthetic fixture. Never opens application databases. */
public class CatchupSqlBenchmark {
 static String sql="SELECT payload FROM programs WHERE scope=? AND channel=? AND start<=? AND end>=? ORDER BY start DESC,id LIMIT ?";
 static String[] args={"scope","12","80000","0","40"};
 static List<String> run(SQLiteDatabase db,String label) {
  try(Cursor c=db.rawQuery("EXPLAIN QUERY PLAN "+sql,args)){while(c.moveToNext())System.out.println(label+" plan "+c.getString(3));}
  List<String> rows=new ArrayList<>();long[] samples=new long[60];
  for(int i=0;i<samples.length;i++){rows.clear();long t=System.nanoTime();
   try(Cursor c=db.rawQuery(sql,args)){while(c.moveToNext())rows.add(c.getString(0));}
   samples[i]=(System.nanoTime()-t)/1000;
  }
  Arrays.sort(samples);System.out.println(label+" median_us="+samples[30]+" p95_us="+samples[57]+" rows="+rows.size());return new ArrayList<>(rows);
 }
 public static void main(String[] ignored) {
  SQLiteDatabase db=SQLiteDatabase.create(null);
  db.execSQL("CREATE TABLE programs(scope TEXT,id TEXT,channel TEXT,day TEXT,start INTEGER,end INTEGER,genre TEXT,payload TEXT,PRIMARY KEY(scope,id))");
  db.execSQL("CREATE INDEX programs_channel ON programs(scope,channel,day,start DESC)");
  db.beginTransaction();
  try(SQLiteStatement s=db.compileStatement("INSERT INTO programs VALUES(?,?,?,?,?,?,?,?)")){
   for(int i=0;i<80000;i++){
    s.bindString(1,"scope");s.bindString(2,""+i);s.bindString(3,""+(i%180));s.bindString(4,""+((i/180)%8));
    s.bindLong(5,i);s.bindLong(6,i+60);s.bindString(7,"genre");s.bindString(8,"item:"+i);s.executeInsert();
   }db.setTransactionSuccessful();
  }finally{db.endTransaction();}
  List<String> old=run(db,"before");
  db.execSQL("CREATE INDEX programs_strip ON programs(scope,channel,start DESC,id)");
  List<String> newer=run(db,"after");
  if(!old.equals(newer))throw new AssertionError("Rows differ");
  System.out.println("PASS same programmes and ordering");db.close();
 }
}
