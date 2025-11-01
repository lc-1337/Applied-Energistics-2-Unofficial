package appeng.parts.p2p;

import static appeng.helpers.DualityInterface.NUMBER_OF_STORAGE_SLOTS;
import static appeng.util.Platform.stackConvertPacket;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import com.google.common.collect.ImmutableSet;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.Upgrades;
import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.tiles.ITileStorageMonitorable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkChannelsChanged;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkPowerStatusChange;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IStorageMonitorable;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.IConfigManager;
import appeng.core.sync.GuiBridge;
import appeng.helpers.DualityInterface;
import appeng.helpers.IInterfaceHost;
import appeng.helpers.IPrimaryGuiIconProvider;
import appeng.helpers.IPriorityHost;
import appeng.helpers.Reflected;
import appeng.me.GridAccessException;
import appeng.me.cache.CraftingGridCache;
import appeng.parts.automation.UpgradeInventory;
import appeng.tile.inventory.AppEngInternalInventory;
import appeng.tile.inventory.IAEAppEngInventory;
import appeng.tile.inventory.InvOperation;
import appeng.util.Platform;
import appeng.util.inv.AdaptorIInventory;
import appeng.util.inv.IInventoryDestination;
import appeng.util.inv.WrapperInvSlot;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class PartP2PInterface extends PartP2PTunnelStatic<PartP2PInterface>
        implements IGridTickable, IStorageMonitorable, IInventoryDestination, IInterfaceHost, ISidedInventory,
        IAEAppEngInventory, ITileStorageMonitorable, IPriorityHost, IPrimaryGuiIconProvider {

    boolean needUpdateOnNetworkBooted = false;
    boolean lastPowerStart = false;

    @Reflected
    public PartP2PInterface(ItemStack is) {
        super(is);
    }

    protected final DualityInterface duality = new DualityInterface(this.getProxy(), this) {

        @Override
        public void updateCraftingList() {
            if (!isOutput()) {
                CraftingGridCache.pauseRebuilds();

                super.updateCraftingList();

                try {
                    for (PartP2PInterface p2p : getOutputs()) {
                        p2p.duality.updateCraftingList();
                    }
                } catch (GridAccessException e) {
                    // ?
                }

                CraftingGridCache.unpauseRebuilds();
            } else {
                PartP2PInterface p2p = getInput();
                if (p2p != null) {
                    this.craftingList = p2p.duality.craftingList;
                    try {
                        this.gridProxy.getGrid()
                                .postEvent(new MENetworkCraftingPatternChange(this, this.gridProxy.getNode()));
                    } catch (final GridAccessException e) {
                        // :P
                    }
                }
            }
        }

        @Override
        public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
            final boolean powerState = proxy.isActive();
            if (needUpdateOnNetworkBooted || lastPowerStart != powerState) {
                needUpdateOnNetworkBooted = false;
                lastPowerStart = powerState;
                updateSharingInventory();
            }
            return super.tickingRequest(node, ticksSinceLastCall);
        }

        @Override
        protected boolean hasWorkToDo() {
            if (isOutput()) {
                return hasItemsToSend() || hasConfig() || !getStorage().isEmpty();
            } else return super.hasWorkToDo();
        }

        @Override
        public void readConfig() {
            if (isOutput()) {
                PartP2PInterface p2p = getInput();
                boolean alertDevice = false;

                if (p2p != null) {
                    this.setHasConfig(p2p.duality.hasConfig());

                    alertDevice = hasWorkToDo();

                } else {
                    this.setHasConfig(false);
                }

                if (alertDevice) {
                    try {
                        this.gridProxy.getTick().alertDevice(this.gridProxy.getNode());
                    } catch (final GridAccessException ignored) {}
                } else {
                    try {
                        this.gridProxy.getTick().sleepDevice(this.gridProxy.getNode());
                    } catch (final GridAccessException ignored) {}
                }

                this.notifyNeighbors();
            } else {
                super.readConfig();
            }
        }

        @Override
        public void addDrops(final List<ItemStack> drops) {
            if (!isOutput()) {
                super.addDrops(drops);
                try {
                    for (PartP2PInterface p2p : getOutputs()) p2p.duality.addDrops(drops);
                } catch (GridAccessException ignored) {}
            } else {
                if (this.getWaitingToSend() != null) {
                    for (final IAEStack<?> is : this.getWaitingToSend()) {
                        if (is != null) {
                            drops.add(stackConvertPacket(is).getItemStack());
                        }
                    }
                }

                for (final ItemStack is : this.getUpgrades()) {
                    if (is != null) {
                        drops.add(is);
                    }
                }

                for (final ItemStack is : this.getPatterns()) {
                    if (is != null) {
                        drops.add(is);
                    }
                }
            }
        }

        @Override
        public int getInstalledUpgrades(Upgrades u) {
            if (isOutput() && u == Upgrades.PATTERN_CAPACITY) return -1;
            return super.getInstalledUpgrades(u);
        }

        @Override
        public int getConfigSize() {
            if (isOutput()) return -1;
            return super.getConfigSize();
        }
    };

    private void updateSharingInventory() {
        duality.readConfig();
        if (isOutput()) {
            PartP2PInterface p2p = getInput();
            if (proxy.isActive() && p2p != null) {
                if (!duality.sharedInventory && !duality.getStorage().isEmpty()) {
                    ArrayList<ItemStack> drops = new ArrayList<>();
                    AppEngInternalInventory storageAppEng = this.duality.getStorage();
                    AdaptorIInventory p2pInv = new AdaptorIInventory(p2p.duality.getStorage());
                    for (ItemStack itemStack : storageAppEng) {
                        if (itemStack == null) continue;
                        ItemStack drop = p2pInv.addItems(itemStack);
                        if (drop != null && drop.stackSize > 0) {
                            drops.add(drop);
                        }
                    }
                    TileEntity te = getTileEntity();
                    Platform.spawnDrops(te.getWorldObj(), te.xCoord, te.yCoord, te.zCoord, drops);
                }
                duality.setStorage(p2p.duality.getStorage());
                duality.sharedInventory = true;
            } else {
                if (duality.sharedInventory) {
                    duality.setStorage(new AppEngInternalInventory(this, NUMBER_OF_STORAGE_SLOTS));
                    duality.setSlotInv(new WrapperInvSlot(duality.getStorage()));
                    duality.sharedInventory = false;
                }
            }
        } else {
            if (duality.sharedInventory) {
                duality.setStorage(new AppEngInternalInventory(this, NUMBER_OF_STORAGE_SLOTS));
                duality.setSlotInv(new WrapperInvSlot(duality.getStorage()));
                duality.sharedInventory = false;
            }
            try {
                for (PartP2PInterface p2p : getOutputs()) {
                    p2p.duality.readConfig();
                }
            } catch (GridAccessException ignored) {}
        }
    }

    @MENetworkEventSubscribe
    public void stateChange(final MENetworkChannelsChanged c) {
        this.duality.notifyNeighbors();
        updateSharingInventory();
        needUpdateOnNetworkBooted = true;
    }

    @MENetworkEventSubscribe
    public void stateChange(final MENetworkPowerStatusChange c) {
        this.duality.notifyNeighbors();
        updateSharingInventory();
        needUpdateOnNetworkBooted = true;
    }

    @Override
    public int getInstalledUpgrades(final Upgrades u) {
        return this.duality.getInstalledUpgrades(u);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getTypeTexture() {
        return AEApi.instance().definitions().blocks().iface().maybeBlock().get().getBlockTextureFromSide(0);
    }

    @Override
    public void gridChanged() {
        this.duality.gridChanged();
    }

    @Override
    public void readFromNBT(final NBTTagCompound data) {
        super.readFromNBT(data);
        this.duality.readFromNBT(data);
    }

    @Override
    public void writeToNBT(final NBTTagCompound data) {
        super.writeToNBT(data);
        this.duality.writeToNBT(data);
    }

    @Override
    public NBTTagCompound getMemoryCardData() {
        final NBTTagCompound output = super.getMemoryCardData();
        this.duality.getConfigManager().writeToNBT(output);
        return output;
    }

    @Override
    public PartP2PTunnel<?> applyMemoryCard(EntityPlayer player, IMemoryCard memoryCard, ItemStack is) {
        PartP2PTunnel<?> newTunnel = super.applyMemoryCard(player, memoryCard, is);
        NBTTagCompound data = memoryCard.getData(is);
        if (newTunnel instanceof PartP2PInterface p2PInterface) {
            p2PInterface.duality.getConfigManager().readFromNBT(data);
        }
        return newTunnel;
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        this.duality.initialize();
    }

    @Override
    public void getDrops(final List<ItemStack> drops, final boolean wrenched) {
        this.duality.addDrops(drops);
    }

    @Override
    public int cableConnectionRenderTo() {
        return 4;
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.duality.getConfigManager();
    }

    @Override
    public IInventory getInventoryByName(final String name) {
        return this.duality.getInventoryByName(name);
    }

    @Override
    public void onNeighborChanged() {
        this.duality.updateRedstoneState();
    }

    @Override
    public boolean onPartActivate(final EntityPlayer p, final Vec3 pos) {

        if (super.onPartActivate(p, pos)) {
            return true;
        }

        if (p.isSneaking()) {
            return false;
        }

        if (Platform.isServer()) {
            Platform.openGUI(p, this.getTileEntity(), this.getSide(), GuiBridge.GUI_INTERFACE);
        }

        return true;
    }

    private void dropPatterns(final PartP2PInterface p2p) {
        final AppEngInternalInventory patterns = p2p.duality.getPatterns();
        final List<ItemStack> drops = new ArrayList<>();
        for (int i = 0; i < patterns.getSizeInventory(); i++) {
            if (patterns.getStackInSlot(i) == null) continue;
            drops.add(patterns.getStackInSlot(i));
        }
        if (!drops.isEmpty()) {
            final TileEntity te = p2p.getTileEntity();
            Platform.spawnDrops(te.getWorldObj(), te.xCoord, te.yCoord, te.zCoord, drops);
        }
    }

    @Override
    protected void copyContents(final PartP2PTunnel<?> from) {
        if (from instanceof PartP2PInterface fromInterface) {
            DualityInterface newDuality = this.duality;
            // Copy interface storage, upgrades, and settings over
            UpgradeInventory upgrades = (UpgradeInventory) fromInterface.duality.getInventoryByName("upgrades");
            UpgradeInventory newUpgrade = (UpgradeInventory) newDuality.getInventoryByName("upgrades");
            for (int i = 0; i < upgrades.getSizeInventory(); ++i) {
                newUpgrade.setInventorySlotContents(i, upgrades.getStackInSlot(i));
            }

            if (!fromInterface.duality.sharedInventory) {
                IInventory storage = fromInterface.duality.getStorage();
                IInventory newStorage = newDuality.getStorage();
                for (int i = 0; i < storage.getSizeInventory(); ++i) {
                    newStorage.setInventorySlotContents(i, storage.getStackInSlot(i));
                }
            }

            AppEngInternalInventory patterns = fromInterface.duality.getPatterns();
            boolean drop = true;
            if (!from.isOutput() && !this.isOutput()) { // input to input
                for (int i = 0; i < patterns.getSizeInventory(); i++) {
                    newDuality.getPatterns().setInventorySlotContents(i, patterns.getStackInSlot(i));
                }
                drop = false;
            }

            if (drop) {
                this.dropPatterns(fromInterface);
            }
        }
    }

    @Override
    protected void copySettings(final PartP2PTunnel<?> from) {
        if (from instanceof PartP2PInterface fromInterface) {
            DualityInterface newDuality = this.duality;

            IConfigManager config = fromInterface.duality.getConfigManager();
            config.getSettings()
                    .forEach(setting -> newDuality.getConfigManager().putSetting(setting, config.getSetting(setting)));
        }
    }

    @Override
    public IIcon getBreakingTexture() {
        return this.getItemStack().getIconIndex();
    }

    @Override
    public boolean canInsert(final ItemStack stack) {
        return this.duality.canInsert(stack);
    }

    @Override
    public IMEMonitor<IAEItemStack> getItemInventory() {
        return this.duality.getItemInventory();
    }

    @Override
    public IMEMonitor<IAEFluidStack> getFluidInventory() {
        return this.duality.getFluidInventory();
    }

    @Override
    public TickingRequest getTickingRequest(final IGridNode node) {
        return this.duality.getTickingRequest(node);
    }

    @Override
    public TickRateModulation tickingRequest(final IGridNode node, final int ticksSinceLastCall) {
        return this.duality.tickingRequest(node, ticksSinceLastCall);
    }

    @Override
    public int getSizeInventory() {
        return this.duality.getStorage().getSizeInventory();
    }

    @Override
    public ItemStack getStackInSlot(final int i) {
        return this.duality.getStorage().getStackInSlot(i);
    }

    @Override
    public ItemStack decrStackSize(final int i, final int j) {
        return this.duality.getStorage().decrStackSize(i, j);
    }

    @Override
    public ItemStack getStackInSlotOnClosing(final int i) {
        return this.duality.getStorage().getStackInSlotOnClosing(i);
    }

    @Override
    public void setInventorySlotContents(final int i, final ItemStack itemstack) {
        this.duality.getStorage().setInventorySlotContents(i, itemstack);
    }

    @Override
    public String getInventoryName() {
        return this.duality.getStorage().getInventoryName();
    }

    @Override
    public boolean hasCustomInventoryName() {
        return this.duality.getStorage().hasCustomInventoryName();
    }

    @Override
    public int getInventoryStackLimit() {
        return this.duality.getStorage().getInventoryStackLimit();
    }

    @Override
    public void markDirty() {
        this.duality.getStorage().markDirty();
    }

    @Override
    public boolean isUseableByPlayer(final EntityPlayer entityplayer) {
        return this.duality.getStorage().isUseableByPlayer(entityplayer);
    }

    @Override
    public void openInventory() {
        this.duality.getStorage().openInventory();
    }

    @Override
    public void closeInventory() {
        this.duality.getStorage().closeInventory();
    }

    @Override
    public boolean isItemValidForSlot(final int i, final ItemStack itemstack) {
        return this.duality.getStorage().isItemValidForSlot(i, itemstack);
    }

    @Override
    public int[] getAccessibleSlotsFromSide(final int s) {
        return this.duality.getAccessibleSlotsFromSide(s);
    }

    @Override
    public boolean canInsertItem(final int i, final ItemStack itemstack, final int j) {
        return true;
    }

    @Override
    public boolean canExtractItem(final int i, final ItemStack itemstack, final int j) {
        return true;
    }

    @Override
    public void onChangeInventory(final IInventory inv, final int slot, final InvOperation mc,
            final ItemStack removedStack, final ItemStack newStack) {
        this.duality.onChangeInventory(inv, slot, mc, removedStack, newStack);
    }

    @Override
    public DualityInterface getInterfaceDuality() {
        return this.duality;
    }

    @Override
    public EnumSet<ForgeDirection> getTargets() {
        return EnumSet.of(this.getSide());
    }

    @Override
    public TileEntity getTileEntity() {
        return super.getHost().getTile();
    }

    @Override
    public IStorageMonitorable getMonitorable(final ForgeDirection side, final BaseActionSource src) {
        return this.duality.getMonitorable(side, src, this);
    }

    @Override
    public boolean pushPattern(final ICraftingPatternDetails patternDetails, final InventoryCrafting table) {
        return this.duality.pushPattern(patternDetails, table);
    }

    @Override
    public boolean isBusy() {
        return this.duality.isBusy();
    }

    @Override
    public void provideCrafting(final ICraftingProviderHelper craftingTracker) {
        this.duality.provideCrafting(craftingTracker);
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return this.duality.getRequestedJobs();
    }

    @Override
    public IAEItemStack injectCraftedItems(final ICraftingLink link, final IAEItemStack items, final Actionable mode) {
        return this.duality.injectCraftedItems(link, items, mode);
    }

    @Override
    public void jobStateChange(final ICraftingLink link) {
        this.duality.jobStateChange(link);
    }

    @Override
    public int getPriority() {
        return this.duality.getPriority();
    }

    @Override
    public void setPriority(final int newValue) {
        this.duality.setPriority(newValue);
    }

    @Override
    public void onTunnelNetworkChange() {
        this.duality.updateCraftingList();
        updateSharingInventory();
    }

    @Override
    public void onTunnelConfigChange() {
        updateSharingInventory();
    }

    @Override
    public IInventory getPatterns() {
        if (isOutput()) {
            PartP2PInterface input = getInput();
            if (input != null) {
                return input.getPatterns();
            }
            return IInterfaceHost.super.getPatterns();
        }
        return IInterfaceHost.super.getPatterns();
    }

    @Override
    public int rows() {
        if (isOutput()) {
            PartP2PInterface input = getInput();
            if (input != null) {
                return input.rows();
            }
            return IInterfaceHost.super.rows();
        }
        return IInterfaceHost.super.rows();
    }

    @Override
    public int rowSize() {
        if (isOutput()) {
            PartP2PInterface input = getInput();
            if (input != null) {
                return input.rowSize();
            }
            return IInterfaceHost.super.rowSize();
        }
        return IInterfaceHost.super.rowSize();
    }

    @Override
    public String getName() {
        if (isOutput()) {
            PartP2PInterface input = getInput();
            if (input != null) {
                return input.getName();
            }
            return IInterfaceHost.super.getName();
        }
        return IInterfaceHost.super.getName();
    }

    @Override
    public ItemStack getSelfRep() {
        return this.getItemStack();
    }

    @Override
    public ItemStack getPrimaryGuiIcon() {
        return AEApi.instance().definitions().parts().p2PTunnelMEInterface().maybeStack(1).orNull();
    }
}
