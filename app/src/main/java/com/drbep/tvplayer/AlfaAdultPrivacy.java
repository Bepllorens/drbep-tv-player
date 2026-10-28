package com.drbep.tvplayer;
final class AlfaAdultPrivacy {
 static boolean privateID(String id){return id!=null&&id.startsWith("alfa-adult:");}
 static java.util.Map<String,String> mediaHeaders(java.util.Map<String,String> input){
  java.util.Map<String,String> safe=new java.util.HashMap<>();
  if(input==null)return safe;
  for(java.util.Map.Entry<String,String> e:input.entrySet()){
   String key=e.getKey(),value=e.getValue();
   if(key!=null&&value!=null&&("referer".equalsIgnoreCase(key)||"user-agent".equalsIgnoreCase(key)||("origin".equalsIgnoreCase(key)&&"https://chaturbate.com".equals(value)))&&value.length()<=4096&&value.indexOf(13)<0&&value.indexOf(10)<0)safe.put(key,value);
  }
  return safe;
 }
}
