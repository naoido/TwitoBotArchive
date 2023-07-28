package com.naoido.command

import com.naoido.JDACore.Companion.jda
import com.naoido.enum.DiscordEntities
import com.naoido.lavaplayer.GuildMusicManager
import com.naoido.lavaplayer.LavaPlayerManager
import com.naoido.lavaplayer.TrackScheduler
import com.naoido.lavaplayer.model.Track
import com.naoido.model.GuildSettings
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.managers.AudioManager
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction
import net.dv8tion.jda.internal.managers.AudioManagerImpl
import okhttp3.internal.wait
import java.util.concurrent.BlockingQueue

class Music {
    companion object {
        fun getMusicCommandsInstance(): Array<Command> {
            return setOf(
                Play(),
                Disconnect.INSTANCE,
                NowPlaying(),
                Queue(),
                Volume(),
                Skip(),
                Pause(),
                Stop(),
                Repeat(),
                Loop()
            ).toTypedArray();
        }
    }

    class Play: Command {
        override val name: String
            get() = "play"
        override val option: SlashCommandData
            get() = Commands.slash(name, "音楽を再生します。")
                .addOption(OptionType.STRING, "url", "再生する音楽のURLを指定できます。").setGuildOnly(true);

        private fun run(guild: Guild?, channel: TextChannel, member: Member?, url: String?) {
            if (guild == null || member == null || url == null || member.voiceState == null) return;

            val audioManager: AudioManager = guild.audioManager;
            if (!audioManager.isConnected) {
                member.voiceState?.channel?.members;
                audioManager.isAutoReconnect = false;
                audioManager.isSelfDeafened = true;

                member.voiceState?.channel?.let { audioManager.openAudioConnection(it) };
            }

            LavaPlayerManager.INSTANCE.loadAndPlay(channel, member.user.idLong, url);
        }

        override fun commandHandle(event: SlashCommandInteractionEvent) {
            val member: Member? = event.member;

            if (member == null || event.member?.voiceState?.inAudioChannel() != true) {
                event.ephemeralReplay(":x:ボイスチャンネルに接続している必要があります。").queue();
                return;
            }

            run(event.guild, (event.channel as TextChannel), event.member, event.getOption("url")?.asString);
        }

        override fun prefixHandle(event: MessageReceivedEvent) {
            val member: Member? = event.member;

            if (member == null || event.member?.voiceState?.inAudioChannel() != true) {
                event.message.reply(":x:ボイスチャンネルに接続している必要があります。").queue();
                return;
            }

            val url: String = event.message.contentDisplay.split(" ").drop(1).joinToString(" ");
            run(event.guild, (event.channel as TextChannel), event.member, url);
        }

        override fun isGuildPrefixCommand(command: String, guildId: Long): Boolean {
            return GuildSettings.isGuildPrefixCommand(guildId, command, "(${name}|p)");
        }
    }

    class Disconnect: Command, ListenerAdapter() {
        companion object {
            val INSTANCE: Disconnect = Disconnect();

            fun disconnect(guild: Guild): Boolean {
                if (!guild.audioManager.isConnected) return false;
                LavaPlayerManager.INSTANCE.getMusicManager(guild).trackScheduler.clear(guild);
                guild.audioManager.closeAudioConnection();
                return true;
            }
        }

        override val name: String
            get() = "disconnect";
        override val option: SlashCommandData
            get() = Commands.slash("disconnect", "接続しているボイスチャンネルから離脱させます。").setGuildOnly(true);

        override fun commandHandle(event: SlashCommandInteractionEvent) {
            if (disconnect(event.guild!!)) {
                event.reply(":wave:").queue();
            } else {
                event.reply(":x:ボイスチャンネルに参加していません！").queue();
            }
        }

        override fun prefixHandle(event: MessageReceivedEvent) {
            if (disconnect(event.guild)) {
                event.message.reply(":wave:").queue();
            } else {
                event.message.reply(":x:ボイスチャンネルに参加していません！").queue()
            }
        }

