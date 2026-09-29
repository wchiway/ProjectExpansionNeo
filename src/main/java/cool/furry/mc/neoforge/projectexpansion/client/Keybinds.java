package cool.furry.mc.neoforge.projectexpansion.client;

import moze_intel.projecte.utils.text.PELang;
import net.minecraft.client.KeyMapping;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static boolean REGISTERED = false;
    public static KeyMapping OPEN_TRANSMUTATION_TABLET;

    public static void register() {
        OPEN_TRANSMUTATION_TABLET = new KeyMapping(
                "key.projecte.curios.open_transmutation_tablet",
                GLFW.GLFW_KEY_K,
                PELang.PROJECTE.getTranslationKey()
        );
        KeyBindingHelper.registerKeyBinding(OPEN_TRANSMUTATION_TABLET);
        REGISTERED = true;
    }
}
