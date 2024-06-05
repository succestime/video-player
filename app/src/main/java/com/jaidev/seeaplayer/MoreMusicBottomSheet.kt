package com.jaidev.seeaplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.dataClass.exitApplication
import com.jaidev.seeaplayer.databinding.MoreMusicBottomSheetBinding
import com.jaidev.seeaplayer.musicActivity.PlayerMusicActivity

class MoreMusicBottomSheet : BottomSheetDialogFragment() {

    private lateinit var binding: MoreMusicBottomSheetBinding
    private var currentPlaybackSpeed: Float = 1.0f // Default speed

    companion object {
        const val REQUEST_AUDIO_PERMISSION = 100
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MoreMusicBottomSheetBinding.inflate(inflater, container, false)
        // Retrieve the current playback speed from arguments
        currentPlaybackSpeed = arguments?.getFloat("CURRENT_SPEED") ?: 1.0f

        setMusicLayoutBackgroundColor()

        binding.shareText.setOnClickListener {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "audio/*"
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(PlayerMusicActivity.musicListPA[PlayerMusicActivity.songPosition].path))
            startActivity(Intent.createChooser(shareIntent, "Sharing Music File!!"))
            dismiss()
        }

        loadThumbnailImage()

        binding.repeat.setOnClickListener {
            if (!PlayerMusicActivity.repeat) {
                PlayerMusicActivity.repeat = true
                PlayerMusicActivity.binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(requireContext(), R.color.cool_green))
                Toast.makeText(requireContext(), "Repeat mode is on", Toast.LENGTH_SHORT).show()
                dismiss()
            } else {
                PlayerMusicActivity.repeat = false
                Toast.makeText(requireContext(), "Repeat mode is off", Toast.LENGTH_SHORT).show()
                PlayerMusicActivity.binding.repeatBtnPA.setColorFilter(ContextCompat.getColor(requireContext(), R.color.cool_pink))
                dismiss()
            }
        }

        binding.speed.setOnClickListener {
            val speedBottomSheet = SpeedMusicBottomSheet()
            speedBottomSheet.setCurrentSpeed(currentPlaybackSpeed)
            speedBottomSheet.show(parentFragmentManager, speedBottomSheet.tag)
            dismiss()
        }

        binding.equalizer.setOnClickListener {
            if (isEqualizerSupported()) {
                if (checkPermission()) {
                    launchEqualizer()
                    dismiss()
                } else {
                    requestPermission()
                }
            } else {
                Toast.makeText(requireContext(), "Equalizer Feature not Supported!!", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }

        binding.sleepTimer.setOnClickListener {
            val timer = PlayerMusicActivity.min10 || PlayerMusicActivity.min15 || PlayerMusicActivity.min20 || PlayerMusicActivity.min30 || PlayerMusicActivity.min60
            if (!timer) showBottomSheetDialog()
            else {
                val builder = MaterialAlertDialogBuilder(requireContext())
                builder.setTitle("Stop Timer")
                    .setMessage("Do you want to stop timer?")
                    .setPositiveButton("Yes") { _, _ ->
                        PlayerMusicActivity.min10 = false
                        PlayerMusicActivity.min15 = false
                        PlayerMusicActivity.min20 = false
                        PlayerMusicActivity.min30 = false
                        PlayerMusicActivity.min60 = false
                        dismiss()
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        dialog.dismiss()
                        dismiss()
                    }
                val customDialog = builder.create()
                customDialog.show()
            }
        }

        return binding.root
    }

    private fun setMusicLayoutBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to dark_cool_blue
            requireActivity().window.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.dark_cool_blue)
        } else {
            // Light mode is enabled, set background color to white
            requireActivity().window.navigationBarColor = ContextCompat.getColor(requireContext(), R.color.white)
        }
    }

    private fun isEqualizerSupported(): Boolean {
        val pm = requireContext().packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_AUDIO_OUTPUT)
    }

    private fun checkPermission(): Boolean {
        val permission = Manifest.permission.MODIFY_AUDIO_SETTINGS
        return ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.MODIFY_AUDIO_SETTINGS), REQUEST_AUDIO_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchEqualizer()
            } else {
                Toast.makeText(requireContext(), "Permission denied to access the equalizer", Toast.LENGTH_SHORT).show()
                // Optionally, prompt the user to grant permission from app settings
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", requireContext().packageName, null)
                startActivity(intent)
            }
        }
    }

    private fun loadThumbnailImage() {
        val currentSong = (requireActivity() as PlayerMusicActivity).getCurrentSong()
        val imageUrl = currentSong.artUri

        Glide.with(this)
            .load(imageUrl)
            .placeholder(R.drawable.music_speaker_three)
            .error(R.drawable.music_speaker_three)
            .into(binding.thumbnail)
        // Set the title of the current playing music
        binding.titleMusic.text = currentSong.title
        binding.subtitleMusic.text = currentSong.album
    }

    private fun launchEqualizer() {
        try {
            val eqIntent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL)
            eqIntent.putExtra(
                AudioEffect.EXTRA_AUDIO_SESSION,
                PlayerMusicActivity.musicService!!.mediaPlayer!!.audioSessionId
            )
            eqIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, requireContext().packageName)
            eqIntent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
            startActivityForResult(eqIntent, 13)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Equalizer Feature not Supported!!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showBottomSheetDialog() {
        val dialog = BottomSheetDialog(requireActivity())
        dialog.setContentView(R.layout.bottom_sheet_dialog)
        dialog.show()
        dialog.findViewById<LinearLayout>(R.id.min_10)?.setOnClickListener {
            Toast.makeText(requireActivity(), "Music will stop after 10 minutes", Toast.LENGTH_SHORT).show()
            PlayerMusicActivity.min10 = true
            Thread {
                Thread.sleep((10 * 60000).toLong())
                if (PlayerMusicActivity.min10) exitApplication()
            }.start()
            dialog.dismiss()
            dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_15)?.setOnClickListener {
            Toast.makeText(requireActivity(), "Music will stop after 15 minutes", Toast.LENGTH_SHORT).show()
            PlayerMusicActivity.min15 = true
            Thread {
                Thread.sleep((15 * 60000).toLong())
                if (PlayerMusicActivity.min15) exitApplication()
            }.start()
            dialog.dismiss()
            dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_20)?.setOnClickListener {
            Toast.makeText(requireActivity(), "Music will stop after 20 minutes", Toast.LENGTH_SHORT).show()
            PlayerMusicActivity.min20 = true
            Thread {
                Thread.sleep((20 * 60000).toLong())
                if (PlayerMusicActivity.min20) exitApplication()
            }.start()
            dialog.dismiss()
            dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_30)?.setOnClickListener {
            Toast.makeText(requireActivity(), "Music will stop after 30 minutes", Toast.LENGTH_SHORT).show()
            PlayerMusicActivity.min30 = true
            Thread {
                Thread.sleep((30 * 60000).toLong())
                if (PlayerMusicActivity.min30) exitApplication()
            }.start()
            dialog.dismiss()
            dismiss()
        }
        dialog.findViewById<LinearLayout>(R.id.min_60)?.setOnClickListener {
            Toast.makeText(requireActivity(), "Music will stop after 60 minutes", Toast.LENGTH_SHORT).show()
            PlayerMusicActivity.min60 = true
            Thread {
                Thread.sleep((60 * 60000).toLong())
                if (PlayerMusicActivity.min60) exitApplication()
            }.start()
            dialog.dismiss()
            dismiss()
        }
    }

    override fun onResume() {
        super.onResume()
        setMusicLayoutBackgroundColor()
    }
}
