package com.naoido.lavaplayer.model

import com.naoido.JDACore.Companion.jda
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.User

// Pair<guildId, userId>
data class Track(val audioTrack: AudioTrack?, val pair: Pair<Long, Long>) {
    fun getUser(): User? {
        return jda.getUserById(pair.second);
    }

    fun getGuild(): Guild? {
        return jda.getGuildById(pair.first);
    }
}
