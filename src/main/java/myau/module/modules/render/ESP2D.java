package myau.module.modules.render;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.mixin.IAccessorEntityRenderer;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.*;
import myau.util.ColorUtil;
import myau.util.RenderUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

import javax.vecmath.Vector4d;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Collectors;
//this class from LBPR
public class ESP2D extends Module {
    private final DecimalFormat dFormat = new DecimalFormat("0.0");

    private final BooleanProperty outline = new BooleanProperty("Outline", true);
    private final ModeProperty boxMode = new ModeProperty("Mode", 0, new String[]{"Box", "Corners"});

    private final BooleanProperty healthBar = new BooleanProperty("Health-Bar", true);
    private final ModeProperty hpBarMode = new ModeProperty("HBar-Mode", 0, new String[]{"Dot", "Line"}, healthBar::getValue);
    private final BooleanProperty absorption = new BooleanProperty("Render-Absorption", true, () -> healthBar.getValue() && hpBarMode.getValue() == 1);
    private final BooleanProperty healthNumber = new BooleanProperty("HealthNumber", true, healthBar::getValue);
    private final ModeProperty hpMode = new ModeProperty("HP-Mode", 0, new String[]{"Health", "Percent"}, () -> healthBar.getValue() && healthNumber.getValue());

    private final BooleanProperty armorBar = new BooleanProperty("Armor-Bar", true);
    private final ModeProperty armorBarMode = new ModeProperty("ABar-Mode", 0, new String[]{"Total", "Items"}, armorBar::getValue);
    private final BooleanProperty armorNumber = new BooleanProperty("ItemArmorNumber", true, armorBar::getValue);
    private final BooleanProperty armorItems = new BooleanProperty("ArmorItems", true);
    private final BooleanProperty armorDur = new BooleanProperty("ArmorDurability", true, armorItems::getValue);

    public final BooleanProperty tagsValue = new BooleanProperty("Tags", true);
    private final BooleanProperty tagsBGValue = new BooleanProperty("Tags-Background", false, tagsValue::getValue);
    private final BooleanProperty itemTagsValue = new BooleanProperty("Item-Tags", true);
    private final FloatProperty fontScaleValue = new FloatProperty("Font-Scale", 0.5f, 0.1f, 1.0f);

    private final BooleanProperty localPlayer = new BooleanProperty("Local-Player", true);
    private final BooleanProperty droppedItems = new BooleanProperty("Dropped-Items", false);
    private final ModeProperty colorModeValue = new ModeProperty("Color", 0, new String[]{"Custom", "Rainbow", "Sky", "Fade", "HUD", "Team"});
    private final IntProperty colorRedValue = new IntProperty("Red", 255, 0, 255, () -> colorModeValue.getValue() == 0);
    private final IntProperty colorGreenValue = new IntProperty("Green", 255, 0, 255, () -> colorModeValue.getValue() == 0);
    private final IntProperty colorBlueValue = new IntProperty("Blue", 255, 0, 255, () -> colorModeValue.getValue() == 0);
    private final FloatProperty saturationValue = new FloatProperty("Saturation", 1f, 0f, 1f, () -> colorModeValue.getValue() != 0 && colorModeValue.getValue() != 4);
    private final FloatProperty brightnessValue = new FloatProperty("Brightness", 1f, 0f, 1f, () -> colorModeValue.getValue() != 0 && colorModeValue.getValue() != 4);

