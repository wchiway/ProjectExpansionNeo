package cool.furry.mc.neoforge.projectexpansion.mixin;

import cool.furry.mc.neoforge.projectexpansion.events.ItemEvents;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class ItemAttributesMixin {
    @Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlot;Ljava/util/function/BiConsumer;)V", at = @At("TAIL"))
    private void projectexpansion$slotModifiers(EquipmentSlot slot, BiConsumer<Holder<Attribute>, AttributeModifier> consumer, CallbackInfo ci) {
        ItemEvents.addProtection((ItemStack) (Object) this, EquipmentSlotGroup.bySlot(slot), consumer);
    }
    @Inject(method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V", at = @At("TAIL"))
    private void projectexpansion$groupModifiers(EquipmentSlotGroup group, BiConsumer<Holder<Attribute>, AttributeModifier> consumer, CallbackInfo ci) {
        ItemEvents.addProtection((ItemStack) (Object) this, group, consumer);
    }
}
