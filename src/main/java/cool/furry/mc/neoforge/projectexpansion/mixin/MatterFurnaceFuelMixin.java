package cool.furry.mc.neoforge.projectexpansion.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.item.ItemInfiniteFuel;
import moze_intel.projecte.gameObjs.block_entities.DMFurnaceBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DMFurnaceBlockEntity.class)
public abstract class MatterFurnaceFuelMixin {
    // The duration query also runs on load, so payment belongs only to the guarded tick call below.
    @Inject(method = "getItemBurnTime", at = @At("HEAD"), cancellable = true)
    private void projectexpansion$infiniteFuelBurnTime(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.getItem() instanceof ItemInfiniteFuel) cir.setReturnValue(Config.server.infiniteFuelBurnTime.get());
    }

    @WrapOperation(method = "tickServer", at = @At(value = "INVOKE",
            target = "Lmoze_intel/projecte/gameObjs/block_entities/DMFurnaceBlockEntity;getItemBurnTime(Lnet/minecraft/world/item/ItemStack;)I"))
    private static int projectexpansion$payForTick(DMFurnaceBlockEntity furnace, ItemStack stack, Operation<Integer> original) {
        if (!(stack.getItem() instanceof ItemInfiniteFuel fuel)) return original.call(furnace, stack);
        return fuel.consumeTick(stack, Config.server.infiniteFuelBurnTime.get()) ? 1 : 0;
    }

    @WrapOperation(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private static void projectexpansion$keepInfiniteFuel(ItemStack stack, int amount, Operation<Void> original) {
        if (!(stack.getItem() instanceof ItemInfiniteFuel)) original.call(stack, amount);
    }
}
