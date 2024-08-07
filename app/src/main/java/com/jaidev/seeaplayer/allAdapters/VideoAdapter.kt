package com.jaidev.seeaplayer.allAdapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.text.SpannableStringBuilder
import android.text.format.DateUtils
import android.text.format.Formatter
import android.view.ActionMode
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.jaidev.seeaplayer.FoldersActivity
import com.jaidev.seeaplayer.MainActivity
import com.jaidev.seeaplayer.PlayerActivity
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.browserActivity.PlayerFileActivity
import com.jaidev.seeaplayer.dataClass.VideoData
import com.jaidev.seeaplayer.databinding.GridVideoViewBinding
import com.jaidev.seeaplayer.databinding.VideoMoreFeaturesBinding
import java.io.File
import java.text.NumberFormat

@SuppressLint("NotifyDataSetChanged")
class VideoAdapter(private val context: Context,
                   private var videoList: ArrayList<VideoData>,
                   private val isFolder: Boolean = false,
                   private val fileCountChangeListener: OnFileCountChangeListener,

                   )
    : RecyclerView.Adapter<VideoAdapter.MyHolder>() {
    interface OnFileCountChangeListener {
        fun onFileCountChanged(newCount: Int)
    }

    fun getSelectedItemCount(): Int {
        return selectedItems.size
    }
    private var newPosition = 0
    private lateinit var dialogRF: AlertDialog
    private var sharedPreferences: SharedPreferences
    val selectedItems = HashSet<Int>()
    var actionMode: ActionMode? = null
    private var isGridMode = false
    private var isSelectionModeEnabled = false
    private var isAllSelected = false // Add this flag

    private lateinit var bottomToolbar: View
    private lateinit var deleteBtn: ImageView
    private lateinit var checkBtn: ImageView
    private lateinit var labelBtn: ImageView
    private lateinit var shareBtn: ImageView
    private lateinit var renameBtn: ImageView

    init {
        adapterInstance = this

        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadVideoTitles()
        loadVideoList() // Initialize videoList with saved isNew status

    }
    companion object {
        const val PREF_NAME = "video_titles"

        @SuppressLint("StaticFieldLeak")
        private var adapterInstance: VideoAdapter? = null

        fun updateNewIndicator(videoId: String) {
            adapterInstance?.let { adapter ->
                adapter.videoList.find { it.id == videoId }?.let { video ->
                    video.isNew = false
                    adapter.saveIsNewStatus(video.id, false )
                    adapter.markVideoAsPlayed(video.id)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }




    init {
        if (context is AppCompatActivity) {
            bottomToolbar = context.findViewById(R.id.bottomToolbar)
        deleteBtn = bottomToolbar.findViewById(R.id.deleteBtn)
            shareBtn = bottomToolbar.findViewById(R.id.shareBtn)
            renameBtn = bottomToolbar.findViewById(R.id.renameBtn)
            checkBtn = bottomToolbar.findViewById(R.id.checkBtn)
            labelBtn = bottomToolbar.findViewById(R.id.labelBtn)

            // Set click listeners for bottom toolbar actions
            deleteBtn.setOnClickListener {

                if (selectedItems.isNotEmpty()) {
                        // Build confirmation dialog
                        AlertDialog.Builder(context)
                            .setTitle("Confirm Delete")
                            .setMessage("Are you sure you want to delete these ${selectedItems.size} selected videos?")
                            .setPositiveButton("Delete") { _, _ ->
                                // User clicked Delete, proceed with deletion
                                val positionsToDelete = ArrayList(selectedItems)
                                positionsToDelete.sortDescending()

                                for (position in positionsToDelete) {
                                    val video = videoList[position]
                                    val file = File(video.path)

                                    if (file.exists() && file.delete()) {
                                        MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                                        videoList.removeAt(position)
                                    }
                                }
                                selectedItems.clear()
                                notifyDataSetChanged()
                                updateActionModeTitle()
                                fileCountChangeListener.onFileCountChanged(videoList.size)
                            }
                            .setNegativeButton("Cancel") { dialog, _ ->
                                // User clicked Cancel, dismiss dialog
                                dialog.dismiss()
                            }
                            .show()

                }
            }
            shareBtn.setOnClickListener {
                shareSelectedFiles()
            }
            renameBtn.setOnClickListener {
                // Call the showRenameDialog method here
                if (selectedItems.size == 1) {
                    val selectedPosition = selectedItems.first()
                    val defaultName = videoList[selectedPosition].title
                    showRenameDialog(selectedPosition, defaultName)
                } else {
                    updateRenameButtonState()
                }

            }

            checkBtn.setOnClickListener {
                toggleSelectAllItems()

            }
            labelBtn.setOnClickListener {
                showLabelDialog()
            }
        }

    }
    private fun showLabelDialog() {
        val builder = AlertDialog.Builder(context)
        val inflater = LayoutInflater.from(context)
        val dialogView = inflater.inflate(R.layout.label_acton_mode_dialog, null)
        builder.setView(dialogView)

        val makeNew = dialogView.findViewById<TextView>(R.id.makeNew)
        val makeNone = dialogView.findViewById<TextView>(R.id.makeNone)
        val makeFinished = dialogView.findViewById<TextView>(R.id.makeFinished)

        val dialog = builder.create()
        dialog.show()

        makeNew.setOnClickListener {
            updateIndicatorVisibility(true)
            dialog.dismiss()
        }

        makeNone.setOnClickListener {
            updateIndicatorVisibility(false)
            dialog.dismiss()
        }
        makeFinished.setOnClickListener {
            updateIndicatorVisibility(false)
            dialog.dismiss()
        }
    }

    private fun updateIndicatorVisibility(show: Boolean) {
        for (position in selectedItems) {
            val video = videoList[position]
            video.isNew = show

            // Persist the 'isNew' status
            saveIsNewStatus(video.id, show)

            // If 'show' is false, mark as played if necessary
            if (!show) {
                markVideoAsPlayed(video.id)
            }
        }
        notifyDataSetChanged()
    }
    private fun loadVideoList() {
        // Load your video list data
        videoList.forEach { video ->
            video.isNew = loadIsNewStatus(video.id)
        }
    }


    private fun toggleSelectAllItems() {
        isAllSelected = if (isAllSelected) {
            // Unselect all items
            selectedItems.clear()
            false
        } else {
            // Select all items
            for (i in 0 until videoList.size) {
                selectedItems.add(i)
            }
            true
        }
            notifyDataSetChanged()
            updateActionModeTitle()


    }


    interface VideoDeleteListener {
        fun onVideoDeleted()
    }

    private var videoDeleteListener: VideoDeleteListener? = null

    @SuppressLint("NotifyDataSetChanged")
    fun enableGridMode(enable: Boolean) {
        isGridMode = enable
        notifyDataSetChanged()
    }

    fun isSelectionModeEnabled(): Boolean {

        return isSelectionModeEnabled
    }
    class MyHolder(binding: GridVideoViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val title = binding.videoName
        val duration = binding.duration
        val image = binding.videoImage
        val more = binding.MoreChoose
val indicator = binding.newIndicator

        val root = binding.root
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
      return MyHolder(GridVideoViewBinding.inflate(LayoutInflater.from(context), parent, false))

}

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: MyHolder, @SuppressLint("RecyclerView") position: Int) {
        val video = videoList[position]

        holder.title.text = videoList[position].title
        holder.duration.text = DateUtils.formatElapsedTime(videoList[position].duration / 1000)
        Glide.with(context)
            .load(videoList[position].artUri)
            .apply(RequestOptions().placeholder(R.color.place_holder_video).centerCrop())
            .into(holder.image)

        setIconTint(holder.more)

        // Show the indicator if the video is new
        holder.indicator.visibility = if (video.isNew) View.VISIBLE else View.GONE


        // Determine if the item is currently selected
        if (selectedItems.contains(position)) {
            // Set your custom selected background on the root view of the item
            holder.root.setBackgroundResource(R.drawable.browser_selected_background)
        } else {
            holder.root.setBackgroundResource(R.drawable.browser_selected_white_background)
        }

        // Hide or show the more button based on selection mode
      if (isSelectionModeEnabled) {
          holder.more.visibility =  View.GONE
        } else {
          holder.more.visibility = View.VISIBLE
        }


        holder.root.setOnLongClickListener {
            toggleSelection(position)
            startActionMode()
            true
        }
        holder.root.setOnClickListener {
            if (actionMode != null) {
                toggleSelection(position)
            } else {
                when {
                    isFolder -> {
                        PlayerActivity.pipStatus = 1
                        sendIntent(pos = position, ref = "FoldersActivity")
                        markVideoAsPlayed(video.id)
                        notifyDataSetChanged()
                    }

                    videoList[position].id == PlayerActivity.nowPlayingId -> {
                        sendIntent(pos = position, ref = "NowPlaying")
                    }

                }

            }
        }

        holder.more.setOnClickListener {
            newPosition = position

            // Inflate the custom dialog layout
            val customDialog = LayoutInflater.from(context).inflate(R.layout.video_more_features, holder.root, false)
            val bindingMf = VideoMoreFeaturesBinding.bind(customDialog)
            // Create the dialog
            val dialogBuilder = MaterialAlertDialogBuilder(context)
                .setView(customDialog)
            val dialog = dialogBuilder.create()

            // Show the dialog
            dialog.show()

            // Get the window attributes of the dialog
            val window = dialog.window
            window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT) // Set dialog width and height
            window?.setGravity(Gravity.BOTTOM) // Set dialog gravity to bottom

            // Set click listeners for dialog buttons
            bindingMf.shareBtn.setOnClickListener {
                dialog.dismiss()

                // Create an ACTION_SEND intent to share the video
                val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
                shareIntent.type = "video/*"
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(videoList[position].path))
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                // Get the list of apps that can handle the intent
                val packageManager = context.packageManager
                val resolvedActivityList = packageManager.queryIntentActivities(shareIntent, 0)
                val excludedComponents = mutableListOf<ComponentName>()


                // Iterate through the list and exclude your app
                for (resolvedActivity in resolvedActivityList) {
                    if (resolvedActivity.activityInfo.packageName == context.packageName) {
                        excludedComponents.add(ComponentName(resolvedActivity.activityInfo.packageName, resolvedActivity.activityInfo.name))
                    }
                }

                // Create a chooser intent
                val chooserIntent = Intent.createChooser(shareIntent, "Sharing Video")

                // Exclude your app from the chooser intent
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excludedComponents.toTypedArray())
                }

                chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(context, chooserIntent, null)
            }


            bindingMf.infoBtn.setOnClickListener {

               dialog.dismiss()
                val customDialogIF = LayoutInflater.from(context).inflate(R.layout.info_one_dialog, null)
                val positiveButton = customDialogIF.findViewById<Button>(R.id.positiveButton)
                val fileNameTextView = customDialogIF.findViewById<TextView>(R.id.fileName)
                val durationTextView = customDialogIF.findViewById<TextView>(R.id.DurationDetail)
                val sizeTextView = customDialogIF.findViewById<TextView>(R.id.sizeDetail)
                val locationTextView = customDialogIF.findViewById<TextView>(R.id.locationDetail)

                // Populate dialog views with data
                fileNameTextView.text = videoList[position].title
                durationTextView.text = DateUtils.formatElapsedTime(videoList[position].duration / 1000)
                sizeTextView.text = Formatter.formatShortFileSize(context, videoList[position].size.toLong())
                locationTextView.text = videoList[position].path

                val dialogIF = MaterialAlertDialogBuilder(context)
                    .setView(customDialogIF)
                    .setCancelable(false)
                    .create()
                positiveButton.setOnClickListener {
                    dialogIF.dismiss()
                }
                dialogIF.show()
            }

            bindingMf.renameBtn.setOnClickListener {
                dialog.dismiss()
                showRenameDialog(position, videoList[position].title)
            }


            bindingMf.deleteBtn.setOnClickListener {
                dialog.dismiss()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                    showPermissionRequestDialog()
                } else {
                    showDeleteDialog(position)
                }
            }
        }

    }



    @RequiresApi(Build.VERSION_CODES.R)
    private fun showPermissionRequestDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.video_music_delete_permission_dialog, null)
        val alertDialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<Button>(R.id.buttonOpenSettings).setOnClickListener {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            intent.data = Uri.parse("package:${context.packageName}")
            ContextCompat.startActivity(context, intent, null)
            alertDialog.dismiss()
        }

        dialogView.findViewById<Button>(R.id.buttonNotNow).setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showDeleteDialog(position: Int) {
        val alertDialogBuilder = AlertDialog.Builder(context)
        val view = LayoutInflater.from(context).inflate(R.layout.delete_alertdialog, null)

        val videoNameDelete = view.findViewById<TextView>(R.id.videmusicNameDelete)
        val deleteText = view.findViewById<TextView>(R.id.deleteText)
        val cancelText = view.findViewById<TextView>(R.id.cancelText)
        val iconImageView = view.findViewById<ImageView>(R.id.videoImage)

        // Set the delete text color to red
        deleteText.setTextColor(ContextCompat.getColor(context, R.color.red))

        // Set the cancel text color to black
        cancelText.setTextColor(ContextCompat.getColor(context, R.color.black))

        // Load video image into iconImageView using Glide
        Glide.with(context)
            .asBitmap()
            .load(videoList[position].artUri)
            .apply(RequestOptions().placeholder(R.color.place_holder_video).centerCrop())
            .into(iconImageView)

        videoNameDelete.text = videoList[position].title

        alertDialogBuilder.setView(view)

        val alertDialog = alertDialogBuilder.create()

        deleteText.setOnClickListener {
            val file = File(videoList[position].path)
            if (file.exists() && file.delete()) {
                MediaScannerConnection.scanFile(context, arrayOf(file.path), null, null)
                when {
                    MainActivity.search -> {
                        MainActivity.dataChanged = true
                        videoList.removeAt(position)
                        notifyDataSetChanged()
                        videoDeleteListener?.onVideoDeleted()

                    }
                    isFolder -> {
                        MainActivity.dataChanged = true
                        FoldersActivity.currentFolderVideos.removeAt(position)
                        notifyDataSetChanged()
                        videoDeleteListener?.onVideoDeleted()
                    }
                }
                // Notify listener of the file count change
                fileCountChangeListener.onFileCountChanged(videoList.size)
            } else {
                Toast.makeText(context, "Permission Denied!!", Toast.LENGTH_SHORT).show()
            }
            alertDialog.dismiss()
        }

        cancelText.setOnClickListener {
            // Handle cancel action here
            alertDialog.dismiss()
        }
        alertDialog.show()
    }
    override fun getItemCount(): Int {
        return videoList.size
    }

    private fun showRenameDialog(position: Int, defaultName: String) {
        val dialogBuilder = AlertDialog.Builder(context)

        // Set up the layout for the dialog
        val view = LayoutInflater.from(context).inflate(R.layout.rename_field, null)
        val editText = view.findViewById<EditText>(R.id.renameField)
        editText.setText(defaultName) // Set default text as current music title


        dialogBuilder.setView(view)
            .setTitle("Rename Video")
            .setMessage("Enter new name for the video:")
            .setCancelable(false)
            .setPositiveButton("Rename") { _, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != defaultName) {
                    renameVideo(position, newName)
                    dialogRF.dismiss()
                } else {
                    Toast.makeText(context, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        dialogRF = dialogBuilder.create()
        dialogRF.show()
        // Set the positive and negative button colors to cool_blue
        dialogRF.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.cool_blue))
        dialogRF.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, R.color.cool_blue))
    }
    private fun renameVideo(position: Int, newName: String) {
        val music = videoList[position]
        music.title = newName
        notifyItemChanged(position)
        saveVideoTitles(music.id, newName)
        val defaultTitle = music.title
        showRenameDialog(position, defaultTitle)
    }

    // Save updated video titles to SharedPreferences
    private fun saveVideoTitles(uniqueIdentifier: String, newName: String) {
        val editor = sharedPreferences.edit()
        editor.putString(uniqueIdentifier, newName)
        editor.apply()
    }

    // Load saved video titles from SharedPreferences
    private fun loadVideoTitles() {
        for (video in videoList) {
            val savedTitle = sharedPreferences.getString(video.id, null)
            savedTitle?.let {
                video.title = it
            }
        }
    }

    private fun updateRenameButtonState() {
        if (selectedItems.size != 1) {
            renameBtn.isEnabled = false
            renameBtn.setColorFilter(ContextCompat.getColor(context, R.color.gray), PorterDuff.Mode.SRC_IN)
        } else {
            renameBtn.isEnabled = true
            renameBtn.clearColorFilter()
        }
    }

    private fun toggleSelection(position: Int) {
        if (selectedItems.contains(position)) {
            selectedItems.remove(position)
        } else {
            selectedItems.add(position)
        }

        if (selectedItems.isEmpty()) {
            actionMode?.finish()

        } else {
            startActionMode()

        }

        notifyItemChanged(position) // Update selected state for the item
        updateActionModeTitle()
        updateRenameButtonState()
        actionMode?.invalidate()

    }


    // Start action mode for multi-select
    @SuppressLint("NotifyDataSetChanged")
    private fun startActionMode() {
        if (actionMode == null) {
            actionMode = (context as AppCompatActivity).startActionMode(actionModeCallback)
            isSelectionModeEnabled = true // Enable selection mode
            notifyDataSetChanged() // Update all item views to hide the "more" button
              bottomToolbar.visibility = View.VISIBLE // Show bottom toolbar

        }
        updateActionModeTitle()

    }
    // Update action mode title with selected and total item counts
    private fun updateActionModeTitle() {
        actionMode?.title = "${selectedItems.size} / ${videoList.size} Selected"
    }
    // Action mode callback
    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
            // Inflate action mode menu
            mode?.menuInflater?.inflate(R.menu.multiple_player_select_menu_two, menu)
            isSelectionModeEnabled = true // Enable selection mode

            return true
        }

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {


            return true
        }


        @SuppressLint("NotifyDataSetChanged", "ResourceType", "SetTextI18n")
        override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
            when (item?.itemId) {
                R.id.playMulti -> {
                    val selectedUris = selectedItems.map { videoList[it].artUri }
                    val intent = Intent(context, PlayerFileActivity::class.java).apply {
                        action = Intent.ACTION_SEND_MULTIPLE
                        putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(selectedUris))
                    }
                    context.startActivity(intent)
                    return true
                }

                R.id.infoMulti -> {
                    if (selectedItems.size > 1) {
                        // More than one item is selected
                        val totalSize = selectedItems.sumOf { videoList[it].size.toLong() }
                        val totalSizeFormatted = Formatter.formatShortFileSize(context, totalSize)
                        val totalSizeBytesFormatted = NumberFormat.getInstance().format(totalSize)

                        // Calculate the total duration
                        val totalDuration = selectedItems.sumOf { videoList[it].duration }
                        val totalDurationFormatted = formatDurationWithApproximation(totalDuration)

                        val customDialogView = LayoutInflater.from(context).inflate(R.layout.info_dialog, null, false)
                        val containsDetailView = customDialogView.findViewById<TextView>(R.id.containsDetail)
                        val durationDetailView = customDialogView.findViewById<TextView>(R.id.durationDetail)
                        val totalSizeDetailView = customDialogView.findViewById<TextView>(R.id.totalSizeDetail)
                        val positiveButton = customDialogView.findViewById<Button>(R.id.positiveButton)

                        containsDetailView.text = "${selectedItems.size} videos"
                        durationDetailView.text = totalDurationFormatted
                        totalSizeDetailView.text = "$totalSizeFormatted ($totalSizeBytesFormatted bytes)"

                        val dialog = MaterialAlertDialogBuilder(context)
                            .setView(customDialogView)
                            .create()

                        positiveButton.setOnClickListener {
                            dialog.dismiss()
                        }

                        dialog.show()
                    }
                    else if (selectedItems.size == 1) {
                        // Only one item is selected
                        val selectedInfo = SpannableStringBuilder()
                        val selectedPosition = selectedItems.first()
                        val video = videoList[selectedPosition]

                        val customDialogView = LayoutInflater.from(context).inflate(R.layout.info_one_dialog, null, false)
                        val titleView = customDialogView.findViewById<TextView>(R.id.titleText)
                        val fileNameView = customDialogView.findViewById<TextView>(R.id.fileName)
                        val durationDetailView = customDialogView.findViewById<TextView>(R.id.DurationDetail)
                        val sizeDetailView = customDialogView.findViewById<TextView>(R.id.sizeDetail)
                        val locationDetailView = customDialogView.findViewById<TextView>(R.id.locationDetail)
                        val positiveButton = customDialogView.findViewById<Button>(R.id.positiveButton)

                        titleView.text = "Properties"
                        fileNameView.text = video.title
                        durationDetailView.text = DateUtils.formatElapsedTime(video.duration / 1000)
                        sizeDetailView.text = Formatter.formatShortFileSize(context, video.size.toLong())
                        locationDetailView.text = video.path


                        val dialog = MaterialAlertDialogBuilder(context)
                            .setView(customDialogView)
                            .setCancelable(false)
                            .create()

                        positiveButton.setOnClickListener{
                            dialog.dismiss()
                        }

                        dialog.show()
                    }

                }

            }

            return false
        }


        @SuppressLint("NotifyDataSetChanged")
        override fun onDestroyActionMode(mode: ActionMode?) {
            // Clear selection and action mode
            selectedItems.clear()
            actionMode = null
            isSelectionModeEnabled = false // Disable selection mode
            notifyDataSetChanged()
            bottomToolbar.visibility = View.GONE // Hide bottom toolbar

        }
    }

    fun formatDurationWithApproximation(duration: Long): String {
        val seconds = duration / 1000
        val minutes = seconds / 60
        val hours = minutes / 60

        return when {
            hours > 0 -> String.format("%02d:%02d:%02d (%d hours approx)", hours, minutes % 60, seconds % 60, hours)
            minutes > 0 -> String.format("%02d:%02d (%d minutes approx)", minutes, seconds % 60, minutes)
            else -> String.format("%02d (%d seconds)", seconds, seconds)
        }
    }
    private fun shareSelectedFiles() {
        val uris = mutableListOf<Uri>()
        for (position in selectedItems) {
            val music = videoList[position]
            val file = File(music.path)
            val fileUri = FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                file
            )
            uris.add(fileUri)
        }

        val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE)
        shareIntent.type = "video/*"
        shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val packageManager = context.packageManager
        val resolvedActivityList = packageManager.queryIntentActivities(shareIntent, 0)
        val excludedComponents = mutableListOf<ComponentName>()

        for (resolvedActivity in resolvedActivityList) {
            if (resolvedActivity.activityInfo.packageName == context.packageName) {
                excludedComponents.add(
                    ComponentName(
                        resolvedActivity.activityInfo.packageName,
                        resolvedActivity.activityInfo.name
                    )
                )
            }
        }

        val chooserIntent = Intent.createChooser(shareIntent, "Share Files")
        chooserIntent.putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, excludedComponents.toTypedArray())
        chooserIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(chooserIntent)

    }





    private fun sendIntent(pos: Int, ref: String) {
        PlayerActivity.position = pos
        val intent = Intent(context, PlayerActivity::class.java)
        intent.putExtra("class", ref)

        startActivity(context, intent, null)

    }


    private fun markVideoAsPlayed(videoId: String) {
        // Set the isNew flag to false for the played video
        videoList.find { it.id == videoId }?.let { video ->
            video.isNew = false
            saveIsNewStatus(video.id, false)
        }

        // Save the played status
        val sharedPreferences = context.getSharedPreferences("played_videos", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean(videoId, true)
            apply()
        }

        notifyDataSetChanged() // Update the adapter to reflect the changes
    }




    private fun setIconTint(imageView: ImageView) {
        // Get the drawable

     val drawable: Drawable? = ContextCompat.getDrawable(context, R.drawable.icon_dark)

        // Get the theme color for the icon tint
        val iconTint = if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES) {
            ContextCompat.getColor(context, R.color.white)
        } else {
            ContextCompat.getColor(context, R.color.black)
        }

        // Set the tint for the drawable
        drawable?.let {
            DrawableCompat.setTint(it, iconTint)
        }

        // Set the modified drawable to your ImageView or wherever you're using it
        imageView.setImageDrawable(drawable)
    }
    private fun saveIsNewStatus(videoId: String, isNew: Boolean) {
        val sharedPreferences = context.getSharedPreferences("new_videos", Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean(videoId, isNew)
            apply()
        }
    }
    private fun loadIsNewStatus(videoId: String): Boolean {
        val sharedPreferences = context.getSharedPreferences("new_videos", Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(videoId, false)
    }

}

