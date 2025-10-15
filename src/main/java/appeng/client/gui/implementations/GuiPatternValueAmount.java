package appeng.client.gui.implementations;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;

import appeng.api.storage.ITerminalHost;
import appeng.container.implementations.ContainerPatternValueAmount;
import appeng.core.localization.GuiColors;
import appeng.core.localization.GuiText;
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
