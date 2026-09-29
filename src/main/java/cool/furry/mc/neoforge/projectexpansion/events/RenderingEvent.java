package cool.furry.mc.neoforge.projectexpansion.events;

import cool.furry.mc.neoforge.projectexpansion.registries.BlockEntityTypes;
import cool.furry.mc.neoforge.projectexpansion.rendering.ChestRenderer;
import cool.furry.mc.neoforge.projectexpansion.util.IChestLike;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class RenderingEvent {
    public static void registerRenderers() {
        registerChest(BlockEntityTypes.ADVANCED_ALCHEMICAL_CHEST.get());
        registerChest(BlockEntityTypes.CONDENSER_MK3.get());
    }
    private static <BE extends BlockEntity & IChestLike> void registerChest(BlockEntityType<BE> type) {
        BlockEntityRendererRegistry.register(type, context -> new ChestRenderer<>(context, type));
    }
}
