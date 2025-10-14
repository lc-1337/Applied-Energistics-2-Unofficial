package appeng.helpers;

import appeng.api.implementations.IUpgradeableHost;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.storage.ICellContainer;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.me.storage.MEInventoryHandler;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.util.IConfigManagerHost;
import buildcraft.api.transport.IPipeConnection;

public interface IStorageBus extends IGridTickable, ICellContainer, IMEMonitorHandlerReceiver, IPipeConnection,
        IPriorityHost, IOreFilterable, IAEAppEngInventory, IConfigManagerHost, IUpgradeableHost {

    boolean needSyncGUI();

    void setNeedSyncGUI(boolean needSyncGUI);

    MEInventoryHandler<?> getInternalHandler();
}
