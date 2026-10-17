package myau.config;

import com.google.gson.JsonObject;
import myau.test.MinecraftTestEnvironment;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static org.junit.Assert.*;

public class ConfigTest {
    @Rule public TemporaryFolder folder = new TemporaryFolder();

    @BeforeClass
    public static void initializeMinecraft() throws Exception {
        MinecraftTestEnvironment.minecraft();
    }

    @Test
    public void replacesExistingConfigAndRemovesTemporaryFile() throws Exception {
        File target = folder.newFile("config.json");
        Files.write(target.toPath(), "old config".getBytes(StandardCharsets.UTF_8));
        JsonObject json = new JsonObject();
        json.addProperty("color", "FF0000");
        Config.writeJson(target, json);
        assertEquals(json, new com.google.gson.JsonParser().parse(
                new String(Files.readAllBytes(target.toPath()), StandardCharsets.UTF_8)));
        assertArrayEquals(new String[]{"config.json"}, folder.getRoot().list());
    }

    @Test
    public void failedReplacementPreservesDestinationAndCleansTemporaryFile() throws Exception {
        File target = folder.newFolder("config.json");
        File existing = new File(target, "existing");
        Files.write(existing.toPath(), new byte[]{42});
        try {
            Config.writeJson(target, new JsonObject());
            fail("Replacing a nonempty directory should fail");
        } catch (IOException expected) {
            assertArrayEquals(new byte[]{42}, Files.readAllBytes(existing.toPath()));
            assertArrayEquals(new String[]{"config.json"}, folder.getRoot().list());
        }
    }
}
