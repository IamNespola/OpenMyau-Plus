package keystrokesmod.module.impl.client;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.utility.Commands;
import keystrokesmod.utility.Timer;

public class CommandLine extends Module {
    public static boolean opened = false;
    public static boolean closed = false;
    public static Timer animation;
    public static ButtonSetting animate;

    public CommandLine() {
        super("Command line", category.client);
        this.registerSetting(animate = new ButtonSetting("Animate", true));
    }

    public void onEnable() {
        Commands.setccs();
        opened = true;
        closed = false;
        (animation = new Timer(500.0F)).start();
    }

    public void onDisable() {
        closed = true;
        if (animation != null) {
            animation.start();
        }

        Commands.onDisable();
    }
}
