package com.naoido.command

import com.naoido.model.GuildSettings
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import kotlin.reflect.full.createInstance

interface Command {
    val name: String;
    val option: SlashCommandData?;

    fun commandHandle(event: SlashCommandInteractionEvent): Unit
    fun prefixHandle(event: MessageReceivedEvent): Unit { return }
    fun isPrefixCommand(command: String): Boolean { return false; }
    fun isGuildPrefixCommand(command: String, guildId: Long): Boolean { return GuildSettings.isGuildPrefixCommand(guildId, command, name); }
}