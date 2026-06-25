package myau.enums;

public enum ChatColors {
    BLACK('0', -16777216),
    DARK_BLUE('1', -16777046),
    DARK_GREEN('2', -16733696),
    DARK_AQUA('3', -16733526),
    DARK_RED('4', -5636096),
    DARK_PURPLE('5', -5635926),
    GOLD('6', -22016),
    GRAY('7', -5592406),
    DARK_GRAY('8', -11184811),
    BLUE('9', -11184641),
    GREEN('a', -11141291),
    AQUA('b', -11141121),
    RED('c', -43691),
    LIGHT_PURPLE('d', -43521),
    YELLOW('e', -171),
    WHITE('f', -1),
    MAGIC('k', 0),
    BOLD('l', 0),
    STRIKETHROUGH('m', 0),
    UNDERLINE('n', 0),
    ITALIC('o', 0),
    RESET('r', 0);
    private final String colorCodes;
    private final int rgb;
    public static final char COLOR_CHAR = '§';

    ChatColors(char colorChar, int rgb) {
        this.rgb = rgb;
        this.colorCodes = new String(new char[]{COLOR_CHAR, colorChar});
    }

    @Override
    public String toString() {
        return this.colorCodes;
    }

    public int toAwtColor() {
        return this.rgb;
    }

    public static ChatColors getClosestColor(java.awt.Color target) {
        ChatColors closest = ChatColors.WHITE;
        double minDistance = Double.MAX_VALUE;
        for (ChatColors color : ChatColors.values()) {
            if (color == ChatColors.MAGIC || color == ChatColors.BOLD || color == ChatColors.STRIKETHROUGH ||
                    color == ChatColors.UNDERLINE || color == ChatColors.ITALIC || color == ChatColors.RESET) {
                continue;
            }
            java.awt.Color c = new java.awt.Color(color.toAwtColor());
            double dist = Math.pow(c.getRed() - target.getRed(), 2) +
                    Math.pow(c.getGreen() - target.getGreen(), 2) +
                    Math.pow(c.getBlue() - target.getBlue(), 2);
            if (dist < minDistance) {
                minDistance = dist;
                closest = color;
            }
        }
        return closest;
    }

    public static final String PREFIX_CLEAN = "Miau \u00bb ";

    public static String getDynamicPrefix() {
        return PREFIX_CLEAN;
    }

    public static String formatColor(String string) {
        if (string == null) return null;

        char[] cArray = string.toCharArray();
        for (int i = 0; i < cArray.length - 1; ++i) {
            if (cArray[i] != '&' || "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(cArray[i + 1]) <= -1) continue;
            cArray[i] = COLOR_CHAR;
            cArray[i + 1] = Character.toLowerCase(cArray[i + 1]);
        }
        return new String(cArray);
    }
}