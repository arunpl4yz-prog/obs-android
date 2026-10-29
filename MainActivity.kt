package com.obsmobile.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var toggle: Button

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result[Manifest.permission.RECORD_AUDIO] == true) {
                requestScreenCapture()
            } else {
                Toast.makeText(this, "Mic permission zaroori hai", Toast.LENGTH_SHORT).show()
            }
        }

    private val captureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            if (result.resultCode == RESULT_OK && data != null) {
                val i = Intent(this, RecorderService::class.java).apply {
                    action = RecorderService.ACTION_START
                    putExtra(RecorderService.EXTRA_CODE, result.resultCode)
                    putExtra(RecorderService.EXTRA_DATA, data)
                }
                ContextCompat.startForegroundService(this, i)
                RecorderService.isRecording = true
                refreshUi()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        status = findViewById(R.id.status)
        toggle = findViewById(R.id.toggle)

        toggle.setOnClickListener {
            if (RecorderService.isRecording) {
                startService(Intent(this, RecorderService::class.java).apply {
                    action = RecorderService.ACTION_STOP
                })
                RecorderService.isRecording = false
                refreshUi()
            } else {
                ensurePermissionsThenCapture()
            }
        }
        refreshUi()
    }

    override fun onResume() {
        super.onResume()
        refreshUi()
    }

    private fun ensurePermissionsThenCapture() {
        val needed = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) needed += Manifest.permission.RECORD_AUDIO
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) needed += Manifest.permission.POST_NOTIFICATIONS

        if (needed.isEmpty()) requestScreenCapture()
        else permissionLauncher.launch(needed.toTypedArray())
    }

    private fun requestScreenCapture() {
        val mgr = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureLauncher.launch(mgr.createScreenCaptureIntent())
    }

    private fun refreshUi() {
        if (RecorderService.isRecording) {
            status.text = "Recording chalu hai..."
            toggle.text = "Stop Recording"
        } else {
            status.text = "Ready"
            toggle.text = "Start Recording"
        }
    }
}
