package com.drbep.tvplayer;

/** Numeric timing tied to one explicit request and its actual media-item attempt. */
final class RemoteZapTrace {
    static final class Sample {
        final long operation, pressToFrameMs, tuneToFrameMs;
        final int generation;
        Sample(long op, long press, long tune, int gen) {
            operation=op; pressToFrameMs=press; tuneToFrameMs=tune; generation=gen;
        }
    }
    private long pendingOperation, pendingPress, pendingTune;
    private String pendingChannel;
    private long operation, press, tune;
    private int generation = -1;
    synchronized void requested(long id, long pressedAt, long tunedAt, String channel) {
        cancel();
        pendingOperation=id; pendingPress=pressedAt; pendingTune=tunedAt; pendingChannel=channel;
    }
    synchronized void discardUnclaimed() { pendingChannel=null; pendingOperation=0; }
    synchronized void bind(String channel, int attempt) {
        generation=-1;
        if (pendingChannel != null && pendingChannel.equals(channel) && pendingOperation>0) {
            operation=pendingOperation;press=pendingPress;tune=pendingTune;generation=attempt;
        }
        discardUnclaimed();
    }
    synchronized Sample frame(int attempt,long now) {
        if (generation<0 || generation!=attempt || now<tune || now<press) return null;
        Sample result=new Sample(operation,now-press,now-tune,generation);
        generation=-1;
        return result;
    }
    synchronized void cancel() { generation=-1; discardUnclaimed(); }
}
