package com.drbep.tvplayer;

import androidx.media3.common.C;
import androidx.media3.common.DrmInitData;
import androidx.media3.extractor.mp4.PsshAtomUtil;
import java.nio.ByteBuffer;
import java.util.Arrays;

/** Reads only public initialization identifiers, never keys or licenses. */
final class PublicInitId {
  static byte[] single(DrmInitData data) {
    if(data==null)return null;
    byte[] found=null;
    try {
      for(int i=0;i<data.schemeDataCount;i++) {
        DrmInitData.SchemeData scheme=data.get(i);
        if(!scheme.matches(C.WIDEVINE_UUID)||scheme.data==null)continue;
        PsshAtomUtil.PsshAtom atom=PsshAtomUtil.parsePsshAtom(scheme.data);
        if(atom==null||!C.WIDEVINE_UUID.equals(atom.uuid))return null;
        byte[] candidate;
        if(atom.keyIds!=null && atom.keyIds.length>0) {
          if(atom.keyIds.length!=1)return null;
          candidate=ByteBuffer.allocate(16).putLong(atom.keyIds[0].getMostSignificantBits()).putLong(atom.keyIds[0].getLeastSignificantBits()).array();
        } else candidate=protobufId(atom.schemeData);
        if(candidate==null || (found!=null&&!Arrays.equals(found,candidate)))return null;
        found=candidate;
      }
    } catch(RuntimeException invalid){return null;}
    return found;
  }
  static int varint(byte[] data,int[] position) {
    int value=0;
    for(int shift=0;shift<35;shift+=7) {
      if(position[0]>=data.length)throw new IllegalArgumentException();
      int b=data[position[0]++]&255;
      if(shift==28 && (b&0xf0)!=0)throw new IllegalArgumentException();
      value|=(b&127)<<shift;
      if((b&128)==0)return value;
    }
    throw new IllegalArgumentException();
  }
  static byte[] protobufId(byte[] data) {
    if(data==null||data.length>49152)return null;
    int[] pos={0};byte[] found=null;
    while(pos[0]<data.length) {
      int tag=varint(data,pos), field=tag>>>3,wire=tag&7;
      if(field==0)return null;
      if(wire==0){varint(data,pos);continue;}
      int length=wire==2?varint(data,pos):wire==1?8:wire==5?4:-1;
      if(length<0||length>data.length-pos[0])return null;
      if(field==2&&wire==2) {
        if(length!=16)return null;
        byte[] value=Arrays.copyOfRange(data,pos[0],pos[0]+16);
        if(found!=null&&!Arrays.equals(found,value))return null;
        found=value;
      }
      pos[0]+=length;
    }
    return found;
  }
}
