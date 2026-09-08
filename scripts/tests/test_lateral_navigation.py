"""Regression guard: cursor movement must not reopen an already visible panel."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]


class LateralNavigationTest(unittest.TestCase):
    def test_cursor_fast_path_preserves_open_and_timeout_paths(self):
        source = (ROOT / 'app/src/main/java/com/drbep/tvplayer/MainActivity.java').read_text()
        body = source.split('private void moveOverlaySelection(int delta) {', 1)[1].split('\n    private ', 1)[0]
        self.assertIn('channelOverlayCoordinator.moveOverlaySelection(delta)', body)
        self.assertIn('getSelectedOverlayIndex()', body)
        self.assertIn('scrollOverlayChannelListToPosition(', body)
        self.assertNotIn('syncOverlayStateFromCoordinator()', body)
        self.assertNotIn('persistNavigationState()', body)
        self.assertIn('channelOverlay.getVisibility() == View.VISIBLE', body)
        self.assertIn('touchDeviceMode ? 0L : OVERLAY_HIDE_MS', body)
        self.assertIn('} else {\n            showOverlay();', body)


if __name__ == '__main__':
    unittest.main()
