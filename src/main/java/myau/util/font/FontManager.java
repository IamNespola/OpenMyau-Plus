package myau.util.font;

import myau.util.font.impl.FontRenderer;
import myau.util.font.impl.FontUtil;
import myau.util.font.impl.MinecraftFontRenderer;
import net.minecraft.client.gui.ScaledResolution;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static myau.config.Config.mc;

public class FontManager {
    public static FontRenderer
            regular12, regular14, regular16, regular18, regular22,
            icon20,
            productSans12, productSans16, productSans18, productSans20, productSans24, productSans28, productSans32, productSansLight, productSansMedium,
            tenacity12, tenacity16, tenacity20, tenacity24, tenacity28, tenacity32, tenacity80,
            vision12, vision16, vision20, vision24, vision28, vision32,
            nbpInforma12, nbpInforma16, nbpInforma20, nbpInforma24, nbpInforma28, nbpInforma32,
            tahomaBold12, tahomaBold16, tahomaBold20, tahomaBold24, tahomaBold28, tahomaBold32,
            noti12, noti16, noti18, noti20, noti24, noti28, noti32,
            nunitoBold12, nunitoBold16, nunitoBold18, nunitoBold20, nunitoBold24, nunitoBold28, nunitoBold32, nunitoBold48, nunitoBold80, 
            harmonyOS_Sans20;

    private static int prevScale;

    public static void initializeFonts() {
        ScaledResolution sr = new ScaledResolution(mc);
        int scale = sr.getScaleFactor();

        if (scale != prevScale) {
            prevScale = scale;
            releaseAllFonts();

            Map<String, java.awt.Font> locationMap = new HashMap<>();

            regular12 = create(locationMap, "regular.ttf", 12);
            regular14 = create(locationMap, "regular.ttf", 14);
            regular16 = create(locationMap, "regular.ttf", 16);
            regular18 = create(locationMap, "regular.ttf", 18);
            regular22 = create(locationMap, "regular.ttf", 22);

            icon20 = create(locationMap, "icon.ttf", 20);

            productSans12 = create(locationMap, "product_sans_regular.ttf", 12);
            productSans16 = create(locationMap, "product_sans_regular.ttf", 16);
            productSans18 = create(locationMap, "product_sans_regular.ttf", 18);
            productSans20 = create(locationMap, "product_sans_regular.ttf", 20);
            productSans24 = create(locationMap, "product_sans_regular.ttf", 24);
            productSans28 = create(locationMap, "product_sans_regular.ttf", 28);
            productSans32 = create(locationMap, "product_sans_regular.ttf", 32);
            productSansLight = create(locationMap, "product_sans_light.ttf", 22);
            productSansMedium = create(locationMap, "product_sans_medium.ttf", 22);

            tenacity12 = create(locationMap, "tenacity.ttf", 12);
            tenacity16 = create(locationMap, "tenacity.ttf", 16);
            tenacity20 = create(locationMap, "tenacity.ttf", 20);
            tenacity24 = create(locationMap, "tenacity.ttf", 24);
            tenacity28 = create(locationMap, "tenacity.ttf", 28);
            tenacity32 = create(locationMap, "tenacity.ttf", 32);
            tenacity80 = create(locationMap, "tenacity.ttf", 80);

            vision12 = create(locationMap, "Vision.otf", 12);
            vision16 = create(locationMap, "Vision.otf", 16);
            vision20 = create(locationMap, "Vision.otf", 20);
            vision24 = create(locationMap, "Vision.otf", 24);
            vision28 = create(locationMap, "Vision.otf", 28);
            vision32 = create(locationMap, "Vision.otf", 32);

            nbpInforma12 = create(locationMap, "nbp-informa-fivesix.ttf", 12);
            nbpInforma16 = create(locationMap, "nbp-informa-fivesix.ttf", 16);
            nbpInforma20 = create(locationMap, "nbp-informa-fivesix.ttf", 20);
            nbpInforma24 = create(locationMap, "nbp-informa-fivesix.ttf", 24);
            nbpInforma28 = create(locationMap, "nbp-informa-fivesix.ttf", 28);
            nbpInforma32 = create(locationMap, "nbp-informa-fivesix.ttf", 32);

            tahomaBold12 = create(locationMap, "tahomabold.ttf", 12);
            tahomaBold16 = create(locationMap, "tahomabold.ttf", 16);
            tahomaBold20 = create(locationMap, "tahomabold.ttf", 20);
            tahomaBold24 = create(locationMap, "tahomabold.ttf", 24);
            tahomaBold28 = create(locationMap, "tahomabold.ttf", 28);
            tahomaBold32 = create(locationMap, "tahomabold.ttf", 32);

            noti12 = create(locationMap, "noti.ttf", 12);
            noti16 = create(locationMap, "noti.ttf", 16);
            noti18 = create(locationMap, "noti.ttf", 18);
            noti20 = create(locationMap, "noti.ttf", 20);
            noti24 = create(locationMap, "noti.ttf", 24);
            noti28 = create(locationMap, "noti.ttf", 28);
            noti32 = create(locationMap, "noti.ttf", 32);

            nunitoBold12 = create(locationMap, "Nunito-Bold.ttf", 12);
            nunitoBold16 = create(locationMap, "Nunito-Bold.ttf", 16);
            nunitoBold18 = create(locationMap, "Nunito-Bold.ttf", 18);
            nunitoBold20 = create(locationMap, "Nunito-Bold.ttf", 20);
            nunitoBold24 = create(locationMap, "Nunito-Bold.ttf", 24);
            nunitoBold28 = create(locationMap, "Nunito-Bold.ttf", 28);
            nunitoBold32 = create(locationMap, "Nunito-Bold.ttf", 32);
            nunitoBold48 = create(locationMap, "Nunito-Bold.ttf", 48);
            nunitoBold80 = create(locationMap, "Nunito-Bold.ttf", 80);

            harmonyOS_Sans20 = create(locationMap, "harmonyOS_Sans.ttf", 20);
        }
    }

    private static FontRenderer create(Map<String, java.awt.Font> map, String name, int size) {
        return new FontRenderer(FontUtil.getResource(map, name, size));
    }

    public static void releaseAllFonts() {
        for (Field field : FontManager.class.getDeclaredFields()) {
            try {
                if (field.getType() == FontRenderer.class) {
                    FontRenderer font = (FontRenderer) field.get(null);
                    if (font != null) {
                        font.destroy();
                        field.set(null, null);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static float getStringWidth(FontRenderer font, String text) {
        return font == null ? 0 : (float) font.getStringWidth(text);
    }

    public static float getHeight(FontRenderer font) {
        return font == null ? 0 : (float) font.getHeight();
    }

    public static MinecraftFontRenderer getMinecraft() {
        return MinecraftFontRenderer.INSTANCE;
    }
}