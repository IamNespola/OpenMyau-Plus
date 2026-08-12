package keystrokesmod.module;

import keystrokesmod.Raven;
import keystrokesmod.module.setting.Setting;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.script.Script;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.profile.ProfileModule;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Module {
    protected ArrayList<Setting> settings;
    private String moduleName;
    private Module.category moduleCategory;
    private boolean enabled;
    private int keycode;
    protected static Minecraft mc;
    private boolean isToggled = false;
    public boolean canBeEnabled = true;
    public boolean ignoreOnSave = false;
    public boolean hidden = false;
    public Script script = null;
    public boolean closetModule = false;
    public boolean alwaysOn = false;
    public String lastInfo;
    public static boolean sort; // global boolean in charge of sorting upon info change
    public static List<String> categoriesString = new ArrayList<>();

    static { // loads the categories
        for (category cat : category.values()) {
            categoriesString.add(cat.name());
        }
    }

    public Module(String moduleName, Module.category moduleCategory, int keycode) {
        this.moduleName = moduleName;
        this.moduleCategory = moduleCategory;
        this.keycode = keycode;
        this.enabled = false;
        mc = Minecraft.getMinecraft();
        this.settings = new ArrayList();
    }

    public static Module getModule(Class<? extends Module> a) {
        Iterator var1 = ModuleManager.modules.iterator();

        Module module;
        do {
            if (!var1.hasNext()) {
                return null;
            }

            module = (Module) var1.next();
        } while (module.getClass() != a);

        return module;
    }

    public Module(String name, Module.category moduleCategory) {
        this.moduleName = name;
        this.moduleCategory = moduleCategory;
        this.keycode = 0;
        this.enabled = false;
        mc = Minecraft.getMinecraft();
        this.settings = new ArrayList();
    }

    public Module(Script script) {
        super();
        this.enabled = false;
        this.moduleName = script.name;
        this.script = script;
        this.keycode = 0;
        //noinspection ConstantValue YOU ARE FUCKING LYING
        if (moduleCategory == null) {
            this.moduleCategory = category.scripts;
        }
        this.settings = new ArrayList<>();
    }

    public void onKeyBind() {
        if (this.keycode != 0) {
            try {
                if (!this.isToggled && (this.keycode >= 1000 ? ((this.keycode == 1069 || this.keycode == 1070) ? isScrollDown(this.keycode) : Mouse.isButtonDown(this.keycode - 1000)) : Keyboard.isKeyDown(this.keycode))) {
                    this.toggle();
                    this.isToggled = true;
                }
                else if ((this.keycode >= 1000 ? ((this.keycode == 1069 || this.keycode == 1070) ? !isScrollDown(this.keycode) : !Mouse.isButtonDown(this.keycode - 1000)) : !Keyboard.isKeyDown(this.keycode))) {
                    this.isToggled = false;
                }
            }
            catch (Exception e) {
                e.printStackTrace();
                Utils.sendMessage("&cFailed to check keybinding. Setting to none");
                this.keycode = 0;
            }
        }
    }

    public static boolean isScrollDown(int key) {
        if (key == 1069) {
            return Mouse.getDWheel() > 0;
        }
        else if (key == 1070) {
            return Mouse.getDWheel() < 0;
        }
        return false;
    }

    public boolean canBeEnabled() {
        if (this.script != null && script.error) {
            return false;
        }
        return this.canBeEnabled;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void enable() {
        if (!this.canBeEnabled() || this.isEnabled()) {
            return;
        }
        this.setEnabled(true);
        ModuleManager.organizedModules.add(this);

        if (this.script != null) {
            Raven.scriptManager.onEnable(script);
        }
        else {
            if (!alwaysOn) {
                MinecraftForge.EVENT_BUS.register(this);
            }
            this.onEnable();
        }
    }

    public void disable() {
        if (!this.isEnabled()) {
            return;
        }
        this.setEnabled(false);
        ModuleManager.organizedModules.remove(this);
        if (this.script != null) {
            Raven.scriptManager.onDisable(script);
        }
        else {
            if (!alwaysOn) {
                MinecraftForge.EVENT_BUS.unregister(this);
            }
            this.onDisable();
        }
    }

    public String getInfo() {
        return "";
    }

    public String getInfoUpdate() { // when called updates the modules info, and sorts if necessary
        String info = getInfo();
        if (info != lastInfo) {
            sort = true;
        }
        lastInfo = info;
        return info;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return this.moduleName;
    }

    public String getNameInHud() {
        return this.moduleName;
    }

    public ArrayList<Setting> getSettings() {
        return this.settings;
    }

    public void registerSetting(Setting Setting) {
        this.settings.add(Setting);
    }

    public Module.category moduleCategory() {
        return this.moduleCategory;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void toggle() {
        if (this.isEnabled()) {
            this.disable();
        } else {
            this.enable();
        }
        if (Raven.currentProfile != null) {
            ((ProfileModule) Raven.currentProfile.getModule()).saved = false;
        }
    }

    public void onUpdate() {}

    public void guiUpdate() {}

    public void guiButtonToggled(ButtonSetting b) {}

    public void onSlide(SliderSetting setting) {}

    public int getKeycode() {
        return this.keycode;
    }

    public void setBind(int keybind) {
        this.keycode = keybind;
    }

    public void setCategory(category moduleCategory) {
        this.moduleCategory = moduleCategory;
    }

    public enum category {
        combat,
        movement,
        player,
        world,
        render,
        minigames,
        fun,
        other,
        client,
        profiles,
        scripts;
    }
}
