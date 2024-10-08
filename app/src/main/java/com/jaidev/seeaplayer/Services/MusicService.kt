package com.jaidev.seeaplayer.Services

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jaidev.seeaplayer.MP3ConverterFunctionality.MP3ConverterActivity
import com.jaidev.seeaplayer.MP3ConverterFunctionality.MP3playerActivity
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.exitApplication
import com.jaidev.seeaplayer.dataClass.favouriteChecker
import com.jaidev.seeaplayer.dataClass.formatDuration
import com.jaidev.seeaplayer.dataClass.getImgArt
import com.jaidev.seeaplayer.dataClass.setSongPosition
import com.jaidev.seeaplayer.musicActivity.NowPlaying
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity.Companion.songPosition
import com.jaidev.seeaplayer.recantFragment.ReMusicPlayerActivity

class MusicService: Service(), AudioManager.OnAudioFocusChangeListener {
    private var myBinder = MyBinder()
    var mediaPlayer: MediaPlayer? = null
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var runnable: Runnable
    private val handler = Handler(Looper.getMainLooper()) // Handler instance
    var isPlaying: Boolean = false
    var isReSeekSetupRunning = false // Flag to indicate whether reSeekSetup() is running
    var isNotificationSeeking = false // Flag to indicate if notification seek is in progress

    fun stopService() {
        stopForeground(true)
        stopSelf()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        handler.removeCallbacks(runnable) // Stop the runnable
        PlayerMusicActivity.musicService = null
        val intent = Intent(MP3ConverterActivity.ACTION_HIDE_NOW_PLAYING)
        sendBroadcast(intent)
    }

    override fun onBind(intent: Intent?): IBinder {
        mediaSession = MediaSessionCompat(baseContext, "My Music")
        return myBinder
    }

    inner class MyBinder : Binder() {
        fun currentService(): MusicService {
            return this@MusicService
        }
    }


    @SuppressLint("UnspecifiedImmutableFlag", "ForegroundServiceType")
    fun showNotification(playPauseBtn: Int) {
        val intent = Intent(baseContext, MainActivity::class.java)
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val contentIntent = PendingIntent.getActivity(this, 0, intent, flag)
        val prevIntent = Intent(baseContext, NotificationReceiver::class.java).setAction(
            ApplicationClass.PREVIOUS)
        val prevPendingIntent = PendingIntent.getBroadcast(baseContext, 0, prevIntent, flag)
        val replayIntent = Intent(baseContext, NotificationReceiver::class.java).setAction(
            ApplicationClass.REPLAY)
        val replayPendingIntent = PendingIntent.getBroadcast(baseContext, 0, replayIntent, flag)
        val playIntent = Intent(baseContext, NotificationReceiver::class.java).setAction(
            ApplicationClass.PLAY)
        val playPendingIntent = PendingIntent.getBroadcast(baseContext, 0, playIntent, flag)
        val forwardIntent = Intent(baseContext, NotificationReceiver::class.java).setAction(
            ApplicationClass.FORWARD)
        val forwardPendingIntent = PendingIntent.getBroadcast(baseContext, 0, forwardIntent, flag)
        val nextIntent = Intent(baseContext, NotificationReceiver::class.java).setAction(
            ApplicationClass.NEXT)
        val nextPendingIntent = PendingIntent.getBroadcast(baseContext, 0, nextIntent, flag)

        val imgArt = getImgArt(PlayerMusicActivity.musicListPA[songPosition].path)
        val image = if (imgArt != null) {
            BitmapFactory.decodeByteArray(imgArt, 0, imgArt.size)
        } else {
            BitmapFactory.decodeResource(resources, R.drawable.music_speaker_three)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(baseContext, ApplicationClass.CHANNEL_ID)
            .setContentIntent(contentIntent)
            .setContentTitle(PlayerMusicActivity.musicListPA[songPosition].title)
            .setContentText(PlayerMusicActivity.musicListPA[songPosition].artist)
            .setSmallIcon(R.drawable.music_icon)
            .setLargeIcon(image)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(mediaSession.sessionToken))
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.round_previous, "Previous", prevPendingIntent)
            .addAction(R.drawable.round_replay_10, "Replay", replayPendingIntent)
            .addAction(playPauseBtn, "Play", playPendingIntent)
            .addAction(R.drawable.round_forward_10, "Forward", forwardPendingIntent)
            .addAction(R.drawable.round_next, "Next", nextPendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaSession.setMetadata(MediaMetadataCompat.Builder().putLong(
                MediaMetadataCompat.METADATA_KEY_DURATION, mediaPlayer!!.duration.toLong()
            ).build())

            mediaSession.setPlaybackState(getPlayBackState())
            mediaSession.setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    super.onPlay()
                    handlePlayPause()
                }

