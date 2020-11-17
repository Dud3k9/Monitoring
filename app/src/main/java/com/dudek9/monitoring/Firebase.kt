package com.dudek9.monitoring

import android.graphics.Bitmap
import android.os.Build
import android.os.Message
import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.storage.ktx.storage
import com.google.gson.JsonObject
import com.squareup.okhttp.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.time.LocalDate
import java.time.LocalTime
import java.util.*
import kotlin.random.Random
import kotlin.random.Random.Default.nextInt


class Firebase {


    companion object {


        private val storage = Firebase.storage("gs://monitoring-6b337.appspot.com")
        private val db = Firebase.firestore

        fun checkInfo(listener: EventListener<DocumentSnapshot>) {
            val docRef = db.collection("database").document("info")
            docRef.addSnapshotListener(listener)
        }

        fun setAlarm(boolean: Boolean){
            val docRef = db.collection("database").document("info")
            docRef.update("isAlarm", boolean)
        }


        fun savePicture(bitmap: Bitmap) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                var date = LocalDate.now()
                var time = LocalTime.now()

                saveFolderToDatabse(date.toString())
                var storageRef = storage.reference
                var spaceRef = storageRef.child(date.toString() + "/" + time.toString())
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()

                var uploadTask = spaceRef.putBytes(data)
            }
        }

        fun saveFolderToDatabse(folder:String) {
                val docRef = db.collection("database").document("folders")
                docRef.addSnapshotListener { documentSnapshot, firebaseFirestoreException ->
                    if (!documentSnapshot!!.contains(folder)) {
                        docRef.update(folder, null)
                    }
                }

        }

        fun sendMessage(){
            val payload="{" +
                    "\"category\": \"com.dudek9.flutterpilot\"," +
                    "\"notification\":{" +
                    "\"body\":\"Wykryto ruch w mieszkaniu\"," +
                    "\"title\":\"Alarm!\"" +
                    "\"sound\":\"default\""+
                    "}," +
                    "\"data\":{" +
                    "\"click_action\":\"FLUTTER_NOTIFICATION_CLICK\"," +
                    "\"id\":\"1\"" +
                    "}," +
                    "\"to\":\"/topics/alarm\"" +
                    "}"

            val okHttpClient = OkHttpClient()
            val requestBody = RequestBody.create(MediaType.parse("JSON"),payload)
            val request = Request.Builder()
                .method("POST",requestBody)
                .addHeader("Authorization","key=AAAAI_iX484:APA91bHdgTvyd5WFSJAWzzKFiAD0VodoVLehE1eLVlRVyC8tO6ulFx3eM9whcuDIL7ErYews-TswL3FQ6w7njd9z7goNxCd4eReCkPuczXVr-HwxfhJTvjB7_b1iAzqLyp8gFriMIgg-")
                .addHeader("Content-Type","application/json")
                .url("https://fcm.googleapis.com/fcm/send")
                .build()
//            GlobalScope.launch {
//                okHttpClient.newCall(request).execute()}
            okHttpClient.newCall(request).execute()
//            Thread({okHttpClient.newCall(request).execute()}).start()
        }

    }


}