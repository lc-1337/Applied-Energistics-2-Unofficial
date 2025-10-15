package appeng.container.implementations;

import net.minecraft.entity.player.InventoryPlayer;

import appeng.api.storage.ITerminalHost;
import appeng.helpers.ISecondaryGUI;

public class ContainerPatternItemRenamer extends ContainerPatternValueAmount implements ISecondaryGUI {

    public ContainerPatternItemRenamer(final InventoryPlayer ip, final ITerminalHost te) {
        super(ip, te);
    }
}
