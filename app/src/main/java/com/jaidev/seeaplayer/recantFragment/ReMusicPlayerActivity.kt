package com.jaidev.seeaplayer.recantFragment

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.media.MediaPlayer
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.MusicService
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.dataClass.RecantMusic
import com.jaidev.seeaplayer.dataClass.exitApplication
import com.jaidev.seeaplayer.dataClass.reFormatDuration
import com.jaidev.seeaplayer.databinding.ActivityReMusicPlayerBinding

class ReMusicPlayerActivity : AppCompatActivity()
    , ServiceConnection, MediaPlayer.OnCompletionListener
{


    companion object {
        // of PlayerActivity of this reMusicActivity
        lateinit var reMusicList: ArrayList<RecantMusic>
        var songPosition: Int = 0
      var isPlaying: Boolean = false
        var musicService : MusicService? = null
        private var isServiceBound = false

        var min15: Boolean = false
        var min30: Boolean = false
        var min60: Boolean = false

@SuppressLint("StaticFieldLeak")
lateinit var binding: ActivityReMusicPlayerBinding
        var repeat: Boolean = false
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReMusicPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        initializeLayout()

       binding.backBtnPA.setOnClickListener { finish() }

      binding.playPauseBtnPA.setOnClickListener {
            if (isPlaying) pauseMusic()
            else playMusic()
        }

     binding.previousBtnPA.setOnClickListener {
            prevNextSong(increment = false)
        }
       binding.nextBtnPA.setOnClickListener {
            prevNextSong(increment = true)
        }
        binding.seekBarRPA.setOnSeekBarChangeListener(object  : OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
             if (fromUser) musicService!!.mediaPlayer!!.seekTo(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        binding.repeatBtnPA.setOnClickListener {
            if (!repeat) {
                repeat = true
                binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_green))
            } else {
               repeat = false
               binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_pink))
            }
        }
       binding.equalizerBtnPA.setOnClickListener {
            try {
                val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
                eqIntent.putExtra(
                    AudioEffect.EXTRA_AUDIO_SESSION,
                    musicService!!.mediaPlayer!!.audioSessionId
                )
                eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, baseContext.packageName)
                eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                startActivityForResult( eqIntent, 13)
            } catch (e: Exception) {
                Toast.makeText(this, "Equalizer Feature not Supported!!", Toast.LENGTH_SHORT).show()
            }
        }

       binding.timerBtnPA.setOnClickListener {
            val timer = min15 || min30 || min60
            if (!timer) showBottomSheetDialog()
            else {
                val builder = MaterialAlertDialogBuilder(this)
                builder.setTitle("Stop Timer")
                    .setMessage("Do you want to stop timer?")
                    .setPositiveButton("Yes") { _, _ ->
                     min15 = false
                     min30 = false
                     min60 = false
                     binding.timerBtnPA.setColorFilter(
                            ContextCompat.getColor(
                                this,
                                R.color.cool_pink
                            )
                        )
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                    }
                val customDialog = builder.create()
                customDialog.show()
                customDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.RED)
                customDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.GREEN)
            }
        }

       binding.shareBtnPA.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "audio/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(reMusicList[songPosition].path))
            startActivity(Intent.createChooser(shareIntent, "Sharing Music File!!"))

        }
    }
    private fun setLayout(){
        Glide.with(this)
            .asBitmap()
            .load(reMusicList[songPosition].albumArtUri)
            .apply(RequestOptions().placeholder(R.drawable.speaker)).centerCrop()
            .into(binding.songImgPA)
       binding.songNamePA.text = reMusicList[songPosition].title
        if (repeat) binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(applicationContext,
            R.color.cool_green
        ))
        if(min15 || min30 || min60) binding.timerBtnPA.setColorFilter(ContextCompat.getColor(applicationContext,
            R.color.cool_green
        ))


    }
           private fun createMediaPlayer(){
               try {
                   if (musicService!!.mediaPlayer == null) musicService!!.mediaPlayer = MediaPlayer()
                   musicService!!.mediaPlayer!!.reset()
                   musicService!!.mediaPlayer!!.setDataSource(reMusicList[songPosition].path)
                   musicService!!.mediaPlayer!!.prepare()
                   musicService!!.mediaPlayer!!.start()
                   isPlaying = true
                   binding.tvSeekBarStart1.text = reFormatDuration(musicService!!.mediaPlayer!!.currentPosition.toLong())
                   binding.tvSeekBarEnd2.text = reFormatDuration(musicService!!.mediaPlayer!!.duration.toLong())
                  binding.seekBarRPA.progress = 0
                   binding.seekBarRPA.max = musicService!!.mediaPlayer!!.duration
                   binding.playPauseBtnPA.setIconResource(R.drawable.ic_pause_icon)
                   musicService!!.mediaPlayer!!.setOnCompletionListener(this)
               }catch (e : Exception){return}
           }

    private fun initializeLayout(){
        songPosition =  intent.getIntExtra("index" , 0)
        when(intent.getStringExtra("class")){
            "RecantMusicAdapter" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                reMusicList = ArrayList()
                reMusicList.addAll(MainActivity.musicRecantList)
                setLayout()

            }
            "DaysMusic" -> {
                val intent = Intent(this, MusicService::class.java)
                bindService(intent, this, BIND_AUTO_CREATE)
                startService(intent)
                reMusicList = ArrayList()
                reMusicList.addAll(MainActivity.musicRecantList)
                reMusicList.shuffle()
                setLayout()
            }
        }
    }
    private fun playMusic() {
     isPlaying = true
      binding.playPauseBtnPA.setIconResource(R.drawable.ic_pause_icon)
      musicService!!.mediaPlayer!!.start()

    }

    private fun pauseMusic() {
        isPlaying = false
       binding.playPauseBtnPA.setIconResource(R.drawable.play_music_icon)
        musicService!!.mediaPlayer!!.pause()


    }
    private fun prevNextSong(increment: Boolean) {
        if (increment) {
            setSongPosition(increment = true)
            setLayout()
            createMediaPlayer()
        } else {
            setSongPosition(increment = false)
            setLayout()
            createMediaPlayer()
        }
    }
     private fun setSongPosition(increment: Boolean) {

        if(!repeat){
            if (increment) {
                if (reMusicList.size - 1 == songPosition)
                    songPosition = 0
                else ++songPosition
            } else {
                if (0 == songPosition)
                    songPosition = reMusicList.size - 1
                else --songPosition
            }

        }
    }


        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            if (service is MusicService.MyBinder) {
                musicService = service.currentService()
                isServiceBound = true
            }
            createMediaPlayer()
            musicService!!.reSeekSetup()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isServiceBound = false
        }


    override fun onDestroy() {
        super.onDestroy()
        if (isServiceBound) {
            isServiceBound = false
        }
    }

    override fun onCompletion(p0: MediaPlayer?) {
        setSongPosition(increment = true)
        createMediaPlayer()
        try {
            setLayout()
        }catch (e: Exception){return}
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 13 || resultCode == RESULT_OK)
            return
    }

    private fun showBottomSheetDialog() {
        val dialog = BottomSheetDialog(this@ReMusicPlayerActivity)
        dialog.setContentView(R.layout.bottom_sheet_dialog)
        dialog.show()
        dialog.findViewById<LinearLayout>(R.id.min_15)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 15 minutes", Toast.LENGTH_SHORT)
                .show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_green))
           min15 = true
            Thread {
                Thread.sleep((15 * 60000).toLong())
                if (min15) exitApplication() }.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_30)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 30 minutes", Toast.LENGTH_SHORT)
                .show()
            binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_green))
            min30 = true
            Thread {
                Thread.sleep((30 * 60000).toLong())
                if (min30) exitApplication() }.start()
            dialog.dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_60)?.setOnClickListener {
            Toast.makeText(baseContext, "Music will stop after 60 minutes", Toast.LENGTH_SHORT)
                .show()
           binding.timerBtnPA.setColorFilter(ContextCompat.getColor(this, R.color.cool_green))
            min60 = true
            Thread {
                Thread.sleep((60 * 60000).toLong())
                if (min60) exitApplication() }.start()
            dialog.dismiss()
        }
    }
}


