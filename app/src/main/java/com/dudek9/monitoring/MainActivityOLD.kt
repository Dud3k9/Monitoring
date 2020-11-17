package com.dudek9.monitoring

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.media.ImageReader
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivityOLD : Activity() {

    private var mCameraHandler: Handler? = null
    private var mCameraThread: HandlerThread? = null
    private lateinit var mCamera: DoorbellCamera
    private lateinit var handler: Handler
    private var isTracking = false
    private var isShowingPicture = true //for debugging==true
    private var isSaveing = false

    private var firebaseMessaging=FirebaseMessaging.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
            
        if (checkSelfPermission(Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // A problem occurred auto-granting the permission
            Log.e(TAG, "No permission");
            return;
        }

        handler = Handler()

        mCameraThread = HandlerThread("CameraBackground");
        mCameraThread!!.start();
        mCameraHandler = Handler(mCameraThread!!.looper);

        mCamera = DoorbellCamera.getInstance()

        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener)
        button.setOnClickListener {
            mCamera.takePicture()
        }
    }

    override fun onStart() {
        super.onStart()

        Firebase.checkInfo(object : EventListener<DocumentSnapshot> {
            override fun onEvent(snap: DocumentSnapshot?, p1: FirebaseFirestoreException?) {
                if (snap!=null && snap.exists()){
                   var isArmed:Boolean= snap["isArmed"] as Boolean
                   var isAlarm:Boolean= snap["isAlarm"] as Boolean
                    if (isArmed)
                        startTracking()
                    else
                        stopTracking()

                    if(isAlarm) {
                        isSaveing = true

                    }else
                        isSaveing=false
                }
            }

        })


    }


    fun startTracking() {
        isTracking = true
        GlobalScope.launch {
            Thread.sleep(1000)
            while (isTracking) {
                Thread.sleep(800)
                handler.post {
                    mCamera.takePicture()
                }
            }
        }
    }

    fun stopTracking() {
        isTracking = false;
    }


    var previousImageBytes: ByteArray? =null;

    val mOnImageAvailableListener = ImageReader.OnImageAvailableListener() {
        var i = it?.acquireLatestImage()
        var imageBuffer = i?.planes?.get(0)?.buffer
        var imageBytes = imageBuffer?.remaining()?.let { it1 -> ByteArray(it1) }
        imageBuffer?.get(imageBytes)
        imageBuffer?.flip()

//        var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size - 1)
//        var bitmap = BitmapFactory.decodeByteArray(Convert.YUV_420_888toJPEG(i), 0, imageBytes.size - 1)
        var bitmap = i?.let { it1 -> Convert.imageToBitmap(it1,0f) }
        if (isShowingPicture)//for debugging==true
            handler.post { image.setImageBitmap(bitmap) }

        if(previousImageBytes==null){
            previousImageBytes=imageBytes;
        }else{
            var motionPercent=
                imageBytes?.let { it1 -> MotionDetector.motinPercent(it1,previousImageBytes!!) };
            previousImageBytes=imageBytes;
            Log.d("motion",motionPercent.toString())
            if (motionPercent != null) {
                if(motionPercent>5){
                    isSaveing=true
                    Firebase.setAlarm(true)
                    Firebase.sendMessage()
                }
            }
        }
        if (isSaveing)
            bitmap?.let { it1 -> Firebase.savePicture(it1) }
        i?.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        mCamera.shutDown();
        mCameraThread!!.quitSafely()
    }

}
