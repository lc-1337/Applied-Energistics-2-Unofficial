package appeng.client.gui.widgets;

import appeng.api.storage.data.IAEStack;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public interface IVirtualSlotHolder {

    void receiveSlotStacks(Int2ObjectMap<IAEStack<?>> slotStacks);
}
