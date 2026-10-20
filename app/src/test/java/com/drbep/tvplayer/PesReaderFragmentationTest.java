package com.drbep.tvplayer;

import androidx.media3.extractor.ts.*;
import androidx.media3.extractor.ExtractorOutput;
import androidx.media3.common.util.*;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import org.junit.Test;
import static org.junit.Assert.*;

/** Real Media3 PES reader with synthetic, unencrypted bytes; no network or decoder. */
public class PesReaderFragmentationTest {
 @org.junit.BeforeClass public static void enforceBounds(){ParsableByteArray.setShouldEnforceLimitOnLegacyMethods(true);}
 @org.junit.AfterClass public static void restoreDetection(){ParsableByteArray.setShouldEnforceLimitOnLegacyMethods(null);}

 static final class Sink implements ElementaryStreamReader {
  final ByteArrayOutputStream data=new ByteArrayOutputStream();
  int starts,finishes,seeks;long lastTime;
  public void seek(){seeks++;}
  public void createTracks(ExtractorOutput out,TsPayloadReader.TrackIdGenerator ids){}
  public void packetStarted(long time,int flags){starts++;lastTime=time;}
  public void consume(ParsableByteArray bytes){while(bytes.bytesLeft()>0)data.write(bytes.readUnsignedByte());}
  public void packetFinished(){finishes++;}
 }
 static byte[] packet(int extension,byte[] body){
  int length=3+extension+body.length;
  byte[] p=new byte[9+extension+body.length];
  p[2]=1;p[3]=(byte)0xe0;p[4]=(byte)(length>>8);p[5]=(byte)length;p[6]=(byte)0x80;
  p[8]=(byte)extension;Arrays.fill(p,9,9+extension,(byte)0xff);
  System.arraycopy(body,0,p,9+extension,body.length);return p;
 }
 static final class Probe {
  final Object target;final Class<?> type;
  Probe(Sink sink)throws Exception {
   org.objectweb.asm.ClassWriter output=new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_FRAMES|org.objectweb.asm.ClassWriter.COMPUTE_MAXS);
   try(java.io.InputStream in=PesReader.class.getResourceAsStream("PesReader.class")){
    new org.objectweb.asm.ClassReader(in).accept(new com.drbep.build.PesReaderFix(new com.drbep.build.PesReaderFragmentFix(output)),0);
   }
   byte[] code=output.toByteArray();
   type=new ClassLoader(PesReader.class.getClassLoader()){
    Class<?> patched(){return defineClass(PesReader.class.getName(),code,0,code.length);}
   }.patched();
   target=type.getConstructor(ElementaryStreamReader.class).newInstance(sink);
   type.getMethod("init",TimestampAdjuster.class,ExtractorOutput.class,TsPayloadReader.TrackIdGenerator.class)
     .invoke(target,new TimestampAdjuster(0),null,new TsPayloadReader.TrackIdGenerator(0,1));
  }
  void consume(ParsableByteArray bytes,int flags)throws Exception{type.getMethod("consume",ParsableByteArray.class,int.class).invoke(target,bytes,flags);}
  void seek()throws Exception{type.getMethod("seek").invoke(target);}
  boolean canConsumeSynthesizedEmptyPusi(boolean value)throws Exception{return (boolean)type.getMethod("canConsumeSynthesizedEmptyPusi",boolean.class).invoke(target,value);}
 }
 static Probe reader(Sink sink)throws Exception{return new Probe(sink);}
 static void feed(Probe r,byte[] p,int from,int to,int flags)throws Exception{
  r.consume(new ParsableByteArray(Arrays.copyOfRange(p,from,to)),flags);
 }
 @Test public void everyHeaderAndBodySplitPreservesPayload()throws Exception{
  byte[] body={11,22,33,44};
  for(int extension:new int[]{0,3,20}){
   byte[] p=packet(extension,body);
   for(int split=1;split<p.length;split++){
    Sink sink=new Sink();Probe r=reader(sink);
    feed(r,p,0,split,TsPayloadReader.FLAG_PAYLOAD_UNIT_START_INDICATOR);
    feed(r,p,split,p.length,0);
    assertArrayEquals("extension="+extension+" split="+split,body,sink.data.toByteArray());
    assertEquals(1,sink.starts);assertEquals(1,sink.finishes);
   }
  }
 }
 @Test public void oneByteChunksAndConsecutivePacketsDoNotMixBodies()throws Exception{
  Sink sink=new Sink();Probe r=reader(sink);
  for(byte[] body:new byte[][]{{1,2,3},{4,5}}){
   byte[] p=packet(20,body);
   for(int i=0;i<p.length;i++)feed(r,p,i,i+1,i==0?TsPayloadReader.FLAG_PAYLOAD_UNIT_START_INDICATOR:0);
  }
  assertArrayEquals(new byte[]{1,2,3,4,5},sink.data.toByteArray());assertEquals(2,sink.starts);assertEquals(2,sink.finishes);
 }
 @Test public void seekDiscardsPartialHeaderBeforeNextPacket()throws Exception{
  Sink sink=new Sink();Probe r=reader(sink);byte[] abandoned=packet(0,new byte[]{99});
  feed(r,abandoned,0,5,TsPayloadReader.FLAG_PAYLOAD_UNIT_START_INDICATOR);
  r.seek();byte[] next=packet(0,new byte[]{7,8});
  feed(r,next,0,next.length,TsPayloadReader.FLAG_PAYLOAD_UNIT_START_INDICATOR);
  assertEquals(1,sink.seeks);assertEquals(1,sink.starts);assertArrayEquals(new byte[]{7,8},sink.data.toByteArray());
 }
 @Test public void truncatedBoundedPacketDoesNotSignalCompletion()throws Exception{
  Sink sink=new Sink();Probe r=reader(sink);byte[] p=packet(0,new byte[]{1,2,3,4});
  feed(r,p,0,p.length-2,TsPayloadReader.FLAG_PAYLOAD_UNIT_START_INDICATOR);
  assertEquals(1,sink.starts);assertEquals(0,sink.finishes);assertFalse(r.canConsumeSynthesizedEmptyPusi(true));
 }

 @Test public void originalReaderPrematurelyStartsAtExtendedHeaderBoundary()throws Exception {
  Sink original=new Sink();PesReader raw=new PesReader(original);
  raw.init(new TimestampAdjuster(0),null,new TsPayloadReader.TrackIdGenerator(0,1));
  byte[] p=packet(20,new byte[]{1,2,3});
  raw.consume(new ParsableByteArray(Arrays.copyOfRange(p,0,19)),TsPayloadReader.FLAG_PAYLOAD_UNIT_START_INDICATOR);
  assertEquals("Upstream change requires reviewing the patch",1,original.starts);
  Sink fixed=new Sink();Probe patched=reader(fixed);
  feed(patched,p,0,19,TsPayloadReader.FLAG_PAYLOAD_UNIT_START_INDICATOR);
  assertEquals(0,fixed.starts);
  feed(patched,p,19,p.length,0);assertArrayEquals(new byte[]{1,2,3},fixed.data.toByteArray());
 }
 static void timestamp(byte[] out,int offset,long value,int prefix) {
  out[offset]=(byte)((prefix<<4)|(((value>>30)&7)<<1)|1);
  out[offset+1]=(byte)(value>>22);out[offset+2]=(byte)((((value>>15)&127)<<1)|1);
  out[offset+3]=(byte)(value>>7);out[offset+4]=(byte)(((value&127)<<1)|1);
 }
 @Test public void fragmentedPtsDtsAndStuffingPreserveTimestampAndPayload()throws Exception {
  byte[] body={8,9,10};byte[] p=packet(20,body);p[7]=(byte)0xc0;
  timestamp(p,9,180000,3);timestamp(p,14,90000,1);
  for(int split=1;split<p.length;split++){
   Sink sink=new Sink();Probe r=reader(sink);
   feed(r,p,0,split,TsPayloadReader.FLAG_PAYLOAD_UNIT_START_INDICATOR);
   feed(r,p,split,p.length,0);
   assertArrayEquals("split="+split,body,sink.data.toByteArray());
   assertEquals(1000000,sink.lastTime);assertEquals(1,sink.starts);assertEquals(1,sink.finishes);
  }
 }
}
