package keystrokesmod.script;

import keystrokesmod.Raven;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

public class SecureClassLoader extends URLClassLoader {
    private static final List<String> WHITELISTED_PACKAGES = Arrays.asList("sun.reflect", "keystrokesmod", "java.lang", "java.util", "java.awt");

    public SecureClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (!Manager.allowUnsafeScripts.isToggled() && !isClassSafe(name)) {
            throw new ClassNotFoundException("Unsafe class detected: " + name);
        }
        return super.loadClass(name, resolve);
    }

    private boolean isClassSafe(String name) {
        boolean hasAllowedSuffix = name.endsWith("Exception") || name.endsWith("Throwable");

        boolean isAllowedImport = Raven.scriptManager.imports.stream().anyMatch(prefix -> name.toLowerCase().startsWith(prefix));
        boolean isScriptClass = name.startsWith("sc_") && !name.contains(".");

        boolean isWhitelistedPackage = WHITELISTED_PACKAGES.stream().anyMatch(name::startsWith);

        return hasAllowedSuffix || isAllowedImport || isScriptClass || isWhitelistedPackage;
    }
}