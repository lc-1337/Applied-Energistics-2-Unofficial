package appeng.client.gui.widgets;

import javax.annotation.Nullable;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IDisplayRepo;

public class VirtualMonitorableSlot extends VirtualMESlot {

    protected final IDisplayRepo repo;

    public VirtualMonitorableSlot(int x, int y, IDisplayRepo repo, int slotIndex) {
        super(x, y, slotIndex);
        this.repo = repo;
    }

    @Override
    public @Nullable IAEStack<?> getAEStack() {
        return this.repo.getReferenceStack(this.slotIndex);
    }
}
