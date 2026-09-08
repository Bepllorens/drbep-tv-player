package com.drbep.tvplayer;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class PlaybackIssueClassifierTest {
    @Test
    public void classifiesOperationalFailuresWithoutUiDependencies() {
        assertEquals(PlaybackIssueClassifier.IssueType.AUTH, PlaybackIssueClassifier.classify("HTTP 403 token expired"));
        assertEquals(PlaybackIssueClassifier.IssueType.LICENSE, PlaybackIssueClassifier.classify("Widevine license failed"));
        assertEquals(PlaybackIssueClassifier.IssueType.DECODER, PlaybackIssueClassifier.classify("MediaCodec decoder init"));
        assertEquals(PlaybackIssueClassifier.IssueType.MANIFEST, PlaybackIssueClassifier.classify("MPD returned 404"));
        assertEquals(PlaybackIssueClassifier.IssueType.NETWORK, PlaybackIssueClassifier.classify("DNS connection timeout"));
        assertEquals(PlaybackIssueClassifier.IssueType.SERVER, PlaybackIssueClassifier.classify("upstream 502"));
        assertEquals(PlaybackIssueClassifier.IssueType.UNKNOWN, PlaybackIssueClassifier.classify("unexpected playback failure"));
    }

    @Test
    public void preservesGuidedRecommendationPrecedence() {
        assertEquals(
                PlaybackIssueClassifier.Recommendation.REACTIVATE,
                PlaybackIssueClassifier.recommend("proxy", PlaybackModeStore.MODE_PROXY, "403 session expired")
        );
        assertEquals(
                PlaybackIssueClassifier.Recommendation.LICENSE,
                PlaybackIssueClassifier.recommend("direct", PlaybackModeStore.MODE_DIRECT, "DRM license error")
        );
        assertEquals(
                PlaybackIssueClassifier.Recommendation.DIRECT,
                PlaybackIssueClassifier.recommend("proxy drm", PlaybackModeStore.MODE_PROXY, "upstream failed")
        );
        assertEquals(
                PlaybackIssueClassifier.Recommendation.PROXY,
                PlaybackIssueClassifier.recommend("direct", PlaybackModeStore.MODE_DIRECT, "bad mime")
        );
        assertEquals(
                PlaybackIssueClassifier.Recommendation.AUTO,
                PlaybackIssueClassifier.recommend("", PlaybackModeStore.MODE_AUTO, "")
        );
    }
}
