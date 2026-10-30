package com.drbep.tvplayer;
import java.io.*;import java.nio.file.*;import java.util.concurrent.atomic.AtomicLong;
import org.json.JSONObject;import org.junit.Test;import org.junit.Rule;
import org.junit.rules.TemporaryFolder;import static org.junit.Assert.*;
public class ImageCacheReportTest {
 @Rule public TemporaryFolder temp=new TemporaryFolder();
 @Test public void reportsOnlyFixedDirectoriesAndReusesSnapshotWithoutDeleting()throws Exception {
  File dir=temp.newFolder("channel-svg-v1"),logo=new File(dir,"private-name.svg");
  Files.write(logo.toPath(),new byte[23]);Files.write(temp.newFile("unrelated").toPath(),new byte[100]);
  AtomicLong now=new AtomicLong(1);ImageCacheReport report=new ImageCacheReport(temp.getRoot(),now::get);
  String first=report.read(-1);JSONObject a=new JSONObject(first);
  assertTrue(a.isNull("glide_budget_bytes"));assertEquals(23,a.getJSONObject("svg").getLong("bytes"));
  assertFalse(first.contains("private-name"));assertFalse(first.contains(temp.getRoot().toString()));
  Files.write(logo.toPath(),new byte[50]);
  assertEquals(first,report.read(128));
  now.addAndGet(30_000_000_000L);
  JSONObject b=new JSONObject(report.read(128));assertEquals(50,b.getJSONObject("svg").getLong("bytes"));
  assertEquals(128,b.getLong("glide_budget_bytes"));assertTrue(logo.exists());
 }
 @Test public void ignoresSymlinksAndNestedDirectories()throws Exception {
  File dir=temp.newFolder("channel-svg-v1"),outside=temp.newFile("secret");
  Files.write(outside.toPath(),new byte[900]);
  Files.createSymbolicLink(new File(dir,"link.svg").toPath(),outside.toPath());
  File nested=new File(dir,"nested");assertTrue(nested.mkdir());Files.write(new File(nested,"x").toPath(),new byte[50]);
  JSONObject a=new JSONObject(new ImageCacheReport(temp.getRoot()).read(64));
  assertEquals(0,a.getJSONObject("svg").getLong("bytes"));assertTrue(outside.exists());
 }
 @Test public void missingCachesAreZero()throws Exception {
  JSONObject a=new JSONObject(new ImageCacheReport(temp.getRoot()).read(32));
  assertTrue(a.getJSONObject("glide").getBoolean("complete"));
  assertEquals(0,a.getJSONObject("glide").getLong("bytes"));
 }
}
