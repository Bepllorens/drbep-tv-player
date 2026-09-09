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

This increment uses the existing text-based remote-control options panel. It does
not yet show poster artwork, select seasons separately, or play private content.
Do not promote this beta as a complete HBO playback integration.

Validation: debug unit tests, release Java compilation, release assembly and vital
lint. Added tests for discarded late completion and redacted denied-access errors.
Not yet installed or visually tested on a Fire Stick. Verify the full path, search,
episode context, next/back and cancel on device before declaring the pilot validated.
