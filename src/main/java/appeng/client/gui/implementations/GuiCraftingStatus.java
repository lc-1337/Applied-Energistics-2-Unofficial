/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

/**
 *
 */
package appeng.client.gui.implementations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.lwjgl.input.Mouse;

import appeng.api.config.Settings;
import appeng.api.config.TerminalStyle;
import appeng.api.storage.ITerminalHost;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.IGuiSub;
import appeng.client.gui.widgets.GuiAeButton;
import appeng.client.gui.widgets.GuiCraftingCPUTable;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.ICraftingCPUTableHolder;
import appeng.container.implementations.ContainerCraftingStatus;
import appeng.core.AEConfig;
import appeng.core.AELog;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiText;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketSwitchGuis;
import appeng.core.sync.packets.PacketValueConfig;

public class GuiCraftingStatus extends GuiCraftingCPU implements ICraftingCPUTableHolder, IGuiSub {

    private final ContainerCraftingStatus status;
    private GuiButton selectCPU;
    private GuiAeButton follow;
    private final GuiCraftingCPUTable cpuTable;

    protected GuiTabButton originalGuiBtn;
    private boolean tallMode;
    private GuiImgButton switchTallMode;
    private List<String> playersFollowingCurrentCraft = new ArrayList<>();

    public GuiCraftingStatus(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        super(new ContainerCraftingStatus(inventoryPlayer, te));

        this.status = (ContainerCraftingStatus) this.inventorySlots;
        this.status.setGuiLink(this);
        this.tallMode = AEConfig.instance.getConfigManager().getSetting(Settings.TERMINAL_STYLE) == TerminalStyle.TALL;
        recalculateScreenSize();

        cpuTable = new GuiCraftingCPUTable(this, this.status.getCPUTable(), c -> false);
    }

    @Override
    public GuiCraftingCPUTable getCPUTable() {
        return cpuTable;
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);

        final boolean leftClick = Mouse.isButtonDown(0);
        final boolean rightClick = Mouse.isButtonDown(1);