        override fun isGuildPrefixCommand(command: String, guildId: Long): Boolean {
            return GuildSettings.isGuildPrefixCommand(guildId, command, "(${name}|dis)");
        }

        override fun onGenericGuildVoice(event: GenericGuildVoiceEvent) {
            if (event.guild.audioManager.isConnected && event.member.voiceState?.inAudioChannel() != true) {
                val members: List<Member>? = event.guild.audioManager.connectedChannel?.members;
                members?.let {
                    if (it.size <= 1) {
                        disconnect(event.guild);
                    }
                }
            }
        }
    }

    class NowPlaying: Command {
        override val name: String
            get() = "nowplaying";
        override val option: SlashCommandData
            get() = Commands.slash(name, "現在再生している音楽を表示します。").setGuildOnly(true);

        override fun commandHandle(event: SlashCommandInteractionEvent) {
            val track: Track? = LavaPlayerManager.INSTANCE.getMusicManager(event.guild!!).trackScheduler.nowPlaying;
            track?.let {
                event.reply("現在再生している曲は`${it.audioTrack?.info?.title}`です。").queue();
            }?: event.reply("現在再生している曲はありません。").queue();
        }

        override fun prefixHandle(event: MessageReceivedEvent) {
            val track: Track? = LavaPlayerManager.INSTANCE.getMusicManager(event.guild).trackScheduler.nowPlaying;
            track?.let {
                event.message.reply("現在再生している曲は`${it.audioTrack?.info?.title}`です。").queue();
            }?: event.message.reply("現在再生している曲はありません。").queue();
        }

        override fun isGuildPrefixCommand(command: String, guildId: Long): Boolean {
            return GuildSettings.isGuildPrefixCommand(guildId, command, "(${name}|np)");
        }
    }

    class Queue: Command {
        override val name: String
            get() = "queue"
        override val option: SlashCommandData
            get() = Commands.slash(name, "現在のキューを表示します。").setGuildOnly(true);

        override fun isGuildPrefixCommand(command: String, guildId: Long): Boolean {
            return GuildSettings.isGuildPrefixCommand(guildId, command, "(:?$name|q)");
        }

        override fun isPrefixCommand(command: String): Boolean {
            return false;
        }

        override fun commandHandle(event: SlashCommandInteractionEvent) {
            LavaPlayerManager.INSTANCE.getMusicManager(event.guild!!).let {
                val queue: List<Track> = it.trackScheduler.queue.toList();
                val size: Int = queue.size

                if (queue.isEmpty()) {
                    event.reply("現在のキューは空っぽです。").queue();
                    return;
                }

                event.reply("現在のキューは${size}曲あります。")
                    .addContent(getQueueListString(queue))
                    .queue();
            }
        }

        override fun prefixHandle(event: MessageReceivedEvent) {
            LavaPlayerManager.INSTANCE.getMusicManager(event.guild).let {
                val queue: List<Track> = it.trackScheduler.queue.toList();
                val size: Int = queue.size

                if (queue.isEmpty()) {
                    event.message.reply("現在のキューは空っぽです。").queue();
                    return;
                }
                event.message.reply("現在のキューは${size}曲あります。")
                    .addContent(getQueueListString(queue))
                    .queue();

            }
        }

        private fun getQueueListString(queue: List<Track>): String {
            var content: String = "";
            val size: Int = queue.size
            for (i in 0 until (if (size > 10) 10 else size)) {
                val track: AudioTrack = queue[i].audioTrack?: continue;
                content += "\n[$i]`${track.info?.title} by ${track.info?.author}`";
            }

            return content;
        }
    }

    class Volume: Command {
        override val name: String
            get() = "volume"
        override val option: SlashCommandData
            get() = Commands.slash("volume", "音量を調節できます。")
                .addOptions(
                    OptionData(OptionType.INTEGER, "volume", "音量調整できます。[初期値50,Max150]")
                        .setRequired(true)
                ).setGuildOnly(true);

