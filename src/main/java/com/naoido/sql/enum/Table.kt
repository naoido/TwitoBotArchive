package com.naoido.sql.enum

enum class Table(val info: String) {
    HISTORIES("(user_id, uuid, timestamp)"),
    TWEET_DATA("(uuid, author_id, tweet_id, media_key, media_id, media_url, nsfw, type)"),
    TYPES("(id, type)");

    override fun toString(): String {
        return this.name.lowercase();
    }
    companion object {
        fun values(vararg values: Any?): String {
            var line = "(";
            for (value in values) {
                line += (when (value) {
                    is String -> "'$value',";
                    else -> "$value,";
                });
            }
            return line.substring(0, line.length - 1).plus(")");
        }
    }
}