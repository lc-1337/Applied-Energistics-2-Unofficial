package appeng.core.sync.packets;

import static appeng.util.Platform.readStackByte;
import static appeng.util.Platform.writeStackByte;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

import appeng.api.networking.IGridHost;
import appeng.api.storage.data.IAEStack;
import appeng.client.StorageName;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.PrimaryGui;
import appeng.container.implementations.ContainerCraftingTerm;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.container.implementations.ContainerPatternValueAmount;
import appeng.container.implementations.ContainerStorageBus;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.network.NetworkHandler;
import appeng.helpers.ISecondaryGUI;
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

    private GuiBridge getOriginalGui(Container container) {
        if (container instanceof ContainerPatternTermEx) {
            return GuiBridge.GUI_PATTERN_TERMINAL_EX;
        } else if (container instanceof ContainerPatternTerm) {
            return GuiBridge.GUI_PATTERN_TERMINAL;
        } else if (container instanceof ContainerCraftingTerm) {
            return GuiBridge.GUI_CRAFTING_TERMINAL;
        } else if (container instanceof ContainerStorageBus) {
            return GuiBridge.GUI_STORAGEBUS;
        } else return GuiBridge.GUI_ME;
    }

    @Override
    public void serverPacketData(INetworkInfo manager, AppEngPacket packet, EntityPlayer player) {
        final Container container = player.openContainer;

        if (container instanceof AEBaseContainer bc) {
            final PrimaryGui pg = bc.getPrimaryGui();

            if (container instanceof ContainerPatternTerm cpt) {
                if (this.action == PatternTerminalAction.SET) {
                    cpt.updateVirtualSlot(invName, slotId, slotItem);
                } else if (this.action == PatternTerminalAction.SET_PATTERN_VALUE) {
                    if (slotItem == null) return;
                    final Object target = cpt.getTarget();
                    if (target instanceof IGridHost) {
                        final ContainerOpenContext context = cpt.getOpenContext();
                        if (context != null) {
                            final TileEntity te = context.getTile();
                            Platform.openGUI(
                                    player,
                                    te,
                                    cpt.getOpenContext().getSide(),
                                    GuiBridge.GUI_PATTERN_VALUE_AMOUNT);
                        }
                    } else {
                        Platform.openGUI(player, null, null, GuiBridge.GUI_PATTERN_VALUE_AMOUNT);
                    }
                    if (player.openContainer instanceof ContainerPatternValueAmount) {
                        final PacketVirtualSlot p = new PacketVirtualSlot(
                                invName,
                                slotId,
                                cpt.getPatternTerminal().getAEInventoryByName(invName).getAEStackInSlot(slotId));
                        NetworkHandler.instance.sendTo(p, (EntityPlayerMP) player);
                    }
                } else if (this.action == PatternTerminalAction.SET_PATTERN_ITEM_NAME) {
                    if (slotItem == null || !slotItem.isItem()) return;
                    final Object target = cpt.getTarget();
                    if (target instanceof IGridHost) {
                        final ContainerOpenContext context = cpt.getOpenContext();
                        if (context != null) {
                            final TileEntity te = context.getTile();
                            Platform.openGUI(
                                    player,
                                    te,
                                    cpt.getOpenContext().getSide(),
                                    GuiBridge.GUI_PATTERN_ITEM_RENAMER);
                        }
                    } else {
                        Platform.openGUI(player, null, null, GuiBridge.GUI_PATTERN_ITEM_RENAMER);
                    }
                    if (player.openContainer instanceof ContainerPatternValueAmount) {
                        final PacketVirtualSlot p = new PacketVirtualSlot(
                                invName,
                                slotId,
                                cpt.getPatternTerminal().getAEInventoryByName(invName).getAEStackInSlot(slotId));
                        NetworkHandler.instance.sendTo(p, (EntityPlayerMP) player);
                    }
                }
            }

            if (player.openContainer instanceof ISecondaryGUI sg) {
                sg.setPrimaryGui(pg);
            }
        }
    }
}
