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
        super(x, y, slotIndex);
        this.inventory = inventory;

        this.showAmount = false;
        this.showAmountAlways = false;
        this.showCraftableText = false;
        this.showCraftableIcon = false;
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
        final EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        final ItemStack hand = player.inventory.getItemStack();
        IAEStack<?> aes = null;

        if (hand != null) {
            if (acceptFluid && (!acceptItem || isExtraAction)) {
                aes = AEFluidStack.create(FluidUtils.getFluidFromContainer(hand));
                aes.setStackSize(1);
            } else if (acceptItem) {
                aes = AEItemStack.create(hand);
                aes.setStackSize(1);
            }
        }

        inventory.putAEStackInSlot(this.getSlotIndex(), aes);
        NetworkHandler.instance.sendToServer(new PacketVirtualSlot(this.getStorageName(), this.getSlotIndex(), aes));
    }
}
