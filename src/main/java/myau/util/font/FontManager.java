package myau.util.font;

import myau.util.font.impl.FontRenderer;
import myau.util.font.impl.FontUtil;
import myau.util.font.impl.MinecraftFontRenderer;
import net.minecraft.client.gui.ScaledResolution;

import java.util.HashMap;
import java.util.Map;

import static myau.config.Config.mc;

public class FontManager {
    public static FontRenderer
            regular12, regular14, regular16, regular18, regular22,
            icon20,
            productSans12, productSans16, productSans18, productSans20, productSans24, productSans28, productSans32, productSansLight, productSansMedium,
            googleSans18, googleSans20, googleSans24,
            tenacity12, tenacity16, tenacity20, tenacity24, tenacity28, tenacity32, tenacity80,
            tenacityBold12, tenacityBold16, tenacityBold20, tenacityBold24, tenacityBold28, tenacityBold32,
            sfPro12, sfPro16, sfPro20, sfPro24, sfPro28, sfPro32,
            vision12, vision16, vision20, vision24, vision28, vision32,
            nbpInforma12, nbpInforma16, nbpInforma20, nbpInforma24, nbpInforma28, nbpInforma32,
            tahomaBold12, tahomaBold16, tahomaBold20, tahomaBold24, tahomaBold28, tahomaBold32,
            noti12, noti16, noti18, noti20, noti24, noti28, noti32,
            nunito12, nunito16, nunito18, nunito20, nunito24, nunito28, nunito32, nunito48, nunito80,
            nunitoBold12, nunitoBold16, nunitoBold18, nunitoBold20, nunitoBold24, nunitoBold28, nunitoBold32, nunitoBold48, nunitoBold80, harmonyOS_Sans20;

    private static int prevScale;

    static {
        initializeFonts();
    }

