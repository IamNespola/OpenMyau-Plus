package myau.ui.impl.clickgui.tenacity;

import myau.Myau;
import myau.module.Module;
import myau.module.ModuleManager;
import myau.property.Property;
import myau.property.PropertyManager;
import myau.property.properties.ModeProperty;
import myau.test.MinecraftTestEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.lwjgl.input.Keyboard;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/** Checks real input routing at half scale, without invoking OpenGL drawing. */
public class TenacityClickGuiTest {
    private Screen screen;
    private Module noSlow;
    private ModeProperty mode;

    @Before
    public void setUp() throws Exception {
        MinecraftTestEnvironment.minecraft();
        Myau.moduleManager = mock(ModuleManager.class);
        Myau.propertyManager = new PropertyManager();
        Module eagle = module("Eagle");
        noSlow = module("NoSlow");
        Module sprint = module("Sprint");
        when(Myau.moduleManager.allModules()).thenReturn(Arrays.asList(sprint, noSlow, eagle));
        mode = new ModeProperty("Mode", 0, new String[]{"Vanilla", "Watchdog", "NCP"});
        Myau.propertyManager.properties.put(noSlow, new ArrayList<Property<?>>(Arrays.asList(mode)));
        screen = new Screen();
        set("windowX", 0f);
        set("windowY", 0f);
        set("scale", 0.5f);
        invoke("rebuildModules");
    }

    @Test
    public void scaledLeftClickTogglesTheSortedModule() throws Exception {
        screen.click(103, 66, 0);
        verify(noSlow).toggle();
    }

    @Test
    public void optionsButtonSelectsWithoutToggling() throws Exception {
        screen.click(231, 66, 0);
        assertSame(noSlow, get("selected"));
        verify(noSlow, never()).toggle();
    }

    @Test
    public void settingsPanelCyclesModesInBothDirections() throws Exception {
        screen.click(103, 66, 1);
        screen.click(270, 40, 0);
        assertEquals("Watchdog", mode.getModeString());
        screen.click(270, 40, 1);
        assertEquals("Vanilla", mode.getModeString());
    }

    @Test
    public void middleClickStartsBindingAndEscapeCancelsIt() throws Exception {
        screen.click(103, 66, 2);
        screen.key('k', Keyboard.KEY_K);
        verify(noSlow).setKey(Keyboard.KEY_K);
        screen.click(103, 66, 2);
        screen.key('\0', Keyboard.KEY_ESCAPE);
        verify(noSlow, never()).setKey(0);
        assertNull(get("binding"));
    }

    @Test
    public void deleteClearsAKeybind() throws Exception {
        screen.click(103, 66, 2);
        screen.key('\0', Keyboard.KEY_DELETE);
        verify(noSlow).setKey(0);
    }

    @Test
    public void categoryChangeClearsThePreviousSettings() throws Exception {
        screen.click(103, 66, 1);
        screen.click(35, 52, 0); // Movement
        screen.click(35, 73, 0); // Render (empty in this fixture)
        assertEquals("Render", get("category"));
        assertNull(get("selected"));
        assertTrue(((java.util.List<?>) get("editors")).isEmpty());
    }

    @Test
    public void clippedRowsCannotBeClickedOutsideTheListViewport() throws Exception {
        set("moduleScroll", 100f);
        screen.click(103, 1, 0);
        verify(noSlow, never()).toggle();
    }

    @Test
    public void openingTransformKeepsClicksAlignedWithDisplayedRows() throws Exception {
        screen.width = 640;
        screen.height = 360;
        set("visualScale", 0.96f);
        screen.click(112, 71, 0);
        verify(noSlow).toggle();
    }

    @Test
    public void closingScreenDoesNotAcceptModuleClicks() throws Exception {
        set("closing", true);
        screen.click(103, 66, 0);
        verify(noSlow, never()).toggle();
    }

    @Test
    public void searchFiltersModulesAndSelectsMatchingSettings() throws Exception {
        screen.click(180, 11, 0);
        screen.key('n', Keyboard.KEY_N);
        screen.key('o', Keyboard.KEY_O);
        assertEquals(java.util.Collections.singletonList(noSlow), get("modules"));
        assertSame(noSlow, get("selected"));
        screen.key('\0', Keyboard.KEY_ESCAPE);
        assertEquals(false, get("searching"));
        assertEquals(false, get("closing"));
    }

    private Module module(String name) {
        Module module = mock(Module.class);
        when(module.getName()).thenReturn(name);
        return module;
    }

    private void set(String name, Object value) throws Exception {
        Field field = TenacityClickGui.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(screen, value);
    }

    private Object get(String name) throws Exception {
        Field field = TenacityClickGui.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(screen);
    }

    private void invoke(String name) throws Exception {
        Method method = TenacityClickGui.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(screen);
    }

    private static class Screen extends TenacityClickGui {
        void click(int x, int y, int button) throws Exception { mouseClicked(x, y, button); }
        void key(char character, int code) throws Exception { keyTyped(character, code); }
    }
}
