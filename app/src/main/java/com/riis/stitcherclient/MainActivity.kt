package com.riis.stitcherclient

import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.util.Log
import java.io.File

class MainActivity : AppCompatActivity() {

    private var stitchRequester: StitchRequester? = null
    private val logTag: String = "STITCH_ACTIVITY"
    private lateinit var photoStorageDir: File
    private lateinit var testPhoto: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        photoStorageDir = File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.path.toString())
        testPhoto = File(photoStorageDir.path + "/test.png")

        startPhotoStitch()
    }

    private fun startPhotoStitch(){
        Log.d(logTag, "Requesting new batch id from server...")
        stitchRequester = StitchRequester(this)
        val stitchMessageHandler: Handler = getStitchHandler(stitchRequester!!)
        stitchRequester!!.setHandler(stitchMessageHandler)
        stitchRequester!!.requestStitchId()
    }

    private fun getStitchHandler(requester: StitchRequester): Handler {

        return object : Handler(Looper.getMainLooper()) {

            override fun handleMessage(msg: Message) {

                when (msg.what){
                    StitchRequester.StitchMessage.START_BATCH_SUCCESS.value -> {
                        Log.d(logTag, "Request successful. New batch id: ${msg.obj}")

                        Log.d(logTag, "Attempting to upload images to server...")
                        val pics = photoStorageDir.listFiles()
                        if (pics != null) {
                            for(picture in pics){
                                requester.addImage(picture, msg.obj.toString())
                            }
                        }

                    }
                    StitchRequester.StitchMessage.START_BATCH_FAILURE.value -> {
                        Log.d(logTag, "Request Failed. Error: ${msg.obj}")
                    }
                    StitchRequester.StitchMessage.STITCH_IMAGE_ADD_SUCCESS.value -> {
                        Log.d(logTag, "Image '${msg.obj}' uploaded successful")
                    }
                    StitchRequester.StitchMessage.STITCH_IMAGE_ADD_FAILURE.value -> {
                        Log.d(logTag, "Image upload Failed. Error: ${msg.obj}")
                    }

                }
            }
        }


    }
}