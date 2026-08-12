package myau.font;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class FontProcess {
    private static final Map<String, CFontRenderer> fontRegistry = new HashMap<>();
    private static final Map<String, CFontRenderer> scaledFontCache = new HashMap<>();
    private static String currentFont = "sans";

    public final static String
            BUG = "a",
            LIST = "b",
            BOMB = "c",
            EYE = "d",
            PERSON = "e",
            WHEELCHAIR = "f",
            SCRIPT = "g",
            SKIP_LEFT = "h",
            PAUSE = "i",
            PLAY = "j",
            SKIP_RIGHT = "k",
            SHUFFLE = "l",
            INFO = "m",
            SETTINGS = "n",
            CHECKMARK = "o",
            XMARK = "p",
            TRASH = "q",
            WARNING = "r",
            FOLDER = "s",
            LOAD = "t",
            SAVE = "u",
            UPVOTE_OUTLINE = "v",
            UPVOTE = "w",
            DOWNVOTE_OUTLINE = "x",
            DOWNVOTE = "y",
            DROPDOWN_ARROW = "z",
            PIN = "s",
            EDIT = "A",
            SEARCH = "B",
            UPLOAD = "C",
            REFRESH = "D",
            ADD_FILE = "E",
            STAR_OUTLINE = "F",
            STAR = "G";

    static {
        registerFont("client", createFont("client", 18));
        registerFont("bold", createFont("client-bold", 18));
        registerFont("noto", createFont("noto", 18));
        registerFont("arial", createFont("arial", 18));
        registerFont("apple", createFont("apple", 18));
        registerFont("sans", createFont("sans", 18));
        registerFont("nunito", createFont("nunito", 18));
        registerFont("tenacity", createFont("tenacity", 20));
        registerFont("tenacity-bold", createFont("tenacity-bold", 20));
        registerFont("sf-pro", createFont("San-Francisco-Pro-Fonts", 18));
        registerFont("google-sans", createFont("product_sans_medium", 19));
        registerFont("icon", createFont("icon", 40));
    }

    public static void registerFont(String name, CFontRenderer font) {
        fontRegistry.put(name, font);
    }

    public static CFontRenderer getFont(String name) {
        return fontRegistry.get(name);
    }

    public static void setCurrentFont(String name) {
        if (fontRegistry.containsKey(name)) {
            currentFont = name;
        }
    }

    public static CFontRenderer getCurrentFont() {
        return getFont(currentFont);
    }

    public static void swapFonts(String name1, String name2) {
        CFontRenderer font1 = getFont(name1);
        CFontRenderer font2 = getFont(name2);

        if (font1 != null && font2 != null) {
            fontRegistry.put(name1, font2);
            fontRegistry.put(name2, font1);

            if (currentFont.equals(name1)) currentFont = name2;
            else if (currentFont.equals(name2)) currentFont = name1;
        }
    }

    public static void aliasFont(String aliasName, String sourceName) {
        CFontRenderer source = getFont(sourceName);
        if (source != null) {
            registerFont(aliasName, source);
        }
    }

    public static void rebindFont(String name, CFontRenderer newFont) {
        if (fontRegistry.containsKey(name)) {
            fontRegistry.put(name, newFont);
        }
    }

    public static CFontRenderer getScaledFont(String name, float scale) {
        String cacheKey = name + "|" + scale;
        if (scaledFontCache.containsKey(cacheKey)) {
            return scaledFontCache.get(cacheKey);
        }

        CFontRenderer original = getFont(name);
        if (original == null) return null;

        String fontName = original.getNameFontTTF();
        float originalSize = original.getFont().getSize();
        float newSize = originalSize * scale;

        CFontRenderer scaledFont = new CFontRenderer(fontName, newSize, Font.PLAIN, true, false);
        scaledFontCache.put(cacheKey, scaledFont);

        return scaledFont;
    }

    public static void clearScaledFontCache() {
        scaledFontCache.clear();
    }

    private static CFontRenderer createFont(String name, int size) {
        return new CFontRenderer(name, size, Font.PLAIN, true, false);
    }

}
