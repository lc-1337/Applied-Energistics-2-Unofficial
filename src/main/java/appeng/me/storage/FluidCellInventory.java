package appeng.me.storage;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import appeng.api.AEApi;
import appeng.api.exceptions.AppEngException;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IItemList;
import appeng.util.item.AEFluidStack;

public class FluidCellInventory extends CellInventory<IAEFluidStack> {

    public FluidCellInventory(ItemStack o, ISaveProvider container2) throws AppEngException {
        super(o, container2);

        this.setTypeWeight(256 * 8);
    }

    @Override
    protected IAEFluidStack readStack(NBTTagCompound tag) {
        return AEFluidStack.loadFluidStackFromNBT(tag);
    }

    @Override
    public IItemList<IAEFluidStack> getStorageList() {
        return AEApi.instance().storage().createFluidList();
    }

    @Override
    public StorageChannel getChannel() {
        return StorageChannel.FLUIDS;
    }

    @Override
    protected boolean isBlackListed(IAEFluidStack input) {
        return BLACK_LIST.contains(input.hashCode());
    }

    @Override
    protected String getStackTypeTag() {
        return "ft";
    }

    @Override
    protected String getStackCountTag() {
        return "fc";
    }
}
