package com.drbep.tvplayer;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class ArchiveSavedSnapshotTest {
 @Test public void lateGuideCannotRestoreRemovedProgram() {
  List<String> before=new ArchiveSavedSnapshot<>(0,Arrays.asList("programme"));
  List<String> after=new ArchiveSavedSnapshot<>(1,Collections.emptyList());
  assertFalse(ArchiveSavedSnapshot.current(before,1));
  assertTrue(ArchiveSavedSnapshot.current(after,1));
  assertTrue(after.isEmpty());
 }
 @Test public void snapshotDoesNotFollowMutableInput() {
  List<String> input=new ArrayList<>(Arrays.asList("programme"));
  List<String> snapshot=new ArchiveSavedSnapshot<>(2,input);
  input.clear();assertEquals(1,snapshot.size());
 }
 @Test public void unversionedAndAbsentStateCannotReplaceKnownState() {
  assertFalse(ArchiveSavedSnapshot.current(null,0));
  assertFalse(ArchiveSavedSnapshot.current(Collections.emptyList(),0));
 }
 @Test(expected=UnsupportedOperationException.class) public void snapshotCannotBeChanged() {
  new ArchiveSavedSnapshot<>(0,Arrays.asList("programme")).clear();
 }
}
