package appeng.helpers;

import java.util.List;

import net.minecraft.client.gui.GuiButton;

public interface ICustomButtonProvider {

    void initCustomButtons(int guiLeft, int guiTop, int xSize, int ySize, int xOffset, int yOffset,
            List<GuiButton> buttonList);

    boolean actionPerformedCustomButtons(final GuiButton btn);

    void writeCustomButtonData();

    void readCustomButtonData();

    ICustomButtonDataObject getDataObject();

    void setDataObject(ICustomButtonDataObject dataObject);
}
