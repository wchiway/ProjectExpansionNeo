package cool.furry.mc.neoforge.projectexpansion.platform;

import java.util.Iterator;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import moze_intel.projecte.api.item_handlers.IItemHandler;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;

/** Side-aware bridge. Every side of a block shares one snapshot participant. */
public final class ItemHandlerStorage implements Storage<ItemVariant> {
    private final Supplier<IItemHandler> handler;
    private final SnapshotParticipant<?> snapshots;
    public ItemHandlerStorage(Supplier<IItemHandler> handler, SnapshotParticipant<?> snapshots) {
        this.handler = handler;
        this.snapshots = snapshots;
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank() || maxAmount <= 0) return 0;
        IItemHandler inventory = handler.get();
        if (inventory == null) return 0;
        long inserted = 0;
        for (int slot = 0; slot < inventory.getSlots() && inserted < maxAmount; slot++) {
            int amount = (int) Math.min(Integer.MAX_VALUE, maxAmount - inserted);
            ItemStack input = resource.toStack(amount);
            if (inventory.insertItem(slot, input, true).getCount() == amount) continue;
            snapshots.updateSnapshots(transaction);
            inserted += amount - inventory.insertItem(slot, input, false).getCount();
        }
        return inserted;
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank() || maxAmount <= 0) return 0;
        long extracted = 0;
        IItemHandler inventory = handler.get();
        if (inventory == null) return 0;
        for (int slot = 0; slot < inventory.getSlots() && extracted < maxAmount; slot++) {
            extracted += extractSlot(slot, resource, maxAmount - extracted, transaction);
        }
        return extracted;
    }

    private long extractSlot(int slot, ItemVariant resource, long maxAmount, TransactionContext transaction) {
        IItemHandler inventory = handler.get();
        if (inventory == null || maxAmount <= 0 || !resource.matches(inventory.getStackInSlot(slot))) return 0;
        int amount = (int) Math.min(Integer.MAX_VALUE, maxAmount);
        if (inventory.extractItem(slot, amount, true).isEmpty()) return 0;
        snapshots.updateSnapshots(transaction);
        return inventory.extractItem(slot, amount, false).getCount();
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        IItemHandler inventory = handler.get();
        int slots = inventory == null ? 0 : inventory.getSlots();
        return IntStream.range(0, slots).<StorageView<ItemVariant>>mapToObj(slot -> new StorageView<>() {
            @Override public ItemVariant getResource() { return ItemVariant.of(inventory.getStackInSlot(slot)); }
            @Override public boolean isResourceBlank() { return inventory.getStackInSlot(slot).isEmpty(); }
            @Override public long getAmount() { return inventory.getStackInSlot(slot).getCount(); }
            @Override public long getCapacity() { return inventory.getSlotLimit(slot); }
            @Override public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
                return extractSlot(slot, resource, maxAmount, transaction);
            }
        }).iterator();
    }
}
