package cool.furry.mc.neoforge.projectexpansion.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class CommandRegistry {
    public static final String COMMAND_BASE = "px";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {

        LiteralCommandNode<CommandSourceStack> baseNode = dispatcher.register(Commands.literal(COMMAND_BASE)
                .then(CommandBook.getArguments())
                .then(CommandEMC.getArguments())
                .then(CommandKnowledge.getArguments(buildContext))
                .then(CommandDumpFuelMap.getArguments())
                .then(CommandDevTest.getArguments())
                .then(CommandWiki.getArguments())
                .then(CommandSetOwner.getArguments())
                .then(CommandReloadEMC.getArguments())
        );
        dispatcher.register(Commands.literal("projectexpansion").redirect(baseNode));
        dispatcher.register(Commands.literal("pex").redirect(baseNode));
    }
}
