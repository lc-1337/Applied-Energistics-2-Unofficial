/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.items.tools.powered;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;

import com.glodblock.github.common.item.ItemBaseWirelessTerminal;
import com.google.common.base.Optional;

import appeng.api.AEApi;
import appeng.api.config.Settings;
import appeng.api.config.SortDir;
import appeng.api.config.SortOrder;
import appeng.api.config.TypeFilter;
import appeng.api.config.ViewItems;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.util.IConfigManager;
import appeng.core.AEConfig;
import appeng.core.AppEng;
import appeng.core.features.AEFeature;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.items.tools.powered.powersink.AEBasePoweredItem;
import appeng.util.ConfigManager;
import appeng.util.Platform;
import baubles.api.BaubleType;
import baubles.api.IBauble;
import baubles.api.expanded.IBaubleExpanded;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@cpw.mods.fml.common.Optional.InterfaceList(
        value = { @cpw.mods.fml.common.Optional.Interface(iface = "baubles.api.IBauble", modid = "Baubles"),
                @cpw.mods.fml.common.Optional.Interface(
                        iface = "baubles.api.expanded.IBaubleExpanded",
                        modid = "Baubles|Expanded") })
public class ToolWirelessTerminal extends AEBasePoweredItem implements IWirelessTermHandler, IBauble, IBaubleExpanded {

    public static String infinityBoosterCard = "infinityBoosterCard";
    public static String infinityEnergyCard = "InfinityEnergyCard";

    public ToolWirelessTerminal() {
        super(AEConfig.instance.wirelessTerminalBattery, Optional.absent());
        this.setFeature(EnumSet.of(AEFeature.WirelessAccessTerminal, AEFeature.PoweredTools));
    }

    @Override
    public ItemStack onItemRightClick(final ItemStack item, final World w, final EntityPlayer player) {
        if (ForgeEventFactory.onItemUseStart(player, item, 1) > 0)
            AEApi.instance().registries().wireless().openWirelessTerminalGui(item, w, player);
        return item;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean isFull3D() {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addCheckedInformation(final ItemStack stack, final EntityPlayer player, final List<String> lines,
            final boolean displayMoreInfo) {
        super.addCheckedInformation(stack, player, lines, displayMoreInfo);

        if (stack.hasTagCompound()) {
            final NBTTagCompound tag = Platform.openNbtData(stack);
            if (tag != null) {
                final String encKey = tag.getString("encryptionKey");

                if (encKey == null || encKey.isEmpty()) {
                    lines.add(EnumChatFormatting.RED + GuiText.Unlinked.getLocal() + EnumChatFormatting.RESET);
                } else {
                    lines.add(EnumChatFormatting.GREEN + GuiText.Linked.getLocal() + EnumChatFormatting.RESET);
                }
            }
        } else {
            lines.add(GuiText.Unlinked.getLocal());
        }
    }

    @Override
    public boolean canHandle(final ItemStack is) {
        return AEApi.instance().definitions().items().wirelessTerminal().isSameAs(is);
    }

    @Override
    public boolean usePower(final EntityPlayer player, final double amount, final ItemStack is) {
        return this.extractAEPower(is, amount) >= amount - 0.5;
    }

    @Override
    public boolean hasPower(final EntityPlayer player, final double amt, final ItemStack is) {
        return this.getAECurrentPower(is) >= amt;
    }

    @Override
    public boolean hasInfinityPower(ItemStack is) {
        if (is.getItem() instanceof ItemBaseWirelessTerminal) {
            NBTTagCompound data = Platform.openNbtData(is);
            return (data.hasKey(infinityEnergyCard) && data.getBoolean(infinityEnergyCard));
        }
        return false;
    }

    @Override
    public boolean hasInfinityRange(ItemStack is) {
        if (is.getItem() instanceof ItemBaseWirelessTerminal) {
            NBTTagCompound data = Platform.openNbtData(is);
            return data.hasKey(infinityBoosterCard) && data.getBoolean(infinityBoosterCard);
        }

        return false;
    }

    @Override
    public IConfigManager getConfigManager(final ItemStack target) {
        final ConfigManager out = new ConfigManager((manager, settingName, newValue) -> {
            final NBTTagCompound data = Platform.openNbtData(target);
            manager.writeToNBT(data);
        });

        out.registerSetting(Settings.SORT_BY, SortOrder.NAME);
        out.registerSetting(Settings.VIEW_MODE, ViewItems.ALL);
        out.registerSetting(Settings.SORT_DIRECTION, SortDir.ASCENDING);
        out.registerSetting(Settings.TYPE_FILTER, TypeFilter.ALL);

        out.readFromNBT((NBTTagCompound) Platform.openNbtData(target).copy());
        return out;
    }

    @Override
    public String getEncryptionKey(final ItemStack item) {
        final NBTTagCompound tag = Platform.openNbtData(item);
        return tag.getString("encryptionKey");
    }

    @Override
    public void setEncryptionKey(final ItemStack item, final String encKey, final String name) {
        final NBTTagCompound tag = Platform.openNbtData(item);
        tag.setString("encryptionKey", encKey);
        tag.setString("name", name);
    }

    @Override
    public String[] getBaubleTypes(ItemStack itemstack) {
        return new String[] { AppEng.BAUBLESLOT };
    }

    public void openGui(final ItemStack is, final World w, final EntityPlayer player, final Object mode) {
        Platform.openGUI(player, null, null, GuiBridge.GUI_ME);
    }

    @Override
    @cpw.mods.fml.common.Optional.Method(modid = "Baubles")
    public BaubleType getBaubleType(ItemStack itemStack) {
        return BaubleType.RING;
    }

    @Override
    @cpw.mods.fml.common.Optional.Method(modid = "Baubles")
    public void onWornTick(ItemStack itemstack, EntityLivingBase player) { /**/ }

    @Override
    @cpw.mods.fml.common.Optional.Method(modid = "Baubles")
    public void onEquipped(ItemStack itemstack, EntityLivingBase player) { /**/ }

    @Override
    @cpw.mods.fml.common.Optional.Method(modid = "Baubles")
    public void onUnequipped(ItemStack itemstack, EntityLivingBase player) { /**/ }

    @Override
    @cpw.mods.fml.common.Optional.Method(modid = "Baubles")
    public boolean canEquip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }

    @Override
    @cpw.mods.fml.common.Optional.Method(modid = "Baubles")
    public boolean canUnequip(ItemStack itemstack, EntityLivingBase player) {
        return true;
    }
}
