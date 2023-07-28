package com.naoido.enum

import net.dv8tion.jda.api.entities.emoji.Emoji

enum class Emojis(val unicode: String) {
    X("❌"),
    ARROW_DOWN("⬇️"),
    UNDERAGE("\uD83D\uDD1E");

    fun getEmoji(): Emoji {
        return Emoji.fromUnicode(this.unicode);
    }
}