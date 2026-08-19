package myau.module.modules;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import myau.Myau;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.util.client.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

public class Capes extends Module {
    public static final List<ResourceLocation> LOADED_CAPES = new ArrayList<>();
    public static String[] CAPES_NAME = new String[]{"Myau"};

    public final ModeProperty capeMode = new ModeProperty("Cape", 0, CAPES_NAME);

    private static List<String> getBuiltinCapes() {
        List<String> capes = new ArrayList<>();
        try {
            java.net.URL url = Myau.class.getResource("/assets/myau/capes/");
            if (url != null) {
                if (url.getProtocol().equals("file")) {
                    File dir = new File(url.toURI());
                    if (dir.exists() && dir.listFiles() != null) {
                        for (File f : dir.listFiles()) {
                            if (f.getName().endsWith(".png")) {
                                capes.add(f.getName().replace(".png", ""));
                            }
                        }
                    }
                } else if (url.getProtocol().equals("jar")) {
                    String jarPath = url.getPath().substring(5, url.getPath().indexOf("!"));
                    try (java.util.jar.JarFile jar =
                                 new java.util.jar.JarFile(java.net.URLDecoder.decode(jarPath, "UTF-8"))) {
                        java.util.Enumeration<java.util.jar.JarEntry> entries = jar.entries();
                        while (entries.hasMoreElements()) {
                            String name = entries.nextElement().getName();
                            if (name.startsWith("assets/myau/capes/")
                                    && name.endsWith(".png")) {
                                String capeName = name.substring(name.lastIndexOf("/") + 1).replace(".png", "");
                                capes.add(capeName);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return capes;
    }

    public final BooleanProperty allPlayer = new BooleanProperty("All player", false);
    public final BooleanProperty btnLoadCapes = new BooleanProperty("Load capes", false);
    public final BooleanProperty btnOpenFolder = new BooleanProperty("Open folder", false);

    private static File directory;

    public Capes() {
        super("Capes", false);

        directory =
                new File(Minecraft.getMinecraft().mcDataDir + File.separator + "keystrokes", "customCapes");
        if (!directory.exists()) {
            boolean success = directory.mkdirs();
            if (!success) {
                System.out.println("There was an issue creating customCapes directory.");
            }
        }

        loadCapes();
    }

    @Override
    public void verifyValue(String name) {
        if (name.equals("Load capes") && btnLoadCapes.getValue()) {
            btnLoadCapes.setValue(false);
            loadCapes();
        } else if (name.equals("Open folder") && btnOpenFolder.getValue()) {
            btnOpenFolder.setValue(false);
            try {
                Desktop.getDesktop().open(directory);
            } catch (IOException ex) {
                directory.mkdirs();
                ChatUtil.display("&cError locating folder, recreated.");
            }
        }
    }

    public void loadCapes() {
        final File[] files = directory.listFiles();
        if (files == null) {
            ChatUtil.display("&cFail to load custom capes.");
            return;
        }

        final String[] builtinCapes = getBuiltinCapes().toArray(new String[0]);
        final List<String> capeNames = new ArrayList<>();
        final List<ResourceLocation> capeLocations = new ArrayList<>();

        for (String s : builtinCapes) {
            String name = s.toLowerCase();
            InputStream stream = Myau.class.getResourceAsStream("/assets/myau/capes/" + name + ".png");
            if (stream == null) {
                stream = Myau.class.getResourceAsStream("/assets/myau/capes/" + s + ".png");
            }
            if (stream == null) {
                continue;
            }
            try (InputStream input = stream) {
                BufferedImage bufferedImage = ImageIO.read(input);
                if (bufferedImage != null) {
                    capeNames.add(s);
                    capeLocations.add(Minecraft.getMinecraft().renderEngine
                            .getDynamicTextureLocation(name, new DynamicTexture(bufferedImage)));
                }
            } catch (Exception e) {
                ChatUtil.display("&cFailed to load cape '&r" + s + "&c'");
            }
        }

        for (File file : files) {
            if (!file.exists() || !file.isFile()) continue;
            if (!file.getName().endsWith(".png")) continue;
            String fileName = file.getName().substring(0, file.getName().length() - 4);

            try {
                BufferedImage bufferedImage = ImageIO.read(file);
                if (bufferedImage != null) {
                    capeNames.add(fileName);
                    capeLocations.add(Minecraft.getMinecraft().renderEngine
                            .getDynamicTextureLocation(fileName, new DynamicTexture(bufferedImage)));
                }
            } catch (IOException e) {
                ChatUtil.display("&cFailed to load cape '&r" + fileName + "&c'");
            }
        }

        if (capeLocations.isEmpty()) {
            capeNames.add("Myau");
            capeLocations.add(Minecraft.getMinecraft().renderEngine
                    .getDynamicTextureLocation("myau_cape", new DynamicTexture(createDefaultCape())));
        }

        LOADED_CAPES.clear();
        LOADED_CAPES.addAll(capeLocations);
        CAPES_NAME = capeNames.toArray(new String[0]);
        capeMode.setModes(CAPES_NAME);
        ChatUtil.display("&aLoaded &r" + CAPES_NAME.length + "&a capes.");
    }

    private BufferedImage createDefaultCape() {
        BufferedImage cape = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < cape.getHeight(); y++) {
            for (int x = 0; x < cape.getWidth(); x++) {
                int blue = 110 + (x * 80 / cape.getWidth());
                int green = 55 + (y * 45 / cape.getHeight());
                int red = 20 + ((x + y) * 20 / (cape.getWidth() + cape.getHeight()));
                if ((x + y) % 16 < 3) {
                    red = Math.min(255, red + 35);
                    green = Math.min(255, green + 65);
                    blue = Math.min(255, blue + 75);
                }
                cape.setRGB(x, y, 0xFF000000 | red << 16 | green << 8 | blue);
            }
        }
        return cape;
    }

    public ResourceLocation getCape() {
        int index = capeMode.getValue();
        if (index >= 0 && index < LOADED_CAPES.size()) {
            return LOADED_CAPES.get(index);
        }
        return null;
    }
}
