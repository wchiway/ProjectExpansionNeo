package cool.furry.mc.neoforge.projectexpansion.events;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.client.Keybinds;
import cool.furry.mc.neoforge.projectexpansion.gui.*;
import cool.furry.mc.neoforge.projectexpansion.net.PacketHandler;
import cool.furry.mc.neoforge.projectexpansion.net.packets.to_server.PacketOpenTransmutationTablet;
import cool.furry.mc.neoforge.projectexpansion.registries.MenuTypes;
import moze_intel.projecte.network.PENetwork;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.gui.screens.MenuScreens;

public final class ClientEvents {
    public static void register() {
        MenuScreens.register(MenuTypes.COLLECTOR_TIER_1.get(), GUICollector.Tier1::new);
        MenuScreens.register(MenuTypes.COLLECTOR_TIER_2.get(), GUICollector.Tier2::new);
        MenuScreens.register(MenuTypes.COLLECTOR_TIER_3.get(), GUICollector.Tier3::new);
        MenuScreens.register(MenuTypes.CONDENSER_MK3_INPUT.get(), GUICondenserMK3Input::new);
        MenuScreens.register(MenuTypes.CONDENSER_MK3_OUTPUT.get(), GUICondenserMK3Output::new);
        MenuScreens.register(MenuTypes.ARCANE_TRANSMUTATION_TABLET.get(), GUIArcaneTransmutationTablet::new);
        Keybinds.register();
        PacketHandler.registerClientReceivers();
        RenderingEvent.registerRenderers();
        var phase = Main.rl("after_projecte");
        ItemTooltipCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, phase);
        ItemTooltipCallback.EVENT.register(phase, ItemTooltipEvents::itemTooltipEvent);
        HudRenderCallback.EVENT.register(EMCDisplay.INSTANCE::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> EMCDisplay.reset());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EMCDisplay.onTick();
            if (Keybinds.OPEN_TRANSMUTATION_TABLET.consumeClick() && client.screen == null && client.player != null) {
                PENetwork.sendToServer(PacketOpenTransmutationTablet.INSTANCE);
            }
        });
    }
}
