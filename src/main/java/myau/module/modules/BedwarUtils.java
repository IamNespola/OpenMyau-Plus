package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.LoadWorldEvent;
import myau.events.PacketEvent;
import myau.events.Render2DEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.*;
import myau.enums.ChatColors;
import myau.util.client.ChatUtil;
import myau.util.ColorUtil;
import myau.util.SoundUtil;
import myau.util.player.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BedwarUtils extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final BooleanProperty hud = new BooleanProperty("hud", true);
    public final IntProperty hudX = new IntProperty("hud-x", 4, 0, 500, this.hud::getValue);
    public final IntProperty hudY = new IntProperty("hud-y", 66, 0, 500, this.hud::getValue);
    public final FloatProperty hudScale = new FloatProperty("hud-scale", 1.0F, 0.5F, 2.0F, this.hud::getValue);
    public final BooleanProperty hudShadow = new BooleanProperty("hud-shadow", true, this.hud::getValue);
    public final BooleanProperty diamondUpgrades = new BooleanProperty("diamond-upgrades", true);
    public final BooleanProperty itemTracker = new BooleanProperty("item-tracker", true);
    public final BooleanProperty bedTracker = new BooleanProperty("bedtracker", true);
    public final BooleanProperty invisAlert = new BooleanProperty("invis-alert", true);

    public final BooleanProperty bedTrackerAlerts;
    public final IntProperty bedTrackerAlertRange;
    public final BooleanProperty bedTrackerAlertOnPearl;
    public final ModeProperty bedTrackerAlertSound;
    public final IntProperty bedTrackerAlertFrequency;
    public final BooleanProperty bedTrackerAutoInc;
    public final BooleanProperty bedTrackerMacro;
    public final IntProperty bedTrackerMacroRange;
    public final BooleanProperty bedTrackerMacroOnPearl;
    public final TextProperty bedTrackerMacroText;
    public final IntProperty bedTrackerMacroDelay;
    public final BooleanProperty bedTrackerHud;
    public final ModeProperty bedTrackerHudPosX;
    public final ModeProperty bedTrackerHudPosY;
    public final IntProperty bedTrackerHudOffX;
    public final IntProperty bedTrackerHudOffY;
    public final FloatProperty bedTrackerHudScale;
    public final BooleanProperty bedTrackerHudShadow;

    private static final Pattern ITEM_TRACKER_PATTERN = Pattern.compile("(.+?)\\s+has\\s+(?:an?\\s+)?(.+?)(?:[.!])?$",
            Pattern.CASE_INSENSITIVE);
    private final Set<String> trackedItemMessages = new HashSet<>();
    private final BedTracker bedTrackerDelegate;

    private boolean trap;
    private String trapType = "";
    private boolean sharp;
    private int protLevel;
    private final LinkedHashMap<String, Long> invisAlertCooldowns = new LinkedHashMap<>();

    public BedwarUtils() {
        super("BedwarUtils", false, false);
        this.bedTrackerDelegate = new BedTracker();
        this.bedTrackerAlerts = this.bedTrackerDelegate.alerts;
        this.bedTrackerAlertRange = this.bedTrackerDelegate.alertRange;
        this.bedTrackerAlertOnPearl = this.bedTrackerDelegate.alertOnPearl;
        this.bedTrackerAlertSound = this.bedTrackerDelegate.alertSound;
        this.bedTrackerAlertFrequency = this.bedTrackerDelegate.alertFrequency;
        this.bedTrackerAutoInc = this.bedTrackerDelegate.autoInc;
        this.bedTrackerMacro = this.bedTrackerDelegate.marco;
        this.bedTrackerMacroRange = this.bedTrackerDelegate.marcoRange;
        this.bedTrackerMacroOnPearl = this.bedTrackerDelegate.marcoOnPreal;
        this.bedTrackerMacroText = this.bedTrackerDelegate.marcoText;
        this.bedTrackerMacroDelay = this.bedTrackerDelegate.marcoDelay;
        this.bedTrackerHud = this.bedTrackerDelegate.hud;
        this.bedTrackerHudPosX = this.bedTrackerDelegate.hudPosX;
        this.bedTrackerHudPosY = this.bedTrackerDelegate.hudPosY;
        this.bedTrackerHudOffX = this.bedTrackerDelegate.hudOffX;
        this.bedTrackerHudOffY = this.bedTrackerDelegate.hudOffY;
        this.bedTrackerHudScale = this.bedTrackerDelegate.hudScale;
        this.bedTrackerHudShadow = this.bedTrackerDelegate.hudShadow;
        this.bedTrackerDelegate.setEnabled(true);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.RECEIVE
                || !(event.getPacket() instanceof S02PacketChat)) {
            return;
        }
        String text = ((S02PacketChat) event.getPacket()).getChatComponent().getUnformattedText();
        String formattedText = ((S02PacketChat) event.getPacket()).getChatComponent().getFormattedText();
        this.scanMessage(text, formattedText);
        if (this.bedTracker.getValue()) {
            this.bedTrackerDelegate.onPacket(event);
        }
    }

    @EventTarget
    public void onLoadWorld(LoadWorldEvent event) {
        this.reset(false);
        this.bedTrackerDelegate.onLoadWorld(event);
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (!this.isEnabled() || event.getType() != EventType.POST || mc.theWorld == null || mc.thePlayer == null) {
            return;
        }
        if (this.itemTracker.getValue()) {
            for (Object object : mc.theWorld.playerEntities) {
                if (!(object instanceof EntityPlayer)) {
                    continue;
                }
                EntityPlayer player = (EntityPlayer) object;
                if (player == mc.thePlayer || player.isDead || player.getName() == null || player.getName().isEmpty()) {
                    continue;
                }
                this.scanPlayerItem(player, "held", player.getHeldItem());
                for (int slot = 0; slot < 4; slot++) {
                    this.scanPlayerItem(player, "armor-" + slot, player.getCurrentArmor(slot));
                }
            }
        }
        if (this.bedTracker.getValue()) {
            this.bedTrackerDelegate.onTick(event);
        }
        if (this.invisAlert.getValue()) {
            this.scanInvisiblePlayers();
        }
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled() || !this.hud.getValue()) {
            return;
        }
        float scale = this.hudScale.getValue();
        float x = this.hudX.getValue() / scale;
        float y = this.hudY.getValue() / scale;
        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0F);
        float rowY = y;
        if (this.diamondUpgrades.getValue()) {
            this.drawTrapLine(x, rowY);
            rowY += 10.0F;
            this.drawLine("Sharp", this.sharp, -1, x, rowY);
            rowY += 10.0F;
            this.drawLine("Prot", this.protLevel > 0, this.protLevel, x, rowY);
            rowY += 10.0F;
        }
        if (this.bedTracker.getValue()) {
            rowY += this.diamondUpgrades.getValue() ? 20.0F : 0.0F;
            this.drawBedLine(x, rowY);
        }
        GlStateManager.popMatrix();
    }

    private void scanMessage(String text, String formattedText) {
        if (text == null) {
            return;
        }
        String lower = text.toLowerCase();
        if (this.isNewGameMessage(lower)) {
            this.reset(true);
            return;
        }
        if (this.diamondUpgrades.getValue()) {
            if (lower.contains("trap") || lower.contains("it's a trap") || lower.contains("alarm trap")
                    || lower.contains("miner fatigue")) {
                this.trap = true;
                this.trapType = this.parseTrapType(lower);
            }
            if (lower.contains("sharpened swords") || lower.contains("sharpness") || lower.contains("sharp")) {
                this.sharp = true;
            }
            if (lower.contains("reinforced armor") || lower.contains("protection") || lower.contains("prot")) {
                int level = this.parseProtLevel(lower);
                this.protLevel = Math.max(this.protLevel, level <= 0 ? 1 : level);
            }
        }
        this.scanItemTracker(text, formattedText);
    }

    private boolean isNewGameMessage(String lower) {
        return lower.contains("protect your bed")
                || lower.contains("you are playing on")
                || lower.contains("the game starts in 1 second")
                || lower.contains("the game has started")
                || lower.contains("bed wars") && lower.contains("protect your bed");
    }

    private void scanPlayerItem(EntityPlayer player, String slot, ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return;
        }
        String item = this.normalizeItemName(EnumChatFormatting.getTextWithoutFormattingCodes(stack.getDisplayName()));
        if (!this.isTrackedItem(item)) {
            return;
        }
        String key = player.getName().toLowerCase() + ":" + slot + ":" + item.toLowerCase();
        if (!this.trackedItemMessages.add(key)) {
            return;
        }
        this.sendItemTrackerMessage(player.getDisplayName().getFormattedText(), item);
    }

    private void scanItemTracker(String text, String formattedText) {
        if (!this.itemTracker.getValue()) {
            return;
        }
        Matcher matcher = ITEM_TRACKER_PATTERN.matcher(text);
        if (!matcher.find()) {
            return;
        }
        String item = this.normalizeItemName(matcher.group(2).trim());
        if (!this.isTrackedItem(item)) {
            return;
        }
        String key = (matcher.group(1).trim() + " has " + item).toLowerCase();
        if (!this.trackedItemMessages.add(key)) {
            return;
        }
        this.sendItemTrackerMessage(this.extractFormattedPlayer(formattedText, matcher.group(1).trim()), item);
    }

    private boolean isTrackedItem(String item) {
        String lower = item.toLowerCase();
        boolean tieredGear = (lower.contains("stone") || lower.contains("iron") || lower.contains("diamond"))
                && (lower.contains("sword")
                || lower.contains("armor")
                || lower.contains("chestplate")
                || lower.contains("leggings")
                || lower.contains("boots")
                || lower.contains("helmet")
                || lower.contains("pickaxe")
                || lower.contains("axe"));
        boolean utilityItem = lower.contains("bow")
                || lower.contains("shears")
                || lower.contains("fireball")
                || lower.contains("ender pearl")
                || lower.contains("pearl")
                || lower.contains("invisibility")
                || lower.contains("invis")
                || lower.contains("jump")
                || lower.contains("speed");
        return tieredGear || utilityItem;
    }

    private String normalizeItemName(String item) {
        String normalized = item.replaceAll("(?i)^an?\\s+", "").trim();
        if (normalized.endsWith(".")) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        return normalized;
    }

    private String extractFormattedPlayer(String formattedText, String fallback) {
        if (formattedText == null) {
            return fallback;
        }
        String marker = " has ";
        String lowerFormatted = formattedText.toLowerCase();
        int index = lowerFormatted.indexOf(marker);
        if (index < 0) {
            index = lowerFormatted.indexOf(" has an ");
        }
        if (index < 0) {
            index = lowerFormatted.indexOf(" has a ");
        }
        return index > 0 ? formattedText.substring(0, index) : fallback;
    }

    private int parseProtLevel(String text) {
        if (text.contains(" iv") || text.contains(" 4") || text.contains("level iv") || text.contains("level 4"))
            return 4;
        if (text.contains(" iii") || text.contains(" 3") || text.contains("level iii") || text.contains("level 3"))
            return 3;
        if (text.contains(" ii") || text.contains(" 2") || text.contains("level ii") || text.contains("level 2"))
            return 2;
        if (text.contains(" i") || text.contains(" 1") || text.contains("level i") || text.contains("level 1"))
            return 1;
        return 0;
    }

    private void drawLine(String name, boolean value, int level, float x, float y) {
        int white = 0xFFFFFFFF;
        int green = 0xFF55FF55;
        int red = 0xFFFF5555;
        boolean shadow = this.hudShadow.getValue();
        String prefix = "- " + name + ": ";
        mc.fontRendererObj.drawString(prefix, x, y, white, shadow);
        float valueX = x + mc.fontRendererObj.getStringWidth(prefix);
        mc.fontRendererObj.drawString(value ? "true" : "false", valueX, y, value ? green : red, shadow);
        if (level > 0) {
            String suffix = " [" + this.toRoman(level) + "]";
            mc.fontRendererObj.drawString(suffix, valueX + mc.fontRendererObj.getStringWidth("true"), y, white, shadow);
        }
    }

    private void drawBedLine(float x, float y) {
        int white = 0xFFFFFFFF;
        int green = 0xFF55FF55;
        int red = 0xFFFF5555;
        boolean shadow = this.hudShadow.getValue();
        boolean hasBed = this.bedTrackerDelegate.isBed(this.bedTrackerDelegate.getBedPos());
        String prefix = "Bed: ";
        mc.fontRendererObj.drawString(prefix, x, y, white, shadow);
        float valueX = x + mc.fontRendererObj.getStringWidth(prefix);
        mc.fontRendererObj.drawString(hasBed ? "true" : "false", valueX, y, hasBed ? green : red, shadow);
    }

    private void scanInvisiblePlayers() {
        BlockPos bed = this.bedTrackerDelegate.getBedPos();
        if (bed == null || mc.theWorld == null || mc.thePlayer == null)
            return;
        long now = System.currentTimeMillis();
        for (Object object : mc.theWorld.playerEntities) {
            if (!(object instanceof EntityPlayer))
                continue;
            EntityPlayer player = (EntityPlayer) object;
            if (player == mc.thePlayer || player.isDead || player.getName() == null || TeamUtil.isSameTeam(player))
                continue;
            double distance = player.getDistance(bed.getX() + 0.5D, bed.getY() + 0.5D, bed.getZ() + 0.5D);
            if (distance > 18.0D)
                continue;
            int armorPieces = 0;
            for (int slot = 0; slot < 4; slot++) {
                if (player.getCurrentArmor(slot) != null)
                    armorPieces++;
            }
            boolean suspicious = player.isInvisible() || armorPieces <= 1;
            String key = player.getName().toLowerCase();
            long last = this.invisAlertCooldowns.getOrDefault(key, 0L);
            if (suspicious && now - last > 5000L) {
                this.invisAlertCooldowns.put(key, now);
                ChatUtil.display(this.getMyauPrefix() + " &cSuspicious/invis player near bed: &f"
                        + player.getDisplayName().getFormattedText());
                SoundUtil.playSound("note.pling");
            }
        }
    }

    private void drawTrapLine(float x, float y) {
        int white = 0xFFFFFFFF;
        int green = 0xFF55FF55;
        int red = 0xFFFF5555;
        boolean shadow = this.hudShadow.getValue();
        String prefix = "- Trap: ";
        mc.fontRendererObj.drawString(prefix, x, y, white, shadow);
        float valueX = x + mc.fontRendererObj.getStringWidth(prefix);
        String value = this.trap ? (this.trapType.isEmpty() ? "Unknown" : this.trapType) : "false";
        mc.fontRendererObj.drawString(value, valueX, y, this.trap ? green : red, shadow);
    }

    private String parseTrapType(String lower) {
        if (lower.contains("alarm"))
            return "Alarm";
        if (lower.contains("miner fatigue") || lower.contains("miner"))
            return "Miner Fatigue";
        if (lower.contains("counter-offensive") || lower.contains("counter offensive") || lower.contains("counter"))
            return "Counter-Offensive";
        if (lower.contains("it's a trap") || lower.contains("its a trap"))
            return "It's a Trap";
        return "Unknown";
    }

    private String toRoman(int level) {
        switch (level) {
            case 1:
                return "I";
            case 2:
                return "II";
            case 3:
                return "III";
            case 4:
                return "IV";
            default:
                return String.valueOf(level);
        }
    }

    private void reset(boolean resetDiamondUpgrades) {
        if (resetDiamondUpgrades) {
            this.trap = false;
            this.trapType = "";
            this.sharp = false;
            this.protLevel = 0;
        }
        this.trackedItemMessages.clear();
        this.invisAlertCooldowns.clear();
    }

    private void sendItemTrackerMessage(String formattedPlayer, String item) {
        if (mc.thePlayer == null) {
            return;
        }
        mc.thePlayer.addChatMessage(
                new ChatComponentText(this.getMyauPrefix() + " §f" + formattedPlayer + " §fhas §a" + item));
    }

    private String getMyauPrefix() {
        return ChatColors.formatColor(Myau.clientName).trim();
    }

    private class BedTracker extends Module {
        private static final long BED_SCAN_DELAY_MS = 3000L;
        private static final long BED_RESCAN_DELAY_MS = 5000L;
        private final LinkedHashMap<String, Long> alertCooldowns;
        private final LinkedHashSet<EntityEnderPearl> trackedPearls;
        private final LinkedHashSet<String> whitelistedPlayers;
        private final LinkedHashSet<String> autoIncPlayers;
        private final Color wBed;
        private final Color rBed;
        private final Color yBed;
        private final Color gBed;
        private BlockPos bedPos;
        private long lastMarcoTime;
        private boolean waiting;
        private long bedScanAt;
        private boolean scannedThisGame;
        public final BooleanProperty alerts;
        public final IntProperty alertRange;
        public final BooleanProperty alertOnPearl;
        public final ModeProperty alertSound;
        public final IntProperty alertFrequency;
        public final BooleanProperty autoInc;
        public final BooleanProperty marco;
        public final IntProperty marcoRange;
        public final BooleanProperty marcoOnPreal;
        public final TextProperty marcoText;
        public final IntProperty marcoDelay;
        public final BooleanProperty hud;
        public final ModeProperty hudPosX;
        public final ModeProperty hudPosY;
        public final IntProperty hudOffX;
        public final IntProperty hudOffY;
        public final FloatProperty hudScale;
        public final BooleanProperty hudShadow;

        private void playAlertSound() {
            switch (this.alertSound.getValue()) {
                case 1:
                    SoundUtil.playSound("mob.cat.meow");
                    break;
                case 2:
                    SoundUtil.playSound("random.anvil_land");
            }
        }

        private Color getHudColor(int distance) {
            if (distance < 0) {
                return this.wBed;
            } else if (distance <= 100) {
                return this.gBed;
            } else if (distance <= 114) {
                return ColorUtil.interpolate((float) (114 - distance) / 14.0F, this.yBed, this.gBed);
            } else {
                return distance <= 128 ? ColorUtil.interpolate((float) (128 - distance) / 14.0F, this.rBed, this.yBed)
                        : this.rBed;
            }
        }

        private boolean isBed(BlockPos blockPos) {
            return blockPos != null && mc.theWorld != null
                    && mc.theWorld.getBlockState(blockPos).getBlock() == Blocks.bed;
        }

        public BedTracker() {
            super("BedTracker", false, true);
            this.alertCooldowns = new LinkedHashMap<>();
            this.trackedPearls = new LinkedHashSet<>();
            this.whitelistedPlayers = new LinkedHashSet<>();
            this.autoIncPlayers = new LinkedHashSet<>();
            this.wBed = new Color(ChatColors.WHITE.toAwtColor());
            this.rBed = new Color(ChatColors.RED.toAwtColor());
            this.yBed = new Color(ChatColors.YELLOW.toAwtColor());
            this.gBed = new Color(ChatColors.GREEN.toAwtColor());
            this.bedPos = null;
            this.lastMarcoTime = -1L;
            this.waiting = false;
            this.bedScanAt = -1L;
            this.scannedThisGame = false;
            this.alerts = new BooleanProperty("alerts", true, BedwarUtils.this.bedTracker::getValue);
            this.alertRange = new IntProperty("alerts-range", 48, 8, 128,
                    () -> BedwarUtils.this.bedTracker.getValue() && this.alerts.getValue());
            this.alertOnPearl = new BooleanProperty("alerts-on-pearl", true, BedwarUtils.this.bedTracker::getValue);
            this.alertSound = new ModeProperty("alerts-sound", 1, new String[] { "NONE", "MEOW", "ANVIL" },
                    () -> BedwarUtils.this.bedTracker.getValue()
                            && (this.alerts.getValue() || this.alertOnPearl.getValue()));
            this.alertFrequency = new IntProperty("alerts-frequency", 5, 1, 30,
                    () -> BedwarUtils.this.bedTracker.getValue()
                            && (this.alerts.getValue() || this.alertOnPearl.getValue()));
            this.autoInc = new BooleanProperty("auto-inc", false, BedwarUtils.this.bedTracker::getValue);
            this.marco = new BooleanProperty("macro", false, BedwarUtils.this.bedTracker::getValue);
            this.marcoRange = new IntProperty("macro-range", 24, 8, 128,
                    () -> BedwarUtils.this.bedTracker.getValue() && this.marco.getValue());
            this.marcoOnPreal = new BooleanProperty("macro-on-pearl", false, BedwarUtils.this.bedTracker::getValue);
            this.marcoText = new TextProperty("macro-text", "/lobby",
                    () -> BedwarUtils.this.bedTracker.getValue()
                            && (this.marco.getValue() || this.marcoOnPreal.getValue()));
            this.marcoDelay = new IntProperty("macro-delay", 1, 1, 10,
                    () -> BedwarUtils.this.bedTracker.getValue()
                            && (this.marco.getValue() || this.marcoOnPreal.getValue()));
            this.hud = new BooleanProperty("hud", true, BedwarUtils.this.bedTracker::getValue);
            this.hudPosX = new ModeProperty("hud-position-x", 0, new String[] { "LEFT", "MIDDLE", "RIGHT" },
                    () -> BedwarUtils.this.bedTracker.getValue() && this.hud.getValue());
            this.hudPosY = new ModeProperty("hud-position-y", 0, new String[] { "TOP", "MIDDLE", "BOTTOM" },
                    () -> BedwarUtils.this.bedTracker.getValue() && this.hud.getValue());
            this.hudOffX = new IntProperty("hud-offset-x", 2, 0, 255,
                    () -> BedwarUtils.this.bedTracker.getValue() && this.hud.getValue());
            this.hudOffY = new IntProperty("hud-offset-y", 2, 0, 255,
                    () -> BedwarUtils.this.bedTracker.getValue() && this.hud.getValue());
            this.hudScale = new FloatProperty("hud-scale", 1.0F, 0.5F, 1.5F,
                    () -> BedwarUtils.this.bedTracker.getValue() && this.hud.getValue());
            this.hudShadow = new BooleanProperty("hud-shadow", true,
                    () -> BedwarUtils.this.bedTracker.getValue() && this.hud.getValue());
        }

        private void resetTracking() {
            this.alertCooldowns.clear();
            this.trackedPearls.clear();
            this.whitelistedPlayers.clear();
            this.autoIncPlayers.clear();
            this.bedPos = null;
            this.lastMarcoTime = -1L;
        }

        private BlockPos getBedPos() {
            return this.bedPos;
        }

        private void scheduleBedScan() {
            if (!this.scannedThisGame && this.bedScanAt == -1L) {
                this.bedScanAt = System.currentTimeMillis() + BED_SCAN_DELAY_MS;
            }
        }

        private void scheduleAutomaticBedScan() {
            if (this.scannedThisGame || mc.theWorld == null || mc.thePlayer == null || this.isBed(this.bedPos))
                return;
            if (this.bedScanAt == -1L) {
                this.bedScanAt = System.currentTimeMillis() + BED_SCAN_DELAY_MS;
            }
        }

        private void runPendingBedScan() {
            if (this.bedScanAt == -1L || System.currentTimeMillis() < this.bedScanAt) {
                return;
            }
            this.bedScanAt = -1L;
            if (mc.theWorld == null || mc.thePlayer == null) {
                this.bedScanAt = System.currentTimeMillis() + BED_RESCAN_DELAY_MS;
                return;
            }
            int x = MathHelper.floor_double(mc.thePlayer.posX);
            int y = MathHelper.floor_double(mc.thePlayer.posY + (double) mc.thePlayer.getEyeHeight());
            int z = MathHelper.floor_double(mc.thePlayer.posZ);
            for (int i = x - 25; i <= x + 25; i++) {
                for (int j = y - 25; j <= y + 25; j++) {
                    for (int k = z - 25; k <= z + 25; k++) {
                        BlockPos blockPos = new BlockPos(i, j, k);
                        if (this.isBed(blockPos)) {
                            this.bedPos = blockPos;
                            this.scannedThisGame = true;
                            ChatUtil.display(
                                    String.format(
                                            "%s%s: &fWhitelisted your bed at (%d, %d, %d) &a&l\u2714&r",
                                            Myau.clientName,
                                            this.getName(),
                                            this.bedPos.getX(),
                                            this.bedPos.getY(),
                                            this.bedPos.getZ()));
                            SoundUtil.playSound("note.pling");
                            return;
                        }
                    }
                }
            }
            this.bedScanAt = System.currentTimeMillis() + BED_RESCAN_DELAY_MS;
        }

        private void pruneTrackedPearls() {
            if (mc.theWorld == null) {
                this.trackedPearls.clear();
                return;
            }
            Iterator<EntityEnderPearl> iterator = this.trackedPearls.iterator();
            while (iterator.hasNext()) {
                EntityEnderPearl pearl = iterator.next();
                if (pearl.isDead || !mc.theWorld.loadedEntityList.contains(pearl)) {
                    iterator.remove();
                }
            }
        }

        @EventTarget
        public void onTick(TickEvent event) {
            if (this.isEnabled() && event.getType() == EventType.POST) {
                this.scheduleAutomaticBedScan();
                this.runPendingBedScan();
                this.pruneTrackedPearls();
                if (!this.isBed(this.bedPos)) {
                    return;
                }
                long millis = System.currentTimeMillis();
                boolean pearl = false;
                boolean marco = false;
                for (Entity entity : mc.theWorld.loadedEntityList) {
                    if (entity instanceof EntityEnderPearl) {
                        EntityEnderPearl enderPearl = (EntityEnderPearl) entity;
                        if (!this.trackedPearls.contains(enderPearl)) {
                            this.trackedPearls.add(enderPearl);
                            if (this.alertOnPearl.getValue()) {
                                ChatUtil.display("%s%s: &fDetected &5Ender Pearl&r &e&l⚠&r",
                                        Myau.clientName, this.getName());
                                pearl = true;
                            }
                            if (this.marcoOnPreal.getValue()
                                    && this.lastMarcoTime + (long) this.marcoDelay.getValue() * 1000L <= millis) {
                                this.lastMarcoTime = millis;
                                marco = true;
                            }
                        }
                    }
                }
                for (EntityPlayer player : mc.theWorld.loadedEntityList
                        .stream()
                        .filter(entity -> entity instanceof EntityPlayer)
                        .map(entity -> (EntityPlayer) entity)
                        .filter(entityPlayer -> !TeamUtil.isBot(entityPlayer)
                                && !this.whitelistedPlayers.contains(entityPlayer.getName()))
                        .collect(Collectors.toList())) {
                    if (TeamUtil.isSameTeam(player)) {
                        this.whitelistedPlayers.add(player.getName());
                    } else {
                        double distance = player.getDistance((double) this.bedPos.getX() + 0.5,
                                (double) this.bedPos.getY() + 0.5, (double) this.bedPos.getZ() + 0.5);
                        String name = player.getName();
                        String text = player.getDisplayName().getFormattedText();
                        ItemStack item = player.getHeldItem();
                        boolean isPearl = item != null && item.getItem() instanceof ItemEnderPearl;
                        if (this.alerts.getValue() && distance < (double) this.alertRange.getValue()) {
                            Long cooldown = this.alertCooldowns.get(name);
                            if (cooldown == null
                                    || cooldown + (long) this.alertFrequency.getValue() * 1000L <= millis) {
                                this.alertCooldowns.put(name, millis);
                                ChatUtil.display(
                                        String.format("%s%s: %s&r &fis %d blocks away from your bed &e&l⚠&r",
                                                Myau.clientName, this.getName(), text, (int) distance + 1));
                                pearl = true;
                            }
                            if (this.autoInc.getValue() && this.autoIncPlayers.add(name.toLowerCase())) {
                                ChatUtil.sendMessage(this.getIncMessage(player));
                            }
                        }
                        if (this.alertOnPearl.getValue() && isPearl) {
                            Long cooldown = this.alertCooldowns.get(name);
                            if (cooldown == null
                                    || cooldown + (long) this.alertFrequency.getValue() * 1000L <= millis) {
                                this.alertCooldowns.put(name, millis);
                                ChatUtil.display(
                                        String.format("%s%s: %s&r &fhas &5Ender Pearl&r &e&l⚠&r",
                                                this.getName(), text));
                                pearl = true;
                            }
                        }
                        if ((this.marco.getValue() && distance < (double) this.marcoRange.getValue()
                                || this.marcoOnPreal.getValue() && isPearl)
                                && this.lastMarcoTime + (long) this.marcoDelay.getValue() * 1000L <= millis) {
                            this.lastMarcoTime = millis;
                            marco = true;
                        }
                    }
                }
                if (pearl) {
                    this.playAlertSound();
                }
                if (marco) {
                    ChatUtil.sendRaw(
                            String.format(
                                    ChatColors.formatColor("%s%s: &fRunning &6%s&r"),
                                    ChatColors.formatColor(Myau.clientName),
                                    this.getName(),
                                    this.marcoText.getValue()));
                    ChatUtil.sendMessage(this.marcoText.getValue());
                }
            }
        }

        private String getIncMessage(EntityPlayer player) {
            String team = this.getTeamName(player);
            return team.isEmpty() ? "inc" : team + " inc";
        }

        private String getTeamName(EntityPlayer player) {
            String formatted = player.getDisplayName().getFormattedText().toLowerCase();
            if (formatted.contains("§c") || formatted.contains("red"))
                return "red";
            if (formatted.contains("§e") || formatted.contains("yellow"))
                return "yellow";
            if (formatted.contains("§a") || formatted.contains("green"))
                return "green";
            if (formatted.contains("§9") || formatted.contains("blue"))
                return "blue";
            if (formatted.contains("§b") || formatted.contains("aqua"))
                return "aqua";
            if (formatted.contains("§f") || formatted.contains("white"))
                return "white";
            if (formatted.contains("§d") || formatted.contains("pink"))
                return "pink";
            if (formatted.contains("§7") || formatted.contains("gray") || formatted.contains("grey"))
                return "gray";
            return "";
        }

        @EventTarget(Priority.LOW)
        public void onRender(Render2DEvent event) {
            if (this.isEnabled() && this.hud.getValue()) {
                if (mc.theWorld != null && mc.thePlayer != null && !mc.gameSettings.showDebugInfo) {
                    GuiScreen currentScreen = mc.currentScreen;
                    if (currentScreen == null || currentScreen instanceof GuiChat) {
                        int distanceSq = 0;
                        boolean hasBed = this.isBed(this.bedPos);
                        if (hasBed) {
                            double xDiff = mc.thePlayer.posX - (double) this.bedPos.getX();
                            double zDiff = mc.thePlayer.posZ - (double) this.bedPos.getZ();
                            distanceSq = (int) Math.sqrt(xDiff * xDiff + zDiff * zDiff) + 1;
                        }
                        String text = ChatColors.formatColor(
                                String.format(
                                        "&fBed: %s%s",
                                        !hasBed ? "&cfalse&r" : "&atrue&r",
                                        !hasBed ? ""
                                                : String.format(" &7| &fDistance: &r%d%s", distanceSq,
                                                distanceSq >= 128 ? " &c&l⚠&r" : "")));
                        ScaledResolution scaledResolution = new ScaledResolution(mc);
                        float width = (float) mc.fontRendererObj.getStringWidth(text);
                        float height = (float) mc.fontRendererObj.FONT_HEIGHT - 1.0F;
                        float scale = (float) this.hudOffX.getValue() / this.hudScale.getValue();
                        switch (this.hudPosX.getValue()) {
                            case 0:
                                scale++;
                                break;
                            case 1:
                                scale += (float) scaledResolution.getScaledWidth() / this.hudScale.getValue() / 2.0F
                                        - width / 2.0F;
                                break;
                            case 2:
                                scale = (scale + 1.0F) * -1.0F;
                                scale += (float) scaledResolution.getScaledWidth() / this.hudScale.getValue() - width;
                        }
                        float offset = (float) this.hudOffY.getValue() / this.hudScale.getValue();
                        switch (this.hudPosY.getValue()) {
                            case 0:
                                offset++;
                                break;
                            case 1:
                                offset += (float) scaledResolution.getScaledHeight() / this.hudScale.getValue() / 2.0F
                                        - height / 2.0F;
                                break;
                            case 2:
                                offset = (offset + 1.0F) * -1.0F;
                                offset += (float) scaledResolution.getScaledHeight() / this.hudScale.getValue()
                                        - height;
                        }
                        GlStateManager.pushMatrix();
                        GlStateManager.scale(this.hudScale.getValue(), this.hudScale.getValue(), 1.0F);
                        GlStateManager.translate(scale, offset, 0.0F);
                        GlStateManager.disableDepth();
                        GlStateManager.enableBlend();
                        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                        mc.fontRendererObj.drawString(text, 0.0F, 0.0F, this.getHudColor(distanceSq).getRGB(),
                                this.hudShadow.getValue());
                        GlStateManager.disableBlend();
                        GlStateManager.enableDepth();
                        GlStateManager.popMatrix();
                    }
                }
            }
        }

        @EventTarget
        public void onLoadWorld(LoadWorldEvent event) {
            this.waiting = false;
            this.bedScanAt = -1L;
            this.scannedThisGame = false;
            this.resetTracking();
        }

        @EventTarget
        public void onPacket(PacketEvent event) {
            if (this.isEnabled()) {
                if (event.getPacket() instanceof S02PacketChat) {
                    String msg = ((S02PacketChat) event.getPacket()).getChatComponent().getFormattedText();
                    if (msg.contains("§e§lProtect your bed and destroy the enemy bed")
                            || msg.contains("§e§lDestroy the enemy bed and then eliminate them")) {
                        this.bedScanAt = -1L;
                        this.resetTracking();
                        this.scannedThisGame = false;
                        this.waiting = true;
                    }
                }
                if (event.getPacket() instanceof S08PacketPlayerPosLook && this.waiting) {
                    this.waiting = false;
                    this.scheduleBedScan();
                }
            }
        }

        @Override
        public void onDisabled() {
            this.waiting = false;
            this.bedScanAt = -1L;
            this.scannedThisGame = false;
            this.resetTracking();
        }
    }

}