package keystrokesmod.script.classes;

import com.google.common.collect.Iterables;
import com.mojang.authlib.properties.Property;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.scoreboard.ScorePlayerTeam;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;

public class NetworkPlayer {
    private NetworkPlayerInfo playerInfo;
    private static HashMap<String, NetworkPlayer> cache = new HashMap<>();

    public NetworkPlayer(NetworkPlayerInfo playerInfo) {
        this.playerInfo = playerInfo;
    }

    public String getCape() {
        return playerInfo.getLocationCape().getResourcePath();
    }

    public String getDisplayName() {
        return (this.playerInfo.getGameProfile() == Minecraft.getMinecraft().thePlayer.getGameProfile()) ? Minecraft.getMinecraft().thePlayer.getDisplayName().getUnformattedText() : ScorePlayerTeam.formatPlayerName(this.playerInfo.getPlayerTeam(), this.getName());
    }

    public String getName() {
        if (playerInfo == null) {
            return "";
        }
        return playerInfo.getGameProfile().getName();
    }

    public int getPing() {
        if (playerInfo == null) {
            return 0;
        }
        return playerInfo.getResponseTime();
    }

    public String getSkinData() {
        final Property texture = (Property) Iterables.getFirst(playerInfo.getGameProfile().getProperties().get("textures"), (Object)null);
        if (texture == null) {
            return null;
        }
        return new String(Base64.getDecoder().decode(texture.getValue().getBytes(StandardCharsets.UTF_8)));
    }

    public String getUUID() {
        if (playerInfo == null) {
            return "";
        }
        return playerInfo.getGameProfile().getId().toString();
    }

    public static NetworkPlayer convert(NetworkPlayerInfo networkPlayerInfo) {
        if (networkPlayerInfo == null) {
            return null;
        }
        String id = networkPlayerInfo.getGameProfile().getId().toString();
        NetworkPlayer cachedEntity = cache.get(id);

        if (cachedEntity == null) {
            cachedEntity = new NetworkPlayer(networkPlayerInfo);
            cache.put(id, cachedEntity);
        }
        return cachedEntity;
    }

    public static void clearCache() {
        cache.clear();
    }
}
