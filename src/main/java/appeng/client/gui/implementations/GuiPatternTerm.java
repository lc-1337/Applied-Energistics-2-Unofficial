/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.client.gui.implementations;

import java.io.IOException;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import appeng.api.config.ActionItems;
import appeng.api.config.ItemSubstitution;
import appeng.api.config.PatternBeSubstitution;
import appeng.api.config.Settings;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEStack;
import appeng.client.StorageName;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.container.slot.AppEngSlot;
import appeng.container.slot.VirtualMESlotPattern;
import appeng.core.AELog;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.core.sync.packets.PacketPatternTerminalSlotUpdate;
import appeng.core.sync.packets.PacketValueConfig;
import appeng.helpers.InventoryAction;
import appeng.helpers.PatternTerminalAction;

public class GuiPatternTerm extends GuiMEMonitorable {

    private static final String SUBSITUTION_DISABLE = "0";
    private static final String SUBSITUTION_ENABLE = "1";

    private static final String CRAFTMODE_CRFTING = "1";
    private static final String CRAFTMODE_PROCESSING = "0";

    private final ContainerPatternTerm container;

    private GuiTabButton tabCraftButton;
    private GuiTabButton tabProcessButton;
    protected GuiImgButton substitutionsEnabledBtn;
    protected GuiImgButton substitutionsDisabledBtn;
    protected GuiImgButton beSubstitutionsEnabledBtn;
    protected GuiImgButton beSubstitutionsDisabledBtn;
    private GuiImgButton encodeBtn;
    protected GuiImgButton clearBtn;
    protected GuiImgButton doubleBtn;

    public GuiPatternTerm(final InventoryPlayer inventoryPlayer, final ITerminalHost te,
            final ContainerPatternTermEx containerPatternTermEx) {
        super(inventoryPlayer, te, containerPatternTermEx);
        this.container = (ContainerPatternTerm) this.inventorySlots;
        this.setReservedSpace(81);
    }

    public GuiPatternTerm(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        super(inventoryPlayer, te, new ContainerPatternTerm(inventoryPlayer, te, true));
        this.container = (ContainerPatternTerm) this.inventorySlots;
        this.setReservedSpace(81);
    }

    @Override
    protected void mouseClicked(final int xCoord, final int yCoord, final int btn) {

        if (btn == 2 && doubleBtn.mousePressed(this.mc, xCoord, yCoord)) { //
            InventoryAction action = InventoryAction.SET_PATTERN_MULTI;

            final PacketInventoryAction p = new PacketInventoryAction(action, 0, 0);
            NetworkHandler.instance.sendToServer(p);
        } else super.mouseClicked(xCoord, yCoord, btn);

    }

    @Override
    protected boolean handleSlotClick(int mouseX, int mouseY, int mouseButton) {
        final VirtualMESlotPattern slot = container.craftingSlots[0]; // TODO get slot somehow
        final StorageName name = StorageName.CRAFTING_INPUT; // TODO get storage name somehow
        if (slot != null) {
            final EntityPlayer player = Minecraft.getMinecraft().thePlayer;

            final boolean isLShiftDown = isShiftKeyDown();
            final boolean isLControlDown = isCtrlKeyDown();

            switch (mouseButton) {
                /*
                 * case 0 -> { // left click if (player.inventory.getItemStack() != null) { this.sendAction(
                 * isLControlDown ? PatternTerminalAction.EMPTY_CAN : PatternTerminalAction.SET_STACK, null,
                 * slot.getSlotIndex(), name); return true; } if (isLControlDown) { this.sendAction( isLShiftDown ?
                 * MonitorableAction.FILL_CONTAINERS : MonitorableAction.FILL_SINGLE_CONTAINER,
                 * stackConvert(slot.getAEStack()), this.meSlots.length); return true; } if (isLShiftDown) {
                 * this.sendAction(MonitorableAction.SHIFT_CLICK, slotStack, this.meSlots.length); return true; } if
                 * (slot.getAEStack() != null && slot.getAEStack().getStackSize() == 0 &&
                 * player.inventory.getItemStack() == null) { // TODO: native
                 * this.sendAction(MonitorableAction.AUTO_CRAFT, stackConvert(slot.getAEStack()), this.meSlots.length);
                 * return true; } this.sendAction(MonitorableAction.PICKUP_OR_SET_DOWN, slotStack, this.meSlots.length);
                 * return true; } case 1 -> { // right click if (slot instanceof VirtualPinSlot) { if (isLShiftDown) {
                 * this.sendAction(MonitorableAction.UNSET_PIN, null, slot.getSlotIndex()); } else { this.sendAction(
                 * isLControlDown ? MonitorableAction.SET_CONTAINER_PIN : MonitorableAction.SET_ITEM_PIN, null,
                 * slot.getSlotIndex()); } return true; } if (isLControlDown) { this.sendAction( isLShiftDown ?
                 * MonitorableAction.DRAIN_CONTAINERS : MonitorableAction.DRAIN_SINGLE_CONTAINER,
                 * stackConvert(slot.getAEStack()), this.meSlots.length); return true; } if (isLShiftDown) {
                 * this.sendAction(MonitorableAction.PICKUP_SINGLE, slotStack, this.meSlots.length); return true; }
                 * this.sendAction(MonitorableAction.SPLIT_OR_PLACE_SINGLE, slotStack, this.meSlots.length); return
                 * true; }
                 */
                case 2 -> { // middle click
                    if (slot.getAEStack() != null) {
                        PatternTerminalAction action = PatternTerminalAction.SET_PATTERN_VALUE;
                        if (isCtrlKeyDown()) {
                            action = PatternTerminalAction.SET_PATTERN_ITEM_NAME;
                        }
                        this.sendAction(action, slot.getAEStack(), slot.getSlotIndex(), name);
                        return true;
                    }
                }
                default -> {}
            }
        }

        return super.handleSlotClick(mouseX, mouseY, mouseButton);
    }

