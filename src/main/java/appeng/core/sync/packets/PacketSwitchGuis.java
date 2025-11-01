/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.core.sync.packets;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.tileentity.TileEntity;

import appeng.client.gui.AEBaseGui;
import appeng.container.AEBaseContainer;
import appeng.container.ContainerOpenContext;
import appeng.container.PrimaryGui;
import appeng.container.interfaces.IContainerSubGui;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.GuiBridge;
import appeng.core.sync.network.INetworkInfo;
import appeng.util.Platform;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class PacketSwitchGuis extends AppEngPacket {

    private final GuiBridge newGui;

    // automatic.
    public PacketSwitchGuis(final ByteBuf stream) {
        if (stream.readBoolean()) this.newGui = GuiBridge.values()[stream.readInt()];
        else this.newGui = null;
    }

    // api
    public PacketSwitchGuis(final GuiBridge newGui) {
        this.newGui = newGui;

        if (Platform.isClient()) {
            AEBaseGui.setSwitchingGuis(true);
        }

        final ByteBuf data = Unpooled.buffer();

        data.writeInt(this.getPacketID());
        if (newGui != null) {
            data.writeBoolean(true);
            data.writeInt(newGui.ordinal());
        } else data.writeBoolean(false);

        this.configureWrite(data);
    }

    // api
    public PacketSwitchGuis() {
        this.newGui = null;

        if (Platform.isClient()) {
            AEBaseGui.setSwitchingGuis(true);
        }

        final ByteBuf data = Unpooled.buffer();

        data.writeInt(this.getPacketID());

        data.writeBoolean(false);

        this.configureWrite(data);
    }

    @Override
    public void serverPacketData(final INetworkInfo manager, final AppEngPacket packet, final EntityPlayer player) {
        final Container c = player.openContainer;
        if (c instanceof AEBaseContainer bc) {
            final PrimaryGui pGui = bc.getPrimaryGui();
            if (this.newGui == null) {
                bc.getPrimaryGui().open(player);
            } else {
                final ContainerOpenContext context = bc.getOpenContext();
                if (context != null) {
                    final TileEntity te = context.getTile();
                    Platform.openGUI(player, te, context.getSide(), this.newGui);
                }
            }

            if (player.openContainer instanceof IContainerSubGui sg) {
                sg.setPrimaryGui(pGui);
            }
        }
    }

    @Override
    public void clientPacketData(final INetworkInfo network, final AppEngPacket packet, final EntityPlayer player) {
        AEBaseGui.setSwitchingGuis(true);
    }
}
