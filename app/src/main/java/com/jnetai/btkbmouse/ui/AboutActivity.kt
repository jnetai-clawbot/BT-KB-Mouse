package com.jnetai.btkbmouse.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.jnetai.btkbmouse.databinding.ActivityAboutBinding
import com.jnetai.btkbmouse.ui.viewmodel.AboutViewModel
import kotlinx.coroutines.launch

/**
 * Activity for app information and credits.
 * Uses StateFlow for reactive UI updates.
 */
class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    private val viewModel: AboutViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupClickListeners() {
        binding.btnCheckUpdates.setOnClickListener {
            viewModel.checkForUpdate()
        }

        binding.btnShare.setOnClickListener {
            shareApp()
        }

        binding.cardSourceCode.setOnClickListener {
            openUrl(viewModel.sourceCodeUrl)
        }

        binding.cardPlayStore.setOnClickListener {
            openUrl(viewModel.playStoreUrl)
        }

        binding.cardDocumentation.setOnClickListener {
            openUrl(viewModel.documentationUrl)
        }

        binding.cardLicense.setOnClickListener {
            showLicenseInfo()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe app name
                launch {
                    viewModel.appName.collect { name ->
                        binding.tvAppName.text = name
                    }
                }

                // Observe version
                launch {
                    viewModel.versionName.collect { version ->
                        binding.tvVersion.text = version
                    }
                }

                // Observe build date
                launch {
                    viewModel.buildDate.collect { date ->
                        binding.tvBuildDate.text = "Built: $date"
                    }
                }

                // Observe developer
                launch {
                    viewModel.developerName.collect { developer ->
                        binding.tvDeveloper.text = developer
                    }
                }

                // Observe update checking state
                launch {
                    viewModel.isCheckingUpdate.collect { isChecking ->
                        binding.btnCheckUpdates.isEnabled = !isChecking
                        binding.progressUpdate.visibility = if (isChecking) {
                            android.view.View.VISIBLE
                        } else {
                            android.view.View.GONE
                        }
                    }
                }

                // Observe update status
                launch {
                    viewModel.updateStatus.collect { status ->
                        when (status) {
                            is AboutViewModel.UpdateStatus.Idle -> {
                                binding.tvUpdateStatus.text = ""
                            }
                            is AboutViewModel.UpdateStatus.Checking -> {
                                binding.tvUpdateStatus.text = "Checking for updates..."
                            }
                            is AboutViewModel.UpdateStatus.UpToDate -> {
                                binding.tvUpdateStatus.text = "You're up to date!"
                                Toast.makeText(this@AboutActivity, "App is up to date", Toast.LENGTH_SHORT).show()
                            }
                            is AboutViewModel.UpdateStatus.UpdateAvailable -> {
                                binding.tvUpdateStatus.text = "Update available: ${status.version}"
                                Toast.makeText(this@AboutActivity, "New version available!", Toast.LENGTH_LONG).show()
                            }
                            is AboutViewModel.UpdateStatus.Error -> {
                                binding.tvUpdateStatus.text = "Update check failed"
                                Toast.makeText(this@AboutActivity, status.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "BT-KB-Mouse App")
            putExtra(Intent.EXTRA_TEXT, viewModel.getShareText())
        }
        startActivity(Intent.createChooser(shareIntent, "Share via"))
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showLicenseInfo() {
        android.app.AlertDialog.Builder(this)
            .setTitle("License")
            .setMessage("""
                BT-KB-Mouse
                Copyright © 2024 jnetai.com

                Licensed under the Apache License, Version 2.0 (the "License");
                you may not use this file except in compliance with the License.

                Unless required by applicable law or agreed to in writing, software
                distributed under the License is distributed on an "AS IS" BASIS,
                WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
            """.trimIndent())
            .setPositiveButton("OK", null)
            .show()
    }
}