        override fun isPrefixCommand(command: String): Boolean {
            return false;
        }

        override fun isGuildPrefixCommand(command: String, guildId: Long): Boolean {
            return GuildSettings.isGuildPrefixCommand(guildId, command, name);
        }
        override fun commandHandle(event: SlashCommandInteractionEvent) {
            val player: AudioPlayer = LavaPlayerManager.INSTANCE.getMusicManager(event.guild!!).player;
            val oldVolume: Int = player.volume;
            val volume: Int = event.getOption("volume")?.asInt?: return;
            if (volume !in 1..150) {
                event.reply("Volumeは1から150の範囲で指定してください。").queue();
                return;
            }
            LavaPlayerManager.INSTANCE.getMusicManager(event.guild!!).player.volume = volume;
            event.reply("Volumeを`${oldVolume}`から`${volume}`に変更しました！").queue();
        }

        override fun prefixHandle(event: MessageReceivedEvent) {
            val args: List<String> = event.message.contentRaw.split(" ").drop(1);
            if (args.isEmpty()) {
                event.message.reply("ボリュームの値を設定してください。`例: !!volume 50`").queue();
            }
            try {
                val volume: Int = args[0].toInt();
                val player: AudioPlayer = LavaPlayerManager.INSTANCE.getMusicManager(event.guild).player;
                val oldVolume: Int = player.volume;
                if (volume in 1..150) {
                    player.volume = volume;
                    event.message.reply("Volumeを`${oldVolume}`から`${volume}`に変更しました！").queue();
                }
            } catch (e: NumberFormatException) {
                event.message.reply("数字を入力してください。`例: !!volume 50`");
            }
        }
    }

    class Skip: Command {
        override val name: String
            get() = "skip"
        override val option: SlashCommandData
            get() = Commands.slash("skip", "次の曲を再生します。").setGuildOnly(true);

        override fun isPrefixCommand(command: String): Boolean {
            return false;
        }

        override fun isGuildPrefixCommand(command: String, guildId: Long): Boolean {
            return GuildSettings.isGuildPrefixCommand(guildId, command, "(:?$name|s)");
        }

        override fun commandHandle(event: SlashCommandInteractionEvent) {
            val musicManager: GuildMusicManager = LavaPlayerManager.INSTANCE.getMusicManager(event.guild!!);
            musicManager.trackScheduler.next();
            event.reply("次の曲を再生します！").queue();
        }

        override fun prefixHandle(event: MessageReceivedEvent) {
            LavaPlayerManager.INSTANCE.getMusicManager(event.guild).trackScheduler.next();
            event.message.reply("次の曲を再生します！").queue();
        }
    }

    class Pause: Command {
        override val name: String
            get() = "pause"
        override val option: SlashCommandData
            get() = Commands.slash("pause", "曲を一時停止/一時停止の解除をします。").setGuildOnly(true);

        override fun isPrefixCommand(command: String): Boolean {
            return false;
        }

        override fun isGuildPrefixCommand(command: String, guildId: Long): Boolean {
            return GuildSettings.isGuildPrefixCommand(guildId, command, name);
        }

        override fun commandHandle(event: SlashCommandInteractionEvent) {
            val player: AudioPlayer = LavaPlayerManager.INSTANCE.getMusicManager(event.guild!!).player;
            val nowPause: Boolean = player.isPaused;
            player.isPaused = !nowPause;
            event.reply(if (nowPause) "再生を再開しました！" else "曲を一時停止しました。").queue();
        }

        override fun prefixHandle(event: MessageReceivedEvent) {
            val player: AudioPlayer = LavaPlayerManager.INSTANCE.getMusicManager(event.guild).player;
            val nowPause: Boolean = player.isPaused;
            player.isPaused = !nowPause;
            event.message.reply(if (nowPause) "再生を再開しました！" else "曲を一時停止しました。").queue();
        }
    }

