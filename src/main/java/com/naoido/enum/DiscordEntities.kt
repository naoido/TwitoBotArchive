package com.naoido.enum

import com.naoido.JDACore.Companion.jda
import com.naoido.enum.DiscordEntities.Entity.*

enum class DiscordEntities(private val id: Long, private val entity: Entity) {
    OWNER(279583321766494208L, USER),
    DEBUG_GUILD(859683674068615178L, GUILD),
    DEBUG_CHANNEL(958400764160663612L, GUILD_CHANNEL);

    fun toEntity(): Any? {
        return when(this.entity) {
            USER -> jda.getUserById(this.id);
            GUILD -> jda.getGuildById(this.id);
            GUILD_CHANNEL -> jda.getGuildChannelById(this.id);
        }
    }

    enum class Entity {
        USER,
        GUILD,
        GUILD_CHANNEL;
    }
}