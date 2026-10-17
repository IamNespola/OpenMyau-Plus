package myau.property.properties;

import com.google.gson.JsonObject;
import org.junit.Test;
import java.awt.Color;
import static org.junit.Assert.*;

public class ColorPropertyTest {
    @Test
    public void opaqueRedKeepsItsRgbWhenFormattedAndSaved() {
        ColorProperty property = new ColorProperty("color", Color.RED.getRGB());
        assertEquals("&cFF&a00&900", property.formatValue());
        JsonObject json = new JsonObject();
        property.write(json);
        assertEquals("FF0000", json.get("color").getAsString());
        ColorProperty restored = new ColorProperty("color", 0);
        assertTrue(restored.read(json));
        assertEquals(0xFF0000, restored.getValue().intValue());
    }

    @Test
    public void legacyArgbConfigUsesTrailingRgbDigits() {
        ColorProperty property = new ColorProperty("color", 0);
        JsonObject json = new JsonObject();
        json.addProperty("color", "FF123456");
        assertTrue(property.read(json));
        assertEquals(0x123456, property.getValue().intValue());
    }

    @Test
    public void shortHexConfigIsNotTruncated() {
        ColorProperty property = new ColorProperty("color", 0);
        JsonObject json = new JsonObject();
        json.addProperty("color", "ABC");
        assertTrue(property.read(json));
        assertEquals(0xABC, property.getValue().intValue());
    }
}
