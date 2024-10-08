package com.jaidev.seeaplayer.dataClass.VideoPlaylistData

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jaidev.seeaplayer.VideoPlaylistFunctionality.PlaylistVideoActivity

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: PlaylistVideoActivity.SortType = PlaylistVideoActivity.SortType.TITLE_ASC // default value


)
