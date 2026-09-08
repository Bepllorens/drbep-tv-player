from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]


class HomeLocalFirstTest(unittest.TestCase):
    def test_local_render_precedes_network(self):
        source = (ROOT / 'app/src/main/java/com/drbep/tvplayer/MainActivity.java').read_text()
        body = source.split('private void loadStartupHubStateAndShow() {', 1)[1].split('private List<ChannelItem> resolveStartupContinueVodItems()', 1)[0]
        self.assertLess(body.index('showStartupHubDialog(localState)'), body.index('ioExecutor.execute'))
        self.assertIn('startupSummaryGate.accepts(summaryGeneration)', body)
        self.assertNotIn('showStartupHubDialog(state)', body)
        self.assertIn('!shouldHideProtectedItem(item)', body)

    def test_refresh_does_not_replace_composition(self):
        source = (ROOT / 'app/src/main/java/com/drbep/tvplayer/StartupHomeHubComposeBinder.kt').read_text()
        body = source.split('fun update(', 1)[1].split('@JvmStatic', 1)[0]
        self.assertNotIn('setContent', body)
        self.assertIn('state?.value = model', body)
        self.assertIn('LaunchedEffect(Unit)', source)


if __name__ == '__main__':
    unittest.main()
