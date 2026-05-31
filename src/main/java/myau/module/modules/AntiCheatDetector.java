package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.network.play.server.S01PacketJoinGame;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AntiCheatDetector extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final int NOTIFY_COLOR = 0x55FFFF;
    private static final String PREFIX = "&7[&bAntiCheatDetector&7] &f";

    public static String detectedACName = "";

    public final BooleanProperty debug = new BooleanProperty("debug", false);
    public final IntProperty sampleSize = new IntProperty("sample-size", 5, 4, 12);
    public final IntProperty timeoutTicks = new IntProperty("timeout-ticks", 40, 20, 120);

    private final List<Integer> actionNumbers = new ArrayList<>();
    private boolean checking;
    private int ticksPassed;
    private String lastDetection = "";

    public AntiCheatDetector() {
        super("AntiCheatDetector", true, true, "Detects common server anticheats from transaction action patterns");
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!isEnabled() || event.getType() != EventType.RECEIVE) return;

        if (event.getPacket() instanceof S01PacketJoinGame) {
            startCheck();
            return;
        }

        if (checking && event.getPacket() instanceof S32PacketConfirmTransaction) {
            handleTransaction(((S32PacketConfirmTransaction) event.getPacket()).getActionNumber());
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (isEnabled() && checking && ++ticksPassed > timeoutTicks.getValue()) {
            detect("None", "Low", false);
        }
    }

    private void startCheck() {
        reset();
        checking = true;
        detectedACName = "";

        if (getServerAddress().contains("hypixel")) {
            detect("Watchdog", "High", false);
        }
    }

    private void handleTransaction(short action) {
        actionNumbers.add((int) action);
        ticksPassed = 0;

        if (debug.getValue()) {
            ChatUtil.sendFormatted(PREFIX + "ID: &e" + action);
        }

        if (actionNumbers.size() >= sampleSize.getValue()) {
            DetectionResult result = analyzeActionNumbers();
            detect(result.name, result.confidence, result.logDebug);
        }
    }

    private DetectionResult analyzeActionNumbers() {
        List<Integer> diffs = getDiffs();
        int first = actionNumbers.get(0);

        if (isOldVulcan()) return DetectionResult.high("Old Vulcan");
        if (isDuplicateThenIncrement()) return DetectionResult.medium("Verus");
        if (isPolarBurst(diffs)) return DetectionResult.medium("Polar");
        if (first < -3000 && actionNumbers.contains(0)) return DetectionResult.medium("Intave");

        if (isConstantStep(diffs)) {
            int step = diffs.get(0);
            if (step == 1) return DetectionResult.high(detectPositiveStep(first));
            if (step == -1) return DetectionResult.high(detectNegativeStep(first));
        }

        return DetectionResult.unknown();
    }

    private boolean isConstantStep(List<Integer> diffs) {
        if (diffs.isEmpty()) return false;
        int first = diffs.get(0);
        for (int diff : diffs) {
            if (diff != first) return false;
        }
        return true;
    }

    private boolean isDuplicateThenIncrement() {
        return actionNumbers.size() >= 4
                && actionNumbers.get(0).equals(actionNumbers.get(1))
                && isIncrementingFrom(2, 1);
    }

    private boolean isPolarBurst(List<Integer> diffs) {
        if (diffs.size() < 3 || diffs.get(0) < 100 || diffs.get(1) != -1) return false;
        for (int i = 2; i < diffs.size(); i++) {
            if (diffs.get(i) != -1) return false;
        }
        return true;
    }

    private boolean isOldVulcan() {
        return actionNumbers.size() >= 4
                && actionNumbers.get(0) == -30767
                && actionNumbers.get(1) == -30766
                && actionNumbers.get(2) == -25767
                && isIncrementingFrom(3, 1);
    }

    private boolean isIncrementingFrom(int start, int step) {
        for (int i = start; i < actionNumbers.size() - 1; i++) {
            if (actionNumbers.get(i + 1) - actionNumbers.get(i) != step) return false;
        }
        return true;
    }

    private String detectPositiveStep(int first) {
        if (inRange(first, -23772, -23762)) return "Vulcan";
        if (inRange(first, 95, 105) || inRange(first, -20005, -19995)) return "Matrix";
        if (inRange(first, -32773, -32762)) return "Grizzly";
        return "Verus";
    }

    private String detectNegativeStep(int first) {
        if (inRange(first, -8287, -8280)) return "Errata";
        if (first < -3000) return "Intave";
        if (inRange(first, -5, 0)) return "Grim";
        if (inRange(first, -3000, -2995)) return "Karhu";
        return "Polar";
    }

    private boolean inRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    private void detect(String name, String confidence, boolean logDebug) {
        detectedACName = name;
        notifyDetection(name, confidence);
        if (logDebug && debug.getValue()) {
            logNumbers();
        }
        reset();
    }

    private void notifyDetection(String name, String confidence) {
        String key = name + ':' + confidence;
        if (key.equals(lastDetection)) return;
        lastDetection = key;

        String message = "Anticheat detected: " + name + " (" + confidence + ")";
        ChatUtil.sendFormatted(PREFIX + message);
        if (Myau.notificationManager != null) {
            Myau.notificationManager.add(message, 3000L, NOTIFY_COLOR);
        }
    }

    private List<Integer> getDiffs() {
        List<Integer> diffs = new ArrayList<>();
        for (int i = 0; i < actionNumbers.size() - 1; i++) {
            diffs.add(actionNumbers.get(i + 1) - actionNumbers.get(i));
        }
        return diffs;
    }

    private void logNumbers() {
        ChatUtil.sendFormatted(PREFIX + "Action Numbers: &e" + join(actionNumbers));
        ChatUtil.sendFormatted(PREFIX + "Differences: &e" + join(getDiffs()));
    }

    private String join(List<Integer> values) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) builder.append(", ");
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private String getServerAddress() {
        ServerData serverData = mc.getCurrentServerData();
        return serverData == null || serverData.serverIP == null ? "" : serverData.serverIP.toLowerCase(Locale.ROOT);
    }

    private void reset() {
        actionNumbers.clear();
        ticksPassed = 0;
        checking = false;
    }

    @Override
    public void onEnabled() {
        startCheck();
    }

    @Override
    public void onDisabled() {
        reset();
    }

    private static class DetectionResult {
        private final String name;
        private final String confidence;
        private final boolean logDebug;

        private DetectionResult(String name, String confidence, boolean logDebug) {
            this.name = name;
            this.confidence = confidence;
            this.logDebug = logDebug;
        }

        private static DetectionResult high(String name) {
            return new DetectionResult(name, "High", false);
        }

        private static DetectionResult medium(String name) {
            return new DetectionResult(name, "Medium", false);
        }

        private static DetectionResult unknown() {
            return new DetectionResult("Unknown", "Low", true);
        }
    }
}
