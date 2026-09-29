package cool.furry.mc.neoforge.projectexpansion.events;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.registries.Enchantments;
import cool.furry.mc.neoforge.projectexpansion.util.ColorStyle;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import moze_intel.projecte.api.ItemInfo;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.proxy.IEMCProxy;
import moze_intel.projecte.config.ProjectEConfig;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import moze_intel.projecte.utils.EMCHelper;
import moze_intel.projecte.utils.text.PELang;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.fabricmc.api.EnvType;

import java.util.concurrent.atomic.AtomicInteger;

public class ItemTooltipEvents {
    // we need to be lower priority than ProjectE's listener so the EMC component is present when we get the event

    public static void itemTooltipEvent(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, net.minecraft.world.item.TooltipFlag flag, java.util.List<Component> lines) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (stack.isEmpty()|| player == null || player.isDeadOrDying()) {
            return;
        }

        learnedTooltip: if(Config.client.enableLearnedTooltip.get() && (!ProjectEConfig.client.shiftEmcToolTips.get() || Screen.hasShiftDown())) {
            boolean hasValue = IEMCProxy.INSTANCE.hasValue(stack);
            if (!hasValue) {
                break learnedTooltip;
            }

            IKnowledgeProvider provider = Util.getKnowledgeProvider(player);
            if (provider == null) {
                break learnedTooltip;
            }

            boolean hasKnowledge = provider.hasKnowledge(ItemInfo.fromStack(stack));
            long value = IEMCProxy.INSTANCE.getValue(stack);
            AtomicInteger index = new AtomicInteger(-1);
            AtomicInteger peTransmutableIndex = new AtomicInteger(-1);
            for (Component c : lines) {
                if (c.getString().equals(EMCHelper.getEmcTextComponent(value, 1).getString())) {
                    index.set(lines.indexOf(c));
                    continue;
                }

                if (c.getString().equals(I18n.get(PELang.EMC_HAS_KNOWLEDGE.getTranslationKey()))) {
                    peTransmutableIndex.set(lines.indexOf(c));
                }
            }

            // attempt to add a minimal notice
            if (index.get() != -1) {
                lines.set(index.get(), lines.get(index.get()).copy().append(Component.literal(" (").setStyle(ColorStyle.WHITE)).append(hasKnowledge ?
                        Component.literal("✓").setStyle(ColorStyle.GREEN) : Component.literal("✗").setStyle(ColorStyle.RED)
                ).append(Component.literal(")").setStyle(ColorStyle.WHITE)));
            } else {
                // if we can't find an existing EMC element, add a new more detailed element
                lines.add(hasKnowledge ?
                    Lang.LEARNED.translateColored(ChatFormatting.GREEN) : Lang.NOT_LEARNED.translateColored(ChatFormatting.RED)
                );
            }


            if (peTransmutableIndex.get() != -1) {
                lines.remove(peTransmutableIndex.get());
            }
        }

        boolean hasEnch = EnchantmentHelper.getItemEnchantmentLevel(player.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.ALCHEMICAL_COLLECTION), stack) > 0;
        if(hasEnch) {
            boolean enabled = stack.getOrDefault(PEDataComponentTypes.ACTIVE.get(), true);
            lines.add(Lang.ALCHEMICAL_COLLECTION.translate(enabled ? Lang.ENABLED.translateColored(ChatFormatting.GREEN) : Lang.DISABLED.translateColored(ChatFormatting.RED)));
        }

    }
}
