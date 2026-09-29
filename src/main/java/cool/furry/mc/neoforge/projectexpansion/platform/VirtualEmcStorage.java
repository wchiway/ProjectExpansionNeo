package cool.furry.mc.neoforge.projectexpansion.platform;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;

/** A transaction-aware inventory whose contents are purchased from the owner's EMC account. */
public final class VirtualEmcStorage implements Storage<ItemVariant> {
    private final Supplier<UUID> owner;
    private final Supplier<List<ItemStack>> outputs;
    private final Predicate<ItemStack> accepts;
    private final IntSupplier importLimit;
    private final IntSupplier exportLimit;
    private final BiConsumer<Integer, TransactionContext> imported;
    private final BiConsumer<Integer, TransactionContext> exported;

    public VirtualEmcStorage(Supplier<UUID> owner, Supplier<List<ItemStack>> outputs, Predicate<ItemStack> accepts,
            IntSupplier importLimit, IntSupplier exportLimit, BiConsumer<Integer, TransactionContext> imported,
            BiConsumer<Integer, TransactionContext> exported) {
        this.owner = owner;
        this.outputs = outputs;
        this.accepts = accepts;
        this.importLimit = importLimit;
        this.exportLimit = exportLimit;
        this.imported = imported;
        this.exported = exported;
    }

    @Override
    public long insert(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank() || maxAmount <= 0) return 0;
        var account = EmcTransactions.get(owner.get());
        ItemStack stack = resource.toStack();
        if (account == null || !accepts.test(stack)) return 0;
        long value = IEMCProxy.INSTANCE.getSellValue(stack);
        if (value <= 0) return 0;
        int amount = (int) Math.min(maxAmount, Math.max(0, importLimit.getAsInt()));
        if (amount == 0) return 0;
        account.change(BigInteger.valueOf(value).multiply(BigInteger.valueOf(amount)), transaction);
        account.learn(IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(stack)), transaction);
        imported.accept(amount, transaction);
        return amount;
    }

    @Override
    public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
        if (resource.isBlank() || maxAmount <= 0) return 0;
        var account = EmcTransactions.get(owner.get());
        if (account == null || outputs.get().stream().noneMatch(stack -> resource.matches(stack))) return 0;
        long value = IEMCProxy.INSTANCE.getValue(resource.toStack());
        if (value <= 0) return 0;
        long amount = account.balance().divide(BigInteger.valueOf(value))
            .min(BigInteger.valueOf(Math.min(maxAmount, Math.max(0, exportLimit.getAsInt())))).longValue();
        if (amount <= 0) return 0;
        account.change(BigInteger.valueOf(value).multiply(BigInteger.valueOf(amount)).negate(), transaction);
        exported.accept((int) amount, transaction);
        return amount;
    }

    @Override
    public Iterator<StorageView<ItemVariant>> iterator() {
        return outputs.get().stream().filter(stack -> !stack.isEmpty()).<StorageView<ItemVariant>>map(stack -> new StorageView<>() {
            private final ItemVariant variant = ItemVariant.of(stack);
            @Override public ItemVariant getResource() { return variant; }
            @Override public boolean isResourceBlank() { return variant.isBlank(); }
            @Override public long getCapacity() { return Integer.MAX_VALUE; }
            @Override public long getAmount() {
                var account = EmcTransactions.get(owner.get());
                long value = IEMCProxy.INSTANCE.getValue(stack);
                return account == null || value <= 0 ? 0 : account.balance().divide(BigInteger.valueOf(value))
                    .min(BigInteger.valueOf(Math.max(0, exportLimit.getAsInt()))).longValue();
            }
            @Override public long extract(ItemVariant resource, long maxAmount, TransactionContext transaction) {
                return variant.equals(resource) ? VirtualEmcStorage.this.extract(resource, maxAmount, transaction) : 0;
            }
        }).iterator();
    }
}
