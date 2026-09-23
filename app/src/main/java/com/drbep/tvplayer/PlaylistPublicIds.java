package com.drbep.tvplayer;

import androidx.media3.common.util.UriUtil;
import androidx.media3.exoplayer.hls.playlist.*;
import androidx.media3.exoplayer.upstream.ParsingLoadable;
import java.util.*;

/** Per-probe public metadata index. No URLs or identifiers are logged. */
final class PlaylistPublicIds implements HlsPlaylistParserFactory {
  private final HlsPlaylistParserFactory delegate=new DefaultHlsPlaylistParserFactory();
  private final Map<String,byte[]> ids=new HashMap<>();
  private boolean overflow;
  synchronized byte[] get(String uri) {
    byte[] id=overflow?null:ids.get(uri);
    return id==null?null:id.clone();
  }
  private void bind(String uri,byte[] id) {
    if(ids.containsKey(uri)&&!Arrays.equals(ids.get(uri),id)) ids.put(uri,null);
    else if(!ids.containsKey(uri)) ids.put(uri,id);
    if(ids.size()>20000){overflow=true;ids.clear();}
  }
  private synchronized void observe(HlsPlaylist playlist) {
    if(overflow||!(playlist instanceof HlsMediaPlaylist))return;
    HlsMediaPlaylist media=(HlsMediaPlaylist)playlist;
    byte[] unique=null;boolean ambiguous=!media.hasEndTag;
    // This diagnostic intentionally declines key rotation: extractors can be
    // reused between segments. Never keep a stale fallback after rotation.
    for(HlsMediaPlaylist.Segment segment:media.segments) {
      if(segment.drmInitData==null)continue;
      byte[] id=PublicInitId.single(segment.drmInitData);
      if(id==null||(unique!=null&&!Arrays.equals(unique,id))){ambiguous=true;break;}
      unique=id;
    }
    for(HlsMediaPlaylist.Segment segment:media.segments) {
      byte[] id=ambiguous||segment.drmInitData==null?null:unique;
      bind(UriUtil.resolve(media.baseUri,segment.url),id);
      if(segment.initializationSegment!=null)
        bind(UriUtil.resolve(media.baseUri,segment.initializationSegment.url),id);
      if(overflow)break;
    }
  }
  private ParsingLoadable.Parser<HlsPlaylist> wrap(ParsingLoadable.Parser<HlsPlaylist> parser) {
    return (uri,input)->{HlsPlaylist result=parser.parse(uri,input);observe(result);return result;};
  }
  @Override public ParsingLoadable.Parser<HlsPlaylist> createPlaylistParser() {
    return wrap(delegate.createPlaylistParser());
  }
  @Override public ParsingLoadable.Parser<HlsPlaylist> createPlaylistParser(
      HlsMultivariantPlaylist master,HlsMediaPlaylist previous) {
    return wrap(delegate.createPlaylistParser(master,previous));
  }
}
