package keystrokesmod;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import keystrokesmod.event.PostProfileLoadEvent;
import keystrokesmod.event.PostSetSliderEvent;
import keystrokesmod.module.Module;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.script.ScriptDefaults;
import keystrokesmod.script.ScriptManager;
import keystrokesmod.script.classes.Entity;
import keystrokesmod.script.classes.NetworkPlayer;
import keystrokesmod.utility.*;
import keystrokesmod.utility.command.CommandManager;
import keystrokesmod.utility.profile.Profile;
import keystrokesmod.utility.profile.ProfileManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;

@Mod(modid = "rsl", clientSideOnly = true)
public class Raven {
    public static boolean debug = false;
    public static Minecraft mc = Minecraft.getMinecraft();
    private static final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);
    private static final ExecutorService cachedExecutor = Executors.newCachedThreadPool();
    public static ModuleManager moduleManager;
    public static ClickGui clickGui;
    public static ProfileManager profileManager;
    public static ScriptManager scriptManager;
    public static CommandManager commandManager;
    public static Profile currentProfile;
    public static PacketsHandler packetsHandler;
    private static boolean firstLoad;

    public Raven() {
        moduleManager = new ModuleManager();
    }

    @EventHandler
    public void init(FMLInitializationEvent e) {
        Runtime.getRuntime().addShutdownHook(new Thread(scheduledExecutor::shutdown));
        Runtime.getRuntime().addShutdownHook(new Thread(cachedExecutor::shutdown));
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new Debugger());
        MinecraftForge.EVENT_BUS.register(new CPSCalculator());
        MinecraftForge.EVENT_BUS.register(new Ping());
        MinecraftForge.EVENT_BUS.register(packetsHandler = new PacketsHandler());
        MinecraftForge.EVENT_BUS.register(new ModHelper(this.mc));
        Reflection.getFields();
        moduleManager.register();
        scriptManager = new ScriptManager();
        clickGui = new ClickGui();
        profileManager = new ProfileManager();
        ScriptDefaults.reloadModules();
        scriptManager.loadScripts();
        profileManager.loadProfiles();
        profileManager.loadProfile("default");
        Reflection.setKeyBindings();
        commandManager = new CommandManager();
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent e) {
        if (e.phase == Phase.END) {
            if (Utils.nullCheck()) {
                if (mc.thePlayer.ticksExisted % 6000 == 0) { // reset cache every 5 minutes
                    Entity.clearCache();
                    NetworkPlayer.clearCache();
                    if (Debugger.BACKGROUND) {
                        Utils.sendMessage("&aticks % 6000 == 0 &7reached, clearing script caches. (&dEntity&7, &dNetworkPlayer&7)");
                    }
                }
                if (Reflection.sendMessage) {
                    Utils.sendMessage("&cThere was an error, relaunch the game.");
                    Reflection.sendMessage = false;
                }
                for (Module module : getModuleManager().getModules()) {
                    if (mc.currentScreen == null && module.canBeEnabled()) {
                        module.onKeyBind();
                    }
                    else if (mc.currentScreen instanceof ClickGui) {
                        module.guiUpdate();
                    }

                    if (module.isEnabled()) {
                        module.onUpdate();
                    }
                }
                if (mc.currentScreen == null) {
                    for (Module module : Raven.scriptManager.scripts.values()) {
                        module.onKeyBind();
                    }
                }
            }
        }
        else {
            if (mc.currentScreen == null && Utils.nullCheck()) {
                for (Profile profile : Raven.profileManager.profiles) {
                    profile.getModule().onKeyBind();
                }
            }
        }
    }

    @SubscribeEvent
    public void onPostProfileLoad(PostProfileLoadEvent e) {
        clickGui.onSliderChange();
    }

    @SubscribeEvent
    public void onPostSetSlider(PostSetSliderEvent e) {
        clickGui.onSliderChange();
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            if (!firstLoad) {
                firstLoad = true;
                scriptManager.loadScripts();
            }
            Entity.clearCache();
            NetworkPlayer.clearCache();
            if (Debugger.BACKGROUND) {
                Utils.sendMessage("&enew world&7, clearing script caches. (&dEntity&7, &dNetworkPlayer&7)");
            }
        }
    }

    public static ModuleManager getModuleManager() {
        return moduleManager;
    }

    public static ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public static ExecutorService getCachedExecutor() {
        return cachedExecutor;
    }

}