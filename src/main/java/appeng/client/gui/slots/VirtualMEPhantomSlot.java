package appeng.client.gui.slots;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import appeng.api.storage.data.IAEStack;
import appeng.client.StorageName;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.core.sync.packets.PacketVirtualSlot;
import appeng.tile.inventory.IAEStackInventory;
import appeng.util.FluidUtils;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;

public class VirtualMEPhantomSlot extends VirtualMESlot {

    private final IAEStackInventory inventory;

    public VirtualMEPhantomSlot(int x, int y, IAEStackInventory inventory, int slotIndex) {
        this(x, y, inventory, slotIndex, false);
    }

    public VirtualMEPhantomSlot(int x, int y, IAEStackInventory inventory, int slotIndex, boolean showAmount) {
        super(x, y, slotIndex);
        this.inventory = inventory;

        this.showAmount = showAmount;
    }

    @Nullable
    @Override
    public IAEStack<?> getAEStack() {
        return this.inventory.getAEStackInSlot(this.getSlotIndex());
    }

    public StorageName getStorageName() {
        return this.inventory.getStorageName();
    }

    public void handleMouseClicked(boolean acceptItem, boolean acceptFluid, boolean isExtraAction) {
        handleMouseClicked(acceptItem, acceptFluid, isExtraAction, null, 0);
    }

    // try nei dragNDrop make frind with regular interaction, unfinished
    public void handleMouseClicked(boolean acceptItem, boolean acceptFluid, boolean isExtraAction, ItemStack dragNDrop,
            int mouseButton) {
        final EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        ItemStack hand = player.inventory.getItemStack();

        if (hand != null && dragNDrop != null) return;

        if (hand == null) hand = dragNDrop;

        IAEStack<?> aes = null;
        final IAEStack<?> oldStack = getAEStack();

        if (hand != null) {
            if (acceptFluid && (!acceptItem || isExtraAction)) {
                aes = AEFluidStack.create(FluidUtils.getFluidFromContainer(hand));
                if (!showAmount) aes.setStackSize(1);
            } else if (acceptItem) {
                aes = AEItemStack.create(hand);
                if (!showAmount) aes.setStackSize(1);
            }

            if (showAmount && aes != null && mouseButton == 1 && aes.equals(oldStack)) {
                oldStack.decStackSize(-2); // because guipattern decrease by 1 when player hand null
                aes = oldStack.copy();
            }
        }

        inventory.putAEStackInSlot(this.getSlotIndex(), aes);
        NetworkHandler.instance.sendToServer(new PacketVirtualSlot(this.getStorageName(), this.getSlotIndex(), aes));
    }

    protected void handleVirtualSlotInteraction(VirtualMEPhantomSlot slot, boolean isLControlDown, int mouseButton) {
        final EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        IAEStack<?> aes = slot.getAEStack();
        final ItemStack playerHand = player.inventory.getItemStack();
        final ItemStack is = playerHand != null ? playerHand.copy() : null;

        switch (mouseButton) {
            case 0 -> { // left click
                if (playerHand != null) {
                    if (isLControlDown) {
                        aes = AEFluidStack.create(FluidUtils.getFluidFromContainer(is));
                    } else {
                        aes = AEItemStack.create(is);
                    }
                } else {
                    aes = null;
                }
            }
            case 1 -> { // right click
                if (playerHand != null) {
                    is.stackSize = 1;
                    if (isLControlDown) aes = AEFluidStack.create(FluidUtils.getFluidFromContainer(is));
                    else aes = AEItemStack.create(is);

                    if (aes != null && aes.equals(slot.getAEStack())) {
                        aes = slot.getAEStack();
                        aes.decStackSize(-1);
                    }
                } else if (aes != null) {
                    aes.decStackSize(1);
                    if (aes.getStackSize() <= 0) aes = null;
                }
            }
            case 2 -> { // middle click
                if (aes != null) {
                    if (isLControlDown) {
                        NetworkHandler.instance.sendToServer(new PacketSwitchGuis(GuiBridge.GUI_PATTERN_ITEM_RENAMER));
                    } else {
                        if (showAmount) NetworkHandler.instance
                                .sendToServer(new PacketSwitchGuis(GuiBridge.GUI_PATTERN_VALUE_AMOUNT));
                    }
                }
            }
        }

        NetworkHandler.instance.sendToServer(new PacketVirtualSlot(slot.getStorageName(), slot.getSlotIndex(), aes));
    }
}
