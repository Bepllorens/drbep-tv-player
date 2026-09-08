"""Source invariants complement merged-manifest lint and real-device tests."""
from pathlib import Path
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
ANDROID = "{http://schemas.android.com/apk/res/android}"
APP = "{http://schemas.android.com/apk/res-auto}"


class StartupManifestLayoutTest(unittest.TestCase):
    def test_tv_launcher_and_artwork_remain_declared(self):
        root = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        activities = root.findall("application/activity")
        tv = [a for a in activities if any(
            c.get(ANDROID + "name") == "android.intent.category.LEANBACK_LAUNCHER"
            for c in a.findall("intent-filter/category"))]
        self.assertEqual(len(tv), 1)
        self.assertEqual(tv[0].get(ANDROID + "name"), ".TvLauncherActivity")
        self.assertEqual(tv[0].get(ANDROID + "exported"), "true")
        self.assertEqual(tv[0].get(ANDROID + "icon"), "@mipmap/drbep_launcher")
        self.assertEqual(tv[0].get(ANDROID + "banner"), "@drawable/drbep_launcher_banner")

    def test_touchscreen_and_leanback_stay_optional(self):
        root = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
        features = {f.get(ANDROID + "name"): f.get(ANDROID + "required")
                    for f in root.findall("uses-feature")}
        self.assertEqual(features["android.hardware.touchscreen"], "false")
        self.assertEqual(features["android.software.leanback"], "false")

    def test_multiview_is_lazy_with_original_ids_and_surface(self):
        root = ET.parse(ROOT / "app/src/main/res/layout/activity_main.xml").getroot()
        self.assertEqual(len(list(root.iter("androidx.media3.ui.PlayerView"))), 1)
        stubs = list(root.iter("ViewStub"))
        for slot in range(1, 5):
            stub = next(s for s in stubs if s.get(ANDROID + "id") == f"@+id/multiPlayerStub{slot}")
            self.assertEqual(stub.get(ANDROID + "inflatedId"), f"@+id/multiPlayerView{slot}")
            self.assertEqual(stub.get(ANDROID + "layout"), "@layout/multiview_player")
        player = ET.parse(ROOT / "app/src/main/res/layout/multiview_player.xml").getroot()
        self.assertEqual(player.tag, "androidx.media3.ui.PlayerView")
        self.assertEqual(player.get(APP + "surface_type"), "texture_view")


if __name__ == "__main__":
    unittest.main()
