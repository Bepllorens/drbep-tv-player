package com.drbep.tvplayer;

/** UI-thread, session-only destination. An old completion cannot consume a newer return. */
public final class LiveBrowseReturnState {
    public static final class Target {
        public final long request;
        public final String group, channel;
        private Target(long request, String group, String channel) {
            this.request=request; this.group=group; this.channel=channel;
        }
    }
    private long serial;
    private Target pending;
    public Target request(String group, String channel) {
        if (group==null || group.isEmpty() || channel==null || channel.isEmpty()) { clear(); return null; }
        return pending=new Target(++serial,group,channel);
    }
    public Target pending() { return pending; }
    public void complete(long request) { if (pending!=null && pending.request==request) pending=null; }
    public void clear() { serial++; pending=null; }
}
