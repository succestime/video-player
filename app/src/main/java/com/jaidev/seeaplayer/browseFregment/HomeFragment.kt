package com.jaidev.seeaplayer.browseFregment

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.internal.ViewUtils.hideKeyboard
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.allAdapters.BookmarkAdapter
import com.jaidev.seeaplayer.allAdapters.SavedTitlesAdapter
import com.jaidev.seeaplayer.browserActivity.BookmarkActivity
import com.jaidev.seeaplayer.browserActivity.LinkTubeActivity
import com.jaidev.seeaplayer.browserActivity.changeTab
import com.jaidev.seeaplayer.browserActivity.checkForInternet
import com.jaidev.seeaplayer.dataClass.Bookmark
import com.jaidev.seeaplayer.dataClass.SearchTitle
import com.jaidev.seeaplayer.dataClass.SearchTitleStore
import com.jaidev.seeaplayer.dataClass.SharedPreferencesBookmarkSaver
import com.jaidev.seeaplayer.databinding.FragmentHomeBinding

class HomeFragment : Fragment(), BookmarkAdapter.BookmarkAdapterCallback{

    private lateinit var binding: FragmentHomeBinding
    private var isBtnTextUrlFocused = false // Flag to track if btnTextUrl has been focused
    companion object {
        lateinit var bookmarkList: ArrayList<Bookmark>
    }
    private lateinit var bookmarkSaver: SharedPreferencesBookmarkSaver
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Create a ContextThemeWrapper with the desired theme
        val contextThemeWrapper = ContextThemeWrapper(activity, R.style.TransparentTheme)

        // Inflate the layout using the themed context
        val themedInflater = inflater.cloneInContext(contextThemeWrapper)
        val view = themedInflater.inflate(R.layout.fragment_home, container, false)
        binding = FragmentHomeBinding.bind(view)
        saveDefaultBookmarks()
        addDefaultBookmarks()

