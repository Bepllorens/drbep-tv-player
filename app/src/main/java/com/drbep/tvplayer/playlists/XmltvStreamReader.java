package com.drbep.tvplayer.playlists;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
/** Bounded XMLTV reader: entities and external DTDs are disabled. */
public final class XmltvStreamReader {
    public static final class Programme { public String channel="",title="",description="",subtitle="",genre="",year="",image="",episode="",age="",rating="",country="";public long start,end;
        public final java.util.Map<String,String> credits=new java.util.LinkedHashMap<>();
        public String metadata() throws Exception {return new org.json.JSONObject().put("subtitle",subtitle).put("genre",genre).put("year",year).put("image",image).put("episode",episode).put("age",age).put("rating",rating).put("country",country).put("credits",new org.json.JSONObject(credits)).toString();} }
    public interface Sink {void accept(Programme p)throws Exception;}
    public interface ChannelSink {void accept(String id,String name)throws Exception;}
    public void read(InputStream input,Sink sink)throws Exception {read(input,sink,(id,name)->{});}
    public void read(InputStream input,Sink sink,ChannelSink channels)throws Exception {
        SAXParserFactory f=SAXParserFactory.newInstance();f.setNamespaceAware(false);
        f.setFeature("http://xml.org/sax/features/external-general-entities",false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities",false);
        try { f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true); }
        catch(ParserConfigurationException | SAXNotRecognizedException | SAXNotSupportedException ignored) { }
        // Android's SAX implementation does not support every Xerces feature. This
        // byte guard independently forbids DTDs and non UTF-8 compatible encodings.
        InputStream guarded=new FilterInputStream(input) {
            final byte[] marker="<!DOCTYPE".getBytes(java.nio.charset.StandardCharsets.US_ASCII);int matched;
            private void check(int b)throws IOException {
                if(b==0)throw new IOException("XMLTV must use a UTF-8 compatible encoding");
                if(b==marker[matched]){if(++matched==marker.length)throw new IOException("DTD forbidden");}
                else matched=b==marker[0]?1:0;
            }
            public int read()throws IOException{int b=in.read();if(b>=0)check(b);return b;}
            public int read(byte[] b,int off,int len)throws IOException{int n=in.read(b,off,len);for(int i=0;i<n;i++)check(b[off+i]&255);return n;}
        };
        // DateFormat is mutable: keep it local to this read, never shared between imports.
        final DateParser dates=new DateParser();
        f.newSAXParser().parse(guarded,new DefaultHandler(){
            String channelId="",channelName="";
            Programme current;String capture="",container="",episodeSystem="";StringBuilder text=new StringBuilder();
            public void startElement(String uri,String local,String name,Attributes a)throws SAXException {
                if(Thread.currentThread().isInterrupted())throw new SAXException("Cancelled");
                if(name.equals("channel")){channelId=a.getValue("id");channelName="";}
                if(name.equals("display-name")&&current==null){capture=name;text.setLength(0);}
                if(name.equals("programme")){current=new Programme();current.channel=a.getValue("channel");try{current.start=dates.parse(a.getValue("start"));current.end=dates.parse(a.getValue("stop"));}catch(Exception e){throw new SAXException("Invalid programme time");}}
                else if(current!=null) {
                    if(name.equals("rating")||name.equals("star-rating"))container=name;
                    if(name.equals("episode-num"))episodeSystem=a.getValue("system");
                    if(name.equals("icon")&&current.image.isEmpty()){String src=a.getValue("src");if(src!=null&&(src.startsWith("https://")||src.startsWith("http://")))current.image=src.length()<=8192?src:"";}
                    if(isTextElement(name)){capture=name;text.setLength(0);}
                }
            }
            public void characters(char[] ch,int start,int length)throws SAXException {if(!capture.isEmpty()){if(text.length()+length>16384)throw new SAXException("Programme text too long");text.append(ch,start,length);}}
            public void endElement(String uri,String local,String name)throws SAXException {
                if(current==null){
                    if(name.equals("display-name")){channelName=text.toString();capture="";}
                    if(name.equals("channel")&&channelId!=null&&!channelId.isEmpty()&&!channelName.isEmpty())try{channels.accept(channelId,channelName);}catch(Exception e){throw new SAXException(e);}
                    return;
                }
                if(name.equals(capture)){if(name.equals("title")&&current.title.isEmpty())current.title=text.toString();if(name.equals("desc")&&current.description.isEmpty())current.description=text.toString();String value=text.toString().trim();
                    switch(name){case "sub-title":current.subtitle=value;break;case "category":if(current.genre.length()<1024)current.genre=current.genre.isEmpty()?value:current.genre+", "+value;break;case "date":current.year=value.length()>=4?value.substring(0,4):value;break;case "episode-num":if(current.episode.isEmpty())current.episode=episode(value,episodeSystem);break;case "country":current.country=value;break;case "director":case "actor":case "presenter":case "writer":String previous=current.credits.get(name);if(previous==null||previous.length()<4096)current.credits.put(name,previous==null?value:previous+", "+value);break;case "value":if(container.equals("rating"))current.age=value;else if(container.equals("star-rating"))current.rating=value;break;default:break;}
                    capture="";}
                if(name.equals(container))container="";
                if(name.equals("programme")){if(current.channel==null||current.channel.isEmpty()||current.end<=current.start)throw new SAXException("Invalid programme");try{sink.accept(current);}catch(Exception e){throw new SAXException(e);}current=null;}
            }
        });
    }
    static String episode(String value,String system) {
        if(!"xmltv_ns".equals(system))return value;
        String[] parts=value.split("\\.");StringBuilder out=new StringBuilder();
        for(int i=0;i<Math.min(2,parts.length);i++)try {
            String n=parts[i].trim().split("/")[0].trim();if(n.isEmpty())continue;
            if(out.length()>0)out.append(" ");out.append(i==0?"T":"E").append(Integer.parseInt(n)+1);
        }catch(NumberFormatException ignored){}
        return out.toString();
    }
    private static boolean isTextElement(String name) {
        switch(name) {
            case "title":case "desc":case "sub-title":case "category":case "date":
            case "episode-num":case "country":case "director":case "actor":
            case "presenter":case "writer":case "value":return true;
            default:return false;
        }
    }
    static final class DateParser {
        private final SimpleDateFormat format=new SimpleDateFormat("yyyyMMddHHmmss Z",Locale.ROOT);
        private final TimeZone defaultZone=(TimeZone)format.getTimeZone().clone();
        private final ParsePosition position=new ParsePosition(0);
        DateParser(){format.setLenient(false);}
        long parse(String value)throws ParseException {
            if(value==null)throw new ParseException("Missing time",0);
            String s=value.trim();
            if(isBareUtc(s))s+=" +0000";
            // Named zones may mutate DateFormat's calendar. Every value must start
            // with the same default zone as a newly constructed legacy formatter.
            format.setTimeZone(defaultZone);
            position.setIndex(0);position.setErrorIndex(-1);
            Date parsed=format.parse(s,position);
            if(parsed==null||position.getIndex()!=s.length())throw new ParseException("Invalid XMLTV time",0);
            return parsed.getTime();
        }
        private static boolean isBareUtc(String value) {
            if(value.length()!=14)return false;
            for(int i=0;i<14;i++)if(value.charAt(i)<'0'||value.charAt(i)>'9')return false;
            return true;
        }
    }
    static long date(String value)throws ParseException {return new DateParser().parse(value);}
}
