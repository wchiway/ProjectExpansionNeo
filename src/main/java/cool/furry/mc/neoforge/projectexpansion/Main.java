package cool.furry.mc.neoforge.projectexpansion;

import cool.furry.mc.neoforge.projectexpansion.commands.CommandRegistry;
import cool.furry.mc.neoforge.projectexpansion.config.Config;
import cool.furry.mc.neoforge.projectexpansion.events.CapabilityEvents;
import cool.furry.mc.neoforge.projectexpansion.events.PlayerEvents;
import cool.furry.mc.neoforge.projectexpansion.events.ServerEvents;
import cool.furry.mc.neoforge.projectexpansion.net.PacketHandler;
import cool.furry.mc.neoforge.projectexpansion.platform.CapabilityRegistrar;
import cool.furry.mc.neoforge.projectexpansion.platform.ServerContext;
import cool.furry.mc.neoforge.projectexpansion.registries.*;
import cool.furry.mc.neoforge.projectexpansion.util.*;
import moze_intel.projecte.gameObjs.registries.PEDataComponentTypes;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.apache.logging.log4j.LogManager;

public final class Main implements ModInitializer {
    public static final String MOD_ID = "projectexpansion";
    public static final org.apache.logging.log4j.Logger Logger = LogManager.getLogger();
    public static ModContainer MOD_CONTAINER;
    private static PacketHandler packetHandler;
    private static boolean initializationRequested;
    private static boolean projectEReady;
    private static boolean initialized;

    @Override
    public void onInitialize() {
        initializationRequested = true;
        initializeWhenReady();
    }

    /** Fabric entrypoint ordering is unspecified, so wait for ProjectEF's registry binding. */
    public static void projectEInitialized() {
        projectEReady = true;
        initializeWhenReady();
    }

    private static void initializeWhenReady() {
        if (!initializationRequested || !projectEReady || initialized) return;
        initialized = true;
        initialize();
    }

    private static void initialize() {
        MOD_CONTAINER = FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow();
        Config.register();
        Fuel.registerAll();
        Matter.registerAll();
        Star.registerAll();
        AdvancedAlchemicalChest.register();

        AttachmentTypes.init();
        Attributes.Registry.register();
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(net.minecraft.world.entity.EntityType.PLAYER,
            net.minecraft.world.entity.player.Player.createAttributes().add(Attributes.SUN_EXPOSURE_PROTECTION));
        DataComponentTypes.Registry.register();
        BlockTypes.Registry.register();
        Blocks.Registry.register();
        Items.Registry.register();
        for (Fuel fuel : Fuel.VALUES) {
            if (fuel.getItem() != null) net.fabricmc.fabric.api.registry.FuelRegistry.INSTANCE.add(fuel.getItem(), fuel.getBurnTime());
            if (fuel.getBlockItem() != null) net.fabricmc.fabric.api.registry.FuelRegistry.INSTANCE.add(fuel.getBlockItem(), fuel.getBurnTime() * 9);
        }
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (player.isSpectator()) return net.minecraft.world.InteractionResult.PASS;
            var stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof cool.furry.mc.neoforge.projectexpansion.item.ItemMatterUpgrader upgrader) {
                return upgrader.onItemUseFirst(stack, new net.minecraft.world.item.context.UseOnContext(player, hand, hit));
            }
            if (stack.getItem() instanceof moze_intel.projecte.gameObjs.items.PhilosophersStone &&
                    world.getBlockEntity(hit.getBlockPos()) instanceof cool.furry.mc.neoforge.projectexpansion.block.entity.BlockEntityNBTFilterable block) {
                if (!world.isClientSide) block.toggleFilter(player);
                return net.minecraft.world.InteractionResult.SUCCESS;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
        BlockEntityTypes.Registry.register();
        SoundEvents.Registry.register();
        MenuTypes.Registry.register();
        CreativeTabs.Registry.register();

        CapabilityEvents.registerCapabilities(new CapabilityRegistrar());
        cool.furry.mc.neoforge.projectexpansion.platform.InventoryCapabilities.register();
        packetHandler = new PacketHandler();
        PlayerEvents.register();
        CommandRegistrationCallback.EVENT.register((dispatcher, context, environment) -> CommandRegistry.register(dispatcher, context));
        ServerLifecycleEvents.SERVER_STARTING.register(ServerContext::set);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            ServerContext.set(null);
            ServerEvents.clear();
            PowerFlowerCollector.clear();
            cool.furry.mc.neoforge.projectexpansion.platform.EmcTransactions.clear();
        });
        ServerTickEvents.END_SERVER_TICK.register(Main::serverTick);
        ServerTickEvents.END_SERVER_TICK.register(ServerEvents::onServerTick);
        ServerTickEvents.END_SERVER_TICK.register(PowerFlowerCollector::onTick);
    }

    public static PacketHandler packetHandler() { return packetHandler; }

    private static void serverTick(MinecraftServer server) {
        AlchemicalCollectionCollector.process();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (stack.is(Items.INFINITE_FUEL.get()) && !stack.has(DataComponentTypes.OWNER.get())) {
                    stack.set(DataComponentTypes.OWNER.get(), new DataComponentTypes.OwnerData(player.getUUID(), player.getName().getString()));
                    continue;
                }
                boolean enchanted = EnchantmentHelper.getItemEnchantmentLevel(server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.ALCHEMICAL_COLLECTION), stack) > 0;
                if (enchanted && !stack.has(PEDataComponentTypes.ACTIVE.get())) {
                    stack.set(PEDataComponentTypes.ACTIVE.get(), true);
                }
            }
        }
    }

    public static ResourceLocation rl(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }
}
