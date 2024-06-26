package com.jaidev.seeaplayer.browserActivity

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewStub
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.HistoryAdapter
import com.jaidev.seeaplayer.browseFregment.BrowseFragment
import com.jaidev.seeaplayer.dataClass.HistoryItem
import com.jaidev.seeaplayer.dataClass.HistoryManager
import com.jaidev.seeaplayer.databinding.ActivityHistoryBrowserBinding

class HistoryBrowser : AppCompatActivity() , HistoryAdapter.ItemClickListener  {
    private var isEditTextVisible = false
    private lateinit var binding: ActivityHistoryBrowserBinding
    private lateinit var fileListAdapter: HistoryAdapter
    private lateinit var emptyStateLayout: ViewStub // Add reference to emptyStateLayout
    private var appOpenAd : AppOpenAd? = null
    private var isAdDisplayed = false
    private lateinit var swipeRefreshLayout: ConstraintLayout
    companion object{
        val historyItems: MutableList<HistoryItem> = mutableListOf()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()
        MobileAds.initialize(this){}
        loadAppOpenAd()

        val historyList = HistoryManager.getHistoryList(this).toMutableList()
        fileListAdapter = HistoryAdapter(historyList ,this )
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        binding.recyclerFileView.setHasFixedSize(true)
        binding.recyclerFileView.setItemViewCacheSize(10)
        binding.recyclerFileView.layoutManager = LinearLayoutManager(this)
        binding.recyclerFileView.adapter = fileListAdapter
        // Show empty state layout based on history list size
        updateEmptyStateVisibility()
        initializeBinding()
        swipeRefreshLayout = binding.historyActivityLayout
        setSwipeRefreshBackgroundColor()
    }
    private fun setSwipeRefreshBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            swipeRefreshLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.dark_cool_blue)

        } else {
            // Light mode is enabled, set background color to white
            swipeRefreshLayout.setBackgroundColor(resources.getColor(android.R.color.white))
            window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
            window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        }
    }
    @SuppressLint("ClickableViewAccessibility")
    private fun initializeBinding(){

        binding.imageButtonSearch.setOnClickListener {
            // Show editTextSearch
            binding.editTextSearch.visibility = View.VISIBLE
            binding.imageButtonSearch.visibility = View.GONE
            binding.editTextSearch.text?.clear()

            // Set focus to editTextSearch
            binding.editTextSearch.requestFocus()

            // Show the keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.editTextSearch, InputMethodManager.SHOW_IMPLICIT)

            isEditTextVisible = true
        }



        binding.ClearData.setOnClickListener {
            showClearDataConfirmationDialog()
        }
        binding.editTextSearch.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                // Handle touch events on EditText
                handleEditTextTouch(v, event)
            }
            false
        }
        binding.editTextSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // Not used in this case
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Not used in this case
            }

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    fileListAdapter.filter(s.toString())
                    updateEmptyStateVisibility()
                }
            }
        })

    }


    private fun handleEditTextTouch(v: View, event: MotionEvent) {
        val bounds: Rect = binding.editTextSearch.compoundDrawablesRelative[2].bounds
        val drawableStart = binding.editTextSearch.compoundDrawablesRelative[0] // Index 0 for drawable start
        val bound = drawableStart?.bounds

        if (bound != null) {
            val touchArea = Rect(
                v.paddingLeft,
                v.paddingTop,
                v.paddingLeft + bounds.width(),
                v.height - v.paddingBottom
            )

            if (touchArea.contains(event.x.toInt(), event.y.toInt())) {
                // Hide editTextSearch
                hideEditText()
                fileListAdapter.filter("") // Passing empty string to show all files



                return
            }
        }

        val touchArea = Rect(
            v.width - bounds.width() - v.paddingEnd,
            v.paddingTop,
            v.width - v.paddingEnd,
            v.height - v.paddingBottom
        )

        if (touchArea.contains(event.x.toInt(), event.y.toInt())) {
            // Clear the text in the EditText
            binding.editTextSearch.text?.clear()
            fileListAdapter.filter("") // Passing empty string to show all files


        }
    }


    private fun hideEditText() {
        binding.editTextSearch.visibility = View.GONE
        binding.imageButtonSearch.visibility = View.VISIBLE
        // Hide keyboard
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.editTextSearch.windowToken, 0)
        isEditTextVisible = false


    }
    private fun updateEmptyStateVisibility() {
        if (fileListAdapter.itemCount == 0) {
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            emptyStateLayout.visibility = View.GONE
        }
    }
    override fun onItemClick(historyItem: HistoryItem) {
        // Handle item click here
        val query = historyItem.url
        openUrlInBrowser(query)
        showLoadingToast()
    }
    private fun openUrlInBrowser(query: String) {
        val browserFragment = BrowseFragment(urlNew = query)
        changeTab("Brave", browserFragment)
    }

    private fun showLoadingToast() {
        Toast.makeText(this, "Provided link is loading. You can go back.", Toast.LENGTH_LONG).show()
    }


    private fun showClearDataConfirmationDialog() {
        if (fileListAdapter.itemCount == 0) {
            // Show toast indicating no search history to delete
            Toast.makeText(this, "There is no any Search History to delete", Toast.LENGTH_SHORT).show()
        } else {
            // Create a custom dialog layout
            val dialogView = LayoutInflater.from(this).inflate(R.layout.custom_dialog_layout, null)

            // Initialize AlertDialogBuilder with custom layout
            val alertDialogBuilder = AlertDialog.Builder(this)
                .setView(dialogView)

            // Set the dialog titles
            val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)
            dialogTitle.text = getString(R.string.clear_data_dialog_title)

            val dialogMessage = dialogView.findViewById<TextView>(R.id.dialogMessage)
            dialogMessage.text = getString(R.string.clear_data_dialog_message)

            // Set positive button (Clear Data)
            alertDialogBuilder.setPositiveButton("Clear Data") { dialog, _ ->
                clearBrowsingData()
                dialog.dismiss()
            }

            // Set negative button (Cancel)
            alertDialogBuilder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

            // Create and show the AlertDialog
            val alertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }
    }




    @SuppressLint("NotifyDataSetChanged")
    private fun clearBrowsingData() {
        // Clear browsing history data
        HistoryManager.clearHistory(this)

        // Fetch updated history list
        val updatedHistoryList = HistoryManager.getHistoryList(this).toMutableList()

        // Log the size of the updated history list for debugging
        Log.d("HistoryBrowser", "Updated history list size: ${updatedHistoryList.size}")

        // Update historyItems with the updated list
        historyItems.clear()
        historyItems.addAll(updatedHistoryList)

        // Log the size of historyItems after update for debugging
        Log.d("HistoryBrowser", "Updated historyItems size: ${historyItems.size}")

        // Notify the adapter that the dataset has changed
        fileListAdapter.notifyDataSetChanged()

        // Show a toast message to indicate successful data clearance
        Toast.makeText(this, "Browsing data cleared successfully", Toast.LENGTH_SHORT).show()
    }

    private fun loadAppOpenAd() {
        if (!isAdDisplayed) {
            val adRequest = AdRequest.Builder().build()
            AppOpenAd.load(
                this,
                "ca-app-pub-3504589383575544/5888373387",
                adRequest,
                AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT,
                appOpenAdLoadCallback
            )
        }
    }

    private val appOpenAdLoadCallback = object : AppOpenAd.AppOpenAdLoadCallback() {
        override fun onAdLoaded(ad: AppOpenAd) {
            appOpenAd = ad
            appOpenAd!!.show(this@HistoryBrowser)
            isAdDisplayed = true // Mark ad as displayed
        }

        override fun onAdFailedToLoad(p0: LoadAdError) {
            // Handle failed ad loading
        }
    }

}