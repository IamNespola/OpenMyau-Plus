package keystrokesmod.script.classes;

import keystrokesmod.script.ScriptDefaults;
import keystrokesmod.utility.NetworkUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.image.BufferedImage;
import java.util.HashMap;

public class Image {
    private static HashMap<String, BufferedImage> imageCache = new HashMap<>();
    public String url;
    public BufferedImage bufferedImage;
    public int height;
    public int width;
    public int textureId;
    public boolean cached;

    public Image(String url, boolean cached) {
        this.textureId = -1;
        this.url = url;
        this.cached = cached;
        final BufferedImage cachedImage = cached ? imageCache.get(url) : null;
        if (cachedImage == null) {
            ScriptDefaults.client.async(() -> {
                BufferedImage newImage = NetworkUtils.getImageFromURL(url);
                if (newImage != null) {
                    this.bufferedImage = newImage;
                    this.height = newImage.getHeight();
                    this.width = newImage.getWidth();
                    if (cached) {
                        imageCache.put(url, newImage);
                    }
                }
            });
        }
        else {
            this.bufferedImage = cachedImage;
            this.height = cachedImage.getHeight();
            this.width = cachedImage.getWidth();
        }
    }

    public float[] getDimensions() {
        final int scaleFactor = new ScaledResolution(Minecraft.getMinecraft()).getScaleFactor();
        return new float[] { this.width / scaleFactor, this.height / scaleFactor };
    }

    public boolean isLoaded() {
        return this.bufferedImage != null;
    }

    public static void clearCache() {
        imageCache.clear();
    }

    @Override
    public String toString() {
        return "Image(" + this.height + "," + this.width + "," + this.url + ")";
    }
}
