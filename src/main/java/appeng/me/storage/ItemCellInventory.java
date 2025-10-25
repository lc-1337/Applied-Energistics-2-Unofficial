package appeng.me.storage;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

import appeng.api.AEApi;
import appeng.api.exceptions.AppEngException;
import appeng.api.storage.ISaveProvider;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

public class ItemCellInventory extends CellInventory<IAEItemStack> {

    public ItemCellInventory(ItemStack o, ISaveProvider container2) throws AppEngException {
        super(o, container2);

        this.setTypeWeight(8);
    }

    @Override
    protected IAEItemStack readStack(NBTTagCompound tag) {
        return AEItemStack.loadItemStackFromNBT(tag);
    }

    @Override
    public IItemList<IAEItemStack> getStorageList() {
        return AEApi.instance().storage().createPrimitiveItemList();
    }

    @Override
    public StorageChannel getChannel() {
        return StorageChannel.ITEMS;
    }

    @Override
    protected boolean isBlackListed(IAEItemStack input) {
        if (BLACK_LIST.contains(
                (OreDictionary.WILDCARD_VALUE << Platform.DEF_OFFSET) | Item.getIdFromItem(input.getItem()))) {
            return true;
        }

        return BLACK_LIST
                .contains((input.getItemDamage() << Platform.DEF_OFFSET) | Item.getIdFromItem(input.getItem()));
    }

    @Override
    protected String getStackTypeTag() {
        return "it";
    }

    @Override
    protected String getStackCountTag() {
        return "ic";
    }
}
