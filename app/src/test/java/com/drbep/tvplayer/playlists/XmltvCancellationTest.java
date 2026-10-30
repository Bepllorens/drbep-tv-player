package com.drbep.tvplayer.playlists;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

public class XmltvCancellationTest {
    static final class CommentStream extends InputStream {
        final byte[] head="<tv><!--".getBytes(StandardCharsets.UTF_8);
        final byte[] tail="--></tv>".getBytes(StandardCharsets.UTF_8);
        final int length=10*1024*1024;
        int count;
        final boolean single;
        CommentStream(boolean single){this.single=single;}
        public int read(){
            int result=count<head.length?head[count]:count<head.length+length?'x':
                count<head.length+length+tail.length?tail[count-head.length-length]:-1;
            if(result>=0 && ++count>=32768)Thread.currentThread().interrupt();
            return result;
        }
        public int read(byte[] bytes,int off,int len){
            int n=0,limit=single?1:Math.min(len,4096);
            while(n<limit){int b=read();if(b<0)break;bytes[off+n++]=(byte)b;}
            return n==0?-1:n;
        }
    }
    private void checkComment(boolean single)throws Exception {
        CommentStream input=new CommentStream(single);
        try {
            new XmltvStreamReader().read(input,p->fail("No programmes in comment"));
            fail("Cancellation expected");
        }catch(IOException | org.xml.sax.SAXException expected){
            assertTrue(Thread.currentThread().isInterrupted());
            assertTrue("Must stop before reading full ignored comment: "+input.count,input.count<=36864);
        }finally{Thread.interrupted();}
    }
    @Test public void cancelsInsideLargeIgnoredComment()throws Exception{checkComment(false);}
    @Test public void cancelsWithSingleByteChunks()throws Exception{checkComment(true);}
    @Test public void alreadyInterruptedDoesNotReadInput()throws Exception{
        try {
            Thread.currentThread().interrupt();
            new XmltvStreamReader().read(new InputStream(){public int read(){throw new AssertionError("Must not read");}},p->{});
            fail();
        }catch(IOException | org.xml.sax.SAXException expected){assertTrue(Thread.currentThread().isInterrupted());}
        finally{Thread.interrupted();}
    }
    @Test public void readerWorksAgainAfterCallerClearsCancellation()throws Exception{
        XmltvStreamReader reader=new XmltvStreamReader();
        try{
            Thread.currentThread().interrupt();
            reader.read(new ByteArrayInputStream("<tv/>".getBytes(StandardCharsets.UTF_8)),p->{});
            fail();
        }catch(IOException | org.xml.sax.SAXException expected){}
        finally{Thread.interrupted();}
        AtomicInteger count=new AtomicInteger();
        String xml="<tv><programme channel=\"synthetic\" start=\"20261030000000 +0000\" stop=\"20261030010000 +0000\"><title>Test</title></programme></tv>";
        reader.read(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)),p->count.incrementAndGet());
        assertEquals(1,count.get());
    }
}
