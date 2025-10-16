package appeng.core.sync.packets;

import static appeng.util.Platform.readStackByte;
import static appeng.util.Platform.writeStackByte;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;

import appeng.api.storage.data.IAEStack;
import appeng.client.StorageName;
import appeng.container.AEBaseContainer;
import appeng.container.IContainerSubGui;
import appeng.container.PrimaryGui;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.helpers.IVirtualMESlotHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketPatternTerminalSlotUpdate extends AppEngPacket {

    public final IAEStack<?> slotItem;

    public final int slotId;

    public final StorageName invName;

    // automatic.
    public PacketPatternTerminalSlotUpdate(final ByteBuf stream) throws IOException {
        this.invName = StorageName.values()[stream.readInt()];

        this.slotId = stream.readInt();

        this.slotItem = this.readItem(stream);

    }

    // api
    public PacketPatternTerminalSlotUpdate(final StorageName invName, final int slotId, final IAEStack<?> slotItem)
            throws IOException {
        this.invName = invName;

        this.slotItem = slotItem;

        this.slotId = slotId;

        final ByteBuf data = Unpooled.buffer();

        data.writeInt(this.getPacketID());

        data.writeInt(invName.ordinal());

        data.writeInt(slotId);

        this.writeItem(slotItem, data);

        this.configureWrite(data);
    }

    private IAEStack<?> readItem(final ByteBuf stream) throws IOException {
        final boolean hasItem = stream.readBoolean();

        if (hasItem) {
            return readStackByte(stream);
        }
        return null;
    }

    private void writeItem(final IAEStack<?> slotItem, final ByteBuf data) throws IOException {
        if (slotItem == null) {
            data.writeBoolean(false);
        } else {
            data.writeBoolean(true);
            writeStackByte(slotItem, data);
        }
    }

    @Override
    public void clientPacketData(final INetworkInfo network, final AppEngPacket packet, final EntityPlayer player) {
        final GuiScreen gs = Minecraft.getMinecraft().currentScreen;

        if (gs instanceof IVirtualMESlotHandler vsh) {
            vsh.setVirtualSlot(this.invName, this.slotId, this.slotItem);
        }
    }

    @Override
    public void serverPacketData(INetworkInfo manager, AppEngPacket packet, EntityPlayer player) {
        final Container container = player.openContainer;

        if (container instanceof AEBaseContainer bc) {
            final PrimaryGui pg = bc.getPrimaryGui();

            if (container instanceof IVirtualMESlotHandler vsh) {
                vsh.setVirtualSlot(invName, slotId, slotItem);
            }

            if (player.openContainer instanceof IContainerSubGui sg) {
                sg.setPrimaryGui(pg);
            }
        }
    }
}
