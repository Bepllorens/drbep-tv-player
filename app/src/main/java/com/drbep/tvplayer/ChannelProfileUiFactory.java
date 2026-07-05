package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class ChannelProfileUiFactory {
    interface Host {
        String text(int resId);
        String displayName(ChannelItem item);
        boolean contains(ChannelCollectionStore.ChannelCollection collection, ChannelItem item);
        boolean hasAlias(ChannelItem item);
        boolean hasTag(ChannelItem item);
        boolean isHidden(ChannelItem item);
        void openPersonalLists(ChannelItem item, List<ChannelCollectionStore.ChannelCollection> collections, boolean[] checked);
        void savePersonalLists(ChannelItem item, List<ChannelCollectionStore.ChannelCollection> collections, boolean[] checked);
        void openAlias(ChannelItem item);
        void clearAlias(ChannelItem item);
        void openTag(ChannelItem item);
        void clearTag(ChannelItem item);
        void setHidden(ChannelItem item, boolean hidden);
        void setStartup(ChannelItem item);
        void openTemporaryPlaybackMode(ChannelItem item);
    }

    private ChannelProfileUiFactory() {
    }

    static TvOptionsMenuModel buildPersonalLists(ChannelItem item, List<ChannelCollectionStore.ChannelCollection> collections, boolean[] checked, boolean initialize, Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        if (item == null || collections == null || checked == null || host == null) {
            return new TvOptionsMenuModel(options, actions, "");
        }
        for (int i = 0; i < collections.size(); i++) {
            ChannelCollectionStore.ChannelCollection collection = collections.get(i);
            if (initialize) {
                checked[i] = host.contains(collection, item);
            }
            final int index = i;
            options.add((checked[i] ? "[x] " : "[ ] ") + (collection == null ? "" : collection.label));
            actions.add(() -> {
                checked[index] = !checked[index];
                host.openPersonalLists(item, collections, checked);
            });
        }
        options.add(host.text(android.R.string.ok));
        actions.add(() -> host.savePersonalLists(item, collections, checked));
        return new TvOptionsMenuModel(options, actions, host.displayName(item));
    }

    static TvOptionsMenuModel buildProfile(ChannelItem item, Host host) {
        List<String> options = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();
        if (item == null || host == null) {
            return new TvOptionsMenuModel(options, actions, "");
        }
        boolean hasAlias = host.hasAlias(item);
        boolean hasTag = host.hasTag(item);
        boolean hidden = host.isHidden(item);
        options.add(host.text(R.string.channel_profile_alias));
        actions.add(() -> host.openAlias(item));
        if (hasAlias) {
            options.add(host.text(R.string.channel_profile_clear_alias));
            actions.add(() -> host.clearAlias(item));
        }
        options.add(host.text(R.string.channel_profile_tag));
        actions.add(() -> host.openTag(item));
        if (hasTag) {
            options.add(host.text(R.string.channel_profile_clear_tag));
            actions.add(() -> host.clearTag(item));
        }
        options.add(host.text(hidden ? R.string.channel_profile_unhide : R.string.channel_profile_hide));
        actions.add(() -> host.setHidden(item, !hidden));
        options.add(host.text(R.string.channel_profile_startup));
        actions.add(() -> host.setStartup(item));
        options.add(host.text(R.string.menu_playback_mode_temporary));
        actions.add(() -> host.openTemporaryPlaybackMode(item));
        return new TvOptionsMenuModel(options, actions, null);
    }
}
