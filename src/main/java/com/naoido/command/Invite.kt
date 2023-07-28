package com.naoido.command

import com.naoido.JDACore
import com.naoido.model.GuildSettings
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

class Invite: Command {
    companion object {
        private const val INVITE_URL: String = "https://discord.com/api/oauth2/authorize?client_id=381680291384655873&permissions=8&scope=applications.commands%20bot";
    }

    override val name: String = "invite";

    override val option: SlashCommandData = Commands.slash(this.name, "このbotの招待URLを取得");

    override fun commandHandle(event: SlashCommandInteractionEvent) {
        event.ephemeralReplay(INVITE_URL).queue();
    }

    override fun isPrefixCommand(command: String): Boolean {
        return command == "${JDACore.DEFAULT_PREFIX}${this.name}";
    }

    override fun isGuildPrefixCommand(command: String, guildId: Long): Boolean {
        return command == "${GuildSettings.INSTANCE.getPrefix(guildId)}${this.name}"
    }

    override fun prefixHandle(event: MessageReceivedEvent) {
        event.message.reply(INVITE_URL).mentionRepliedUser(false).queue();
    }
}