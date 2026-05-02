package com.drbep.tvplayer;

final class RecordingsController {
    static final class SummaryStats {
        final int total;
        final int scheduled;
        final int recording;
        final int issue;
        final int conflict;

        SummaryStats(int total, int scheduled, int recording, int issue, int conflict) {
            this.total = total;
            this.scheduled = scheduled;
            this.recording = recording;
            this.issue = issue;
            this.conflict = conflict;
        }
    }

    private RecordingsRepository.RecordingsResult currentResult;
    private int selectedIndex;
    private boolean scheduledMode;
    private String lastSelectedId;

    boolean isScheduledMode() {
        return scheduledMode;
    }

    String getLastSelectedId() {
        return lastSelectedId;
    }

    RecordingsRepository.RecordingsResult getCurrentResult() {
        return currentResult;
    }

    int getSelectedIndex() {
        normalizeSelectedIndex();
        return selectedIndex;
    }

    void setScheduledMode(boolean scheduledMode) {
        this.scheduledMode = scheduledMode;
    }

    void applyResult(RecordingsRepository.RecordingsResult result, String preferredId) {
        currentResult = result;
        scheduledMode = result != null && result.scheduledMode;
        selectedIndex = findPreferredIndex(result, preferredId);
        lastSelectedId = preferredId;
    }

    void clearCurrentResult() {
        RecordingsRepository.RecordingItem selected = getSelectedItem();
        if (selected != null) {
            lastSelectedId = selected.id;
        }
        currentResult = null;
        selectedIndex = 0;
    }

    RecordingsRepository.RecordingItem getSelectedItem() {
        if (currentResult == null || currentResult.items == null || currentResult.items.isEmpty()) {
            return null;
        }
        normalizeSelectedIndex();
        return currentResult.items.get(selectedIndex);
    }

    RecordingsRepository.RecordingItem selectIndex(int index) {
        if (currentResult == null || currentResult.items == null || currentResult.items.isEmpty()) {
            selectedIndex = 0;
            return null;
        }
        selectedIndex = Math.max(0, Math.min(index, currentResult.items.size() - 1));
        return currentResult.items.get(selectedIndex);
    }

    RecordingsRepository.RecordingItem moveSelection(int delta) {
        if (currentResult == null || currentResult.items == null || currentResult.items.isEmpty()) {
            return null;
        }
        selectedIndex += delta;
        if (selectedIndex < 0) {
            selectedIndex = currentResult.items.size() - 1;
        }
        if (selectedIndex >= currentResult.items.size()) {
            selectedIndex = 0;
        }
        return currentResult.items.get(selectedIndex);
    }

    SummaryStats buildSummaryStats() {
        if (currentResult == null) {
            return new SummaryStats(0, 0, 0, 0, 0);
        }
        return buildSummaryStats(currentResult);
    }

    static SummaryStats buildSummaryStats(RecordingsRepository.RecordingsResult result) {
        if (result == null || result.items == null || result.items.isEmpty()) {
            return new SummaryStats(0, 0, 0, 0, 0);
        }
        if (!result.scheduledMode) {
            return new SummaryStats(result.items.size(), 0, 0, 0, 0);
        }
        int scheduled = 0;
        int recording = 0;
        int issue = 0;
        for (RecordingsRepository.RecordingItem item : result.items) {
            String status = item == null ? "" : safeLower(item.status);
            switch (status) {
                case "recording":
                case "running":
                case "in_progress":
                    recording++;
                    break;
                case "failed":
                case "error":
                case "cancelled":
                case "canceled":
                    issue++;
                    break;
                default:
                    scheduled++;
                    break;
            }
        }
        return new SummaryStats(result.items.size(), scheduled, recording, issue, countConflicts(result));
    }

    static int countConflicts(RecordingsRepository.RecordingsResult result) {
        if (result == null || result.items == null || !result.scheduledMode) {
            return 0;
        }
        int conflicts = 0;
        for (int i = 0; i < result.items.size(); i++) {
            RecordingsRepository.RecordingItem a = result.items.get(i);
            long aStart = parseIsoMillis(a == null ? null : a.startTime);
            long aEnd = parseIsoMillis(a == null ? null : a.endTime);
            if (aStart <= 0L || aEnd <= aStart) {
                continue;
            }
            for (int j = i + 1; j < result.items.size(); j++) {
                RecordingsRepository.RecordingItem b = result.items.get(j);
                long bStart = parseIsoMillis(b == null ? null : b.startTime);
                long bEnd = parseIsoMillis(b == null ? null : b.endTime);
                if (bStart <= 0L || bEnd <= bStart) {
                    continue;
                }
                if (aStart < bEnd && bStart < aEnd) {
                    conflicts++;
                    break;
                }
            }
        }
        return conflicts;
    }

    private void normalizeSelectedIndex() {
        if (currentResult == null || currentResult.items == null || currentResult.items.isEmpty()) {
            selectedIndex = 0;
            return;
        }
        if (selectedIndex < 0 || selectedIndex >= currentResult.items.size()) {
            selectedIndex = 0;
        }
    }

    private static int findPreferredIndex(RecordingsRepository.RecordingsResult result, String preferredId) {
        if (result == null || result.items == null || result.items.isEmpty() || preferredId == null || preferredId.trim().isEmpty()) {
            return 0;
        }
        for (int i = 0; i < result.items.size(); i++) {
            RecordingsRepository.RecordingItem item = result.items.get(i);
            if (item != null && preferredId.equals(item.id)) {
                return i;
            }
        }
        return 0;
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private static long parseIsoMillis(String iso) {
        if (iso == null || iso.trim().isEmpty()) {
            return 0L;
        }
        String value = iso.trim();
        String[] patterns = new String[]{
                "yyyy-MM-dd'T'HH:mm:ssXXX",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss"
        };
        for (String pattern : patterns) {
            try {
                java.text.SimpleDateFormat format = new java.text.SimpleDateFormat(pattern, java.util.Locale.US);
                java.util.Date date = format.parse(value);
                if (date != null) {
                    return date.getTime();
                }
            } catch (Exception ignored) {
            }
        }
        return 0L;
    }
}
