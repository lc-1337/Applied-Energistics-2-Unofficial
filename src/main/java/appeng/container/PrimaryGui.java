package appeng.container;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.bsideup.jabel.Desugar;

import appeng.core.sync.GuiBridge;
import appeng.util.Platform;

@Desugar
public record PrimaryGui(Object originalGui, ItemStack originalGuiIcon, TileEntity te, ForgeDirection side) {

    public void openOriginalGui(EntityPlayer p) {
        if (originalGui instanceof GuiBridge gb) {
            Platform.openGUI(p, te, side, gb);
        }
    }
}
