package com.drbep.tvplayer;

/** Bounded choice of group query. Large group catalogues retain the old plan. */
final class MetadataIndexGroups {
    static final int PROBE_LIMIT=33;
    private static final String CURRENT=" FROM entries e JOIN sources s ON s.id=e.source AND COALESCE(s.active,s.preview)=e.generation ";
    static final String LEGACY="SELECT DISTINCT e.group_name"+CURRENT+"LEFT JOIN preferences p ON p.source=e.source AND p.identity=e.identity AND p.occurrence=e.occurrence WHERE e.source=? AND e.kind IN ('movie','series') AND (?='' OR e.kind=?) AND e.group_name<>'' AND COALESCE(p.hidden,0)=0 ORDER BY e.group_name LIMIT 1001";
    private static String kind(String alias,String kind) {
        new MetadataIndexSelection("",kind);
        return alias+(kind.isEmpty()?".kind IN ('movie','series')":".kind=?");
    }
    static String probeSql(String kind) {
        return "SELECT DISTINCT e.group_name"+CURRENT+"WHERE e.source=? AND "+kind("e",kind)+" AND e.group_name<>'' ORDER BY e.group_name LIMIT "+PROBE_LIMIT;
    }
    static String[] probeArgs(String source,String kind) {
        return kind.isEmpty()?new String[]{source}:new String[]{source,kind};
    }
    static boolean grouped(int candidates) { return candidates<PROBE_LIMIT; }
    static String sql(String kind,boolean grouped) {
        if(!grouped)return LEGACY;
        return "SELECT c.group_name FROM (SELECT DISTINCT e.source,e.generation,e.group_name"+CURRENT+"WHERE e.source=? AND "+kind("e",kind)+" AND e.group_name<>'') c WHERE EXISTS (SELECT 1 FROM entries g LEFT JOIN preferences p ON p.source=g.source AND p.identity=g.identity AND p.occurrence=g.occurrence WHERE g.source=c.source AND g.generation=c.generation AND g.group_name=c.group_name AND "+kind("g",kind)+" AND COALESCE(p.hidden,0)=0) ORDER BY c.group_name LIMIT 1001";
    }
    static String[] args(String source,String kind,boolean grouped) {
        return !grouped||!kind.isEmpty()?new String[]{source,kind,kind}:new String[]{source};
    }
}
