package com.drbep.tvplayer;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.Collections;
public class CatchupFailurePolicyTest {
 @Test public void onlyExplicitDenialStopsRetries() {
  assertTrue(CatchupFailurePolicy.isEntitlementDenial(403, Collections.singletonMap("x-drbep-playback-error",Collections.singletonList("catchup_not_entitled"))));
  assertFalse(CatchupFailurePolicy.isEntitlementDenial(403,Collections.emptyMap()));
  assertFalse(CatchupFailurePolicy.isEntitlementDenial(500,Collections.singletonMap("X-DRBEP-Playback-Error",Collections.singletonList("catchup_not_entitled"))));
  assertFalse(CatchupFailurePolicy.isEntitlementDenial(403,Collections.singletonMap("X-DRBEP-Playback-Error",Collections.singletonList("network_error"))));
 }
}
