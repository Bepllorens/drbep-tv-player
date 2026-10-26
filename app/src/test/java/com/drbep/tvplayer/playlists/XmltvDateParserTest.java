package com.drbep.tvplayer.playlists;
import java.io.*;import java.nio.charset.StandardCharsets;import java.text.*;import java.util.*;import java.util.concurrent.*;
import org.junit.Test;import static org.junit.Assert.*;
public class XmltvDateParserTest {
    private long legacy(String value)throws ParseException {
        if(value==null)throw new ParseException("Missing time",0);
        String s=value.trim();if(s.matches("[0-9]{14}"))s+=" +0000";
        SimpleDateFormat f=new SimpleDateFormat("yyyyMMddHHmmss Z",Locale.ROOT);f.setLenient(false);
        ParsePosition position=new ParsePosition(0);Date date=f.parse(s,position);
        if(date==null||position.getIndex()!=s.length())throw new ParseException("Invalid XMLTV time",0);
        return date.getTime();
    }
    private void same(XmltvStreamReader.DateParser parser,String value)throws Exception {
        Long expected=null;try{expected=legacy(value);}catch(ParseException rejected){}
        if(expected==null) {try{parser.parse(value);fail("Accepted an invalid legacy date: "+value);}catch(ParseException rejected){}}
        else assertEquals("Date changed: "+value,expected.longValue(),parser.parse(value));
    }
    @Test public void reusesFormatterWithoutChangingOffsetsOrValidation()throws Exception {
        XmltvStreamReader.DateParser parser=new XmltvStreamReader.DateParser();
        String[] dates={"20261025013000","20261025023000","20240229120000","20230229120000",
            "20261026000000","20261301120000","20261026240000","00000101120000"," 20261026121212 ",
            "",null,"202610261212","20261026121212 +0200 trailing","20261026121212  +0200"};
        String[] zones={""," +0000"," +0200"," +0100"," -0500"," +0530"," +1245"," GMT"," PST"," UTC"," +2500"," +1260"};
        for(int round=0;round<4;round++)for(String date:dates)for(String zone:zones)same(parser,date==null?null:date+zone);
    }
    @Test public void failedDateDoesNotContaminateNextProgramme()throws Exception {
        XmltvStreamReader.DateParser parser=new XmltvStreamReader.DateParser();
        same(parser,"20260230120000 +0200");same(parser,"20261026120000 GMT");
        same(parser,"20261026120000 -0500");same(parser,"20261026120000");
        same(parser,"not a date");same(parser,"20261026120000 +0530");
    }
    @Test public void sameReaderCanParseConcurrentFeedsWithDifferentZones()throws Exception {
        XmltvStreamReader reader=new XmltvStreamReader();ExecutorService pool=Executors.newFixedThreadPool(4);
        try {
            List<Future<Boolean>> futures=new ArrayList<>();
            for(String zone:new String[]{"+0000","+0200","-0500","+0530"})futures.add(pool.submit(()->{
                for(int i=0;i<40;i++){
                    String xml="<tv><programme channel='c' start='20261026120000 "+zone+"' stop='20261026130000 "+zone+"'><title>Programa</title></programme></tv>";
                    long[] times=new long[2];reader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),p->{times[0]=p.start;times[1]=p.end;});
                    if(times[0]!=legacy("20261026120000 "+zone)||times[1]!=legacy("20261026130000 "+zone))return false;
                }return true;
            }));
            for(Future<Boolean> f:futures)assertTrue(f.get(10,TimeUnit.SECONDS));
        }finally{pool.shutdownNow();}
    }
}
