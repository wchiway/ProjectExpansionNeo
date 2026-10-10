package cool.furry.mc.neoforge.projectexpansion.test;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.gui.GUIArcaneTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.gui.container.ContainerArcaneTransmutationTablet;
import java.util.ArrayList;
import java.util.List;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Exercises real screen mouse routing without sending the probe clicks to the server. */
final class ArcaneTabletClientSmokeTest {
    private int ticks;
    private boolean started;

    boolean tick(Minecraft client) {
        if (++ticks > 6000) throw new IllegalStateException("Timed out loading the arcane tablet test world");
        if (!started) {
            started = true;
            var settings = new LevelSettings("Arcane tablet smoke test", GameType.CREATIVE, false,
                    Difficulty.PEACEFUL, true, new GameRules(), WorldDataConfiguration.DEFAULT);
            client.createWorldOpenFlows().createFreshLevel("arcane-tablet-" + System.currentTimeMillis(), settings,
                    new WorldOptions(0L, false, false),
                    registries -> registries.registryOrThrow(Registries.WORLD_PRESET)
                            .getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(), client.screen);
            return false;
        }
        if (client.player == null || client.level == null || client.getOverlay() != null || client.screen != null) return false;

        var menu = new ContainerArcaneTransmutationTablet(0, client.player.getInventory(),
                PECapabilities.KNOWLEDGE_CAPABILITY.find(client.player));
        for (int[] size : new int[][] {{854, 480}, {427, 240}}) {
            for (int button = 0; button <= 1; button++) {
                for (int slot = 63; slot < 73; slot++) {
                    checkClick(client, menu, size, slot, button, false);
                    checkClick(client, menu, size, slot, button, true);
                }
                var screen = new ClickProbe(menu, client);
                screen.init(client, size[0], size[1]);
                screen.checkBounds(button);
                menu.setCarried(ItemStack.EMPTY);
                screen.mouseClicked(screen.left() - 80, screen.top() + 40, button);
                require(screen.clicks.equals(List.of(new Click(AbstractContainerMenu.SLOT_CLICKED_OUTSIDE, ClickType.THROW))),
                        "Clicks outside both panels must still use vanilla drop handling");
            }
        }
        Main.Logger.info("PROJECTEXPANSION ARCANE TABLET CLIENT SMOKE PASSED: input/output clicks, carried placement and panel bounds");
        return true;
    }

    private static void checkClick(Minecraft client, ContainerArcaneTransmutationTablet menu, int[] size,
                                   int slotIndex, int button, boolean carrying) {
        var screen = new ClickProbe(menu, client);
        screen.init(client, size[0], size[1]);
        menu.setCarried(carrying ? new ItemStack(Items.OAK_LOG, 8) : ItemStack.EMPTY);
        Slot slot = menu.getSlot(slotIndex);
        double x = screen.left() + slot.x + 8;
        double y = screen.top() + slot.y + 8;
        screen.mouseClicked(x, y, button);
        screen.mouseReleased(x, y, button);
        require(screen.clicks.equals(List.of(new Click(slotIndex, ClickType.PICKUP))),
                "Crafting click must target slot " + slotIndex + " instead of dropping: " + screen.clicks);
        menu.setCarried(ItemStack.EMPTY);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private record Click(int slot, ClickType type) {}

    private static final class ClickProbe extends GUIArcaneTransmutationTablet {
        private final List<Click> clicks = new ArrayList<>();

        ClickProbe(ContainerArcaneTransmutationTablet menu, Minecraft client) {
            super(menu, client.player.getInventory(), Component.empty());
        }

        @Override
        protected void slotClicked(Slot slot, int slotIndex, int button, ClickType type) {
            clicks.add(new Click(slotIndex, type));
        }

        int left() {
            return leftPos;
        }

        int top() {
            return topPos;
        }

        void checkBounds(int button) {
            for (int[] point : new int[][] {{-75, 10}, {-1, 98}, {-60, 72}, {0, 0}, {175, 216}}) {
                require(!hasClickedOutside(leftPos + point[0], topPos + point[1], leftPos, topPos, button),
                        "Visible panel pixels must not be a drop area");
            }
            for (int[] point : new int[][] {{-76, 10}, {-75, 9}, {-1, 99}, {-1, 0}, {176, 40}, {10, 217}}) {
                require(hasClickedOutside(leftPos + point[0], topPos + point[1], leftPos, topPos, button),
                        "Empty space beyond the panels must remain outside");
            }
        }
    }
}
