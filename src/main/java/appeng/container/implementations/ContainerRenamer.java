package appeng.container.implementations;

import net.minecraft.entity.player.InventoryPlayer;

import appeng.api.config.SecurityPermissions;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.ContainerSubGui;
import appeng.helpers.ICustomNameObject;
import appeng.util.Platform;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ContainerRenamer extends ContainerSubGui {

    private final ICustomNameObject namedObject;

    @SideOnly(Side.CLIENT)
    private MEGuiTextField textField;

    public ContainerRenamer(InventoryPlayer ip, ICustomNameObject obj) {
        super(ip, obj);
        namedObject = obj;
    }

    @SideOnly(Side.CLIENT)
    public void setTextField(final MEGuiTextField name) {
        this.textField = name;
        if (getCustomName() != null) textField.setText(getCustomName());
    }

    public void setNewName(String newValue) {
        this.namedObject.setCustomName(newValue);
    }

    @Override
    public void setCustomName(final String customName) {
        super.setCustomName(customName);
        if (!Platform.isServer() && customName != null) textField.setText(customName);
    }

    @Override
    public void detectAndSendChanges() {
        verifyPermissions(SecurityPermissions.BUILD, false);
        super.detectAndSendChanges();
        if (!Platform.isServer() && getCustomName() != null) textField.setText(getCustomName());
    }
}
