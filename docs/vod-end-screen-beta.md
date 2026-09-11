# VOD end screen — beta 557

Actual playback completion now opens an action panel instead of leaving a frozen frame.
HBO episodes offer the next catalog episode when available; there is no autoplay.
All VOD panels provide return to catalog, live TV and restart. Back returns to catalog.
HBO browsing restores the page, query, season and selected card. A VOD opened outside
that browsing context falls back to the provider catalog, never an unrelated series.

Completion suppresses subsequent end-position writes. HBO explicitly records completion
even when media duration is missing; repeated completion does not replace the pending
next episode. Live TV, U7D and recording callbacks retain their existing behavior.

Changed files: MainActivity, PrivateVodBrowser, PrivateVodCardsBinder, HboWatchHistory,
HboWatchHistoryTest and app/build.gradle. Audio routing and quality were not changed.

Validation: 13 focused history/browser/message-panel unit tests passed. Signed release
build and lint completed. Device interaction still requires user approval before
interrupting an active Fire playback.

Device acceptance checklist:
- Finish a movie: cover, catalog/live/restart actions; no resume at the final position.
- Finish an HBO episode: next title and cover, explicit play; no autoplay.
- Finish the last episode: catalog is the first action.
- Back/catalog restores HBO search, season/page and selected card.
- A delayed/failed next-episode lookup cannot reopen the panel after leaving it.
- Confirm live TV and replay-from-start work and sound quality stays unchanged.
