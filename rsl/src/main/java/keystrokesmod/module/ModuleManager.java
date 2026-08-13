package keystrokesmod.module;

import keystrokesmod.Raven;
import keystrokesmod.module.impl.client.ChatCommands;
import keystrokesmod.module.impl.client.CommandLine;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.utility.profile.Manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ModuleManager {
    public static List<Module> modules = new ArrayList<>();
    public static List<Module> organizedModules = Collections.synchronizedList(new ArrayList<>());
    public static Module commandLine;
    public static ChatCommands chatCommands;

    public void register() {
        this.addModule(chatCommands = new ChatCommands());
        this.addModule(new Settings());
        this.addModule(new keystrokesmod.script.Manager());
        this.addModule(commandLine = new CommandLine());
        this.addModule(new Manager());
        this.addModule(new Gui());
        this.addModule(new HUD());
        modules.sort(Comparator.comparing(Module::getName));
    }

    public void addModule(Module m) {
        modules.add(m);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> inCategory(Module.category categ) {
        ArrayList<Module> categML = new ArrayList<>();

        for (Module mod : this.getModules()) {
            if (mod.moduleCategory().equals(categ)) {
                categML.add(mod);
            }
        }

        for (Module mod : Raven.scriptManager.scripts.values()) {
            if (mod.moduleCategory().equals(categ)) {
                categML.add(mod);
            }
        }

        return categML;
    }

    public Module getModule(String moduleName) {
        for (Module module : modules) {
            if (module.getName().equals(moduleName)) {
                return module;
            }
        }
        return null;
    }

    public Module getModule(Class clazz) {
        for (Module module : modules) {
            if (module.getClass().equals(clazz)) {
                return module;
            }
        }
        return null;
    }

    public static boolean canExecuteChatCommand() {
        return ModuleManager.chatCommands != null && ModuleManager.chatCommands.isEnabled();
    }

    public static boolean lowercaseChatCommands() {
        return ModuleManager.chatCommands != null && ModuleManager.chatCommands.isEnabled() && ModuleManager.chatCommands.lowercase();
    }
}
