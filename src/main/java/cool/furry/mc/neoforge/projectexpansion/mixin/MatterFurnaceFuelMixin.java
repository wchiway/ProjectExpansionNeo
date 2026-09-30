package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.item.ItemInfiniteFuel;
import moze_intel.projecte.gameObjs.block_entities.DMFurnaceBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * ProjectE's dark and red matter furnaces take their fuel duration from the vanilla fuel map scaled by the furnace
 * tier, and they always consume one fuel item per burn. The infinite fuel is not part of that map, so both the
 * duration and the consumption have to be replaced here.
 */
@Mixin(DMFurnaceBlockEntity.class)
public abstract class MatterFurnaceFuelMixin {
    @Inject(method = "getItemBurnTime", at = @At("HEAD"), cancellable = true)
    private void projectexpansion$infiniteFuelBurnTime(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.getItem() instanceof ItemInfiniteFuel fuel) cir.setReturnValue(fuel.getBurnTime(stack));
    }

    /**
     * The only fuel consumption site in the matter furnace tick, reached only after the duration above allowed the
     * fuel to start burning. Charging here keeps the item, exactly like a vanilla furnace.
     */
    @Redirect(method = "tickServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private static void projectexpansion$keepInfiniteFuel(ItemStack stack, int amount) {
        if (stack.getItem() instanceof ItemInfiniteFuel fuel) {
            fuel.consumeCharge(stack);
        } else {
            stack.shrink(amount);
        }
    }
}