    public static void initializeFonts() {
        Map<String, java.awt.Font> locationMap = new HashMap<>();

        ScaledResolution sr = new ScaledResolution(mc);

        int scale = sr.getScaleFactor();

        if (scale != prevScale) {
            prevScale = scale;

            releaseAllFonts();

            regular12 = new FontRenderer(FontUtil.getResource(locationMap, "regular.ttf", 12));
            regular14 = new FontRenderer(FontUtil.getResource(locationMap, "regular.ttf", 14));
            regular16 = new FontRenderer(FontUtil.getResource(locationMap, "regular.ttf", 16));
            regular18 = new FontRenderer(FontUtil.getResource(locationMap, "regular.ttf", 18));
            regular22 = new FontRenderer(FontUtil.getResource(locationMap, "regular.ttf", 22));

            // Icon Font
            icon20 = new FontRenderer(FontUtil.getResource(locationMap, "icon.ttf", 20));

            // Product Sans (Google Style)
            productSans12 = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_regular.ttf", 12));
            productSans16 = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_regular.ttf", 16));
            productSans18 = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_regular.ttf", 18));
            productSans20 = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_regular.ttf", 20));
            productSans24 = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_regular.ttf", 24));
            productSans28 = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_regular.ttf", 28));
            productSans32 = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_regular.ttf", 32));
            productSansLight = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_light.ttf", 22));
            productSansMedium = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_medium.ttf", 22));

            // Google Sans
            googleSans18 = new FontRenderer(FontUtil.getResource(locationMap, "Google-Sans.ttf", 18));
            googleSans20 = new FontRenderer(FontUtil.getResource(locationMap, "Google-Sans.ttf", 20));
            googleSans24 = new FontRenderer(FontUtil.getResource(locationMap, "Google-Sans.ttf", 24));

            // Tenacity Fonts
            tenacity12 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity.ttf", 12));
            tenacity16 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity.ttf", 16));
            tenacity20 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity.ttf", 20));
            tenacity24 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity.ttf", 24));
            tenacity28 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity.ttf", 28));
            tenacity32 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity.ttf", 32));
            tenacity80 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity.ttf", 80));

            // Tenacity Bold Fonts
            tenacityBold12 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity-bold.ttf", 12));
            tenacityBold16 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity-bold.ttf", 16));
            tenacityBold20 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity-bold.ttf", 20));
            tenacityBold24 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity-bold.ttf", 24));
            tenacityBold28 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity-bold.ttf", 28));
            tenacityBold32 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity-bold.ttf", 32));

            // San Francisco Pro
            sfPro12 = new FontRenderer(FontUtil.getResource(locationMap, "San-Francisco-Pro-Fonts.ttf", 12));
            sfPro16 = new FontRenderer(FontUtil.getResource(locationMap, "San-Francisco-Pro-Fonts.ttf", 16));
            sfPro20 = new FontRenderer(FontUtil.getResource(locationMap, "San-Francisco-Pro-Fonts.ttf", 20));
            sfPro24 = new FontRenderer(FontUtil.getResource(locationMap, "San-Francisco-Pro-Fonts.ttf", 24));
            sfPro28 = new FontRenderer(FontUtil.getResource(locationMap, "San-Francisco-Pro-Fonts.ttf", 28));
            sfPro32 = new FontRenderer(FontUtil.getResource(locationMap, "San-Francisco-Pro-Fonts.ttf", 32));

            // Vision Fonts
            vision12 = new FontRenderer(FontUtil.getResource(locationMap, "Vision.otf", 12));
            vision16 = new FontRenderer(FontUtil.getResource(locationMap, "Vision.otf", 16));
            vision20 = new FontRenderer(FontUtil.getResource(locationMap, "Vision.otf", 20));
            vision24 = new FontRenderer(FontUtil.getResource(locationMap, "Vision.otf", 24));
            vision28 = new FontRenderer(FontUtil.getResource(locationMap, "Vision.otf", 28));
            vision32 = new FontRenderer(FontUtil.getResource(locationMap, "Vision.otf", 32));

            // NBP Informa
            nbpInforma12 = new FontRenderer(FontUtil.getResource(locationMap, "nbp-informa-fivesix.ttf", 12));
            nbpInforma16 = new FontRenderer(FontUtil.getResource(locationMap, "nbp-informa-fivesix.ttf", 16));
            nbpInforma20 = new FontRenderer(FontUtil.getResource(locationMap, "nbp-informa-fivesix.ttf", 20));
            nbpInforma24 = new FontRenderer(FontUtil.getResource(locationMap, "nbp-informa-fivesix.ttf", 24));
            nbpInforma28 = new FontRenderer(FontUtil.getResource(locationMap, "nbp-informa-fivesix.ttf", 28));
            nbpInforma32 = new FontRenderer(FontUtil.getResource(locationMap, "nbp-informa-fivesix.ttf", 32));

            // Tahoma Bold
            tahomaBold12 = new FontRenderer(FontUtil.getResource(locationMap, "tahomabold.ttf", 12));
            tahomaBold16 = new FontRenderer(FontUtil.getResource(locationMap, "tahomabold.ttf", 16));
            tahomaBold20 = new FontRenderer(FontUtil.getResource(locationMap, "tahomabold.ttf", 20));
            tahomaBold24 = new FontRenderer(FontUtil.getResource(locationMap, "tahomabold.ttf", 24));
            tahomaBold28 = new FontRenderer(FontUtil.getResource(locationMap, "tahomabold.ttf", 28));
            tahomaBold32 = new FontRenderer(FontUtil.getResource(locationMap, "tahomabold.ttf", 32));

            // Notification Icons
            noti12 = new FontRenderer(FontUtil.getResource(locationMap, "noti.ttf", 12));
            noti16 = new FontRenderer(FontUtil.getResource(locationMap, "noti.ttf", 16));
            noti18 = new FontRenderer(FontUtil.getResource(locationMap, "noti.ttf", 18));
            noti20 = new FontRenderer(FontUtil.getResource(locationMap, "noti.ttf", 20));
            noti24 = new FontRenderer(FontUtil.getResource(locationMap, "noti.ttf", 24));
            noti28 = new FontRenderer(FontUtil.getResource(locationMap, "noti.ttf", 28));
            noti32 = new FontRenderer(FontUtil.getResource(locationMap, "noti.ttf", 32));

            // Nunito
            nunito12 = new FontRenderer(FontUtil.getResource(locationMap, "nunito.ttf", 12));
            nunito16 = new FontRenderer(FontUtil.getResource(locationMap, "nunito.ttf", 16));
            nunito18 = new FontRenderer(FontUtil.getResource(locationMap, "nunito.ttf", 18));
            nunito20 = new FontRenderer(FontUtil.getResource(locationMap, "nunito.ttf", 20));
            nunito24 = new FontRenderer(FontUtil.getResource(locationMap, "nunito.ttf", 24));
            nunito28 = new FontRenderer(FontUtil.getResource(locationMap, "nunito.ttf", 28));
            nunito32 = new FontRenderer(FontUtil.getResource(locationMap, "nunito.ttf", 32));
            nunito48 = new FontRenderer(FontUtil.getResource(locationMap, "nunito.ttf", 48));
            nunito80 = new FontRenderer(FontUtil.getResource(locationMap, "nunito.ttf", 80));

            // Nunito Bold
            nunitoBold12 = new FontRenderer(FontUtil.getResource(locationMap, "Nunito-Bold.ttf", 12));
            nunitoBold16 = new FontRenderer(FontUtil.getResource(locationMap, "Nunito-Bold.ttf", 16));
            nunitoBold18 = new FontRenderer(FontUtil.getResource(locationMap, "Nunito-Bold.ttf", 18));
            nunitoBold20 = new FontRenderer(FontUtil.getResource(locationMap, "Nunito-Bold.ttf", 20));
            nunitoBold24 = new FontRenderer(FontUtil.getResource(locationMap, "Nunito-Bold.ttf", 24));
            nunitoBold28 = new FontRenderer(FontUtil.getResource(locationMap, "Nunito-Bold.ttf", 28));
            nunitoBold32 = new FontRenderer(FontUtil.getResource(locationMap, "Nunito-Bold.ttf", 32));
            nunitoBold48 = new FontRenderer(FontUtil.getResource(locationMap, "Nunito-Bold.ttf", 48));
            nunitoBold80 = new FontRenderer(FontUtil.getResource(locationMap, "Nunito-Bold.ttf", 80));

            harmonyOS_Sans20 = new FontRenderer(FontUtil.getResource(locationMap, "harmonyOS_Sans.ttf", 20));
        }
    }

