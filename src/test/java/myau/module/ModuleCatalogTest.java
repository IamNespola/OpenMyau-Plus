package myau.module;

import myau.Myau;
import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ModuleCatalogTest {
    private Module module(String name) {
        return new Module(name, false) { };
    }

    @Test
    public void categoriesFollowFunctionRatherThanOldGuiLists() {
        assertEquals("Render", ModuleCatalog.category(module("AntiObfuscate")));
        assertEquals("Render", ModuleCatalog.category(module("Hotbar")));
        assertEquals("Movement", ModuleCatalog.category(module("InvWalk")));
        assertEquals("Movement", ModuleCatalog.category(module("TargetStrafe")));
        assertEquals("Combat", ModuleCatalog.category(module("TimerRange")));
        assertEquals("Network", ModuleCatalog.category(module("ServerLag")));
        assertEquals("Player", ModuleCatalog.category(module("BedNuker")));
    }

    @Test
    public void descriptionsExplainBehaviorAndPreserveExternalDescriptions() {
        assertTrue(module("NoSlow").getDescription().contains("slowdown"));
        assertTrue(module("Capes").getDescription().contains("cape textures"));
        Module external = new Module("CustomScript", false, false, "A custom script.") { };
        assertEquals("A custom script.", external.getDescription());
    }

    @Test
    public void groupsContainEveryMatchingModuleExactlyOnceAndAreSorted() {
        Module timer = module("FakeLag");
        Module lag = module("ServerLag");
        Module hud = module("HUD");
        Myau.moduleManager = mock(ModuleManager.class);
        when(Myau.moduleManager.allModules()).thenReturn(Arrays.asList(timer, hud, lag));
        assertEquals(Arrays.asList(timer, lag), ModuleCatalog.modules("Network"));
        assertEquals(Arrays.asList(hud), ModuleCatalog.modules("Render"));
    }
}
