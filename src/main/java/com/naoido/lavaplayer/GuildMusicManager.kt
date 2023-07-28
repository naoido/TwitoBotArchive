package com.naoido.lavaplayer

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager

class GuildMusicManager(manager: AudioPlayerManager) {
    val player: AudioPlayer;
    val trackScheduler: TrackScheduler;
    val sendHandler: AudioPlayerSendHandler

    init {
        this.player = manager.createPlayer();
        this.trackScheduler = TrackScheduler(this.player);

        this.player.addListener(this.trackScheduler);
        this.player.volume = 50;

        this.sendHandler = AudioPlayerSendHandler(this.player);
    }
}