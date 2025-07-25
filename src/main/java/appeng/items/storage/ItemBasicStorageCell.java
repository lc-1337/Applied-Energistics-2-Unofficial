/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.items.storage;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.IntStream;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;

import com.google.common.base.Optional;

import appeng.api.AEApi;
import appeng.api.config.FuzzyMode;
import appeng.api.config.IncludeExclude;
import appeng.api.definitions.IItemDefinition;
import appeng.api.exceptions.MissingDefinition;
import appeng.api.implementations.items.IItemGroup;
import appeng.api.implementations.items.IStorageCell;
import appeng.api.implementations.items.IUpgradeModule;
import appeng.api.storage.ICellHandler;
import appeng.api.storage.ICellInventoryHandler;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.core.AEConfig;
import appeng.core.features.AEFeature;
import appeng.core.localization.GuiText;
import appeng.helpers.ICellRestriction;
import appeng.items.AEBaseItem;
import appeng.items.contents.CellConfig;
import appeng.items.contents.CellUpgrades;
import appeng.items.materials.MaterialType;
import appeng.me.storage.CellInventory;
import appeng.me.storage.CellInventoryHandler;
import appeng.util.InventoryAdaptor;
import appeng.util.IterationCounter;
import appeng.util.Platform;
import appeng.util.item.ItemList;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemBasicStorageCell extends AEBaseItem implements IStorageCell, IItemGroup, ICellRestriction {

    protected MaterialType component;
    protected long totalBytes;
    protected int perType;
    protected double idleDrain;

    @SuppressWarnings("Guava")
    public ItemBasicStorageCell(final MaterialType whichCell, final long kilobytes) {
        super(Optional.of(kilobytes + "k"));

        this.setFeature(EnumSet.of(AEFeature.StorageCells));
        this.setMaxStackSize(1);
        this.totalBytes = kilobytes * 1024;
        this.component = whichCell;

        switch (this.component) {
            case Cell1kPart -> {
                this.idleDrain = 0.5;
                this.perType = 8;
            }
            case Cell4kPart -> {
                this.idleDrain = 1.0;
                this.perType = 32;
            }
            case Cell16kPart -> {
                this.idleDrain = 1.5;
                this.perType = 128;
            }
            case Cell64kPart -> {
                this.idleDrain = 2.0;
                this.perType = 512;
            }
            default -> {
                this.idleDrain = 0.0;
                this.perType = 8;
            }
        }
    }

    @SuppressWarnings("Guava")
    public ItemBasicStorageCell(final Optional<String> subName) {
        super(subName);
    }

    public static boolean checkInvalidForLockingAndStickyCarding(ItemStack cell, ICellHandler cellHandler) {
        return cellHandler == null || cell == null
                || !(cell.getItem() instanceof ItemBasicStorageCell)
                || (cell.getItem() instanceof ItemBasicStorageCell storageCell && storageCell.getTotalTypes(cell) == 0);
    }

    public static boolean cellIsPartitioned(ICellInventoryHandler cellHandler) {
        return cellHandler != null && cellHandler.getCellInv() != null
                && cellHandler.getCellInv().getConfigInventory() != null
                && IntStream.range(0, cellHandler.getCellInv().getConfigInventory().getSizeInventory())
                        .anyMatch(i -> cellHandler.getCellInv().getConfigInventory().getStackInSlot(i) != null);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addCheckedInformation(final ItemStack stack, final EntityPlayer player, final List<String> lines,
            final boolean displayMoreInfo) {
        final IMEInventoryHandler<?> inventory = AEApi.instance().registries().cell()
                .getCellInventory(stack, null, StorageChannel.ITEMS);

        if (!(inventory instanceof CellInventoryHandler handler)) {
            return;
        }

        if (!(handler.getCellInv() instanceof CellInventory cellInventory)) {
            return;
        }

        lines.add(
                EnumChatFormatting.WHITE + NumberFormat.getInstance(Locale.ENGLISH).format(cellInventory.getUsedBytes())
                        + EnumChatFormatting.GRAY
                        + " "
                        + GuiText.Of.getLocal()
                        + " "
                        + EnumChatFormatting.DARK_GREEN
                        + NumberFormat.getInstance().format(cellInventory.getTotalBytes())
                        + " "
                        + EnumChatFormatting.GRAY
                        + GuiText.BytesUsed.getLocal());

        lines.add(
                EnumChatFormatting.WHITE
                        + NumberFormat.getInstance(Locale.ENGLISH).format(cellInventory.getStoredItemTypes())
                        + EnumChatFormatting.GRAY
                        + " "
                        + GuiText.Of.getLocal()
                        + " "
                        + EnumChatFormatting.DARK_GREEN
                        + NumberFormat.getInstance().format(cellInventory.getMaxItemTypes())
                        + " "
                        + EnumChatFormatting.GRAY
                        + GuiText.Types.getLocal());

        if (cellInventory.getTotalItemTypes() == 1 && cellInventory.getStoredItemTypes() != 0) {
            ItemStack itemStack = handler.getAvailableItems(new ItemList(), IterationCounter.fetchNewId())
                    .getFirstItem().getItemStack();
            lines.add(GuiText.Contains.getLocal() + ": " + itemStack.getDisplayName());
        }

        if (handler.isPreformatted()) {
            String filter = cellInventory.getOreFilter();
            if (filter.isEmpty()) {
                final String list = (handler.getIncludeExcludeMode() == IncludeExclude.WHITELIST ? GuiText.Included
                        : GuiText.Excluded).getLocal();

                if (handler.isFuzzy()) {
                    lines.add(GuiText.Partitioned.getLocal() + " - " + list + ' ' + GuiText.Fuzzy.getLocal());
                } else {
                    lines.add(GuiText.Partitioned.getLocal() + " - " + list + ' ' + GuiText.Precise.getLocal());
                }
                if (GuiScreen.isShiftKeyDown()) {
                    int usedFilters = 0;
                    ArrayList<String> filtersTexts = new ArrayList<>();
                    for (int i = 0; i < cellInventory.getConfigInventory().getSizeInventory(); ++i) {
                        ItemStack s = cellInventory.getConfigInventory().getStackInSlot(i);
                        if (s != null) {
                            usedFilters++;
                            filtersTexts.add(s.getDisplayName());
                        }
                    }
                    lines.add(
                            GuiText.Filter.getLocal() + " ("
                                    + usedFilters
                                    + "/"
                                    + cellInventory.getConfigInventory().getSizeInventory()
                                    + ")"
                                    + ": ");

                    if (!filtersTexts.isEmpty()) {
                        lines.addAll(filtersTexts);
                    }

                }
            } else {
                lines.add(GuiText.PartitionedOre.getLocal() + " : " + filter);
            }

            if (handler.getSticky()) {
                lines.add(GuiText.Sticky.getLocal());
            }
        }
        List<Object> restricted = handler.getRestricted();
        if (restricted != null && ((long) restricted.get(0) != 0 || (byte) restricted.get(1) != 0)) {
            lines.add(GuiText.Restricted.getLocal());
            if (GuiScreen.isShiftKeyDown()) {
                NumberFormat nf = NumberFormat.getNumberInstance();
                if ((long) restricted.get(0) != 0)
                    lines.add(GuiText.MaxItems.getLocal() + " " + nf.format((long) restricted.get(0)));
                if ((byte) restricted.get(1) != 0) lines.add(GuiText.MaxTypes.getLocal() + " " + restricted.get(1));
            }
        }
    }

    @Override
    public int getBytes(final ItemStack cellItem) {
        return (int) this.totalBytes;
    }

    @Override
    public long getBytesLong(final ItemStack cellItem) {
        return this.totalBytes;
    }

    @Override
    public int BytePerType(final ItemStack cell) {
        return this.perType;
    }

    @Override
    public int getBytesPerType(final ItemStack cellItem) {
        return this.perType;
    }

    @Override
    public int getTotalTypes(final ItemStack cellItem) {
        return 63;
    }

    @Override
    public boolean isBlackListed(final ItemStack cellItem, final IAEItemStack requestedAddition) {
        return false;
    }

    @Override
    public boolean storableInStorageCell() {
        return false;
    }

    @Override
    public boolean isStorageCell(final ItemStack i) {
        return true;
    }

    @Override
    public double getIdleDrain() {
        return this.idleDrain;
    }

    @Override
    public String getUnlocalizedGroupName(final Set<ItemStack> others, final ItemStack is) {
        return GuiText.StorageCells.getUnlocalized();
    }

    @Override
    public boolean isEditable(final ItemStack is) {
        return true;
    }

    @Override
    public IInventory getUpgradesInventory(final ItemStack is) {
        return new CellUpgrades(is, 5);
    }

    @Override
    public IInventory getConfigInventory(final ItemStack is) {
        return new CellConfig(is);
    }

    @Override
    public FuzzyMode getFuzzyMode(final ItemStack is) {
        return FuzzyMode.fromItemStack(is);
    }

    @Override
    public void setFuzzyMode(final ItemStack is, final FuzzyMode fzMode) {
        Platform.openNbtData(is).setString("FuzzyMode", fzMode.name());
    }

    @Override
    public String getOreFilter(ItemStack is) {
        return Platform.openNbtData(is).getString("OreFilter");
    }

    @Override
    public void setOreFilter(ItemStack is, String filter) {
        Platform.openNbtData(is).setString("OreFilter", filter);
    }

    @Override
    public ItemStack onItemRightClick(final ItemStack stack, final World world, final EntityPlayer player) {
        this.disassembleDrive(stack, world, player);
        return stack;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private boolean disassembleDrive(final ItemStack stack, final World world, final EntityPlayer player) {
        if (player.isSneaking()) {
            if (Platform.isClient()) {
                return false;
            }
            final InventoryPlayer playerInventory = player.inventory;
            final IMEInventoryHandler inv = AEApi.instance().registries().cell()
                    .getCellInventory(stack, null, StorageChannel.ITEMS);
            if (inv != null && playerInventory.getCurrentItem() == stack) {
                final InventoryAdaptor ia = InventoryAdaptor.getAdaptor(player, ForgeDirection.UNKNOWN);
                final IItemList<IAEItemStack> list = inv
                        .getAvailableItems(StorageChannel.ITEMS.createList(), IterationCounter.fetchNewId());
                if (list.isEmpty() && ia != null) {
                    playerInventory.setInventorySlotContents(playerInventory.currentItem, null);

                    // drop core
                    final ItemStack extraB = ia.addItems(this.component.stack(1));
                    if (extraB != null) {
                        player.dropPlayerItemWithRandomChoice(extraB, false);
                    }

                    // drop upgrades
                    final IInventory upgradesInventory = this.getUpgradesInventory(stack);
                    for (int upgradeIndex = 0; upgradeIndex < upgradesInventory.getSizeInventory(); upgradeIndex++) {
                        final ItemStack upgradeStack = upgradesInventory.getStackInSlot(upgradeIndex);
                        final ItemStack leftStack = ia.addItems(upgradeStack);
                        if (leftStack != null && upgradeStack.getItem() instanceof IUpgradeModule) {
                            player.dropPlayerItemWithRandomChoice(upgradeStack, false);
                        }
                    }

                    // drop empty storage cell case
                    for (final ItemStack storageCellStack : getStorageCellCase().maybeStack(1).asSet()) {
                        final ItemStack extraA = ia.addItems(storageCellStack);
                        if (extraA != null) {
                            player.dropPlayerItemWithRandomChoice(extraA, false);
                        }
                    }

                    if (player.inventoryContainer != null) {
                        player.inventoryContainer.detectAndSendChanges();
                    }

                    return true;
                }
            }
        }
        return false;
    }

    protected IItemDefinition getStorageCellCase() {
        return AEApi.instance().definitions().materials().emptyStorageCell();
    }

    @Override
    public boolean onItemUseFirst(final ItemStack stack, final EntityPlayer player, final World world, final int x,
            final int y, final int z, final int side, final float hitX, final float hitY, final float hitZ) {
        if (ForgeEventFactory.onItemUseStart(player, stack, 1) <= 0) return true;

        return this.disassembleDrive(stack, world, player);
    }

    @Override
    public ItemStack getContainerItem(final ItemStack itemStack) {
        for (final ItemStack stack : AEApi.instance().definitions().materials().emptyStorageCell().maybeStack(1)
                .asSet()) {
            return stack;
        }

        throw new MissingDefinition("Tried to use empty storage cells while basic storage cells are defined.");
    }

    @Override
    public boolean hasContainerItem(final ItemStack stack) {
        return AEConfig.instance.isFeatureEnabled(AEFeature.EnableDisassemblyCrafting);
    }

    @Override
    public String getCellData(ItemStack is) {
        return totalBytes + ","
                + this.getTotalTypes(null)
                + ","
                + perType
                + ","
                + 8
                + ","
                + Platform.openNbtData(is).getByte("cellRestrictionTypes")
                + ","
                + Platform.openNbtData(is).getLong("cellRestrictionAmount")
                + ","
                + "item";
    }

    @Override
    public void setCellRestriction(ItemStack is, String newData) {
        List<String> data = Arrays.asList(newData.split(",", 2));
        Platform.openNbtData(is).setByte("cellRestrictionTypes", Byte.parseByte(data.get(0)));
        Platform.openNbtData(is).setLong("cellRestrictionAmount", Long.parseLong(data.get(1)));
    }
}
