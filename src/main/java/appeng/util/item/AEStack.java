/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.util.item;

import java.io.IOException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

import appeng.api.AEApi;
import appeng.api.config.TerminalFontSize;
import appeng.api.storage.data.IAEStack;
import appeng.client.render.StackSizeRenderer;
import appeng.core.AEConfig;
import appeng.core.localization.GuiText;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public abstract class AEStack<StackType extends IAEStack<StackType>> implements IAEStack<StackType> {

    private boolean isCraftable;
    private long stackSize;
    private long countRequestable;
    private long countRequestableCrafts;
    private float usedPercent;

    static long getPacketValue(final byte type, final ByteBuf tag) {
        if (type == 0) {
            long l = tag.readByte();
            l -= Byte.MIN_VALUE;
            return l;
        } else if (type == 1) {
            long l = tag.readShort();
            l -= Short.MIN_VALUE;
            return l;
        } else if (type == 2) {
            long l = tag.readInt();
            l -= Integer.MIN_VALUE;
            return l;
        }

        return tag.readLong();
    }

    @Override
    public long getStackSize() {
        return this.stackSize;
    }

    @Override
    public StackType setStackSize(final long ss) {
        this.stackSize = ss;
        return (StackType) this;
    }

    @Override
    public StackType setUsedPercent(final float percent) {
        this.usedPercent = percent;
        return (StackType) this;
    }

    @Override
    public float getUsedPercent() {
        return this.usedPercent;
    }

    @Override
    public long getCountRequestable() {
        return this.countRequestable;
    }

    @Override
    public StackType setCountRequestable(final long countRequestable) {
        this.countRequestable = countRequestable;
        return (StackType) this;
    }

    @Override
    public long getCountRequestableCrafts() {
        return this.countRequestableCrafts;
    }

    @Override
    public StackType setCountRequestableCrafts(long countRequestableCrafts) {
        this.countRequestableCrafts = countRequestableCrafts;
        return (StackType) this;
    }

    @Override
    public boolean isCraftable() {
        return this.isCraftable;
    }

    @Override
    public StackType setCraftable(final boolean isCraftable) {
        this.isCraftable = isCraftable;
        return (StackType) this;
    }

    @Override
    public StackType reset() {
        this.stackSize = 0;
        // priority = Integer.MIN_VALUE;
        this.setCountRequestable(0);
        this.setCraftable(false);
        this.setCountRequestableCrafts(0);
        this.setUsedPercent(0);
        return (StackType) this;
    }

    @Override
    public boolean isMeaningful() {
        return this.stackSize != 0 || this.countRequestable > 0 || this.isCraftable;
    }

    @Override
    public void incStackSize(final long i) {
        this.stackSize += i;
    }

    @Override
    public void decStackSize(final long i) {
        this.stackSize -= i;
    }

    @Override
    public void incCountRequestable(final long i) {
        this.countRequestable += i;
    }

    @Override
    public void decCountRequestable(final long i) {
        this.countRequestable -= i;
    }

    @Override
    public void writeToPacket(final ByteBuf i) throws IOException {
        final byte mask = (byte) (this.getType(0) | (this.getType(this.stackSize) << 2)
                | (this.getType(this.countRequestable) << 4)
                | ((byte) (this.isCraftable ? 1 : 0) << 6)
                | (this.hasTagCompound() ? 1 : 0) << 7);

        i.writeByte(mask);
        this.writeIdentity(i);

        this.readNBT(i);

        // putPacketValue( i, priority );
        this.putPacketValue(i, this.stackSize);
        this.putPacketValue(i, this.countRequestable);
        final long longUsedPercent = (long) (this.usedPercent * 10000);
        final byte mask2 = (byte) (this.getType(this.countRequestableCrafts) | (this.getType(longUsedPercent) << 2));
        i.writeByte(mask2);
        this.putPacketValue(i, this.countRequestableCrafts);
        this.putPacketValue(i, longUsedPercent);
    }

    private byte getType(final long num) {
        if (num <= 255) {
            return 0;
        } else if (num <= 65535) {
            return 1;
        } else if (num <= 4294967295L) {
            return 2;
        } else {
            return 3;
        }
    }

    abstract void writeIdentity(ByteBuf i) throws IOException;

    abstract void readNBT(ByteBuf i) throws IOException;

    private void putPacketValue(final ByteBuf tag, final long num) {
        if (num <= 255) {
            tag.writeByte((byte) (num + Byte.MIN_VALUE));
        } else if (num <= 65535) {
            tag.writeShort((short) (num + Short.MIN_VALUE));
        } else if (num <= 4294967295L) {
            tag.writeInt((int) (num + Integer.MIN_VALUE));
        } else {
            tag.writeLong(num);
        }
    }

    private static final ItemStack PATTERN = AEApi.instance().definitions().items().encodedPattern().maybeStack(1)
            .orNull();

    @Override
    @SideOnly(Side.CLIENT)
    public void drawOverlayInGui(Minecraft mc, int x, int y, boolean showAmount, boolean showAmountAlways,
            boolean showCraftableText, boolean showCraftableIcon) {
        final TerminalFontSize fontSize = AEConfig.instance.getTerminalFontSize();

        GL11.glTranslatef(0.0f, 0.0f, 200.0f);
        GL11.glDisable(GL11.GL_LIGHTING);

        if (showCraftableText && this.isCraftable()) {
            if (this.stackSize == 0) {
                final String craftLabelText = fontSize == TerminalFontSize.SMALL ? GuiText.SmallFontCraft.getLocal()
                        : GuiText.LargeFontCraft.getLocal();

                GL11.glPushMatrix();
                StackSizeRenderer.drawStackSize(x, y, craftLabelText, mc.fontRenderer, fontSize);
                GL11.glPopMatrix();
            } else {
                if (showCraftableIcon) {
                    GL11.glPushMatrix();
                    GL11.glScalef(0.4f, 0.4f, 0.4f);
                    RenderItem itemRender = new RenderItem();
                    itemRender.renderItemIntoGUI(
                            mc.fontRenderer,
                            mc.renderEngine,
                            PATTERN,
                            (int) ((x + 10) * 2.5),
                            (int) (y * 2.5));
                    GL11.glScalef(2.5f, 2.5f, 2.5f);
                    GL11.glPopMatrix();

                }
            }
        }

        if (showAmount && (this.stackSize > 1 || (showAmountAlways && (!showCraftableText || !this.isCraftable)))) {
            GL11.glPushMatrix();
            StackSizeRenderer.drawStackSize(x, y, this.stackSize, mc.fontRenderer, fontSize);
            GL11.glPopMatrix();
        }

        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glTranslatef(0.0f, 0.0f, -200.0f);
    }
}
