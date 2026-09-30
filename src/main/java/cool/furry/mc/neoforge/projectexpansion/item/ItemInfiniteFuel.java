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

    /**
     * Burn time a single use grants the host furnace, or 0 while the bound owner cannot pay for it.
     * <p>
     * The item is never consumed, so furnaces that use it must call {@link #consumeCharge(ItemStack)} exactly once
     * when the burn starts. Their fuel duration is recomputed on every start, so a burn that keeps running does not
     * charge again.
     */
    public int getBurnTime(ItemStack stack) {
        @Nullable IKnowledgeProvider provider = getOwnerProvider(stack);
        if (provider == null) return 0;
        if (Config.server.infiniteFuelCost.get() == 0 || Config.server.infiniteFuelBurnTime.get() == 0) return 0;
        return provider.getEmc().compareTo(BigInteger.valueOf(Config.server.infiniteFuelCost.get())) < 0 ? 0 : Config.server.infiniteFuelBurnTime.get();
    }

    /**
     * Charges the bound owner once for a single use. Unbound items and owners without a knowledge provider are ignored,
     * matching {@link #getBurnTime(ItemStack)}, which refuses to burn in the same situations.
     */
    public void consumeCharge(ItemStack stack) {
        @Nullable UUID owner = getOwner(stack);
        if (owner == null) return;
        @Nullable IKnowledgeProvider provider = Util.getKnowledgeProvider(owner);
        if (provider == null) return;
        provider.setEmc(provider.getEmc().subtract(BigInteger.valueOf(Config.server.infiniteFuelCost.get())));
        @Nullable ServerPlayer player = Util.getPlayer(owner);
        if (player != null) provider.syncEmc(player);
    }

    private static @Nullable UUID getOwner(ItemStack stack) {
        @Nullable DataComponentTypes.OwnerData owner = stack.get(DataComponentTypes.OWNER.get());
        return owner == null ? null : owner.uuid();
    }

    private static @Nullable IKnowledgeProvider getOwnerProvider(ItemStack stack) {
        @Nullable UUID owner = getOwner(stack);
        return owner == null ? null : Util.getKnowledgeProvider(owner);
    }
}
