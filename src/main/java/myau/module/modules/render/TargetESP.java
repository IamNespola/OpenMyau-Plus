// not rat bro XD
package myau.module.modules.render;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.Render3DEvent;
import myau.mixin.IAccessorRenderManager;
import myau.module.Category;
import myau.module.Module;
import myau.module.modules.combat.KillAura;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.util.TeamUtil;
import myau.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class TargetESP extends Module {

    private static final Minecraft mc = Minecraft.getMinecraft();

    // ── Color palette ────────────────────────────────────────────────────────
    private static final int[] COLORS = {
            0xFF4FC3F7, // Sky Blue
            0xFF81C784, // Green
            0xFFFF8A65, // Orange
            0xFFBA68C8, // Purple
            0xFFFFD54F, // Yellow
            0xFFFF6B6B, // Red
            0xFF4DB6AC, // Teal
            0xFFFFFFFF, // White
    };
    private static final String[] COLOR_NAMES = {
            "Sky Blue", "Green", "Orange", "Purple", "Yellow", "Red", "Teal", "White"
    };

    // Module properties
    public final ModeProperty style;
    public final ModeProperty hatStyle;
    public final ModeProperty color;
    public final FloatProperty opacity;
    public final FloatProperty size;
    public final BooleanProperty fade;
    public final FloatProperty fadeSpeed;

    public TargetESP() {
        super("TargetESP", Category.RENDER,false);
        this.style     = new ModeProperty("Style",     0, new String[]{"BOX", "HAT"});
        this.hatStyle  = new ModeProperty("HatStyle",  0, new String[]{"FLAT", "PYRAMID", "CHINA", "CROWN"});
        this.color     = new ModeProperty("Color",     0, COLOR_NAMES);
        this.opacity   = new FloatProperty("Opacity",  0.85f, 0.1f, 1.0f);
        this.size      = new FloatProperty("Size",     1.0f, 0.5f, 2.0f);
        this.fade      = new BooleanProperty("Fade",   false);
        this.fadeSpeed = new FloatProperty("FadeSpeed", 1.0f, 0.1f, 5.0f, () -> fade.getValue());
    }

    // Fade wave state
    private long lastFadeMs = -1;
    private float fadePhase = 0f;

    // ── Color resolution ─────────────────────────────────────────────────────
    private Color getBaseColor() {
        int idx = color.getValue();
        if (idx < 0 || idx >= COLORS.length) idx = 0;
        return new Color(COLORS[idx], true);
    }

    @Override
    public String[] getSuffix() {
        int idx = color.getValue();
        if (idx < 0 || idx >= COLOR_NAMES.length) idx = 0;
        return new String[]{COLOR_NAMES[idx]};
    }

    private void tickFade() {
        long now = System.currentTimeMillis();
        if (lastFadeMs >= 0) {
            float dt = (now - lastFadeMs) / 1000f;
            fadePhase += dt * fadeSpeed.getValue() * 2f * (float) Math.PI;
            if (fadePhase > 100000f) fadePhase -= 100000f;
        }
        lastFadeMs = now;
    }

    private Color getFadedColor() {
        Color base = getBaseColor();
        if (!fade.getValue()) return base;

        // Fade between the base color and a darker version of it
        float t = (float) (0.5 + 0.5 * Math.sin(fadePhase));
        int r = (int) (base.getRed()   * (0.3f + 0.7f * t));
        int g = (int) (base.getGreen() * (0.3f + 0.7f * t));
        int b = (int) (base.getBlue()  * (0.3f + 0.7f * t));
        return new Color(
                Math.min(255, Math.max(0, r)),
                Math.min(255, Math.max(0, g)),
                Math.min(255, Math.max(0, b))
        );
    }

    // ── Rendering ─────────────────────────────────────────────────────────────
    @EventTarget
    public void onRender(Render3DEvent event) {
        if (!this.isEnabled()) return;

        KillAura ka = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (ka == null || ka.target == null) return;

        EntityLivingBase target = ka.target.getEntity();
        if (target == null || !TeamUtil.isEntityLoaded(target)) return;

        tickFade();
        Color c = getFadedColor();
        int r = c.getRed();
        int g = c.getGreen();
        int b = c.getBlue();
        float a = opacity.getValue();

        RenderUtil.enableRenderState();

        if (style.getValue() == 0) {
            renderBox(target, r, g, b, a, event.getPartialTicks());
        } else {
            renderHat(target, r, g, b, a, event.getPartialTicks());
        }

        RenderUtil.disableRenderState();
    }

    // ── Box style ─────────────────────────────────────────────────────────────
    private void renderBox(EntityLivingBase entity, int r, int g, int b, float a, float partialTicks) {
        int ai = (int) (a * 200);
        RenderUtil.drawEntityBoundingBox(entity, r, g, b, ai, 2.0F, 0.05 * size.getValue());

        GL11.glColor4f(r / 255f, g / 255f, b / 255f, a * 0.15f);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderUtil.drawEntityBox(entity, r, g, b);
        GL11.glDisable(GL11.GL_BLEND);
    }

    // ── Hat styles ────────────────────────────────────────────────────────────
    private void renderHat(EntityLivingBase entity, int r, int g, int b, float a, float partialTicks) {
        double ex = lerp(entity.posX, entity.lastTickPosX, partialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
        double ey = lerp(entity.posY, entity.lastTickPosY, partialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
        double ez = lerp(entity.posZ, entity.lastTickPosZ, partialTicks)
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();

        double top = entity.getEntityBoundingBox().maxY
                - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY()
                + (lerp(entity.posY, entity.lastTickPosY, partialTicks) - entity.posY);

        double hw = (entity.width / 2.0 + 0.15) * size.getValue();
        float fr = r / 255f, fg = g / 255f, fb = b / 255f;

        GL11.glPushMatrix();
        GL11.glTranslated(ex, top, ez);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glLineWidth(1.5f);

        switch (hatStyle.getValue()) {
            case 0: drawFlatHat(hw, fr, fg, fb, a);    break;
            case 1: drawPyramidHat(hw, fr, fg, fb, a); break;
            case 2: drawChinaHat(hw, fr, fg, fb, a);   break;
            case 3: drawCrownHat(hw, fr, fg, fb, a);   break;
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    // ── FLAT hat (top hat) ────────────────────────────────────────────────────
    private void drawFlatHat(double hw, float r, float g, float b, float a) {
        double gap    = 0.04;
        double hatH   = 0.35;
        double brimX  = 0.12;
        double brimH  = 0.06;
        double tb     = gap + brimH;

        // Brim faces
        GL11.glColor4f(r, g, b, a * 0.85f);
        GL11.glBegin(GL11.GL_QUADS);
        quad(-hw-brimX, gap, -hw-brimX,  hw+brimX, gap, -hw-brimX,  hw+brimX, tb, -hw-brimX, -hw-brimX, tb, -hw-brimX);
        quad( hw+brimX, gap,  hw+brimX, -hw-brimX, gap,  hw+brimX, -hw-brimX, tb,  hw+brimX,  hw+brimX, tb,  hw+brimX);
        quad(-hw-brimX, gap, -hw-brimX, -hw-brimX, gap,  hw+brimX, -hw-brimX, tb,  hw+brimX, -hw-brimX, tb, -hw-brimX);
        quad( hw+brimX, gap,  hw+brimX,  hw+brimX, gap, -hw-brimX,  hw+brimX, tb, -hw-brimX,  hw+brimX, tb,  hw+brimX);
        // Brim top
        quad(-hw-brimX, gap, -hw-brimX,  hw+brimX, gap, -hw-brimX,  hw+brimX, gap,  hw+brimX, -hw-brimX, gap,  hw+brimX);
        GL11.glEnd();

        // Body
        GL11.glColor4f(r, g, b, a * 0.75f);
        GL11.glBegin(GL11.GL_QUADS);
        quad(-hw, tb, -hw,  hw, tb, -hw,  hw, tb+hatH, -hw, -hw, tb+hatH, -hw);
        quad(-hw, tb,  hw,  hw, tb,  hw,  hw, tb+hatH,  hw, -hw, tb+hatH,  hw);
        quad(-hw, tb, -hw, -hw, tb,  hw, -hw, tb+hatH,  hw, -hw, tb+hatH, -hw);
        quad( hw, tb, -hw,  hw, tb,  hw,  hw, tb+hatH,  hw,  hw, tb+hatH, -hw);
        quad(-hw, tb+hatH, -hw,  hw, tb+hatH, -hw,  hw, tb+hatH,  hw, -hw, tb+hatH,  hw);
        GL11.glEnd();

        // Edges
        GL11.glColor4f(r, g, b, 1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3d(-hw, tb+hatH, -hw); GL11.glVertex3d( hw, tb+hatH, -hw);
        GL11.glVertex3d( hw, tb+hatH,  hw); GL11.glVertex3d(-hw, tb+hatH,  hw);
        GL11.glEnd();
    }

    // ── PYRAMID hat ───────────────────────────────────────────────────────────
    private void drawPyramidHat(double hw, float r, float g, float b, float a) {
        double gap = 0.04, h = 0.6;
        GL11.glColor4f(r, g, b, a * 0.75f);
        GL11.glBegin(GL11.GL_TRIANGLES);
        tri(0, gap+h, 0,  -hw, gap, -hw,  hw, gap, -hw);
        tri(0, gap+h, 0,   hw, gap, -hw,  hw, gap,  hw);
        tri(0, gap+h, 0,   hw, gap,  hw, -hw, gap,  hw);
        tri(0, gap+h, 0,  -hw, gap,  hw, -hw, gap, -hw);
        GL11.glEnd();
        GL11.glColor4f(r, g, b, 1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3d(-hw, gap, -hw); GL11.glVertex3d(hw, gap, -hw);
        GL11.glVertex3d( hw, gap,  hw); GL11.glVertex3d(-hw, gap,  hw);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_LINES);
        edge(-hw, gap, -hw, 0, gap+h, 0); edge(hw, gap, -hw, 0, gap+h, 0);
        edge( hw, gap,  hw, 0, gap+h, 0); edge(-hw, gap, hw, 0, gap+h, 0);
        GL11.glEnd();
    }

    // ── CHINA hat (smooth conical straw hat) ──────────────────────────────────
    private void drawChinaHat(double hw, float r, float g, float b, float a) {
        int segments = 24;
        double gap   = 0.04;
        double tip   = 0.22 * size.getValue();
        double brimR = hw * 2.0;
        double brimDrop = -0.08 * size.getValue();
        double brimH = 0.025 * size.getValue();

        // Filled cone body (smooth)
        GL11.glColor4f(r, g, b, a * 0.72f);
        GL11.glBegin(GL11.GL_TRIANGLES);
        for (int i = 0; i < segments; i++) {
            double a1 = 2 * Math.PI * i       / segments;
            double a2 = 2 * Math.PI * (i + 1) / segments;
            GL11.glVertex3d(0, gap + tip, 0);
            GL11.glVertex3d(brimR * Math.cos(a1), gap + brimDrop, brimR * Math.sin(a1));
            GL11.glVertex3d(brimR * Math.cos(a2), gap + brimDrop, brimR * Math.sin(a2));
        }
        GL11.glEnd();

        // Brim ring (flat annulus)
        GL11.glColor4f(r, g, b, a * 0.65f);
        GL11.glBegin(GL11.GL_QUAD_STRIP);
        double innerR = hw * 0.85;
        for (int i = 0; i <= segments; i++) {
            double ang = 2 * Math.PI * i / segments;
            double cx = Math.cos(ang), cz = Math.sin(ang);
            GL11.glVertex3d(innerR * cx, gap + brimDrop + brimH, innerR * cz);
            GL11.glVertex3d(brimR  * cx, gap + brimDrop,          brimR  * cz);
        }
        GL11.glEnd();

        // Smooth outline on brim edge
        GL11.glColor4f(r, g, b, 1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < segments; i++) {
            double ang = 2 * Math.PI * i / segments;
            GL11.glVertex3d(brimR * Math.cos(ang), gap + brimDrop, brimR * Math.sin(ang));
        }
        GL11.glEnd();
    }

    // ── CROWN hat ─────────────────────────────────────────────────────────────
    private void drawCrownHat(double hw, float r, float g, float b, float a) {
        double gap   = 0.04;
        double baseH = 0.12;
        double spikeH = 0.45;
        int spikes = 5;

        GL11.glColor4f(r, g, b, a * 0.8f);
        GL11.glBegin(GL11.GL_QUADS);
        quad(-hw, gap, -hw,  hw, gap, -hw,  hw, gap+baseH, -hw, -hw, gap+baseH, -hw);
        quad(-hw, gap,  hw,  hw, gap,  hw,  hw, gap+baseH,  hw, -hw, gap+baseH,  hw);
        quad(-hw, gap, -hw, -hw, gap,  hw, -hw, gap+baseH,  hw, -hw, gap+baseH, -hw);
        quad( hw, gap, -hw,  hw, gap,  hw,  hw, gap+baseH,  hw,  hw, gap+baseH, -hw);
        GL11.glEnd();

        GL11.glColor4f(r, g, b, a * 0.85f);
        GL11.glBegin(GL11.GL_TRIANGLES);
        for (int i = 0; i < spikes; i++) {
            double t  = (double) i / spikes;
            double tm = (double) i / spikes + 0.5 / spikes;
            double t2 = (double)(i + 1) / spikes;
            double[] p1 = perimPoint(hw, t);
            double[] pm = perimPoint(hw, tm);
            double[] p2 = perimPoint(hw, t2);
            tri(p1[0], gap+baseH, p1[1],  pm[0], gap+baseH+spikeH, pm[1],  p2[0], gap+baseH, p2[1]);
        }
        GL11.glEnd();

        GL11.glColor4f(r, g, b, 1.0f);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3d(-hw, gap, -hw); GL11.glVertex3d(hw, gap, -hw);
        GL11.glVertex3d( hw, gap,  hw); GL11.glVertex3d(-hw, gap, hw);
        GL11.glEnd();
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex3d(-hw, gap+baseH, -hw); GL11.glVertex3d(hw, gap+baseH, -hw);
        GL11.glVertex3d( hw, gap+baseH,  hw); GL11.glVertex3d(-hw, gap+baseH, hw);
        GL11.glEnd();
    }

    // ── GL helpers ────────────────────────────────────────────────────────────
    private static void quad(double x1,double y1,double z1, double x2,double y2,double z2,
                             double x3,double y3,double z3, double x4,double y4,double z4) {
        GL11.glVertex3d(x1,y1,z1); GL11.glVertex3d(x2,y2,z2);
        GL11.glVertex3d(x3,y3,z3); GL11.glVertex3d(x4,y4,z4);
    }
    private static void tri(double x1,double y1,double z1, double x2,double y2,double z2,
                            double x3,double y3,double z3) {
        GL11.glVertex3d(x1,y1,z1); GL11.glVertex3d(x2,y2,z2); GL11.glVertex3d(x3,y3,z3);
    }
    private static void edge(double x1,double y1,double z1, double x2,double y2,double z2) {
        GL11.glVertex3d(x1,y1,z1); GL11.glVertex3d(x2,y2,z2);
    }

    private static double[] perimPoint(double hw, double t) {
        t = t % 1.0;
        double side = t * 4.0;
        int s = (int) side;
        double f = side - s;
        switch (s % 4) {
            case 0: return new double[]{-hw + 2*hw*f, -hw};
            case 1: return new double[]{hw,  -hw + 2*hw*f};
            case 2: return new double[]{hw  - 2*hw*f,  hw};
            case 3: return new double[]{-hw,  hw - 2*hw*f};
        }
        return new double[]{0, 0};
    }

    private double lerp(double a, double b, float t) {
        return b + (a - b) * t;
    }
}