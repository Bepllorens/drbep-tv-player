package com.drbep.tvplayer;
import java.nio.*;import java.nio.charset.*;import java.util.*;
final class ArchiveText {
 static int damage(String s){int n=0;for(char c:s.toCharArray())if(c=='\u00c3'||c=='\u00c2'||c=='\ufffd'||(c>=128&&c<=159))n++;return n;}
 static String repair(String text){String s=text==null?"":text;for(int i=0;i<2&&damage(s)>0;i++)try{
  ByteBuffer bytes=StandardCharsets.ISO_8859_1.newEncoder().onUnmappableCharacter(CodingErrorAction.REPORT).encode(CharBuffer.wrap(s));
  String next=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(bytes).toString();
  if(damage(next)>=damage(s))break;s=next;
 }catch(CharacterCodingException e){break;}return s;}
 static String channel(String preferred,String official){String fixed=repair(preferred),other=repair(official);return damage(fixed)>0&&!other.isEmpty()&&damage(other)<damage(fixed)?other:fixed;}
 static String[] sports(String title,String category){String t=repair(title),c=repair(category);String lower=c.toLowerCase(Locale.ROOT);
  if(!(lower.contains("deport")||lower.contains("fútbol")||lower.contains("futbol")||lower.contains("sport")||lower.contains("baloncesto")))return new String[]{t,""};
  int split=t.indexOf(": ");if(split>0){String event=t.substring(split+2);if(event.matches(".+\\s(?:-|–|vs\\.?)\\s.+"))return new String[]{event,t.substring(0,split)};}
  return new String[]{t,""};
 }
}