        if (btn == this.selectCPU) {
            cpuTable.cycleCPU(rightClick);
        } else if (btn == this.follow) {
            try {
                NetworkHandler.instance.sendToServer(
                        new PacketValueConfig("TileCrafting.Follow", this.mc.thePlayer.getCommandSenderName()));
            } catch (final IOException e) {
                AELog.debug(e);
            }
        } else if (btn == this.originalGuiBtn) {
            NetworkHandler.instance.sendToServer(new PacketSwitchGuis());
        } else if (btn == this.switchTallMode) {
            tallMode = !tallMode;
            AEConfig.instance.getConfigManager()
                    .putSetting(Settings.TERMINAL_STYLE, tallMode ? TerminalStyle.TALL : TerminalStyle.SMALL);
            switchTallMode.set(tallMode ? TerminalStyle.TALL : TerminalStyle.SMALL);
            recalculateScreenSize();
            this.setWorldAndResolution(mc, width, height);
        } else if (btn == this.toggleHideStored) {
            this.setScrollBar();
        }
    }

    @Override
    public void initGui() {
        recalculateScreenSize();
        super.initGui();
        this.setScrollBar();

        if (status.getPrimaryGuiIcon() != null) initPrimaryGuiButton();

        this.selectCPU = new GuiButton(
                0,
                this.guiLeft + 8,
                this.guiTop + this.ySize - 25,
                50,
                20,
                GuiText.CraftingCPU.getLocal() + ": " + GuiText.NoCraftingCPUs);
        this.buttonList.add(this.selectCPU);

        this.follow = new GuiAeButton(
                1,
                this.guiLeft + 111,
                this.guiTop + this.ySize - 25,
                50,
                20,
                GuiText.ToFollow.getLocal(),
                ButtonToolTips.ToFollow.getLocal());
        this.buttonList.add(this.follow);

        this.switchTallMode = new GuiImgButton(
                this.guiLeft - 18,
                this.guiTop + this.ySize - 18,
                Settings.TERMINAL_STYLE,
                tallMode ? TerminalStyle.TALL : TerminalStyle.SMALL);
        this.buttonList.add(switchTallMode);
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float btn) {
        this.follow.enabled = !this.visual.isEmpty();
        this.cpuTable.drawScreen();
        this.updateCPUButtonText();
        this.updateFollowButtonText();
        super.drawScreen(mouseX, mouseY, btn);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        this.cpuTable.drawFG(offsetX, offsetY, mouseX, mouseY, guiLeft, guiTop);
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY) {
        this.bindTexture("guis/craftingcpu.png");
        if (tallMode) {
            this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, TEXTURE_BELOW_TOP_ROW_Y);
            int y = TEXTURE_BELOW_TOP_ROW_Y;
            // first and last row are pre-baked
            for (int row = 1; row < rows - 1; row++) {
                this.drawTexturedModalRect(
                        offsetX,
                        offsetY + y,
                        0,
                        TEXTURE_BELOW_TOP_ROW_Y,
                        this.xSize,
                        SECTION_HEIGHT);
                y += SECTION_HEIGHT;
            }
            this.drawTexturedModalRect(
                    offsetX,
                    offsetY + y,
                    0,
                    GUI_HEIGHT - TEXTURE_ABOVE_BOTTOM_ROW_Y,
                    this.xSize,
                    TEXTURE_ABOVE_BOTTOM_ROW_Y);
        } else {
            this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
        }
        this.cpuTable.drawBG(offsetX, offsetY);
        drawSearch();
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        super.mouseClicked(xCoord, yCoord, btn);
        cpuTable.mouseClicked(xCoord - guiLeft, yCoord - guiTop, btn);
    }

    @Override
    protected void mouseClickMove(int x, int y, int c, long d) {
        super.mouseClickMove(x, y, c, d);
        cpuTable.mouseClickMove(x - guiLeft, y - guiTop);
    }

    @Override
    public void handleMouseInput() {
        if (cpuTable.handleMouseInput(guiLeft, guiTop)) {
            return;
        }
        super.handleMouseInput();
    }

    public boolean hideItemPanelSlot(int x, int y, int w, int h) {
        return cpuTable.hideItemPanelSlot(x - guiLeft, y - guiTop, w, h);
    }

    private void updateCPUButtonText() {
        String btnTextText = GuiText.NoCraftingJobs.getLocal();

        final int selectedSerial = this.cpuTable.getContainer().selectedCpuSerial;
        if (selectedSerial >= 0) {
            String selectedCPUName = cpuTable.getSelectedCPUName();
            if (selectedCPUName != null && selectedCPUName.length() > 0) {
                final String name = selectedCPUName.substring(0, Math.min(20, selectedCPUName.length()));
                btnTextText = GuiText.CPUs.getLocal() + ": " + name;
            } else {
                btnTextText = GuiText.CPUs.getLocal() + ": #" + selectedSerial;
            }
        }

        if (this.status.getCPUs().isEmpty()) {
            btnTextText = GuiText.NoCraftingJobs.getLocal();
        }

        this.selectCPU.displayString = btnTextText;
    }

    private void updateFollowButtonText() {
        boolean isFollow = this.playersFollowingCurrentCraft.contains(this.mc.thePlayer.getCommandSenderName());

        this.follow.displayString = isFollow ? GuiText.ToUnfollow.getLocal() : GuiText.ToFollow.getLocal();
        this.follow
                .setTootipString(isFollow ? ButtonToolTips.ToUnfollow.getLocal() : ButtonToolTips.ToFollow.getLocal());
    }

    @Override
    protected String getGuiDisplayName(final String in) {
        return in; // the cup name is on the button
    }

    protected void recalculateScreenSize() {
        final int maxAvailableHeight = height - 64;
        this.xSize = GUI_WIDTH;
        if (tallMode) {
            this.rows = (maxAvailableHeight - (SCROLLBAR_TOP + SECTION_HEIGHT)) / SECTION_HEIGHT;
            this.ySize = SCROLLBAR_TOP + SECTION_HEIGHT + 5 + this.rows * SECTION_HEIGHT;
        } else {
            this.rows = DISPLAYED_ROWS;
            this.ySize = GUI_HEIGHT;
        }
        GuiCraftingCPUTable.CPU_TABLE_SLOTS = this.rows;
        GuiCraftingCPUTable.CPU_TABLE_HEIGHT = this.rows * GuiCraftingCPUTable.CPU_TABLE_SLOT_HEIGHT + 27;
    }

    private void setScrollBar() {
        int size;
        if (this.hideStored) {
            size = this.visualHiddenStored.size();
        } else {
            size = this.visual.size();
        }

        this.getScrollBar().setTop(SCROLLBAR_TOP).setLeft(SCROLLBAR_LEFT).setHeight(ySize - 47);
        this.getScrollBar().setRange(0, (size + 2) / 3 - this.rows, 1);
    }

    @Override
    public void postUpdate(List<IAEStack<?>> list, byte ref) {
        super.postUpdate(list, ref);
        setScrollBar();
    }

    public void postUpdate(final NBTTagCompound playerNameListNBT) {
        this.playersFollowingCurrentCraft.clear();
        NBTTagList tagList = (NBTTagList) playerNameListNBT.getTag("playNameList");
        for (int index = 0; index < tagList.tagCount(); index++) {
            this.playersFollowingCurrentCraft.add(tagList.getStringTagAt(index));
        }
    }

    public void initPrimaryGuiButton() {
        this.buttonList.add(
                this.originalGuiBtn = new GuiTabButton(
                        this.guiLeft + this.xSize - 25,
                        this.guiTop - 4,
                        status.getPrimaryGuiIcon(),
                        status.getPrimaryGuiIcon().getDisplayName(),
                        itemRender));
        this.originalGuiBtn.setHideEdge(13);
    }
}
