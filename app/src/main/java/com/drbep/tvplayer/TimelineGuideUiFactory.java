package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.function.Consumer;

final class TimelineGuideUiFactory {
    interface Host {
        String text(int resId);
        List<MainActivity.TimelineVisibleBlock> visibleBlocks(MainActivity.TimelineChannelPrograms row);
        String programBlockTitle(EpgRepository.EpgProgram program, boolean scheduled);
        String programTimeLabel(EpgRepository.EpgProgram program);
        TimelineProgramDetailUiModel programDetail(ChannelItem channel, EpgRepository.EpgProgram program, boolean live, boolean scheduled);
        void focusEmpty(ChannelItem channel, long windowStartMs, Consumer<TimelineProgramDetailUiModel> renderDetail);
        void focusProgram(ChannelItem channel, long windowStartMs, int centerMinute, TimelineProgramDetailUiModel detail, Consumer<TimelineProgramDetailUiModel> renderDetail);
        void openProgramActions(ChannelItem channel, EpgRepository.EpgProgram program);
        void toggleRecording(ChannelItem channel, EpgRepository.EpgProgram program, boolean scheduled);
    }

    interface HeaderHost {
        String text(int resId);
        String text(int resId, Object... args);
        void now();
        void previous();
        void nextChannel();
        void next();
        void close();
    }

    private TimelineGuideUiFactory() {
    }

    static TimelineGuideRowsUiModel buildRows(
            List<MainActivity.TimelineChannelPrograms> rows,
            long windowStartMs,
            int labelWidth,
            int stripWidth,
            String anchorChannelId,
            boolean rememberFocusedCenter,
            int lastFocusedCenterMinute,
            Consumer<TimelineProgramDetailUiModel> renderTimelineProgramDetail,
            Host host
    ) {
        List<TimelineGuideRowUiModel> outputRows = new ArrayList<>();
        boolean[] preferredAssigned = new boolean[]{false};
        if (rows == null || host == null) {
            return new TimelineGuideRowsUiModel(outputRows);
        }
        for (MainActivity.TimelineChannelPrograms row : rows) {
            if (row == null || row.channel == null) {
                continue;
            }
            List<TimelineGuideBlockUiModel> blocks = buildBlocks(
                    row,
                    windowStartMs,
                    stripWidth,
                    anchorChannelId,
                    rememberFocusedCenter,
                    lastFocusedCenterMinute,
                    preferredAssigned,
                    renderTimelineProgramDetail,
                    host
            );
            outputRows.add(new TimelineGuideRowUiModel(row.channel.name, row.channel.logoUrl, labelWidth, blocks));
        }
        return new TimelineGuideRowsUiModel(outputRows);
    }

    static TimelineHeaderUiModel buildHeader(long windowStartMs, long windowEndMs, SimpleDateFormat dayFormat, SimpleDateFormat hourFormat, Runnable afterActionFocus, HeaderHost host) {
        String windowLabel = host.text(
                R.string.timeline_window_label,
                dayFormat.format(new Date(windowStartMs)),
                hourFormat.format(new Date(windowStartMs)),
                hourFormat.format(new Date(windowEndMs))
        );
        return new TimelineHeaderUiModel(
                host.text(R.string.title_timeline_guide),
                windowLabel,
                java.util.Arrays.asList(
                        new TimelineHeaderUiModel.TimelineHeaderActionUiModel(host.text(R.string.timeline_now_button), host::now, afterActionFocus),
                        new TimelineHeaderUiModel.TimelineHeaderActionUiModel(host.text(R.string.timeline_prev_button), host::previous, afterActionFocus),
                        new TimelineHeaderUiModel.TimelineHeaderActionUiModel(host.text(R.string.timeline_channel_next_button), host::nextChannel, afterActionFocus),
                        new TimelineHeaderUiModel.TimelineHeaderActionUiModel(host.text(R.string.timeline_next_button), host::next, afterActionFocus),
                        new TimelineHeaderUiModel.TimelineHeaderActionUiModel(host.text(R.string.dialog_close), host::close, afterActionFocus)
                )
        );
    }

