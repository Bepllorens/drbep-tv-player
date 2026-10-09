package com.drbep.tvplayer;
import java.net.URI;
import java.util.EnumMap;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
final class DevicePanelQr {
    static BitMatrix encode(String address) throws Exception {
        URI uri=new URI(address);
        if(!"http".equals(uri.getScheme())||uri.getHost()==null||uri.getPort()<1||uri.getPort()>65535||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null||!(uri.getPath().isEmpty()||uri.getPath().equals("/"))) throw new IllegalArgumentException("Panel QR must contain only its address");
        EnumMap<EncodeHintType,Object> hints=new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN,4);hints.put(EncodeHintType.ERROR_CORRECTION,ErrorCorrectionLevel.M);
        return new QRCodeWriter().encode(address,BarcodeFormat.QR_CODE,384,384,hints);
    }
}
