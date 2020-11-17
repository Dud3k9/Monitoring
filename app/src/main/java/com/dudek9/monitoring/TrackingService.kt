package com.dudek9.monitoring

import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TrackingService(private val context: MainActivity) {

    val mainHandler=Handler(Looper.getMainLooper())
    private lateinit var mCamera: DoorbellCamera
    private lateinit var mCameraThread: HandlerThread
    private var isSaveing=false
    private var isTracking=false
    var previousImageBytes: ByteArray? =null

   fun initCamera() {
        mCamera = DoorbellCamera.getInstance()
        mCamera.initializeCamera(context, getCameraHandler(), myOnImageAvailableListener)
        onChangeFirebaseInfo()
    }

    private fun getCameraHandler(): Handler {
        mCameraThread = HandlerThread("CameraBackground");
        mCameraThread!!.start();
        return Handler(mCameraThread!!.looper);
    }

     private fun onChangeFirebaseInfo() {
        Firebase.checkInfo(EventListener<DocumentSnapshot> { snap, p1 ->
            if (snap!=null && snap.exists()){
                var isArmed:Boolean= snap["isArmed"] as Boolean
                var isAlarm:Boolean= snap["isAlarm"] as Boolean
                if (isArmed)
                    startTracking()
                else
                    stopTracking()

                isSaveing = isAlarm
            }
        })
    }

    private fun startTracking() {
        isTracking = true
        GlobalScope.launch {
            Thread.sleep(1000)
            while (isTracking) {
                Thread.sleep(800)
                mainHandler.post {
                    mCamera.takePicture()
                }
            }
        }
    }

    private fun stopTracking() {
        isTracking = false;
    }


    val myOnImageAvailableListener=ImageReader.OnImageAvailableListener() {
        var i = it?.acquireLatestImage()
        var imageBuffer = i?.planes?.get(0)?.buffer
        var imageBytes = imageBuffer?.remaining()?.let { it1 -> ByteArray(it1) }
        imageBuffer?.get(imageBytes)//??
        imageBuffer?.flip()//??

        var bitmap = i?.let { it1 -> Convert.imageToBitmap(it1,0f) }


        if(previousImageBytes==null){
            previousImageBytes=imageBytes
        }else{
            var motionPercent= imageBytes?.let { it1 -> MotionDetector.motinPercent(it1,previousImageBytes!!) }
            previousImageBytes=imageBytes
            Log.d("motion",motionPercent.toString())
            if (!isSaveing) {
                if(motionPercent!=null && motionPercent>5){
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

    fun clear(){
        mCamera.shutDown()
        mCameraThread.quitSafely()
    }

}
