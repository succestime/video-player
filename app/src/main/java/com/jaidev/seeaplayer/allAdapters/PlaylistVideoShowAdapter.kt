package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.imageview.ShapeableImageView
import com.jaidev.seeaplayer.PlaylistVideoActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.browserActivity.PlayerFileActivity
import com.jaidev.seeaplayer.dataClass.VideoData
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PlaylistVideoShowAdapter(
    private val context: Context,
    private var videoList: List<VideoData>,
    private val onVideoRemoved: (VideoData) -> Unit
) : RecyclerView.Adapter<PlaylistVideoShowAdapter.VideoViewHolder>() {

    private val selectedItems = mutableSetOf<Int>()
    private val selectedVideos = mutableSetOf<VideoData>()

    var isSelectionMode = false
    interface OnSelectionChangeListener {
        fun onSelectionChanged(isAllSelected: Boolean)
    }

    var selectionChangeListener: OnSelectionChangeListener? = null
    fun getVideos(): List<VideoData> {
        return videoList
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateVideoList(newList: List<VideoData>) {
        videoList = newList
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection() {
        selectedItems.clear()
        isSelectionMode = true
        notifyDataSetChanged()
        selectionChangeListener?.onSelectionChanged(isAllSelected = false)

        // Notify the activity to update UI elements like the title
        val activity = context as PlaylistVideoActivity
        activity.updateSelectionMode(true)
        activity.updatePlaylistName(0)
    }
    fun getSelectedVideos(): List<VideoData> {
        return videoList.filter { it.selected }
    }
    // Inside PlaylistVideoShowAdapter class

    @SuppressLint("NotifyDataSetChanged")
    fun selectAll() {
        selectedItems.clear()
        for (i in videoList.indices) {
            selectedItems.add(i)
        }
        notifyDataSetChanged()

        // Update selection mode and notify listener
        isSelectionMode = true
        val activity = context as PlaylistVideoActivity
        activity.updateSelectionMode(true)
        activity.updatePlaylistName(selectedItems.size)
        selectionChangeListener?.onSelectionChanged(selectedItems.size == videoList.size)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateSelectionMode() {
        val activity = context as PlaylistVideoActivity

        // Update the playlist name to show the number of selected items
        val selectedCount = selectedItems.size
        activity.updatePlaylistName(selectedCount)

        // Keep the selection mode active even if no items are selected
        isSelectionMode = true
        activity.updateSelectionMode(true)

        // Trigger UI updates
        notifyDataSetChanged()

        // Notify the listener about the selection state
        selectionChangeListener?.onSelectionChanged(selectedCount == videoList.size)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun disableSelectionMode() {
        isSelectionMode = false
        selectedItems.clear()
        notifyDataSetChanged()

        // Notify the activity to update UI elements like the title
        val activity = context as PlaylistVideoActivity
        activity.updateSelectionMode(false)
        activity.updatePlaylistName(0) // Set to "0 videos selected"
    }


    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.videoName)
        val durationTextView: TextView = itemView.findViewById(R.id.duration)
        val videoImage: ShapeableImageView = itemView.findViewById(R.id.videoImage)
        val moreChoose: ImageButton = itemView.findViewById(R.id.MoreChoose)
        val emptyCheck: ImageButton = itemView.findViewById(R.id.emptyCheck)
        val fillCheck: ImageButton = itemView.findViewById(R.id.fillCheck)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.playlist_video_video_view, parent, false)
        return VideoViewHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoList[position]
        holder.titleTextView.text = video.title
        holder.durationTextView.text = DateUtils.formatElapsedTime(video.duration / 1000)
        Glide.with(context)
            .load(video.artUri)
            .apply(RequestOptions().placeholder(R.color.place_holder_video).centerCrop())
            .into(holder.videoImage)


        // If selection mode is enabled
        if (isSelectionMode) {
            holder.emptyCheck.visibility = View.VISIBLE
            holder.moreChoose.visibility = View.GONE

            // If the item is selected
            if (selectedItems.contains(position)) {
                holder.fillCheck.visibility = View.VISIBLE
                holder.emptyCheck.visibility = View.GONE
            } else {
                holder.fillCheck.visibility = View.GONE
                holder.emptyCheck.visibility = View.VISIBLE
            }
        } else {
            // If selection mode is not enabled
            holder.emptyCheck.visibility = View.GONE
            holder.fillCheck.visibility = View.GONE
            holder.moreChoose.visibility = View.VISIBLE
        }
        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(position)
            } else {
                val intent = Intent(context, PlayerFileActivity::class.java).apply {
                    putExtra("videoUri", video.path)
                    putExtra("videoTitle", video.title)
                }
                context.startActivity(intent)
            }
        }

        // Handle long click to enable selection mode
        holder.itemView.setOnLongClickListener {
            if (!isSelectionMode) {
                isSelectionMode = true
                updateSelectionMode()
                notifyDataSetChanged() // Refresh to show/hide checkboxes for all items
            }
            toggleSelection(position)
            true
        }

        // Handle more button click only when not in selection mode
        holder.moreChoose.setOnClickListener {
            if (!isSelectionMode) {
                showBottomSheetDialog(video)
            }
        }
    }

    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }
        val video = videoList[position]
        video.selected = !video.selected
        notifyItemChanged(position)
        updateSelectionMode()
        updateSelectionMode()
    }


    @SuppressLint("UseCompatLoadingForDrawables", "SetTextI18n")
    private fun showBottomSheetDialog(video: VideoData) {
        val bottomSheetDialog = BottomSheetDialog(context)
        val view = LayoutInflater.from(context).inflate(R.layout.playlist_video_bottom_sheet, null)
        bottomSheetDialog.setContentView(view)
        view.background = context.getDrawable(R.drawable.rounded_bottom_sheet_2)

        val videoTitle: TextView = view.findViewById(R.id.videoTitle)
        val videoThumbnail: ShapeableImageView = view.findViewById(R.id.videoThumbnail)
        val duration: TextView = view.findViewById(R.id.duration)

        duration.text = DateUtils.formatElapsedTime(video.duration / 1000)
        videoTitle.text = video.title
        Glide.with(context)
            .load(video.artUri)
            .apply(RequestOptions().placeholder(R.color.place_holder_video).centerCrop())
            .into(videoThumbnail)

        val playOptionLayout: LinearLayout = view.findViewById(R.id.playOptionLayout)
        val removeOptionLayout: LinearLayout = view.findViewById(R.id.removeOptionLayout)
        val propertiesOptionLayout: LinearLayout = view.findViewById(R.id.propertiesOptionLayout)

        playOptionLayout.setOnClickListener {
            val intent = Intent(context, PlayerFileActivity::class.java).apply {
                putExtra("videoUri", video.path)
                putExtra("videoTitle", video.title)
            }
            context.startActivity(intent)
            bottomSheetDialog.dismiss()
        }

        removeOptionLayout.setOnClickListener {
            val builder = android.app.AlertDialog.Builder(context)
            val dialogView = LayoutInflater.from(context).inflate(R.layout.playlist_remove, null)
            builder.setView(dialogView)

            val dialog = builder.create()

            val cancelButton: Button = dialogView.findViewById(R.id.cancel_button)
            val removeButton: Button = dialogView.findViewById(R.id.remove_button)

            cancelButton.setOnClickListener {
                dialog.dismiss()
            }

            removeButton.setOnClickListener {
                onVideoRemoved(video)
                dialog.dismiss()
                bottomSheetDialog.dismiss()
            }

            dialog.show()
            bottomSheetDialog.dismiss()
        }

        propertiesOptionLayout.setOnClickListener {
            val customDialogIF = LayoutInflater.from(context).inflate(R.layout.info_one_dialog, null)
            val positiveButton = customDialogIF.findViewById<Button>(R.id.positiveButton)
            val fileNameTextView = customDialogIF.findViewById<TextView>(R.id.fileName)
            val durationTextView = customDialogIF.findViewById<TextView>(R.id.DurationDetail)
            val sizeTextView = customDialogIF.findViewById<TextView>(R.id.sizeDetail)
            val locationTextView = customDialogIF.findViewById<TextView>(R.id.locationDetail)
            val dateTextView = customDialogIF.findViewById<TextView>(R.id.dateDetail)

            fileNameTextView.text = video.title
            durationTextView.text = DateUtils.formatElapsedTime(video.duration / 1000)
            val sizeInBytes = video.size.toLong()
            val sizeInMb = sizeInBytes / (1024 * 1024)
            val formattedSizeInMb = String.format("%.1fMB", sizeInMb.toDouble())
            val formattedSizeInBytes = String.format("(%,d bytes)", sizeInBytes)
            sizeTextView.text = "$formattedSizeInMb $formattedSizeInBytes"
            locationTextView.text = video.path

            val dateFormat = SimpleDateFormat("MMMM d, yyyy, HH:mm", Locale.getDefault())
            val formattedDate = video.dateAdded?.let { Date(it) }?.let { dateFormat.format(it) }
            dateTextView.text = formattedDate ?: "Unknown date"

            val dialogIF = MaterialAlertDialogBuilder(context)
                .setView(customDialogIF)
                .setCancelable(false)
                .create()
            positiveButton.setOnClickListener {
                dialogIF.dismiss()
            }
            dialogIF.show()
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    override fun getItemCount() = videoList.size
}