package com.drbep.tvplayer;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import java.io.*;
import java.nio.file.Files;
import static org.junit.Assert.*;
public class SvgCachePrunerTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder();
 private File file(String name,long age)throws Exception {
  File f=temp.newFile(name);Files.write(f.toPath(),new byte[100]);
  assertTrue(f.setLastModified(System.currentTimeMillis()-age));return f;
 }
 @Test public void underBudgetDoesNotDelete()throws Exception {
  File f=file("old.svg",120000);
  assertEquals(0,SvgCachePruner.prune(temp.getRoot(),100,32,System.currentTimeMillis()));
  assertTrue(f.exists());
 }
 @Test public void deletesOldestWithBoundedPasses()throws Exception {
  File old=file("old.svg",180000), newer=file("new.svg",120000);
  assertEquals(1,SvgCachePruner.prune(temp.getRoot(),0,1,System.currentTimeMillis()));
  assertFalse(old.exists());assertTrue(newer.exists());
  assertEquals(1,SvgCachePruner.prune(temp.getRoot(),0,1,System.currentTimeMillis()));
 }
 @Test public void preservesFreshWritesAndOtherFiles()throws Exception {
  File fresh=file("fresh.svg",0), tmp=file("write.tmp",180000), json=file("catalog.json",180000);
  File old=file("old.svg",180000);
  assertEquals(1,SvgCachePruner.prune(temp.getRoot(),0,32,System.currentTimeMillis()));
  assertFalse(old.exists());assertTrue(fresh.exists());assertTrue(tmp.exists());assertTrue(json.exists());
 }
 @Test public void missingDirectoryIsHarmless() {
  assertEquals(0,SvgCachePruner.prune(new File(temp.getRoot(),"missing"),0,32,System.currentTimeMillis()));
 }
}
