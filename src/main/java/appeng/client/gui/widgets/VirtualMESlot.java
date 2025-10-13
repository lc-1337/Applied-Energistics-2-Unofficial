package appeng.client.gui.widgets;

import static net.minecraft.client.gui.Gui.drawRect;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;

import org.lwjgl.opengl.GL11;

import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IDisplayRepo;

public class VirtualMESlot {

    private final int xPos;
    private final int yPos;
    private boolean isHidden = false;

    protected final IDisplayRepo repo;
    protected final int slotIndex;

    public VirtualMESlot(int x, int y, IDisplayRepo repo, int slotIndex) {
        this.xPos = x;
        this.yPos = y;
        this.repo = repo;
        this.slotIndex = slotIndex;
    }

    public int getX() {
        return this.xPos;
    }

    public int getY() {
        return this.yPos;
    }

    public int getSlotIndex() {
        return this.slotIndex;
    }

    @Nullable
    public IAEStack<?> getAEStack() {
        return this.repo.getReferenceStack(this.slotIndex);
    }

    public boolean isHovered(int mouseX, int mouseY) {
        return mouseX >= this.xPos && mouseX < this.xPos + 18 && mouseY >= this.yPos && mouseY < this.yPos + 18;
    }

    /**
     * Use {@link appeng.client.gui.AEBaseGui#drawVirtualSlots(VirtualMESlot[], int, int)} for drawing slots.<br>
     * Use {@link appeng.client.gui.AEBaseGui#drawSingleVirtualSlot(VirtualMESlot, int, int)} for drawing single slot.
     * 
     * @return ture if hovered
     */
    public boolean drawStackAndOverlay(Minecraft mc, int mouseX, int mouseY) {
        if (this.isHidden) return false;

        IAEStack<?> aes = this.getAEStack();
        if (aes != null) {
            aes.drawInGui(mc, this.xPos, this.yPos);
            aes.drawOverlayInGui(mc, this.xPos, this.yPos, true, true, true);
        }
        final boolean hovered = this.isHovered(mouseX, mouseY);
        if (hovered) {
            this.drawHoveredOverlay();
        }
        return hovered;
    }

    protected void drawHoveredOverlay() {
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glColorMask(true, true, true, false);
        drawRect(this.xPos, this.yPos, this.xPos + 16, this.yPos + 16, -2130706433);
        GL11.glColorMask(true, true, true, true);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    public boolean isHidden() {
        return this.isHidden;
    }

    public void setHidden(boolean hidden) {
        this.isHidden = hidden;
    }
}
