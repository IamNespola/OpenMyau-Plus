package myau.module;

import myau.Myau;
import myau.module.modules.ScriptModule;
import java.util.*;

/** One module catalog shared by every GUI style. */
public final class ModuleCatalog {
    public static final String[] CATEGORIES = {"Combat", "Movement", "Render", "Player", "Network", "Misc", "Scripts"};
    private static final Map<String, String> CATEGORIES_BY_NAME = new HashMap<>();
    private static final Map<String, String> DESCRIPTIONS = new HashMap<>();
    static {
        add("AimAssist", "Combat", "Adjusts aim toward nearby targets while attacking.");
        add("AntiAFK", "Movement", "Makes small movements to prevent idle kicks.");
        add("AntiDebuff", "Render", "Hides blindness and nausea visual effects.");
        add("AntiFireball", "Combat", "Attacks nearby fireballs to deflect them.");
        add("AntiObbyTrap", "Player", "Helps escape blocks trapping the player.");
        add("AntiObfuscate", "Render", "Removes obfuscated text formatting.");
        add("AntiVoid", "Movement", "Attempts to recover when falling into the void.");
        add("AutoClicker", "Combat", "Repeats attack clicks while the mouse is held.");
        add("AutoAnduril", "Player", "Selects the Anduril sword when needed.");
        add("KnockbackDelay", "Combat", "Delays the application of knockback packets.");
        add("TargetESP", "Render", "Highlights the current combat target.");
        add("AutoHeal", "Player", "Uses healing items when health is low.");
        add("AutoTool", "Player", "Selects a suitable tool for the block being mined.");
        add("AutoSwap", "Player", "Switches held items according to configured rules.");
        add("BedNuker", "Player", "Finds and breaks nearby beds and covering blocks.");
        add("BedESP", "Render", "Highlights bed blocks in the world.");
        add("BedTracker", "Player", "Alerts when players or pearls approach your bed.");
        add("Blink", "Network", "Queues outgoing movement packets until released.");
        add("Backtrack", "Combat", "Delays entity updates for selected targets.");
        add("Hitflick", "Combat", "Adjusts aim during attacks on nearby players.");
        add("AutoHeadHitter", "Movement", "Times jumps beneath overhead blocks.");
        add("Fpscounter", "Render", "Displays frame rate in a compact overlay.");
        add("Chams", "Render", "Renders highlighted player models.");
        add("WaterMark", "Render", "Displays a customizable client watermark.");
        add("ChestESP", "Render", "Highlights storage blocks in the world.");
        add("ClickGUI", "Render", "Opens the module browser and settings.");
        add("ChestStealer", "Player", "Moves items from open chests into inventory.");
        add("Eagle", "Movement", "Sneaks near block edges while bridging.");
        add("ESP", "Render", "Highlights players with configurable overlays.");
        add("FastPlace", "Player", "Reduces the delay between item-use clicks.");
        add("ServerLag", "Network", "Delays incoming packets to simulate latency.");
        add("Fly", "Movement", "Changes player movement for flight modes.");
        add("FakeLag", "Network", "Queues packets to simulate network lag.");
        add("Fullbright", "Render", "Brightens dark areas without changing the world.");
        add("GhostHand", "Player", "Ignores selected players when targeting blocks.");
        add("HitSelect", "Combat", "Controls attack timing around knockback.");
        add("AutoHypixel", "Misc", "Automates selected Hypixel-specific actions.");
        add("HUD", "Render", "Configures the module list and HUD colors.");
        add("Notifications", "Render", "Displays module and client notifications.");
        add("Hotbar", "Render", "Replaces the hotbar with a customizable overlay.");
        add("MoreKB", "Combat", "Changes sprint timing around attacks.");
        add("Indicators", "Render", "Shows player and combat status indicators.");
        add("InventoryClicker", "Player", "Repeats inventory clicks while held.");
        add("InvManager", "Player", "Sorts inventory and removes unwanted items.");
        add("InvWalk", "Movement", "Allows movement while selected screens are open.");
        add("Criticals", "Combat", "Changes attack movement for critical hits.");
        add("BlockHit", "Combat", "Times sword blocking around attacks.");
        add("ThrowAura", "Combat", "Uses throwable items against nearby targets.");
        add("ESP2D", "Render", "Draws screen-space boxes around entities.");
        add("ClientSpoofer", "Network", "Changes the client brand sent to the server.");
        add("ItemESP", "Render", "Highlights dropped items in the world.");
        add("Jesus", "Movement", "Changes movement on water surfaces.");
        add("Disabler", "Network", "Changes packet handling using server-specific modes.");
        add("Displace", "Combat", "Changes movement or timing around a target.");
        add("KeepSprint", "Movement", "Retains sprinting through attacks.");
        add("FlagDetector", "Misc", "Reports server movement corrections.");
        add("HitBox", "Combat", "Changes entity targeting box sizes.");
        add("KillAura", "Combat", "Automatically selects and attacks nearby targets.");
        add("LagRange", "Combat", "Controls packet delay based on target distance.");
        add("LightningTracker", "Misc", "Reports lightning positions in chat.");
        add("LongJump", "Movement", "Changes movement to extend jump distance.");
        add("MCF", "Misc", "Adds or removes friends with the middle mouse button.");
        add("Ambience", "Render", "Changes local time and weather appearance.");
        add("ChestAura", "Player", "Opens nearby storage containers automatically.");
        add("NameTags", "Render", "Displays configurable player name labels.");
        add("NickHider", "Misc", "Replaces player names in displayed text.");
        add("NoFall", "Movement", "Changes fall-related movement packets.");
        add("Stasis", "Combat", "Controls movement and packet timing during combat.");
        add("NoHitDelay", "Combat", "Removes the local attack click delay.");
        add("NoHurtCam", "Render", "Removes the camera shake when damaged.");
        add("NoJumpDelay", "Movement", "Removes the delay between held jumps.");
        add("NoRotate", "Network", "Changes handling of server rotation corrections.");
        add("BlockOverlay", "Render", "Customizes the targeted block outline.");
        add("MouseRawInput", "Misc", "Uses raw mouse input for camera movement.");
        add("Piercing", "Combat", "Changes targeting behavior around intervening entities.");
        add("BedwarUtils", "Misc", "Adds utilities for Bed Wars matches.");
        add("NoSlow", "Movement", "Changes movement slowdown while using items.");
        add("AutoAuth", "Misc", "Sends configured login commands on supported servers.");
        add("Capes", "Render", "Loads and displays local cape textures.");
        add("MoveFix", "Movement", "Aligns movement input with modified rotations.");
        add("ClickAssits", "Combat", "Adds configured assistance to attack clicks.");
        add("Timer", "Movement", "Changes the local game tick rate.");
        add("BreakProgress", "Render", "Shows progress on the block being mined.");
        add("SprintReset", "Combat", "Resets sprinting around attacks.");
        add("Radar", "Render", "Shows nearby entities on a compact radar.");
        add("Reach", "Combat", "Changes the local targeting reach distance.");
        add("RenderFixes", "Render", "Styles chat and scoreboard backgrounds.");
        add("Refill", "Player", "Moves items from inventory into hotbar slots.");
        add("SafeWalk", "Movement", "Prevents movement over block edges.");
        add("DynamicIsland", "Render", "Displays server and player status in the HUD.");
        add("Scaffold", "Player", "Places blocks beneath the player while moving.");
        add("AutoBlockIn", "Player", "Places protective blocks around the player.");
        add("AntiBot", "Combat", "Filters suspected bot entities from targeting.");
        add("AutoBedDef", "Player", "Places blocks to defend a bed.");
        add("TickBase", "Combat", "Changes movement timing near combat targets.");
        add("Statistics", "Render", "Displays match and player statistics.");
        add("FreeLook", "Render", "Moves the camera independently of player rotation.");
        add("ItemPhysics", "Render", "Changes the appearance of dropped items.");
        add("Spammer", "Misc", "Repeats configured chat messages.");
        add("Speed", "Movement", "Changes movement speed using selected modes.");
        add("SpeedMine", "Player", "Changes block-breaking timing.");
        add("Sprint", "Movement", "Keeps sprint active while moving forward.");
        add("TargetHUD", "Render", "Displays the current target's health and status.");
        add("TargetStrafe", "Movement", "Moves around the selected combat target.");
        add("Tracers", "Render", "Draws lines toward nearby entities.");
        add("TimerRange", "Combat", "Changes local tick timing around target distance.");
        add("Trajectories", "Render", "Previews the path of thrown items and arrows.");
        add("Velocity", "Combat", "Changes the application of knockback.");
        add("ViewClip", "Render", "Allows the third-person camera through blocks.");
        add("WTap", "Combat", "Resets forward movement around attacks.");
        add("Xray", "Render", "Shows selected blocks through surrounding terrain.");
        add("TeamHealthDisplay", "Render", "Displays teammates' health.");
        add("Animations", "Render", "Customizes swinging and blocking animations.");
        add("Gapple", "Player", "Uses golden apples according to health settings.");
        add("HitParticleEffects", "Render", "Adds particles when attacks hit a target.");
    }
    private static void add(String name, String category, String description) {
        CATEGORIES_BY_NAME.put(name.toLowerCase(Locale.ROOT), category);
        DESCRIPTIONS.put(name.toLowerCase(Locale.ROOT), description);
    }
    public static String category(Module module) {
        return module instanceof ScriptModule ? "Scripts" : CATEGORIES_BY_NAME.getOrDefault(module.getName().toLowerCase(Locale.ROOT), "Misc");
    }
    public static String description(String name) {
        return DESCRIPTIONS.getOrDefault(name.toLowerCase(Locale.ROOT), "");
    }
    public static List<Module> modules(String category) {
        List<Module> result = new ArrayList<>();
        for (Module module : Myau.moduleManager.allModules()) if (category.equals(category(module))) result.add(module);
        result.sort(Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }
    private ModuleCatalog() { }
}
