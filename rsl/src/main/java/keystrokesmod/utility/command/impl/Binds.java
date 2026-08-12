package keystrokesmod.utility.command.impl;

import keystrokesmod.Raven;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.utility.command.Command;
import keystrokesmod.utility.profile.Profile;
import org.lwjgl.input.Keyboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Binds extends Command {
    public Binds() {
        super("binds");
    }

    @Override
    public void onExecute(String[] args) {
        if (args.length <= 1) {
            Map<String, List<String>> binds = getBindsModulesMap(0);
            int size = getTotalModules(binds);
            chatWithPrefix("&b" + size + " &7module" + (size == 1 ? "" : "s") + " have keybinds.");
            for (Map.Entry<String, List<String>> entry : binds.entrySet()) {
                String key = entry.getKey();
                List<String> moduleNames = entry.getValue();
                for (String moduleName : moduleNames) {
                    chatWithPrefix(" &b" + key + " &7" + moduleName);
                }
            }
        }
        else if (args.length == 2) {
            int keycode = Keyboard.getKeyIndex(args[1].toUpperCase());
            if (keycode == 0) {
                chatWithPrefix("&7Invalid key.");
                return;
            }

            Map<String, List<String>> binds = getBindsModulesMap(keycode);
            int size = getTotalModules(binds);
            chatWithPrefix("&b" + size + " &7module" + (size == 1 ? "" : "s") + " has keybind &b" + args[1].toUpperCase() + "&7.");

            for (Map.Entry<String, List<String>> entry : binds.entrySet()) {
                String key = entry.getKey();
                List<String> moduleNames = entry.getValue();
                for (String moduleName : moduleNames) {
                    chatWithPrefix(" &b" + key + " &7" + moduleName);
                }
            }
        }
        else {
            syntaxError();
        }
    }

    private Map<String, List<String>> getBindsModulesMap(int keycode) {
        Map<String, List<String>> binds = new HashMap<>();
        for (Module module : ModuleManager.modules) {
            addModuleIfMatches(binds, module, keycode);
        }
        for (Profile profile : Raven.profileManager.profiles) {
            Module module = profile.getModule();
            addModuleIfMatches(binds, module, keycode);
        }
        for (Module scriptModule : Raven.scriptManager.scripts.values()) {
            addModuleIfMatches(binds, scriptModule, keycode);
        }
        return binds;
    }

    private void addModuleIfMatches(Map<String, List<String>> bindsMap, Module module, int keycode) {
        if (module.getKeycode() == 0) {
            return;
        }
        if (keycode != 0 && module.getKeycode() != keycode) {
            return;
        }
        String keyName = (module.getKeycode() >= 1000) ? "M" + (module.getKeycode() - 1000) : Keyboard.getKeyName(module.getKeycode());
        List<String> moduleNames = bindsMap.get(keyName);
        if (moduleNames == null) {
            moduleNames = new ArrayList<>();
            bindsMap.put(keyName, moduleNames);
        }
        moduleNames.add(module.getName());
    }

    private int getTotalModules(Map<String, List<String>> binds) {
        int total = 0;
        for (List<String> modules : binds.values()) {
            total += modules.size();
        }
        return total;
    }
}
