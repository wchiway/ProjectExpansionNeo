package cool.furry.mc.neoforge.projectexpansion.block.entity;

import cool.furry.mc.neoforge.projectexpansion.block.BlockEMCLink;
import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.neoforge.projectexpansion.util.*;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.block_entity.IEmcStorage;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import cool.furry.mc.neoforge.projectexpansion.platform.InventoryCapabilities;
import java.util.function.BiFunction;
import cool.furry.mc.neoforge.projectexpansion.platform.CapabilityRegistrar;
import moze_intel.projecte.api.item_handlers.IItemHandler;
import moze_intel.projecte.api.item_handlers.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Objects;

@SuppressWarnings("unused")
public class BlockEntityEMCLink extends BlockEntityNBTFilterable implements IHasMatter {
    public static final BiFunction<BlockEntityEMCLink, @org.jetbrains.annotations.Nullable Direction, IEmcStorage> EMC_STORAGE_PROVIDER = (link, side) -> link.getEMCHandler();
    public static final BiFunction<BlockEntityEMCLink, @org.jetbrains.annotations.Nullable Direction, IEmcStorageBigInteger> BIG_EMC_STORAGE_PROVIDER = (link, side) -> link.getEMCHandler();
    public static final BiFunction<BlockEntityEMCLink, @org.jetbrains.annotations.Nullable Direction, IItemHandler> ITEM_HANDLER_PROVIDER = (link, side) -> link.getItemHandler();
    public BigInteger emc = BigInteger.ZERO;
    public ItemStack itemStack;
    public Matter matter;
    public BigInteger remainingEMC = BigInteger.ZERO;
    public int remainingImport = 0;
    public int remainingExport = 0;
    public long remainingFluid = 0;
    private final net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant<long[]> transferLimits =
        new net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant<>() {
            @Override protected long[] createSnapshot() { return new long[]{remainingImport, remainingExport, remainingFluid}; }
            @Override protected void readSnapshot(long[] values) { remainingImport = (int) values[0]; remainingExport = (int) values[1]; remainingFluid = values[2]; }
            @Override protected void onFinalCommit() { markDirty(); }
        };
    private final cool.furry.mc.neoforge.projectexpansion.platform.VirtualEmcStorage fabricItems =
        new cool.furry.mc.neoforge.projectexpansion.platform.VirtualEmcStorage(() -> owner, () -> java.util.List.of(itemStack),
            stack -> getMatter().getEMCLinkInventorySize() > 1 && IEMCProxy.INSTANCE.hasValue(stack) &&
                (!getFilterStatus() || IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(stack)).equals(ItemInfo.fromStack(stack))),
            () -> getMatter() == Matter.FINAL ? Integer.MAX_VALUE : remainingImport,
            () -> getMatter() == Matter.FINAL ? Integer.MAX_VALUE : remainingExport,
            (amount, transaction) -> { transferLimits.updateSnapshots(transaction); if (getMatter() != Matter.FINAL) remainingImport -= amount; },
            (amount, transaction) -> { transferLimits.updateSnapshots(transaction); if (getMatter() != Matter.FINAL) remainingExport -= amount; });
    private final FluidHandler fabricFluids = new FluidHandler();
    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.item.ItemVariant> fabricItems() { return fabricItems; }
    public net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> fabricFluids() { return fabricFluids; }


    public static void registerCapabilities(CapabilityRegistrar event) {
        event.registerBlockEntity(PECapabilities.EMC_STORAGE_CAPABILITY, BlockEntityTypes.EMC_LINK.get(), EMC_STORAGE_PROVIDER);
        event.registerBlockEntity(cool.furry.mc.neoforge.projectexpansion.registries.Capabilities.BIG_EMC_STORAGE_CAPABILITY, BlockEntityTypes.EMC_LINK.get(), BIG_EMC_STORAGE_PROVIDER);
        event.registerBlockEntity(InventoryCapabilities.ItemHandler.BLOCK, BlockEntityTypes.EMC_LINK.get(), ITEM_HANDLER_PROVIDER);
    }

    public BlockEntityEMCLink(BlockPos pos, BlockState state) {
        super(BlockEntityTypes.EMC_LINK.get(), pos, state);
        itemStack = ItemStack.EMPTY;
    }

    private EMCHandler getEMCHandler() {
        return new EMCHandler();
    }

    private ItemHandler getItemHandler() {
        return new ItemHandler();
    }

    private FluidHandler getFluidHandler() {
        return fabricFluids;
    }

    /*******
     * NBT *
     *******/

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TagNames.STORED_EMC, Tag.TAG_STRING)) emc = new BigInteger(tag.getString((TagNames.STORED_EMC)));
        if (tag.contains(TagNames.ITEM, Tag.TAG_COMPOUND)) itemStack = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(ItemStack.parseOptional(registries, tag.getCompound(TagNames.ITEM)))).createStack();
        if (tag.contains(TagNames.REMAINING_EMC, Tag.TAG_STRING)) remainingEMC = new BigInteger(tag.getString(TagNames.REMAINING_EMC));
        if (tag.contains(TagNames.REMAINING_IMPORT, Tag.TAG_INT)) remainingImport = tag.getInt(TagNames.REMAINING_IMPORT);
        if (tag.contains(TagNames.REMAINING_EXPORT, Tag.TAG_INT)) remainingExport = tag.getInt(TagNames.REMAINING_EXPORT);
        if (tag.contains(TagNames.REMAINING_FLUID, Tag.TAG_LONG)) remainingFluid = tag.getLong(TagNames.REMAINING_FLUID);
        else if (tag.contains(TagNames.REMAINING_FLUID, Tag.TAG_INT)) remainingFluid = tag.getInt(TagNames.REMAINING_FLUID) * 81L;
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(TagNames.STORED_EMC, emc.toString());
        tag.put(TagNames.ITEM, itemStack.saveOptional(registries));
        tag.putString(TagNames.REMAINING_EMC, remainingEMC.toString());
        tag.putInt(TagNames.REMAINING_IMPORT, remainingImport);
        tag.putInt(TagNames.REMAINING_EXPORT, remainingExport);
        tag.putLong(TagNames.REMAINING_FLUID, remainingFluid);
    }

    /********
     * MISC *
     ********/

    public static void tickServer(Level level, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (blockEntity instanceof BlockEntityEMCLink be) be.tickServer(level, pos, state, be);
    }

    public void tickServer(Level level, BlockPos pos, BlockState state, BlockEntityEMCLink blockEntity) {
        // due to the nature of per second this block follows, using the config value isn't really possible
        if (level.isClientSide || (level.getGameTime() % 20L) != Util.mod(hashCode(), 20)) return;
        resetLimits();
        if (emc.equals(BigInteger.ZERO)) return;
        ServerPlayer player = Util.getPlayer(level, owner);
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
        if (provider == null) return;

        BigInteger toAdd = getMatter() == Matter.FINAL ? emc : remainingEMC.min(emc);
        provider.setEmc(provider.getEmc().add(toAdd));
        emc = emc.subtract(toAdd).max(BigInteger.ZERO);
        if (player != null) provider.syncEmc(player);
        markDirty(level, pos);
        emc = BigInteger.ZERO;
    }

    private void resetLimits() {
        Matter m = getMatter();
        remainingEMC    = m.getEMCLinkEMCLimit();
        remainingImport = remainingExport = m.getEMCLinkItemLimit();
        remainingFluid  = m.getEMCLinkFluidLimit() * 81L;
    }

    private void setInternalItem(ItemStack stack) {
        itemStack = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(stack.copyWithCount(1))).createStack();
        markDirty();
    }

    @Override
    public void handlePlace(@Nullable LivingEntity livingEntity, ItemStack stack) {
        super.handlePlace(livingEntity, stack);
        resetLimits();
    }

    @Override
    public Matter getMatter() {
        if (level != null) {
            BlockEMCLink block = (BlockEMCLink) getBlockState().getBlock();
            if (block.getMatter() != matter) setMatter(block.getMatter());
            return matter;
        }
        return Matter.BASIC;
    }

    private void setMatter(Matter matter) {
        this.matter = matter;
    }

    public InteractionResult handleActivation(Player player, InteractionHand hand) {
        ItemStack inHand = player.getItemInHand(hand);
        ItemHandler itemHandler = getItemHandlerCapability();
        FluidHandler fluidHandler = fabricFluids;

        if(!super.handleActivation(player, ActivationType.CHECK_OWNERSHIP)) return InteractionResult.CONSUME;

        if (player.isCrouching()) {
            if (itemStack.isEmpty()) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_NOT_SET.translateColored(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
            if (inHand.isEmpty()) {
                setInternalItem(ItemStack.EMPTY);
                player.displayClientMessage(Lang.Blocks.EMC_LINK_CLEARED.translateColored(ChatFormatting.RED), true);
                return InteractionResult.SUCCESS;
            }
        }

        if (itemStack.isEmpty()) {
            if (inHand.isEmpty()) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_NOT_SET.translateColored(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
            if (!itemHandler.isItemValid(0, inHand)) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_EMPTY_HAND.translateColored(ChatFormatting.RED, Component.translatable(itemStack.getItem().toString()).setStyle(ColorStyle.BLUE)), true);
                return InteractionResult.CONSUME;
            }
            setInternalItem(inHand);
            player.displayClientMessage(Lang.Blocks.EMC_LINK_SET.translateColored(ChatFormatting.GREEN, Component.literal(itemStack.getItem().toString()).setStyle(ColorStyle.BLUE)), true);
            return InteractionResult.SUCCESS;
        }

        Fluid fluid = fluidHandler.getFluid();
        if (fluid != null && inHand.is(net.minecraft.world.item.Items.BUCKET)) {
            try (var transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                long bucket = net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET;
                long extracted = fluidHandler.extract(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(fluid), bucket, transaction);
                if (extracted != bucket) {
                    player.displayClientMessage(Lang.Blocks.EMC_LINK_NOT_ENOUGH_EMC.translateColored(ChatFormatting.RED,
                        Component.literal(EMCFormat.format(BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(itemStack))))), true);
                    return InteractionResult.FAIL;
                }
                player.setItemInHand(hand, net.minecraft.world.item.ItemUtils.createFilledResult(inHand, player, new ItemStack(fluid.getBucket())));
                transaction.commit();
                return InteractionResult.SUCCESS;
            }
        }

        if (inHand.isEmpty() || itemStack.is(inHand.getItem())) {
            if (Config.server.limitEmcLinkVendor.get() && remainingExport <= 0) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_NO_EXPORT_REMAINING.translateColored(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
            ItemStack extract = itemHandler.extractItemInternal(0, itemStack.getMaxStackSize(), false, Config.server.limitEmcLinkVendor.get());
            if (extract.isEmpty()) {
                player.displayClientMessage(Lang.Blocks.EMC_LINK_NOT_ENOUGH_EMC.translateColored(ChatFormatting.RED, Component.literal(EMCFormat.format(BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(itemStack)))).setStyle(ColorStyle.GREEN)), true);
                return InteractionResult.CONSUME;
            }
            if (!player.getInventory().add(extract)) player.drop(extract, false);
            return InteractionResult.SUCCESS;
        }

        player.displayClientMessage(Lang.Blocks.EMC_LINK_EMPTY_HAND.translateColored(ChatFormatting.RED), true);
        return InteractionResult.CONSUME;
    }

    /****************
     * Capabilities *
     ****************/

    private class EMCHandler implements IEmcStorageBigInteger {
        @Override
        public BigInteger getStoredEmcBigInteger() {
            return BigInteger.ZERO;
        }
        @Override
        public BigInteger getMaximumEmcBigInteger() {
            return getMatter().getEMCLinkEMCLimit();
        }

        @Override
        public BigInteger extractEmcBigInteger(BigInteger value, EmcAction action) {
            return emc.compareTo(BigInteger.ZERO) < 0 ? insertEmcBigInteger(value.negate(), action) : value;
        }

        @Override
        public BigInteger insertEmcBigInteger(BigInteger value, EmcAction action) {
            boolean isFinal = getMatter() == Matter.FINAL;
            BigInteger v = isFinal ? value : remainingEMC.min(value);

            if (value.compareTo(BigInteger.ZERO) < 0) return BigInteger.ZERO;
            if (action.execute()) {
                if (!isFinal) remainingEMC = remainingEMC.subtract(v);
                emc = emc.add(v);
                markDirty();
            }

            return v;
        }
    }

    EMCHandler getEMCHandlerCapability() {
        return (EMCHandler) WorldHelper.getCapability(level, PECapabilities.EMC_STORAGE_CAPABILITY.lookup(), worldPosition, getBlockState(), this, null);
    }

    private class ItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return getMatter().getEMCLinkInventorySize();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot != 0 || itemStack.isEmpty()) return ItemStack.EMPTY;
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
            if (provider == null) return ItemStack.EMPTY;
            BigInteger val = BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(itemStack));
            if(val.equals(BigInteger.ZERO)) return ItemStack.EMPTY;
            BigInteger maxCount = provider.getEmc().divide(val).min(BigInteger.valueOf(Integer.MAX_VALUE));
            int count = maxCount.intValueExact();
            if (count <= 0) return ItemStack.EMPTY;

            return itemStack.copyWithCount(count);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            boolean isFinal = getMatter() == Matter.FINAL;
            if (slot == 0 || (!isFinal && remainingImport <= 0) || owner == null || stack.isEmpty() || !isItemValid(slot, stack) || Util.getPlayer(owner) == null) return stack;

            int count = stack.getCount();
            stack = stack.copyWithCount(1);

            if (count <= 0) return stack;

            ItemInfo info = ItemInfo.fromStack(stack);
            if(getFilterStatus() && !IEMCProxy.INSTANCE.getPersistentInfo(info).equals(info)) return stack;

            int insertCount = isFinal ? count : Math.min(count, remainingImport);
            if (!simulate) {
                long itemValue = IEMCProxy.INSTANCE.getSellValue(stack);
                @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
                if (provider == null) return stack;
                BigInteger totalValue = BigInteger.valueOf(itemValue).multiply(BigInteger.valueOf(insertCount));
                provider.setEmc(provider.getEmc().add(totalValue));
                ServerPlayer player = Util.getPlayer(owner);
                if (player != null) {
                    if (provider.addKnowledge(stack))
                        provider.syncKnowledgeChange(player, IEMCProxy.INSTANCE.getPersistentInfo(info), true);
                    provider.syncEmc(player);
                }
                if(!isFinal) remainingImport -= insertCount;
                markDirty();
            }

            if (insertCount == count) return ItemStack.EMPTY;

            stack.setCount(count - insertCount);
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return extractItemInternal(slot, amount, simulate, true);
        }

        public ItemStack extractItemInternal(int slot, int amount, boolean simulate, boolean limit) {
            boolean isFinal = getMatter() == Matter.FINAL;
            if (slot != 0 || (!isFinal && remainingExport <= 0) || owner == null || itemStack.isEmpty() || Util.getPlayer(owner) == null) return ItemStack.EMPTY;

            BigInteger itemValue = BigInteger.valueOf(IEMCProxy.INSTANCE.getValue(itemStack));
            if(itemValue.equals(BigInteger.ZERO)) return ItemStack.EMPTY;
            @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
            if (provider == null) return ItemStack.EMPTY;
            BigInteger maxCount = provider.getEmc().divide(itemValue).min(BigInteger.valueOf(Integer.MAX_VALUE));
            int extractCount = Math.min(amount, limit && !isFinal ? Math.min(maxCount.intValueExact(), remainingExport) : maxCount.intValueExact());
            if (extractCount <= 0) return ItemStack.EMPTY;

            ItemStack r = IEMCProxy.INSTANCE.getPersistentInfo(ItemInfo.fromStack(itemStack.copy())).createStack();
            r.setCount(extractCount);
            if (simulate) return r;

            BigInteger totalPrice = itemValue.multiply(BigInteger.valueOf(extractCount));
            provider.setEmc(provider.getEmc().subtract(totalPrice));
            ServerPlayer player = Util.getPlayer(owner);
            if (player != null) provider.syncEmc(player);

            if (limit && !isFinal) remainingExport -= extractCount;
            markDirty();
            return r;
        }

        @Override
        public int getSlotLimit(int slot) {
            return getMatter().getEMCLinkItemLimit();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return IEMCProxy.INSTANCE.hasValue(stack);
        }
    }

    ItemHandler getItemHandlerCapability() {
        return getItemHandler();
    }

    private class FluidHandler implements net.fabricmc.fabric.api.transfer.v1.storage.Storage<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant>,
            net.fabricmc.fabric.api.transfer.v1.storage.StorageView<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> {
        private Fluid getFluid() {
            return !itemStack.isEmpty() && itemStack.getItem() instanceof BucketItem bucket && bucket.content != Fluids.EMPTY ? bucket.content : null;
        }
        private BigDecimal bucketPrice() {
            long full = IEMCProxy.INSTANCE.getValue(itemStack);
            long empty = IEMCProxy.INSTANCE.getValue(net.minecraft.world.item.Items.BUCKET);
            return BigDecimal.valueOf(full).subtract(BigDecimal.valueOf(empty)
                .multiply(BigDecimal.valueOf(getMatter().getFluidEfficiencyPercentage())).divide(BigDecimal.valueOf(100)));
        }
        @Override public long insert(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant resource, long maxAmount,
                net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) { return 0; }
        @Override public long extract(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant resource, long maxAmount,
                net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0 || !resource.equals(getResource())) return 0;
            var account = cool.furry.mc.neoforge.projectexpansion.platform.EmcTransactions.get(owner);
            if (account == null) return 0;
            BigDecimal price = bucketPrice();
            if (price.signum() < 0 || price.signum() == 0 && !Config.server.zeroEmcFluidsAreFree.get()) return 0;
            long amount = Math.min(maxAmount, Math.max(0, remainingFluid));
            long bucket = net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET;
            if (price.signum() > 0) amount = new BigDecimal(account.balance()).multiply(BigDecimal.valueOf(bucket))
                .divide(price, 0, RoundingMode.FLOOR).min(BigDecimal.valueOf(amount)).longValue();
            if (amount <= 0) return 0;
            BigInteger cost = price.multiply(BigDecimal.valueOf(amount)).divide(BigDecimal.valueOf(bucket), 0, RoundingMode.CEILING).toBigIntegerExact();
            account.change(cost.negate(), transaction);
            transferLimits.updateSnapshots(transaction);
            if (getMatter() != Matter.FINAL) remainingFluid -= amount;
            return amount;
        }
        @Override public net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant getResource() {
            Fluid fluid = getFluid();
            return fluid == null ? net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.blank() : net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(fluid);
        }
        @Override public boolean isResourceBlank() { return getResource().isBlank(); }
        @Override public long getAmount() {
            if (isResourceBlank()) return 0;
            var account = cool.furry.mc.neoforge.projectexpansion.platform.EmcTransactions.get(owner);
            if (account == null) return 0;
            BigDecimal price = bucketPrice();
            if (price.signum() < 0 || price.signum() == 0 && !Config.server.zeroEmcFluidsAreFree.get()) return 0;
            long quota = Math.max(0, remainingFluid);
            return price.signum() == 0 ? quota : new BigDecimal(account.balance())
                .multiply(BigDecimal.valueOf(net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET))
                .divide(price, 0, RoundingMode.FLOOR).min(BigDecimal.valueOf(quota)).longValue();
        }
        @Override public long getCapacity() { return Math.max(0, remainingFluid); }
        @Override public java.util.Iterator<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant>> iterator() {
            return java.util.Collections.<net.fabricmc.fabric.api.transfer.v1.storage.StorageView<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant>>singleton(this).iterator();
        }
    }
}