    static TimelineScaleUiModel buildScale(long windowStartMs, int labelWidth, int stripWidth, long windowMs, SimpleDateFormat hourFormat) {
        int totalWindowMinutes = (int) (windowMs / 60000L);
        int headerSlotMinutes = windowMs >= 6L * 60L * 60L * 1000L ? 60 : 30;
        int headerSlotCount = Math.max(1, totalWindowMinutes / headerSlotMinutes);
        int headerSlotWidth = stripWidth / headerSlotCount;
        List<TimelineScaleSlotUiModel> scaleSlots = new ArrayList<>();
        for (int i = 0; i < headerSlotCount; i++) {
            long slotStartMs = windowStartMs + (i * headerSlotMinutes * 60L * 1000L);
            scaleSlots.add(new TimelineScaleSlotUiModel(
                    hourFormat.format(new Date(slotStartMs)),
                    i % 2 == 0 ? 0xFFA7D0FF : 0xFF6F92B8,
                    headerSlotWidth
            ));
        }
        return new TimelineScaleUiModel(labelWidth, scaleSlots);
    }

    private static List<TimelineGuideBlockUiModel> buildBlocks(
            MainActivity.TimelineChannelPrograms row,
            long windowStartMs,
            int stripWidth,
            String anchorChannelId,
            boolean rememberFocusedCenter,
            int lastFocusedCenterMinute,
            boolean[] preferredAssigned,
            Consumer<TimelineProgramDetailUiModel> renderTimelineProgramDetail,
            Host host
    ) {
        List<TimelineGuideBlockUiModel> blocks = new ArrayList<>();
        List<MainActivity.TimelineVisibleBlock> visibleBlocks = host.visibleBlocks(row);
        if (visibleBlocks == null || visibleBlocks.isEmpty()) {
            boolean preferred = !preferredAssigned[0] && anchorChannelId != null && anchorChannelId.equals(row.channel.id);
            if (preferred) {
                preferredAssigned[0] = true;
            }
            blocks.add(new TimelineGuideBlockUiModel(
                    host.text(R.string.timeline_no_epg),
                    "",
                    "",
                    0,
                    stripWidth,
                    true,
                    false,
                    false,
                    preferred,
                    () -> host.focusEmpty(row.channel, windowStartMs, renderTimelineProgramDetail),
                    null,
                    null
            ));
            return blocks;
        }

        MainActivity.TimelineVisibleBlock rememberedCandidate = findRememberedCandidate(
                row,
                visibleBlocks,
                anchorChannelId,
                rememberFocusedCenter,
                lastFocusedCenterMinute
        );
        for (MainActivity.TimelineVisibleBlock visibleBlock : visibleBlocks) {
            EpgRepository.EpgProgram program = visibleBlock.program;
            boolean scheduled = visibleBlock.scheduled;
            boolean live = visibleBlock.live;
            boolean anchorMatch = anchorChannelId != null && anchorChannelId.equals(row.channel.id);
            boolean preferred = false;
            if (!preferredAssigned[0] && anchorMatch && visibleBlock.activeNow) {
                preferred = true;
            } else if (!preferredAssigned[0] && anchorMatch && visibleBlock == rememberedCandidate) {
                preferred = true;
            } else if (!preferredAssigned[0] && anchorMatch) {
                preferred = true;
            }
            if (preferred) {
                preferredAssigned[0] = true;
            }
            final int centerMinute = visibleBlock.centerMinute;
            blocks.add(new TimelineGuideBlockUiModel(
                    host.programBlockTitle(program, scheduled),
                    host.programTimeLabel(program),
                    scheduled ? host.text(R.string.timeline_program_scheduled_short) : live ? host.text(R.string.guide_program_now) : "",
                    visibleBlock.spacerWidth,
                    visibleBlock.blockWidth,
                    false,
                    live,
                    scheduled,
                    preferred,
                    () -> host.focusProgram(row.channel, windowStartMs, centerMinute, host.programDetail(row.channel, program, live, scheduled), renderTimelineProgramDetail),
                    () -> host.openProgramActions(row.channel, program),
                    () -> host.toggleRecording(row.channel, program, scheduled)
            ));
        }
        return blocks;
    }

    private static MainActivity.TimelineVisibleBlock findRememberedCandidate(
            MainActivity.TimelineChannelPrograms row,
            List<MainActivity.TimelineVisibleBlock> visibleBlocks,
            String anchorChannelId,
            boolean rememberFocusedCenter,
            int lastFocusedCenterMinute
    ) {
        int bestRememberedDelta = Integer.MAX_VALUE;
        MainActivity.TimelineVisibleBlock rememberedCandidate = null;
        for (MainActivity.TimelineVisibleBlock visibleBlock : visibleBlocks) {
            if (anchorChannelId != null && anchorChannelId.equals(row.channel.id) && rememberFocusedCenter) {
                int delta = Math.abs(visibleBlock.centerMinute - lastFocusedCenterMinute);
                if (delta < bestRememberedDelta) {
                    bestRememberedDelta = delta;
                    rememberedCandidate = visibleBlock;
                }
            }
        }
        return rememberedCandidate;
    }
}
