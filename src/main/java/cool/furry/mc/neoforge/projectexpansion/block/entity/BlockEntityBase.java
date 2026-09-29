package cool.furry.mc.neoforge.projectexpansion.block.entity;

import cool.furry.mc.neoforge.projectexpansion.util.Util;
import moze_intel.projecte.utils.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import moze_intel.projecte.api.item_handlers.ItemStackHandler;

import java.util.stream.IntStream;

public class BlockEntityBase extends BlockEntity {
    private boolean updateComparators;
    private final net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant<java.util.List<InventoryState>> inventorySnapshots =
        new net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant<>() {
            @Override protected java.util.List<InventoryState> createSnapshot() {
                java.util.List<InventoryState> states = new java.util.ArrayList<>();
                java.util.List<net.minecraft.core.Direction> sides = new java.util.ArrayList<>(java.util.Arrays.asList(net.minecraft.core.Direction.values()));
                sides.add(null);
                for (var side : sides) {
                    var handler = inventory(side);
                    if (handler instanceof moze_intel.projecte.api.item_handlers.IItemHandlerModifiable mutable) {
                        var stacks = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
                        for (int slot = 0; slot < mutable.getSlots(); slot++) stacks.add(mutable.getStackInSlot(slot).copy());
                        states.add(new InventoryState(mutable, stacks));
                    } else if (handler != null) {
                        throw new IllegalStateException("Non-transactional inventory on " + getBlockPos());
                    }
                }
                return states;
            }
            @Override protected void readSnapshot(java.util.List<InventoryState> states) {
                for (var state : states) for (int slot = 0; slot < state.stacks().size(); slot++) {
                    state.handler().setStackInSlot(slot, state.stacks().get(slot));
                }
            }
            @Override protected void onFinalCommit() { setChanged(); }
        };
    private record InventoryState(moze_intel.projecte.api.item_handlers.IItemHandlerModifiable handler,
                                  java.util.List<net.minecraft.world.item.ItemStack> stacks) {}
    private moze_intel.projecte.api.item_handlers.IItemHandler inventory(net.minecraft.core.Direction side) {
        return cool.furry.mc.neoforge.projectexpansion.platform.InventoryCapabilities.ItemHandler.BLOCK.find(level, worldPosition, getBlockState(), this, side);
    }
    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant> fabricInventory(net.minecraft.core.Direction side) {
        return new cool.furry.mc.neoforge.projectexpansion.platform.ItemHandlerStorage(() -> inventory(side), inventorySnapshots);
    }

    public BlockEntityBase(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    /***************
     * Comparators *
     ***************/

    protected void updateComparators(Level level, BlockPos pos) {
        //Only update the comparator state if we need to update comparators
        //Note: We call this at the end of child implementations to try and update any changes immediately instead
        // of them having to be delayed a tick
        if (updateComparators) {
            BlockState state = getBlockState();
            if (!state.isAir()) {
                level.updateNeighbourForOutputSignal(pos, state.getBlock());
            }
            updateComparators = false;
        }
    }

    @Override
    public final void setChanged() {
        if (level != null) {
            markDirty(level, worldPosition, true);
        }
    }

    /*********
     * Dirty *
     *********/

    public void markDirty() {
        if (level != null) {
            markDirty(level, worldPosition);
        }
    }

    public void markDirty(Level level, BlockPos pos) {
        markDirty(level, pos, false);
    }

    public void markDirty(Level level, BlockPos pos, boolean recheckComparators) {
        Util.markDirty(level, pos);
        if (recheckComparators && !level.isClientSide) {
            updateComparators = true;
        }
    }

    /********
     * Data *
     ********/

    @Override
    public final CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public final ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    protected class StackHandler extends ItemStackHandler {

        public StackHandler(int size) {
            super(size);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            setChanged();
        }
    }

    @SuppressWarnings("unused")
    protected class CompactableStackHandler extends StackHandler {

        //Start as needing to check for compacting when loaded
        private boolean needsCompacting = true;
        private boolean empty;

        public CompactableStackHandler(int size) {
            super(size);
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            needsCompacting = true;
        }

        public void compact() {
            if (needsCompacting) {
                if (level != null && !level.isClientSide) {
                    empty = ItemHelper.compactInventory(this);
                }
                needsCompacting = false;
            }
        }

        @Override
        protected void onLoad() {
            super.onLoad();
            empty = IntStream.range(0, getSlots()).allMatch(slot -> getStackInSlot(slot).isEmpty());
        }

        /**
         * @apiNote Only use this on the server
         */
        public boolean isEmpty() {
            return empty;
        }
    }
}
