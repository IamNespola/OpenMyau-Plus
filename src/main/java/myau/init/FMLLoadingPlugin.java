package myau.init;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class FMLLoadingPlugin implements IMixinConfigPlugin {
    private static final List<FMLLoadingPlugin> mixinPlugins = new ArrayList<>();
    private String mixinPackage;
    private List<String> mixins = null;

    public static List<FMLLoadingPlugin> getMixinPlugins() {
        return mixinPlugins;
    }

    public void onLoad(String mixinPackage) {
        this.mixinPackage = mixinPackage;
        mixinPlugins.add(this);
    }

    public URL getBaseUrlForClassUrl(URL classUrl) {
        String string = classUrl.toString();
        if (classUrl.getProtocol().equals("jar")) {
            try {
                return new URL(string.substring(4).split("!")[0]);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        } else if (string.endsWith(".class")) {
            try {
                return new URL(string.replace("\\", "/").replace(this.getClass().getCanonicalName().replace(".", "/") + ".class", ""));
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        } else {
            return classUrl;
        }
    }

    public String getMixinPackage() {
        return this.mixinPackage;
    }

    public String getMixinBaseDir() {
        return this.mixinPackage.replace(".", "/");
    }

    public void tryAddMixinClass(String className) {
        if (!className.endsWith(".class")) {
            return;
        }
        String norm = className.substring(0, className.length() - ".class".length()).replace("\\", "/").replace("/", ".");
        if (norm.startsWith(this.getMixinPackage() + ".") && !norm.endsWith(".")) {
            this.mixins.add(norm.substring(this.getMixinPackage().length() + 1));
        }
    }

    public List<String> getMixins() {
        if (this.mixins != null) {
            return this.mixins;
        } else {
            this.mixins = new ArrayList<>();
            URL classUrl = this.getClass().getProtectionDomain().getCodeSource().getLocation();
            System.out.println("Found classes at " + classUrl);
            Path file;
            try {
                file = Paths.get(this.getBaseUrlForClassUrl(classUrl).toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Base directory found at " + file);
            if (Files.isDirectory(file)) {
                this.walkDir(file);
            } else {
                this.walkJar(file);
            }
            System.out.println("Found mixins: " + this.mixins);
            return this.mixins;
        }
    }

    private void walkDir(Path classRoot) {
        System.out.println("Trying to find mixins from directory");
        try (Stream<Path> classes = Files.walk(classRoot.resolve(this.getMixinBaseDir()))) {
            classes.map((it) -> classRoot.relativize(it).toString()).forEach(this::tryAddMixinClass);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void walkJar(Path file) {
        System.out.println("Trying to find mixins from jar file");
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(file))) {
            ZipEntry next;
            while ((next = zis.getNextEntry()) != null) {
                this.tryAddMixinClass(next.getName());
                zis.closeEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    public String getRefMapperConfig() {
        return null;
    }

    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }
}
