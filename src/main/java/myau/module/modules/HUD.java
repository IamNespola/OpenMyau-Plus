package myau.module.modules;

import myau.Myau;
import myau.enums.BlinkModules;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.Render2DEvent;
import myau.events.TickEvent;
import myau.font.CFontRenderer;
import myau.font.FontProcess;
import myau.mixin.IAccessorGuiChat;
import myau.module.Module;
import myau.util.ColorUtil;
import myau.util.RenderUtil;
import myau.property.properties.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class HUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public static int targetHUDX = 100;
    public static int targetHUDY = 100;

    public static void setTargetHUDPosition(int x, int y) {
        targetHUDX = x;
        targetHUDY = y;
    }

    public static void resetTargetHUDPosition() {
        targetHUDX = 100;
        targetHUDY = 100;
    }

    private static final Set<Class<?>> RENDER_MODULES = new HashSet<>(Arrays.<Class<?>>asList(
            ESP.class, Chams.class, FullBright.class, Tracers.class, NameTags.class, Xray.class,
            TargetESP.class, TargetHUD.class, Indicators.class, BedESP.class, ItemESP.class,
            ViewClip.class, NoHurtCam.class, HUD.class, GuiModule.class, RiseClickGUIModule.class,
            ChestESP.class, Trajectories.class, Radar.class, RenderFixes.class, FPScounter.class,
            WaterMark.class, WaterMark2.class, HitParticleEffects.class, DynamicIsland.class,
            ESP2D.class, TeamHealthDisplay.class, SeasonDisplay.class, Animations.class
    ));
    private static final Set<Class<?>> PLAYER_MODULES = new HashSet<>(Arrays.<Class<?>>asList(
            AutoHeal.class, FakeLag.class, AutoTool.class, ChestStealer.class, InvManager.class,
            InvWalk.class, Scaffold.class, AutoBlockIn.class, AutoSwap.class, SpeedMine.class,
            FastPlace.class, GhostHand.class, MCF.class, AntiDebuff.class, FlagDetector.class,
            AutoGapple.class, ThrowAura.class, InventoryClicker.class
    ));
    private static final Set<Class<?>> MISC_MODULES = new HashSet<>(Arrays.<Class<?>>asList(
            Spammer.class, BedNuker.class, BedTracker.class, LightningTracker.class, NoRotate.class,
            NickHider.class, AntiObbyTrap.class, AntiObfuscate.class, AutoAnduril.class,
            Disabler.class, ClientSpoofer.class, AutoHypixel.class
    ));
    private List<Module> activeModules = new ArrayList<>();
    public final ModeProperty colorMode = new ModeProperty(
            "color", 3, new String[]{"RAINBOW", "CHROMA", "ASTOLFO", "CUSTOM1", "CUSTOM12", "CUSTOM123"}
    );
    public final FloatProperty colorSpeed = new FloatProperty("color-speed", 1.0F, 0.5F, 1.5F);
    public final PercentProperty colorSaturation = new PercentProperty("color-saturation", 50);
    public final PercentProperty colorBrightness = new PercentProperty("color-brightness", 100);
    public final ColorProperty custom1 = new ColorProperty("custom-color-1", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 3 || this.colorMode.getValue() == 4 || this.colorMode.getValue() == 5);
    public final ColorProperty custom2 = new ColorProperty("custom-color-2", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 4 || this.colorMode.getValue() == 5);
    public final ColorProperty custom3 = new ColorProperty("custom-color-3", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 5);
    public final ModeProperty posX = new ModeProperty("position-x", 0, new String[]{"LEFT", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 0, new String[]{"TOP", "BOTTOM"});
    public final IntProperty offsetX = new IntProperty("offset-x", 2, 0, 255);
    public final IntProperty offsetY = new IntProperty("offset-y", 2, 0, 255);
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final ModeProperty interfaceMode = new ModeProperty("interface", 0, new String[]{"MYAU", "CREIDA"});
    public final PercentProperty background = new PercentProperty("background", 25);
    public final IntProperty bgAlpha = new IntProperty("bg-alpha", 120, 0, 255);
    public final BooleanProperty blur = new BooleanProperty("blur", false);
    public final FloatProperty blurRadius = new FloatProperty("blur-radius", 10.0F, 1.0F, 30.0F, this.blur::getValue);
    public final BooleanProperty glow = new BooleanProperty("glow", false);
    public final ModeProperty glowColorMode = new ModeProperty("glow-color", 0, new String[]{"SYNC", "CUSTOM"}, this.glow::getValue);
    public final ColorProperty glowCustomColor = new ColorProperty("glow-custom-color", Color.WHITE.getRGB() & 0xFFFFFF, () -> this.glow.getValue() && this.glowColorMode.getValue() == 1);
    public final IntProperty glowAlpha = new IntProperty("glow-alpha", 100, 0, 255, this.glow::getValue);
    public final FloatProperty glowRadius = new FloatProperty("glow-radius", 3.0F, 1.0F, 15.0F, this.glow::getValue);
    public final BooleanProperty showBar = new BooleanProperty("bar", true);
    public final ModeProperty sidebarMode = new ModeProperty("sidebar-mode", 0, new String[]{"RIGHT", "LEFT", "TOP", "OUTLINE", "NONE"}, this.showBar::getValue);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final FloatProperty colorDistance = new FloatProperty("color-dist", 50F, 10F, 100F);
    public final BooleanProperty suffixes = new BooleanProperty("suffixes", true);
    public final ModeProperty separatorMode = new ModeProperty("separator-mode", 0, new String[]{"SPACE", "-", "!", "[]", "{}", "()", "\"\""}, this.suffixes::getValue);
    public final BooleanProperty lowerCase = new BooleanProperty("lower-case", false);
    public final BooleanProperty hideRender = new BooleanProperty("hide-render", false);
    public final BooleanProperty hidePlayer = new BooleanProperty("hide-player", false);
    public final BooleanProperty hideMisc = new BooleanProperty("hide-misc", false);
    public final BooleanProperty chatOutline = new BooleanProperty("chat-outline", true);
    public final BooleanProperty blinkTimer = new BooleanProperty("blink-timer", true);
    public final BooleanProperty notifications = new BooleanProperty("notifications", true);
    public final BooleanProperty toggleSound = new BooleanProperty("toggle-sounds", true);
    public final BooleanProperty toggleAlerts = new BooleanProperty("toggle-alerts", false);
    public final ModeProperty fontMode = new ModeProperty("font-mode", 0, new String[]{"SANS", "MINECRAFT", "NUNITO"});
    public final BooleanProperty creidaFont = new BooleanProperty("creida-font", true, () -> this.interfaceMode.getValue() == 1);
    public final BooleanProperty creidaWatermark = new BooleanProperty("creida-watermark", true, () -> this.interfaceMode.getValue() == 1);
    public final BooleanProperty rounded = new BooleanProperty("rounded", true);
    public final FloatProperty cornerRadius = new FloatProperty("corner-radius", 4.0F, 1.0F, 8.0F, () -> rounded.getValue());
    public final FloatProperty padding = new FloatProperty("padding", 2.0F, 0.0F, 6.0F);

    private String getModuleName(Module module) {
        String moduleName = module.getName();
        if (this.lowerCase.getValue()) {
            moduleName = moduleName.toLowerCase(Locale.ROOT);
        }
        return moduleName;
    }

    CFontRenderer fontRenderer;
    private CFontRenderer nunitoFontRenderer;
    private final net.minecraft.client.gui.FontRenderer mcFont = mc.fontRendererObj;
    private int lastFontMode = -1; // Cache to prevent unnecessary updates

    private String[] getModuleSuffix(Module module) {
        String[] moduleSuffix = module.getSuffix();
        if (this.lowerCase.getValue()) {
            for (int i = 0; i < moduleSuffix.length; i++) {
                moduleSuffix[i] = moduleSuffix[i].toLowerCase();
            }
        }
        return moduleSuffix;
    }


    private int getModuleWidth(Module module) {
        if (this.interfaceMode.getValue() == 1) {
            return Math.round(this.getCreidaModuleWidth(module));
        }
        return this.calculateStringWidth(
                this.getModuleName(module), this.getModuleSuffix(module)
        );
    }

    private int calculateStringWidth(String string, String[] arr) {
        int width;
        switch (fontMode.getValue()) {
            case 0: // SANS
                width = fontRenderer.getStringWidth(string);
                break;
            case 1: // MINECRAFT
                width = mcFont.getStringWidth(string);
                break;
            default:
                width = fontRenderer.getStringWidth(string);
                break;
        }
        if (this.suffixes.getValue()) {
            for (String str : arr) {
                width += getStringWidth(this.formatSuffix(str));
            }
        }
        return width;
    }

    private int getStringWidth(String string) {
        switch (fontMode.getValue()) {
            case 1:
                return mcFont.getStringWidth(string);
            default:
                return fontRenderer.getStringWidth(string);
        }
    }

    private String formatSuffix(String suffix) {
        switch (this.separatorMode.getValue()) {
            case 1:
                return " - " + suffix;
            case 2:
                return " ! " + suffix;
            case 3:
                return " [" + suffix + "]";
            case 4:
                return " {" + suffix + "}";
            case 5:
                return " (" + suffix + ")";
            case 6:
                return " \"" + suffix + "\"";
            default:
                return " " + suffix;
        }
    }

    private float getColorCycle(long long3, long long4) {
        long speed = (long) (3000.0 / Math.pow(Math.min(Math.max(0.5F, this.colorSpeed.getValue()), 1.5F), 3.0));
        return 1.0F - (float) (Math.abs(long3 - long4 * 300L) % speed) / (float) speed;
    }

    public HUD() {
        super("HUD", true, true, "Wdym It HUD u never know it :?");
        updateFontRenderer();
    }

    private void updateFontRenderer() {
        // Only update if font mode actually changed
        if (lastFontMode == fontMode.getValue()) {
            return;
        }

        CFontRenderer selectedFont;
        switch (fontMode.getValue()) {
            case 0: // SANS
                selectedFont = FontProcess.getFont("sans");
                break;
            case 1: // MINECRAFT - handled separately
                selectedFont = FontProcess.getFont("sans");
                break;
            case 2: // NUNITO
                selectedFont = getNunitoFontRenderer();
                break;
            default:
                selectedFont = FontProcess.getFont("sans");
                break;
        }

        if (selectedFont == null) {
            System.err.println("[Myau] Failed to resolve HUD font mode: " + fontMode.getModeString());
            fontRenderer = FontProcess.getFont("sans");
            lastFontMode = -1;
            return;
        }

        fontRenderer = selectedFont;
        lastFontMode = fontMode.getValue();
    }

    private CFontRenderer getNunitoFontRenderer() {
        if (nunitoFontRenderer == null) {
            nunitoFontRenderer = new CFontRenderer("nunito", 18, Font.PLAIN, true, false);
            System.out.println("[Myau] HUD Nunito loaded as: " + nunitoFontRenderer.getFont().getFontName());
        }
        return nunitoFontRenderer;
    }

    public Color getColor(long time) {
        return this.getColor(time, 0L);
    }

    public Color getColor(long time, long offset) {
        Color color = Color.white;
        switch (this.colorMode.getValue()) {
            case 0:
                color = ColorUtil.fromHSB(this.getColorCycle(time, offset), 1.0F, 1.0F);
                break;
            case 1:
                color = ColorUtil.fromHSB(this.getColorCycle(time / 3L, 0L), 1.0F, 1.0F);
                break;
            case 2:
                float cycle = this.getColorCycle(time, offset);
                if (cycle % 1.0F < 0.5F) {
                    cycle = 1.0F - cycle % 1.0F;
                }
                color = ColorUtil.fromHSB(cycle, 1.0F, 1.0F);
                break;
            case 3:
                color = new Color(this.custom1.getValue());
                break;
            case 4:
                double cycle1 = this.getColorCycle(time, offset);
                color = ColorUtil.interpolate(
                        (float) (2.0 * Math.abs(cycle1 - Math.floor(cycle1 + 0.5))),
                        new Color(this.custom1.getValue()),
                        new Color(this.custom2.getValue())
                );
                break;
            case 5:
                double cycle2 = this.getColorCycle(time, offset);
                float floor = (float) (2.0 * Math.abs(cycle2 - Math.floor(cycle2 + 0.5)));
                if (floor <= 0.5F) {
                    color = ColorUtil.interpolate(floor * 2.0F, new Color(this.custom1.getValue()), new Color(this.custom2.getValue()));
                } else {
                    color = ColorUtil.interpolate((floor - 0.5F) * 2.0F, new Color(this.custom2.getValue()), new Color(this.custom3.getValue()));
                }
        }
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return Color.getHSBColor(
                hsb[0],
                hsb[1] * (this.colorSaturation.getValue().floatValue() / 100.0F),
                hsb[2] * (this.colorBrightness.getValue().floatValue() / 100.0F)
        );
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.POST) {
            this.activeModules = Myau.moduleManager.modules.values().stream().filter(this::shouldShowInArraylist).sorted(Comparator.comparingInt(this::getModuleWidth).reversed()).collect(Collectors.<Module>toList());
        }
    }

    private boolean shouldShowInArraylist(Module module) {
        return module != null && module.isEnabled() && !module.isHidden() && !isHiddenByArraylistCategory(module);
    }

    private boolean isHiddenByArraylistCategory(Module module) {
        Class<?> moduleClass = module.getClass();
        return (this.hideRender.getValue() && RENDER_MODULES.contains(moduleClass))
                || (this.hidePlayer.getValue() && PLAYER_MODULES.contains(moduleClass))
                || (this.hideMisc.getValue() && MISC_MODULES.contains(moduleClass));
    }

    private boolean hasSidebar() {
        return this.showBar.getValue() && this.sidebarMode.getValue() != 4;
    }

    private float getModuleRenderWidth(Module module) {
        return (float) this.calculateStringWidth(this.getModuleName(module), this.getModuleSuffix(module));
    }

    private void renderSidebar(Module module, int index, float bgX1, float bgY1, float bgX2, float bgY2, int color) {
        if (!this.hasSidebar()) {
            return;
        }

        float thickness = 1.0F;
        boolean first = index == 0;
        boolean last = index == this.activeModules.size() - 1;
        boolean topList = this.posY.getValue() == 0;
        boolean visualFirst = topList ? first : last;
        boolean visualLast = topList ? last : first;

        switch (this.sidebarMode.getValue()) {
            case 1:
                drawVerticalSidebar(bgX1 - thickness, bgY1, bgY2, thickness, color);
                break;
            case 2:
                if (visualFirst) {
                    RenderUtil.drawRect(bgX1, bgY1 - thickness, bgX2, bgY1, color);
                }
                break;
            case 3:
                drawVerticalSidebar(bgX2, bgY1, bgY2, thickness, color);
                drawVerticalSidebar(bgX1 - thickness, bgY1, bgY2, thickness, color);
                if (visualFirst) {
                    RenderUtil.drawRect(bgX1 - thickness, bgY1 - thickness, bgX2 + thickness, bgY1, color);
                } else {
                    renderOutlineConnector(module, index, bgX1, bgY1, bgX2, bgY2, thickness, color);
                }
                if (visualLast) {
                    RenderUtil.drawRect(bgX1 - thickness, bgY2, bgX2 + thickness, bgY2 + thickness, color);
                }
                break;
            default:
                drawVerticalSidebar(bgX2, bgY1, bgY2, thickness, color);
                break;
        }
    }

    private void renderOutlineConnector(Module module, int index, float bgX1, float bgY1, float bgX2, float bgY2, float thickness, int color) {
        if (index <= 0) {
            return;
        }

        Module previous = this.activeModules.get(index - 1);
        float previousWidth = this.getModuleRenderWidth(previous);
        float rectExtraWidth = 2.0F + this.padding.getValue() * 2.0F;
        float boundaryY1 = this.posY.getValue() == 0 ? bgY1 - thickness : bgY2;
        float boundaryY2 = this.posY.getValue() == 0 ? bgY1 : bgY2 + thickness;

        if (this.posX.getValue() == 0) {
            float previousRight = bgX1 + previousWidth + rectExtraWidth;
            float start = Math.min(previousRight, bgX2);
            float end = Math.max(previousRight, bgX2) + thickness;
            RenderUtil.drawRect(start, boundaryY1, end, boundaryY2, color);
        } else {
            float previousLeft = bgX2 - previousWidth - rectExtraWidth;
            float start = Math.min(previousLeft, bgX1) - thickness;
            float end = Math.max(previousLeft, bgX1);
            RenderUtil.drawRect(start, boundaryY1, end, boundaryY2, color);
        }
    }

    private void drawVerticalSidebar(float x, float y1, float y2, float width, int color) {
        RenderUtil.drawRect(x, y1, x + width, y2, color);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        // Update font renderer to ensure it uses current setting
        updateFontRenderer();
        
        if (this.chatOutline.getValue() && mc.currentScreen instanceof GuiChat) {
            String text = ((IAccessorGuiChat) mc.currentScreen).getInputField().getText().trim();
            if (Myau.commandManager != null && Myau.commandManager.isTypingCommand(text)) {
                RenderUtil.enableRenderState();
                RenderUtil.drawOutlineRect(
                        2.0F,
                        (float) (mc.currentScreen.height - 14),
                        (float) (mc.currentScreen.width - 2),
                        (float) (mc.currentScreen.height - 2),
                        1.5F,
                        0,
                        this.getColor(System.currentTimeMillis()).getRGB()
                );
                RenderUtil.disableRenderState();
            }
        }
        if (this.isEnabled() && !mc.gameSettings.showDebugInfo) {
            if (this.interfaceMode.getValue() == 1) {
                renderCreidaInterface();
            } else {
            float height = (float) fontRenderer.FONT_HEIGHT - 1.0F;
            float x = (float) this.offsetX.getValue()
                    + (1.0F + (this.hasSidebar() ? 1.0F : 0.0F)) * this.scale.getValue();
            float y = (float) this.offsetY.getValue() + 1.0F * this.scale.getValue();
            if (this.posX.getValue() == 1) {
                x = (float) new ScaledResolution(mc).getScaledWidth() - x;
            }
            if (this.posY.getValue() == 1) {
                y = (float) new ScaledResolution(mc).getScaledHeight() - y - height * this.scale.getValue();
            }
            GlStateManager.pushMatrix();
            GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0F);


            long l = System.currentTimeMillis();
            long offset = 0L;
            for (Module module : this.activeModules) {
                String moduleName = this.getModuleName(module);
                String[] moduleSuffix = this.getModuleSuffix(module);
                float totalWidth = this.getModuleRenderWidth(module);
                int color = this.getColor(l, offset).getRGB();
                float pad = this.padding.getValue();
                float bgX1 = x / this.scale.getValue() - 1.0F - pad - (this.posX.getValue() == 0 ? 0.0F : totalWidth);
                float bgY1 = y / this.scale.getValue() - pad - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : (this.shadow.getValue() ? 1.0F : 0.0F));
                float bgX2 = x / this.scale.getValue() + 1.0F + pad + (this.posX.getValue() == 0 ? totalWidth : 0.0F);
                float bgY2 = y / this.scale.getValue() + height + pad + (this.posY.getValue() == 0 ? (this.shadow.getValue() ? 1.0F : 0.0F) : (offset == 0L ? 1.0F : 0.0F));
                RenderUtil.enableRenderState();
                if (this.background.getValue() > 0) {
                    int bgColor = new Color(0.0F, 0.0F, 0.0F, this.background.getValue().floatValue() / 100.0F).getRGB();
                    if (this.rounded.getValue()) {
                        float bgW = bgX2 - bgX1;
                        float bgH = bgY2 - bgY1;
                        float rad = this.cornerRadius.getValue();
                        boolean isFirst = (offset == 0L);
                        boolean isLast = (offset == this.activeModules.size() - 1);
                        boolean sideLeft = this.posX.getValue() == 1;
                        boolean sideRight = this.posX.getValue() == 0;
                        boolean isTopEntry = (this.posY.getValue() == 0) ? isFirst : isLast;
                        boolean isBottomEntry = (this.posY.getValue() == 0) ? isLast : isFirst;
                        RenderUtil.drawRoundedRect(
                                bgX1, bgY1, bgW, bgH, rad, bgColor,
                                sideLeft && isTopEntry, sideRight && isTopEntry,
                                sideLeft && isBottomEntry, sideRight && isBottomEntry
                        );
                    } else {
                        RenderUtil.drawRect(bgX1, bgY1, bgX2, bgY2, bgColor);
                    }
                }
                renderSidebar(module, (int) offset, bgX1, bgY1, bgX2, bgY2, color);
                RenderUtil.disableRenderState();
                GlStateManager.disableDepth();
                if (this.shadow.getValue()) {
                    if (fontMode.getValue() == 1) { // MINECRAFT
                        mcFont.drawStringWithShadow(moduleName, x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F), y / this.scale.getValue(), color);
                    } else {
                        fontRenderer.drawStringWithShadow(moduleName, x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F), y / this.scale.getValue(), color);
                    }
                } else {
                    if (fontMode.getValue() == 1) { // MINECRAFT
                        mcFont.drawString(
                                moduleName,
                                x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F),
                                y / this.scale.getValue() + (this.posY.getValue() == 1 ? 1.0F : 0.0F),
                                color,
                                false
                        );
                    } else {
                        fontRenderer.drawString(
                                moduleName,
                                x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F),
                                y / this.scale.getValue() + (this.posY.getValue() == 1 ? 1.0F : 0.0F),
                                color,
                                false
                        );
                    }
                }
                if (this.suffixes.getValue() && moduleSuffix.length > 0) {
                    float width;
                    switch (fontMode.getValue()) {
                        case 1: // MINECRAFT
                            width = (float) mcFont.getStringWidth(moduleName);
                            break;
                        default:
                            width = (float) fontRenderer.getStringWidth(moduleName);
                            break;
                    }
                    for (String suffix : moduleSuffix) {
                        String string = this.formatSuffix(suffix);
                        if (this.shadow.getValue()) {
                            if (fontMode.getValue() == 1) { // MINECRAFT
                                mcFont.drawStringWithShadow(
                                        string,
                                        x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F) + width,
                                        y / this.scale.getValue(),
                                        ChatColors.GRAY.toAwtColor()
                                );
                            } else {
                                fontRenderer.drawStringWithShadow(
                                        string,
                                        x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F) + width,
                                        y / this.scale.getValue(),
                                        ChatColors.GRAY.toAwtColor()
                                );
                            }
                        } else {
                            if (fontMode.getValue() == 1) { // MINECRAFT
                                mcFont.drawString(
                                        string,
                                        x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F) + width,
                                        y / this.scale.getValue() + (this.posY.getValue() == 1 ? 1.0F : 0.0F),
                                        ChatColors.GRAY.toAwtColor(),
                                        false
                                );
                            } else {
                                fontRenderer.drawString(
                                        string,
                                        x / this.scale.getValue() - (this.posX.getValue() == 1 ? totalWidth : 0.0F) + width,
                                        y / this.scale.getValue() + (this.posY.getValue() == 1 ? 1.0F : 0.0F),
                                        ChatColors.GRAY.toAwtColor(),
                                        false
                                );
                            }
                        }
                        width += this.getStringWidth(string);
                    }
                }
                y += (height + (this.shadow.getValue() ? 1.0F : 0.0F) + this.padding.getValue() * 2.0F) * this.scale.getValue() * (this.posY.getValue() == 0 ? 1.0F : -1.0F);
                offset++;
            }
            if (this.blinkTimer.getValue()) {
                BlinkModules blinkingModule = Myau.blinkManager.getBlinkingModule();
                if (blinkingModule != BlinkModules.NONE && blinkingModule != BlinkModules.AUTO_BLOCK) {
                    long movementPacketSize = Myau.blinkManager.countMovement();
                    if (movementPacketSize > 0L) {
                        GlStateManager.enableBlend();
                        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                        if (fontMode.getValue() == 1) { // MINECRAFT
                            mcFont.drawString(
                                    String.valueOf(movementPacketSize),
                                    (float) new ScaledResolution(mc).getScaledWidth() / 2.0F / this.scale.getValue()
                                            - (float) mcFont.getStringWidth(String.valueOf(movementPacketSize)) / 2.0F,
                                    (float) new ScaledResolution(mc).getScaledHeight() / 5.0F * 3.0F / this.scale.getValue(),
                                    this.getColor(l, offset).getRGB() & 16777215 | -1090519040,
                                    this.shadow.getValue()
                            );
                        } else {
                            fontRenderer.drawString(
                                    String.valueOf(movementPacketSize),
                                    (float) new ScaledResolution(mc).getScaledWidth() / 2.0F / this.scale.getValue()
                                            - (float) fontRenderer.getStringWidth(String.valueOf(movementPacketSize)) / 2.0F,
                                    (float) new ScaledResolution(mc).getScaledHeight() / 5.0F * 3.0F / this.scale.getValue(),
                                    this.getColor(l, offset).getRGB() & 16777215 | -1090519040,
                                    this.shadow.getValue()
                            );
                        }
                        GlStateManager.disableBlend();
                    }
                }
            }
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();


            }
        }
        renderNotifications();
    }

    private void renderCreidaInterface() {
        ScaledResolution sr = new ScaledResolution(mc);
        float hudScale = Math.max(0.5F, Math.min(1.5F, this.scale.getValue()));
        float scaledWidth = sr.getScaledWidth() / hudScale;
        float edgeOffset = Math.max(3.0F, this.offsetX.getValue());
        float right = scaledWidth - edgeOffset;
        float y = Math.max(3.0F, this.offsetY.getValue() + 1.0F);
        float entryHeight = getCreidaEntryHeight();
        long time = System.currentTimeMillis();

        GlStateManager.pushMatrix();
        GlStateManager.scale(hudScale, hudScale, 1.0F);

        if (this.creidaWatermark.getValue()) {
            renderCreidaWatermark(time);
        }

        long offset = 0L;
        for (Module module : this.activeModules) {
            renderCreidaModule(module, right, y, time, offset, entryHeight);
            y += entryHeight;
            offset++;
        }

        GlStateManager.popMatrix();
    }

    private void renderCreidaModule(Module module, float right, float y, long time, long offset, float entryHeight) {
        String moduleName = this.getModuleName(module);
        String[] moduleSuffix = this.getModuleSuffix(module);
        float nameWidth = this.getCreidaTextWidth(moduleName);
        float tagWidth = this.getCreidaTagWidth(moduleSuffix);
        float boxWidth = nameWidth + tagWidth + 7.0F;
        float boxHeight = entryHeight;
        float x = right - boxWidth;
        float textY = y + getCreidaTextOffset(entryHeight);
        int accent = this.getColor(time, offset).getRGB();
        int backgroundColor = new Color(0, 0, 0, 110).getRGB();
        int depthColor = new Color(0, 0, 0, 65).getRGB();

        RenderUtil.enableRenderState();
        RenderUtil.drawRect(x - 1.0F, y, x + boxWidth, y + boxHeight, depthColor);
        RenderUtil.drawRect(x - 1.0F, y, x + boxWidth, y + boxHeight, backgroundColor);
        RenderUtil.drawRect(x + boxWidth - 1.0F, y, x + boxWidth, y + boxHeight, accent);
        RenderUtil.disableRenderState();

        GlStateManager.disableDepth();
        drawCreidaStringWithShadow(moduleName, x + 1.0F, textY, accent);

        if (this.suffixes.getValue() && moduleSuffix.length > 0) {
            float suffixX = x + 1.0F + nameWidth + 4.0F;
            for (String suffix : moduleSuffix) {
                drawCreidaStringWithShadow(suffix, suffixX, textY, 0xFFCCCCCC);
                suffixX += this.getCreidaTextWidth(suffix) + 3.0F;
            }
        }
        GlStateManager.enableDepth();
    }

    private void renderCreidaWatermark(long time) {
        String text = getCreidaWatermarkText();
        float textWidth = getCreidaTextWidth(text);
        float boxWidth = textWidth + 8.0F;
        float boxHeight = Math.max(15.0F, getCreidaTextHeight() + 6.0F);
        float x = 2.0F;
        float y = 3.0F;

        RenderUtil.drawRoundedRect(x, y, boxWidth, boxHeight, 4.0F,
                new Color(0, 0, 0, 100).getRGB(), true, true, true, true);

        GlStateManager.resetColor();
        GlStateManager.disableDepth();

        float currentX = x + 4.0F;
        float textY = y + (boxHeight - getCreidaTextHeight()) / 2.0F + 1.0F;
        for (int i = 0; i < text.length(); i++) {
            String character = String.valueOf(text.charAt(i));
            int color = this.getColor(time, (long) (i * this.colorDistance.getValue())).getRGB();
            drawCreidaStringWithShadow(character, currentX, textY, color);
            currentX += getCreidaTextWidth(character);
        }

        GlStateManager.enableDepth();
    }

    private String getCreidaWatermarkText() {
        String playerName = mc.thePlayer == null ? "Player" : mc.thePlayer.getName();
        String versionText = Myau.version == null ? "dev" : Myau.version;
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        return "Myau+ Client | " + versionText + " | " + playerName + " | " + time;
    }

    private float getCreidaModuleWidth(Module module) {
        return getCreidaTextWidth(this.getModuleName(module)) + getCreidaTagWidth(this.getModuleSuffix(module)) + 7.0F;
    }

    private float getCreidaTagWidth(String[] suffixes) {
        if (!this.suffixes.getValue() || suffixes.length == 0) {
            return 1.0F;
        }

        float width = 0.0F;
        for (String suffix : suffixes) {
            width += this.getCreidaTextWidth(suffix) + 3.0F;
        }
        return width + 1.0F;
    }

    private float getCreidaEntryHeight() {
        return Math.max(14.0F, this.getCreidaTextHeight() + 5.0F);
    }

    private float getCreidaTextOffset(float entryHeight) {
        return Math.max(2.0F, (entryHeight - this.getCreidaTextHeight()) / 2.0F);
    }

    private float getCreidaTextWidth(String text) {
        if (isCreidaMinecraftFont()) {
            return mcFont.getStringWidth(text);
        }
        return fontRenderer.getStringWidth(text);
    }

    private float getCreidaTextHeight() {
        if (isCreidaMinecraftFont()) {
            return mcFont.FONT_HEIGHT;
        }
        return fontRenderer.FONT_HEIGHT;
    }

    private void drawCreidaStringWithShadow(String text, float x, float y, int color) {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (isCreidaMinecraftFont()) {
            mcFont.drawStringWithShadow(text, x, y, color);
        } else {
            fontRenderer.drawStringWithShadow(text, x, y, color);
        }
        GlStateManager.disableBlend();
    }

    private boolean isCreidaMinecraftFont() {
        return !this.creidaFont.getValue() || this.fontMode.getValue() == 1;
    }

    private void renderNotifications() {
        if (!this.notifications.getValue()) return;

        try {
            if (Myau.notificationManager == null) return;

            java.util.List<myau.management.NotificationManager.NotificationEntry> entries = Myau.notificationManager.getActive();
            if (entries.isEmpty()) return;

            float notificationScale = Math.max(0.5F, Math.min(1.5F, this.scale.getValue()));
            ScaledResolution sr = new ScaledResolution(mc);
            float scaledWidth = sr.getScaledWidth() / notificationScale;
            float scaledHeight = sr.getScaledHeight() / notificationScale;
            float margin = 8.0F;
            float paddingX = 8.0F;
            float paddingY = 5.0F;
            float spacing = 4.0F;
            float y = scaledHeight - margin;

            GlStateManager.pushMatrix();
            GlStateManager.scale(notificationScale, notificationScale, 1.0F);

            for (int i = entries.size() - 1; i >= 0; i--) {
                myau.management.NotificationManager.NotificationEntry entry = entries.get(i);
                float alpha = notificationAlpha(entry);
                if (alpha <= 0.01F) continue;

                String text = modernNotificationText(entry.message);
                float textWidth = getHudTextWidth(text);
                float textHeight = getHudTextHeight();
                float boxWidth = Math.max(86.0F, textWidth + paddingX * 2.0F + 2.0F);
                float boxHeight = textHeight + paddingY * 2.0F + 3.0F;
                float x = scaledWidth - margin - boxWidth;
                y -= boxHeight;

                drawModernNotification(entry, text, x, y, boxWidth, boxHeight, paddingX, paddingY, alpha);
                y -= spacing;
            }

            GlStateManager.popMatrix();
        } catch (Exception ignored) {
        }
    }

    private void drawModernNotification(myau.management.NotificationManager.NotificationEntry entry, String text,
                                        float x, float y, float boxWidth, float boxHeight,
                                        float paddingX, float paddingY, float alpha) {
        float motion = notificationMotion(entry);
        float slide = (1.0F - motion) * 14.0F + (1.0F - alpha) * 5.0F;
        float renderX = x + slide;
        int statusColor = notificationStatusColor(text, alpha);
        int glass = new Color(10, 12, 16, (int) (92 * alpha)).getRGB();
        int hoverLayer = new Color(255, 255, 255, (int) (9 * alpha)).getRGB();
        int border = new Color(255, 255, 255, (int) (24 * alpha)).getRGB();
        int depth = new Color(0, 0, 0, (int) (28 * alpha)).getRGB();
        int neutralText = new Color(238, 241, 245, (int) (242 * alpha)).getRGB();
        float radius = 6.0F;

        RenderUtil.drawRoundedRect(renderX + 1.0F, y + 1.5F, boxWidth, boxHeight, radius + 1.0F,
                depth, true, true, true, true);
        RenderUtil.drawRoundedRect(renderX, y, boxWidth, boxHeight, radius,
                glass, true, true, true, true);
        RenderUtil.drawRoundedRect(renderX + 1.0F, y + 1.0F, boxWidth - 2.0F, boxHeight - 2.0F, radius - 1.0F,
                hoverLayer, true, true, true, true);
        RenderUtil.drawRoundedRectOutline(renderX + 0.5F, y + 0.5F, boxWidth - 1.0F, boxHeight - 1.0F,
                radius, 1.0F, border, true, true, true, true);

        float progress = notificationProgress(entry);
        float progressX = renderX + 8.0F;
        float progressY = y + boxHeight - 2.0F;
        float progressW = boxWidth - 16.0F;
        RenderUtil.drawRoundedRect(progressX, progressY, progressW, 1.0F, 0.5F,
                new Color(255, 255, 255, (int) (10 * alpha)).getRGB(), true, true, true, true);
        RenderUtil.drawRoundedRect(progressX, progressY, Math.max(1.0F, progressW * progress), 1.0F, 0.5F,
                statusColor, true, true, true, true);

        drawNotificationText(text, renderX + paddingX + 1.0F, y + paddingY + 1.0F, neutralText, statusColor);
    }

    private float notificationAlpha(myau.management.NotificationManager.NotificationEntry entry) {
        if (entry.durationMillis <= 0) return 1.0F;

        float age = entry.getAge();
        float remaining = entry.durationMillis - age;
        float fade = Math.min(220.0F, entry.durationMillis / 3.0F);
        float alpha = Math.min(1.0F, Math.min(age / fade, remaining / fade));
        alpha = Math.max(0.0F, Math.min(1.0F, alpha));
        return alpha * alpha * (3.0F - 2.0F * alpha);
    }

    private float notificationProgress(myau.management.NotificationManager.NotificationEntry entry) {
        if (entry.durationMillis <= 0) return 1.0F;
        return Math.max(0.0F, Math.min(1.0F, 1.0F - entry.getAge() / (float) entry.durationMillis));
    }

    private float notificationMotion(myau.management.NotificationManager.NotificationEntry entry) {
        if (entry.durationMillis <= 0) return 1.0F;

        float age = entry.getAge();
        float remaining = entry.durationMillis - age;
        float in = Math.max(0.0F, Math.min(1.0F, age / 260.0F));
        float out = Math.max(0.0F, Math.min(1.0F, remaining / 220.0F));
        float motion = Math.min(in, out);
        return motion * motion * (3.0F - 2.0F * motion);
    }

    private String modernNotificationText(String message) {
        if (message == null) return "";
        return message
                .replace(" was toggled successfully", " enabled")
                .replace(" was untoggled successfully", " disabled");
    }

    private int softenColor(int rgb, float amount) {
        amount = Math.max(0.0F, Math.min(1.0F, amount));
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        r += (int) ((255 - r) * amount);
        g += (int) ((255 - g) * amount);
        b += (int) ((255 - b) * amount);
        return (r << 16) | (g << 8) | b;
    }

    private int notificationStatusColor(String text, float alpha) {
        String lower = text.toLowerCase(Locale.ROOT);
        int rgb = lower.endsWith(" enabled") ? 0x41D982 : lower.endsWith(" disabled") ? 0xFF5C6C : 0xE5E9F0;
        return colorWithAlpha(rgb, (int) (245 * alpha));
    }

    private int colorWithAlpha(int rgb, int alpha) {
        return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF,
                Math.max(0, Math.min(255, alpha))).getRGB();
    }

    private float getHudTextWidth(String text) {
        return fontMode.getValue() == 1 ? mcFont.getStringWidth(text) : fontRenderer.getStringWidth(text);
    }

    private float getHudTextHeight() {
        return fontMode.getValue() == 1 ? mcFont.FONT_HEIGHT : fontRenderer.FONT_HEIGHT;
    }

    private void drawHudText(String text, float x, float y, int color) {
        if (fontMode.getValue() == 1) {
            mcFont.drawString(text, x, y, color, false);
        } else {
            fontRenderer.drawString(text, x, y, color, false);
        }
    }

    private void drawNotificationText(String text, float x, float y, int neutralColor, int statusColor) {
        String lower = text.toLowerCase(Locale.ROOT);
        if (lower.endsWith(" enabled")) {
            drawSplitNotificationText(text, " enabled", x, y, neutralColor, statusColor);
        } else if (lower.endsWith(" disabled")) {
            drawSplitNotificationText(text, " disabled", x, y, neutralColor, statusColor);
        } else {
            drawHudText(text, x, y, neutralColor);
        }
    }

    private void drawSplitNotificationText(String text, String suffix, float x, float y, int neutralColor, int statusColor) {
        String main = text.substring(0, text.length() - suffix.length());
        drawHudText(main, x, y, neutralColor);
        drawHudText(suffix.trim(), x + getHudTextWidth(main + " "), y, statusColor);
    }

    @Override
    public boolean shouldKeepSprint() {
        return false;
    }
}
