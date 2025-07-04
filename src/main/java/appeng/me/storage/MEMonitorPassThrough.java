/*
 * This file is part of Applied Energistics 2. Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved. Applied
 * Energistics 2 is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any
 * later version. Applied Energistics 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details. You should have received a copy of the GNU Lesser General Public License along with
 * Applied Energistics 2. If not, see <http://www.gnu.org/licenses/lgpl>.
 */

package appeng.me.storage;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import appeng.api.config.StorageFilter;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEStack;
import appeng.api.storage.data.IItemList;
import appeng.util.IterationCounter;
import appeng.util.Platform;
import appeng.util.inv.ItemListIgnoreCrafting;
import appeng.util.item.ItemFilterList;

public class MEMonitorPassThrough<T extends IAEStack<T>> extends MEPassThrough<T>
        implements IMEMonitor<T>, IMEMonitorHandlerReceiver<T> {

    private final HashMap<IMEMonitorHandlerReceiver<T>, Object> listeners = new HashMap<>();
    private BaseActionSource changeSource;
    private IMEMonitor<T> monitor;
    private StorageFilter mode = StorageFilter.EXTRACTABLE_ONLY;

    public MEMonitorPassThrough(final IMEInventory<T> i, final StorageChannel channel) {
        super(i, channel);
        if (i instanceof IMEMonitor) {
            this.monitor = (IMEMonitor<T>) i;
        }
    }

    @Override
    public void setInternal(final IMEInventory<T> i) {
        if (this.monitor != null) {
            this.monitor.removeListener(this);
        }

        this.monitor = null;
        final IItemList<T> before = this.getInternal() == null ? this.getWrappedChannel().createList()
                : this.getInternal().getAvailableItems(
                        new ItemListIgnoreCrafting(this.getWrappedChannel().createList()),
                        IterationCounter.fetchNewId());

        super.setInternal(i);
        if (i instanceof IMEMonitor) {
            this.monitor = (IMEMonitor<T>) i;
        }

        final IItemList<T> after = this.getInternal() == null ? this.getWrappedChannel().createList()
                : this.getInternal().getAvailableItems(
                        new ItemListIgnoreCrafting(this.getWrappedChannel().createList()),
                        IterationCounter.fetchNewId());

        if (this.monitor != null) {
            this.monitor.addListener(this, this.monitor);
        }

        Platform.postListChanges(before, after, this, this.getChangeSource());
    }

    @Override
    public IItemList<T> getAvailableItems(final IItemList out, int iterator) {
        if (out instanceof ItemFilterList) {
            return super.getAvailableItems(out, iterator);
        }
        super.getAvailableItems(new ItemListIgnoreCrafting(out), iterator);
        return out;
    }

    @Override
    public void addListener(final IMEMonitorHandlerReceiver<T> l, final Object verificationToken) {
        this.listeners.put(l, verificationToken);
    }

    @Override
    public void removeListener(final IMEMonitorHandlerReceiver<T> l) {
        this.listeners.remove(l);
    }

    @Override
    public IItemList<T> getStorageList() {
        if (this.monitor == null) {
            final IItemList<T> out = this.getWrappedChannel().createList();
            this.getInternal().getAvailableItems(new ItemListIgnoreCrafting(out), IterationCounter.fetchNewId());
            return out;
        }
        return this.monitor.getStorageList();
    }

    @Override
    public boolean isValid(final Object verificationToken) {
        return verificationToken == this.monitor;
    }

    @Override
    public void postChange(final IBaseMonitor<T> monitor, final Iterable<T> change, final BaseActionSource source) {
        final Iterator<Entry<IMEMonitorHandlerReceiver<T>, Object>> i = this.listeners.entrySet().iterator();
        while (i.hasNext()) {
            final Entry<IMEMonitorHandlerReceiver<T>, Object> e = i.next();
            final IMEMonitorHandlerReceiver<T> receiver = e.getKey();
            if (receiver.isValid(e.getValue())) {
                receiver.postChange(this, change, source);
            } else {
                i.remove();
            }
        }
    }

    @Override
    public void onListUpdate() {
        final Iterator<Entry<IMEMonitorHandlerReceiver<T>, Object>> i = this.listeners.entrySet().iterator();
        while (i.hasNext()) {
            final Entry<IMEMonitorHandlerReceiver<T>, Object> e = i.next();
            final IMEMonitorHandlerReceiver<T> receiver = e.getKey();
            if (receiver.isValid(e.getValue())) {
                receiver.onListUpdate();
            } else {
                i.remove();
            }
        }
    }

    private BaseActionSource getChangeSource() {
        return this.changeSource;
    }

    public void setChangeSource(final BaseActionSource changeSource) {
        this.changeSource = changeSource;
    }

    public void setMode(final StorageFilter mode) {
        this.mode = mode;
    }

    public StorageFilter getMode() {
        return this.mode;
    }
}
