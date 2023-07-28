package com.naoido

import com.naoido.command.CommandManager
import com.naoido.command.Music
import com.naoido.enum.DiscordEntities
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.net.InetAddress
import java.util.*

class JDACore {
    companion object {
        private val isHomePc: Boolean = InetAddress.getLocalHost().hostName == "naoido-homepc";
        const val DEFAULT_PREFIX: String = "!!";
        lateinit var jda: JDA;
        private val DISCORD_TOKEN: String;

        init {
            val properties: Properties = Properties();
            properties.load(this::class.java.getResourceAsStream("/token.properties"));
            DISCORD_TOKEN = if (isHomePc) properties.getProperty("debug_discord_token") else properties.getProperty("discord_token");
        }

        fun build() {
            logger.info("starting とぅうぃとBot...");

            // building bot
            jda = JDABuilder.create(DISCORD_TOKEN, listOf(*GatewayIntent.values()))
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.watching("twitter"))
                .addEventListeners(CommandManager.INSTANCE, Music.Disconnect.INSTANCE)
                .enableCache(CacheFlag.VOICE_STATE)
                .build();

            jda.awaitReady();

            // register commands
            CommandManager.INSTANCE.register();

            // send message
            sendMessageToOwner("info", "Startup completed.");
            logger.info("とぅうぃとBot finished launching.");
            logger.info("In use on ${jda.selfUser.mutualGuilds.size} servers");
        }

        fun sendMessageToOwner(level: String, message: String) {
            (DiscordEntities.OWNER.toEntity() as? User)?.let{
                it.openPrivateChannel().complete()?.sendMessage("[${level}] $message")?.queue()
            }?: logger.warning("Not found owner's User");
        }
    }
}
