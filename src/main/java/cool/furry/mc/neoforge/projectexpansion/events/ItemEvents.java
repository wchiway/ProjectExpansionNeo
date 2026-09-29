package cool.furry.mc.neoforge.projectexpansion.events;

import cool.furry.mc.neoforge.projectexpansion.registries.Attributes;
import cool.furry.mc.neoforge.projectexpansion.util.SunExposureHelper;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import java.util.function.BiConsumer;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

public final class ItemEvents {
    public static void addProtection(ItemStack stack, EquipmentSlotGroup group, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        if (stack.is(SunExposureHelper.PROTECTIVE_ITEMS) && stack.getItem() instanceof ArmorItem armor && group.test(armor.getEquipmentSlot())) {
            EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(armor.getEquipmentSlot());
            consumer.accept(Attributes.SUN_EXPOSURE_PROTECTION, new AttributeModifier(
                Util.SUN_EXPOSURE_PROTECTION.apply(slot.getSerializedName()), 0.25D, AttributeModifier.Operation.ADD_VALUE));
        }
    }
}