        // Check if the keyboard is visible
        val rootView = view?.rootView
        rootView?.viewTreeObserver?.addOnGlobalLayoutListener {
            val rect = Rect()
            rootView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootView.height
            val keypadHeight = screenHeight - rect.bottom
            val isKeyboardVisible = keypadHeight > screenHeight * 0.15

            // Update the visibility of RecyclerView based on keyboard visibility
            binding.historyRecycler.visibility = if (isKeyboardVisible) View.VISIBLE else View.GONE
        }
        return view
    }

    private fun saveDefaultBookmarks() {
        val bookmarkTitles = listOf("Wikipedia - Default", "Google - Default", "YouTube - Default")
        val bookmarkUrls = listOf("https://www.wikipedia.org", "https://www.google.com", "https://m.youtube.com")

        val sharedPreferences = requireContext().getSharedPreferences("DefaultBookmarks", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        for (i in bookmarkTitles.indices) {
            val title = bookmarkTitles[i]
            val url = bookmarkUrls[i]

            editor.putString(title, url)
        }
        editor.apply()
    }

    private fun addDefaultBookmarks() {
        val sharedPreferences = requireContext().getSharedPreferences("DefaultBookmarks", Context.MODE_PRIVATE)

        val defaultBookmarkTitles = listOf("Wikipedia - Default", "Google - Default", "YouTube - Default")

        for (title in defaultBookmarkTitles) {
            val url = sharedPreferences.getString(title, "")
            if (!url.isNullOrEmpty()) {
                val bookmarkExists = LinkTubeActivity.bookmarkList.any { it.name == title && it.url == url }
                if (!bookmarkExists) {
                    LinkTubeActivity.bookmarkList.add(Bookmark(title, url))
                }
            }
        }
    }

    @SuppressLint("RestrictedApi", "NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()

        val linkTubeRef = requireActivity() as LinkTubeActivity

        LinkTubeActivity.tabsBtn.text = LinkTubeActivity.tabsList.size.toString()
        LinkTubeActivity.tabs2Btn.text = LinkTubeActivity.tabsList.size.toString()
        LinkTubeActivity.tabsList[LinkTubeActivity.myPager.currentItem].name = "Home"
        linkTubeRef.binding.btnTextUrl.setText("")
        linkTubeRef.binding.webIcon.setImageResource(R.drawable.search_licktube_icon)
        linkTubeRef.binding.googleMicBtn.visibility = View.VISIBLE
        linkTubeRef.binding.bookMarkBtn.visibility = View.VISIBLE
        linkTubeRef.binding.crossBtn.visibility = View.GONE

        // TextWatcher for btnTextUrl
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    linkTubeRef.binding.googleMicBtn.visibility = View.VISIBLE
                    linkTubeRef.binding.bookMarkBtn.visibility = View.VISIBLE
                    linkTubeRef.binding.crossBtn.visibility = View.GONE
                } else {
                    linkTubeRef.binding.googleMicBtn.visibility = View.GONE
                    linkTubeRef.binding.bookMarkBtn.visibility = View.VISIBLE
                    linkTubeRef.binding.crossBtn.visibility = View.VISIBLE
                }

                // Filter the titles based on the entered text
                val adapter = binding.historyRecycler.adapter as? SavedTitlesAdapter
                adapter?.filter(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        }
        linkTubeRef.binding.btnTextUrl.addTextChangedListener(textWatcher)

        linkTubeRef.binding.crossBtn.setOnClickListener {
            linkTubeRef.binding.btnTextUrl.text.clear()
        }

        linkTubeRef.binding.btnTextUrl.setOnClickListener {
            // Check if btnTextUrl has text, if so, select all text
            if (linkTubeRef.binding.btnTextUrl.text.isNotEmpty()) {
                linkTubeRef.binding.btnTextUrl.selectAll()
            }

            // Show the keyboard explicitly
            val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(linkTubeRef.binding.btnTextUrl, InputMethodManager.SHOW_IMPLICIT)

            // Check if the keyboard is visible
            val rootView = view?.rootView
            rootView?.viewTreeObserver?.addOnGlobalLayoutListener {
                val rect = Rect()
                rootView.getWindowVisibleDisplayFrame(rect)
                val screenHeight = rootView.height
                val keypadHeight = screenHeight - rect.bottom
                val isKeyboardVisible = keypadHeight > screenHeight * 0.15

                // Update the visibility of RecyclerView based on keyboard visibility
                binding.historyRecycler.visibility = if (isKeyboardVisible) View.VISIBLE else View.GONE
            }
        }

        linkTubeRef.binding.searchBrowser.setOnClickListener {
            // Request focus on btnTextUrl
            linkTubeRef.binding.btnTextUrl.requestFocus()

            // Check if btnTextUrl has text, if so, select all text
            if (linkTubeRef.binding.btnTextUrl.text.isNotEmpty()) {
                linkTubeRef.binding.btnTextUrl.selectAll()
            }

            // Show the keyboard explicitly
            val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(linkTubeRef.binding.btnTextUrl, InputMethodManager.SHOW_IMPLICIT)

            // Check if the keyboard is visible
            val rootView = view?.rootView
            rootView?.viewTreeObserver?.addOnGlobalLayoutListener {
                val rect = Rect()
                rootView.getWindowVisibleDisplayFrame(rect)
                val screenHeight = rootView.height
                val keypadHeight = screenHeight - rect.bottom
                val isKeyboardVisible = keypadHeight > screenHeight * 0.15

                // Update the visibility of RecyclerView based on keyboard visibility
                binding.historyRecycler.visibility = if (isKeyboardVisible) View.VISIBLE else View.GONE
            }
        }

        // Set click listener for homeTextUrl
        binding.homeTextUrl.setOnClickListener {
            // Request focus on btnTextUrl only if it's not already focused
            if (!isBtnTextUrlFocused) {
                linkTubeRef.binding.btnTextUrl.requestFocus()
                isBtnTextUrlFocused = true // Update the flag
            }

            // Show the keyboard explicitly
            val inputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            inputMethodManager.showSoftInput(linkTubeRef.binding.btnTextUrl, InputMethodManager.SHOW_IMPLICIT)

            // Check if the keyboard is visible
            val rootView = view?.rootView
            rootView?.viewTreeObserver?.addOnGlobalLayoutListener {
                val rect = Rect()
                rootView.getWindowVisibleDisplayFrame(rect)
                val screenHeight = rootView.height
                val keypadHeight = screenHeight - rect.bottom
                val isKeyboardVisible = keypadHeight > screenHeight * 0.15

                // Update the visibility of RecyclerView based on keyboard visibility
                binding.historyRecycler.visibility = if (isKeyboardVisible) View.VISIBLE else View.GONE
            }
        }

        binding.historyRecycler.setHasFixedSize(true)
        binding.historyRecycler.setItemViewCacheSize(5)
        binding.historyRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.historyRecycler.adapter = SavedTitlesAdapter(requireContext())


        // Set editor action listener for btnTextUrl (IME_ACTION_DONE or IME_ACTION_GO)
        linkTubeRef.binding.btnTextUrl.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE || actionId == EditorInfo.IME_ACTION_GO) {
                val query = linkTubeRef.binding.btnTextUrl.text.toString()
                if (checkForInternet(requireContext())) {
                    // Hide keyboard and RecyclerView
                    hideKeyboard(linkTubeRef.binding.btnTextUrl)
                    binding.historyRecycler.visibility = View.GONE
                    // Perform search
                    performSearch(query)
                } else {
                    Toast.makeText(requireContext(), "No Internet Connection \uD83C\uDF10", Toast.LENGTH_SHORT).show()
                }
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        binding.voiceSearchButton.setOnClickListener {
            linkTubeRef.speak()
        }

        binding.recyclerView.setHasFixedSize(true)
        binding.recyclerView.setItemViewCacheSize(5)

        // Set layout manager to scroll horizontally
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        binding.recyclerView.adapter = BookmarkAdapter(requireContext() , isActivity = false  ,callback = this ,requireContext() as BookmarkAdapter.BookmarkSaver)
        bookmarkSaver = SharedPreferencesBookmarkSaver(requireContext())
        BookmarkActivity.bookmarkList = bookmarkSaver.loadBookmarks()
        if (LinkTubeActivity.bookmarkList.size < 1) {
            binding.viewAllBtn.visibility = View.GONE
        }

        binding.viewAllBtn.setOnClickListener {
            startActivity(Intent(requireContext(), BookmarkActivity::class.java))
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun performSearch(query: String) {
        if (checkForInternet(requireContext())) {
            // Check if the title is already saved
            val isTitleSaved = SearchTitleStore.getTitles(requireContext()).any { it.title == query }

            if (!isTitleSaved) {
                // Create a SearchTitle object with the query and save it
                val searchTitle = SearchTitle(query)
                SearchTitleStore.addTitle(requireContext(), searchTitle)
                // Add new item to the adapter
                val adapter = binding.historyRecycler.adapter as? SavedTitlesAdapter
                adapter?.addItem(query)
            }

            // Change tab and perform search
            changeTab(query, BrowseFragment(query))
        } else {
            Toast.makeText(requireContext(), "No Internet Connection \uD83C\uDF10", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onListEmpty(isEmpty: Boolean) {
        TODO("Not yet implemented")
    }




}
