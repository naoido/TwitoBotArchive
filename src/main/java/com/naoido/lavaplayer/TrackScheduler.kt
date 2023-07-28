package com.naoido.lavaplayer

import com.naoido.JDACore.Companion.jda
import com.naoido.lavaplayer.model.Track
import com.naoido.logger
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import net.dv8tion.jda.api.entities.Guild
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class TrackScheduler(private val player: AudioPlayer): AudioEventAdapter() {
    val queue: BlockingQueue<Track> = LinkedBlockingQueue();
    var nowPlaying: Track? = null;
    var loop: Boolean = false;
    var repeat: Boolean = false;

    fun clear(guild: Guild) {
        this.queue.clear();
        this.nowPlaying = null;
        LavaPlayerManager.INSTANCE.getMusicManager(guild).trackScheduler.player.stopTrack();
    }

    // Pair<channelId, userId>
    fun queue(track: AudioTrack, pair: Pair<Long, Long>) {
        //すでに再生している場合は${queue}に追加する。
        if (!this.player.startTrack(track, true)) {
            this.queue.offer(Track(track, pair));
        } else {
            this.nowPlaying = Track(track, pair);
        }
    }

    fun next() {
        val guild: Guild? = this.nowPlaying?.getGuild();

        this.nowPlaying?.let {
            it.audioTrack?.let {audioTrack ->
                when {
                    loop -> {
                        this.player.startTrack(audioTrack.makeClone(), false);
                        return;
                    }
                    repeat -> {
                        this.queue(audioTrack.makeClone(), this.nowPlaying!!.pair);
                    }
                }
                this.nowPlaying = this.queue.poll();
                this.player.startTrack(this.nowPlaying?.audioTrack, false);
            }
        }?: guild?.audioManager?.closeAudioConnection();
    }

    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
        if (endReason != null) {
            if (endReason.mayStartNext) {
                if (this.queue.isEmpty()) {
                    jda.getTextChannelById(this.nowPlaying?.pair!!.first)?.guild?.audioManager?.closeAudioConnection();
                    return;
                }
                next();
            }
        }
    }
}
