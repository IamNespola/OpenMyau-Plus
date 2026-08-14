package keystrokesmod.script;

import keystrokesmod.Raven;
import keystrokesmod.clickgui.ClickGui;
import keystrokesmod.clickgui.components.impl.CategoryComponent;
import keystrokesmod.module.Module;
import keystrokesmod.utility.NetworkUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;

public class ScriptManager {
    private final Minecraft mc = Minecraft.getMinecraft();

    public final LinkedHashMap<Script, Module> scripts = new LinkedHashMap<>();
    public final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    public boolean deleteTempFiles = true;
    public File directory;

    public final List<String> imports = Arrays.asList(
        Color.class.getName(),
        Collections.class.getName(),
        List.class.getName(),
        ArrayList.class.getName(),
        Arrays.class.getName(),
        Map.class.getName(),
        HashMap.class.getName(),
        HashSet.class.getName(),
        ConcurrentHashMap.class.getName(),
        LinkedHashMap.class.getName(),
        Iterator.class.getName(),
        Comparator.class.getName(),
        AtomicInteger.class.getName(),
        AtomicLong.class.getName(),
        AtomicBoolean.class.getName(),
        Random.class.getName(),
        Matcher.class.getName()
    );

    public final String COMPILED_DIR = Utils.getCompilerDirectory();

    public final String jarPath =
        ((String[]) ScriptManager.class.getProtectionDomain()
            .getCodeSource()
            .getLocation()
            .getPath()
            .split("\\.jar!"))[0]
            .substring(5) + ".jar";

    private final Map<String, String> loadedHashes = new HashMap<>();

    /*
     * Tracks which Raven script modules have already been mirrored into Myau.
     *
     * IdentityHashMap is intentional: Module instances should be treated as
     * distinct objects even if a future Module.equals implementation changes.
     */
    private final Map<Module, myau.module.Module> myauScriptModules =
        new IdentityHashMap<>();

    public ScriptManager() {
        directory = new File(
            mc.mcDataDir + File.separator + "keystrokes",
            "scripts"
        );
    }

    public void onEnable(Script dv) {
        if (dv.event == null) {
            dv.event = new ScriptEvents(getModule(dv));
            MinecraftForge.EVENT_BUS.register(dv.event);
        }

        dv.invoke("onEnable");
    }

    public Module getModule(Script dv) {
        for (Map.Entry<Script, Module> entry : scripts.entrySet()) {
            if (entry.getKey().equals(dv)) {
                return entry.getValue();
            }
        }

        return null;
    }

    /**
     * Call this once Myau has finished creating moduleManager and propertyManager.
     *
     * Example, at the end of Myau's normal initialization:
     *
     * Raven.scriptManager.onMyauInitialized();
     *
     * This is important if ScriptManager loads before Myau has completed startup.
     */
    public void onMyauInitialized() {
        if (!isMyauReady()) {
            System.err.println(
                "[Scripts] Myau initialization notification received, "
                    + "but Myau managers are still null."
            );
            return;
        }

        syncMyauModules();
    }

    public void loadScripts() {
        for (Module module : scripts.values()) {
            module.disable();
        }

        if (deleteTempFiles) {
            deleteTempFiles = false;
            deleteCompiledTempFiles();
        } else {
            unloadChangedOrRemovedScripts();
        }

        final File scriptDirectory = directory;

        if (scriptDirectory.exists() && scriptDirectory.isDirectory()) {
            final File[] scriptFiles = scriptDirectory.listFiles();

            if (scriptFiles != null) {
                for (final File scriptFile : scriptFiles) {
                    if (!scriptFile.isFile()
                        || !scriptFile.getName().endsWith(".java")) {
                        continue;
                    }

                    String fileName = scriptFile.getName();
                    String hash = calculateHash(scriptFile);
                    String cachedHash = loadedHashes.get(fileName);

                    if (cachedHash != null && cachedHash.equals(hash)) {
                        continue;
                    }

                    if (parseFile(scriptFile)) {
                        loadedHashes.put(fileName, hash);
                    } else {
                        loadedHashes.remove(fileName);
                    }
                }
            }
        } else {
            if (scriptDirectory.mkdirs()) {
                System.out.println(
                    "Created script directory: "
                        + scriptDirectory.getAbsolutePath()
                );
            } else {
                System.err.println(
                    "Failed to create script directory: "
                        + scriptDirectory.getAbsolutePath()
                );
            }
        }

        for (Module module : scripts.values()) {
            module.disable();
        }

        /*
         * This is now safe with zero scripts. If Myau has not initialized yet,
         * syncMyauModules simply returns rather than dereferencing null managers.
         */
        syncMyauModules();

        for (CategoryComponent categoryComponent : Raven.clickGui.categories) {
            if (categoryComponent.category != Module.category.profiles) {
                categoryComponent.reloadModules(false);
            }
        }

        ScriptDefaults.reloadModules();

        deleteCompiledTempFiles();
    }

