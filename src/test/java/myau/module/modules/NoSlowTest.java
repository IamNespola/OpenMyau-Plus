package myau.module.modules;

import myau.event.types.EventType;
import myau.events.LoadWorldEvent;
import myau.events.UpdateEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.init.Blocks;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemFood;
import myau.test.MinecraftTestEnvironment;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovementInput;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Exercises the real NoSlow implementation with a simulated Minecraft connection. */
public class NoSlowTest {
    private static Minecraft minecraft;
    private NoSlow noSlow;

    @BeforeClass
    public static void initializeMinecraft() throws Exception {
        minecraft = MinecraftTestEnvironment.minecraft();
    }

    @Before
    public void setUp() {
        minecraft.thePlayer = mock(EntityPlayerSP.class);
        minecraft.theWorld = mock(WorldClient.class);
        minecraft.thePlayer.movementInput = new MovementInput();
        minecraft.thePlayer.inventory = new InventoryPlayer(minecraft.thePlayer);
        when(minecraft.theWorld.getBlockState(any(BlockPos.class))).thenReturn(Blocks.stone.getDefaultState());
        noSlow = new NoSlow();
        noSlow.foodMode.parseString("Watchdog");
        noSlow.onEnabled();
    }

    @Test
    public void watchdogDoesNotAmplifyAlreadyUnslowedInput() throws Exception {
        ItemStack food = new ItemStack(new ItemFood(4, 0.3f, false));
        when(minecraft.thePlayer.getHeldItem()).thenReturn(food);
        when(minecraft.thePlayer.isUsingItem()).thenReturn(true);
        minecraft.thePlayer.movementInput.moveForward = 1f;
        minecraft.thePlayer.movementInput.moveStrafe = -0.5f;
        updateWatchdog();
        assertEquals(1f, minecraft.thePlayer.movementInput.moveForward, 0f);
        assertEquals(-0.5f, minecraft.thePlayer.movementInput.moveStrafe, 0f);
    }

    @Test
    public void inactiveItemUseResetsAirborneTracking() throws Exception {
        set("wdOffGroundTicks", 7);
        when(minecraft.thePlayer.isUsingItem()).thenReturn(false);
        updateWatchdog();
        assertEquals(0, get("wdOffGroundTicks"));
    }

    @Test
    public void worldChangesClearUseItemAndSlotStateWithoutSendingPackets() throws Exception {
        set("wdDisable", true);
        set("wdOffGroundTicks", 9);
        set("opalBlocking", true);
        set("opalSlotChangeTick", 120);
        set("opalNextCycleTick", 123);
        set("savedSlot", true);
        noSlow.onWorldLoad(new LoadWorldEvent());
        assertEquals(false, get("wdDisable"));
        assertEquals(0, get("wdOffGroundTicks"));
        assertEquals(false, get("opalBlocking"));
        assertEquals(-1, get("opalSlotChangeTick"));
        assertEquals(-1, get("opalNextCycleTick"));
        assertEquals(false, get("savedSlot"));
        verifyNoInteractions(minecraft.thePlayer);
    }

    @Test
    public void updateWithoutAWorldClearsStateAndDoesNotDereferencePlayer() throws Exception {
        set("wdOffGroundTicks", 5);
        minecraft.thePlayer = null;
        minecraft.theWorld = null;
        noSlow.onUpdate(new UpdateEvent(EventType.PRE, 0, 0, 0, 0));
        assertEquals(0, get("wdOffGroundTicks"));
    }

    @Test
    public void disabledWatchdogStateKeepsVanillaSlowdown() throws Exception {
        when(minecraft.thePlayer.getHeldItem()).thenReturn(new ItemStack(new ItemFood(4, 0.3f, false)));
        noSlow.setEnabled(true);
        set("wdDisable", true);
        assertFalse(noSlow.shouldCancelMiauSlowdown());
        set("wdDisable", false);
        assertTrue(noSlow.shouldCancelMiauSlowdown());
    }

    private void updateWatchdog() throws Exception {
        Method update = NoSlow.class.getDeclaredMethod("updateMiauWatchdog", UpdateEvent.class);
        update.setAccessible(true);
        update.invoke(noSlow, new UpdateEvent(EventType.PRE, 0, 0, 0, 0));
    }

    private void set(String name, Object value) throws Exception {
        Field field = NoSlow.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(noSlow, value);
    }

    private Object get(String name) throws Exception {
        Field field = NoSlow.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(noSlow);
    }
}
