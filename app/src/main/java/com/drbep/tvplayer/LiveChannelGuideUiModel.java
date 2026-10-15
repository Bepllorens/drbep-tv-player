package com.drbep.tvplayer;
import java.util.*;
public final class LiveChannelGuideUiModel {
 public final String channel,message;public final List<Row> rows;
 public LiveChannelGuideUiModel(String channel,String message,List<Row> rows){this.channel=channel;this.message=message;this.rows=rows;}
 public static final class Row {
  public ProgrammeMetadataUiModel programmeMetadata;
  public final String day,time,title,description;public final float progress;public final boolean current;
  public Row(String day,String time,String title,String description,float progress,boolean current){this.day=day;this.time=time;this.title=title==null?"":title;this.description=description==null?"":description;this.progress=progress;this.current=current;}
 }
}
