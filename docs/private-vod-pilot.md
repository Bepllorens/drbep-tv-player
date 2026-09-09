# HBO metadata browser pilot — 544

Based on published stable/beta 543, commit 30f517f. Local macOS sources at version
489 are stale and were not used. Isolated branch: codex/private-vod-browser.

Beta builds expose **VOD > Vista lista > HBO Max · Catálogo en pruebas** through
the existing VOD menu. The server must opt the user into
DRBEP_PRIVATE_VOD_PREVIEW_USERS and require the ordinary VOD permission.
The entry is beta-only; it is not evidence that an account is enabled.

The browser fetches a descriptor and at most 40 rows per page, with remote search,
newest-first movies/series, and season/episode ordering for episode lists. Back
navigation retains series context and refetches the prior page. Only cursor history
is retained, not the entire catalog. Closing the browser or destroying the activity
cancels its pending Future and discards late results. It adds nothing to the offline
snapshot and does not preload at startup. Error messages never include raw URLs.

Version 544 uses the existing text-based remote-control options panel. It does
not show poster artwork, select seasons separately, or play private content.
Do not promote this beta as a complete HBO playback integration.

Validation: debug unit tests, release Java compilation, release assembly and vital
lint. Added tests for discarded late completion and redacted denied-access errors.
Not yet installed or visually tested on a Fire Stick. Verify the full path, search,
episode context, next/back and cancel on device before declaring the pilot validated.

## 545 — Compact cards

The user confirmed search and retained series context on Fire with 544. The next
beta replaces page results with a lazy adaptive grid of compact poster/synopsis
cards. Controls and detail navigation remain unchanged. Series titles remain in
the page header; the full synopsis is still available by opening a movie/episode.

Posters use only the authenticated backend endpoint and include the existing
device bearer credentials in headers, never in URLs. Glide decodes at 192x300,
fits without cropping, uses no disk/memory cache for these requests, and clears
requests when the view leaves composition. A neutral placeholder replaces missing
artwork. Only composed cards load images, not all 40 at once. Focusing a card uses
a blue border and contrasting background without changing its dimensions.

Compilation/unit tests passed; real-device appearance/performance still needs
verification after update. Playback remains disabled. No backend restart required.

## 546 — Owned modal lifecycle

545 reproduced on Fire 192.168.93.164: five app windows accumulated, and Back from
the regular VOD library exposed a stale private loading window. The private Host
now owns its dialog and dismisses it before any replacement (loading, result,
search, detail). Browser close/cancel also disposes its owned window; late request
callbacks are still invalidated. Generic options/input builders return their dialog
so ownership does not require dismissing unrelated app windows. Card Back uses the
existing modal transition guard. Unit test checks cancellation invokes dismissal.

546 installed with adb install -r on .164, retaining user data. Real-device checks:
40 movie cards, page 2, Back through HBO/menu/library to home, LA 2 displaying video.
ViewRootImpl stays at 2 in catalog and returns to 1 on TV (545 had 5). A second
visit searched Batman, returned eight cards with artwork, and retained two windows.
No FATAL EXCEPTION, ANR or OutOfMemoryError in the inspected logcat interval.
This confirms the tested navigation path, not an exhaustive performance benchmark.

## 547 — Season filter

Episode pages offer a season selector using server counts, plus All seasons.
Search and next/previous retain the selected season; selecting a season resets
the page cursor. Detail return retains it too. The series title stays in the
header. Existing modal ownership is unchanged. No backend restart or full snapshot
download. Playback remains a separate pending block.

547 installed on Fire .164 retaining app data. Real check: 2 Dope Queens shows
8 episodes across both seasons, then 4 in season 2. Returning from detail keeps
season 2 and the series header. Back through the catalog to LA 2 displays live TV;
window roots return from 2 to 1. No FATAL, ANR or OOM in the inspected log interval.
Season pagination preservation is implemented but not exercised with a >40 episode
season on device. Unit tests and signed release build passed.
