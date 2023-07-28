package com.naoido.command

import com.naoido.JDACore
import com.naoido.JDACore.Companion.jda
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import kotlin.system.exitProcess

class Owner: Command {
    override val name: String
        get() = "owner"
    override val option: SlashCommandData? = null;

    override fun commandHandle(event: SlashCommandInteractionEvent) {
        // Not implemented slash command.
        return;
    }

    override fun isPrefixCommand(command: String): Boolean {
        return command == JDACore.DEFAULT_PREFIX + name;
    }

    override fun prefixHandle(event: MessageReceivedEvent) {
        if (!event.author.isOwner()) {
            return
        }
        val args: List<String> = event.message.contentRaw.split(" ");
        when(args[1]) {
            "restart" -> {
                event.channel.sendMessage("再起動します。").complete();
                exitProcess(0);
            }
            "user" -> {
                val user: User? = jda.getUserById(args[2]);
                user?.let { event.channel.sendMessage(it.name).queue(); }?: event.message.reply("null").queue();
            }
        }
    }
}