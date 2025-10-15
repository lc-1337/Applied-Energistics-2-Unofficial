package appeng.core.sync.packets;

import static appeng.util.Platform.readStackByte;
import static appeng.util.Platform.writeStackByte;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;

import appeng.api.storage.data.IAEStack;
import appeng.client.StorageName;
import appeng.client.gui.implementations.GuiPatternItemRenamer;
import appeng.client.gui.implementations.GuiPatternValueAmount;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerPatternValueAmount;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketPatternValueSet extends AppEngPacket {

    private final IAEStack<?> aes;
    private final StorageName invName;
    private final int slotIndex;

    public PacketPatternValueSet(final ByteBuf stream) {
        this.aes = readStackByte(stream);
        this.invName = StorageName.values()[stream.readInt()];
        this.slotIndex = stream.readInt();
    }

    public PacketPatternValueSet(IAEStack<?> aes, StorageName invName, int slotIndex) {
        this.aes = aes;
        this.invName = invName;
        this.slotIndex = slotIndex;

        final ByteBuf data = Unpooled.buffer();

        data.writeInt(this.getPacketID());
        writeStackByte(aes, data);
        data.writeInt(invName.ordinal());
        data.writeInt(slotIndex);

        this.configureWrite(data);
    }

    @Override
    public void clientPacketData(INetworkInfo network, AppEngPacket packet, EntityPlayer player) {
        if (player.openContainer instanceof ContainerPatternValueAmount cpva) {
            cpva.setVirtualSlot(invName, slotIndex, aes);
        }
        final GuiScreen gs = Minecraft.getMinecraft().currentScreen;

        if (gs instanceof GuiPatternValueAmount gpva) {
            gpva.update();
        } else if (gs instanceof GuiPatternItemRenamer gpir) {
            gpir.update();
        }
    }

    @Override
    public void serverPacketData(INetworkInfo manager, AppEngPacket packet, EntityPlayer player) {
        if (player.openContainer instanceof AEBaseContainer bc) {
            bc.getPrimaryGui().openOriginalGui(player);
        }
    }
}
