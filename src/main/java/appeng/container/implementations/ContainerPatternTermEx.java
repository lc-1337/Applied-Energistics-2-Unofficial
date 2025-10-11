package appeng.container.implementations;

import static appeng.parts.reporting.PartPatternTerminalEx.*;

import net.minecraft.entity.player.InventoryPlayer;

import appeng.api.storage.ITerminalHost;
import appeng.container.guisync.GuiSync;
import appeng.parts.reporting.PartPatternTerminalEx;
import appeng.util.Platform;

public class ContainerPatternTermEx extends ContainerPatternTerm {

    @GuiSync(96 + (17 - 9) + 16)
    public boolean inverted;

    @GuiSync(96 + (17 - 9) + 17)
    public int activePage = 0;

    public ContainerPatternTermEx(final InventoryPlayer ip, final ITerminalHost monitorable) {
        super(ip, monitorable, false);
        inverted = getExPatternTerminal().isInverted();
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        if (Platform.isServer()) {
            final PartPatternTerminalEx temp = getExPatternTerminal();
            substitute = temp.isSubstitution();
            beSubstitute = temp.canBeSubstitution();

            if (inverted != temp.isInverted() || activePage != temp.getActivePage()) {
                inverted = temp.isInverted();
                activePage = temp.getActivePage();
                offsetSlots();
            }
        }
    }

    private void offsetSlots() {
        for (int page = 0; page < getPatternInputPages(); page++) {
            for (int y = 0; y < getPatternInputsHeigh(); y++) {
                for (int x = 0; x < getPatternInputsWidth(); x++) {
                    this.craftingSlots[x + y * getPatternInputsWidth()
                            + page * (getPatternInputsWidth() * getPatternInputsHeigh())]
                                    .setHidden(page != activePage || x > 0 && inverted);
                }
            }
        }
        for (int page = 0; page < getPatternOutputPages(); page++) {
            for (int y = 0; y < getPatternOutputsHeigh(); y++) {
                for (int x = 0; x < getPatternOutputsWidth(); x++) {
                    this.outputSlots[x * getPatternOutputsHeigh() + y
                            + page * (getPatternOutputsWidth() * getPatternOutputsHeigh())]
                                    .setHidden(page != activePage || x > 0 && !inverted);
                }
            }
        }
    }

    @Override
    public void onUpdate(final String field, final Object oldValue, final Object newValue) {
        super.onUpdate(field, oldValue, newValue);

        if (field.equals("inverted") || field.equals("activePage")) {
            offsetSlots();
        }
    }

    public PartPatternTerminalEx getExPatternTerminal() {
        return (PartPatternTerminalEx) getPatternTerminal();
    }

    @Override
    protected int getPatternInputsWidth() {
        return exPatternInputsWidth;
    }

    @Override
    protected int getPatternInputsHeigh() {
        return exPatternInputsHeigh;
    }

    @Override
    protected int getPatternInputPages() {
        return exPatternInputsPages;
    }

    @Override
    protected int getPatternOutputsWidth() {
        return exPatternOutputsWidth;
    }

    @Override
    protected int getPatternOutputsHeigh() {
        return exPatternOutputsHeigh;
    }

    @Override
    protected int getPatternOutputPages() {
        return exPatternOutputPages;
    }
}