                override fun onPause() {
                    super.onPause()
                    handlePlayPause()
                }

                override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
                    handlePlayPause()
                    return super.onMediaButtonEvent(mediaButtonEvent)
                }

                override fun onSkipToNext() {
                    super.onSkipToNext()
                    prevNextSong(increment = true, context = this@MusicService)
                }

                override fun onSkipToPrevious() {
                    super.onSkipToPrevious()
                    prevNextSong(increment = false, context = this@MusicService)
                }

                override fun onStop() {
                    super.onStop()
                    exitApplication()
                }

                override fun onSeekTo(pos: Long) {
                    super.onSeekTo(pos)
                    mediaPlayer?.seekTo(pos.toInt())
                    isNotificationSeeking = true
                    if (!isReSeekSetupRunning) {
                        mediaSession.setPlaybackState(getPlayBackState())
                    }
                }
            })
        }
        startForeground(13, notification)
    }

    fun createMediaPlayer() {
        try {
            if (mediaPlayer == null) mediaPlayer = MediaPlayer()
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(PlayerMusicActivity.musicListPA[songPosition].path)
            mediaPlayer?.prepare()

            PlayerMusicActivity.binding.playPauseBtnPA.setIconResource(R.drawable.round_pause_24)
            showNotification(R.drawable.round_pause_24)
            PlayerMusicActivity.binding.tvSeekBarStart.text = formatDuration(mediaPlayer!!.currentPosition.toLong())
            PlayerMusicActivity.binding.tvSeekBarEnd.text = formatDuration(mediaPlayer!!.duration.toLong())
            PlayerMusicActivity.binding.seekBarPA.progress = 0
            PlayerMusicActivity.binding.seekBarPA.max = mediaPlayer!!.duration
            PlayerMusicActivity.nowMusicPlayingId = PlayerMusicActivity.musicListPA[songPosition].musicid
            PlayerMusicActivity.loudnessEnhancer = LoudnessEnhancer(mediaPlayer!!.audioSessionId)
            PlayerMusicActivity.loudnessEnhancer.enabled = true
        } catch (e: Exception) {
            return
        }
    }
    fun createMediaPlayer1() {
        try {
            if (mediaPlayer == null) mediaPlayer = MediaPlayer()
            mediaPlayer?.reset()
            mediaPlayer?.setDataSource(ReMusicPlayerActivity.reMusicListPA[songPosition].path)
            mediaPlayer?.prepare()

            ReMusicPlayerActivity.binding.playPauseBtnPA.setIconResource(R.drawable.round_pause_24)
            showNotification(R.drawable.round_pause_24)
            ReMusicPlayerActivity.binding.tvSeekBarStart.text = formatDuration(mediaPlayer!!.currentPosition.toLong())
            ReMusicPlayerActivity.binding.tvSeekBarEnd.text = formatDuration(mediaPlayer!!.duration.toLong())
            ReMusicPlayerActivity.binding.seekBarPA.progress = 0
            ReMusicPlayerActivity.binding.seekBarPA.max = mediaPlayer!!.duration
            ReMusicPlayerActivity.nowMusicPlayingId = ReMusicPlayerActivity.reMusicListPA[songPosition].id
            PlayerMusicActivity.loudnessEnhancer = LoudnessEnhancer(mediaPlayer!!.audioSessionId)
            PlayerMusicActivity.loudnessEnhancer.enabled = true
        } catch (e: Exception) {
            return
        }
    }

    fun seekBarSetup() {
        try {
            runnable = Runnable {
                PlayerMusicActivity.binding.tvSeekBarStart.text = formatDuration(mediaPlayer!!.currentPosition.toLong())
                PlayerMusicActivity.binding.seekBarPA.progress = mediaPlayer!!.currentPosition
                handler.postDelayed(runnable, 200)
            }
            handler.postDelayed(runnable, 0)
        } catch (_: Exception) { }
    }

    fun reSeekSetup() {
        if (isNotificationSeeking) {
            mediaPlayer?.let {
                it.seekTo(it.currentPosition)
                mediaSession.setPlaybackState(getPlayBackState())
                isNotificationSeeking = false
            }
        }
        try {
            runnable = Runnable {
                ReMusicPlayerActivity.binding.tvSeekBarStart.text = formatDuration(mediaPlayer!!.currentPosition.toLong())
                ReMusicPlayerActivity.binding.seekBarPA.progress = mediaPlayer!!.currentPosition
                handler.postDelayed(runnable, 200)
            }
            handler.postDelayed(runnable, 0)
        } catch (_: Exception) { }
    }

    fun getPlayBackState(): PlaybackStateCompat {
        val playbackSpeed = if (PlayerMusicActivity.isPlaying) 1F else 0F

        return PlaybackStateCompat.Builder()
            .setState(
                if (mediaPlayer?.isPlaying == true) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED,
                mediaPlayer!!.currentPosition.toLong(), playbackSpeed
            )
            .setActions(
                PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_STOP or
                        PlaybackStateCompat.ACTION_SEEK_TO
            )
            .build()
    }

    fun handlePlayPause() {
        if (PlayerMusicActivity.isPlaying) {
            PlayerMusicActivity.binding.playPauseBtnPA.setIconResource(R.drawable.round_play)
            NowPlaying.binding.playPauseBtnNP.setIconResource(R.drawable.round_play)
            PlayerMusicActivity.isPlaying = false
            mediaPlayer?.pause()
            showNotification(R.drawable.round_play)
        } else {
            PlayerMusicActivity.binding.playPauseBtnPA.setIconResource(R.drawable.round_pause_24)
            NowPlaying.binding.playPauseBtnNP.setIconResource(R.drawable.round_pause_24)
            PlayerMusicActivity.isPlaying = true
            mediaPlayer?.start()
            showNotification(R.drawable.round_pause_24)
        }
        mediaSession.setPlaybackState(getPlayBackState())
    }

    fun prevNextSong(increment: Boolean, context: Context) {
        setSongPosition(increment = increment)
        createMediaPlayer()
        Glide.with(context)
            .load(PlayerMusicActivity.musicListPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
            .into(PlayerMusicActivity.binding.songImgPA)

        PlayerMusicActivity.binding.songNamePA.text = PlayerMusicActivity.musicListPA[songPosition].title
        Glide.with(context)
            .load(PlayerMusicActivity.musicListPA[songPosition].artUri)
            .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
            .into(NowPlaying.binding.songImgNP)

        NowPlaying.binding.songNameNP.text = PlayerMusicActivity.musicListPA[songPosition].title
        playMusic()

        PlayerMusicActivity.fIndex = favouriteChecker(PlayerMusicActivity.musicListPA[songPosition].musicid)

        if (PlayerMusicActivity.isFavourite) {
            PlayerMusicActivity.binding.favouriteBtnPA.setImageResource(R.drawable.round_favorite_music)
        } else {
            PlayerMusicActivity.binding.favouriteBtnPA.setImageResource(R.drawable.round_favorite_border_music)
        }
    }

    private fun playMusic() {
        PlayerMusicActivity.isPlaying = true
        mediaPlayer!!.start()
        showNotification(R.drawable.round_pause_notification)
        PlayerMusicActivity.binding.playPauseBtnPA.setIconResource(R.drawable.round_pause_24)
        try {
            NowPlaying.binding.playPauseBtnNP.setIconResource(R.drawable.round_pause_24)
        } catch (_: Exception) { }
    }

    override fun onAudioFocusChange(focusChange: Int) {
        if (focusChange <= 0) {
            PlayerMusicActivity.binding.playPauseBtnPA.setIconResource(R.drawable.round_play)
            NowPlaying.binding.playPauseBtnNP.setIconResource(R.drawable.round_play)
            PlayerMusicActivity.isPlaying = false
            mediaPlayer!!.pause()
            showNotification(R.drawable.round_play)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MP3playerActivity.musicMP3Service?.stopService()
        return START_STICKY
    }

}