    public static void releaseAllFonts() {
        if (regular12 != null) {
            regular12.destroy();
            regular12 = null;
        }
        if (regular14 != null) {
            regular14.destroy();
            regular14 = null;
        }
        if (regular16 != null) {
            regular16.destroy();
            regular16 = null;
        }
        if (regular18 != null) {
            regular18.destroy();
            regular18 = null;
        }
        if (regular22 != null) {
            regular22.destroy();
            regular22 = null;
        }
        if (icon20 != null) {
            icon20.destroy();
            icon20 = null;
        }
        if (productSans12 != null) {
            productSans12.destroy();
            productSans12 = null;
        }
        if (productSans16 != null) {
            productSans16.destroy();
            productSans16 = null;
        }
        if (productSans18 != null) {
            productSans18.destroy();
            productSans18 = null;
        }
        if (productSans20 != null) {
            productSans20.destroy();
            productSans20 = null;
        }
        if (productSans24 != null) {
            productSans24.destroy();
            productSans24 = null;
        }
        if (productSans28 != null) {
            productSans28.destroy();
            productSans28 = null;
        }
        if (productSans32 != null) {
            productSans32.destroy();
            productSans32 = null;
        }
        if (productSansLight != null) {
            productSansLight.destroy();
            productSansLight = null;
        }
        if (productSansMedium != null) {
            productSansMedium.destroy();
            productSansMedium = null;
        }
        if (googleSans18 != null) {
            googleSans18.destroy();
            googleSans18 = null;
        }
        if (googleSans20 != null) {
            googleSans20.destroy();
            googleSans20 = null;
        }
        if (googleSans24 != null) {
            googleSans24.destroy();
            googleSans24 = null;
        }
        if (tenacity12 != null) {
            tenacity12.destroy();
            tenacity12 = null;
        }
        if (tenacity16 != null) {
            tenacity16.destroy();
            tenacity16 = null;
        }
        if (tenacity20 != null) {
            tenacity20.destroy();
            tenacity20 = null;
        }
        if (tenacity24 != null) {
            tenacity24.destroy();
            tenacity24 = null;
        }
        if (tenacity28 != null) {
            tenacity28.destroy();
            tenacity28 = null;
        }
        if (tenacity32 != null) {
            tenacity32.destroy();
            tenacity32 = null;
        }
        if (tenacity80 != null) {
            tenacity80.destroy();
            tenacity80 = null;
        }
        if (tenacityBold12 != null) {
            tenacityBold12.destroy();
            tenacityBold12 = null;
        }
        if (tenacityBold16 != null) {
            tenacityBold16.destroy();
            tenacityBold16 = null;
        }
        if (tenacityBold20 != null) {
            tenacityBold20.destroy();
            tenacityBold20 = null;
        }
        if (tenacityBold24 != null) {
            tenacityBold24.destroy();
            tenacityBold24 = null;
        }
        if (tenacityBold28 != null) {
            tenacityBold28.destroy();
            tenacityBold28 = null;
        }
        if (tenacityBold32 != null) {
            tenacityBold32.destroy();
            tenacityBold32 = null;
        }
        if (sfPro12 != null) {
            sfPro12.destroy();
            sfPro12 = null;
        }
        if (sfPro16 != null) {
            sfPro16.destroy();
            sfPro16 = null;
        }
        if (sfPro20 != null) {
            sfPro20.destroy();
            sfPro20 = null;
        }
        if (sfPro24 != null) {
            sfPro24.destroy();
            sfPro24 = null;
        }
        if (sfPro28 != null) {
            sfPro28.destroy();
            sfPro28 = null;
        }
        if (sfPro32 != null) {
            sfPro32.destroy();
            sfPro32 = null;
        }
        if (vision12 != null) {
            vision12.destroy();
            vision12 = null;
        }
        if (vision16 != null) {
            vision16.destroy();
            vision16 = null;
        }
        if (vision20 != null) {
            vision20.destroy();
            vision20 = null;
        }
        if (vision24 != null) {
            vision24.destroy();
            vision24 = null;
        }
        if (vision28 != null) {
            vision28.destroy();
            vision28 = null;
        }
        if (vision32 != null) {
            vision32.destroy();
            vision32 = null;
        }
        if (nbpInforma12 != null) {
            nbpInforma12.destroy();
            nbpInforma12 = null;
        }
        if (nbpInforma16 != null) {
            nbpInforma16.destroy();
            nbpInforma16 = null;
        }
        if (nbpInforma20 != null) {
            nbpInforma20.destroy();
            nbpInforma20 = null;
        }
        if (nbpInforma24 != null) {
            nbpInforma24.destroy();
            nbpInforma24 = null;
        }
        if (nbpInforma28 != null) {
            nbpInforma28.destroy();
            nbpInforma28 = null;
        }
        if (nbpInforma32 != null) {
            nbpInforma32.destroy();
            nbpInforma32 = null;
        }
        if (tahomaBold12 != null) {
            tahomaBold12.destroy();
            tahomaBold12 = null;
        }
        if (tahomaBold16 != null) {
            tahomaBold16.destroy();
            tahomaBold16 = null;
        }
        if (tahomaBold20 != null) {
            tahomaBold20.destroy();
            tahomaBold20 = null;
        }
        if (tahomaBold24 != null) {
            tahomaBold24.destroy();
            tahomaBold24 = null;
        }
        if (tahomaBold28 != null) {
            tahomaBold28.destroy();
            tahomaBold28 = null;
        }
        if (tahomaBold32 != null) {
            tahomaBold32.destroy();
            tahomaBold32 = null;
        }
        if (noti12 != null) {
            noti12.destroy();
            noti12 = null;
        }
        if (noti16 != null) {
            noti16.destroy();
            noti16 = null;
        }
        if (noti18 != null) {
            noti18.destroy();
            noti18 = null;
        }
        if (noti20 != null) {
            noti20.destroy();
            noti20 = null;
        }
        if (noti24 != null) {
            noti24.destroy();
            noti24 = null;
        }
        if (noti28 != null) {
            noti28.destroy();
            noti28 = null;
        }
        if (noti32 != null) {
            noti32.destroy();
            noti32 = null;
        }
        if (nunito12 != null) {
            nunito12.destroy();
            nunito12 = null;
        }
        if (nunito16 != null) {
            nunito16.destroy();
            nunito16 = null;
        }
        if (nunito18 != null) {
            nunito18.destroy();
            nunito18 = null;
        }
        if (nunito20 != null) {
            nunito20.destroy();
            nunito20 = null;
        }
        if (nunito24 != null) {
            nunito24.destroy();
            nunito24 = null;
        }
        if (nunito28 != null) {
            nunito28.destroy();
            nunito28 = null;
        }
        if (nunito32 != null) {
            nunito32.destroy();
            nunito32 = null;
        }
        if (nunito48 != null) {
            nunito48.destroy();
            nunito48 = null;
        }
        if (nunito80 != null) {
            nunito80.destroy();
            nunito80 = null;
        }
        if (nunitoBold12 != null) {
            nunitoBold12.destroy();
            nunitoBold12 = null;
        }
        if (nunitoBold16 != null) {
            nunitoBold16.destroy();
            nunitoBold16 = null;
        }
        if (nunitoBold18 != null) {
            nunitoBold18.destroy();
            nunitoBold18 = null;
        }
        if (nunitoBold20 != null) {
            nunitoBold20.destroy();
            nunitoBold20 = null;
        }
        if (nunitoBold24 != null) {
            nunitoBold24.destroy();
            nunitoBold24 = null;
        }
        if (nunitoBold28 != null) {
            nunitoBold28.destroy();
            nunitoBold28 = null;
        }
        if (nunitoBold32 != null) {
            nunitoBold32.destroy();
            nunitoBold32 = null;
        }
        if (nunitoBold48 != null) {
            nunitoBold48.destroy();
            nunitoBold48 = null;
        }
        if (nunitoBold80 != null) {
            nunitoBold80.destroy();
            nunitoBold80 = null;
        }
    }

    public static float getStringWidth(FontRenderer font, String text) {
        return (float) font.getStringWidth(text);
    }

    public static float getHeight(FontRenderer font) {
        return (float) font.getHeight();
    }

    public static MinecraftFontRenderer getMinecraft() {
        return MinecraftFontRenderer.INSTANCE;
    }

}
