package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

final class PersonalListUiFactory {
    interface Host {
        String text(int resId);
        String text(int resId, Object... args);
        String preview(ChannelCollectionStore.ChannelCollection collection);
        void beforeAction();
        void openChannels(ChannelCollectionStore.ChannelCollection collection);
        void openActions(ChannelCollectionStore.ChannelCollection collection);
    }

    private PersonalListUiFactory() {
    }

    static PersonalListManagerUiModel build(List<ChannelCollectionStore.ChannelCollection> collections, Host host) {
        List<PersonalListRowUiModel> rows = new ArrayList<>();
        if (host != null && collections != null) {
            for (ChannelCollectionStore.ChannelCollection collection : collections) {
                if (collection == null) {
                    continue;
                }
                rows.add(new PersonalListRowUiModel(
                        String.valueOf(Math.min(99, collection.channelIds.size())),
                        collection.label,
                        host.text(R.string.personal_list_count, collection.channelIds.size()) + "  ·  " + host.preview(collection),
                        host.text(R.string.personal_list_action_badge),
                        () -> {
                            host.beforeAction();
                            host.openChannels(collection);
                        },
                        () -> {
                            host.beforeAction();
                            host.openActions(collection);
                        }
                ));
            }
        }
        return new PersonalListManagerUiModel(rows);
    }
}
