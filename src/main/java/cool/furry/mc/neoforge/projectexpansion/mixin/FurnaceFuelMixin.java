package cool.furry.mc.neoforge.projectexpansion.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.item.ItemInfiniteFuel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.SmokerBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceFuelMixin {
    // Loading a previously paid burn must not charge EMC again.
    @Inject(method = "getBurnDuration", at = @At("HEAD"), cancellable = true)
    private void projectexpansion$fuelDuration(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.getItem() instanceof ItemInfiniteFuel) cir.setReturnValue(Config.server.infiniteFuelBurnTime.get());
    }

    @Inject(method = "isFuel", at = @At("HEAD"), cancellable = true)
    private static void projectexpansion$acceptInfiniteFuel(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getItem() instanceof ItemInfiniteFuel) cir.setReturnValue(true);
    }

    // This call is behind canBurn: idle or blocked furnaces never charge, and each paid grant lasts one tick.
    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/entity/AbstractFurnaceBlockEntity;getBurnDuration(Lnet/minecraft/world/item/ItemStack;)I"))
    private static int projectexpansion$payForTick(AbstractFurnaceBlockEntity furnace, ItemStack stack, Operation<Integer> original) {
        if (!(stack.getItem() instanceof ItemInfiniteFuel fuel)) return original.call(furnace, stack);
        int period = Config.server.infiniteFuelBurnTime.get();
        if (furnace instanceof BlastFurnaceBlockEntity || furnace instanceof SmokerBlockEntity) period = Math.max(1, period / 2);
        return fuel.consumeTick(stack, period) ? 1 : 0;
    }

    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private static void projectexpansion$keepInfiniteFuel(ItemStack stack, int amount, Operation<Void> original) {
        if (!(stack.getItem() instanceof ItemInfiniteFuel)) original.call(stack, amount);
    }
}
