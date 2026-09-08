from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]


class RecordingStartChoiceTest(unittest.TestCase):
    def test_common_entry_checks_permission_before_choice(self):
        source = (ROOT / 'app/src/main/java/com/drbep/tvplayer/MainActivity.java').read_text()
        body = source.split('private void scheduleProgram(ChannelItem ch, EpgRepository.EpgProgram program) {', 1)[1].split('private void scheduleProgram(', 1)[0]
        self.assertLess(body.index('!canScheduleRecordings()'), body.index('showRecordingStartChoiceDialog'))
        self.assertIn('canRecordCurrentProgramFromBeginning(ch, program)', body)
        self.assertIn('scheduleProgram(ch, program, "remaining")', body)

    def test_choice_actions_do_not_reenter_the_prompt(self):
        source = (ROOT / 'app/src/main/java/com/drbep/tvplayer/MainActivity.java').read_text()
        body = source.split('private void showRecordingStartChoiceDialog(', 1)[1].split('private void createScheduleFromEndpoint(', 1)[0]
        self.assertIn('scheduleProgram(channel, program, "beginning")', body)
        self.assertIn('scheduleProgram(channel, program, "remaining")', body)
        self.assertIn('R.string.dialog_cancel), false, null', body)
