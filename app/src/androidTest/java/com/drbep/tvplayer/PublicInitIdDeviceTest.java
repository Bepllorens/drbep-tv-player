package com.drbep.tvplayer;
import static org.junit.Assert.*;
import java.io.ByteArrayOutputStream;
import org.junit.Test;
import androidx.media3.common.C;
import androidx.media3.common.DrmInitData;
import androidx.media3.extractor.mp4.PsshAtomUtil;
/** Media3 1.11 parser inspects android.os.Build; run with the real Android runtime. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public class PublicInitIdDeviceTest {
  private byte[] message(byte[] id,byte[] suffix) {
    ByteArrayOutputStream out=new ByteArrayOutputStream();
    out.write(0x12);out.write(id.length);out.write(id,0,id.length);
    out.write(suffix,0,suffix.length);return out.toByteArray();
  }
  @Test public void publicWidevineBoxIsReadWithoutLicenseData() {
    byte[] id=new byte[16];id[15]=42;
    byte[] payload=message(id,new byte[]{0x48,(byte)0xf3,(byte)0xc6,(byte)0x89,(byte)0x9b,0x06});
    byte[] box=PsshAtomUtil.buildPsshAtom(C.WIDEVINE_UUID,payload);
    assertArrayEquals(id,PublicInitId.single(new DrmInitData(
      new DrmInitData.SchemeData(C.WIDEVINE_UUID,"video/mp4",box))));
  }
}
