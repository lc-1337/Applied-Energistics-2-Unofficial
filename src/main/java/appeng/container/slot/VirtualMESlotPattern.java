package appeng.container.slot;

import javax.annotation.Nullable;

import appeng.api.storage.data.IAEStack;
import appeng.client.gui.widgets.VirtualMESlot;
import appeng.tile.inventory.IAEStackInventory;

public class VirtualMESlotPattern extends VirtualMESlot {

    private final IAEStackInventory inventory;

    public VirtualMESlotPattern(int x, int y, IAEStackInventory inventory, int slotIndex) {
        super(x, y, slotIndex);
        this.inventory = inventory;
    }

    @Override
    public @Nullable IAEStack<?> getAEStack() {
        return this.inventory.getAEStackInSlot(this.getSlotIndex());
    }
}
