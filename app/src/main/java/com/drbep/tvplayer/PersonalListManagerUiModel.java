package com.drbep.tvplayer;

import java.util.ArrayList;
import java.util.List;

public final class PersonalListManagerUiModel {
    public final List<PersonalListRowUiModel> items;

    public PersonalListManagerUiModel(List<PersonalListRowUiModel> items) {
        this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
    }
}
