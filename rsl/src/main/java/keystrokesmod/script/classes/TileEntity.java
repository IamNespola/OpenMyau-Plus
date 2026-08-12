package keystrokesmod.script.classes;

import net.minecraft.tileentity.TileEntitySkull;

public class TileEntity {
    private net.minecraft.tileentity.TileEntity tileEntity;
    private Vec3 position;

    public String type;
    public String name;

    public TileEntity(net.minecraft.tileentity.TileEntity tileEntity) {
        this.tileEntity = tileEntity;
        this.position = new Vec3(tileEntity.getPos().getX(), tileEntity.getPos().getY(), tileEntity.getPos().getZ());
        this.type = tileEntity.getBlockType().getClass().getSimpleName();
        this.name = tileEntity.getBlockType().getRegistryName().replace("minecraft:", "");
    }

    public Vec3 getPosition() {
        return position;
    }

    public Object[] getSkullData() {
        if (this.tileEntity instanceof TileEntitySkull) {
            final TileEntitySkull skull = (TileEntitySkull)this.tileEntity;
            final Object[] skullData = { skull.getSkullType(), skull.getSkullRotation(), null, null, null };
            if (skull.getPlayerProfile() == null) {
                skullData[2] = (skullData[3] = null);
            }
            else {
                skullData[2] = skull.getPlayerProfile().getName();
                skullData[3] = skull.getPlayerProfile().getId();
            }
            return skullData;
        }
        return null;
    }
}
