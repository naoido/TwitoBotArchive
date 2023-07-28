package com.naoido.lavaplayer

import com.naoido.JDACore.Companion.jda
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.channel.Channel
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import java.lang.reflect.TypeVariable
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.locks.ReentrantLock

class LavaPlayerManager {
    companion object {
        val INSTANCE: LavaPlayerManager by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { LavaPlayerManager() }
    }
    private val musicManagers: HashMap<Long, GuildMusicManager> = HashMap();
    private val audioPlayerManager: AudioPlayerManager = DefaultAudioPlayerManager();

    init {
        AudioSourceManagers.registerRemoteSources(this.audioPlayerManager);
        AudioSourceManagers.registerLocalSource(this.audioPlayerManager);
    }

    fun getMusicManagerByTextChannelId(channelId: Long): GuildMusicManager {
        return jda.getTextChannelById(channelId)?.guild?.let { getMusicManager(it) }!!;
    }

    fun getMusicManager(guild: Guild): GuildMusicManager {
        return this.musicManagers.computeIfAbsent(guild.idLong) {
            val musicManager: GuildMusicManager = GuildMusicManager(this.audioPlayerManager);

            guild.audioManager.sendingHandler = musicManager.sendHandler;

            musicManager;
        }
    }

    fun loadAndPlay(channel: TextChannel, userId: Long, url: String?) {
        if (url == null) return;

        var mutableUrl: String = url;
        val isSearch: Boolean = isUrl(url).not();
        val guildMusicManager: GuildMusicManager = getMusicManager(channel.guild);

        if (isSearch) {
            mutableUrl = "ytsearch:$url"
        }

        this.audioPlayerManager.loadItemOrdered(guildMusicManager, mutableUrl, object: AudioLoadResultHandler {
            override fun trackLoaded(track: AudioTrack) {
                guildMusicManager.trackScheduler.queue(track, Pair(channel.idLong, userId));
                channel.sendMessage("${track.info.title}を読み込みました！").queue();
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                playlist.tracks.let {
                    val pair: Pair<Long, Long> = Pair(channel.idLong, userId);
                    if (isSearch) {
                        guildMusicManager.trackScheduler.queue(it.first(), pair);
                        channel.sendMessage("${it.first().info.title}を読み込みました！").queue();
                    } else {
                        for (track in it) {
                            guildMusicManager.trackScheduler.queue(track, pair);
                        }
                        channel.sendMessage(":notes:${it.size}個の曲をキューに追加しました！").queue();
                    }
                }
            }

            override fun noMatches() {
                channel.sendMessage(":x:検索結果がありませんでした。").queue();
            }

            override fun loadFailed(exception: FriendlyException?) {
                channel.sendMessage(":x:ロードに失敗しました").queue();
            }
        });
    }

    private fun isUrl(url: String): Boolean {
        return try {
            URI(url).isAbsolute;
        } catch (e: URISyntaxException) {
            false;
        }
    }
}