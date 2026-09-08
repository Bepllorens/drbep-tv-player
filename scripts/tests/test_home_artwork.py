from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]


class HomeArtworkTest(unittest.TestCase):
    def test_placeholder_for_loading_and_failure(self):
        source = (ROOT / 'app/src/main/java/com/drbep/tvplayer/HomePosterLoader.java').read_text()
        self.assertIn('.placeholder(R.drawable.home_artwork_placeholder)', source)
        self.assertIn('.error(R.drawable.home_artwork_placeholder)', source)
        self.assertIn('.diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)', source)

    def test_diagnostics_do_not_log_models_or_exceptions(self):
        source = (ROOT / 'app/src/main/java/com/drbep/tvplayer/HomePosterLoader.java').read_text()
        self.assertEqual(source.count('Log.println(Log.INFO,'), 2)
        for block in source.split('Log.println(')[1:]:
            statement = block.split(';', 1)[0]
            self.assertNotIn('ignoredModel', statement)
            self.assertNotIn('error', statement)
            self.assertNotIn('resource', statement)
        self.assertIn('HomePosterLoader.bind(imageView, authenticatedPosterModel(posterUrl)',
                      (ROOT / 'app/src/main/java/com/drbep/tvplayer/MainActivity.java').read_text())
