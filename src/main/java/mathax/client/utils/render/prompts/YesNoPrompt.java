package mathax.client.utils.render.prompts;

import com.mojang.blaze3d.systems.RenderSystem;
import mathax.client.gui.widgets.containers.WHorizontalList;
import mathax.client.gui.widgets.pressable.WCheckbox;
import mathax.client.gui.GuiTheme;
import mathax.client.gui.GuiThemes;
import mathax.client.gui.WindowScreen;
import mathax.client.gui.widgets.pressable.WButton;
import mathax.client.systems.config.Config;
import net.minecraft.client.gui.screen.Screen;

import java.util.ArrayList;
import java.util.List;

import static mathax.client.MatHax.mc;

public class YesNoPrompt {
    private final GuiTheme theme;
    private final Screen parent;

    private String title = "";
    private final List<String> messages = new ArrayList<>();
    private String id = null;

    private Runnable onYes = () -> {};
    private Runnable onNo = () -> {};

    private YesNoPrompt() {
        this(GuiThemes.get(), mc.currentScreen);
    }

    private YesNoPrompt(GuiTheme theme, Screen parent) {
        this.theme = theme;
        this.parent = parent;
    }

    public static YesNoPrompt create() {
        return new YesNoPrompt();
    }

    public static YesNoPrompt create(GuiTheme theme, Screen parent) {
        return new YesNoPrompt(theme, parent);
    }

    public YesNoPrompt title(String title) {
        this.title = title;
        return this;
    }

    public YesNoPrompt message(String message) {
        this.messages.add(message);
        return this;
    }

    public YesNoPrompt message(String message, Object... args) {
        this.messages.add(String.format(message, args));
        return this;
    }

    public YesNoPrompt id(String from) {
        this.id = from;
        return this;
    }

    public YesNoPrompt onYes(Runnable action) {
        this.onYes = action;
        return this;
    }

    public YesNoPrompt onNo(Runnable action) {
        this.onNo = action;
        return this;
    }

    public void show() {
        if (id == null) this.id(this.title);
        if (Config.get().dontShowAgainPrompts.contains(id)) return;

        if (!RenderSystem.isOnRenderThread()) RenderSystem.recordRenderCall(() -> mc.setScreen(new PromptScreen(theme)));
        else mc.setScreen(new PromptScreen(theme));
    }

    public class PromptScreen extends WindowScreen {
        public PromptScreen(GuiTheme theme) {
            super(theme, YesNoPrompt.this.title);

            this.parent = YesNoPrompt.this.parent;
        }

        @Override
        public void initWidgets() {
            for (String line : messages) add(theme.label(line)).expandX();
            add(theme.horizontalSeparator()).expandX();

            WHorizontalList checkboxContainer = add(theme.horizontalList()).expandX().widget();
            WCheckbox dontShowAgainCheckbox = checkboxContainer.add(theme.checkbox(false)).widget();
            checkboxContainer.add(theme.label("Don't show this again.")).expandX();

            WHorizontalList list = add(theme.horizontalList()).expandX().widget();

            WButton yesButton = list.add(theme.button("Yes")).expandX().widget();
            yesButton.action = () -> {
                if (dontShowAgainCheckbox.checked) Config.get().dontShowAgainPrompts.add(id);
                onYes.run();
                onClose();
            };

            WButton noButton = list.add(theme.button("No")).expandX().widget();
            noButton.action = () -> {
                if (dontShowAgainCheckbox.checked) Config.get().dontShowAgainPrompts.add(id);
                onNo.run();
                onClose();
            };
        }
    }
}
