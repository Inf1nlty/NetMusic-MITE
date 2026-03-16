package com.github.tartaricacid.netmusic.network.message;

import com.github.tartaricacid.netmusic.NetMusic;
import com.github.tartaricacid.netmusic.client.gui.GuiCDBurnerScreen;
import com.github.tartaricacid.netmusic.client.gui.GuiComputerScreen;
import com.github.tartaricacid.netmusic.inventory.CDBurnerMenu;
import com.github.tartaricacid.netmusic.inventory.ComputerMenu;
import com.github.tartaricacid.netmusic.tileentity.TileEntityCDBurner;
import moddedmite.rustedironcore.network.PacketByteBuf;
import net.minecraft.EntityPlayer;
import net.minecraft.Minecraft;
import net.minecraft.ResourceLocation;
import net.minecraft.TileEntity;
import net.minecraft.World;

public class OpenMenuMessage implements Message {
    public static final ResourceLocation ID = new ResourceLocation(NetMusic.MOD_ID, "open_menu");

    private final Type type;
    private final int windowId;
    private final int x;
    private final int y;
    private final int z;

    public OpenMenuMessage(Type type, int windowId, int x, int y, int z) {
        this.type = type == null ? Type.CD_BURNER : type;
        this.windowId = windowId;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public OpenMenuMessage(PacketByteBuf buf) {
        this(Type.fromOrdinal(buf.readInt()), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(this.type.ordinal());
        buf.writeInt(this.windowId);
        buf.writeInt(this.x);
        buf.writeInt(this.y);
        buf.writeInt(this.z);
    }

    @Override
    public ResourceLocation getChannel() {
        return ID;
    }

    @Override
    public void apply(EntityPlayer entityPlayer) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null || entityPlayer != mc.thePlayer) {
            return;
        }

        World world = mc.theWorld;
        if (world == null) {
            return;
        }

        if (this.type == Type.CD_BURNER) {
            TileEntity tileEntity = world.getBlockTileEntity(this.x, this.y, this.z);
            TileEntityCDBurner burner = tileEntity instanceof TileEntityCDBurner
                    ? (TileEntityCDBurner) tileEntity
                    : new TileEntityCDBurner();
            CDBurnerMenu menu = new CDBurnerMenu(mc.thePlayer, burner);
            menu.windowId = this.windowId;
            mc.thePlayer.openContainer = menu;
            mc.displayGuiScreen(new GuiCDBurnerScreen(menu));
            return;
        }

        ComputerMenu menu = new ComputerMenu(mc.thePlayer);
        menu.windowId = this.windowId;
        mc.thePlayer.openContainer = menu;
        mc.displayGuiScreen(new GuiComputerScreen(menu));
    }

    public enum Type {
        CD_BURNER,
        COMPUTER;

        public static Type fromOrdinal(int ordinal) {
            Type[] values = values();
            if (ordinal < 0 || ordinal >= values.length) {
                return CD_BURNER;
            }
            return values[ordinal];
        }
    }
}
