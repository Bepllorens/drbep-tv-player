package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
public class LiveBrowseReturnStateTest {
 @Test public void olderCompletionDoesNotConsumeNewChannel() {
  LiveBrowseReturnState s=new LiveBrowseReturnState();
  LiveBrowseReturnState.Target a=s.request("a","1"), b=s.request("b","2");
  s.complete(a.request); assertSame(b,s.pending());
  s.complete(b.request); assertNull(s.pending());
 }
 @Test public void deliberateBrowseCancelsRestore() {
  LiveBrowseReturnState s=new LiveBrowseReturnState(); s.request("a","1");
  s.clear(); assertNull(s.pending());
 }
 @Test public void destinationIsImmutableAndInvalidTargetClearsIt() {
  LiveBrowseReturnState s=new LiveBrowseReturnState();
  LiveBrowseReturnState.Target a=s.request("a","1"); s.request("b","2");
  assertEquals("a",a.group); assertEquals("1",a.channel);
  s.request("","3"); assertNull(s.pending());
 }
}
