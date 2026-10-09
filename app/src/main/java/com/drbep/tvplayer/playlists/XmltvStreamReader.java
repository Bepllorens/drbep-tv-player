package com.drbep.tvplayer.playlists;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;
/** Bounded XMLTV reader: entities and external DTDs are disabled. */
public final class XmltvStreamReader {
    public static final class Programme { public String channel="",title="",description="";public long start,end; }
    public interface Sink {void accept(Programme p)throws Exception;}
    public void read(InputStream input,Sink sink)throws Exception {
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
        f.newSAXParser().parse(guarded,new DefaultHandler(){
            Programme current;String capture="";StringBuilder text=new StringBuilder();
            public void startElement(String uri,String local,String name,Attributes a)throws SAXException {
                if(Thread.currentThread().isInterrupted())throw new SAXException("Cancelled");
                if(name.equals("programme")){current=new Programme();current.channel=a.getValue("channel");try{current.start=date(a.getValue("start"));current.end=date(a.getValue("stop"));}catch(Exception e){throw new SAXException("Invalid programme time");}}
                else if(current!=null&&(name.equals("title")||name.equals("desc"))){capture=name;text.setLength(0);}
            }
            public void characters(char[] ch,int start,int length)throws SAXException {if(!capture.isEmpty()){if(text.length()+length>16384)throw new SAXException("Programme text too long");text.append(ch,start,length);}}
            public void endElement(String uri,String local,String name)throws SAXException {
                if(current==null)return;
                if(name.equals(capture)){if(name.equals("title")&&current.title.isEmpty())current.title=text.toString();if(name.equals("desc")&&current.description.isEmpty())current.description=text.toString();capture="";}
                if(name.equals("programme")){if(current.channel==null||current.channel.isEmpty()||current.end<=current.start)throw new SAXException("Invalid programme");try{sink.accept(current);}catch(Exception e){throw new SAXException(e);}current=null;}
            }
        });
    }
    static long date(String value)throws ParseException {if(value==null)throw new ParseException("Missing time",0);String s=value.trim();if(s.matches("[0-9]{14}"))s+=" +0000";SimpleDateFormat f=new SimpleDateFormat("yyyyMMddHHmmss Z",Locale.ROOT);f.setLenient(false);ParsePosition pos=new ParsePosition(0);Date date=f.parse(s,pos);if(date==null||pos.getIndex()!=s.length())throw new ParseException("Invalid XMLTV time",0);return date.getTime();}
}
