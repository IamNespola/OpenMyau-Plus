package myau.ui.impl.clickgui.clean;

public final class CleanTheme {
    public static final int ACCENT = 0xFFE00012;
    public static final int ACCENT_SOFT = 0x66E00012;
    public static final int OVERLAY = 0xF5000000;
    public static final int PANEL = 0xEE101010;
    public static final int PANEL_DARK = 0xF4060606;
    public static final int PANEL_ELEVATED = 0xF2181818;
    public static final int ROW_HOVER = 0xFF202020;
    public static final int BORDER = 0x33FFFFFF;
    public static final int TEXT = 0xFFE8E8E8;
    public static final int TEXT_ACTIVE = 0xFFFFFFFF;
    public static final int MUTED = 0xFF909090;
    public static final int SHADOW = 0x66000000;

    private CleanTheme() {
    }

    public static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }
}
