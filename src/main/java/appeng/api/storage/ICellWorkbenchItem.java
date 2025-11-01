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

package appeng.api.storage;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import appeng.api.config.FuzzyMode;
import appeng.items.contents.CellConfigLegacy;
import appeng.items.contents.CellConfigLegacyWrapper;
import appeng.tile.inventory.IAEStackInventory;

public interface ICellWorkbenchItem {

    /**
     * if this return false, the item will not be treated as a cell, and cannot be inserted into the work bench.
     *
     * @param is item
     * @return true if the item should be editable in the cell workbench.
     */
    boolean isEditable(ItemStack is);

    /**
     * used to edit the upgrade slots on your cell, should have a capacity of 0-24, you are also responsible for
     * implementing the valid checks, and any storage/usage of them.
     * <p>
     * onInventoryChange will be called when saving is needed.
     */
    IInventory getUpgradesInventory(ItemStack is);

    @Deprecated
    default IInventory getConfigInventory(ItemStack is) {
        return new CellConfigLegacy(this.getConfigAEInventory(is), this.getStorageChannel());
    }

    /**
     * Used to extract, or mirror the contents of the work bench onto the cell.
     * <p>
     * - This should have exactly 63 slots, any more, or less might cause issues.
     * <p>
     * onInventoryChange will be called when saving is needed.
     */
    default IAEStackInventory getConfigAEInventory(ItemStack is) {
        return new CellConfigLegacyWrapper(this.getConfigInventory(is));
    }

    /**
     * @return the current fuzzy status.
     */
    FuzzyMode getFuzzyMode(ItemStack is);

    /**
     * sets the setting on the cell.
     */
    void setFuzzyMode(ItemStack is, FuzzyMode fzMode);

    /**
     * @param is cell item
     * @return current ore dictionary filter
     */
    default String getOreFilter(ItemStack is) {
        return "";
    }

    /**
     * @param is     cell item
     * @param filter new ore dictionary filter
     */
    default void setOreFilter(ItemStack is, String filter) {}

    default StorageChannel getStorageChannel() {
        return StorageChannel.ITEMS;
    }
}
