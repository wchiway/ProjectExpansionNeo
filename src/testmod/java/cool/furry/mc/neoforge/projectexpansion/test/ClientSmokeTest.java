package cool.furry.mc.neoforge.projectexpansion.test;

import cool.furry.mc.neoforge.projectexpansion.Main;
import cool.furry.mc.neoforge.projectexpansion.client.Keybinds;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Runs only in the isolated clientSmoke profile, then closes the test client. */
public final class ClientSmokeTest implements ClientModInitializer {
    private int ticks;
    private boolean complete;

    @Override
    public void onInitializeClient() {
        if (!Boolean.getBoolean("projectexpansion.clientSmoke")) return;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (complete || ++ticks < 100 || client.getOverlay() != null || client.screen == null) return;
            if (!Keybinds.REGISTERED) throw new IllegalStateException("Project Expansion key binding was not registered");
            int models = 0;
            for (var item : BuiltInRegistries.ITEM) {
                if (!BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(Main.MOD_ID)) continue;
                var model = client.getItemRenderer().getModel(new ItemStack(item), null, null, 0);
                if (model == client.getModelManager().getMissingModel()) {
                    throw new IllegalStateException("Missing item model: " + BuiltInRegistries.ITEM.getKey(item));
                }
                models++;
            }
            if (models == 0) throw new IllegalStateException("No Project Expansion items loaded");
            Main.Logger.info("PROJECTEXPANSION CLIENT SMOKE PASSED: {} item models and client initialization", models);
            complete = true;
            client.stop();
        });
    }
}
