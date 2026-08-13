package myau.module;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.KeyEvent;
import myau.events.TickEvent;
import myau.module.modules.GuiModule;
import myau.module.modules.HUD;
import myau.util.ChatUtil;
import myau.util.SoundUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

public class ModuleManager {
    private boolean sound = false;
    public final LinkedHashMap<Class<?>, Module> modules = new LinkedHashMap<>();
    // Modules that aren't backed by a fixed class (one instance per loaded script), keyed by
    // unique name instead. Kept separate from `modules` because that map is Class-keyed and
    // can only ever hold one instance per runtime type.
    public final LinkedHashMap<String, Module> dynamicModules = new LinkedHashMap<>();

    public Module getModule(String string) {
        Module module = this.modules.values().stream().filter(mD -> mD.getName().equalsIgnoreCase(string)).findFirst().orElse(null);
        if (module != null) {
            return module;
        }
        return this.dynamicModules.values().stream().filter(mD -> mD.getName().equalsIgnoreCase(string)).findFirst().orElse(null);
    }

    public Module getModule(Class<?> clazz){
        return this.modules.get(clazz);
    }

    public Collection<Module> allModules() {
        ArrayList<Module> all = new ArrayList<>(this.modules.values());
        all.addAll(this.dynamicModules.values());
        return all;
    }

    public void registerDynamicModule(Module module) {
        if (this.dynamicModules.containsKey(module.getName())) {
            this.unregisterDynamicModule(module.getName());
        }
        this.dynamicModules.put(module.getName(), module);
        // Only claim the `.<name>` chat command if it isn't already a built-in module's -
        // a script sharing a real module's name shouldn't be able to shadow it.
        if (this.modules.values().stream().noneMatch(m -> m.getName().equalsIgnoreCase(module.getName()))) {
            setModuleCommandName(module.getName(), true);
        }
    }

    public void unregisterDynamicModule(String name) {
        Module module = this.dynamicModules.remove(name);
        if (module != null && module.isEnabled()) {
            module.setEnabled(false);
        }
        Myau.propertyManager.properties.remove(module);
        setModuleCommandName(name, false);
    }

    private void setModuleCommandName(String name, boolean add) {
        if (Myau.commandManager == null) {
            return;
        }
        for (myau.command.Command command : Myau.commandManager.commands) {
            if (command instanceof myau.command.commands.ModuleCommand) {
                if (add) {
                    if (!command.names.contains(name)) {
                        command.names.add(name);
                    }
                } else {
                    command.names.remove(name);
                }
                return;
            }
        }
    }

    public void playSound() {
        this.sound = true;
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        for (Module module : this.allModules()) {
            if (module.getKey() != event.getKey()) {
                continue;
            }
            boolean shouldNotify = module.toggle();
            HUD hud = (HUD) this.modules.get(HUD.class);
            if (hud != null && shouldNotify) {
                shouldNotify = hud.toggleAlerts.getValue();
            }
            if(module instanceof GuiModule){
                shouldNotify = false;
            }
            if (shouldNotify) {
                String status = module.isEnabled() ? "&a&lON" : "&c&lOFF";
                String message = String.format("%s%s: %s&r", Myau.clientName, module.getName(), status);
                ChatUtil.sendFormatted(message);
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.sound) {
                this.sound = false;
                SoundUtil.playSound("random.click");
            }
        }
    }
}
