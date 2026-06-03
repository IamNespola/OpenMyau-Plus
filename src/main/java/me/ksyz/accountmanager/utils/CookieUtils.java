package me.ksyz.accountmanager.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.LinkedHashMap;
import java.util.Map;

/*
 * This file is derived from https://github.com/ksyzov/AccountManager.
 * Originally licensed under the GNU LGPL.
 *
 * This modified version is licensed under the GNU GPL v3.
 */
public final class CookieUtils {
    private CookieUtils() {
    }

    /**
     * Parses the contents of an exported cookies file into an ordered
     * {@code name -> value} jar. Three common export shapes are supported:
     * <ul>
     *     <li>Netscape / Mozilla {@code cookies.txt} (tab separated, the format
     *     produced by "Get cookies.txt" style browser extensions);</li>
     *     <li>JSON exports (e.g. EditThisCookie / Cookie-Editor), either an
     *     array of objects or a single object with {@code name}/{@code value};</li>
     *     <li>a raw {@code Cookie:} header string ({@code name=value; ...}).</li>
     * </ul>
     */
    public static Map<String, String> parse(String content) {
        Map<String, String> jar = new LinkedHashMap<>();
        if (content == null) {
            return jar;
        }
        String trimmed = content.trim();
        if (trimmed.isEmpty()) {
            return jar;
        }

        // JSON export
        char first = trimmed.charAt(0);
        if (first == '[' || first == '{') {
            try {
                parseJson(trimmed, jar);
            } catch (Exception ignored) {
                //
            }
            if (!jar.isEmpty()) {
                return jar;
            }
        }

        // Netscape / Mozilla cookies.txt
        boolean any = false;
        for (String raw : content.split("\\r?\\n")) {
            String line = raw.trim();
            if (line.isEmpty()) {
                continue;
            }
            // Some exporters prefix HttpOnly cookies with "#HttpOnly_".
            if (line.startsWith("#HttpOnly_")) {
                line = line.substring("#HttpOnly_".length());
            } else if (line.charAt(0) == '#') {
                continue;
            }

            String[] f = line.split("\t");
            if (f.length < 7) {
                // Fall back to whitespace splitting for space-aligned exports.
                f = line.split("\\s+");
            }
            if (f.length >= 7) {
                String name = f[5].trim();
                StringBuilder value = new StringBuilder(f[6]);
                // A value should never contain tabs, but rejoin defensively.
                for (int i = 7; i < f.length; i++) {
                    value.append('\t').append(f[i]);
                }
                if (!name.isEmpty()) {
                    jar.put(name, value.toString().trim());
                    any = true;
                }
            }
        }
        if (any) {
            return jar;
        }

        // Raw "Cookie:" header fallback
        String header = trimmed;
        if (header.regionMatches(true, 0, "cookie:", 0, 7)) {
            header = header.substring(7).trim();
        }
        for (String part : header.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && !kv[0].trim().isEmpty()) {
                jar.put(kv[0].trim(), kv[1].trim());
            }
        }
        return jar;
    }

    private static void parseJson(String json, Map<String, String> jar) {
        JsonElement root = new JsonParser().parse(json);
        JsonArray array;
        if (root.isJsonArray()) {
            array = root.getAsJsonArray();
        } else {
            array = new JsonArray();
            if (root.isJsonObject()) {
                array.add(root);
            }
        }
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            if (object.has("name") && object.has("value")) {
                String name = object.get("name").getAsString();
                String value = object.get("value").getAsString();
                if (!name.isEmpty()) {
                    jar.put(name, value);
                }
            }
        }
    }
}
