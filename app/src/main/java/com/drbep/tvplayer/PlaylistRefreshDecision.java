package com.drbep.tvplayer;
import java.time.ZoneId;
/** Shared decision for scheduled delivery, independent of WorkManager and Android. */
final class PlaylistRefreshDecision {
 static boolean due(String encoded, boolean automatic, boolean refreshable,
                    long updated, long handledSlot, long now, ZoneId zone, boolean busy) {
  if(!automatic || !refreshable || busy || encoded==null || encoded.isEmpty())return false;
  try {return PlaylistRefreshSchedule.decode(encoded).due(Math.max(updated,handledSlot),now,zone);}
  catch(IllegalArgumentException invalid){return false;}
 }
}
