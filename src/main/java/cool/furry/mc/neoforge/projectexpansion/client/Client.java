package cool.furry.mc.neoforge.projectexpansion.client;

import cool.furry.mc.neoforge.projectexpansion.events.ClientEvents;
import net.fabricmc.api.ClientModInitializer;

public final class Client implements ClientModInitializer {
    @Override
    public void onInitializeClient() { ClientEvents.register(); }
}
