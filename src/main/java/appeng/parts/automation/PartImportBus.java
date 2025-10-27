/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.parts.automation;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.item.ItemStack;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.Settings;
import appeng.api.config.Upgrades;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.parts.IPartRenderHelper;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.client.texture.CableBusTextures;
import appeng.helpers.Reflected;
import appeng.me.GridAccessException;
import appeng.util.InventoryAdaptor;
import appeng.util.Platform;
import appeng.util.inv.IInventoryDestination;
import appeng.util.inv.ItemSlot;
import appeng.util.item.AEItemStack;
import appeng.util.prioitylist.OreFilteredList;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PartImportBus extends PartBaseImportBus<IAEItemStack> implements IInventoryDestination {

    @Reflected
    public PartImportBus(final ItemStack is) {
        super(is);

        this.getConfigManager().registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
    }

    @Override
    public boolean canInsert(final ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }

        final IAEItemStack out = this.destination.injectItems(
                this.lastItemChecked = AEApi.instance().storage().createItemStack(stack),
                Actionable.SIMULATE,
                this.mySrc);
        if (out == null) {
            return true;
        }
        return out.getStackSize() != stack.stackSize;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventory(final IPartRenderHelper rh, final RenderBlocks renderer) {
        rh.setTexture(
                CableBusTextures.PartImportSides.getIcon(),
                CableBusTextures.PartImportSides.getIcon(),
                CableBusTextures.PartMonitorBack.getIcon(),
                this.getItemStack().getIconIndex(),
                CableBusTextures.PartImportSides.getIcon(),
                CableBusTextures.PartImportSides.getIcon());

        rh.setBounds(3, 3, 15, 13, 13, 16);
        rh.renderInventoryBox(renderer);

        rh.setBounds(4, 4, 14, 12, 12, 15);
        rh.renderInventoryBox(renderer);

        rh.setBounds(5, 5, 13, 11, 11, 14);
        rh.renderInventoryBox(renderer);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(final int x, final int y, final int z, final IPartRenderHelper rh,
            final RenderBlocks renderer) {
        this.setRenderCache(rh.useSimplifiedRendering(x, y, z, this, this.getRenderCache()));
        rh.setTexture(
                CableBusTextures.PartImportSides.getIcon(),
                CableBusTextures.PartImportSides.getIcon(),
                CableBusTextures.PartMonitorBack.getIcon(),
                this.getItemStack().getIconIndex(),
                CableBusTextures.PartImportSides.getIcon(),
                CableBusTextures.PartImportSides.getIcon());

        rh.setBounds(4, 4, 14, 12, 12, 16);
        rh.renderBlock(x, y, z, renderer);

        rh.setBounds(5, 5, 13, 11, 11, 14);
        rh.renderBlock(x, y, z, renderer);

        rh.setBounds(6, 6, 12, 10, 10, 13);
        rh.renderBlock(x, y, z, renderer);
        rh.setTexture(
                CableBusTextures.PartMonitorSidesStatus.getIcon(),
                CableBusTextures.PartMonitorSidesStatus.getIcon(),
                CableBusTextures.PartMonitorBack.getIcon(),
                this.getItemStack().getIconIndex(),
                CableBusTextures.PartMonitorSidesStatus.getIcon(),
                CableBusTextures.PartMonitorSidesStatus.getIcon());

        rh.setBounds(6, 6, 11, 10, 10, 12);
        rh.renderBlock(x, y, z, renderer);

        this.renderLights(x, y, z, rh, renderer);
    }

    @Override
    protected float getPowerMultiplier() {
        return 1;
    }

    @Override
    protected boolean supportFuzzy() {
        return true;
    }

    @Override
    protected boolean supportOreDict() {
        return true;
    }

    @Override
    protected IMEMonitor<IAEItemStack> getMonitor() {
        try {
            return this.getProxy().getStorage().getItemInventory();
        } catch (final GridAccessException e) {
            return null;
        }
    }

    @Override
    protected int calculateItemsToSend() {
        return switch (this.getInstalledUpgrades(Upgrades.SPEED)) {
            case 1 -> 8;
            case 2 -> 32;
            case 3 -> 64;
            case 4 -> 96;
            default -> 1;
        };
    }

    @Override
    protected boolean importStuff(final InventoryAdaptor myAdaptor, final IAEItemStack whatToImport,
            final IMEMonitor<IAEItemStack> inv, final IEnergySource energy, final FuzzyMode fzMode) {
        final int toSend = this.calculateMaximumAmountToImport(myAdaptor, whatToImport, inv, fzMode);
        final ItemStack newItems;

        if (this.getInstalledUpgrades(Upgrades.FUZZY) > 0) {
            newItems = myAdaptor.removeSimilarItems(
                    toSend,
                    whatToImport == null ? null : whatToImport.getItemStack(),
                    fzMode,
                    this.configDestination(inv));
        } else {
            newItems = myAdaptor.removeItems(
                    toSend,
                    whatToImport == null ? null : whatToImport.getItemStack(),
                    this.configDestination(inv));
        }

        if (newItems != null) {
            newItems.stackSize = (int) (Math.min(
                    newItems.stackSize,
                    energy.extractAEPower(newItems.stackSize, Actionable.SIMULATE, PowerMultiplier.CONFIG)) + 0.01);
            this.itemToSend -= newItems.stackSize;

            if (this.lastItemChecked == null || !this.lastItemChecked.isSameType(newItems)) {
                this.lastItemChecked = AEApi.instance().storage().createItemStack(newItems);
            } else {
                this.lastItemChecked.setStackSize(newItems.stackSize);
            }

            final IAEItemStack failed = Platform
                    .poweredInsert(energy, this.destination, this.lastItemChecked, this.mySrc);

            if (failed != null) {
                myAdaptor.addItems(failed.getItemStack());
                return true;
            } else {
                this.worked = true;
            }
        } else {
            return true;
        }

        return false;
    }

    private int calculateMaximumAmountToImport(final InventoryAdaptor myAdaptor, final IAEItemStack whatToImport,
            final IMEMonitor<IAEItemStack> inv, final FuzzyMode fzMode) {
        final int toSend = Math.min(this.itemToSend, 64);
        final ItemStack itemStackToImport;

        if (whatToImport == null) {
            itemStackToImport = null;
        } else {
            itemStackToImport = whatToImport.getItemStack();
        }

        final IAEItemStack itemAmountNotStorable;
        final ItemStack simResult;
        if (this.getInstalledUpgrades(Upgrades.FUZZY) > 0) {
            simResult = myAdaptor.simulateSimilarRemove(toSend, itemStackToImport, fzMode, this.configDestination(inv));
        } else {
            simResult = myAdaptor.simulateRemove(toSend, itemStackToImport, this.configDestination(inv));
        }
        itemAmountNotStorable = this.destination
                .injectItems(AEItemStack.create(simResult), Actionable.SIMULATE, this.mySrc);

        if (itemAmountNotStorable != null) {
            return (int) Math.min(simResult.stackSize - itemAmountNotStorable.getStackSize(), toSend);
        }

        return toSend;
    }

    private IInventoryDestination configDestination(final IMEMonitor<IAEItemStack> itemInventory) {
        this.destination = itemInventory;
        return this;
    }

    @Override
    protected boolean doOreDict(final InventoryAdaptor myAdaptor, IMEMonitor<IAEItemStack> inv,
            final IEnergyGrid energy, final FuzzyMode fzMode) {
        if (!oreFilterString.isEmpty()) {
            if (filterPredicate == null) filterPredicate = OreFilteredList.makeFilter(oreFilterString);
            for (ItemSlot slot : myAdaptor) {
                if (this.itemToSend <= 0) break;
                if (slot.isExtractable() && filterPredicate != null && filterPredicate.test(slot.getAEItemStack())) {
                    while (this.itemToSend > 0) {
                        if (this.importStuff(myAdaptor, slot.getAEItemStack(), inv, energy, fzMode)) break;
                    }
                }
            }
            return true;
        }
        return false;
    }
}
