package com.dudek9.monitoring

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestoreException

class MainActivity: Activity() {

    private val trackingService by lazy { TrackingService(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkCameraPermission()
        trackingService.initCamera()
    }


    private fun checkCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(ContentValues.TAG, "No permission");
            return;
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        trackingService.clear()
    }

}