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
import myau.ui.overlay.dynamicisland.DynamicIsland;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class HUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
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
    public final PercentProperty background = new PercentProperty("background", 25);
    public final BooleanProperty dynamicIsland = new BooleanProperty("dynamic-island", true);
    public final BooleanProperty showBar = new BooleanProperty("bar", true);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final BooleanProperty suffixes = new BooleanProperty("suffixes", true);
    public final BooleanProperty lowerCase = new BooleanProperty("lower-case", false);
    public final BooleanProperty chatOutline = new BooleanProperty("chat-outline", true);
    public final BooleanProperty blinkTimer = new BooleanProperty("blink-timer", true);
    public final BooleanProperty toggleSound = new BooleanProperty("toggle-sounds", true);
    public final BooleanProperty toggleAlerts = new BooleanProperty("toggle-alerts", false);
    public final ModeProperty fontMode = new ModeProperty("font-mode", 0, new String[]{"SANS", "MINECRAFT"});

    private String getModuleName(Module module) {
        String moduleName = module.getName();
        if (this.lowerCase.getValue()) {
            moduleName = moduleName.toLowerCase(Locale.ROOT);
        }
        return moduleName;
    }

    CFontRenderer fontRenderer;
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
                switch (fontMode.getValue()) {
                    case 1: // MINECRAFT
                        width += 3 + mcFont.getStringWidth(str);
                        break;
                    default:
                        width += 3 + fontRenderer.getStringWidth(str);
                        break;
                }
            }
        }
        return width;
    }

    private float getColorCycle(long long3, long long4) {
        long speed = (long) (3000.0 / Math.pow(Math.min(Math.max(0.5F, this.colorSpeed.getValue()), 1.5F), 3.0));
        return 1.0F - (float) (Math.abs(long3 - long4 * 300L) % speed) / (float) speed;
    }

    public HUD() {
        super("HUD", true, true);
        updateFontRenderer();
    }

    private void updateFontRenderer() {
        // Only update if font mode actually changed
        if (lastFontMode == fontMode.getValue()) {
            return;
        }
        
        lastFontMode = fontMode.getValue();
        
        switch (fontMode.getValue()) {
            case 0: // SANS
                fontRenderer = FontProcess.getFont("sans");
                break;
            case 1: // MINECRAFT - handled separately
                fontRenderer = FontProcess.getFont("sans");
                break;
            default:
                fontRenderer = FontProcess.getFont("sans");
                break;
        }
        
        // Debug: Check if font renderer is null
        if (fontRenderer == null) {
            fontRenderer = FontProcess.getFont("sans");
        }
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
            this.activeModules = Myau.moduleManager.modules.values().stream().filter(module -> module.isEnabled() && !module.isHidden()).sorted(Comparator.comparingInt(this::getModuleWidth).reversed()).collect(Collectors.<Module>toList());
        }
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
            float height = (float) fontRenderer.FONT_HEIGHT - 1.0F;
            float x = (float) this.offsetX.getValue()
                    + (1.0F + (this.showBar.getValue() ? (this.shadow.getValue() ? 2.0F : 1.0F) : 0.0F)) * this.scale.getValue();
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
                float totalWidth = (float) (this.calculateStringWidth(moduleName, moduleSuffix) - (this.shadow.getValue() ? 0 : 1));
                int color = this.getColor(l, offset).getRGB();
                RenderUtil.enableRenderState();
                if (this.background.getValue() > 0) {
                    RenderUtil.drawRect(
                            x / this.scale.getValue() - 1.0F - (this.posX.getValue() == 0 ? 0.0F : totalWidth),
                            y / this.scale.getValue() - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : (this.shadow.getValue() ? 1.0F : 0.0F)),
                            x / this.scale.getValue() + 1.0F + (this.posX.getValue() == 0 ? totalWidth : 0.0F),
                            y / this.scale.getValue() + height + (this.posY.getValue() == 0 ? (this.shadow.getValue() ? 1.0F : 0.0F) : (offset == 0L ? 1.0F : 0.0F)),
                            new Color(0.0F, 0.0F, 0.0F, this.background.getValue().floatValue() / 100.0F).getRGB()
                    );
                }
                if (this.showBar.getValue()) {
                    if (this.shadow.getValue()) {
                        RenderUtil.drawRect(
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -3.0F : 1.0F),
                                y / this.scale.getValue() - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 1.0F),
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -2.0F : 2.0F),
                                y / this.scale.getValue() + height + (this.posY.getValue() == 0 ? 1.0F : (offset == 0L ? 1.0F : 0.0F)),
                                color
                        );
                        RenderUtil.drawRect(
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -2.0F : 2.0F),
                                y / this.scale.getValue() - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 1.0F),
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -1.0F : 3.0F),
                                y / this.scale.getValue() + height + (this.posY.getValue() == 0 ? 1.0F : (offset == 0L ? 1.0F : 0.0F)),
                                (color & 16579836) >> 2 | color & 0xFF000000
                        );
                    } else {
                        RenderUtil.drawRect(
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -2.0F : 1.0F),
                                y / this.scale.getValue() - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 0.0F),
                                x / this.scale.getValue() + (this.posX.getValue() == 0 ? -1.0F : 2.0F),
                                y / this.scale.getValue() + height + (this.posY.getValue() == 0 ? 0.0F : (offset == 0L ? 1.0F : 0.0F)),
                                color
                        );
                    }
                }
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
                            width = (float) mcFont.getStringWidth(moduleName) + 3.0F;
                            break;
                        default:
                            width = (float) fontRenderer.getStringWidth(moduleName) + 3.0F;
                            break;
                    }
                    for (String string : moduleSuffix) {
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
                        width += (fontMode.getValue() == 1 ? (float) mcFont.getStringWidth(string) : (float) fontRenderer.getStringWidth(string)) + (this.shadow.getValue() ? 3.0F : 2.0F);
                    }
                }
                y += (height + (this.shadow.getValue() ? 1.0F : 0.0F)) * this.scale.getValue() * (this.posY.getValue() == 0 ? 1.0F : -1.0F);
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

            if (this.dynamicIsland.getValue()) {
                try {
                    myau.ui.overlay.dynamicisland.DynamicIsland.render(event, this.scale.getValue());
                } catch (Throwable ignored) {
                }
            }
        }
        // Render bottom-right transient notifications (e.g., module toggles)
        try {
            if (Myau.notificationManager != null) {
                java.util.List<myau.management.NotificationManager.NotificationEntry> entries = Myau.notificationManager.getActive();
                if (!entries.isEmpty()) {
                    ScaledResolution sr = new ScaledResolution(mc);
                    float baseX = sr.getScaledWidth() - 6.0F; // right padding
                    float baseY = sr.getScaledHeight() - 6.0F; // bottom padding
                    // render newest last, so iterate normally but compute offset from bottom
                    for (int i = entries.size() - 1; i >= 0; i--) {
                        myau.management.NotificationManager.NotificationEntry entry = entries.get(i);
                        String text = entry.message;
                        float textWidth = mc.fontRendererObj.getStringWidth(text);
                        float boxWidth = textWidth + 8.0F;
                        float boxHeight = mc.fontRendererObj.FONT_HEIGHT + 6.0F;
                        float x = baseX - boxWidth / this.scale.getValue();
                        float y = baseY - boxHeight / this.scale.getValue() - (entries.size() - 1 - i) * (boxHeight + 4.0F) / this.scale.getValue();

                        // fade alpha based on remaining time (simple)
                        float age = entry.getAge();
                        float alpha = 1.0F;
                        if (entry.durationMillis > 0) {
                            alpha = Math.max(0.0F, Math.min(1.0F, 1.0F - (age / (float) entry.durationMillis)));
                        }
                        int bg = new Color(0, 0, 0, (int) (150 * alpha)).getRGB();
                        // use entry.color for border/text, applying alpha
                        int baseColor = entry.color;
                        int r = (baseColor >> 16) & 0xFF;
                        int g = (baseColor >> 8) & 0xFF;
                        int bCol = baseColor & 0xFF;
                        int border = new Color(r, g, bCol, (int) (200 * alpha)).getRGB();

                        RenderUtil.enableRenderState();
                        RenderUtil.drawRect(x, y, x + boxWidth / this.scale.getValue(), y + boxHeight / this.scale.getValue(), bg);
                        RenderUtil.drawOutlineRect(x, y, x + boxWidth / this.scale.getValue(), y + boxHeight / this.scale.getValue(), 1.0F, 0, border);
                        RenderUtil.disableRenderState();

                        GlStateManager.pushMatrix();
                        GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0F);
                        // text color with alpha
                        int textColor = new Color(r, g, bCol, (int) (255 * alpha)).getRGB();
                        mc.fontRendererObj.drawStringWithShadow(text, x + 4.0F / this.scale.getValue(), y + 3.0F / this.scale.getValue(), textColor);
                        GlStateManager.popMatrix();
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}



