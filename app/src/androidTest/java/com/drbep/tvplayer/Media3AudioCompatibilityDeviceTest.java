package com.drbep.tvplayer;

import androidx.media3.common.Format;
import androidx.media3.exoplayer.audio.AudioSink;
import com.google.common.primitives.ImmutableIntArray;
import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.reflect.Proxy;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicReference;

/** The beta diagnostic wrapper must forward Media3 1.11 configuration without changing it. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public class Media3AudioCompatibilityDeviceTest {
    @Test public void forwardsConfigurationAndAudioBuffersUnchanged() throws Exception {
        AtomicReference<AudioSink.AudioSinkConfig> received=new AtomicReference<>();
        AtomicReference<ByteBuffer> audio=new AtomicReference<>();
        AudioSink delegate=(AudioSink)Proxy.newProxyInstance(AudioSink.class.getClassLoader(),new Class<?>[]{AudioSink.class},(proxy,method,args)->{
            if(method.getName().equals("configure"))received.set((AudioSink.AudioSinkConfig)args[0]);
            if(method.getName().equals("handleBuffer")){audio.set((ByteBuffer)args[0]);return true;}
            return null;
        });
        DiagnosticAudioSink sink=new DiagnosticAudioSink(delegate);
        Format format=new Format.Builder().setSampleMimeType("audio/raw").setSampleRate(48000).setChannelCount(2).build();
        AudioSink.AudioSinkConfig config=new AudioSink.AudioSinkConfig.Builder(format)
                .setPreferredBufferSizeOverride(16384).setOutputChannelMapping(ImmutableIntArray.of(1,0)).build();
        sink.configure(config);
        assertSame(config,received.get());
        ByteBuffer buffer=ByteBuffer.allocateDirect(16);buffer.putInt(42).flip();
        assertTrue(sink.handleBuffer(buffer,12345,1));assertSame(buffer,audio.get());assertEquals(0,buffer.position());assertEquals(42,buffer.getInt(0));
        sink.flush();sink.reset();
    }
}
