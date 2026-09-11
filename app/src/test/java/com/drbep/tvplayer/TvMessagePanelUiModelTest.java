package com.drbep.tvplayer;

import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class TvMessagePanelUiModelTest {
    @Test public void existingDialogsKeepNoPoster() {
        TvMessagePanelUiModel model = new TvMessagePanelUiModel(null, null, Collections.emptyList());
        assertEquals("", model.title);
        assertEquals("", model.message);
        assertNull(model.bindPoster);
    }
    @Test public void resumeDialogKeepsPosterAndActions() {
        Runnable resume = () -> {};
        Runnable back = () -> {};
        java.util.function.Consumer<android.widget.ImageView> poster = image -> {};
        TvMessageActionUiModel action = new TvMessageActionUiModel("Continuar",false,resume);
        TvMessagePanelUiModel model = new TvMessagePanelUiModel("Serie", "30:11",
                Collections.singletonList(action), back, poster);
        assertSame(poster, model.bindPoster);
        assertSame(resume, model.actions.get(0).onClick);
        assertSame(back, model.onBack);
    }
}
