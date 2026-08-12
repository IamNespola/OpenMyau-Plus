package keystrokesmod.script;

import keystrokesmod.Raven;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.script.classes.Entity;
import keystrokesmod.script.classes.Image;
import keystrokesmod.script.classes.NetworkPlayer;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.profile.ProfileModule;
import org.lwjgl.Sys;

import java.awt.*;
import java.io.IOException;
import java.net.URI;

public class Manager extends Module {
    private long lastLoad;
    public final String documentationURL = "https://blowsy.gitbook.io/raven";
    public static ButtonSetting allowUnsafeScripts;

    public Manager() {
        super("Manager", category.scripts);
        this.registerSetting(new ButtonSetting("Load scripts", () -> {
            if (Raven.scriptManager.compiler == null) {
                Utils.sendMessage("&cCompiler error, JDK not found");
            }
            else {
                final long currentTimeMillis = System.currentTimeMillis();
                if (Utils.timeBetween(this.lastLoad, currentTimeMillis) > 1500) {
                    this.lastLoad = currentTimeMillis;
                    Raven.scriptManager.loadScripts();
                    if (Raven.scriptManager.scripts.isEmpty()) {
                        Utils.sendMessage("&7No scripts found.");
                    }
                    else {
                        double timeTaken = Utils.round((System.currentTimeMillis() - currentTimeMillis) / 1000.0, 1);
                        Utils.sendMessage("&7Loaded &b" + Raven.scriptManager.scripts.size() + " &7script" + ((Raven.scriptManager.scripts.size() == 1) ? "" : "s") + " in &b" + Utils.asWholeNum(timeTaken) + "&7s.");
                    }
                    Entity.clearCache();
                    NetworkPlayer.clearCache();
                    Image.clearCache();
                    ScriptDefaults.reloadModules();
                    if (Raven.currentProfile != null && Raven.currentProfile.getModule() != null) {
                        ((ProfileModule) Raven.currentProfile.getModule()).saved = false;
                    }
                }
                else {
                    Utils.sendMessage("&cYou are on cooldown.");
                }
            }
        }));
        this.registerSetting(new ButtonSetting("Open folder", () -> {
            try {
                Desktop.getDesktop().open(Raven.scriptManager.directory);
            }
            catch (IOException ex) {
                Raven.scriptManager.directory.mkdirs();
                Utils.sendMessage("&cError locating folder, recreated.");
            }
        }));
        this.registerSetting(new ButtonSetting("View documentation", () -> {
            try {
                Desktop.getDesktop().browse(new URI(documentationURL));
            } catch (Throwable t) {
                Sys.openURL(documentationURL);
            }
        }));
        if (allowUnsafeScripts == null) {
            this.registerSetting(allowUnsafeScripts = new ButtonSetting("Allow unsafe scripts", false));
        } else {
            this.registerSetting(allowUnsafeScripts);
        }
        this.canBeEnabled = false;
        this.ignoreOnSave = true;
    }
}