    private void sendAction(PatternTerminalAction action, @Nullable IAEStack<?> stack, int slotIndex,
            StorageName invName) {
        try {
            NetworkHandler.instance
                    .sendToServer(new PacketPatternTerminalSlotUpdate(invName, slotIndex, stack, action));
        } catch (IOException e) {
            AELog.error(e);
        }
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);

        try {
            if (this.tabCraftButton == btn || this.tabProcessButton == btn) {
                NetworkHandler.instance.sendToServer(
                        new PacketValueConfig(
                                "PatternTerminal.CraftMode",
                                this.tabProcessButton == btn ? CRAFTMODE_CRFTING : CRAFTMODE_PROCESSING));
            } else if (this.encodeBtn == btn) {
                NetworkHandler.instance.sendToServer(
                        new PacketValueConfig(
                                "PatternTerminal.Encode",
                                isCtrlKeyDown() ? (isShiftKeyDown() ? "6" : "1") : (isShiftKeyDown() ? "2" : "1")));
            } else if (this.clearBtn == btn) {
                NetworkHandler.instance.sendToServer(new PacketValueConfig("PatternTerminal.Clear", "1"));
            } else if (this.substitutionsEnabledBtn == btn || this.substitutionsDisabledBtn == btn) {
                NetworkHandler.instance.sendToServer(
                        new PacketValueConfig(
                                "PatternTerminal.Substitute",
                                this.substitutionsEnabledBtn == btn ? SUBSITUTION_DISABLE : SUBSITUTION_ENABLE));
            } else if (this.beSubstitutionsEnabledBtn == btn || this.beSubstitutionsDisabledBtn == btn) {
                NetworkHandler.instance.sendToServer(
                        new PacketValueConfig(
                                "PatternTerminal.BeSubstitute",
                                this.beSubstitutionsEnabledBtn == btn ? SUBSITUTION_DISABLE : SUBSITUTION_ENABLE));
            } else if (doubleBtn == btn) {
                int val = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) ? 1 : 0;
                if (Mouse.isButtonDown(1)) val |= 0b10;
                NetworkHandler.instance
                        .sendToServer(new PacketValueConfig("PatternTerminal.Double", String.valueOf(val)));
            }
        } catch (final IOException e) {
            AELog.error(e);
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        if (this.container.getCraftingModeSupport()) {
            this.tabCraftButton = new GuiTabButton(
                    this.guiLeft + 173,
                    this.guiTop + this.ySize - 177,
                    new ItemStack(Blocks.crafting_table),
                    GuiText.CraftingPattern.getLocal(),
                    itemRender);
            this.buttonList.add(this.tabCraftButton);

            this.tabProcessButton = new GuiTabButton(
                    this.guiLeft + 173,
                    this.guiTop + this.ySize - 177,
                    new ItemStack(Blocks.furnace),
                    GuiText.ProcessingPattern.getLocal(),
                    itemRender);
            this.buttonList.add(this.tabProcessButton);
        }

        this.substitutionsEnabledBtn = new GuiImgButton(
                this.guiLeft + 84,
                this.guiTop + this.ySize - 163,
                Settings.ACTIONS,
                ItemSubstitution.ENABLED);
        this.substitutionsEnabledBtn.setHalfSize(true);
        this.buttonList.add(this.substitutionsEnabledBtn);

        this.substitutionsDisabledBtn = new GuiImgButton(
                this.guiLeft + 84,
                this.guiTop + this.ySize - 163,
                Settings.ACTIONS,
                ItemSubstitution.DISABLED);
        this.substitutionsDisabledBtn.setHalfSize(true);
        this.buttonList.add(this.substitutionsDisabledBtn);

        this.beSubstitutionsEnabledBtn = new GuiImgButton(
                this.guiLeft + 84,
                this.guiTop + this.ySize - 153,
                Settings.ACTIONS,
                PatternBeSubstitution.ENABLED);
        this.beSubstitutionsEnabledBtn.setHalfSize(true);
        this.buttonList.add(this.beSubstitutionsEnabledBtn);

        this.beSubstitutionsDisabledBtn = new GuiImgButton(
                this.guiLeft + 84,
                this.guiTop + this.ySize - 153,
                Settings.ACTIONS,
                PatternBeSubstitution.DISABLED);
        this.beSubstitutionsDisabledBtn.setHalfSize(true);
        this.buttonList.add(this.beSubstitutionsDisabledBtn);

        this.clearBtn = new GuiImgButton(
                this.guiLeft + 74,
                this.guiTop + this.ySize - 163,
                Settings.ACTIONS,
                ActionItems.CLOSE);
        this.clearBtn.setHalfSize(true);
        this.buttonList.add(this.clearBtn);

        this.encodeBtn = new GuiImgButton(
                this.guiLeft + 147,
                this.guiTop + this.ySize - 142,
                Settings.ACTIONS,
                ActionItems.ENCODE);
        this.buttonList.add(this.encodeBtn);

        this.doubleBtn = new GuiImgButton(
                this.guiLeft + 74,
                this.guiTop + this.ySize - 153,
                Settings.ACTIONS,
                ActionItems.DOUBLE);
        this.doubleBtn.setHalfSize(true);
        this.buttonList.add(this.doubleBtn);
        this.updateButtonVisibility();
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.updateButtonVisibility();

        super.drawFG(offsetX, offsetY, mouseX, mouseY);

        drawVirtualSlots(container.craftingSlots);
        drawVirtualSlots(container.outputSlots);

        drawTitle();
    }

    protected void drawTitle() {
        this.fontRendererObj.drawString(
                GuiText.PatternTerminal.getLocal(),
                8,
                this.ySize - 96 + 2 - this.getReservedSpace(),
                GuiColors.PatternTerminalTitle.getColor());
    }

    private void updateButtonVisibility() {
        if (this.tabCraftButton != null) {
            if (!this.container.isCraftingMode()) {
                this.tabCraftButton.visible = false;
                this.tabProcessButton.visible = true;
                this.doubleBtn.visible = true;
            } else {
                this.tabCraftButton.visible = true;
                this.tabProcessButton.visible = false;
                this.doubleBtn.visible = false;
            }
        }

        if (this.container.substitute) {
            this.substitutionsEnabledBtn.visible = true;
            this.substitutionsDisabledBtn.visible = false;
        } else {
            this.substitutionsEnabledBtn.visible = false;
            this.substitutionsDisabledBtn.visible = true;
        }

        this.beSubstitutionsEnabledBtn.visible = this.container.beSubstitute;
        this.beSubstitutionsDisabledBtn.visible = !this.container.beSubstitute;
    }

    @Override
    protected String getBackground() {
        if (this.container.isCraftingMode()) {
            return "guis/pattern.png";
        }
        return "guis/pattern2.png";
    }

    @Override
    protected void repositionSlot(final AppEngSlot s) {
        if (s.isPlayerSide()) {
            s.yDisplayPosition = s.getY() + this.ySize - 78 - 5;
        } else {
            s.yDisplayPosition = s.getY() + this.ySize - 78 - 3;
        }
    }

    public void setPatternSlot(StorageName name, int slotId, IAEStack<?> aes) {
        this.container.setPatternSlot(name, slotId, aes);
    }
}
