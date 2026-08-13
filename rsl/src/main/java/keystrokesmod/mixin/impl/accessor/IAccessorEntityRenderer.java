package keystrokesmod.mixin.impl.accessor;

import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@SideOnly(Side.CLIENT)
@Mixin(EntityRenderer.class)
public interface IAccessorEntityRenderer {
    @Invoker("setupCameraTransform")
    void callSetupCameraTransform(float partialTicks, int pass);

    @Invoker("loadShader")
    void callLoadShader(ResourceLocation resourceLocationIn);

    @Accessor("shaderResourceLocations")
    ResourceLocation[] getShaderResourceLocations();

    @Accessor("useShader")
    boolean getUseShader();

    @Accessor("useShader")
    void setUseShader(boolean useShader);

    @Accessor("shaderIndex")
    int getShaderIndex();

    @Accessor("shaderIndex")
    void setShaderIndex(int index);

    @Accessor("thirdPersonDistance")
    void setThirdPersonDistance(float distance);
}