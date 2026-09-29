package cool.furry.mc.neoforge.projectexpansion.item;

import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.util.ContainerData;
import cool.furry.mc.neoforge.projectexpansion.util.ITransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.util.Lang;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ItemArcaneTransmutationTablet extends Item implements ITransmutationTablet {
    public ItemArcaneTransmutationTablet() {
        super(new Properties().rarity(Rarity.RARE).stacksTo(1).fireResistant());
    }

    @Environment(EnvType.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> list, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, list, tooltipFlag);
        list.add(Lang.Items.ARCANE_TRANSMUTATION_TABLET_TOOLTIP.translateColored(ChatFormatting.GRAY));
        list.add(Lang.SEE_WIKI.translateColored(ChatFormatting.AQUA));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide) {
            openContainer(player, hand, player.getInventory().selected);
        }

        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public void openContainer(Player player, InteractionHand hand, int selected) {
        cool.furry.mc.neoforge.projectexpansion.platform.Menus.open(player, new Provider(hand), (buf) -> ContainerData.inHand(buf, hand, selected));
    }

    @Override
    public void openContainer(Player player) {
        cool.furry.mc.neoforge.projectexpansion.platform.Menus.open(player, new Provider(null), ContainerData::noHand);
    }

    private record Provider(@Nullable InteractionHand hand) implements MenuProvider {
        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int windowId, Inventory inventory, Player player) {
            IKnowledgeProvider provider = PECapabilities.KNOWLEDGE_CAPABILITY.find(player);
            if (provider == null) return null;
            return new ContainerArcaneTransmutationTablet(windowId, inventory, provider, hand, inventory.selected);
        }

        @Override
        public Component getDisplayName() {
            return Lang.GUI.ARCANE_TRANSMUTATION_TABLET.translate();
        }

    }
}
