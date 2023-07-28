package com.naoido.model

import com.naoido.JDACore

class GuildSettings {
    companion object {
        val INSTANCE: GuildSettings = GuildSettings();
        fun isGuildPrefixCommand(guildId: Long, command: String, regex: String): Boolean {
            return command.matches("^${INSTANCE.getPrefix(guildId)}${regex}$".toRegex());
        }
    }

    private val settings: HashMap<Long, Setting> = HashMap();


    fun getGuildSetting(guildId: Long): Setting {
        return settings.computeIfAbsent(guildId){ Setting(guildId, JDACore.DEFAULT_PREFIX) };
    }

    fun getPrefix(guildId: Long): String {
        return this.getGuildSetting(guildId).prefix;
    }

    fun setPrefix(guildId: Long, prefix: String) {
        getGuildSetting(guildId).prefix = prefix;
    }

    data class Setting(val guildId: Long,
                       var prefix: String,
                       val twitter: Twitter = Twitter()) {
        data class Twitter(var allowAutoAddReaction: Boolean = true,
                           var isSendToDm: Boolean = false) {

        }
    }
}
