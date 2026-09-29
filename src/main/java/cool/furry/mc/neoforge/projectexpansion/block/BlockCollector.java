package cool.furry.mc.neoforge.projectexpansion.block;

import com.mojang.serialization.MapCodec;
import cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityCollector;
import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.neoforge.projectexpansion.registries.BlockTypes;
import cool.furry.mc.neoforge.projectexpansion.util.*;
import moze_intel.projecte.api.capabilities.PECapabilities;
import moze_intel.projecte.api.capabilities.item.IItemEmcHolder;
import moze_intel.projecte.gameObjs.IMatterType;
import moze_intel.projecte.gameObjs.block_entities.CollectorMK1BlockEntity;
import moze_intel.projecte.gameObjs.blocks.BlockDirection;
import moze_intel.projecte.gameObjs.blocks.IMatterBlock;
import moze_intel.projecte.utils.MathUtils;
import moze_intel.projecte.utils.WorldHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import cool.furry.mc.neoforge.projectexpansion.platform.InventoryCapabilities;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;

@SuppressWarnings("deprecation")
public class BlockCollector extends BlockDirection implements IHasMatter, EntityBlock, IMatterBlock {
    private final Matter matter;

    public BlockCollector(BlockBehaviour.Properties properties, Matter matter) {
        super(properties.pushReaction(PushReaction.BLOCK).mapColor(matter.mapColor == null ? MapColor.NONE : matter.mapColor.get()));
        this.matter = matter;
    }

    public static BlockBehaviour.Properties getProperties(Matter matter) {
        return Block.Properties.of().strength(getDestroyTime(matter), getExplosionResistance(matter)).requiresCorrectToolForDrops().lightLevel((state) -> Math.min(matter.ordinal(), 15));
    }

    private static float getDestroyTime(Matter matter) {
        return switch (matter) {
            case BASIC -> 0.3F;
            case DARK -> 1_000_000;
            default -> 2_000_000;
        };
    }

    private static float getExplosionResistance(Matter matter) {
        return switch (matter) {
            case BASIC -> 0.9F;
            case DARK -> 3_000_000;
            default -> 6_000_000;
        };
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BlockEntityCollector(pos, state);
    }

    @Override
    public Matter getMatter() {
        return matter;
    }

    @Override
    public IMatterType getMatterType() {
        return Util.getMatterForProjectE(getMatter());
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, list, tooltipFlag);
        list.add(Lang.Blocks.COLLECTOR_TOOLTIP.translateColored(ChatFormatting.GRAY));
        list.add(Lang.Blocks.COLLECTOR_EMC.translateColored(ChatFormatting.GRAY, EMCFormat.getComponent(getMatter().getCollectorOutputForTicks(Config.server.tickDelay.get())).setStyle(ColorStyle.GREEN)));
        if(stack.getCount() > 1) {
            list.add(Lang.Blocks.COLLECTOR_STACK_EMC.translateColored(ChatFormatting.GRAY, EMCFormat.getComponent(getMatter().getCollectorOutputForTicks(Config.server.tickDelay.get()).multiply(BigDecimal.valueOf(stack.getCount()))).setStyle(ColorStyle.GREEN)));
        }
        list.add(Lang.Blocks.COLLECTOR_MAX_STORAGE.translateColored(ChatFormatting.GRAY, EMCFormat.getComponent(Fuel.getCollectorEMCLimit(getMatter())).setStyle(ColorStyle.GREEN)));
        list.add(Lang.SEE_WIKI.translateColored(ChatFormatting.AQUA));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == BlockEntityTypes.COLLECTOR.get() && !level.isClientSide) return BlockEntityCollector::tickServer;
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntityCollector collector = WorldHelper.getBlockEntity(BlockEntityCollector.class, level, pos);
        if (collector == null) return InteractionResult.FAIL;
        cool.furry.mc.neoforge.projectexpansion.platform.Menus.open(player, collector, pos);

        return InteractionResult.CONSUME;
    }

    @Override
    @Deprecated
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    @Deprecated
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        CollectorMK1BlockEntity collector = WorldHelper.getBlockEntity(CollectorMK1BlockEntity.class, level, pos, true);
        if (collector == null) {
            //If something went wrong fallback to default implementation
            return super.getAnalogOutputSignal(state, level, pos);
        }
        moze_intel.projecte.api.item_handlers.IItemHandler handler = WorldHelper.getItemHandler(level, pos, state, collector, Direction.UP);
        if (handler == null) {
            //If something went wrong fallback to default implementation
            return super.getAnalogOutputSignal(state, level, pos);
        }
        ItemStack charging = handler.getStackInSlot(CollectorMK1BlockEntity.UPGRADING_SLOT);
        if (charging.isEmpty()) {
            return MathUtils.scaleToRedstone(collector.getStoredEmc(), collector.getMaximumEmc());
        }
        IItemEmcHolder emcHolder = PECapabilities.EMC_HOLDER_ITEM_CAPABILITY.find(charging);
        if (emcHolder != null) {
            return MathUtils.scaleToRedstone(emcHolder.getStoredEmc(charging), emcHolder.getMaximumEmc(charging));
        }
        return MathUtils.scaleToRedstone(collector.getStoredEmc(), collector.getEmcToNextGoal());
    }

    @Override
    @Deprecated
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntityCollector collector = WorldHelper.getBlockEntity(BlockEntityCollector.class, level, pos);
            if (collector != null) {
                //Clear the ghost slot so calling super doesn't drop the item in it
                collector.clearLocked();
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockTypes.COLLECTOR.get();
    }
}
