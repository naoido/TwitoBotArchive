package com.naoido.command

import com.naoido.enum.DiscordEntities
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction

fun SlashCommandInteractionEvent.ephemeralReplay(content: String): ReplyCallbackAction {
    return this.reply(content).setEphemeral(true);
}

fun User.isOwner(): Boolean {
    return this == DiscordEntities.OWNER.toEntity();
}