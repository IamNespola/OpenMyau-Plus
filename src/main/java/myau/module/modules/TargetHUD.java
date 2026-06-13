package myau.module.modules;

import myau.Myau;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ColorProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.ModeProperty;
import myau.property.properties.PercentProperty;
import myau.util.AnimationUtil;
import myau.util.ColorUtil;
import myau.util.RenderUtil;
import myau.util.TeamUtil;
import myau.util.TimerUtil;
import myau.util.font.FontManager;
import myau.util.shader.BlurShader;
import myau.util.shader.BlurUtils;
import myau.util.shader.RoundedUtils;
import myau.util.shader.ShadowShader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TargetHUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat healthFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    private static final DecimalFormat diffFormat = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));
    private static final int MYAU_STYLE_START = 8;

    public final ModeProperty style = new ModeProperty("style", 0, new String[]{
            "DEFAULT", "RAVENBS-MODERN", "RAVENBS-LEGACY", "FACE", "THREED", "SIMPLE", "CIRCLE", "BlueDEV",
            "ASTOLFO", "EXHIBITION", "MOON", "RISE", "NEVERLOSE", "TENACITY"
    });
    public final ModeProperty color = new ModeProperty("color", 0, new String[]{"DEFAULT", "HUD"}, this::isOpenMyauStyle);
    public final ModeProperty animMode = new ModeProperty("Anim Mode", 0, new String[]{"ELASTIC", "SCALE"}, this::isMyauPlusStyle);
    public final ModeProperty colorMode = new ModeProperty("Color", 0, new String[]{"SYNC", "CUSTOM", "HEALTH", "ASTOLFO"}, this::isMyauPlusStyle);
    public final ColorProperty customColor = new ColorProperty("CustomColor", new Color(255, 50, 50).getRGB() & 0xFFFFFF, () -> isMyauPlusStyle() && colorMode.getValue() == 1);
    public final IntProperty bgAlpha = new IntProperty("Bg-Alpha", 180, 0, 255, () -> isMyauPlusStyle() && getMyauPlusStyle() != 1);
    public final ModeProperty posX = new ModeProperty("position-x", 1, new String[]{"LEFT", "MIDDLE", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 1, new String[]{"TOP", "MIDDLE", "BOTTOM"});
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 2.0F);
    public final IntProperty offX = new IntProperty("offset-x", 0, -500, 500);
    public final IntProperty offY = new IntProperty("offset-y", 40, -500, 500);
    public final PercentProperty background = new PercentProperty("background", 25, () -> this.style.getValue() == 0);
    public final BooleanProperty head = new BooleanProperty("head", true, () -> this.style.getValue() == 0);
    public final BooleanProperty indicator = new BooleanProperty("indicator", true, () -> this.style.getValue() == 0);
    public final BooleanProperty outline = new BooleanProperty("outline", false, () -> isOpenMyauStyle() && (this.style.getValue() == 0 || this.style.getValue() == 1 || this.style.getValue() >= 3));
    public final BooleanProperty animations = new BooleanProperty("animations", true, () -> this.style.getValue() == 0);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true, () -> this.style.getValue() == 0);
    public final BooleanProperty kaOnly = new BooleanProperty("ka-only", true);
    public final BooleanProperty chatPreview = new BooleanProperty("chat-preview", false);

    private final TimerUtil lastAttackTimer = new TimerUtil();
    private final TimerUtil animTimer = new TimerUtil();
    private final AnimationUtil openingAnimation = new AnimationUtil(AnimationUtil.Easing.EASE_OUT_ELASTIC, 600);
    private final AnimationUtil healthAnimation = new AnimationUtil(AnimationUtil.Easing.EASE_OUT_QUINT, 250);

    private EntityLivingBase lastTarget;
    private EntityLivingBase target;
    private ResourceLocation headTexture;
    private float oldHealth;
    private float newHealth;
    private float maxHealth;
    private float lastHealthBar;
    private TimerUtil fadeTimer;
    private boolean fadingIn;
    private EntityLivingBase fadingEntity;
    private float animatedHealth;
    private float animatedArmor;
    private float animatedScale;
    private long damageFlashTime;
    private float lastHealthVal;
    private EntityLivingBase renderTarget;

    public TargetHUD() {
        super("TargetHUD", false, true, "Target HUD");
    }

    private boolean isOpenMyauStyle() {
        return this.style.getValue() < MYAU_STYLE_START;
    }

    private boolean isMyauPlusStyle() {
        return this.style.getValue() >= MYAU_STYLE_START;
    }

    private int getMyauPlusStyle() {
        return this.style.getValue() - MYAU_STYLE_START;
    }

    private EntityLivingBase resolveTarget() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura != null && killAura.isEnabled() && killAura.isAttackAllowed() && TeamUtil.isEntityLoaded(killAura.getTarget())) {
            return killAura.getTarget();
        } else if (!this.kaOnly.getValue()
                && !this.lastAttackTimer.hasTimeElapsed(1500L)
                && TeamUtil.isEntityLoaded(this.lastTarget)) {
            return this.lastTarget;
        } else {
            return this.chatPreview.getValue() && mc.currentScreen instanceof GuiChat ? mc.thePlayer : null;
        }
    }

    private ResourceLocation getSkin(EntityLivingBase entityLivingBase) {
        if (entityLivingBase instanceof EntityPlayer) {
            NetworkPlayerInfo playerInfo = mc.getNetHandler().getPlayerInfo(entityLivingBase.getName());
            if (playerInfo != null) {
                return playerInfo.getLocationSkin();
            }
        }
        return null;
    }

    private Color getTargetColor(EntityLivingBase entityLivingBase) {
        if (entityLivingBase instanceof EntityPlayer) {
            if (TeamUtil.isFriend((EntityPlayer) entityLivingBase)) {
                return Myau.friendManager.getColor();
            }
            if (TeamUtil.isTarget((EntityPlayer) entityLivingBase)) {
                return Myau.targetManager.getColor();
            }
        }
        switch (this.color.getValue()) {
            case 0:
                if (!(entityLivingBase instanceof EntityPlayer)) {
                    return new Color(-1);
                }
                return TeamUtil.getTeamColor((EntityPlayer) entityLivingBase, 1.0F);
            case 1:
                HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
                return hud != null ? hud.getColor(System.currentTimeMillis()) : new Color(-1);
            default:
                return new Color(-1);
        }
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) {
            return;
        }

        if (isMyauPlusStyle()) {
            renderMyauPlusTargetHud();
        } else {
            renderOpenMyauTargetHud();
        }
    }

    private void renderOpenMyauTargetHud() {
        EntityLivingBase previousTarget = this.target;
        this.target = this.resolveTarget();

        if (this.target != null) {
            if (previousTarget == null && fadeTimer == null) {
                fadeTimer = new TimerUtil();
                fadeTimer.reset();
                fadingIn = true;
            } else if (fadingIn && fadeTimer != null && fadeTimer.getElapsedTime() >= 400) {
                fadeTimer = null;
                fadingIn = false;
            }
        } else if (previousTarget != null && fadeTimer == null) {
            fadeTimer = new TimerUtil();
            fadeTimer.reset();
            fadingIn = false;
            fadingEntity = previousTarget;
        }

        if (previousTarget == null && fadeTimer == null) {
            return;
        }

        EntityLivingBase entity = this.target != null ? this.target : fadingEntity;
        if (entity == null) {
            return;
        }

        float playerHealth = (mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) / 2.0F;
        float absorption = entity.getAbsorptionAmount() / 2.0F;
        float targetHealth = entity.getHealth() / 2.0F + absorption;

        float entityHealth = entity.getHealth();
        if (entityHealth < this.lastHealthVal) {
            this.damageFlashTime = System.currentTimeMillis();
        }
        this.lastHealthVal = entityHealth;

        if (entity != previousTarget) {
            this.headTexture = null;
            this.animTimer.setTime();
            this.oldHealth = targetHealth;
            this.newHealth = targetHealth;
            this.animatedHealth = entityHealth;
            this.animatedArmor = entity instanceof EntityPlayer ? ((EntityPlayer) entity).getTotalArmorValue() : 0;
        }
        if (!this.animations.getValue() || this.animTimer.hasTimeElapsed(150L)) {
            this.oldHealth = this.newHealth;
            this.newHealth = targetHealth;
            this.maxHealth = entity.getMaxHealth() / 2.0F;
            if (this.oldHealth != this.newHealth) {
                this.animTimer.reset();
            }
        }

        if (this.animatedHealth == 0.0F || entity == mc.thePlayer) {
            this.animatedHealth = entityHealth;
        }
        this.animatedHealth = this.animatedHealth + (entityHealth - this.animatedHealth) * 0.1F;

        int targetArmor = entity instanceof EntityPlayer ? ((EntityPlayer) entity).getTotalArmorValue() : 0;
        if (this.animatedArmor == 0.0F || entity == mc.thePlayer) {
            this.animatedArmor = targetArmor;
        }
        this.animatedArmor = this.animatedArmor + ((float) targetArmor - this.animatedArmor) * 0.1F;

        if (fadeTimer != null) {
            long elapsed = fadeTimer.getElapsedTime();
            this.animatedScale = fadingIn ? Math.min(1.0F, elapsed / 400.0F) : Math.max(0.0F, 1.0F - elapsed / 400.0F);
        } else {
            this.animatedScale = this.target != null ? 1.0F : 0.0F;
        }

        ResourceLocation resourceLocation = this.getSkin(entity);
        if (resourceLocation != null) {
            this.headTexture = resourceLocation;
        }

        int styleMode = this.style.getValue();
        if (styleMode == 0) {
            drawDefaultStyle(entity, playerHealth, absorption, targetHealth);
        } else if (styleMode <= 2) {
            drawRavenBSStyle(styleMode - 1, entity, playerHealth, absorption, targetHealth);
        } else if (styleMode <= 6) {
            drawRavenStyle(styleMode - 3, entity);
        } else if (styleMode == 7) {
            drawBlueDevStyle(entity);
        }
    }

    private void drawDefaultStyle(EntityLivingBase entity, float health, float abs, float heal) {
        float elapsedTime = (float) Math.min(Math.max(this.animTimer.getElapsedTime(), 0L), 150L);
        float healthMax = this.maxHealth <= 0.0F ? Math.max(1.0F, entity.getMaxHealth() / 2.0F) : this.maxHealth;
        float lerpedHealthRatio = Math.min(Math.max(RenderUtil.lerpFloat(this.newHealth, this.oldHealth, elapsedTime / 150.0F) / healthMax, 0.0F), 1.0F);
        Color targetColor = this.getTargetColor(entity);
        Color healthBarColor = this.color.getValue() == 0 ? ColorUtil.getHealthBlend(lerpedHealthRatio) : targetColor;
        float healthDeltaRatio = Math.min(Math.max((health - heal + 1.0F) / 2.0F, 0.0F), 1.0F);
        Color healthDeltaColor = ColorUtil.getHealthBlend(healthDeltaRatio);
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        String targetNameText = ChatColors.formatColor(String.format("&r%s&r", TeamUtil.stripName(entity)));
        int targetNameWidth = mc.fontRendererObj.getStringWidth(targetNameText);
        String healthText = ChatColors.formatColor(
                String.format("&r&f%s%s\u2764&r", healthFormat.format(heal), abs > 0.0F ? "&6" : "&c")
        );
        int healthTextWidth = mc.fontRendererObj.getStringWidth(healthText);
        String statusText = ChatColors.formatColor(String.format("&r&l%s&r", heal == health ? "D" : (heal < health ? "W" : "L")));
        int statusTextWidth = mc.fontRendererObj.getStringWidth(statusText);
        String healthDiffText = ChatColors.formatColor(
                String.format("&r%s&r", heal == health ? "0.0" : diffFormat.format(health - heal))
        );
        int healthDiffWidth = mc.fontRendererObj.getStringWidth(healthDiffText);
        float barContentWidth = Math.max(
                (float) targetNameWidth + (this.indicator.getValue() ? 2.0F + (float) statusTextWidth + 2.0F : 0.0F),
                (float) healthTextWidth + (this.indicator.getValue() ? 2.0F + (float) healthDiffWidth + 2.0F : 0.0F)
        );
        float headIconOffset = this.head.getValue() && this.headTexture != null ? 25.0F : 0.0F;
        float barTotalWidth = Math.max(headIconOffset + 70.0F, headIconOffset + 2.0F + barContentWidth + 2.0F);
        float posX = this.offX.getValue().floatValue() / this.scale.getValue();
        switch (this.posX.getValue()) {
            case 1:
                posX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() / 2.0F - barTotalWidth / 2.0F;
                break;
            case 2:
                posX *= -1.0F;
                posX += (float) scaledResolution.getScaledWidth() / this.scale.getValue() - barTotalWidth;
                break;
            default:
                break;
        }
        float posY = this.offY.getValue().floatValue() / this.scale.getValue();
        switch (this.posY.getValue()) {
            case 1:
                posY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() / 2.0F - 13.5F;
                break;
            case 2:
                posY *= -1.0F;
                posY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() - 27.0F;
                break;
            default:
                break;
        }
        GlStateManager.pushMatrix();
        GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0F);
        GlStateManager.translate(posX, posY, -450.0F);
        RenderUtil.enableRenderState();
        int backgroundColor = new Color(0.0F, 0.0F, 0.0F, (float) this.background.getValue() / 100.0F).getRGB();
        int outlineColor = this.outline.getValue() ? targetColor.getRGB() : new Color(0, 0, 0, 0).getRGB();
        RenderUtil.drawOutlineRect(0.0F, 0.0F, barTotalWidth, 27.0F, 1.5F, backgroundColor, outlineColor);
        RenderUtil.drawRect(headIconOffset + 2.0F, 22.0F, barTotalWidth - 2.0F, 25.0F, ColorUtil.darker(healthBarColor, 0.2F).getRGB());
        RenderUtil.drawRect(headIconOffset + 2.0F, 22.0F, headIconOffset + 2.0F + lerpedHealthRatio * (barTotalWidth - 2.0F - headIconOffset - 2.0F), 25.0F, healthBarColor.getRGB());
        RenderUtil.disableRenderState();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        mc.fontRendererObj.drawString(targetNameText, headIconOffset + 2.0F, 2.0F, -1, this.shadow.getValue());
        mc.fontRendererObj.drawString(healthText, headIconOffset + 2.0F, 12.0F, -1, this.shadow.getValue());
        if (this.indicator.getValue()) {
            mc.fontRendererObj.drawString(statusText, barTotalWidth - 2.0F - (float) statusTextWidth, 2.0F, healthDeltaColor.getRGB(), this.shadow.getValue());
            mc.fontRendererObj.drawString(healthDiffText, barTotalWidth - 2.0F - (float) healthDiffWidth, 12.0F, ColorUtil.darker(healthDeltaColor, 0.8F).getRGB(), this.shadow.getValue());
        }
        if (this.head.getValue() && this.headTexture != null) {
            GlStateManager.color(1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(this.headTexture);
            Gui.drawScaledCustomSizeModalRect(2, 2, 8.0F, 8.0F, 8, 8, 23, 23, 64.0F, 64.0F);
            Gui.drawScaledCustomSizeModalRect(2, 2, 40.0F, 8.0F, 8, 8, 23, 23, 64.0F, 64.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F);
        }
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private void drawRavenBSStyle(int mode, EntityLivingBase entity, float health, float abs, float heal) {
        String playerInfo = entity.getDisplayName().getFormattedText();
        double healthRatio = entity.getHealth() / entity.getMaxHealth();
        if (entity.isDead) {
            healthRatio = 0;
        }
        playerInfo += " \u00a7c" + String.format("%.1f", heal);

        if (this.indicator.getValue()) {
            playerInfo += " " + (healthRatio <= health / mc.thePlayer.getMaxHealth() ? "\u00a7aW" : "\u00a7cL");
        }

        int alpha = 255;
        if (fadeTimer != null) {
            long elapsed = fadeTimer.getElapsedTime();
            if (elapsed < 400) {
                alpha = fadingIn ? (int) ((elapsed / 400.0F) * 255) : (int) (255 - (elapsed / 400.0F) * 255);
            } else {
                alpha = fadingIn ? 255 : 0;
                if (!fadingIn) {
                    this.target = null;
                    fadeTimer = null;
                    fadingEntity = null;
                    return;
                }
            }
        }

        ScaledResolution scaledResolution = new ScaledResolution(mc);
        int padding = 8;
        int targetStrWithPadding = mc.fontRendererObj.getStringWidth(playerInfo) + padding;
        int x = (scaledResolution.getScaledWidth() / 2 - targetStrWithPadding / 2) + offX.getValue();
        int y = (scaledResolution.getScaledHeight() / 2 + 15) + offY.getValue();
        int n6 = x - padding;
        int n7 = y - padding;
        int n8 = x + targetStrWithPadding;
        int n9 = y + (mc.fontRendererObj.FONT_HEIGHT + 5) - 6 + padding;

        int maxAlphaOutline = Math.min(alpha, 110);
        int maxAlphaBackground = Math.min(alpha, 210);

        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
        int gradientLeft = hud != null ? hud.getColor(System.currentTimeMillis()).getRGB() : Color.WHITE.getRGB();
        int gradientRight = hud != null ? hud.getColor(System.currentTimeMillis() + 500).getRGB() : Color.WHITE.getRGB();

        switch (mode) {
            case 0:
                float bloomRadius = fadeTimer == null ? 2.0F : 2.0F * alpha / 255.0F;
                float blurRadius = fadeTimer == null ? 3.0F : 3.0F * alpha / 255.0F;
                if (RenderFixes.shouldUseShaders()) {
                    BlurUtils.prepareBloom();
                    RoundedUtils.drawRound(n6, n7, n8 - n6, n9 + 13 - n7, 8.0F, true, new Color(0, 0, 0, maxAlphaBackground));
                    BlurUtils.bloomEnd(3, bloomRadius);
                    BlurUtils.prepareBlur();
                    RoundedUtils.drawRound(n6, n7, n8 - n6, n9 + 13 - n7, 8.0F, true, new Color(RenderUtil.mergeAlpha(Color.black.getRGB(), maxAlphaOutline)));
                    BlurUtils.blurEnd(2, blurRadius);
                } else {
                    RenderUtil.drawRoundedRect(n6, n7, n8 - n6, n9 + 13 - n7, 8.0F,
                            RenderUtil.mergeAlpha(Color.black.getRGB(), maxAlphaOutline), true, true, true, true);
                }
                break;
            case 1:
                RenderUtil.drawRoundedGradientOutlinedRectangle(n6, n7, n8, n9 + 13, 10.0F,
                        RenderUtil.mergeAlpha(Color.black.getRGB(), maxAlphaOutline),
                        RenderUtil.mergeAlpha(gradientLeft, alpha),
                        RenderUtil.mergeAlpha(gradientRight, alpha));
                break;
            default:
                break;
        }

        int n13 = n6 + 6;
        int n14 = n8 - 6;
        int n15 = n9;

        RenderUtil.drawRoundedRectangle(n13, n15, n14, n15 + 5, 4.0F,
                RenderUtil.mergeAlpha(Color.black.getRGB(), maxAlphaOutline));

        int mergedGradientLeft = RenderUtil.mergeAlpha(gradientLeft, maxAlphaBackground);
        int mergedGradientRight = RenderUtil.mergeAlpha(gradientRight, maxAlphaBackground);

        float healthBar = (float) (n14 + (n13 - n14) * (1 - healthRatio));

        if (lastHealthBar != healthBar && lastHealthBar - n13 >= 3) {
            float diff = lastHealthBar - healthBar;
            if (diff > 0) {
                lastHealthBar = lastHealthBar - diff * 0.1F;
            } else {
                lastHealthBar = lastHealthBar + (-diff) * 0.1F;
            }
        } else {
            lastHealthBar = healthBar;
        }

        if (lastHealthBar > n14) {
            lastHealthBar = n14;
        }

        switch (mode) {
            case 0:
                RenderUtil.drawRoundedRectangle(n13, n15, lastHealthBar, n15 + 5, 4.0F,
                        RenderUtil.darkenColor(mergedGradientRight, 25));
                RenderUtil.drawRoundedGradientRect(n13, n15, healthBar, n15 + 5, 4.0F,
                        mergedGradientLeft, mergedGradientLeft, mergedGradientRight, mergedGradientRight);
                break;
            case 1:
                RenderUtil.drawRoundedGradientRect(n13, n15, lastHealthBar, n15 + 5, 4.0F,
                        mergedGradientLeft, mergedGradientLeft, mergedGradientRight, mergedGradientRight);
                break;
            default:
                break;
        }

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        mc.fontRendererObj.drawString(playerInfo, x, y,
                new Color(220, 220, 220, 255).getRGB() & 0xFFFFFF | Math.min(alpha + 15, 255) << 24, true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void drawRavenStyle(int mode, EntityLivingBase entity) {
        switch (mode) {
            case 0:
                drawFaceStyle(entity);
                break;
            case 1:
                draw3DStyle(entity);
                break;
            case 2:
                drawSimpleStyle(entity);
                break;
            case 3:
                drawCircleStyle(entity);
                break;
            default:
                break;
        }
    }

    private void drawFaceStyle(EntityLivingBase entity) {
        drawClassicPanelStyle(entity, true, false, 150, 50);
    }

    private void draw3DStyle(EntityLivingBase entity) {
        drawClassicPanelStyle(entity, false, true, 150, 50);
    }

    private void drawClassicPanelStyle(EntityLivingBase entity, boolean forceHead, boolean prefer3d, int hudWidth, int hudHeight) {
        float alpha = Math.min(1.0F, this.animatedScale);
        int baseX = HUD.targetHUDX;
        int baseY = HUD.targetHUDY;

        float sc = this.scale.getValue();
        GL11.glPushMatrix();
        GL11.glTranslated(baseX + hudWidth / 2.0, baseY + hudHeight / 2.0, 0);
        GL11.glScalef(sc, sc, 1.0F);
        GL11.glTranslated(-(baseX + hudWidth / 2.0), -(baseY + hudHeight / 2.0), 0);

        int x = baseX;
        int y = baseY;

        long timeSinceDamage = System.currentTimeMillis() - this.damageFlashTime;
        float flashAlpha = timeSinceDamage < 300 ? 1.0F - (timeSinceDamage / 300.0F) : 0.0F;

        int bgBase = new Color(26, 26, 26, (int) (alpha * 128)).getRGB();
        if (flashAlpha > 0) {
            int r = (int) (26 + (255 - 26) * flashAlpha);
            int g = (int) (26 * (1 - flashAlpha));
            int b = (int) (26 * (1 - flashAlpha));
            bgBase = new Color(r, g, b, (int) (alpha * 128)).getRGB();
        }
        RenderUtil.drawRoundedRect(x, y, hudWidth, hudHeight, 8.0F, bgBase);

        int borderColor = (int) (alpha * 0xFF) << 24 | 0xFF8C00;
        if (flashAlpha > 0) {
            borderColor = (int) (alpha * 0xFF) << 24 | 0xFF0000;
        }
        if (this.outline.getValue()) {
            RenderUtil.drawRoundedRectOutline(x, y, hudWidth, hudHeight, 8.0F, 2.0F, borderColor, true, true, true, true);
        }

        if (prefer3d && this.animatedScale > 0.5F && entity instanceof EntityPlayer) {
            try {
                GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
                GuiInventory.drawEntityOnScreen(x + 20, y + 34, 14, 0, 0, (EntityPlayer) entity);
            } catch (Exception ignored) {
            }
        } else if (this.headTexture != null) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
            mc.getTextureManager().bindTexture(this.headTexture);
            Gui.drawScaledCustomSizeModalRect(x + 5, y + (forceHead ? 5 : 8), 8.0F, 8.0F, 8, 8, forceHead ? 30 : 24, forceHead ? 30 : 24, 64.0F, 64.0F);
        }

        String targetName = TeamUtil.stripName(entity);
        int nameColor = (int) (alpha * 0xFF) << 24 | 0xFFFFFF;
        mc.fontRendererObj.drawString(targetName, x + 40, y + 8, nameColor, true);

        float maxHealth = entity.getMaxHealth();
        float healthPercent = Math.min(1.0F, this.animatedHealth / maxHealth);

        int healthBarY = y + 25;
        int healthBarWidth = hudWidth - 45;

        RenderUtil.drawRoundedRect(x + 40, healthBarY, healthBarWidth, 8.0F, 4.0F,
                (int) (alpha * 0x40) << 24 | 0x404040);

        int healthColor = healthPercent > 0.5F ? 0xFF00FF00 : healthPercent > 0.25F ? 0xFFFFFF00 : 0xFFFF0000;
        int healthFillColor = (int) (alpha * 0xFF) << 24 | healthColor;
        RenderUtil.drawRoundedRect(x + 40, healthBarY, (int) (healthBarWidth * healthPercent), 8.0F, 4.0F, healthFillColor);

        float armorPercent = Math.min(1.0F, this.animatedArmor / 20.0F);
        int armorBarY = healthBarY + 10;
        RenderUtil.drawRoundedRect(x + 40, armorBarY, healthBarWidth, 4.0F, 2.0F,
                (int) (alpha * 0x40) << 24 | 0x404040);

        int armorFillColor = (int) (alpha * 0xFF) << 24 | 0x00BFFF;
        RenderUtil.drawRoundedRect(x + 40, armorBarY, (int) (healthBarWidth * armorPercent), 4.0F, 2.0F, armorFillColor);

        String healthText = String.format("%.1f/%.1f", this.animatedHealth, maxHealth);
        int healthTextColor = (int) (alpha * 0xFF) << 24 | 0xCCCCCC;
        mc.fontRendererObj.drawString(healthText, x + 40, y + 15, healthTextColor, true);

        GL11.glPopMatrix();
    }

    private void drawSimpleStyle(EntityLivingBase entity) {
        float alpha = Math.min(1.0F, this.animatedScale);
        int baseX = HUD.targetHUDX;
        int baseY = HUD.targetHUDY;
        int hudWidth = 130;
        int hudHeight = 32;

        float sc = this.scale.getValue();
        GL11.glPushMatrix();
        GL11.glTranslated(baseX + hudWidth / 2.0, baseY + hudHeight / 2.0, 0);
        GL11.glScalef(sc, sc, 1.0F);
        GL11.glTranslated(-(baseX + hudWidth / 2.0), -(baseY + hudHeight / 2.0), 0);

        int x = baseX;
        int y = baseY;

        long timeSinceDamage = System.currentTimeMillis() - this.damageFlashTime;
        float flashAlpha = timeSinceDamage < 300 ? 1.0F - (timeSinceDamage / 300.0F) : 0.0F;

        int bgBase = new Color(26, 26, 26, (int) (alpha * 128)).getRGB();
        if (flashAlpha > 0) {
            int r = (int) (26 + (255 - 26) * flashAlpha);
            int g = (int) (26 * (1 - flashAlpha));
            int b = (int) (26 * (1 - flashAlpha));
            bgBase = new Color(r, g, b, (int) (alpha * 128)).getRGB();
        }
        RenderUtil.drawRoundedRect(x, y, hudWidth, hudHeight, 8.0F, bgBase);

        int borderColor = (int) (alpha * 0xFF) << 24 | 0xFF8C00;
        if (flashAlpha > 0) {
            borderColor = (int) (alpha * 0xFF) << 24 | 0xFF0000;
        }
        if (this.outline.getValue()) {
            RenderUtil.drawRoundedRectOutline(x, y, hudWidth, hudHeight, 8.0F, 2.0F, borderColor, true, true, true, true);
        }

        String targetName = TeamUtil.stripName(entity);
        int nameColor = (int) (alpha * 0xFF) << 24 | 0xFFFFFF;
        mc.fontRendererObj.drawString(targetName, x + 5, y + 5, nameColor, true);

        float maxHealth = entity.getMaxHealth();
        float healthPercent = Math.min(1.0F, this.animatedHealth / maxHealth);

        int healthBarY = y + 19;
        int healthBarWidth = hudWidth - 10;

        RenderUtil.drawRoundedRect(x + 5, healthBarY, healthBarWidth, 8.0F, 4.0F,
                (int) (alpha * 0x40) << 24 | 0x404040);

        int healthColor = healthPercent > 0.5F ? 0xFF00FF00 : healthPercent > 0.25F ? 0xFFFFFF00 : 0xFFFF0000;
        int healthFillColor = (int) (alpha * 0xFF) << 24 | healthColor;
        RenderUtil.drawRoundedRect(x + 5, healthBarY, (int) (healthBarWidth * healthPercent), 8.0F, 4.0F, healthFillColor);

        int actualHealthInt = Math.round(this.animatedHealth);
        String healthText = String.format("%d/%d", actualHealthInt, (int) maxHealth);
        int healthTextColor = (int) (alpha * 0xFF) << 24 | 0xCCCCCC;
        double healthTextWidth = mc.fontRendererObj.getStringWidth(healthText);
        mc.fontRendererObj.drawString(healthText, (int) (x + hudWidth - 5 - healthTextWidth), y + 5, healthTextColor, true);

        GL11.glPopMatrix();
    }

    private void drawCircleStyle(EntityLivingBase entity) {
        float alpha = Math.min(1.0F, this.animatedScale);
        int baseX = HUD.targetHUDX;
        int baseY = HUD.targetHUDY;

        float sc = this.scale.getValue();
        GL11.glPushMatrix();
        GL11.glTranslated(baseX, baseY, 0);
        GL11.glScalef(sc, sc, 1.0F);
        GL11.glTranslated(-baseX, -baseY, 0);

        int x = baseX;
        int y = baseY;

        int circleX = x + 20;
        int circleY = y + 20;
        int radius = 15;
        int segments = 48;

        if (this.headTexture != null) {
            mc.getTextureManager().bindTexture(this.headTexture);
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            worldrenderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_TEX);
            worldrenderer.pos(circleX, circleY, 0).tex(12.0 / 64.0, 12.0 / 64.0).endVertex();
            for (int i = 0; i <= segments; i++) {
                double a = Math.toRadians(360.0 * i / segments);
                double tx = (12.0 + 4.0 * Math.cos(a)) / 64.0;
                double ty = (12.0 + 4.0 * Math.sin(a)) / 64.0;
                worldrenderer.pos(circleX + radius * Math.cos(a), circleY + radius * Math.sin(a), 0).tex(tx, ty).endVertex();
            }
            tessellator.draw();
        }

        float maxHealth = entity.getMaxHealth();
        float healthPercent = Math.min(1.0F, entity.getHealth() / maxHealth);
        int armor = entity instanceof EntityPlayer ? ((EntityPlayer) entity).getTotalArmorValue() : 0;
        float armorPercent = Math.min(1.0F, armor / 20.0F);

        float healthArc = healthPercent * 180.0F;
        float armorArc = armorPercent * 180.0F;

        int innerR = radius + 1;
        int outerR = radius + 4;
        int arcSeg = 30;

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        if (healthArc > 0) {
            GL11.glColor4f(1, 0, 0, alpha);
            worldrenderer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION);
            for (int i = 0; i <= arcSeg; i++) {
                double a = Math.toRadians(-90 + healthArc * i / arcSeg);
                worldrenderer.pos(circleX + outerR * Math.cos(a), circleY + outerR * Math.sin(a), 0).endVertex();
                worldrenderer.pos(circleX + innerR * Math.cos(a), circleY + innerR * Math.sin(a), 0).endVertex();
            }
            tessellator.draw();
        }

        if (armorArc > 0) {
            GL11.glColor4f(0, 0.75F, 1, alpha);
            worldrenderer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION);
            for (int i = 0; i <= arcSeg; i++) {
                double a = Math.toRadians(90 + armorArc * i / arcSeg);
                worldrenderer.pos(circleX + outerR * Math.cos(a), circleY + outerR * Math.sin(a), 0).endVertex();
                worldrenderer.pos(circleX + innerR * Math.cos(a), circleY + innerR * Math.sin(a), 0).endVertex();
            }
            tessellator.draw();
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);

        String targetName = TeamUtil.stripName(entity);
        int nameColor = (int) (alpha * 0xFF) << 24 | 0xFFFFFF;
        mc.fontRendererObj.drawString(targetName, x + 40, y + 8, nameColor, true);

        String healthText = String.format("%.1f/%.1f", this.animatedHealth, maxHealth);
        int healthTextColor = (int) (alpha * 0xFF) << 24 | 0xCCCCCC;
        mc.fontRendererObj.drawString(healthText, x + 40, y + 22, healthTextColor, true);

        GL11.glPopMatrix();
    }

    private void drawBlueDevStyle(EntityLivingBase entity) {
        float alpha = Math.min(1.0F, this.animatedScale);

        int x = HUD.targetHUDX;
        int y = HUD.targetHUDY;

        int width = 165;
        int height = 55;

        float sc = this.scale.getValue();

        GL11.glPushMatrix();
        GL11.glTranslated(x + width / 2.0, y + height / 2.0, 0);
        GL11.glScalef(sc, sc, 1.0F);
        GL11.glTranslated(-(x + width / 2.0), -(y + height / 2.0), 0);

        long timeSinceDamage = System.currentTimeMillis() - this.damageFlashTime;
        float flashAlpha = timeSinceDamage < 300 ? 1.0F - (timeSinceDamage / 300.0F) : 0.0F;

        int maxAlphaOutline = (int) (alpha * 110);
        int maxAlphaBackground = (int) (alpha * 210);

        if (RenderFixes.shouldUseShaders()) {
            BlurUtils.prepareBloom();
            RoundedUtils.drawRound(x, y, width, height, 10.0F, true, new Color(0, 0, 0, maxAlphaBackground));
            BlurUtils.bloomEnd(3, 2.0F);

            BlurUtils.prepareBlur();
            RoundedUtils.drawRound(
                    x,
                    y,
                    width,
                    height,
                    10.0F,
                    true,
                    new Color(RenderUtil.mergeAlpha(Color.black.getRGB(), maxAlphaOutline))
            );
            BlurUtils.blurEnd(2, 3.0F);
        }

        int bgColor = new Color(
                (int) (15 + 100 * flashAlpha),
                (int) (22 * (1.0F - flashAlpha)),
                (int) (35 * (1.0F - flashAlpha)),
                (int) (alpha * 180)
        ).getRGB();

        RenderUtil.drawRoundedRect(x, y, width, height, 10.0F, bgColor);

        if (this.outline.getValue()) {
            int outlineColor = flashAlpha > 0
                    ? new Color(255, 60, 60, (int) (alpha * 255)).getRGB()
                    : new Color(0, 220, 255, (int) (alpha * 255)).getRGB();

            RenderUtil.drawRoundedRectOutline(x, y, width, height, 10.0F, 1.5F, outlineColor, true, true, true, true);
        }

        if (this.headTexture != null) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
            mc.getTextureManager().bindTexture(this.headTexture);
            Gui.drawScaledCustomSizeModalRect(x + 6, y + 6, 8, 8, 8, 8, 32, 32, 64, 64);
            Gui.drawScaledCustomSizeModalRect(x + 6, y + 6, 40, 8, 8, 8, 32, 32, 64, 64);
        }

        String targetName = TeamUtil.stripName(entity);
        mc.fontRendererObj.drawStringWithShadow(
                targetName,
                x + 45,
                y + 7,
                new Color(255, 255, 255, (int) (alpha * 255)).getRGB()
        );

        float hp = this.animatedHealth;
        float maxHp = entity.getMaxHealth();
        float healthPercent = Math.max(0.0F, Math.min(1.0F, hp / maxHp));
        float armorPercent = Math.max(0.0F, Math.min(1.0F, this.animatedArmor / 20.0F));

        int barX = x + 45;
        int healthBarY = y + 24;
        int armorBarY = y + 34;
        int barWidth = 110;

        RenderUtil.drawRoundedRect(barX, healthBarY, barWidth, 6, 3, new Color(20, 20, 20, 140).getRGB());

        int cyan = new Color(0, 220, 255).getRGB();
        int blue = new Color(0, 120, 255).getRGB();

        if (RenderFixes.shouldUseShaders()) {
            BlurUtils.prepareBloom();
            RoundedUtils.drawRound(barX, healthBarY, barWidth * healthPercent, 6, 3, true, new Color(0, 220, 255, 180));
            BlurUtils.bloomEnd(4, 3.0F);
        }

        RenderUtil.drawRoundedGradientRect(barX, healthBarY, barX + barWidth * healthPercent, healthBarY + 6, 3, cyan, cyan, blue, blue);

        RenderUtil.drawRoundedRect(barX, armorBarY, barWidth, 4, 2, new Color(20, 20, 20, 120).getRGB());
        RenderUtil.drawRoundedRect(barX, armorBarY, barWidth * armorPercent, 4, 2, new Color(70, 170, 255).getRGB());

        mc.fontRendererObj.drawStringWithShadow(
                String.format("%.1f \u2764", hp),
                barX,
                y + 43,
                new Color(220, 220, 220, (int) (alpha * 255)).getRGB()
        );

        mc.fontRendererObj.drawStringWithShadow(
                Math.round(this.animatedArmor) + " \u26e8",
                barX + 60,
                y + 43,
                new Color(120, 200, 255, (int) (alpha * 255)).getRGB()
        );

        GL11.glPopMatrix();
    }

    private boolean shouldUseHudShadow() {
        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
        return hud != null && hud.isEnabled() && hud.shadow.getValue();
    }

    private boolean shouldUseHudBlur() {
        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
        return hud != null && hud.isEnabled() && hud.background.getValue() > 0 && hud.blur.getValue();
    }

    private float getHudBlurRadius() {
        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
        return hud != null && hud.isEnabled() ? hud.blurRadius.getValue() : 10.0F;
    }

    private boolean shouldUseHudGlow() {
        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
        return hud != null && hud.isEnabled() && hud.glow.getValue();
    }

    private Color getHudGlowColor() {
        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
        if (hud != null && hud.isEnabled()) {
            if (hud.glowColorMode.getValue() == 0) {
                return hud.getColor(System.currentTimeMillis(), 0);
            }
            return new Color(hud.glowCustomColor.getValue());
        }
        return Color.BLACK;
    }

    private int getHudGlowAlpha() {
        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
        return hud != null && hud.isEnabled() ? hud.glowAlpha.getValue() : 100;
    }

    private float getHudGlowRadius() {
        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
        return hud != null && hud.isEnabled() ? hud.glowRadius.getValue() : 3.0F;
    }

    private void renderMyauPlusTargetHud() {
        EntityLivingBase currentTarget = this.resolveTarget();
        boolean isOut = currentTarget == null;

        if (animMode.getValue() == 0) {
            openingAnimation.setDuration(isOut ? 250 : 650);
            openingAnimation.setEasing(isOut ? AnimationUtil.Easing.EASE_IN_BACK : AnimationUtil.Easing.EASE_OUT_ELASTIC);
        } else {
            openingAnimation.setDuration(isOut ? 200 : 350);
            openingAnimation.setEasing(AnimationUtil.Easing.EASE_OUT_QUINT);
        }

        openingAnimation.run(isOut ? 0 : 1);

        double animValue = openingAnimation.getValue();
        if (animValue <= 0.01 && isOut) {
            this.renderTarget = null;
            return;
        }

        if (currentTarget != null) {
            this.renderTarget = currentTarget;
        }
        if (this.renderTarget == null) {
            return;
        }

        float[] size = getMyauPlusStyleSize();
        float width = size[0];
        float height = size[1];
        float[] pos = getMyauPlusPosition(width, height);

        healthAnimation.run(renderTarget.getHealth());
        float renderHealth = (float) healthAnimation.getValue();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GlStateManager.enableBlend();

        float finalScale = Math.max(0.001F, (float) (this.scale.getValue() * animValue));
        float centerX = pos[0] + width / 2.0F;
        float centerY = pos[1] + height / 2.0F;

        GlStateManager.pushMatrix();
        GlStateManager.translate(centerX, centerY, 0.0F);
        GlStateManager.scale(finalScale, finalScale, 1.0F);
        GlStateManager.translate(-centerX, -centerY, 0.0F);

        checkSetupFBO();

        boolean useBlur = shouldUseHudBlur();
        boolean useShadow = shouldUseHudShadow();
        boolean useGlow = shouldUseHudGlow();

        if (useBlur) {
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
            GL11.glEnable(GL11.GL_STENCIL_TEST);
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
            GL11.glColorMask(false, false, false, false);
            RenderUtil.drawRect(pos[0], pos[1], pos[0] + width, pos[1] + height, -1);
            GL11.glColorMask(true, true, true, true);
            GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);
            BlurShader.renderBlur(getHudBlurRadius(), pos[0], pos[1], width, height, 1.0F);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
        }

        if (useGlow) {
            GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
            GL11.glEnable(GL11.GL_STENCIL_TEST);
            GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
            GL11.glColorMask(false, false, false, false);
            RenderUtil.drawRect(pos[0], pos[1], pos[0] + width, pos[1] + height, -1);
            GL11.glColorMask(true, true, true, true);
            GL11.glStencilFunc(GL11.GL_NOTEQUAL, 1, 0xFF);
            GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP);

            int finalGlowColor = ColorUtil.withAlpha(getHudGlowColor(), getHudGlowAlpha()).getRGB();
            ShadowShader.drawShadow(pos[0], pos[1], width, height, getHudGlowRadius(), getHudGlowRadius() + 5.0F, finalGlowColor);

            GL11.glDisable(GL11.GL_STENCIL_TEST);
        }

        switch (getMyauPlusStyle()) {
            case 0:
                renderAstolfo(pos[0], pos[1], width, height, renderHealth, useShadow);
                break;
            case 1:
                renderExhibition(pos[0], pos[1], width, height, renderHealth, useShadow);
                break;
            case 2:
                renderMoon(pos[0], pos[1], width, height, renderHealth, useShadow);
                break;
            case 3:
                renderRise(pos[0], pos[1], width, height, renderHealth, useShadow);
                break;
            case 4:
                renderNeverlose(pos[0], pos[1], renderHealth, useShadow);
                break;
            case 5:
                renderTenacity(pos[0], pos[1], width, height, renderHealth, useShadow);
                break;
            default:
                break;
        }

        GlStateManager.popMatrix();
        GL11.glPopAttrib();
        GlStateManager.resetColor();
    }

    private void renderRise(float x, float y, float width, float height, float health, boolean shadow) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        if (shadow) {
            ShadowShader.drawShadow(0, 0, width, height, 8, 5, new Color(0, 0, 0, 100).getRGB());
        }

        RenderUtil.drawRoundedRect(0, 0, width, height, 8, new Color(10, 10, 10, bgAlpha.getValue()).getRGB());

        float healthPct = MathHelper.clamp_float(health / renderTarget.getMaxHealth(), 0, 1);
        float barWidth = width - 48;

        RenderUtil.drawRoundedRect(42, 22.0F, barWidth, 6, 3, new Color(0, 0, 0, 120).getRGB());

        if (healthPct > 0.01F) {
            float displayW = Math.max(6, barWidth * healthPct);
            drawGradientRoundedRect(42, 22.0F, displayW, 6, 3, getMyauPlusColor(0), getMyauPlusColor((int) (displayW * 2)));
        }

        drawMyauPlusFace(renderTarget, 4.0F, 3.3F, 33, 33, 6);
        FontManager.productSans18.drawString(String.format("%.1f", health), width - 30, 9, getMyauPlusColor(0).getRGB(), true);
        FontManager.productSans18.drawString(renderTarget.getName(), 42, 9, -1, true);
        GlStateManager.popMatrix();
    }

    private void renderAstolfo(float x, float y, float width, float height, float health, boolean shadow) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        if (shadow) {
            ShadowShader.drawShadow(0, 0, width, height, 6, 6, new Color(0, 0, 0, 100).getRGB());
        }

        RenderUtil.drawRect(0, 0, width, height, new Color(0, 0, 0, bgAlpha.getValue()).getRGB());

        drawEntityOnScreen(25, 45, renderTarget);
        FontManager.regular18.drawString(renderTarget.getName(), 50, 6, -1, true);

        GlStateManager.pushMatrix();
        GlStateManager.scale(1.5F, 1.5F, 1.5F);
        FontManager.regular18.drawString(String.format("%.1f", health) + " \u2764", 50 / 1.5F, 22 / 1.5F, getMyauPlusColor(0).getRGB(), true);
        GlStateManager.popMatrix();

        float healthPct = MathHelper.clamp_float(health / renderTarget.getMaxHealth(), 0, 1);
        float barWidth = width - 54;
        RenderUtil.drawRect(48, 42, 48 + barWidth, 49, ColorUtil.darker(getMyauPlusColor(0), 0.3F).getRGB());

        drawHorizontalGradientRect(48, 42, barWidth * healthPct, 7, getMyauPlusColor(0).getRGB(), getMyauPlusColor((int) (barWidth * healthPct * 2)).getRGB());
        GlStateManager.popMatrix();
    }

    private void renderExhibition(float x, float y, float width, float height, float health, boolean shadow) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        if (shadow) {
            ShadowShader.drawShadow(-2.5F, -2.5F, width + 5.0F, height + 5.0F, 4, 4, new Color(0, 0, 0, 100).getRGB());
        }

        drawExhibitionBorderedRect(-2.5F, -2.5F, width + 2.5F, height + 2.5F, 0.5F, getExhibitionColor(60), getExhibitionColor(10));
        drawExhibitionBorderedRect(-1.5F, -1.5F, width + 1.5F, height + 1.5F, 1.5F, getExhibitionColor(60), getExhibitionColor(40));
        drawExhibitionBorderedRect(0, 0, width, height, 0.5F, getExhibitionColor(22), getExhibitionColor(60));
        drawExhibitionBorderedRect(2, 2, 38, 38, 0.5F, getExhibitionColor(0, 0), getExhibitionColor(10));
        drawExhibitionBorderedRect(2.5F, 2.5F, 37.5F, 37.5F, 0.5F, getExhibitionColor(17), getExhibitionColor(48));
        drawEntityOnScreen(20, 36, renderTarget);

        FontManager.tahomaBold16.drawString(renderTarget.getName(), 46, 4, -1, true);

        float pct = MathHelper.clamp_float(health / renderTarget.getMaxHealth(), 0, 1);
        Color hpColor = blendColors(new float[]{0.0F, 0.5F, 1.0F}, new Color[]{Color.RED, Color.YELLOW, Color.GREEN}, pct);

        RenderUtil.drawRect(42, 12, width - 8, 16, getExhibitionColor(0, 0));
        RenderUtil.drawRect(42.5F, 12.5F, 42.5F + (width - 51.0F) * pct, 15.5F, hpColor.getRGB());

        FontManager.tahomaBold12.drawString("HP: " + (int) health + " | Dist: " + (int) mc.thePlayer.getDistanceToEntity(renderTarget), 46, 19, -1, true);
        GlStateManager.popMatrix();
    }

    private void renderMoon(float x, float y, float width, float height, float health, boolean shadow) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        if (shadow) {
            ShadowShader.drawShadow(0, 0, width, height, 8, 5, new Color(0, 0, 0, 100).getRGB());
        }

        RenderUtil.drawRoundedRect(0, 0, width, height, 8, new Color(20, 20, 20, bgAlpha.getValue()).getRGB());

        float pct = MathHelper.clamp_float(health / renderTarget.getMaxHealth(), 0, 1);
        float barWidth = width - 48;
        RenderUtil.drawRoundedRect(42, 26.5F, barWidth, 8, 4, new Color(0, 0, 0, 150).getRGB());

        if (pct > 0.01F) {
            float displayW = Math.max(8, barWidth * pct);
            drawGradientRoundedRect(42, 26.5F, displayW, 8.5F, 4, getMyauPlusColor(0), getMyauPlusColor((int) (displayW * 2)));
        }

        drawMyauPlusFace(renderTarget, 2.5F, 2.5F, 35, 35, 8);
        FontManager.tenacity12.drawString(String.format("%.1f", health) + "HP", 40, 17, -1, true);
        FontManager.tenacity16.drawString(renderTarget.getName(), 40, 6, -1, true);
        GlStateManager.popMatrix();
    }

    private void renderNeverlose(float x, float y, float health, boolean shadow) {
        float width = Math.max(125.0F, (float) (FontManager.tenacity16.getStringWidth(renderTarget.getName()) + 42));
        float height = 32.5F;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        if (shadow) {
            ShadowShader.drawShadow(0, 0, width, height, 4, 4, new Color(0, 0, 0, 150).getRGB());
        }

        RenderUtil.drawRoundedRect(0, 0, width, height, 4, new Color(10, 10, 16, bgAlpha.getValue()).getRGB());

        drawMyauPlusFace(renderTarget, 3, 3, 26, 26, 4);

        float circleX = width - 15.0F;
        drawCircle(circleX, new Color(0, 0, 0, 100).getRGB());
        drawArc(circleX, 360 * MathHelper.clamp_float(health / renderTarget.getMaxHealth(), 0, 1), getMyauPlusColor(0).getRGB());

        FontManager.tenacity16.drawString(renderTarget.getName(), 34, 8, -1, true);
        FontManager.tenacity12.drawString("Dist: " + String.format("%.1f", renderTarget.getDistanceToEntity(mc.thePlayer)) + "m", 34, 20, getMyauPlusColor(0).getRGB(), true);
        GlStateManager.popMatrix();
    }

    private void renderTenacity(float x, float y, float width, float height, float health, boolean shadow) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        if (shadow) {
            ShadowShader.drawShadow(0, 0, width, height, 6, 6, new Color(0, 0, 0, 120).getRGB());
        }

        RenderUtil.drawRoundedRect(0, 0, width, height, 6, new Color(0, 0, 0, bgAlpha.getValue()).getRGB());

        drawMyauPlusFace(renderTarget, 4, 4, 34, 34, 6);
        FontManager.tenacity20.drawString(renderTarget.getName(), 43, 10, -1, true);
        FontManager.tenacity12.drawString("HP: " + String.format("%.1f", health), 43, 20, -1, true);

        float pct = MathHelper.clamp_float(health / renderTarget.getMaxHealth(), 0, 1);
        float barWidth = width - 52;
        RenderUtil.drawRoundedRect(44, 30, barWidth, 6, 3, new Color(0, 0, 0, 150).getRGB());

        if (pct > 0.01F) {
            float displayW = Math.max(6, barWidth * pct);
            drawGradientRoundedRect(44, 30, displayW, 6, 3, getMyauPlusColor(0), getMyauPlusColor((int) (displayW * 2)));
        }
        GlStateManager.popMatrix();
    }

    private void drawGradientRoundedRect(float x, float y, float width, float height, float radius, Color startColor, Color endColor) {
        if (width <= 0 || height <= 0) {
            return;
        }

        float r = Math.min(radius, Math.min(width, height) * 0.5F);

        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glStencilMask(0xFF);

        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.0F);
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);

        GL11.glColorMask(false, false, false, false);
        RenderUtil.drawRoundedRect(x, y, width, height, r, -1, true, true, true, true);
        GL11.glColorMask(true, true, true, true);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilMask(0x00);

        drawHorizontalGradientRect(x, y, width, height, startColor.getRGB(), endColor.getRGB());

        GL11.glStencilMask(0xFF);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private void drawHorizontalGradientRect(float x, float y, float width, float height, int startColor, int endColor) {
        float startA = (float) (startColor >> 24 & 255) / 255.0F;
        float startR = (float) (startColor >> 16 & 255) / 255.0F;
        float startG = (float) (startColor >> 8 & 255) / 255.0F;
        float startB = (float) (startColor & 255) / 255.0F;
        float endA = (float) (endColor >> 24 & 255) / 255.0F;
        float endR = (float) (endColor >> 16 & 255) / 255.0F;
        float endG = (float) (endColor >> 8 & 255) / 255.0F;
        float endB = (float) (endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);

        worldrenderer.pos(x, y, 0.0D).color(startR, startG, startB, startA).endVertex();
        worldrenderer.pos(x, y + height, 0.0D).color(startR, startG, startB, startA).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).color(endR, endG, endB, endA).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).color(endR, endG, endB, endA).endVertex();

        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    private Color getMyauPlusColor(int offset) {
        switch (colorMode.getValue()) {
            case 0:
                HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
                return hud != null ? hud.getColor(System.currentTimeMillis(), offset) : new Color(0, 150, 255);
            case 1:
                return new Color(customColor.getValue());
            case 2:
                return ColorUtil.getHealthBlend(renderTarget.getHealth() / renderTarget.getMaxHealth());
            case 3:
                float h = ((System.currentTimeMillis() + offset) % 3000) / 3000.0F;
                return Color.getHSBColor(h > 0.5F ? 1.0F - h : h + 0.5F, 0.5F, 1.0F);
            default:
                return Color.WHITE;
        }
    }

    private float[] getMyauPlusStyleSize() {
        if (renderTarget == null) {
            return new float[]{120, 40};
        }

        String name = renderTarget.getName();
        switch (getMyauPlusStyle()) {
            case 0:
                return new float[]{Math.max(130, (float) (FontManager.regular18.getStringWidth(name) + 60)), 56};
            case 1:
                return new float[]{Math.max(120, (float) (FontManager.tahomaBold16.getStringWidth(name) + 50)), 40};
            case 2:
                return new float[]{Math.max(110, (float) (FontManager.tenacity16.getStringWidth(name) + 68)), 40.5F};
            case 3:
                return new float[]{Math.max(160, (float) (FontManager.productSans18.getStringWidth(name) + 30)), 40.5F};
            case 4:
                return new float[]{Math.max(125, (float) (FontManager.tenacity16.getStringWidth(name) + 42)), 32.5F};
            case 5:
                return new float[]{Math.max(120, (float) (FontManager.tenacity20.getStringWidth(name) + 50)), 44};
            default:
                return new float[]{120, 40};
        }
    }

    private float[] getMyauPlusPosition(float width, float height) {
        ScaledResolution sr = new ScaledResolution(mc);
        float x = offX.getValue().floatValue();
        float y = offY.getValue().floatValue();
        switch (posX.getValue()) {
            case 1:
                x += sr.getScaledWidth() / 2.0F - width / 2.0F;
                break;
            case 2:
                x = sr.getScaledWidth() - width - x;
                break;
            default:
                break;
        }
        switch (posY.getValue()) {
            case 1:
                y += sr.getScaledHeight() / 2.0F - height / 2.0F;
                break;
            case 2:
                y = sr.getScaledHeight() - height - y;
                break;
            default:
                break;
        }
        return new float[]{x, y};
    }

    private void drawMyauPlusFace(EntityLivingBase entity, float x, float y, float w, float h, float r) {
        if (!(entity instanceof EntityPlayer)) {
            return;
        }

        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(entity.getName());
        ResourceLocation skin = info != null ? info.getLocationSkin() : new ResourceLocation("textures/entity/steve.png");

        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glStencilMask(0xFF);

        GL11.glColorMask(false, false, false, false);
        RenderUtil.drawRoundedRect(x, y, w, h, r, -1, true, true, true, true);
        GL11.glColorMask(true, true, true, true);

        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilMask(0x00);
        drawHeadTexture(skin, x, y, w, h);
        GL11.glStencilMask(0xFF);
        GL11.glDisable(GL11.GL_STENCIL_TEST);
    }

    private void drawHeadTexture(ResourceLocation skin, float x, float y, float w, float h) {
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(skin);
        Gui.drawScaledCustomSizeModalRect(Math.round(x), Math.round(y), 8.0F, 8.0F, 8, 8, Math.round(w), Math.round(h), 64.0F, 64.0F);
        Gui.drawScaledCustomSizeModalRect(Math.round(x), Math.round(y), 40.0F, 8.0F, 8, 8, Math.round(w), Math.round(h), 64.0F, 64.0F);
        GlStateManager.resetColor();
    }

    private void drawEntityOnScreen(int x, int y, EntityLivingBase entity) {
        try {
            GlStateManager.enableColorMaterial();
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, 50.0F);
            float size = 16.0F / Math.max(entity.height / 1.8F, 1.0F);
            GlStateManager.scale(-size, size, size);
            GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
            RenderHelper.enableStandardItemLighting();
            RenderManager rm = mc.getRenderManager();
            rm.setRenderShadow(false);
            rm.renderEntityWithPosYaw(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
            rm.setRenderShadow(true);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.popMatrix();
        } catch (Exception ignored) {
        }
    }

    private void drawExhibitionBorderedRect(float x1, float y1, float x2, float y2, float border, int fill, int out) {
        RenderUtil.drawRect(x1, y1, x2, y2, out);
        RenderUtil.drawRect(x1 + border, y1 + border, x2 - border, y2 - border, fill);
    }

    private int getExhibitionColor(int b) {
        return new Color(b, b, b, 255).getRGB();
    }

    private int getExhibitionColor(int b, int a) {
        return new Color(b, b, b, a).getRGB();
    }

    private Color blendColors(float[] fractions, Color[] colors, float progress) {
        if (progress >= 1.0F) {
            return colors[colors.length - 1];
        }
        if (progress <= 0.0F) {
            return colors[0];
        }

        int index = 0;
        while (index < fractions.length - 2 && fractions[index + 1] <= progress) {
            index++;
        }

        float factor = (progress - fractions[index]) / (fractions[index + 1] - fractions[index]);
        return new Color(
                (int) (colors[index].getRed() + (colors[index + 1].getRed() - colors[index].getRed()) * factor),
                (int) (colors[index].getGreen() + (colors[index + 1].getGreen() - colors[index].getGreen()) * factor),
                (int) (colors[index].getBlue() + (colors[index + 1].getBlue() - colors[index].getBlue()) * factor)
        );
    }

    private void drawCircle(float x, int color) {
        RenderUtil.enableRenderState();
        RenderUtil.setColor(color);
        GL11.glLineWidth(3.0F);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= 360; i++) {
            GL11.glVertex2d(x + Math.sin(Math.toRadians(i)) * 12, 16.0F + Math.cos(Math.toRadians(i)) * 12);
        }
        GL11.glEnd();
        RenderUtil.disableRenderState();
    }

    private void drawArc(float x, float deg, int color) {
        RenderUtil.enableRenderState();
        RenderUtil.setColor(color);
        GL11.glLineWidth(3.0F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i <= deg; i++) {
            GL11.glVertex2d(x + Math.sin(Math.toRadians(i)) * 12, 16.0F - Math.cos(Math.toRadians(i)) * 12);
        }
        GL11.glEnd();
        RenderUtil.disableRenderState();
    }

    private void checkSetupFBO() {
        Framebuffer fbo = mc.getFramebuffer();
        if (fbo != null && fbo.depthBuffer > -1) {
            EXTFramebufferObject.glDeleteRenderbuffersEXT(fbo.depthBuffer);
            int stencilDepthBufferId = EXTFramebufferObject.glGenRenderbuffersEXT();
            EXTFramebufferObject.glBindRenderbufferEXT(36161, stencilDepthBufferId);
            EXTFramebufferObject.glRenderbufferStorageEXT(36161, 34041, mc.displayWidth, mc.displayHeight);
            EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36128, 36161, stencilDepthBufferId);
            EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36096, 36161, stencilDepthBufferId);
            fbo.depthBuffer = -1;
        }
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
            if (packet.getAction() == Action.ATTACK) {
                Entity entity = packet.getEntityFromWorld(mc.theWorld);
                if (entity instanceof EntityLivingBase && !(entity instanceof EntityArmorStand)) {
                    this.lastAttackTimer.reset();
                    this.lastTarget = (EntityLivingBase) entity;
                }
            }
        }
    }

    @Override
    public void onDisabled() {
        super.onDisabled();
        this.target = null;
        this.renderTarget = null;
        this.fadingEntity = null;
        this.fadeTimer = null;
    }
}
