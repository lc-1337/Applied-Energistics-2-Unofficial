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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import appeng.api.AEApi;
import appeng.api.config.ActionItems;
import appeng.api.config.Settings;
import appeng.api.config.StringOrder;
import appeng.api.config.TerminalStyle;
import appeng.api.config.YesNo;
import appeng.api.util.DimensionalCoord;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.IGuiTooltipHandler;
import appeng.client.gui.IInterfaceTerminalPostUpdate;
import appeng.client.gui.widgets.GuiImgButton;
import appeng.client.gui.widgets.GuiScrollbar;
import appeng.client.gui.widgets.IDropToFillTextField;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.client.render.BlockPosHighlighter;
import appeng.container.implementations.ContainerInterfaceTerminal;
import appeng.container.slot.AppEngSlot;
import appeng.core.AEConfig;
import appeng.core.AppEng;
import appeng.core.CommonHelper;
import appeng.core.localization.ButtonToolTips;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.localization.PlayerMessages;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate;
import appeng.core.sync.packets.PacketInterfaceTerminalUpdate.PacketEntry;
import appeng.core.sync.packets.PacketInventoryAction;
import appeng.helpers.InventoryAction;
import appeng.helpers.PatternHelper;
import appeng.integration.IntegrationRegistry;
import appeng.integration.IntegrationType;
import appeng.integration.modules.NEI;
import appeng.items.misc.ItemEncodedPattern;
import appeng.parts.reporting.PartInterfaceTerminal;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.Loader;

/**
 * Interface Terminal GUI <br/>
 * Most of the interface terminal has been rewritten. You may ask, why are you not using SLOTS? And I asked myself that
 * too. I was about to go and rewrite it again until I realized that the interface terminal must support adding/removing
 * entries dynamically. And finding the slots, removing them from the slot list - not exactly a huge fan, especially
 * considering we are doing this first, and foremost, for performance gains. Also, the whole Z-level thing is very
 * frustrating for me. Basically, I've gone rogue and taken the Thaumic Energistics route. <br/>
 * The previous implementation did not use static slots either - instead, in generated new slots dynamically every
 * render tick or when slots were clicked. Why do that, when you can write 500 extra lines of code to reimplement a
 * version of slots specific to the interface terminal?
 *
 * @author firenoo
 */
