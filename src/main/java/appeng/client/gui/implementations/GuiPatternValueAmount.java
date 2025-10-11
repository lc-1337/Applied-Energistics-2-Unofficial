package appeng.client.gui.implementations;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;

import appeng.api.AEApi;
import appeng.api.definitions.IDefinitions;
import appeng.api.definitions.IParts;
import appeng.api.storage.ITerminalHost;
import appeng.container.implementations.ContainerPatternValueAmount;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.NetworkHandler;
import appeng.core.sync.packets.PacketPatternValueSet;
import appeng.helpers.Reflected;

public class GuiPatternValueAmount extends GuiAmount {

    ContainerPatternValueAmount container;

    @Reflected
    public GuiPatternValueAmount(final InventoryPlayer inventoryPlayer, final ITerminalHost te) {
        super(new ContainerPatternValueAmount(inventoryPlayer, te));
        container = (ContainerPatternValueAmount) inventorySlots;
    }

    @Override
    protected void setOriginGUI(Object target) {
        final IDefinitions definitions = AEApi.instance().definitions();
        final IParts parts = definitions.parts();

        if (container.getOriginalGui() == GuiBridge.GUI_PATTERN_TERMINAL) {
            for (final ItemStack stack : parts.patternTerminal().maybeStack(1).asSet()) {
                myIcon = stack;
            }
        } else {
            for (final ItemStack stack : parts.patternTerminalEx().maybeStack(1).asSet()) {
                myIcon = stack;
            }
        }
    }

    public void update() {
        this.amountTextField.setText(String.valueOf(container.getStack().getStackSize()));
        this.amountTextField.setCursorPositionEnd();
        this.amountTextField.setSelectionPos(0);
    }

    @Override
    public void drawFG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.fontRendererObj
                .drawString(GuiText.SelectAmount.getLocal(), 8, 6, GuiColors.CraftAmountSelectAmount.getColor());
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY);
        this.nextBtn.displayString = GuiText.Set.getLocal();

        try {
            long resultI = getAmountLong();
            this.nextBtn.enabled = resultI > 0;
        } catch (final NumberFormatException e) {
            this.nextBtn.enabled = false;
        }

        this.amountTextField.drawTextBox();
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);

        try {
            if (btn == this.nextBtn && btn.enabled) {
                NetworkHandler.instance.sendToServer(
                        new PacketPatternValueSet(
                                container.getOriginalGui(),
                                container.getStack().setStackSize(getAmountLong()),
                                container.getInvName(),
                                container.getSlotsIndex()));
            }
        } catch (final NumberFormatException e) {
            // nope..
            this.amountTextField.setText("1");
        }
    }

    protected String getBackground() {
        return "guis/craftAmt.png";
    }
}
