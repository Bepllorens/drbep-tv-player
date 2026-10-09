package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import com.google.zxing.*;
import com.google.zxing.common.*;
import com.google.zxing.qrcode.QRCodeReader;
public class DevicePanelQrTest {
    @Test public void decodesExactlyTheAddress() throws Exception {
        String url="http://192.168.93.190:49152";BitMatrix m=DevicePanelQr.encode(url);
        int[] pixels=new int[m.getWidth()*m.getHeight()];for(int y=0;y<m.getHeight();y++)for(int x=0;x<m.getWidth();x++)pixels[y*m.getWidth()+x]=m.get(x,y)?0xff000000:0xffffffff;
        String decoded=new QRCodeReader().decode(new BinaryBitmap(new HybridBinarizer(new RGBLuminanceSource(m.getWidth(),m.getHeight(),pixels)))).getText();
        assertEquals(url,decoded);assertFalse(m.get(0,0));
    }
    @Test public void rejectsCredentialsAndPinParameters() throws Exception {
        for(String url:new String[]{"http://192.168.93.190:1234/?pin=123456","http://u:p@192.168.93.190:1234","http://192.168.93.190:1234/#secret","http://192.168.93.190:1234/session"}) {
            try {DevicePanelQr.encode(url);fail();}catch(IllegalArgumentException expected){}
        }
    }
}
