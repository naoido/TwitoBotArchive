package com.naoido.lavaplayer

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer

data class AudioPlayerSendHandler(private val audioPlayer: AudioPlayer): AudioSendHandler {
    private val buffer: ByteBuffer = ByteBuffer.allocate(1024);
    private val frame: MutableAudioFrame = MutableAudioFrame();

    init {
        this.frame.setBuffer(buffer);
    }

    override fun canProvide(): Boolean {
        return this.audioPlayer.provide(this.frame);
    }

    override fun provide20MsAudio(): ByteBuffer {
        return this.buffer.flip();
    }

    override fun isOpus(): Boolean {
        return true;
    }
}