    class Stop: Command {
        override val name: String
            get() = "stop"
        override val option: SlashCommandData
            get() = Commands.slash("stop", "再生している曲をすべて停止します。").setGuildOnly(true);

        override fun isPrefixCommand(command: String): Boolean {
            return false;
        }

        override fun isGuildPrefixCommand(command: String, guildId: Long): Boolean {
            return GuildSettings.isGuildPrefixCommand(guildId, command, name);
        }

        private fun stop(trackScheduler: TrackScheduler): Boolean {
            if (trackScheduler.nowPlaying == null) return false;
            trackScheduler.queue.clear();
            trackScheduler.nowPlaying = null;
            trackScheduler.next();
            return true;
        }

        override fun commandHandle(event: SlashCommandInteractionEvent) {
            if (stop(LavaPlayerManager.INSTANCE.getMusicManager(event.guild!!).trackScheduler)) {
                event.reply("現在再生している曲はありません。").queue();
                return;
            }
            event.reply("曲の再生をすべて停止しました！").queue();
        }

        override fun prefixHandle(event: MessageReceivedEvent) {
            if (stop(LavaPlayerManager.INSTANCE.getMusicManager(event.guild).trackScheduler)) {
                event.message.reply("現在再生している曲はありません。").queue();
                return;
            }
            event.message.reply("曲の再生をすべて停止しました！").queue();
        }
    }

    class Repeat: Command {
        override val name: String
            get() = "repeat"
        override val option: SlashCommandData
            get() = Commands.slash("repeat", "リピートのon/offができます。").setGuildOnly(true);

        override fun isPrefixCommand(command: String): Boolean {
            return false;
        }

        override fun isGuildPrefixCommand(command: String, guildId: Long): Boolean {
            return GuildSettings.isGuildPrefixCommand(guildId, command, name);
        }

        private fun setRepeat(guild: Guild): Boolean {
            val trackScheduler: TrackScheduler = LavaPlayerManager.INSTANCE.getMusicManager(guild).trackScheduler;
            val repeat: Boolean = !trackScheduler.repeat;
            trackScheduler.repeat = repeat;
            return repeat;
        }
        override fun commandHandle(event: SlashCommandInteractionEvent) {
            event.reply("Repeatを${if(setRepeat(event.guild!!)) "有効" else "無効"}にしました。").queue();
        }

        override fun prefixHandle(event: MessageReceivedEvent) {
            event.message.reply("repeatを${if (setRepeat(event.guild)) "有効" else "無効"}にしました。").queue();
        }
    }

    class Loop: Command {
        override val name: String
            get() = "loop"
        override val option: SlashCommandData
            get() = Commands.slash("loop", "loopを有効/無効にできます。").setGuildOnly(true);

        override fun isPrefixCommand(command: String): Boolean {
            return false;
        }

        override fun isGuildPrefixCommand(command: String, guildId: Long): Boolean {
            return GuildSettings.isGuildPrefixCommand(guildId, command, name);
        }

        private fun setLoop(guild: Guild): Boolean {
            val trackScheduler: TrackScheduler = LavaPlayerManager.INSTANCE.getMusicManager(guild).trackScheduler;
            val loop: Boolean = !trackScheduler.loop;
            trackScheduler.loop = loop;
            return loop;
        }

        override fun commandHandle(event: SlashCommandInteractionEvent) {
            event.reply("loopを${if (setLoop(event.guild!!)) "有効" else "無効"}にしました！").queue();
        }

        override fun prefixHandle(event: MessageReceivedEvent) {
            event.message.reply("loopを${if (!setLoop(event.guild)) "有効" else "無効"}にしました！").queue();
        }
    }

    class PlayList: Command {
        override val name: String
            get() = TODO("Not yet implemented")
        override val option: SlashCommandData?
            get() = TODO("Not yet implemented")

        override fun commandHandle(event: SlashCommandInteractionEvent) {
            TODO("Not yet implemented")
        }
    }
}