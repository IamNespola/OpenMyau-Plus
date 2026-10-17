package myau.test;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Bootstrap;
import net.minecraft.item.Item;

import java.lang.reflect.Field;

import static org.mockito.Mockito.mock;

/** Shared connection fixture; no display, launcher, network, or statistics loader. */
public final class MinecraftTestEnvironment {
    private static Minecraft minecraft;

    public static Minecraft minecraft() throws Exception {
        if (minecraft == null) {
            Field registered = Bootstrap.class.getDeclaredField("alreadyRegistered");
            registered.setAccessible(true);
            registered.setBoolean(null, true);
            Block.registerBlocks();
            Item.registerItems();
            minecraft = mock(Minecraft.class);
            Field instance = Minecraft.class.getDeclaredField("theMinecraft");
            instance.setAccessible(true);
            instance.set(null, minecraft);
        }
        return minecraft;
    }

    private MinecraftTestEnvironment() { }
}
