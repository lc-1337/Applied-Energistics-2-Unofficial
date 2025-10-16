package appeng.container.implementations;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ICrafting;

import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEStack;
import appeng.client.StorageName;
import appeng.container.ContainerSubGui;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketPatternValueSet;
import appeng.helpers.IVirtualMESlotHandler;

public class ContainerPatternValueAmount extends ContainerSubGui implements IVirtualMESlotHandler {

    private IAEStack<?> aes;
    private int slotsIndex;
    private StorageName invName;

    public ContainerPatternValueAmount(final InventoryPlayer ip, final ITerminalHost te) {
        super(ip, te);
    }

    public IAEStack<?> getStack() {
        return aes;
    }

    public int getSlotsIndex() {
        return slotsIndex;
    }

    public StorageName getInvName() {
        return invName;
    }

    @Override
    public void setVirtualSlot(StorageName invName, int slotId, IAEStack<?> aes) {
        this.invName = invName;
        this.slotsIndex = slotId;
        this.aes = aes;

        update();
    }

    private void update() {
        for (ICrafting crafter : this.crafters) {
            final EntityPlayerMP emp = (EntityPlayerMP) crafter;
            NetworkHandler.instance.sendTo(new PacketPatternValueSet(aes, invName, slotsIndex), emp);
        }
    }
}
