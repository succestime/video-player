package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.jaidev.seeaplayer.allAdapters.MusicAdapter2
import com.jaidev.seeaplayer.dataClass.DatabaseClientMusic
import com.jaidev.seeaplayer.databinding.PlaylsitMusicSelectionBottomSheetBinding
import com.jaidev.seeaplayer.musicActivity.PlaylistDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddMusicBottomSheetFragment : BottomSheetDialogFragment(), MusicAdapter2.OnSelectionChangeListener{

    private lateinit var _binding: PlaylsitMusicSelectionBottomSheetBinding
    private val binding get() = _binding
    private lateinit var adapter: MusicAdapter2
    private var playlistId: Long = 0L

    companion object {
        private const val ARG_PLAYLIST_ID = "playlist_id"

        fun newInstance(playlistId: Long): AddMusicBottomSheetFragment {
            val fragment = AddMusicBottomSheetFragment()
            val args = Bundle().apply {
                putLong(ARG_PLAYLIST_ID, playlistId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            playlistId = it.getLong(ARG_PLAYLIST_ID)
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = PlaylsitMusicSelectionBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView with LayoutManager
        binding.playlistSelectionVideoRV.layoutManager = LinearLayoutManager(context)
        binding.playlistSelectionVideoRV.setHasFixedSize(true)
        binding.playlistSelectionVideoRV.setItemViewCacheSize(15)

        // Load the videos and setup adapter
        lifecycleScope.launch {
            val videosInPlaylist = withContext(Dispatchers.IO) {
                val db = DatabaseClientMusic.getInstance(requireContext())
                db.playlistMusicDao().getMusicIdsForPlaylist(playlistId).toSet()
            }

            // Ensure UI updates are done on the main thread
            withContext(Dispatchers.Main) {
                adapter = MusicAdapter2(requireContext(), MainActivity.MusicListMA, videosInPlaylist)
                binding.playlistSelectionVideoRV.adapter = adapter
                adapter.setOnSelectionChangeListener(this@AddMusicBottomSheetFragment)

                updateAddSelectedVideoButtonVisibility()
            }
        }


        binding.clearBottomSheet.setOnClickListener {
            dismiss()
        }
        binding.addSelectedVideo.setOnClickListener {
            val selectedVideos = adapter.getSelectedVideos()
            (activity as? PlaylistDetails)?.addSelectedVideos(selectedVideos)
            dismiss()
        }
        // Set up SearchView to filter items in the adapter
        binding.searchViewSA.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter(newText ?: "")
                return true
            }
        })

        updateAddSelectedVideoButtonVisibility()

    }
    override fun onSelectionChanged(count: Int) {
        updateAddSelectedVideoButtonVisibility()
        updateTotalVideoSelectedText(count)
    }

    @SuppressLint("SetTextI18n")
    private fun updateTotalVideoSelectedText(count: Int) {
        binding.totalVideoSelected.text = "Add to Playlist ($count ${if (count == 1) "song" else "songs"})"
    }
    private fun updateAddSelectedVideoButtonVisibility() {
        if (this::adapter.isInitialized && adapter.getSelectedItemCount() > 0) {
            binding.addSelectedVideo.visibility = View.VISIBLE
        } else {
            binding.addSelectedVideo.visibility = View.GONE
        }
    }
    override fun onStart() {
        super.onStart()

        dialog?.let {
            val bottomSheet = dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT

            val behavior = BottomSheetBehavior.from(bottomSheet!!)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }
    }

}