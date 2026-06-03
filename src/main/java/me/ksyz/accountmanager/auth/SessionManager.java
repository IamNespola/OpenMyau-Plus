package me.ksyz.accountmanager.auth;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/*
 * This file is derived from https://github.com/ksyzov/AccountManager.
 * Originally licensed under the GNU LGPL.
 *
 * This modified version is licensed under the GNU GPL v3.
 */
public class SessionManager {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private static Field field = null;

    private static Field getField() {
        if (field == null) {
            try {
                for (Field f : Minecraft.class.getDeclaredFields()) {
                    if (f.getType().isAssignableFrom(Session.class)) {
                        field = f;
                        field.setAccessible(true);
                        break;
                    }
                }
            } catch (Exception e) {
                field = null;
            }
        }

        return field;
    }

    public static Session get() {
        return mc.getSession();
    }

    /**
     * Builds an offline ("cracked") session for the given username. The UUID is
     * derived the same way the vanilla server does for offline-mode players, so
     * the account behaves consistently across cracked/offline servers. No
     * network request is made.
     */
    public static Session offline(String username) {
        String uuid = UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)
        ).toString().replace("-", "");
        return new Session(username, uuid, "0", Session.Type.LEGACY.toString());
    }

    public static void set(Session session) {
        try {
            getField().set(mc, session);
        } catch (Exception e) {
            //
        }
    }
}