    public ESP2D() {
        super("ESP2D",Category.RENDER, false, false);
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (!this.isEnabled()) return;

        ScaledResolution sr = new ScaledResolution(mc);
        double scaleFactor = sr.getScaleFactor();

        RenderUtil.enableRenderState();

        List<Entity> collectedEntities = mc.theWorld.loadedEntityList.stream()
                .filter(this::isValidEntity)
                .collect(Collectors.toList());

        for (Entity entity : collectedEntities) {
            if (!RenderUtil.isInViewFrustum(entity.getEntityBoundingBox(), 0.1f)) continue;

            ((IAccessorEntityRenderer) mc.entityRenderer).callSetupCameraTransform(event.getPartialTicks(), 0);
            Vector4d pos = RenderUtil.projectToScreen(entity, scaleFactor);
            mc.entityRenderer.setupOverlayRendering();

            if (pos == null) continue;

            double posX = pos.x;
            double posY = pos.y;
            double endPosX = pos.z;
            double endPosY = pos.w;

            if (Math.abs(posX) > 16000 || Math.abs(posY) > 16000) continue;

            int color = getColor(entity).getRGB();
            int black = Color.BLACK.getRGB();
            int background = new Color(0, 0, 0, 120).getRGB();

            if (outline.getValue()) {
                if (boxMode.getValue() == 0) {
                    RenderUtil.drawRect(posX - 1.0, posY, posX + 0.5, endPosY + 0.5, black);
                    RenderUtil.drawRect(posX - 1.0, posY - 0.5, endPosX + 0.5, posY + 0.5 + 0.5, black);
                    RenderUtil.drawRect(endPosX - 0.5 - 0.5, posY, endPosX + 0.5, endPosY + 0.5, black);
                    RenderUtil.drawRect(posX - 1.0, endPosY - 0.5 - 0.5, endPosX + 0.5, endPosY + 0.5, black);
                    RenderUtil.drawRect(posX - 0.5, posY, posX + 0.5 - 0.5, endPosY, color);
                    RenderUtil.drawRect(posX, endPosY - 0.5, endPosX, endPosY, color);
                    RenderUtil.drawRect(posX - 0.5, posY, endPosX, posY + 0.5, color);
                    RenderUtil.drawRect(endPosX - 0.5, posY, endPosX, endPosY, color);
                } else {
                    double lineW = (endPosX - posX) / 3.0;
                    double lineH = (endPosY - posY) / 4.0;
                    RenderUtil.drawRect(posX + 0.5, posY, posX - 1.0, posY + lineH + 0.5, black);
                    RenderUtil.drawRect(posX - 1.0, endPosY, posX + 0.5, endPosY - lineH - 0.5, black);
                    RenderUtil.drawRect(posX - 1.0, posY - 0.5, posX + lineW + 0.5, posY + 1.0, black);
                    RenderUtil.drawRect(endPosX - lineW - 0.5, posY - 0.5, endPosX, posY + 1.0, black);
                    RenderUtil.drawRect(endPosX - 1.0, posY, endPosX + 0.5, posY + lineH + 0.5, black);
                    RenderUtil.drawRect(endPosX - 1.0, endPosY, endPosX + 0.5, endPosY - lineH - 0.5, black);
                    RenderUtil.drawRect(posX - 1.0, endPosY - 1.0, posX + lineW + 0.5, endPosY + 0.5, black);
                    RenderUtil.drawRect(endPosX - lineW - 0.5, endPosY - 1.0, endPosX + 0.5, endPosY + 0.5, black);
                    RenderUtil.drawRect(posX, posY, posX - 0.5, posY + lineH, color);
                    RenderUtil.drawRect(posX, endPosY, posX - 0.5, endPosY - lineH, color);
                    RenderUtil.drawRect(posX - 0.5, posY, posX + lineW, posY + 0.5, color);
                    RenderUtil.drawRect(endPosX - lineW, posY, endPosX, posY + 0.5, color);
                    RenderUtil.drawRect(endPosX - 0.5, posY, endPosX, posY + lineH, color);
                    RenderUtil.drawRect(endPosX - 0.5, endPosY, endPosX, endPosY - lineH, color);
                    RenderUtil.drawRect(posX, endPosY - 0.5, posX + lineW, endPosY, color);
                    RenderUtil.drawRect(endPosX - lineW, endPosY - 0.5, endPosX - 0.5, endPosY, color);
                }
            }

            if (entity instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) entity;

                if (healthBar.getValue()) {
                    float hp = living.getHealth();
                    float maxHp = living.getMaxHealth();
                    if (hp > maxHp) hp = maxHp;
                    double hpPercentage = hp / maxHp;
                    double height = endPosY - posY;
                    double hpHeight = height * hpPercentage;

                    RenderUtil.drawRect(posX - 3.5, posY - 0.5, posX - 1.5, endPosY + 0.5, background);
                    int healthColor = ColorUtil.getHealthBlend(hp / maxHp).getRGB();

                    if (hpBarMode.getValue() == 0 && height >= 60) {
                        for (int k = 0; k < 10; k++) {
                            double reratio = MathHelper.clamp_double(hp - k * (maxHp / 10.0), 0.0, maxHp / 10.0) / (maxHp / 10.0);
                            double hei = (height / 10.0 - 0.5) * reratio;
                            RenderUtil.drawRect(posX - 3.0, endPosY - (height + 0.5) / 10.0 * k, posX - 2.0, endPosY - (height + 0.5) / 10.0 * k - hei, healthColor);
                        }
                    } else {
                        RenderUtil.drawRect(posX - 3.0, endPosY, posX - 2.0, endPosY - hpHeight, healthColor);
                        float absAmount = living.getAbsorptionAmount();
                        if (absorption.getValue() && absAmount > 0) {
                            RenderUtil.drawRect(posX - 3.0, endPosY, posX - 2.0, endPosY - (height / 6.0) * (absAmount / 2.0), new Color(255, 215, 0, 100).getRGB());
                        }
                    }

                    if (healthNumber.getValue()) {
                        String hpText = hpMode.getValue() == 0 ? dFormat.format(hp) + " §c❤" : (int) (hpPercentage * 100) + "%";
                        drawScaledString(hpText, posX - 4.0 - mc.fontRendererObj.getStringWidth(hpText) * fontScaleValue.getValue(), endPosY - hpHeight - mc.fontRendererObj.FONT_HEIGHT / 2.0f * fontScaleValue.getValue(), fontScaleValue.getValue(), -1);
                    }
                }

                if (armorBar.getValue()) {
                    if (armorBarMode.getValue() == 1) {
                        double constHeight = (endPosY - posY) / 4.0;
                        for (int m = 4; m > 0; m--) {
                            ItemStack armorStack = living.getEquipmentInSlot(m);
                            if (armorStack != null && armorStack.getItem() != null) {
                                double durabilityFactor = 1.0 - ((double) armorStack.getItemDamage() / armorStack.getMaxDamage());
                                double theHeight = constHeight + 0.25;
                                RenderUtil.drawRect(endPosX + 1.5, endPosY + 0.5 - theHeight * m, endPosX + 3.5, endPosY + 0.5 - theHeight * (m - 1), background);
                                RenderUtil.drawRect(endPosX + 2.0, endPosY + 0.5 - theHeight * (m - 1) - 0.25, endPosX + 3.0, endPosY + 0.5 - theHeight * (m - 1) - 0.25 - (constHeight - 0.25) * durabilityFactor, new Color(0, 255, 255).getRGB());
                            }
                        }
                    } else {
                        float armorVal = living.getTotalArmorValue();
                        if (armorVal > 0) {
                            double armorHeight = (endPosY - posY) * (armorVal / 20.0);
                            RenderUtil.drawRect(endPosX + 1.5, posY - 0.5, endPosX + 3.5, endPosY + 0.5, background);
                            RenderUtil.drawRect(endPosX + 2.0, endPosY, endPosX + 3.0, endPosY - armorHeight, new Color(0, 255, 255).getRGB());
                        }
                    }
                }

                if (armorItems.getValue()) {
                    double yDist = (endPosY - posY) / 4.0;
                    for (int j = 4; j > 0; j--) {
                        ItemStack armorStack = living.getEquipmentInSlot(j);
                        if (armorStack != null && armorStack.getItem() != null) {
                            double itemY = posY + yDist * (4 - j) + yDist / 2.0 - 5.0;
                            double itemX = endPosX + (armorBar.getValue() ? 4.0 : 2.0);
                            renderItemStack(armorStack, itemX, itemY);
                            if (armorDur.getValue()) {
                                int dur = armorStack.getMaxDamage() - armorStack.getItemDamage();
                                drawScaledCenteredString(String.valueOf(dur), itemX + 4.5, itemY + 9.0, fontScaleValue.getValue(), -1);
                            }
                        }
                    }
                }

                if (tagsValue.getValue()) {
                    String entName = living.getDisplayName().getFormattedText();
                    double textX = posX + (endPosX - posX) / 2.0;
                    double textY = posY - 1.0 - (mc.fontRendererObj.FONT_HEIGHT * fontScaleValue.getValue());
                    if (tagsBGValue.getValue()) {
                        float textW = mc.fontRendererObj.getStringWidth(entName) * fontScaleValue.getValue();
                        RenderUtil.drawRect(textX - textW / 2f - 2f, textY - 2f, textX + textW / 2f + 2f, textY + mc.fontRendererObj.FONT_HEIGHT * fontScaleValue.getValue(), 0x80000000);
                    }
                    drawScaledCenteredString(entName, textX, textY, fontScaleValue.getValue(), -1);
                }

                if (itemTagsValue.getValue()) {
                    ItemStack held = living.getHeldItem();
                    if (held != null && held.getItem() != null) {
                        String itemName = held.getDisplayName();
                        double textX = posX + (endPosX - posX) / 2.0;
                        double textY = endPosY + 1.0;
                        if (tagsBGValue.getValue()) {
                            float textW = mc.fontRendererObj.getStringWidth(itemName) * fontScaleValue.getValue();
                            RenderUtil.drawRect(textX - textW / 2f - 2f, textY - 2f, textX + textW / 2f + 2f, textY + mc.fontRendererObj.FONT_HEIGHT * fontScaleValue.getValue(), 0x80000000);
                        }
                        drawScaledCenteredString(itemName, textX, textY, fontScaleValue.getValue(), -1);
                    }
                }
            } else if (entity instanceof EntityItem && droppedItems.getValue()) {
                EntityItem item = (EntityItem) entity;
                ItemStack stack = item.getEntityItem();
                if (armorBar.getValue() && stack.isItemStackDamageable()) {
                    double maxD = stack.getMaxDamage();
                    double curD = maxD - stack.getItemDamage();
                    double per = curD / maxD;
                    double h = endPosY - posY;
                    RenderUtil.drawRect(endPosX + 1.5, posY - 0.5, endPosX + 3.5, endPosY + 0.5, background);
                    RenderUtil.drawRect(endPosX + 2.0, endPosY, endPosX + 3.0, endPosY - (h * per), new Color(0, 255, 255).getRGB());
                    if (armorNumber.getValue()) {
                        drawScaledString(String.valueOf((int) curD), endPosX + 4.0, endPosY - (h * per) - (mc.fontRendererObj.FONT_HEIGHT / 2f * fontScaleValue.getValue()), fontScaleValue.getValue(), -1);
                    }
                }
                if (itemTagsValue.getValue()) {
                    String entName = stack.getDisplayName();
                    double textX = posX + (endPosX - posX) / 2.0;
                    double textY = endPosY + 1.0;
                    if (tagsBGValue.getValue()) {
                        float textW = mc.fontRendererObj.getStringWidth(entName) * fontScaleValue.getValue();
                        RenderUtil.drawRect(textX - textW / 2f - 2f, textY - 2f, textX + textW / 2f + 2f, textY + mc.fontRendererObj.FONT_HEIGHT * fontScaleValue.getValue(), 0x80000000);
                    }
                    drawScaledCenteredString(entName, textX, textY, fontScaleValue.getValue(), -1);
                }
            }
        }
        RenderUtil.disableRenderState();
    }

    public boolean isValidEntity(Entity entity) {
        if (entity == null) return false;
        if (entity == mc.thePlayer && !localPlayer.getValue() && mc.gameSettings.thirdPersonView == 0) return false;
        if (entity.isInvisible()) return false;
        if (entity instanceof EntityItem) return droppedItems.getValue();
        return entity instanceof EntityLivingBase && entity != mc.getRenderViewEntity();
    }

    private Color getColor(Entity entity) {
        if (entity instanceof EntityPlayer) {
            if (TeamUtil.isFriend((EntityPlayer) entity)) return Color.BLUE;
            if (colorModeValue.getValue() == 5) {
                return TeamUtil.getTeamColor((EntityPlayer) entity, 1.0f);
            }
        }
        switch (colorModeValue.getValue()) {
            case 0: return new Color(colorRedValue.getValue(), colorGreenValue.getValue(), colorBlueValue.getValue());
            case 1: return ColorUtil.rainbow(2, 0, saturationValue.getValue(), brightnessValue.getValue());
            case 2: return ColorUtil.rainbow(2, 180, saturationValue.getValue(), brightnessValue.getValue());
            case 3: return ColorUtil.fade(new Color(colorRedValue.getValue(), colorGreenValue.getValue(), colorBlueValue.getValue()), 1, 100);
            case 4: return ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis());
            default: return Color.WHITE;
        }
    }

    private void drawScaledString(String text, double x, double y, float scale, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(scale, scale, scale);
        RenderUtil.drawOutlinedString(text, 0, 0);
        GlStateManager.popMatrix();
    }

    private void drawScaledCenteredString(String text, double x, double y, float scale, int color) {
        float width = mc.fontRendererObj.getStringWidth(text);
        drawScaledString(text, x - (width * scale) / 2.0, y, scale, color);
    }

    private void renderItemStack(ItemStack stack, double x, double y) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(0.5, 0.5, 0.5);
        RenderUtil.renderItemInGUI(stack, 0, 0);
        GlStateManager.popMatrix();
    }
}