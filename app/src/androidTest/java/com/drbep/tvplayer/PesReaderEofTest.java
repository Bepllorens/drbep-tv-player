package com.drbep.tvplayer;

import androidx.media3.extractor.ts.PesReader;
import androidx.media3.common.util.ParsableBitArray;
import org.junit.Test;
import java.lang.reflect.*;
import static org.junit.Assert.*;

/** Reproduces reuse of the header scratch buffer by PTS/DTS at a segment boundary. */
public class PesReaderEofTest {
    private static Field field(String name) throws Exception {
        Field f = PesReader.class.getDeclaredField(name); f.setAccessible(true); return f;
    }
    private static boolean parse(PesReader reader, byte[] bytes) throws Exception {
        ParsableBitArray scratch = (ParsableBitArray) field("pesScratch").get(reader);
        System.arraycopy(bytes, 0, scratch.data, 0, bytes.length);
        Method m = PesReader.class.getDeclaredMethod("parseHeader"); m.setAccessible(true);
        return (boolean) m.invoke(reader);
    }
    @Test public void ptsScratchAtEofDoesNotReparseOrMutatePayload() throws Exception {
        PesReader reader = new PesReader(null);
        assertTrue(parse(reader, new byte[]{0,0,1,(byte)0xe0,0,0,(byte)0x80,(byte)0x80,5}));
        field("state").setInt(reader,3); // reading body, unbounded video PES
        ParsableBitArray scratch = (ParsableBitArray) field("pesScratch").get(reader);
        scratch.data[0]=0x21; scratch.data[1]=0; scratch.data[2]=0x7d; // PTS, not a header
        assertTrue(reader.canConsumeSynthesizedEmptyPusi(true));
        assertTrue(reader.canConsumeSynthesizedEmptyPusi(true));
        assertEquals(-1,field("payloadSize").getInt(reader));
        field("payloadSize").setInt(reader,137); // incomplete bounded PES must not flush
        assertFalse(reader.canConsumeSynthesizedEmptyPusi(true));
        assertEquals(137,field("payloadSize").getInt(reader));
    }
    @Test public void invalidOrPartialHeadersStillCannotFlush() throws Exception {
        PesReader reader = new PesReader(null);
        assertFalse(parse(reader,new byte[]{0x21,0,0,0,0,0,0,0,0}));
        field("state").setInt(reader,1);
        assertFalse(reader.canConsumeSynthesizedEmptyPusi(true));
        assertTrue(parse(reader,new byte[]{0,0,1,(byte)0xe0,0,0,(byte)0x80,(byte)0x80,5}));
        field("state").setInt(reader,2);
        assertFalse(reader.canConsumeSynthesizedEmptyPusi(true));
    }
}