public class GuiInterfaceTerminal extends AEBaseGui
        implements IDropToFillTextField, IGuiTooltipHandler, IInterfaceTerminalPostUpdate {

    public static final int HEADER_HEIGHT = 52;
    public static final int INV_HEIGHT = 98;
    public static final int VIEW_WIDTH = 174;
    public static final int VIEW_LEFT = 10;
    protected static final ResourceLocation BACKGROUND = new ResourceLocation(
            AppEng.MOD_ID,
            "textures/guis/newinterfaceterminal.png");

    private final InterfaceTerminalList masterList = new InterfaceTerminalList(
            ((StringOrder) AEConfig.instance.settings
                    .getSetting(Settings.INTERFACE_TERMINAL_SECTION_ORDER)).comparator);
    private final MEGuiTextField searchFieldOutputs;
    private final MEGuiTextField searchFieldInputs;
    private final MEGuiTextField searchFieldNames;
    private final GuiImgButton guiButtonHideFull;
    private final GuiImgButton guiButtonAssemblersOnly;
    private final GuiImgButton guiButtonBrokenRecipes;
    private final GuiImgButton guiButtonUseSubstitute;
    protected final GuiImgButton terminalStyleBox;
    private final GuiImgButton searchStringSave;
    private final GuiImgButton guiButtonSectionOrder;
    private boolean onlyMolecularAssemblers = false;
    private boolean onlyBrokenRecipes = false;
    private boolean onlySubstitute = false;
    private boolean online;
    /** The height of the viewport. */
    private int viewHeight;
    private final List<String> extraOptionsText;
    private ItemStack tooltipStack;
    private final boolean neiPresent;
    protected static String searchFieldInputsText = "";
    protected static String searchFieldOutputsText = "";
    protected static String searchFieldNamesText = "";

    protected int offsetY;

    /*
     * Z-level Map (FLOATS) 0.0 - BACKGROUND 1.0 - ItemStacks 2.0 - Slot color overlays 20.0 - ItemStack overlays 21.0 -
     * Slot mouse hover overlay 200.0 - Tooltips
     */
    private static final float ITEM_STACK_Z = 100.0f;
    private static final float SLOT_Z = 0.5f;
    private static final float ITEM_STACK_OVERLAY_Z = 200.0f;
    private static final float SLOT_HOVER_Z = 310.0f;
    private static final float TOOLTIP_Z = 410.0f;
    private static final float STEP_Z = 10.0f;
    private static final float MAGIC_RENDER_ITEM_Z = 50.0f;

    public GuiInterfaceTerminal(final InventoryPlayer inventoryPlayer, final PartInterfaceTerminal te) {
        this(new ContainerInterfaceTerminal(inventoryPlayer, te));
    }

    public GuiInterfaceTerminal(final Container cont) {
        super(cont);

        this.setScrollBar(new GuiScrollbar());
        this.xSize = 208;
        this.ySize = 255;
        this.neiPresent = Loader.isModLoaded("NotEnoughItems");

        searchFieldInputs = new MEGuiTextField(86, 12, ButtonToolTips.SearchFieldInputs.getLocal()) {

            @Override
            public void onTextChange(final String oldText) {
                masterList.markDirty();
            }
        };

        searchFieldOutputs = new MEGuiTextField(86, 12, ButtonToolTips.SearchFieldOutputs.getLocal()) {

            @Override
            public void onTextChange(final String oldText) {
                masterList.markDirty();
            }
        };

        searchFieldNames = new MEGuiTextField(71, 12, ButtonToolTips.SearchFieldNames.getLocal()) {

            @Override
            public void onTextChange(final String oldText) {
                masterList.markDirty();
            }
        };
        searchFieldNames.setFocused(true);

        searchStringSave = new GuiImgButton(
                0,
                0,
                Settings.SAVE_SEARCH,
                AEConfig.instance.preserveSearchBar ? YesNo.YES : YesNo.NO);
        guiButtonAssemblersOnly = new GuiImgButton(0, 0, Settings.ACTIONS, null);
        guiButtonHideFull = new GuiImgButton(0, 0, Settings.ACTIONS, null);
        guiButtonBrokenRecipes = new GuiImgButton(0, 0, Settings.ACTIONS, null);
        guiButtonUseSubstitute = new GuiImgButton(0, 0, Settings.ACTIONS, null);
        guiButtonSectionOrder = new GuiImgButton(0, 0, Settings.INTERFACE_TERMINAL_SECTION_ORDER, StringOrder.NATURAL);

        terminalStyleBox = new GuiImgButton(0, 0, Settings.TERMINAL_STYLE, null);

        this.extraOptionsText = new ArrayList<>(2);
        extraOptionsText.add(ButtonToolTips.HighlightInterface.getLocal());

        NEI.searchField.putFormatter(this.searchFieldInputs);
        NEI.searchField.putFormatter(this.searchFieldOutputs);
    }

    private void setScrollBar() {
        int maxScroll = this.masterList.getHeight() - this.viewHeight - 1;
        if (maxScroll <= 0) {
            this.getScrollBar().setTop(52).setLeft(189).setHeight(this.viewHeight).setRange(0, 0, 1);
        } else {
            this.getScrollBar().setTop(52).setLeft(189).setHeight(this.viewHeight).setRange(0, maxScroll, 12);
        }
    }

    @Override
    public void initGui() {
        super.initGui();

        this.buttonList.clear();
        this.viewHeight = calculateViewHeight();
        this.ySize = HEADER_HEIGHT + INV_HEIGHT + this.viewHeight;

        final int unusedSpace = this.height - this.ySize;
        this.guiTop = (int) Math.floor(unusedSpace / (unusedSpace < 0 ? 3.8f : 2.0f));

        searchFieldInputs.x = guiLeft + Math.max(32, VIEW_LEFT);
        searchFieldInputs.y = guiTop + 25;

        searchFieldOutputs.x = guiLeft + Math.max(32, VIEW_LEFT);
        searchFieldOutputs.y = guiTop + 38;

        searchFieldNames.x = guiLeft + Math.max(32, VIEW_LEFT) + 99;
        searchFieldNames.y = guiTop + 38;

        terminalStyleBox.xPosition = guiLeft - 18;
        terminalStyleBox.yPosition = guiTop + 8;

        searchStringSave.xPosition = guiLeft - 18;
        searchStringSave.yPosition = terminalStyleBox.yPosition + 18;

        guiButtonSectionOrder.xPosition = guiLeft - 18;
        guiButtonSectionOrder.yPosition = searchStringSave.yPosition + 18;

        guiButtonBrokenRecipes.xPosition = guiLeft - 18;
        guiButtonBrokenRecipes.yPosition = guiButtonSectionOrder.yPosition + 18;

        guiButtonHideFull.xPosition = guiLeft - 18;
        guiButtonHideFull.yPosition = guiButtonBrokenRecipes.yPosition + 18;

        guiButtonAssemblersOnly.xPosition = guiLeft - 18;
        guiButtonAssemblersOnly.yPosition = guiButtonHideFull.yPosition + 18;

        guiButtonUseSubstitute.xPosition = guiLeft - 18;
        guiButtonUseSubstitute.yPosition = guiButtonAssemblersOnly.yPosition + 18;

        offsetY = guiButtonUseSubstitute.yPosition; // last button pos for ae2fc

        setSearchString();

        this.setScrollBar();
        this.repositionSlots();

        buttonList.add(guiButtonAssemblersOnly);
        buttonList.add(guiButtonHideFull);
        buttonList.add(guiButtonBrokenRecipes);
        buttonList.add(guiButtonSectionOrder);
        buttonList.add(searchStringSave);
        buttonList.add(terminalStyleBox);
        buttonList.add(guiButtonUseSubstitute);
    }

    protected void repositionSlots() {
        for (final Object obj : this.inventorySlots.inventorySlots) {
            if (obj instanceof AppEngSlot slot) {
                slot.yDisplayPosition = this.ySize + slot.getY() - 78 - 7;
            }
        }
    }

    protected int calculateViewHeight() {
        final int maxViewHeight = this.getMaxViewHeight();
        final boolean hasNEI = IntegrationRegistry.INSTANCE.isEnabled(IntegrationType.NEI);
        final int NEIPadding = hasNEI ? 22 /* input */ + 18 /* top panel */ : 0;
        final int availableSpace = this.height - HEADER_HEIGHT - INV_HEIGHT - NEIPadding;

        // screen should use 95% of the space it can, 5% margins
        return Math.min((int) (availableSpace * 0.95), maxViewHeight);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        fontRendererObj.drawString(
                getGuiDisplayName(GuiText.InterfaceTerminal.getLocal()),
                8,
                6,
                GuiColors.InterfaceTerminalTitle.getColor());
        fontRendererObj.drawString(
                GuiText.inventory.getLocal(),
                GuiInterfaceTerminal.VIEW_LEFT + 2,
                this.ySize - 96,
                GuiColors.InterfaceTerminalInventory.getColor());
        if (!neiPresent && tooltipStack != null) {
            renderToolTip(tooltipStack, mouseX, mouseY);
        }
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float btn) {
        guiButtonAssemblersOnly.set(
                onlyMolecularAssemblers ? ActionItems.MOLECULAR_ASSEMBLEERS_ON : ActionItems.MOLECULAR_ASSEMBLEERS_OFF);
        guiButtonHideFull.set(
                AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal
                        ? ActionItems.TOGGLE_SHOW_FULL_INTERFACES_OFF
                        : ActionItems.TOGGLE_SHOW_FULL_INTERFACES_ON);
        guiButtonBrokenRecipes.set(
                onlyBrokenRecipes ? ActionItems.TOGGLE_SHOW_ONLY_INVALID_PATTERN_OFF
                        : ActionItems.TOGGLE_SHOW_ONLY_INVALID_PATTERN_ON);
        guiButtonUseSubstitute.set(
                onlySubstitute ? ActionItems.TOGGLE_SHOW_ONLY_SUBSTITUTE_OFF
                        : ActionItems.TOGGLE_SHOW_ONLY_SUBSTITUTE_ON);
        guiButtonSectionOrder.set(AEConfig.instance.settings.getSetting(Settings.INTERFACE_TERMINAL_SECTION_ORDER));

        terminalStyleBox.set(AEConfig.instance.settings.getSetting(Settings.TERMINAL_STYLE));

        handleTooltip(mouseX, mouseY, searchFieldInputs);
        handleTooltip(mouseX, mouseY, searchFieldOutputs);
        handleTooltip(mouseX, mouseY, searchFieldNames);

        super.drawScreen(mouseX, mouseY, btn);
    }

    @Override
    protected void mouseClicked(final int xCoord, final int yCoord, final int btn) {
        searchFieldInputs.mouseClicked(xCoord, yCoord, btn);
        searchFieldOutputs.mouseClicked(xCoord, yCoord, btn);
        searchFieldNames.mouseClicked(xCoord, yCoord, btn);

        if (masterList.mouseClicked(xCoord - guiLeft - VIEW_LEFT, yCoord - guiTop - HEADER_HEIGHT, btn)) {
            return;
        }
        super.mouseClicked(xCoord, yCoord, btn);
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        if (btn == guiButtonAssemblersOnly) {
            onlyMolecularAssemblers = !onlyMolecularAssemblers;
            masterList.markDirty();
        } else if (btn == guiButtonHideFull) {
            AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal = !AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal;
            masterList.markDirty();
        } else if (btn == guiButtonBrokenRecipes) {
            onlyBrokenRecipes = !onlyBrokenRecipes;
            masterList.markDirty();
        } else if (btn == guiButtonUseSubstitute) {
            onlySubstitute = !onlySubstitute;
            masterList.markDirty();
        } else if (btn instanceof GuiImgButton iBtn) {
            if (iBtn.getSetting() != Settings.ACTIONS) {
                final Enum cv = iBtn.getCurrentValue();
                final boolean backwards = Mouse.isButtonDown(1);
                final Enum next = Platform.rotateEnum(cv, backwards, iBtn.getSetting().getPossibleValues());

                if (btn == this.terminalStyleBox) {
                    AEConfig.instance.settings.putSetting(iBtn.getSetting(), next);
                    initGui();
                } else if (btn == searchStringSave) {
                    AEConfig.instance.preserveSearchBar = next == YesNo.YES;
                } else if (btn == guiButtonSectionOrder) {
                    AEConfig.instance.settings.putSetting(iBtn.getSetting(), next);
                    masterList.changeSectionComparator(((StringOrder) next).comparator);
                    masterList.markDirty();
                }

                iBtn.set(next);
            }
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        searchFieldInputsText = searchFieldInputs.getText();
        searchFieldOutputsText = searchFieldOutputs.getText();
        searchFieldNamesText = searchFieldNames.getText();
    }

    public void setSearchString() {
        boolean setString = AEConfig.instance.preserveSearchBar || isSubGui();
        if (searchFieldInputs.getText().isEmpty() && setString) {
            searchFieldInputs.setText(searchFieldInputsText);
        }
        if (searchFieldOutputs.getText().isEmpty() && setString) {
            searchFieldOutputs.setText(searchFieldOutputsText);
        }
        if (searchFieldNames.getText().isEmpty() && setString) {
            searchFieldNames.setText(searchFieldNamesText);
        }
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        final Tessellator tessellator = Tessellator.instance;
        this.bindTexture(BACKGROUND);
        /* Draws the top part. */
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, xSize, HEADER_HEIGHT);
        /* Draws the middle part. */
        tessellator.startDrawingQuads();
        addTexturedRectToTesselator(
                offsetX,
                offsetY + HEADER_HEIGHT,
                offsetX + xSize,
                offsetY + HEADER_HEIGHT + viewHeight + 1,
                0.0f,
                0.0f,
                (HEADER_HEIGHT + InterfaceSection.TITLE_HEIGHT + 1.0f) / 256.0f,
                this.xSize / 256.0f,
                (HEADER_HEIGHT + 106.0f) / 256.0f);
        tessellator.draw();
        /* Draw the bottom part */
        this.drawTexturedModalRect(offsetX, offsetY + HEADER_HEIGHT + viewHeight, 0, 158, xSize, INV_HEIGHT);
        if (online) {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            /* (0,0) => viewPort's (0,0) */
            GL11.glPushMatrix();
            GL11.glTranslatef(offsetX + VIEW_LEFT, offsetY + HEADER_HEIGHT, 0);
            tooltipStack = null;
            masterList.hoveredEntry = null;
            drawViewport(mouseX - offsetX - VIEW_LEFT, mouseY - offsetY - HEADER_HEIGHT - 1);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
        }
        searchFieldInputs.drawTextBox();
        searchFieldOutputs.drawTextBox();
        searchFieldNames.drawTextBox();
    }

    /**
     * Draws the viewport area
     */
    private void drawViewport(int relMouseX, int relMouseY) {
        /* Viewport Magic */
        final int scroll = this.getScrollBar().getCurrentScroll();
        int viewY = -scroll; // current y in viewport coordinates
        int entryIdx = 0;
        List<InterfaceSection> visibleSections = this.masterList.getVisibleSections();

        final float guiScaleX = (float) mc.displayWidth / width;
        final float guiScaleY = (float) mc.displayHeight / height;
        GL11.glScissor(
                (int) ((guiLeft + VIEW_LEFT) * guiScaleX),
                (int) ((height - (guiTop + HEADER_HEIGHT + viewHeight)) * guiScaleY),
                (int) (VIEW_WIDTH * guiScaleX),
                (int) (this.viewHeight * guiScaleY));
        GL11.glEnable(GL11.GL_SCISSOR_TEST);

        /*
         * Render each section
         */
        while (viewY < this.viewHeight && entryIdx < visibleSections.size()) {
            InterfaceSection section = visibleSections.get(entryIdx);
            int sectionHeight = section.getHeight();

            /* Is it viewable/in the viewport at all? */
            if (viewY + sectionHeight < 0) {
                entryIdx++;
                viewY += sectionHeight;
                section.visible = false;
                continue;
            }

            section.visible = true;
            int advanceY = drawSection(section, viewY, relMouseX, relMouseY);
            viewY += advanceY;
            entryIdx++;
        }
    }

    /**
     * Render the section (if it is visible)
     *
     * @param section   the section to render
     * @param viewY     current y coordinate relative to gui
     * @param relMouseX transformed mouse coords relative to viewport
     * @param relMouseY transformed mouse coords relative to viewport
     * @return the height of the section rendered in viewport coordinates, max of viewHeight.
     */
    private int drawSection(InterfaceSection section, int viewY, int relMouseX, int relMouseY) {
        int title;
        int renderY = 0;
        final int sectionBottom = viewY + section.getHeight() - 1;
        final int fontColor = GuiColors.InterfaceTerminalInventory.getColor();
        /*
         * Render title
         */
        bindTexture(BACKGROUND);
        GL11.glTranslatef(0.0f, 0.0f, ITEM_STACK_OVERLAY_Z + ITEM_STACK_Z + STEP_Z);
        if (sectionBottom > 0 && sectionBottom < InterfaceSection.TITLE_HEIGHT) {
            /* Transition draw */
            title = sectionBottom;
        } else if (viewY < 0) {
            /* Hidden title draw */
            title = 0;
        } else {
            /* Normal title draw */
            title = 0;
        }
        GL11.glTranslatef(0.0f, 0.0f, -(ITEM_STACK_OVERLAY_Z + ITEM_STACK_Z + STEP_Z));
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        Iterator<InterfaceTerminalEntry> visible = section.getVisible();
        while (visible.hasNext()) {
            InterfaceTerminalEntry entry = visible.next();
            if (viewY + renderY + entry.rows * 18 + 1 > 0 && viewY + renderY < viewHeight) {
                renderY += drawEntry(
                        entry,
                        viewY + InterfaceSection.TITLE_HEIGHT + renderY,
                        title,
                        relMouseX,
                        relMouseY);
            } else {
                entry.dispY = -9999;
                entry.optionsButton.yPosition = -1;
                renderY += entry.rows * 18 + 1;
            }
        }
        /*
         * Render title
         */
        bindTexture(BACKGROUND);
        GL11.glTranslatef(0.0f, 0.0f, ITEM_STACK_OVERLAY_Z + ITEM_STACK_Z + STEP_Z);
        if (sectionBottom > 0 && sectionBottom < InterfaceSection.TITLE_HEIGHT) {
            /* Transition draw */
            drawTexturedModalRect(
                    0,
                    0,
                    VIEW_LEFT,
                    HEADER_HEIGHT + InterfaceSection.TITLE_HEIGHT - sectionBottom,
                    VIEW_WIDTH,
                    sectionBottom);
            fontRendererObj.drawString(section.name, 2, sectionBottom - InterfaceSection.TITLE_HEIGHT + 2, fontColor);
        } else if (viewY < 0) {
            /* Hidden title draw */
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glTranslatef(0.0f, 0.0f, 100f);
            drawTexturedModalRect(0, 0, VIEW_LEFT, HEADER_HEIGHT, VIEW_WIDTH, InterfaceSection.TITLE_HEIGHT);
            fontRendererObj.drawString(section.name, 2, 2, fontColor);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        } else {
            /* Normal title draw */
            drawTexturedModalRect(0, viewY, VIEW_LEFT, HEADER_HEIGHT, VIEW_WIDTH, InterfaceSection.TITLE_HEIGHT);
            fontRendererObj.drawString(section.name, 2, viewY + 2, fontColor);
        }
        GL11.glTranslatef(0.0f, 0.0f, -(ITEM_STACK_OVERLAY_Z + ITEM_STACK_Z + STEP_Z));

        return InterfaceSection.TITLE_HEIGHT + renderY;
    }

    /**
     * Draws the entry. In practice, it just draws the slots + items.
     *
     * @param viewY the gui coordinate z
     */
    private int drawEntry(InterfaceTerminalEntry entry, int viewY, int titleBottom, int relMouseX, int relMouseY) {
        bindTexture(BACKGROUND);
        final Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        int relY = 0;
        final int slotLeftMargin = (VIEW_WIDTH - entry.rowSize * 18);

        entry.dispY = viewY;
        /* PASS 1: BG */
        for (int row = 0; row < entry.rows; ++row) {
            final int rowYTop = row * 18;
            final int rowYBot = rowYTop + 18;

            relY += 18;
            /* Is the slot row in view? */
            if (viewY + rowYBot <= titleBottom) {
                continue;
            }
            for (int col = 0; col < entry.rowSize; ++col) {
                addTexturedRectToTesselator(
                        col * 18 + slotLeftMargin,
                        viewY + rowYTop,
                        18 * col + 18 + slotLeftMargin,
                        viewY + rowYBot,
                        0,
                        21 / 256f,
                        173 / 256f,
                        (21 + 18) / 256f,
                        (173 + 18) / 256f);
            }
        }
        tessellator.draw();
        /* Draw button */
        if (viewY + entry.optionsButton.height > 0 && viewY < viewHeight) {
            entry.optionsButton.yPosition = viewY + 5;
            entry.optionsButton.drawButton(mc, relMouseX, relMouseY);
            if (entry.optionsButton.getMouseIn()
                    && relMouseY >= Math.max(InterfaceSection.TITLE_HEIGHT, entry.optionsButton.yPosition)) {
                // draw a tooltip
                GL11.glTranslatef(0f, 0f, TOOLTIP_Z);
                GL11.glDisable(GL11.GL_SCISSOR_TEST);
                drawHoveringText(extraOptionsText, relMouseX, relMouseY);
                GL11.glTranslatef(0f, 0f, -TOOLTIP_Z);
                GL11.glEnable(GL11.GL_SCISSOR_TEST);
            }
        } else {
            entry.optionsButton.yPosition = -1;
        }
        /* PASS 2: Items */
        for (int row = 0; row < entry.rows; ++row) {
            final int rowYTop = row * 18;
            final int rowYBot = rowYTop + 18;
            /* Is the slot row in view? */
            if (viewY + rowYBot <= titleBottom) {
                continue;
            }
            AppEngInternalInventory inv = entry.getInventory();

            for (int col = 0; col < entry.rowSize; ++col) {
                final int colLeft = col * 18 + slotLeftMargin + 1;
                final int colRight = colLeft + 18 + 1;
                final int slotIdx = row * entry.rowSize + col;
                ItemStack stack = inv.getStackInSlot(slotIdx);

                boolean tooltip = relMouseX > colLeft - 1 && relMouseX < colRight - 1
                        && relMouseY >= Math.max(viewY + rowYTop, InterfaceSection.TITLE_HEIGHT)
                        && relMouseY < Math.min(viewY + rowYBot, viewHeight);
                if (stack != null) {
                    final ItemEncodedPattern iep = (ItemEncodedPattern) stack.getItem();
                    final ItemStack toRender = iep.getOutput(stack);

                    GL11.glPushMatrix();
                    GL11.glTranslatef(colLeft, viewY + rowYTop + 1, ITEM_STACK_Z);
                    GL11.glEnable(GL12.GL_RESCALE_NORMAL);
                    RenderHelper.enableGUIStandardItemLighting();
                    translatedRenderItem.zLevel = ITEM_STACK_Z - MAGIC_RENDER_ITEM_Z;
                    translatedRenderItem
                            .renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), toRender, 0, 0);
                    GL11.glTranslatef(0.0f, 0.0f, ITEM_STACK_OVERLAY_Z);
                    aeRenderItem.setAeStack(AEItemStack.create(toRender));
                    aeRenderItem.renderItemOverlayIntoGUI(fontRendererObj, mc.getTextureManager(), toRender, 0, 0);
                    aeRenderItem.zLevel = 0.0f;
                    RenderHelper.disableStandardItemLighting();
                    if (!tooltip) {
                        if (entry.slotIsBroken(slotIdx)) {
                            GL11.glTranslatef(0.0f, 0.0f, SLOT_Z - ITEM_STACK_OVERLAY_Z);
                            drawRect(0, 0, 16, 16, GuiColors.ItemSlotOverlayInvalid.getColor());
                        } else if (entry.filteredRecipes[slotIdx]) {
                            GL11.glTranslatef(0.0f, 0.0f, ITEM_STACK_OVERLAY_Z);
                            drawRect(0, 0, 16, 16, GuiColors.ItemSlotOverlayUnpowered.getColor());
                        }
                    } else {
                        tooltipStack = stack;
                    }
                    GL11.glPopMatrix();
                } else if (entry.filteredRecipes[slotIdx]) {
                    GL11.glPushMatrix();
                    GL11.glTranslatef(colLeft, viewY + rowYTop + 1, ITEM_STACK_OVERLAY_Z);
                    drawRect(0, 0, 16, 16, GuiColors.ItemSlotOverlayUnpowered.getColor());
                    GL11.glPopMatrix();
                }
                if (tooltip) {
                    // overlay highlight
                    GL11.glDisable(GL11.GL_LIGHTING);
                    GL11.glTranslatef(0.0f, 0.0f, SLOT_HOVER_Z);
                    drawRect(colLeft, viewY + 1 + rowYTop, -2 + colRight, viewY - 1 + rowYBot, 0x77FFFFFF);
                    GL11.glTranslatef(0.0f, 0.0f, -SLOT_HOVER_Z);
                    masterList.hoveredEntry = entry;
                    entry.hoveredSlotIdx = slotIdx;
                }
                GL11.glDisable(GL11.GL_LIGHTING);
            }
        }
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        return relY + 1;
    }

    @Override
    public List<String> handleItemTooltip(ItemStack stack, int mouseX, int mouseY, List<String> currentToolTip) {
        return currentToolTip;
    }

    @Override
    public ItemStack getHoveredStack() {
        return tooltipStack;
    }

    /**
     * A copy of super method, but modified to allow for depth testing.
     */
    @Override
    public void drawHoveringText(List<String> textLines, int x, int y, FontRenderer font) {
        if (!textLines.isEmpty()) {
            GL11.glDisable(GL12.GL_RESCALE_NORMAL);
            RenderHelper.disableStandardItemLighting();
            int maxStrWidth = 0;

            // is this more efficient than doing 1 pass, then doing a translate before drawing the text?
            for (String s : textLines) {
                int width = font.getStringWidth(s);

                if (width > maxStrWidth) {
                    maxStrWidth = width;
                }
            }

            // top left corner
            int curX = x + 12;
            int curY = y - 12;
            int totalHeight = 8;

            if (textLines.size() > 1) {
                totalHeight += 2 + (textLines.size() - 1) * 10;
            }

            /* String is too long? Display on the left side */
            if (curX + maxStrWidth > this.width) {
                curX -= 28 + maxStrWidth;
            }

            /* String is too tall? move it up */
            if (curY + totalHeight + 6 > this.height) {
                curY = this.height - totalHeight - 6;
            }

            int borderColor = -267386864;
            // drawing the border...
            this.drawGradientRect(curX - 3, curY - 4, curX + maxStrWidth + 3, curY - 3, borderColor, borderColor);
            this.drawGradientRect(
                    curX - 3,
                    curY + totalHeight + 3,
                    curX + maxStrWidth + 3,
                    curY + totalHeight + 4,
                    borderColor,
                    borderColor);
            this.drawGradientRect(
                    curX - 3,
                    curY - 3,
                    curX + maxStrWidth + 3,
                    curY + totalHeight + 3,
                    borderColor,
                    borderColor);
            this.drawGradientRect(curX - 4, curY - 3, curX - 3, curY + totalHeight + 3, borderColor, borderColor);
            this.drawGradientRect(
                    curX + maxStrWidth + 3,
                    curY - 3,
                    curX + maxStrWidth + 4,
                    curY + totalHeight + 3,
                    borderColor,
                    borderColor);
            int color1 = 1347420415;
            int color2 = (color1 & 16711422) >> 1 | color1 & -16777216;
            this.drawGradientRect(curX - 3, curY - 3 + 1, curX - 3 + 1, curY + totalHeight + 3 - 1, color1, color2);
            this.drawGradientRect(
                    curX + maxStrWidth + 2,
                    curY - 3 + 1,
                    curX + maxStrWidth + 3,
                    curY + totalHeight + 3 - 1,
                    color1,
                    color2);
            this.drawGradientRect(curX - 3, curY - 3, curX + maxStrWidth + 3, curY - 3 + 1, color1, color1);
            this.drawGradientRect(
                    curX - 3,
                    curY + totalHeight + 2,
                    curX + maxStrWidth + 3,
                    curY + totalHeight + 3,
                    color2,
                    color2);

            for (int i = 0; i < textLines.size(); ++i) {
                String line = textLines.get(i);
                font.drawStringWithShadow(line, curX, curY, -1);

                if (i == 0) {
                    // gap between name and lore text
                    curY += 2;
                }

                curY += 10;
            }

            RenderHelper.enableGUIStandardItemLighting();
            GL11.glEnable(GL12.GL_RESCALE_NORMAL);
        }
    }

    @Override
    protected void keyTyped(final char character, final int key) {
        if (!checkHotbarKeys(key)) {
            if (character == ' ') {
                if ((searchFieldInputs.getText().isEmpty() && searchFieldInputs.isFocused())
                        || (searchFieldOutputs.getText().isEmpty() && searchFieldOutputs.isFocused())
                        || (searchFieldNames.getText().isEmpty() && searchFieldNames.isFocused()))
                    return;
            } else if (character == '\t' && handleTab()) {
                return;
            }
            if (searchFieldInputs.textboxKeyTyped(character, key) || searchFieldOutputs.textboxKeyTyped(character, key)
                    || searchFieldNames.textboxKeyTyped(character, key)) {
                return;
            }
            super.keyTyped(character, key);
        }
    }

    @Override
    protected boolean mouseWheelEvent(int mouseX, int mouseY, int wheel) {
        boolean isMouseInViewport = isMouseInViewport(mouseX, mouseY);
        GuiScrollbar scrollbar = getScrollBar();
        if (isMouseInViewport && isCtrlKeyDown()) {
            if (wheel < 0) {
                scrollbar.setCurrentScroll(masterList.getHeight());
            } else {
                getScrollBar().setCurrentScroll(0);
            }
            return true;
        } else if (isMouseInViewport && isShiftKeyDown()) {
            // advance to the next section
            return masterList.scrollNextSection(wheel > 0);
        } else {
            return super.mouseWheelEvent(mouseX, mouseY, wheel);
        }
    }

    private boolean isMouseInViewport(int mouseX, int mouseY) {
        return mouseX > guiLeft + VIEW_LEFT && mouseX < guiLeft + VIEW_LEFT + VIEW_WIDTH
                && mouseY > guiTop + HEADER_HEIGHT
                && mouseY < guiTop + HEADER_HEIGHT + viewHeight;
    }

    private boolean handleTab() {
        if (searchFieldInputs.isFocused()) {
            searchFieldInputs.setFocused(false);
            if (isShiftKeyDown()) searchFieldNames.setFocused(true);
            else searchFieldOutputs.setFocused(true);
            return true;
        } else if (searchFieldOutputs.isFocused()) {
            searchFieldOutputs.setFocused(false);
            if (isShiftKeyDown()) searchFieldInputs.setFocused(true);
            else searchFieldNames.setFocused(true);
            return true;
        } else if (searchFieldNames.isFocused()) {
            searchFieldNames.setFocused(false);
            if (isShiftKeyDown()) searchFieldOutputs.setFocused(true);
            else searchFieldInputs.setFocused(true);
            return true;
        }
        return false;
    }

    @Override
    public void postUpdate(List<PacketEntry> updates, int statusFlags) {
        if ((statusFlags & PacketInterfaceTerminalUpdate.CLEAR_ALL_BIT)
                == PacketInterfaceTerminalUpdate.CLEAR_ALL_BIT) {
            /* Should clear all client entries. */
            this.masterList.list.clear();
        }
        /* Should indicate disconnected, so the terminal turns dark. */
        this.online = (statusFlags & PacketInterfaceTerminalUpdate.DISCONNECT_BIT)
                != PacketInterfaceTerminalUpdate.DISCONNECT_BIT;

        for (PacketInterfaceTerminalUpdate.PacketEntry cmd : updates) {
            parsePacketCmd(cmd);
        }
        this.masterList.markDirty();
    }

    private void parsePacketCmd(PacketInterfaceTerminalUpdate.PacketEntry cmd) {
        long id = cmd.entryId;
        if (cmd instanceof PacketInterfaceTerminalUpdate.PacketAdd addCmd) {
            InterfaceTerminalEntry entry = new InterfaceTerminalEntry(
                    id,
                    addCmd.name,
                    addCmd.rows,
                    addCmd.rowSize,
                    addCmd.online,
                    addCmd.p2pOutput).setLocation(addCmd.x, addCmd.y, addCmd.z, addCmd.dim)
                            .setIcons(addCmd.selfRep, addCmd.dispRep).setItems(addCmd.items);
            masterList.addEntry(entry);
        } else if (cmd instanceof PacketInterfaceTerminalUpdate.PacketRemove) {
            masterList.removeEntry(id);
        } else if (cmd instanceof PacketInterfaceTerminalUpdate.PacketOverwrite owCmd) {
            InterfaceTerminalEntry entry = masterList.list.get(id);

            if (entry == null) {
                return;
            }

            if (owCmd.onlineValid) {
                entry.online = owCmd.online;
            }

            if (owCmd.itemsValid) {
                if (owCmd.allItemUpdate) {
                    entry.fullItemUpdate(owCmd.items, owCmd.items.tagCount());
                } else {
                    entry.partialItemUpdate(owCmd.items, owCmd.validIndices);
                }
            }
            masterList.isDirty = true;
        } else if (cmd instanceof PacketInterfaceTerminalUpdate.PacketRename renameCmd) {
            InterfaceTerminalEntry entry = masterList.list.get(id);

            if (entry != null) {
                if (StatCollector.canTranslate(renameCmd.newName)) {
                    entry.dispName = StatCollector.translateToLocal(renameCmd.newName);
                } else {
                    entry.dispName = StatCollector.translateToFallback(renameCmd.newName);
                }
            }
            masterList.isDirty = true;
        }
    }

    private static boolean itemStackMatchesSearchTerm(final ItemStack itemStack, final String searchTerm, boolean in) {
        if (itemStack == null) {
            return false;
        }

        final NBTTagCompound encodedValue = itemStack.getTagCompound();

        if (encodedValue == null) {
            return false;
        }

        final NBTTagList tags = encodedValue.getTagList(in ? "in" : "out", NBT.TAG_COMPOUND);
        final boolean containsInvalidDisplayName = GuiText.UnknownItem.getLocal().toLowerCase().contains(searchTerm);
        Predicate<ItemStack> itemFilter;

        if (NEI.searchField.existsSearchField()) {
            itemFilter = NEI.searchField.getFilter(searchTerm);
        } else {
            itemFilter = is -> Platform.getItemDisplayName(AEApi.instance().storage().createItemStack(is)).toLowerCase()
                    .contains(searchTerm);
        }

        for (int i = 0; i < tags.tagCount(); i++) {
            final NBTTagCompound tag = tags.getCompoundTagAt(i);
            final ItemStack parsedItemStack = ItemStack.loadItemStackFromNBT(tag);

            if (parsedItemStack != null) {
                if (itemFilter.test(parsedItemStack)) {
                    return true;
                }
            } else if (containsInvalidDisplayName && !tag.hasNoTags()) {
                return true;
            }
        }

        return false;
    }

    private static boolean interfaceSectionMatchesSearchTerm(final InterfaceSection section, final String searchTerm) {
        if (searchTerm.isEmpty()) return true;

        String sectionName = section.name.toLowerCase();

        if (searchTerm.length() >= 2 && searchTerm.startsWith("\"") && searchTerm.endsWith("\"")) {
            return sectionName.contains(searchTerm.substring(1, searchTerm.length() - 1).toLowerCase());
        } else {
            String[] terms = searchTerm.toLowerCase().split("\s+");

            for (int i = 0; i < terms.length; i++) {
                if (!sectionName.contains(terms[i])) {
                    return false;
                }
            }
            return true;
        }
    }

    private boolean recipeIsBroken(final ItemStack itemStack) {
        if (itemStack == null) {
            return false;
        }

        final NBTTagCompound encodedValue = itemStack.getTagCompound();
        if (encodedValue == null) {
            return true;
        }

        final World w = CommonHelper.proxy.getWorld();
        if (w == null) {
            return false;
        }

        try {
            new PatternHelper(itemStack, w);
            return false;
        } catch (final Throwable t) {
            return true;
        }
    }

    private boolean isUseSubstitute(final ItemStack is) {
        if (is == null) {
            return false;
        }

        final NBTTagCompound encodedValue = is.getTagCompound();
        if (encodedValue == null) {
            return false;
        }

        return encodedValue.getBoolean("substitute") || encodedValue.getBoolean("beSubstitute");
    }

    private int getMaxViewHeight() {
        return AEConfig.instance.getConfigManager().getSetting(Settings.TERMINAL_STYLE) == TerminalStyle.SMALL
                ? AEConfig.instance.InterfaceTerminalSmallSize * 18
                : Integer.MAX_VALUE;
    }

    public boolean isOverTextField(final int mousex, final int mousey) {
        return searchFieldInputs.isMouseIn(mousex, mousey) || searchFieldOutputs.isMouseIn(mousex, mousey)
                || searchFieldNames.isMouseIn(mousex, mousey);
    }

    public void setTextFieldValue(final String displayName, final int mousex, final int mousey, final ItemStack stack) {
        if (searchFieldInputs.isMouseIn(mousex, mousey)) {
            searchFieldInputs.setText(NEI.searchField.getEscapedSearchText(displayName));
        } else if (searchFieldOutputs.isMouseIn(mousex, mousey)) {
            searchFieldOutputs.setText(NEI.searchField.getEscapedSearchText(displayName));
        } else if (searchFieldNames.isMouseIn(mousex, mousey)) {
            searchFieldNames.setText(displayName);
        }
    }

    /**
     * Tracks the list of entries.
     */
    private class InterfaceTerminalList {

        private final Map<Long, InterfaceTerminalEntry> list = new HashMap<>();
        private Map<String, InterfaceSection> sections;
        private final List<InterfaceSection> visibleSections = new ArrayList<>();
        private boolean isDirty;
        private int height;
        private InterfaceTerminalEntry hoveredEntry;

        InterfaceTerminalList(Comparator<String> comparator) {
            this.sections = comparator == null ? new TreeMap<>() : new TreeMap<>(comparator);
            this.isDirty = true;
        }

        void changeSectionComparator(Comparator<String> comparator) {
            if ((this.sections instanceof TreeMap t) && !Objects.equals(comparator, t.comparator())) {
                TreeMap<String, InterfaceSection> map = comparator == null ? new TreeMap<>()
                        : new TreeMap<>(comparator);
                map.putAll(this.sections);
                this.sections = map;
            }
        }

        /**
         * Performs a full update.
         */
        private void update() {
            height = 0;
            visibleSections.clear();

            for (InterfaceSection section : sections.values()) {
                String query = GuiInterfaceTerminal.this.searchFieldNames.getText();
                if (!interfaceSectionMatchesSearchTerm(section, query)) {
                    continue;
                }

                section.isDirty = true;
                if (section.getVisible().hasNext()) {
                    height += section.getHeight();
                    visibleSections.add(section);
                }
            }
            isDirty = false;
        }

        public void markDirty() {
            this.isDirty = true;
            setScrollBar();
        }

        public int getHeight() {
            if (isDirty) {
                update();
            }
            return height;
        }

        /**
         * Jump between sections.
         */
        private boolean scrollNextSection(boolean up) {
            GuiScrollbar scrollbar = getScrollBar();
            int viewY = scrollbar.getCurrentScroll();
            var sections = getVisibleSections();
            boolean result = false;

            if (up) {
                int y = masterList.getHeight();
                int i = sections.size() - 1;

                while (y > 0 && i >= 0) {
                    y -= sections.get(i).getHeight();
                    i -= 1;
                    if (y < viewY) {
                        result = true;
                        scrollbar.setCurrentScroll(y);
                        break;
                    }
                }
            } else {
                int y = 0;

                for (InterfaceSection section : sections) {
                    if (y > viewY) {
                        result = true;
                        scrollbar.setCurrentScroll(y);
                        break;
                    }
                    y += section.getHeight();
                }
            }
            return result;
        }

        public void addEntry(InterfaceTerminalEntry entry) {
            InterfaceSection section = sections.get(entry.dispName);

            if (section == null) {
                section = new InterfaceSection(entry.dispName);
                sections.put(entry.dispName, section);
            }
            section.addEntry(entry);
            list.put(entry.id, entry);
            isDirty = true;
        }

        public void removeEntry(long id) {
            InterfaceTerminalEntry entry = list.remove(id);

            if (entry != null) {
                entry.section.removeEntry(entry);
            }
        }

        public List<InterfaceSection> getVisibleSections() {
            if (isDirty) {
                update();
            }
            return visibleSections;
        }

        /**
         * Mouse button click.
         *
         * @param relMouseX viewport coords mouse X
         * @param relMouseY viewport coords mouse Y
         * @param btn       button code
         */
        public boolean mouseClicked(int relMouseX, int relMouseY, int btn) {
            if (relMouseX < 0 || relMouseX >= VIEW_WIDTH || relMouseY < 0 || relMouseY >= viewHeight) {
                return false;
            }
            for (InterfaceSection section : getVisibleSections()) {
                if (section.mouseClicked(relMouseX, relMouseY, btn)) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * A section holds all the interface entries with the same name.
     */
    private class InterfaceSection {

        public static final int TITLE_HEIGHT = 12;

        String name;
        List<InterfaceTerminalEntry> entries = new ArrayList<>();
        Set<InterfaceTerminalEntry> visibleEntries = new TreeSet<>(Comparator.comparing(e -> {
            if (e.dispRep != null) {
                return e.dispRep.getDisplayName() + e.id;
            } else {
                return String.valueOf(e.id);
            }
        }));
        int height;
        private boolean isDirty = true;
        boolean visible = false;

        InterfaceSection(String name) {
            this.name = name;
        }

        /**
         * Gets the height. Includes title.
         */
        public int getHeight() {
            if (isDirty) {
                update();
            }
            return height;
        }

        private void update() {
            refreshVisible();
            if (visibleEntries.isEmpty()) {
                height = 0;
            } else {
                height = TITLE_HEIGHT;
                for (InterfaceTerminalEntry entry : visibleEntries) {
                    height += entry.guiHeight;
                }
            }
            isDirty = false;
        }

        public void refreshVisible() {
            visibleEntries.clear();
            String input = GuiInterfaceTerminal.this.searchFieldInputs.getText().toLowerCase();
            String output = GuiInterfaceTerminal.this.searchFieldOutputs.getText().toLowerCase();

            for (InterfaceTerminalEntry entry : entries) {
                if (!entry.online || entry.p2pOutput) continue;

                var moleAss = AEApi.instance().definitions().blocks().molecularAssembler().maybeStack(1);
                entry.dispY = -9999;
                if (onlyMolecularAssemblers
                        && (!moleAss.isPresent() || !Platform.isSameItem(moleAss.get(), entry.dispRep))) {
                    continue;
                }
                if (AEConfig.instance.showOnlyInterfacesWithFreeSlotsInInterfaceTerminal
                        && entry.numItems == entry.rows * entry.rowSize) {
                    continue;
                }
                if (onlyBrokenRecipes && !entry.hasBrokenSlot()) {
                    continue;
                }
                if (onlySubstitute && !entry.hasUseSubstitute()) continue;

                // Find search terms
                if (!input.isEmpty() || !output.isEmpty()) {
                    AppEngInternalInventory inv = entry.inv;
                    boolean shouldAdd = false;

                    for (int i = 0; i < inv.getSizeInventory(); ++i) {
                        ItemStack stack = inv.getStackInSlot(i);
                        if (itemStackMatchesSearchTerm(stack, input, true)
                                && itemStackMatchesSearchTerm(stack, output, false)) {
                            shouldAdd = true;
                            entry.filteredRecipes[i] = false;
                        } else {
                            entry.filteredRecipes[i] = true;
                        }
                    }
                    if (!shouldAdd) {
                        continue;
                    }
                } else {
                    Arrays.fill(entry.filteredRecipes, false);
                }
                visibleEntries.add(entry);
            }
        }

        public void addEntry(InterfaceTerminalEntry entry) {
            this.entries.add(entry);
            entry.section = this;
            this.isDirty = true;
        }

        public void removeEntry(InterfaceTerminalEntry entry) {
            this.entries.remove(entry);
            entry.section = null;
            this.isDirty = true;
        }

        public Iterator<InterfaceTerminalEntry> getVisible() {
            if (isDirty) {
                update();
            }
            return visibleEntries.iterator();
        }

        public boolean mouseClicked(int relMouseX, int relMouseY, int btn) {
            Iterator<InterfaceTerminalEntry> it = getVisible();
            boolean ret = false;

            while (it.hasNext() && !ret) {
                ret = it.next().mouseClicked(relMouseX, relMouseY, btn);
            }

            return ret;
        }
    }

    /**
     * This class keeps track of an entry and its widgets.
     */
    private class InterfaceTerminalEntry {

        String dispName;
        AppEngInternalInventory inv;
        GuiImgButton optionsButton;
        /** Nullable - icon that represents the interface */
        ItemStack selfRep;
        /** Nullable - icon that represents the interface's "target" */
        ItemStack dispRep;
        InterfaceSection section;
        long id;
        int x, y, z, dim;
        int rows, rowSize;
        int guiHeight;
        int dispY = -9999;
        boolean online;
        boolean p2pOutput;
        private Boolean[] brokenRecipes;
        int numItems = 0;
        /** Should recipe be filtered out/grayed out? */
        boolean[] filteredRecipes;
        Boolean[] useSubstitute;
        private int hoveredSlotIdx = -1;

        InterfaceTerminalEntry(long id, String name, int rows, int rowSize, boolean online, boolean p2pOutput) {
            this.id = id;
            if (StatCollector.canTranslate(name)) {
                this.dispName = StatCollector.translateToLocal(name);
            } else {
                String fallback = name + ".name"; // its whatever. save some bytes on network but looks ugly
                if (StatCollector.canTranslate(fallback)) {
                    this.dispName = StatCollector.translateToLocal(fallback);
                } else {
                    this.dispName = StatCollector.translateToFallback(name);
                }
            }
            this.inv = new AppEngInternalInventory(null, rows * rowSize, 1);
            this.rows = rows;
            this.rowSize = rowSize;
            this.online = online;
            this.p2pOutput = p2pOutput;
            this.optionsButton = new GuiImgButton(2, 0, Settings.ACTIONS, ActionItems.HIGHLIGHT_INTERFACE);
            this.optionsButton.setHalfSize(true);
            this.guiHeight = 18 * rows + 1;
            this.brokenRecipes = new Boolean[rows * rowSize];
            this.useSubstitute = new Boolean[rows * rowSize];
            this.filteredRecipes = new boolean[rows * rowSize];
        }

        InterfaceTerminalEntry setLocation(int x, int y, int z, int dim) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dim = dim;

            return this;
        }

        InterfaceTerminalEntry setIcons(ItemStack selfRep, ItemStack dispRep) {
            // Kotlin would make this pretty easy :(
            this.selfRep = selfRep;
            this.dispRep = dispRep;

            return this;
        }

        public void fullItemUpdate(NBTTagList items, int newSize) {
            inv = new AppEngInternalInventory(null, newSize);
            rows = newSize / rowSize;
            brokenRecipes = new Boolean[newSize];
            numItems = 0;

            for (int i = 0; i < inv.getSizeInventory(); ++i) {
                setItemInSlot(ItemStack.loadItemStackFromNBT(items.getCompoundTagAt(i)), i);
            }
            this.guiHeight = 18 * rows + 4;
        }

        InterfaceTerminalEntry setItems(NBTTagList items) {
            assert items.tagCount() == inv.getSizeInventory();

            for (int i = 0; i < items.tagCount(); ++i) {
                setItemInSlot(ItemStack.loadItemStackFromNBT(items.getCompoundTagAt(i)), i);
            }
            return this;
        }

        public void partialItemUpdate(NBTTagList items, int[] validIndices) {
            for (int i = 0; i < validIndices.length; ++i) {
                setItemInSlot(ItemStack.loadItemStackFromNBT(items.getCompoundTagAt(i)), validIndices[i]);
            }
        }

        private void setItemInSlot(ItemStack stack, int idx) {
            final int oldHasItem = inv.getStackInSlot(idx) != null ? 1 : 0;
            final int newHasItem = stack != null ? 1 : 0;

            inv.setInventorySlotContents(idx, stack);
            // Update item count
            numItems += newHasItem - oldHasItem;
            assert numItems >= 0;
        }

        public boolean hasBrokenSlot() {
            boolean existsUnknown = false;

            for (int idx = 0; idx < brokenRecipes.length; idx++) {
                if (brokenRecipes[idx] == null) {
                    existsUnknown = true;
                } else if (brokenRecipes[idx] == true) {
                    return true;
                }
            }

            if (existsUnknown) {
                for (int idx = 0; idx < brokenRecipes.length; idx++) {
                    if (slotIsBroken(idx)) {
                        return true;
                    }
                }
            }

            return false;
        }

        public boolean hasUseSubstitute() {
            boolean existsUnknown = false;

            for (Boolean aBoolean : useSubstitute) {
                if (aBoolean == null) {
                    existsUnknown = true;
                } else if (aBoolean) {
                    return true;
                }
            }

            if (existsUnknown) {
                for (int idx = 0; idx < useSubstitute.length; idx++) {
                    if (slotIsUseSubstitute(idx)) {
                        return true;
                    }
                }
            }

            return false;
        }

        public boolean slotIsBroken(int idx) {

            if (brokenRecipes[idx] == null) {
                brokenRecipes[idx] = recipeIsBroken(inv.getStackInSlot(idx));
            }

            return brokenRecipes[idx];
        }

        public boolean slotIsUseSubstitute(int idx) {

            if (useSubstitute[idx] == null) {
                useSubstitute[idx] = isUseSubstitute(inv.getStackInSlot(idx));
            }

            return useSubstitute[idx];
        }

        public AppEngInternalInventory getInventory() {
            return inv;
        }

        public boolean mouseClicked(int mouseX, int mouseY, int btn) {
            if (!section.visible || btn < 0 || btn > 2) {
                return false;
            }
            if (mouseX >= optionsButton.xPosition && mouseX < 2 + optionsButton.width
                    && mouseY > Math.max(optionsButton.yPosition, InterfaceSection.TITLE_HEIGHT)
                    && mouseY <= Math.min(optionsButton.yPosition + optionsButton.height, viewHeight)) {
                optionsButton.func_146113_a(mc.getSoundHandler());
                // When using the highlight from the interface terminal, we want it to only
                // highlight the interface containing the patterns and not any output p2p interfaces
                BlockPosHighlighter.highlightBlocks(
                        mc.thePlayer,
                        Collections.singletonList(new DimensionalCoord(x, y, z, dim)),
                        PlayerMessages.InterfaceHighlighted.getUnlocalized(),
                        PlayerMessages.InterfaceInOtherDim.getUnlocalized());
                mc.thePlayer.closeScreen();
                return true;
            }

            int offsetY = mouseY - dispY - 1;
            int offsetX = mouseX - (VIEW_WIDTH - rowSize * 18) - 1;
            if (offsetX >= 0 && offsetX < (rowSize * 18)
                    && mouseY > Math.max(dispY, InterfaceSection.TITLE_HEIGHT)
                    && offsetY < Math.min(viewHeight - dispY, guiHeight - 1)) {
                final int col = offsetX / 18;
                final int row = offsetY / 18;
                final int slotIdx = row * rowSize + col;

                // send packet to server, request an update
                // TODO: Client prediction.
                PacketInventoryAction packet;

                if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
                    packet = new PacketInventoryAction(InventoryAction.MOVE_REGION, 0, id);
                } else if (isShiftKeyDown() && (btn == 0 || btn == 1)) {
                    packet = new PacketInventoryAction(InventoryAction.SHIFT_CLICK, slotIdx, id);
                } else if (btn == 0 || btn == 1) {
                    packet = new PacketInventoryAction(InventoryAction.PICKUP_OR_SET_DOWN, slotIdx, id);
                } else {
                    packet = new PacketInventoryAction(InventoryAction.CREATIVE_DUPLICATE, slotIdx, id);
                }
                NetworkHandler.instance.sendToServer(packet);
                return true;
            }

            return false;
        }
    }
}
