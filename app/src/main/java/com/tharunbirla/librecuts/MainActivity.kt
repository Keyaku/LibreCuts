package com.tharunbirla.librecuts

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.tharunbirla.librecuts.databinding.ActivityMainBinding
import com.tharunbirla.librecuts.utils.ErrorCode
import com.tharunbirla.librecuts.utils.setBounceClickListener

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val selectVideoLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                Log.d("VideoSelection", "Video selected: $uri")
                navigateToEditingScreen(uri)
            } else {
                Log.e("VideoSelectionError", "No video selected")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGlobalCrashHandler()

        binding.btnImport.setBounceClickListener {
            Log.d("ButtonClick", "Launching video selection.")
            selectVideo()
        }

        // Initialize bottom navigation tab backgrounds
        val attrs = intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
        val ta = obtainStyledAttributes(attrs)
        val inactiveBg = ta.getDrawable(0)
        ta.recycle()
        binding.tabAbout.background = inactiveBg

        // Setup bottom navigation tab switching
        binding.tabHome.setBounceClickListener {
            switchTab(true)
        }

        binding.tabAbout.setBounceClickListener {
            switchTab(false)
        }

        // Setup GitHub button listeners
        binding.btnStarGithub.setBounceClickListener {
            openUrl("https://github.com/tharunbirla/LibreCuts")
        }

        binding.btnReportBug.setBounceClickListener {
            openUrl("https://github.com/tharunbirla/LibreCuts/issues")
        }
        binding.btnSponsor.setBounceClickListener {
            openUrl("https://github.com/sponsors/tharunbirla")
        }

    }

    private fun switchTab(isHome: Boolean) {
        val activeBg = ContextCompat.getDrawable(this, R.drawable.bg_nav_active)
        val attrs = intArrayOf(android.R.attr.selectableItemBackgroundBorderless)
        val ta = obtainStyledAttributes(attrs)
        val inactiveBg = ta.getDrawable(0)
        ta.recycle()

        if (isHome) {
            binding.layoutHomeContent.visibility = View.VISIBLE
            binding.layoutAboutContent.visibility = View.GONE
            binding.tabHome.background = activeBg
            binding.ivHome.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary))
            binding.tabAbout.background = inactiveBg
            binding.ivAbout.setColorFilter(ContextCompat.getColor(this, R.color.inactiveTool))
        } else {
            binding.layoutHomeContent.visibility = View.GONE
            binding.layoutAboutContent.visibility = View.VISIBLE
            binding.tabAbout.background = activeBg
            binding.ivAbout.setColorFilter(ContextCompat.getColor(this, R.color.colorPrimary))
            binding.tabHome.background = inactiveBg
            binding.ivHome.setColorFilter(ContextCompat.getColor(this, R.color.inactiveTool))
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            showToast("Unable to open link")
        }
    }

    private fun setupGlobalCrashHandler() {
        val oldHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("LibreCutsCrash", "CRITICAL CRASH", throwable)

            // Start a dedicated Error Activity to show the dialog
            val intent = Intent(this, ErrorDisplayActivity::class.java).apply {
                putExtra("ERROR_CODE", ErrorCode.UNEXPECTED_CRASH.code)
                putExtra("ERROR_LOG", Log.getStackTraceString(throwable))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            startActivity(intent)

            // Terminate the crashed process
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(1)
        }
    }



    private fun selectVideo() {
        Log.d("VideoSelection", "Launching video selector.")
        selectVideoLauncher.launch("video/*")
    }

    private fun navigateToEditingScreen(videoUri: Uri) {
        Log.d("Navigation", "Navigating to editing screen with URI: $videoUri")
        val intent = Intent(this, VideoEditingActivity::class.java)
        intent.putExtra("VIDEO_URI", videoUri)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Log.d("ToastMessage", "Showing toast: $message")
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}