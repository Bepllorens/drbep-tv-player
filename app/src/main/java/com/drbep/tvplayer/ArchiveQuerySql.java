package com.drbep.tvplayer;

/** Metadata-only query alternatives; no playback or account credentials. */
final class ArchiveQuerySql {
    private ArchiveQuerySql() {}
    static final String GENRES_ALL = "SELECT DISTINCT genre FROM programs WHERE scope=? AND genre<>'' ORDER BY genre";
    // Bound the indexed walk for catalogues with unexpectedly many distinct categories.
    static final String GENRES_SEEK = "WITH RECURSIVE g(genre) AS (SELECT MIN(genre) FROM programs WHERE scope=? AND genre>'' UNION ALL SELECT (SELECT MIN(genre) FROM programs WHERE scope=? AND genre>g.genre) FROM g WHERE genre IS NOT NULL LIMIT 257) SELECT genre FROM g WHERE genre IS NOT NULL ORDER BY genre";
    static final String SEARCH_INDEX = "CREATE INDEX IF NOT EXISTS programs_search ON programs(scope,day,start DESC,id,channel,end,genre,search)";
}
