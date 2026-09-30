package cool.furry.mc.neoforge.projectexpansion.item;

import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.registries.DataComponentTypes;
import cool.furry.mc.neoforge.projectexpansion.util.ColorStyle;
import cool.furry.mc.neoforge.projectexpansion.util.EMCFormat;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import cool.furry.mc.neoforge.projectexpansion.util.Util;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

public class ItemInfiniteFuel extends Item {

    public ItemInfiniteFuel() {
        super(new Properties().stacksTo(1).rarity(Rarity.RARE));
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, list, tooltipFlag);
        list.add(Lang.Items.INFINITE_FUEL_TOOLTIP.translateColored(ChatFormatting.GRAY));
        list.add(Lang.COST.translateColored(ChatFormatting.RED, EMCFormat.getComponent(Config.server.infiniteFuelCost.get()).setStyle(ColorStyle.GRAY)));
    }

    /** Pays for one active furnace tick, retaining less than one prepaid EMC on the item. */
    public boolean consumeTick(ItemStack stack, int ticksPerUse) {
        @Nullable UUID owner = getOwner(stack);
        @Nullable IKnowledgeProvider provider = owner == null ? null : Util.getKnowledgeProvider(owner);
        int cost = Config.server.infiniteFuelCost.get();
        if (provider == null || cost <= 0 || ticksPerUse <= 0) return false;

        long credit = stack.getOrDefault(DataComponentTypes.FUEL_CREDIT.get(), DataComponentTypes.FuelCredit.EMPTY).rescale(ticksPerUse);
        long charge = Math.max(0, (cost - credit + ticksPerUse - 1) / ticksPerUse);
        BigInteger amount = BigInteger.valueOf(charge);
        if (provider.getEmc().compareTo(amount) < 0) return false;
        if (charge > 0) {
            provider.setEmc(provider.getEmc().subtract(amount));
            @Nullable ServerPlayer player = Util.getPlayer(owner);
            if (player != null) provider.syncEmc(player);
        }
        int remaining = (int) (credit + charge * ticksPerUse - cost);
        stack.set(DataComponentTypes.FUEL_CREDIT.get(), new DataComponentTypes.FuelCredit(remaining, ticksPerUse));
        return true;
    }

    private static @Nullable UUID getOwner(ItemStack stack) {
        @Nullable DataComponentTypes.OwnerData owner = stack.get(DataComponentTypes.OWNER.get());
        return owner == null ? null : owner.uuid();
    }
}
