package keystrokesmod.module.impl.client;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;

public class ChatCommands extends Module {
    public ButtonSetting lowercase;
    public ChatCommands() {
        super("Chat Commands", category.client);
        this.registerSetting(new DescriptionSetting("Use §o§e.help§r for help."));
        this.registerSetting(lowercase = new ButtonSetting("Lowercase", false));
    }

    public boolean lowercase() {
        return this.lowercase != null && this.lowercase.isToggled();
    }
}
