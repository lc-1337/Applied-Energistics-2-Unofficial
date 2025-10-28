package appeng.client.gui.slots;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import appeng.api.storage.data.IAEStack;
import appeng.client.StorageName;
import appeng.core.sync.network.NetworkHandler;
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
        handleMouseClicked(acceptItem, acceptFluid, isExtraAction, null, false);
    }

    public void handleMouseClicked(boolean acceptItem, boolean acceptFluid, boolean isExtraAction, ItemStack dragNDrop,
            boolean rightClick) {
        final EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        final ItemStack hand = player.inventory.getItemStack() != null ? player.inventory.getItemStack() : dragNDrop;
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

            if (showAmount && aes != null && rightClick && aes.equals(oldStack)) {
                oldStack.decStackSize(-2); // because guipattern decrease by 1 when player hand null
                aes = oldStack.copy();
            }
        }

        inventory.putAEStackInSlot(this.getSlotIndex(), aes);
        NetworkHandler.instance.sendToServer(new PacketVirtualSlot(this.getStorageName(), this.getSlotIndex(), aes));
    }
}
