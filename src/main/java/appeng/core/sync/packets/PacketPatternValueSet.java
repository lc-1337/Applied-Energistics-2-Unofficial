package appeng.core.sync.packets;

import static appeng.util.Platform.readStackByte;
import static appeng.util.Platform.writeStackByte;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import appeng.api.networking.IGridHost;
import appeng.api.storage.data.IAEStack;
import appeng.client.StorageName;
import appeng.client.gui.implementations.GuiPatternValueAmount;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternValueAmount;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.INetworkInfo;
import appeng.util.Platform;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketPatternValueSet extends AppEngPacket {

    private final GuiBridge originGui;
    private final IAEStack<?> aes;
    private final StorageName invName;
    private final int slotIndex;

    public PacketPatternValueSet(final ByteBuf stream) {
        this.originGui = GuiBridge.values()[stream.readInt()];
        this.aes = readStackByte(stream);
        this.invName = StorageName.values()[stream.readInt()];
        this.slotIndex = stream.readInt();
    }

    public PacketPatternValueSet(GuiBridge originalGui, IAEStack<?> aes, StorageName invName, int slotIndex) {
        this.originGui = originalGui;
        this.aes = aes;
        this.invName = invName;
        this.slotIndex = slotIndex;

        final ByteBuf data = Unpooled.buffer();

        data.writeInt(this.getPacketID());
        data.writeInt(originalGui.ordinal());
        writeStackByte(aes, data);
        data.writeInt(invName.ordinal());
        data.writeInt(slotIndex);

        this.configureWrite(data);
    }

    @Override
    public void clientPacketData(INetworkInfo network, AppEngPacket packet, EntityPlayer player) {
        if (player.openContainer instanceof ContainerPatternValueAmount cpva) {
            cpva.setStack(aes);
            cpva.setSlotsIndex(slotIndex);
            cpva.setInvName(invName);
            cpva.setOriginalGui(originGui);
        }
        final GuiScreen gs = Minecraft.getMinecraft().currentScreen;

        if (gs instanceof GuiPatternValueAmount gpva) {
            gpva.update();
        }
    }

    @Override
    public void serverPacketData(INetworkInfo manager, AppEngPacket packet, EntityPlayer player) {
        if (player.openContainer instanceof ContainerPatternValueAmount cpva) {
            final Object target = cpva.getTarget();
            if (target instanceof IGridHost) {
                final ContainerOpenContext context = cpva.getOpenContext();
                if (context != null) {
                    final TileEntity te = context.getTile();
                    Platform.openGUI(player, te, context.getSide(), originGui);
                    if (player.openContainer instanceof ContainerPatternTerm cpt) {
                        cpt.setPatternSlot(invName, slotIndex, aes);
                    }
                }
            } else {
                Platform.openGUI(player, null, null, originGui);
                if (player.openContainer instanceof ContainerPatternTerm cpt) {
                    cpt.setPatternSlot(invName, slotIndex, aes);
                }
            }
        }
    }
}
