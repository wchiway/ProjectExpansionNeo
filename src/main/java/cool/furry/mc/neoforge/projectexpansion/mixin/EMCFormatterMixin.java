package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.util.EMCFormat;
import moze_intel.projecte.utils.EMCHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.text.NumberFormat;

// This mixin changes ProjectE's formatting to ours
@SuppressWarnings("unused")
@Mixin(value = EMCHelper.class, remap = false)
public class EMCFormatterMixin {
    @Final
    @Shadow
    @Mutable
    private static NumberFormat EMC_FORMATTER;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void projectexpansion$formatter(CallbackInfo ci) {
        EMC_FORMATTER = EMCFormat.INSTANCE;
    }
}
