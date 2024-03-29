package com.jaidev.seeaplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.jaidev.seeaplayer.dataClass.Folder
import com.jaidev.seeaplayer.databinding.FoldersViewBinding


class FoldersAdapter(private val context: Context, private var foldersList: ArrayList<Folder>) : RecyclerView.Adapter<FoldersAdapter.MyHOlder>() {
    class MyHOlder(binding: FoldersViewBinding) : RecyclerView.ViewHolder(binding.root) {
        val folderName = binding.folderNameFV
        val root = binding.root
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHOlder {
        return  MyHOlder(FoldersViewBinding.inflate(LayoutInflater.from(context),parent,false))
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onBindViewHolder(holder: MyHOlder, position: Int) {
        holder.folderName.text = foldersList[position].folderName

        holder.root.setOnClickListener {
            val intent = Intent(context, FoldersActivity::class.java)
            intent.putExtra("position", position)
            ContextCompat.startActivity(context, intent, null)



        }


    }


    override fun getItemCount(): Int {
        return foldersList.size
    }

}

