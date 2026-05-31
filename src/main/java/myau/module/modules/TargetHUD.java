package myau.module.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import myau.Myau;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.property.properties.*;
import myau.util.ColorUtil;
import myau.util.RenderUtil;
import myau.util.TeamUtil;
import myau.util.TimerUtil;
import myau.util.shader.BlurUtils;
import myau.util.shader.RoundedUtils;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class TargetHUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat healthFormat = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.US));
    private static final DecimalFormat diffFormat = new DecimalFormat("+0.0;-0.0", new DecimalFormatSymbols(Locale.US));
    public final ModeProperty style = new ModeProperty("style", 0, new String[]{"DEFAULT", "RAVENBS-MODERN", "RAVENBS-LEGACY", "FACE", "THREED", "SIMPLE", "CIRCLE"});
    public final ModeProperty color = new ModeProperty("color", 0, new String[]{"DEFAULT", "HUD"});
    public final ModeProperty posX = new ModeProperty("position-x", 1, new String[]{"LEFT", "MIDDLE", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 1, new String[]{"TOP", "MIDDLE", "BOTTOM"});
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final IntProperty offX = new IntProperty("offset-x", 0, -255, 255);
    public final IntProperty offY = new IntProperty("offset-y", 40, -255, 255);
    public final PercentProperty background = new PercentProperty("background", 25, () -> this.style.getValue() == 0);
    public final BooleanProperty head = new BooleanProperty("head", true, () -> this.style.getValue() == 0);
    public final BooleanProperty indicator = new BooleanProperty("indicator", true, () -> this.style.getValue() == 0);
    public final BooleanProperty outline = new BooleanProperty("outline", false, () -> this.style.getValue() == 0 || this.style.getValue() == 1 || this.style.getValue() >= 3);
    public final BooleanProperty animations = new BooleanProperty("animations", true, () -> this.style.getValue() == 0);
    public final BooleanProperty kaOnly = new BooleanProperty("ka-only", true);
    public final BooleanProperty chatPreview = new BooleanProperty("chat-preview", false);
    private final TimerUtil lastAttackTimer = new TimerUtil();
    private final TimerUtil animTimer = new TimerUtil();
    private EntityLivingBase lastTarget = null;
    private EntityLivingBase target = null;
    private ResourceLocation headTexture = null;
    private float oldHealth = 0.0F;
    private float newHealth = 0.0F;
    private float maxHealth = 0.0F;
    private float lastHealthBar = 0.0F;
    private TimerUtil fadeTimer = null;
    private boolean fadingIn = false;
    private EntityLivingBase fadingEntity = null;
    private float animatedHealth = 0f;
    private float animatedArmor = 0f;
    private float animatedScale = 0f;
    private long damageFlashTime = 0;
    private float lastHealthVal = 0f;

    public TargetHUD() {
        super("TargetHUD", false, true);
    }

    private EntityLivingBase resolveTarget() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura.isEnabled() && killAura.isAttackAllowed() && TeamUtil.isEntityLoaded(killAura.getTarget())) {
            return killAura.getTarget();
        } else if (!(Boolean) this.kaOnly.getValue()
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
                int rgb = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis()).getRGB();
                return new Color(rgb);
            default:
                return new Color(-1);
        }
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled() && mc.thePlayer != null) {
            EntityLivingBase entityLivingBase = this.target;
            this.target = this.resolveTarget();

            if (this.target != null) {

                if (entityLivingBase == null && fadeTimer == null) {

                    fadeTimer = new TimerUtil();
                    fadeTimer.reset();
                    fadingIn = true;
                } else if (fadingIn && fadeTimer != null && fadeTimer.getElapsedTime() >= 400) {

                    fadeTimer = null;
                    fadingIn = false;
                }
            } else {

                if (entityLivingBase != null && fadeTimer == null) {
                    fadeTimer = new TimerUtil();
                    fadeTimer.reset();
                    fadingIn = false;
                    fadingEntity = entityLivingBase;
                }
            }

            if (entityLivingBase != null || fadeTimer != null) {

                EntityLivingBase entity = this.target != null ? this.target : fadingEntity;
                if (entity == null) {

                    return;
                }
                float health = (mc.thePlayer.getHealth() + mc.thePlayer.getAbsorptionAmount()) / 2.0F;
                float abs = entity.getAbsorptionAmount() / 2.0F;
                float heal = entity.getHealth() / 2.0F + abs;

                float entityHealth = entity.getHealth();
                if (entityHealth < this.lastHealthVal) {
                    this.damageFlashTime = System.currentTimeMillis();
                }
                this.lastHealthVal = entityHealth;

                if (entity != this.target) {
                    this.headTexture = null;
                    this.animTimer.setTime();
                    this.oldHealth = heal;
                    this.newHealth = heal;
                    this.animatedHealth = entityHealth;
                    this.animatedArmor = entity instanceof EntityPlayer ? ((EntityPlayer) entity).getTotalArmorValue() : 0;
                }
                if (!this.animations.getValue() || this.animTimer.hasTimeElapsed(150L)) {
                    this.oldHealth = this.newHealth;
                    this.newHealth = heal;
                    this.maxHealth = entity.getMaxHealth() / 2.0F;
                    if (this.oldHealth != this.newHealth) {
                        this.animTimer.reset();
                    }
                }

                float targetHealthVal = entityHealth;
                if (this.animatedHealth == 0f || entity == mc.thePlayer) this.animatedHealth = targetHealthVal;
                this.animatedHealth = this.animatedHealth + (targetHealthVal - this.animatedHealth) * 0.1f;

                int targetArmor = entity instanceof EntityPlayer ? ((EntityPlayer) entity).getTotalArmorValue() : 0;
                if (this.animatedArmor == 0f || entity == mc.thePlayer) this.animatedArmor = targetArmor;
                this.animatedArmor = this.animatedArmor + ((float) targetArmor - this.animatedArmor) * 0.1f;

                if (fadeTimer != null) {
                    long elapsed = fadeTimer.getElapsedTime();
                    this.animatedScale = fadingIn ? Math.min(1f, elapsed / 400f) : Math.max(0f, 1f - elapsed / 400f);
                } else {
                    this.animatedScale = this.target != null ? 1f : 0f;
                }

                ResourceLocation resourceLocation = this.getSkin(entity);
                if (resourceLocation != null) {
                    this.headTexture = resourceLocation;
                }

                int styleMode = this.style.getValue();
                if (styleMode == 0) {
                    drawDefaultStyle(entity, health, abs, heal);
                } else if (styleMode <= 2) {
                    drawRavenBSStyle(styleMode - 1, entity, health, abs, heal);
                } else {
                    drawRavenStyle(styleMode - 3, entity);
                }
            }
        }
    }

    private void drawDefaultStyle(EntityLivingBase entity, float health, float abs, float heal) {
        float elapsedTime = (float) Math.min(Math.max(this.animTimer.getElapsedTime(), 0L), 150L);
        float lerpedHealthRatio = Math.min(Math.max(RenderUtil.lerpFloat(this.newHealth, this.oldHealth, elapsedTime / 150.0F) / this.maxHealth, 0.0F), 1.0F);
        Color targetColor = this.getTargetColor(entity);
        Color healthBarColor = this.color.getValue() == 0 ? ColorUtil.getHealthBlend(lerpedHealthRatio) : targetColor;
        float healthDeltaRatio = Math.min(Math.max((health - heal + 1.0F) / 2.0F, 0.0F), 1.0F);
        Color healthDeltaColor = ColorUtil.getHealthBlend(healthDeltaRatio);
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        String targetNameText = ChatColors.formatColor(String.format("&r%s&r", TeamUtil.stripName(entity)));
        int targetNameWidth = mc.fontRendererObj.getStringWidth(targetNameText);
        String healthText = ChatColors.formatColor(
                String.format("&r&f%s%s❤&r", healthFormat.format(heal), abs > 0.0F ? "&6" : "&c")
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
        }
        float posY = this.offY.getValue().floatValue() / this.scale.getValue();
        switch (this.posY.getValue()) {
            case 1:
                posY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() / 2.0F - 13.5F;
                break;
            case 2:
                posY *= -1.0F;
                posY += (float) scaledResolution.getScaledHeight() / this.scale.getValue() - 27.0F;
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
        mc.fontRendererObj.drawString(targetNameText, headIconOffset + 2.0F, 2.0F, -1, RenderUtil.hudShadow());
        mc.fontRendererObj.drawString(healthText, headIconOffset + 2.0F, 12.0F, -1, RenderUtil.hudShadow());
        if (this.indicator.getValue()) {
            mc.fontRendererObj.drawString(statusText, barTotalWidth - 2.0F - (float) statusTextWidth, 2.0F, healthDeltaColor.getRGB(), RenderUtil.hudShadow());
            mc.fontRendererObj.drawString(healthDiffText, barTotalWidth - 2.0F - (float) healthDiffWidth, 12.0F, ColorUtil.darker(healthDeltaColor, 0.8F).getRGB(), RenderUtil.hudShadow());
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
        String healthStr = String.format("%.1f", heal);
        playerInfo += " §c" + healthStr;

        if (this.indicator.getValue()) {
            playerInfo += " " + ((healthRatio <= health / mc.thePlayer.getMaxHealth()) ? "§aW" : "§cL");
        }

        int alpha = 255;
        if (fadeTimer != null) {
            long elapsed = fadeTimer.getElapsedTime();
            if (elapsed < 400) {
                if (fadingIn) {

                    alpha = (int) ((elapsed / 400.0f) * 255);
                } else {

                    alpha = (int) (255 - (elapsed / 400.0f) * 255);
                }
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

        final ScaledResolution scaledResolution = new ScaledResolution(mc);
        final int padding = 8;
        final int targetStrWithPadding = mc.fontRendererObj.getStringWidth(playerInfo) + padding;
        final int x = (scaledResolution.getScaledWidth() / 2 - targetStrWithPadding / 2) + offX.getValue();
        final int y = (scaledResolution.getScaledHeight() / 2 + 15) + offY.getValue();
        final int n6 = x - padding;
        final int n7 = y - padding;
        final int n8 = x + targetStrWithPadding;
        final int n9 = y + (mc.fontRendererObj.FONT_HEIGHT + 5) - 6 + padding;

        final int maxAlphaOutline = Math.min(alpha, 110);
        final int maxAlphaBackground = Math.min(alpha, 210);

        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
        int gradientLeft = hud.getColor(System.currentTimeMillis()).getRGB();
        int gradientRight = hud.getColor(System.currentTimeMillis() + 500).getRGB();
        int[] gradientColors = new int[]{gradientLeft, gradientRight};

        switch (mode) {
            case 0:
                float bloomRadius = (fadeTimer == null) ? 2f : (2f * alpha / 255f);
                float blurRadius = (fadeTimer == null) ? 3 : (3f * alpha / 255f);
                if (RenderUtil.hudBloom()) {
                    BlurUtils.prepareBloom();
                    RoundedUtils.drawRound((float) n6, (float) n7, (float) (n8 - n6), (float) (n9 + 13 - n7), 8.0f, true, new Color(0, 0, 0, maxAlphaBackground));
                    BlurUtils.bloomEnd(3, bloomRadius);
                }
                if (RenderUtil.hudBlur()) {
                    BlurUtils.prepareBlur();
                    RoundedUtils.drawRound((float) n6, (float) n7, (float) (n8 - n6), (float) (n9 + 13 - n7), 8.0f, true, new Color(RenderUtil.mergeAlpha(Color.black.getRGB(), maxAlphaOutline)));
                    BlurUtils.blurEnd(2, blurRadius);
                }
                if (!RenderUtil.hudBlur() && !RenderUtil.hudBloom()) {
                    RenderUtil.drawRoundedRect((float) n6, (float) n7, (float) (n8 - n6), (float) (n9 + 13 - n7), 8.0f,
                            RenderUtil.mergeAlpha(Color.black.getRGB(), maxAlphaOutline), true, true, true, true);
                }
                break;
            case 1:
                RenderUtil.drawRoundedGradientOutlinedRectangle((float) n6, (float) n7, (float) n8, (float) (n9 + 13), 10.0f,
                        RenderUtil.mergeAlpha(Color.black.getRGB(), maxAlphaOutline),
                        RenderUtil.mergeAlpha(gradientColors[0], alpha),
                        RenderUtil.mergeAlpha(gradientColors[1], alpha));
                break;
        }

        final int n13 = n6 + 6;
        final int n14 = n8 - 6;
        final int n15 = n9;

        RenderUtil.drawRoundedRectangle((float) n13, (float) n15, (float) n14, (float) (n15 + 5), 4.0f,
                RenderUtil.mergeAlpha(Color.black.getRGB(), maxAlphaOutline));

        int mergedGradientLeft = RenderUtil.mergeAlpha(gradientColors[0], maxAlphaBackground);
        int mergedGradientRight = RenderUtil.mergeAlpha(gradientColors[1], maxAlphaBackground);

        float healthBar = (float) (n14 + (n13 - n14) * (1 - healthRatio));

        if (lastHealthBar != healthBar && lastHealthBar - n13 >= 3) {
            float diff = lastHealthBar - healthBar;
            if (diff > 0) {
                lastHealthBar = lastHealthBar - diff * 0.1f;
            } else {
                lastHealthBar = lastHealthBar + (-diff) * 0.1f;
            }
        } else {
            lastHealthBar = healthBar;
        }

        if (lastHealthBar > n14) {
            lastHealthBar = n14;
        }

        switch (mode) {
            case 0:
                RenderUtil.drawRoundedRectangle((float) n13, (float) n15, lastHealthBar, (float) (n15 + 5), 4.0f,
                        RenderUtil.darkenColor(mergedGradientRight, 25));
                RenderUtil.drawRoundedGradientRect((float) n13, (float) n15, healthBar, (float) (n15 + 5), 4.0f,
                        mergedGradientLeft, mergedGradientLeft, mergedGradientRight, mergedGradientRight);
                break;
            case 1:
                RenderUtil.drawRoundedGradientRect((float) n13, (float) n15, lastHealthBar, (float) (n15 + 5), 4.0f,
                        mergedGradientLeft, mergedGradientLeft, mergedGradientRight, mergedGradientRight);
                break;
        }

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        mc.fontRendererObj.drawString(playerInfo, (float) x, (float) y,
                (new Color(220, 220, 220, 255).getRGB() & 0xFFFFFF) | Math.min(alpha + 15, 255) << 24, RenderUtil.hudShadow());
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
        }
    }

    private void drawFaceStyle(EntityLivingBase entity) {
        float alpha = Math.min(1f, this.animatedScale);
        int baseX = HUD.targetHUDX;
        int baseY = HUD.targetHUDY;
        int hudWidth = 150;
        int hudHeight = 50;

        float sc = this.scale.getValue();
        GL11.glPushMatrix();
        GL11.glTranslated(baseX + hudWidth / 2.0, baseY + hudHeight / 2.0, 0);
        GL11.glScalef(sc, sc, 1.0f);
        GL11.glTranslated(-(baseX + hudWidth / 2.0), -(baseY + hudHeight / 2.0), 0);

        int x = baseX;
        int y = baseY;

        long timeSinceDamage = System.currentTimeMillis() - this.damageFlashTime;
        float flashAlpha = 0f;
        if (timeSinceDamage < 300) {
            flashAlpha = 1f - (timeSinceDamage / 300f);
        }

        int bgBase = new Color(26, 26, 26, (int)(alpha * 128)).getRGB();
        if (flashAlpha > 0) {
            int r = (int) (26 + (255 - 26) * flashAlpha);
            int g = (int) (26 * (1 - flashAlpha));
            int b = (int) (26 * (1 - flashAlpha));
            bgBase = new Color(r, g, b, (int)(alpha * 128)).getRGB();
        }
        RenderUtil.drawRoundedRect((float) x, (float) y, (float) hudWidth, (float) hudHeight, 8.0f, bgBase);

        int borderColor = (int) (alpha * 0xFF) << 24 | 0xFF8C00;
        if (flashAlpha > 0) {
            borderColor = (int) (alpha * 0xFF) << 24 | 0xFF0000;
        }
        if (this.outline.getValue()) {
            RenderUtil.drawRoundedRectOutline((float) x, (float) y, (float) hudWidth, (float) hudHeight, 8.0f, 2.0f, borderColor, true, true, true, true);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        if (this.headTexture != null) {
            mc.getTextureManager().bindTexture(this.headTexture);
            Gui.drawScaledCustomSizeModalRect(x + 5, y + 5, 8.0F, 8.0F, 8, 8, 30, 30, 64.0F, 64.0F);
        }

        String targetName = TeamUtil.stripName(entity);
        int nameColor = (int) (alpha * 0xFF) << 24 | 0xFFFFFF;
        mc.fontRendererObj.drawString(targetName, x + 40, y + 8, nameColor, RenderUtil.hudShadow());

        float maxHealth = entity.getMaxHealth();
        float healthPercent = Math.min(1f, this.animatedHealth / maxHealth);

        int healthBarY = y + 25;
        int healthBarWidth = hudWidth - 45;

        RenderUtil.drawRoundedRect((float) (x + 40), (float) healthBarY, (float) healthBarWidth, 8.0f, 4.0f,
                (int) (alpha * 0x40) << 24 | 0x404040);

        int healthColor = healthPercent > 0.5f ? 0xFF00FF00 : (healthPercent > 0.25f ? 0xFFFFFF00 : 0xFFFF0000);
        int healthFillColor = (int) (alpha * 0xFF) << 24 | healthColor;
        RenderUtil.drawRoundedRect((float) (x + 40), (float) healthBarY,
                (float) (int) (healthBarWidth * healthPercent), 8.0f, 4.0f, healthFillColor);

        float armorPercent = Math.min(1f, this.animatedArmor / 20f);
        int armorBarY = healthBarY + 10;
        RenderUtil.drawRoundedRect((float) (x + 40), (float) armorBarY, (float) healthBarWidth, 4.0f, 2.0f,
                (int) (alpha * 0x40) << 24 | 0x404040);

        int armorFillColor = (int) (alpha * 0xFF) << 24 | 0x00BFFF;
        RenderUtil.drawRoundedRect((float) (x + 40), (float) armorBarY,
                (float) (int) (healthBarWidth * armorPercent), 4.0f, 2.0f, armorFillColor);

        String healthText = String.format("%.1f/%.1f", this.animatedHealth, maxHealth);
        int healthTextColor = (int) (alpha * 0xFF) << 24 | 0xCCCCCC;
        mc.fontRendererObj.drawString(healthText, x + 40, y + 15, healthTextColor, RenderUtil.hudShadow());

        GL11.glPopMatrix();
    }

    private void draw3DStyle(EntityLivingBase entity) {
        float alpha = Math.min(1f, this.animatedScale);
        int baseX = HUD.targetHUDX;
        int baseY = HUD.targetHUDY;
        int hudWidth = 150;
        int hudHeight = 50;

        float sc = this.scale.getValue();
        GL11.glPushMatrix();
        GL11.glTranslated(baseX + hudWidth / 2.0, baseY + hudHeight / 2.0, 0);
        GL11.glScalef(sc, sc, 1.0f);
        GL11.glTranslated(-(baseX + hudWidth / 2.0), -(baseY + hudHeight / 2.0), 0);

        int x = baseX;
        int y = baseY;

        long timeSinceDamage = System.currentTimeMillis() - this.damageFlashTime;
        float flashAlpha = 0f;
        if (timeSinceDamage < 300) {
            flashAlpha = 1f - (timeSinceDamage / 300f);
        }

        int bgBase = new Color(26, 26, 26, (int)(alpha * 128)).getRGB();
        if (flashAlpha > 0) {
            int r = (int) (26 + (255 - 26) * flashAlpha);
            int g = (int) (26 * (1 - flashAlpha));
            int b = (int) (26 * (1 - flashAlpha));
            bgBase = new Color(r, g, b, (int)(alpha * 128)).getRGB();
        }
        RenderUtil.drawRoundedRect((float) x, (float) y, (float) hudWidth, (float) hudHeight, 8.0f, bgBase);

        int borderColor = (int) (alpha * 0xFF) << 24 | 0xFF8C00;
        if (flashAlpha > 0) {
            borderColor = (int) (alpha * 0xFF) << 24 | 0xFF0000;
        }
        if (this.outline.getValue()) {
            RenderUtil.drawRoundedRectOutline((float) x, (float) y, (float) hudWidth, (float) hudHeight, 8.0f, 2.0f, borderColor, true, true, true, true);
        }

        if (this.animatedScale > 0.5f && entity instanceof EntityPlayer) {
            try {
                GlStateManager.color(1f, 1f, 1f, alpha);
                GuiInventory.drawEntityOnScreen(x + 20, y + 34, 14, 0, 0, (EntityPlayer) entity);
            } catch (Exception ignored) {}
        } else if (this.headTexture != null) {
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
            mc.getTextureManager().bindTexture(this.headTexture);
            Gui.drawScaledCustomSizeModalRect(x + 5, y + 8, 8.0F, 8.0F, 8, 8, 24, 24, 64.0F, 64.0F);
        }

        String targetName = TeamUtil.stripName(entity);
        int nameColor = (int) (alpha * 0xFF) << 24 | 0xFFFFFF;
        mc.fontRendererObj.drawString(targetName, x + 40, y + 8, nameColor, RenderUtil.hudShadow());

        float maxHealth = entity.getMaxHealth();
        float healthPercent = Math.min(1f, this.animatedHealth / maxHealth);

        int healthBarY = y + 25;
        int healthBarWidth = hudWidth - 45;

        RenderUtil.drawRoundedRect((float) (x + 40), (float) healthBarY, (float) healthBarWidth, 8.0f, 4.0f,
                (int) (alpha * 0x40) << 24 | 0x404040);

        int healthColor = healthPercent > 0.5f ? 0xFF00FF00 : (healthPercent > 0.25f ? 0xFFFFFF00 : 0xFFFF0000);
        int healthFillColor = (int) (alpha * 0xFF) << 24 | healthColor;
        RenderUtil.drawRoundedRect((float) (x + 40), (float) healthBarY,
                (float) (int) (healthBarWidth * healthPercent), 8.0f, 4.0f, healthFillColor);

        float armorPercent = Math.min(1f, this.animatedArmor / 20f);
        int armorBarY = healthBarY + 10;
        RenderUtil.drawRoundedRect((float) (x + 40), (float) armorBarY, (float) healthBarWidth, 4.0f, 2.0f,
                (int) (alpha * 0x40) << 24 | 0x404040);

        int armorFillColor = (int) (alpha * 0xFF) << 24 | 0x00BFFF;
        RenderUtil.drawRoundedRect((float) (x + 40), (float) armorBarY,
                (float) (int) (healthBarWidth * armorPercent), 4.0f, 2.0f, armorFillColor);

        String healthText = String.format("%.1f/%.1f", this.animatedHealth, maxHealth);
        int healthTextColor = (int) (alpha * 0xFF) << 24 | 0xCCCCCC;
        mc.fontRendererObj.drawString(healthText, x + 40, y + 15, healthTextColor, RenderUtil.hudShadow());

        GL11.glPopMatrix();
    }

    private void drawSimpleStyle(EntityLivingBase entity) {
        float alpha = Math.min(1f, this.animatedScale);
        int baseX = HUD.targetHUDX;
        int baseY = HUD.targetHUDY;
        int hudWidth = 130;
        int hudHeight = 32;

        float sc = this.scale.getValue();
        GL11.glPushMatrix();
        GL11.glTranslated(baseX + hudWidth / 2.0, baseY + hudHeight / 2.0, 0);
        GL11.glScalef(sc, sc, 1.0f);
        GL11.glTranslated(-(baseX + hudWidth / 2.0), -(baseY + hudHeight / 2.0), 0);

        int x = baseX;
        int y = baseY;

        long timeSinceDamage = System.currentTimeMillis() - this.damageFlashTime;
        float flashAlpha = 0f;
        if (timeSinceDamage < 300) {
            flashAlpha = 1f - (timeSinceDamage / 300f);
        }

        int bgBase = new Color(26, 26, 26, (int)(alpha * 128)).getRGB();
        if (flashAlpha > 0) {
            int r = (int) (26 + (255 - 26) * flashAlpha);
            int g = (int) (26 * (1 - flashAlpha));
            int b = (int) (26 * (1 - flashAlpha));
            bgBase = new Color(r, g, b, (int)(alpha * 128)).getRGB();
        }
        RenderUtil.drawRoundedRect((float) x, (float) y, (float) hudWidth, (float) hudHeight, 8.0f, bgBase);

        int borderColor = (int) (alpha * 0xFF) << 24 | 0xFF8C00;
        if (flashAlpha > 0) {
            borderColor = (int) (alpha * 0xFF) << 24 | 0xFF0000;
        }
        if (this.outline.getValue()) {
            RenderUtil.drawRoundedRectOutline((float) x, (float) y, (float) hudWidth, (float) hudHeight, 8.0f, 2.0f, borderColor, true, true, true, true);
        }

        String targetName = TeamUtil.stripName(entity);
        int nameColor = (int) (alpha * 0xFF) << 24 | 0xFFFFFF;
        mc.fontRendererObj.drawString(targetName, x + 5, y + 5, nameColor, RenderUtil.hudShadow());

        float maxHealth = entity.getMaxHealth();
        float healthPercent = Math.min(1f, this.animatedHealth / maxHealth);

        int healthBarY = y + 19;
        int healthBarWidth = hudWidth - 10;

        RenderUtil.drawRoundedRect((float) (x + 5), (float) healthBarY, (float) healthBarWidth, 8.0f, 4.0f,
                (int) (alpha * 0x40) << 24 | 0x404040);

        int healthColor = healthPercent > 0.5f ? 0xFF00FF00 : (healthPercent > 0.25f ? 0xFFFFFF00 : 0xFFFF0000);
        int healthFillColor = (int) (alpha * 0xFF) << 24 | healthColor;
        RenderUtil.drawRoundedRect((float) (x + 5), (float) healthBarY,
                (float) (int) (healthBarWidth * healthPercent), 8.0f, 4.0f, healthFillColor);

        int actualHealthInt = Math.round(this.animatedHealth);
        String healthText = String.format("%d/%d", actualHealthInt, (int) maxHealth);
        int healthTextColor = (int) (alpha * 0xFF) << 24 | 0xCCCCCC;
        double healthTextWidth = mc.fontRendererObj.getStringWidth(healthText);
        mc.fontRendererObj.drawString(healthText, (int) (x + hudWidth - 5 - healthTextWidth), y + 5, healthTextColor, RenderUtil.hudShadow());

        GL11.glPopMatrix();
    }

    private void drawCircleStyle(EntityLivingBase entity) {
        float alpha = Math.min(1f, this.animatedScale);
        int baseX = HUD.targetHUDX;
        int baseY = HUD.targetHUDY;

        float sc = this.scale.getValue();
        GL11.glPushMatrix();
        GL11.glTranslated(baseX, baseY, 0);
        GL11.glScalef(sc, sc, 1.0f);
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
        float healthPercent = Math.min(1f, entity.getHealth() / maxHealth);
        int armor = entity instanceof EntityPlayer ? ((EntityPlayer) entity).getTotalArmorValue() : 0;
        float armorPercent = Math.min(1f, armor / 20f);

        float healthArc = healthPercent * 180f;
        float armorArc = armorPercent * 180f;

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
            GL11.glColor4f(0, 0.75f, 1, alpha);
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
        mc.fontRendererObj.drawString(targetName, x + 40, y + 8, nameColor, RenderUtil.hudShadow());

        String healthText = String.format("%.1f/%.1f", this.animatedHealth, maxHealth);
        int healthTextColor = (int) (alpha * 0xFF) << 24 | 0xCCCCCC;
        mc.fontRendererObj.drawString(healthText, x + 40, y + 22, healthTextColor, RenderUtil.hudShadow());

        GL11.glPopMatrix();
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
            if (packet.getAction() != Action.ATTACK) {
                return;
            }
            Entity entity = packet.getEntityFromWorld(mc.theWorld);
            if (entity instanceof EntityLivingBase) {
                if (entity instanceof EntityArmorStand) {
                    return;
                }
                this.lastAttackTimer.reset();
                this.lastTarget = (EntityLivingBase) entity;
            }
        }
    }
}