    /**
     * Removes scripts which were changed or deleted since the last reload.
     */
    private void unloadChangedOrRemovedScripts() {
        if (scripts.isEmpty()) {
            loadedHashes.clear();
            myauScriptModules.clear();
            return;
        }

        Iterator<Map.Entry<Script, Module>> iterator =
            scripts.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<Script, Module> entry = iterator.next();

            Script script = entry.getKey();
            Module module = entry.getValue();
            File scriptFile = script.file;

            String fileName = scriptFile.getName();
            String currentHash = calculateHash(scriptFile);
            String cachedHash = loadedHashes.get(fileName);

            boolean fileWasDeleted = !scriptFile.exists();
            boolean fileChanged = cachedHash == null || !cachedHash.equals(currentHash);
            boolean scriptHadError = script.error;

            if (!fileWasDeleted && !fileChanged && !scriptHadError) {
                continue;
            }

            try {
                module.disable();
            } catch (Exception ignored) {
            }

            script.delete();
            unregisterMyauModule(module);

            iterator.remove();
            loadedHashes.remove(fileName);
        }
    }

    private boolean parseFile(final File file) {
        if (file.getName().startsWith("_")
            || !file.getName().endsWith(".java")) {
            return false;
        }

        String scriptName = file.getName().replace(".java", "");

        if (scriptName.isEmpty()) {
            return false;
        }

        StringBuilder scriptContents = new StringBuilder();

        try (BufferedReader bufferedReader = new BufferedReader(
            new FileReader(file)
        )) {
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                scriptContents.append(line).append("\n");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }

        if (scriptContents.length() == 0) {
            return false;
        }

        List<String> topLevelLines =
            Utils.getTopLevelLines(scriptContents.toString());

        for (String line : topLevelLines) {
            if (!line.startsWith("load - \"") || !line.endsWith("\"")) {
                continue;
            }

            String url = line.substring(
                "load - \"".length(),
                line.length() - 1
            );

            String externalContents =
                NetworkUtils.getTextFromURL(url, true, true);

            if (externalContents == null || externalContents.isEmpty()) {
                System.err.println(
                    "[Scripts] Could not load external script content: " + url
                );
                continue;
            }

            int loadIndex = scriptContents.indexOf(line);

            if (loadIndex != -1) {
                scriptContents.replace(
                    loadIndex,
                    loadIndex + line.length(),
                    externalContents
                );
            }
        }

        Script script = new Script(scriptName);
        script.file = file;
        script.setCode(scriptContents.toString());
        script.run();

        if (script.error) {
            System.err.println(
                "[Scripts] Failed to load script: " + file.getName()
            );
            return false;
        }

        Module module = new Module(script);

        scripts.put(script, module);

        ScriptDefaults.reloadModules();
        Raven.scriptManager.invoke("onLoad", module);

        /*
         * If Myau is not ready yet, this does nothing. The module will be
         * mirrored later when onMyauInitialized() or syncMyauModules() runs.
         */
        registerMyauModule(module);

        for (CategoryComponent categoryComponent : ClickGui.categories) {
            if (categoryComponent.category != Module.category.profiles) {
                categoryComponent.reloadModules(false);
            }
        }

        return true;
    }

    /**
     * Myau is usable only after its managers have been initialized.
     *
     * This method prevents an empty scripts directory from causing:
     * myau.Myau.moduleManager.dynamicModules -> NullPointerException
     */
    private boolean isMyauReady() {
        return myau.Myau.moduleManager != null
            && myau.Myau.propertyManager != null;
    }

    /**
     * Mirrors a Raven script Module as a Myau ScriptModule.
     *
     * Registration is skipped during early startup and retried later by
     * syncMyauModules() after Myau finishes initialization.
     */
    private void registerMyauModule(Module module) {
        if (module == null || !isMyauReady()) {
            return;
        }

        if (myauScriptModules.containsKey(module)) {
            return;
        }

        myau.module.modules.ScriptModule scriptModule =
            new myau.module.modules.ScriptModule(module);

        myau.Myau.moduleManager.registerDynamicModule(scriptModule);

        ArrayList<myau.property.Property<?>> properties =
            new ArrayList<>(scriptModule.getScriptProperties());

        for (myau.property.Property<?> property : properties) {
            property.setOwner(scriptModule);
        }

        myau.Myau.propertyManager.properties.put(scriptModule, properties);
        myauScriptModules.put(module, scriptModule);

        resetMyauClickGuis();
    }

    /**
     * Removes the Myau-side wrapper for a Raven script module.
     */
    private void unregisterMyauModule(Module module) {
        if (module == null) {
            return;
        }

        myau.module.Module myauModule = myauScriptModules.remove(module);

        if (!isMyauReady() || myauModule == null) {
            return;
        }

        myau.Myau.moduleManager.unregisterDynamicModule(myauModule.getName());

        resetMyauClickGuis();
    }

    /**
     * Ensures every loaded Raven script has a Myau wrapper, then synchronizes
     * enabled states after Raven disables scripts during a reload.
     */
    private void syncMyauModules() {
        if (!isMyauReady()) {
            return;
        }

        /*
         * Scripts may have loaded before Myau did. Register their wrappers now.
         */
        for (Module scriptModule : scripts.values()) {
            registerMyauModule(scriptModule);
        }

        if (myau.Myau.moduleManager.dynamicModules != null) {
            for (myau.module.Module module
                : myau.Myau.moduleManager.dynamicModules.values()) {

                if (module instanceof myau.module.modules.ScriptModule) {
                    ((myau.module.modules.ScriptModule) module)
                        .syncEnabledState();
                }
            }
        }

        resetMyauClickGuis();
    }


    private void resetMyauClickGuis() {
        if (!isMyauReady()) {
            return;
        }

        try {
            myau.ui.ClickGui.resetInstance();
            myau.ui.impl.clickgui.cheadle.CheadleClickGui.resetInstance();
            myau.ui.impl.clickgui.normal.ClickGuiScreen.resetInstance();
            myau.ui.impl.clickgui.rise.RiseClickGUI.resetInstance();
            myau.ui.impl.clickgui.modern.ModernClickGui.resetInstance();
        } catch (Exception exception) {
            System.err.println(
                "[scriptmanager] could not refresh clickGUIs: "
                    + exception.getMessage()
            );
        }
    }

    public void onDisable(Script script) {
        if (script.event != null) {
            MinecraftForge.EVENT_BUS.unregister(script.event);
            script.event = null;
        }

        script.invoke("onDisable");
    }

    public void invoke(
        String methodName,
        Module module,
        final Object... args
    ) {
        for (Map.Entry<Script, Module> entry : scripts.entrySet()) {
            boolean isEnabled =
                entry.getValue().canBeEnabled()
                    && entry.getValue().isEnabled();

            if ((isEnabled || methodName.equals("onLoad"))
                && entry.getValue().equals(module)) {
                entry.getKey().invoke(methodName, args);
            }
        }
    }

    public int invokeBoolean(
        String methodName,
        Module module,
        final Object... args
    ) {
        for (Map.Entry<Script, Module> entry : scripts.entrySet()) {
            boolean isEnabled =
                entry.getValue().canBeEnabled()
                    && entry.getValue().isEnabled();

            if (!isEnabled || !entry.getValue().equals(module)) {
                continue;
            }

            int result = entry.getKey().getBoolean(methodName, args);

            if (result != -1) {
                return result;
            }
        }

        return -1;
    }

    private void deleteCompiledTempFiles() {
        File tempDirectory = new File(COMPILED_DIR);

        if (!tempDirectory.exists() || !tempDirectory.isDirectory()) {
            return;
        }

        File[] tempFiles = tempDirectory.listFiles();

        if (tempFiles == null) {
            return;
        }

        for (File tempFile : tempFiles) {
            if (!tempFile.delete()) {
                System.err.println(
                    "Failed to delete temp file: "
                        + tempFile.getAbsolutePath()
                );
            }
        }
    }

    private String calculateHash(File file) {
        if (file == null || !file.exists() || !file.isFile()) {
            return "";
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] fileBytes = Files.readAllBytes(
                Paths.get(file.getPath())
            );

            byte[] hashBytes = digest.digest(fileBytes);
            StringBuilder hexString = new StringBuilder();

            for (byte b : hashBytes) {
                hexString.append(String.format("%02x", b));
            }

            return hexString.toString();
        } catch (Exception exception) {
            return "";
        }
    }
}