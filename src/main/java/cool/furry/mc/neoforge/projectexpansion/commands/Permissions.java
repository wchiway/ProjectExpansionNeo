package cool.furry.mc.neoforge.projectexpansion.commands;

import cool.furry.mc.neoforge.projectexpansion.Main;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.function.Predicate;

public class Permissions {
    public static final CommandPermissionNode EMC = nodeOpCommand("emc");
    public static final CommandPermissionNode EMC_ADD = nodeOpCommand("emc.add");
    public static final CommandPermissionNode EMC_GET = nodeOpCommand("emc.get");
    public static final CommandPermissionNode EMC_REMOVE = nodeOpCommand("emc.remove");
    public static final CommandPermissionNode EMC_SET = nodeOpCommand("emc.set");
    public static final CommandPermissionNode EMC_TEST = nodeOpCommand("emc.test");
    public static final CommandPermissionNode KNOWLEDGE = nodeOpCommand("knowledge");
    public static final CommandPermissionNode KNOWLEDGE_CLEAR = nodeOpCommand("knowledge.clear");
    public static final CommandPermissionNode KNOWLEDGE_LEARN = nodeOpCommand("knowledge.learn");
    public static final CommandPermissionNode KNOWLEDGE_TEST = nodeOpCommand("knowledge.test");
    public static final CommandPermissionNode KNOWLEDGE_UNLEARN = nodeOpCommand("knowledge.unlearn");
    public static final CommandPermissionNode BOOK_ADD_HAND = nodeOpCommand("book.add.hand");
    public static final CommandPermissionNode BOOK_ADD_PLAYER = nodeOpCommand("book.add.player");
    public static final CommandPermissionNode BOOK_CLEAR_HAND = nodeOpCommand("book.clear.hand");
    public static final CommandPermissionNode BOOK_CLEAR_PLAYER = nodeOpCommand("book.clear.player");
    public static final CommandPermissionNode BOOK_DUMP_HAND = nodeOpCommand("book.dump.hand");
    public static final CommandPermissionNode BOOK_DUMP_PLAYER = nodeOpCommand("book.dump.player");
    public static final CommandPermissionNode BOOK_LIST_HAND = nodeOpCommand("book.list.hand");
    public static final CommandPermissionNode BOOK_LIST_PLAYER = nodeOpCommand("book.list.player");
    public static final CommandPermissionNode BOOK_REINDEX_HAND = nodeOpCommand("book.reindex.hand");
    public static final CommandPermissionNode BOOK_REINDEX_PLAYER = nodeOpCommand("book.reindex.player");
    public static final CommandPermissionNode BOOK_REMOVE_HAND = nodeOpCommand("book.remove.hand");
    public static final CommandPermissionNode BOOK_REMOVE_PLAYER = nodeOpCommand("book.remove.player");
    public static final CommandPermissionNode DUMP_FUEL_MAP = nodeAllCommand("dump_fuel_map");
    public static final CommandPermissionNode WIKI = nodeAllCommand("wiki");
    public static final CommandPermissionNode SET_OWNER = nodeOpCommand("set_owner");
    public static final CommandPermissionNode RELOAD_EMC = nodeOpCommand("reload_emc");

    private static CommandPermissionNode nodeAllCommand(String name) {
        return new CommandPermissionNode(Main.MOD_ID + ".command." + name, Commands.LEVEL_ALL);
    }
    private static CommandPermissionNode nodeOpCommand(String name) {
        return new CommandPermissionNode(Main.MOD_ID + ".command." + name, Commands.LEVEL_GAMEMASTERS);
    }
    public record CommandPermissionNode(String node, int fallbackLevel) implements Predicate<CommandSourceStack> {
        @Override
        public boolean test(CommandSourceStack source) {
            return me.lucko.fabric.api.permissions.v0.Permissions.check(source, node, fallbackLevel);
        }
    }
}
