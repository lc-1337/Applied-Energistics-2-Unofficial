package appeng.core.sync.packets;

import static appeng.util.Platform.readStackByte;
import static appeng.util.Platform.writeStackByte;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

import appeng.api.storage.data.IAEStack;
import appeng.client.StorageName;
import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.container.ContainerOpenContext;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.container.implementations.ContainerPatternValueAmount;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.INetworkInfo;
import appeng.helpers.PatternTerminalAction;
import appeng.util.Platform;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketPatternTerminalSlotUpdate extends AppEngPacket {

    public final IAEStack<?> slotItem;

    public final int slotId;

    public final StorageName invName;

    public final PatternTerminalAction action;

    // automatic.
    public PacketPatternTerminalSlotUpdate(final ByteBuf stream) throws IOException {
        this.invName = StorageName.values()[stream.readInt()];

        this.slotId = stream.readInt();

        this.slotItem = this.readItem(stream);

        this.action = PatternTerminalAction.values()[stream.readInt()];
    }

    // api
    public PacketPatternTerminalSlotUpdate(final StorageName invName, final int slotId, final IAEStack<?> slotItem,
            final PatternTerminalAction action) throws IOException {
        this.invName = invName;

        this.slotItem = slotItem;

        this.slotId = slotId;

        this.action = action;

        final ByteBuf data = Unpooled.buffer();

        data.writeInt(this.getPacketID());

        data.writeInt(invName.ordinal());

        data.writeInt(slotId);

        this.writeItem(slotItem, data);

        data.writeInt(action.ordinal());

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

        if (gs instanceof GuiPatternTerm gpt) {
            gpt.setPatternSlot(this.invName, this.slotId, this.slotItem);
        }
    }

    @Override
    public void serverPacketData(INetworkInfo manager, AppEngPacket packet, EntityPlayer player) {
        final Container container = player.openContainer;

        if (container instanceof ContainerPatternTerm cpt) {
            GuiBridge originalGui = container instanceof ContainerPatternTermEx ? GuiBridge.GUI_PATTERN_TERMINAL_EX
                    : GuiBridge.GUI_PATTERN_TERMINAL;
            if (this.action == PatternTerminalAction.SET_PATTERN_VALUE) {
                final ContainerOpenContext context = cpt.getOpenContext();
                if (context != null) {
                    final TileEntity te = context.getTile();
                    Platform.openGUI(player, te, cpt.getOpenContext().getSide(), GuiBridge.GUI_PATTERN_VALUE_AMOUNT);
                    if (player.openContainer instanceof ContainerPatternValueAmount cpv) {
                        if (slotItem != null) {
                            cpv.setOriginalGui(originalGui);
                            cpv.setStack(slotItem);
                            cpv.setInvName(invName);
                            cpv.setSlotsIndex(slotId);
                            cpv.update();
                        }
                    }
                }
            }
        } /*
           * if (this.action == InventoryAction.SET_PATTERN_VALUE) { final ContainerOpenContext context =
           * baseContainer.getOpenContext(); if (context != null) { final TileEntity te = context.getTile();
           * Platform.openGUI( sender, te, baseContainer.getOpenContext().getSide(),
           * GuiBridge.GUI_PATTERN_VALUE_AMOUNT); if (sender.openContainer instanceof ContainerPatternValueAmount cpv) {
           * if (baseContainer.getTargetStack() != null) { cpv.setValueIndex(this.slot);
           * cpv.getPatternValue().putStack(baseContainer.getTargetStack().getItemStack()); }
           * cpv.detectAndSendChanges(); } } } else if (this.action == InventoryAction.RENAME_PATTERN_ITEM) { final
           * ContainerOpenContext context = baseContainer.getOpenContext(); if (context != null) { final TileEntity te =
           * context.getTile(); Platform.openGUI( sender, te, baseContainer.getOpenContext().getSide(),
           * GuiBridge.GUI_PATTERN_ITEM_RENAMER); if (sender.openContainer instanceof ContainerPatternItemRenamer cpir)
           * { if (baseContainer.getTargetStack() != null) {
           * cpir.getPatternValue().putStack(baseContainer.getTargetStack().getItemStack()); }
           * cpir.detectAndSendChanges(); } } }
           */
    }
}
