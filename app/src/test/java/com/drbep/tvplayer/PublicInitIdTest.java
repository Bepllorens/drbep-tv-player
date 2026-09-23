package com.drbep.tvplayer;

import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import org.junit.Test;
import androidx.media3.common.C;
import androidx.media3.common.DrmInitData;
import androidx.media3.extractor.mp4.PsshAtomUtil;

public final class PublicInitIdTest {
  private byte[] message(byte[] id,byte[] suffix) {
    ByteArrayOutputStream out=new ByteArrayOutputStream();
    out.write(0x12);out.write(id.length);out.write(id,0,id.length);
    out.write(suffix,0,suffix.length);return out.toByteArray();
  }
  @Test public void cbcsFiveByteVarintDoesNotDiscardPublicId() {
    byte[] id=new byte[16];id[15]=42;
    byte[] suffix={(byte)0x48,(byte)0xf3,(byte)0xc6,(byte)0x89,(byte)0x9b,0x06};
    assertArrayEquals(id,PublicInitId.protobufId(message(id,suffix)));
  }
  @Test public void publicWidevineBoxIsReadWithoutLicenseData() {
    byte[] id=new byte[16];id[15]=42;
    byte[] payload=message(id,new byte[]{0x48,(byte)0xf3,(byte)0xc6,(byte)0x89,(byte)0x9b,0x06});
    byte[] box=PsshAtomUtil.buildPsshAtom(C.WIDEVINE_UUID,payload);
    assertArrayEquals(id,PublicInitId.single(new DrmInitData(
      new DrmInitData.SchemeData(C.WIDEVINE_UUID,"video/mp4",box))));
  }
  @Test public void differentDrmSystemIsNotUsed() {
    byte[] box=PsshAtomUtil.buildPsshAtom(C.PLAYREADY_UUID,message(new byte[16],new byte[0]));
    assertNull(PublicInitId.single(new DrmInitData(
      new DrmInitData.SchemeData(C.PLAYREADY_UUID,"video/mp4",box))));
  }
  @Test public void distinctIdsAreAmbiguous() {
    byte[] first=new byte[16],second=new byte[16];second[0]=1;
    assertNull(PublicInitId.protobufId(message(first,message(second,new byte[0]))));
  }
  @Test public void repeatedIdIsAccepted() {
    byte[] id=new byte[16];id[0]=3;
    assertArrayEquals(id,PublicInitId.protobufId(message(id,message(id,new byte[0]))));
  }
  @Test public void wrongLengthIsRejected() {
    assertNull(PublicInitId.protobufId(message(new byte[17],new byte[0])));
  }
  @Test public void truncatedFieldIsRejected() {
    assertNull(PublicInitId.protobufId(new byte[]{0x12,16,1}));
  }
  @Test public void oversizedInputIsRejected() {
    assertNull(PublicInitId.protobufId(new byte[49153]));
  }
  @Test public void uint32MaximumIsAccepted() {
    int[] position={0};
    assertEquals(-1,PublicInitId.varint(new byte[]{-1,-1,-1,-1,15},position));
    assertEquals(5,position[0]);
  }
  @Test(expected=IllegalArgumentException.class) public void overflowIsRejected() {
    PublicInitId.varint(new byte[]{-1,-1,-1,-1,16},new int[]{0});
  }
  @Test(expected=IllegalArgumentException.class) public void truncatedVarintIsRejected() {
    PublicInitId.varint(new byte[]{(byte)0x80},new int[]{0});
  }
}
