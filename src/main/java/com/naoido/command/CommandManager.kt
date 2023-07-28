package com.naoido.command

import com.naoido.JDACore.Companion.jda
import com.naoido.enum.Emojis
import com.naoido.logger
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class CommandManager : ListenerAdapter() {
    companion object {
        val commands = HashSet<Command>();
        val INSTANCE = CommandManager();
        init {
            commands.addAll(Music.getMusicCommandsInstance());
            commands.addAll(
                listOf(GuildSetting(), Invite(), Omikuji(), Owner(), Twitter())
            );
        }
    }

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        commands.find { event.name == it.name; }?.commandHandle(event)?: return;
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        if (event.user?.isBot == true) {
            return;
        }
        when(event.reaction.emoji.name) {
            Emojis.ARROW_DOWN.unicode -> {
                Twitter.reactionHandle(event);
            }
        }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return;

        val text: String = event.message.contentRaw;

        if (Twitter.containsTweetUrl(text)) {
            Twitter.INSTANCE.prefixHandle(event);
            return;
        }

        text.substringBefore(" ").let {command ->
                commands.find {
                    if (event.isFromGuild) it.isGuildPrefixCommand(command, event.guild.idLong);
                    else it.isPrefixCommand(command);
                }
            }?.prefixHandle(event);
    }

    fun register() {
        val slashCommands: HashSet<SlashCommandData> = HashSet();

        for (command in commands) {
            command.option?.let {
                slashCommands.add(it);
                logger.info("Added a command \"${command.name}\"");
            }
        }

        jda.updateCommands().addCommands(slashCommands).queue();
    }
}
