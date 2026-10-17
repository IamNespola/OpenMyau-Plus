package myau.init;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class FMLLoadingPluginTest {
    @Test
    public void discoversNestedMixinClassesWithoutTreatingDirectoriesAsClasses() {
        FMLLoadingPlugin plugin = new FMLLoadingPlugin();
        plugin.onLoad("myau.mixin");
        List<String> mixins = plugin.getMixins();
        assertTrue(mixins.contains("viaversion.MixinBlockLadder"));
        assertFalse(mixins.contains("viaversion"));
        int count = mixins.size();
        plugin.tryAddMixinClass("myau/mixin/viaversion/");
        plugin.tryAddMixinClass("myau/mixin/notes.txt");
        assertEquals(count, mixins.size());
    }
}
