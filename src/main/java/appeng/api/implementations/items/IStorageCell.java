/*
 * The MIT License (MIT) Copyright (c) 2013 AlgorithmX2 Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions: The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package appeng.api.implementations.items;

import javax.annotation.Nullable;

import net.minecraft.item.ItemStack;

import appeng.api.storage.ICellWorkbenchItem;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;

/**
 * Any item which implements this can be treated as an IMEInventory via Util.getCell / Util.isCell It automatically
 * handles the internals and NBT data, which is both nice, and bad for you!
 * <p>
 * Good cause it means you don't have to do anything, bad because you have little to no control over it.
 * <p>
 * The standard AE implementation only provides 1-63 Types
 */
public interface IStorageCell extends ICellWorkbenchItem {

    @Deprecated
    int getBytes(ItemStack cellItem);

    /**
     * It won't work if the return is not a multiple of 8. The limit is ({@link Long#MAX_VALUE} + 1) / 8.
     *
     * @param cellItem item
     * @return number of bytes
     */
    default long getBytesLong(ItemStack cellItem) {
        return getBytes(cellItem);
    }

    /**
     * Determines the number of bytes used for any type included on the cell.
     *
     * @param cellItem item
     * @return number of bytes
     * @deprecated use {@link IStorageCell#getBytesPerType(ItemStack)}
     */
    @Deprecated
    int BytePerType(ItemStack cellItem);

    /**
     * Determines the number of bytes used for any type included on the cell.
     *
     * @param cellItem item
     * @return number of bytes
     */
    int getBytesPerType(ItemStack cellItem);

    /**
     * Must be between 1 and 63, indicates how many types you want to store on the item.
     *
     * @param cellItem item
     * @return number of types
     */
    int getTotalTypes(ItemStack cellItem);

    @Deprecated
    default boolean isBlackListed(ItemStack cellItem, IAEItemStack requestedAddition) {
        return this.isBlackListed(requestedAddition);
    }

    /**
     * Allows you to fine tune which items are allowed on a given cell, if you don't care, just return false; As the
     * handler for this type of cell is still the default cells, the normal AE black list is also applied.
     *
     * @param requestedAddition requested addition
     * @return true to preventAdditionOfItem
     */
    default boolean isBlackListed(IAEStack<?> requestedAddition) {
        return false;
    }

    /**
     * Allows you to specify if this storage cell can be stored inside other storage cells, only set this for special
     * items like the matter cannon that are not general purpose storage.
     *
     * @return true if the storage cell can be stored inside other storage cells, this is generally false, except for
     *         certain situations such as the matter cannon.
     */
    boolean storableInStorageCell();

    /**
     * Allows an item to selectively enable or disable its status as a storage cell.
     *
     * @param i item
     * @return if the ItemStack should behavior as a storage cell.
     */
    boolean isStorageCell(ItemStack i);

    /**
     * @param i item
     * @return drain in ae/t this storage cell will use.
     */
    default double getIdleDrain(@Nullable ItemStack i) {
        // provided for API backwards compatibility
        return getIdleDrain();
    }

    /**
     * The old idle drain API that didn't accept an ItemStack. Unused in base AE2.
     * 
     * @return drain in ae/t this storage cell will use.
     */
    double getIdleDrain();

    default StorageChannel getStorageChannel() {
        return StorageChannel.ITEMS;
    }
}
