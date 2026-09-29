package cool.furry.mc.neoforge.projectexpansion.platform;

import cool.furry.mc.neoforge.projectexpansion.util.Util;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/** One transaction participant per player, shared across all attached machines and sides. */
public final class EmcTransactions {
    private static final Map<UUID, Account> ACCOUNTS = new HashMap<>();
    private EmcTransactions() {}
    public static void clear() { ACCOUNTS.clear(); }

    public static @Nullable Account get(UUID owner) {
        if (owner == null) return null;
        ServerPlayer player = Util.getPlayer(owner);
        IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
        if (player == null || provider == null) return null;
        Account account = ACCOUNTS.get(owner);
        if (account == null || account.player != player) {
            account = new Account(player, provider);
            ACCOUNTS.put(owner, account);
        }
        return account;
    }

    public static final class Account extends SnapshotParticipant<Account.State> {
        private final ServerPlayer player;
        private final IKnowledgeProvider provider;
        private final Set<ItemInfo> pendingKnowledge = new LinkedHashSet<>();
        private Account(ServerPlayer player, IKnowledgeProvider provider) {
            this.player = player;
            this.provider = provider;
        }
        public BigInteger balance() { return provider.getEmc(); }
        public void change(BigInteger amount, TransactionContext transaction) {
            updateSnapshots(transaction);
            provider.setEmc(provider.getEmc().add(amount));
        }
        public void learn(ItemInfo item, TransactionContext transaction) {
            updateSnapshots(transaction);
            pendingKnowledge.add(item);
        }
        @Override
        protected State createSnapshot() { return new State(balance(), new LinkedHashSet<>(pendingKnowledge)); }
        @Override
        protected void readSnapshot(State state) {
            provider.setEmc(state.emc());
            pendingKnowledge.clear();
            pendingKnowledge.addAll(state.knowledge());
        }
        @Override
        protected void onFinalCommit() {
            for (ItemInfo info : pendingKnowledge) {
                if (provider.addKnowledge(info.createStack())) provider.syncKnowledgeChange(player, info, true);
            }
            pendingKnowledge.clear();
            provider.syncEmc(player);
        }
        record State(BigInteger emc, Set<ItemInfo> knowledge) {}
    }
}
