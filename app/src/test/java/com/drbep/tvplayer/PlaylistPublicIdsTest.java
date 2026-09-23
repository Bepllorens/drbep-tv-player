package com.drbep.tvplayer;

import static org.junit.Assert.*;
import java.lang.reflect.Method;
import org.junit.Test;

public final class PlaylistPublicIdsTest {
  private void bind(PlaylistPublicIds index,String uri,byte[] id) throws Exception {
    Method method=PlaylistPublicIds.class.getDeclaredMethod("bind",String.class,byte[].class);
    method.setAccessible(true);method.invoke(index,uri,id);
  }
  @Test public void uriIncludingQueryMustMatchExactly() throws Exception {
    PlaylistPublicIds index=new PlaylistPublicIds();byte[] id=new byte[16];id[0]=7;
    bind(index,"https://example.test/segment?variant=one",id);
    assertArrayEquals(id,index.get("https://example.test/segment?variant=one"));
    assertNull(index.get("https://example.test/segment?variant=two"));
  }
  @Test public void conflictingMappingStaysRejected() throws Exception {
    PlaylistPublicIds index=new PlaylistPublicIds();byte[] a=new byte[16],b=new byte[16];b[0]=1;
    bind(index,"one",a);bind(index,"one",b);bind(index,"one",a);
    assertNull(index.get("one"));
  }
  @Test public void returnedArrayCannotMutateIndex() throws Exception {
    PlaylistPublicIds index=new PlaylistPublicIds();byte[] id=new byte[16];
    bind(index,"one",id);index.get("one")[0]=7;
    assertArrayEquals(id,index.get("one"));
  }
  @Test public void resourceLimitDisablesIndexRatherThanEvictingConflicts() throws Exception {
    PlaylistPublicIds index=new PlaylistPublicIds();byte[] id=new byte[16];
    for(int i=0;i<=20000;i++)bind(index,Integer.toString(i),id);
    assertNull(index.get("0"));assertNull(index.get("20000"));
  }
}
