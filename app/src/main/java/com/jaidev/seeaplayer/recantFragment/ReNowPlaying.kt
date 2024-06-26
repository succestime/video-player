package com.jaidev.seeaplayer.recantFragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.RecantMusicAdapter
import com.jaidev.seeaplayer.dataClass.getImgArt
import com.jaidev.seeaplayer.dataClass.reSetSongPosition
import com.jaidev.seeaplayer.databinding.FragmentReNowPlayingBinding

class ReNowPlaying : Fragment(), RecantMusicAdapter.MusicDeleteListener   {
    lateinit var adapter: RecantMusicAdapter
    companion object{
       @SuppressLint("StaticFieldLeak")
        lateinit var binding: FragmentReNowPlayingBinding
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_re_now_playing, container, false)
        binding = FragmentReNowPlayingBinding.bind(view)
        binding.root.visibility = View.INVISIBLE
        adapter = RecantMusicAdapter(requireContext(), MainActivity.musicRecantList)

        initializeBinding()

        return view
    }

    private fun initializeBinding(){
        binding.playPauseBtnNP.setOnClickListener {
            if(ReMusicPlayerActivity.isPlaying){ pauseMusic()
               }
            else {
                playMusic()
            }
        }
        binding.nextBtnNP.setOnClickListener {
            reSetSongPosition(increment = true)
            ReMusicPlayerActivity.createMediaPlayer()

            Glide.with(this)
                .asBitmap()
                .load(getImgArt(ReMusicPlayerActivity.reMusicList[ReMusicPlayerActivity.songPosition].path))
                .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
                .into(binding.songImgNP)
            binding.songNameNP.text = ReMusicPlayerActivity.reMusicList[ReMusicPlayerActivity.songPosition].title
            playMusic()
        }
        binding.root.setOnClickListener {
            val intent = Intent(requireContext(), ReMusicPlayerActivity::class.java)
            intent.putExtra("index", ReMusicPlayerActivity.songPosition)
            intent.putExtra("class", "ReNowPlaying")
            ContextCompat.startActivity(requireContext(), intent, null)
        }

    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()
        if(ReMusicPlayerActivity.musicService != null){
           // requireActivity().registerReceiver(receiver, IntentFilter(ACTION_MUSIC_DELETED),
             //   Context.RECEIVER_NOT_EXPORTED)

            binding.root.visibility = View.VISIBLE
            binding.songNameNP.isSelected = true
            Glide.with(requireContext())
                .asBitmap()
                .load(getImgArt(ReMusicPlayerActivity.reMusicList[ReMusicPlayerActivity.songPosition].path))
                .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
                .into(binding.songImgNP)
            binding.songNameNP.text = ReMusicPlayerActivity.reMusicList[ReMusicPlayerActivity.songPosition].title
            if(ReMusicPlayerActivity.isPlaying) binding.playPauseBtnNP.setIconResource(R.drawable.round_pause_24)
            else binding.playPauseBtnNP.setIconResource(R.drawable.round_play)

        }
    }

    private fun playMusic(){
        ReMusicPlayerActivity.isPlaying = true
        ReMusicPlayerActivity.musicService!!.mediaPlayer!!.start()
        binding.playPauseBtnNP.setIconResource(R.drawable.round_pause_24)

//        ReMusicPlayerActivity.musicService!!.showNotification(R.drawable.ic_pause_icon)
    }
    private fun pauseMusic(){
        ReMusicPlayerActivity.isPlaying = false
        ReMusicPlayerActivity.musicService!!.mediaPlayer!!.pause()
        binding.playPauseBtnNP.setIconResource(R.drawable.round_play)

//        ReMusicPlayerActivity.musicService!!.showNotification(R.drawable.play_music_icon)
    }
    override fun onMusicDeleted() {
        // Check if the NowPlaying fragment is currently visible
        if (isVisible) {
            // Refresh the UI with the current playing music or take any other necessary action
            refreshNowPlayingUI()
        }
    }

    private fun refreshNowPlayingUI() {

        if (ReMusicPlayerActivity.isPlaying) {

          binding.playPauseBtnNP.setIconResource(R.drawable.round_pause_24)

        } else {
        binding.playPauseBtnNP.setIconResource(R.drawable.round_play)
        }
        ReNowPlaying.binding.songNameNP.text = ReMusicPlayerActivity.reMusicList[ReMusicPlayerActivity.songPosition].title
        Glide.with(requireContext())
            .asBitmap()
            .load(getImgArt(ReMusicPlayerActivity.reMusicList[ReMusicPlayerActivity.songPosition].path))
            .apply(RequestOptions().placeholder(R.drawable.music_speaker_three).centerCrop())
            .into(binding.songImgNP)
    }


}