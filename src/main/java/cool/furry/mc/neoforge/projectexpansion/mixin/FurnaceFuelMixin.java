package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.item.ItemInfiniteFuel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceFuelMixin {
    @Inject(method = "getBurnDuration", at = @At("HEAD"), cancellable = true)
    private void projectexpansion$fuelDuration(ItemStack stack, CallbackInfoReturnable<Integer> cir) {
        if (stack.getItem() instanceof ItemInfiniteFuel fuel) cir.setReturnValue(fuel.getBurnTime(stack, null));
    }
}
