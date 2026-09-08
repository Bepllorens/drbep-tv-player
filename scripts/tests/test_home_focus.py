"""Guard focus reads against accidentally reintroducing card recomposition."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]


class HomeFocusTest(unittest.TestCase):
    def test_cards_defer_focus_to_draw_and_layer(self):
        source = (ROOT / 'app/src/main/java/com/drbep/tvplayer/StartupHomeHubComposeBinder.kt').read_text()
        for name in ('StartupPrimaryCard', 'StartupContinueCard'):
            body = source.split('private fun ' + name + '(', 1)[1].split('\n@Composable', 1)[0]
            self.assertIn('val focused = remember { mutableStateOf(false) }', body)
            self.assertIn('.graphicsLayer {', body)
            self.assertIn('.startupFocusBorder({ focused.value }', body)
            self.assertNotIn('.border(if (focused)', body)
            self.assertNotIn('.scale(if (focused)', body)
            self.assertIn('.clickable(enabled = item.onClick != null)', body)
        self.assertIn('drawWithContent {\n        drawContent()\n        val active = isFocused()', source)


if __name__ == '__main__':
    unittest.main()
