"""Regression guards for the lightweight home preview."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
A = '{http://schemas.android.com/apk/res/android}'
APP = '{http://schemas.android.com/apk/res-auto}'


class StartupPreviewTest(unittest.TestCase):
    def test_preview_keeps_video_and_captions_without_controls(self):
        layout = ROOT / 'app/src/main/res/layout'
        player = ET.parse(layout / 'startup_preview_player.xml').getroot()
        self.assertEqual(player.get(APP + 'use_controller'), 'false')
        self.assertEqual(player.get(APP + 'player_layout_id'), '@layout/startup_preview_content')
        self.assertIn(player.get(APP + 'surface_type'), (None, 'surface_view'))
        content = ET.parse(layout / 'startup_preview_content.xml').getroot()
        ids = {node.get(A + 'id') for node in content.iter()}
        self.assertTrue({'@id/exo_content_frame', '@id/exo_shutter', '@id/exo_subtitles'} <= ids)
        self.assertNotIn('@id/exo_controller_placeholder', ids)
        self.assertNotIn('@id/exo_buffering', ids)

    def test_artwork_survives_video_insertion(self):
        source = (ROOT / 'app/src/main/java/com/drbep/tvplayer/StartupHomeHubComposeBinder.kt').read_text()
        self.assertIn('findViewById<ImageView>(R.id.startup_home_artwork)', source)
        self.assertNotIn('findViewWithTag', source)
        self.assertIn('image.setTag(R.id.startup_home_artwork_request, artworkRequest)', source)
        self.assertIn('image.getTag(R.id.startup_home_artwork_request) === artworkRequest', source)
        self.assertIn('bindPoster(image, item.imageUrl, width.value.toInt(), if (compact) 100 else 112)', source)
        self.assertNotIn('getChildAt(0) as ImageView', source)
        self.assertNotIn('DRBEP-HOME-PERF', source)


if __name__ == '__main__':
    unittest.main()
