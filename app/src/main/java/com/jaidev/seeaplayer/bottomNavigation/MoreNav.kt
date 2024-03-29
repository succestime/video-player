package com.jaidev.seeaplayer.bottomNavigation

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.jaidev.seeaplayer.R
import com.jaidev.seeaplayer.databinding.FragmentMoreNavBinding
import com.jaidev.seeaplayer.signin

class moreNav : Fragment() {
private lateinit var binding : FragmentMoreNavBinding
    private lateinit var relativeLayout: RelativeLayout
    private var checkedItem: Int = 0
    private var selected: String = ""
    private val CHECKED_ITEM = "checked_item"

companion object{
    lateinit var auth : FirebaseAuth
}
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_more_nav, container, false)
        binding = FragmentMoreNavBinding.bind(view)
        (activity as AppCompatActivity).supportActionBar?.title = "SeeA Player"

        auth = FirebaseAuth.getInstance()

        binding.goToSignin.setOnClickListener {
           it.findNavController().navigate(R.id.action_moreNav_to_profile)
        }
        if (auth.currentUser == null) {
            startActivity(Intent(requireContext(), signin::class.java))
            requireActivity().finish()
        }

        binding.signOut.setOnClickListener {
            auth.signOut()
            binding.userDetails.text = updateData()
        }

        binding.subscribePlans.setOnClickListener {
            it.findNavController().navigate(R.id.action_moreNav_to_seeAOne)

        }

        (activity as AppCompatActivity).supportActionBar?.apply {
            setBackgroundDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.background_actionbar
                )
            )
        }

        binding.appThemelayout.setOnClickListener {
            showDialog()

        }
        setActionBarGradient()
        relativeLayout = binding.relativeLayoutMore

        // Set the background color of SwipeRefreshLayout based on app theme
        setRelativeLayoutBackgroundColor()


        return view
    }

    private fun setRelativeLayoutBackgroundColor() {
        val isDarkMode = when (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES -> true
            else -> false
        }

        if (isDarkMode) {
            // Dark mode is enabled, set background color to #012030
            relativeLayout.setBackgroundColor(resources.getColor(R.color.dark_cool_blue))
        } else {
            // Light mode is enabled, set background color to white
            relativeLayout.setBackgroundColor(resources.getColor(android.R.color.white))
        }
    }
    override fun onResume() {
        super.onResume()
        binding.userDetails.text = updateData()

    }

    private fun updateData(): String {

        return "Name : ${auth.currentUser?.displayName}"

    }
    fun showDialog() {
        val themes = resources.getStringArray(R.array.theme)
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Select Theme")
        builder.setSingleChoiceItems(
            R.array.theme,
            getCheckedItem()
        ) { dialogInterface: DialogInterface, i: Int ->
            selected = themes[i]
            checkedItem = i
        }

        builder.setPositiveButton("OK") { dialogInterface: DialogInterface, i: Int ->
            if (selected == null) {
                selected = themes[i]
                checkedItem = i
            }

            when (selected) {
                "System Default" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                "Dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                "Light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

            }
            setCheckedItem(checkedItem)
        }

        builder.setNegativeButton("Cancel") { dialogInterface: DialogInterface, i: Int ->
            dialogInterface.dismiss()
        }

        val dialog = builder.create()
        dialog.show()
    }
    private fun getCheckedItem(): Int {
        return requireContext().getSharedPreferences("YourSharedPreferencesName", Context.MODE_PRIVATE)
            .getInt(CHECKED_ITEM, checkedItem)
    }

    private fun setCheckedItem(i: Int) {
        requireContext().getSharedPreferences("YourSharedPreferencesName", Context.MODE_PRIVATE)
            .edit()
            .putInt(CHECKED_ITEM, i)
            .apply()
    }

    private fun setActionBarGradient() {
        // Check if light mode is applied
        if (AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_NO) {
            // Set gradient background for action bar
            (activity as AppCompatActivity).supportActionBar?.apply {
                setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        requireContext(),
                        R.drawable.background_actionbar_light
                    )
                )
            }

        }
    }

}