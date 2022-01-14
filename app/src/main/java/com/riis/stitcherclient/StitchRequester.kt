package com.riis.stitcherclient

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Message
import android.util.Log
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection
import android.os.FileUtils
import java.io.*
import java.nio.charset.StandardCharsets


class StitchRequester(context: Context) {

    //class variables
    private val activity: AppCompatActivity = context as AppCompatActivity
    private var mHandler: Handler? = null
    private val xtag = "cars"

    val start_stitch_url = "http://192.168.2.25:8000/start_stitch_batch"
    var add_image_url = "http://192.168.2.25:8000/add_stitch_image/"
    var stitch_batch_url = "http://192.168.2.25:8000/stitch_batch"

    //completion callbacks
    enum class StitchMessage(val value: Int) {
        START_BATCH_SUCCESS(100), START_BATCH_FAILURE(101),
        STITCH_IMAGE_ADD_SUCCESS(102), STITCH_IMAGE_ADD_FAILURE(103);
    }

    //setting the handler to one provided by PhotoStitcherActivity.kt
    fun setHandler(h: Handler) {
        this.mHandler = h
    }

    //Obtaining a batch id for a new set of images
    fun requestStitchId() {
        activity.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                //creating a URL object for the link needed to obtain a batch id
                val url: URL? = getURLforPath(start_stitch_url) //NetworkInformation.PATH_STITCH_START

                //trying to establish a connection to the url and then getting a message from it
                var connection: HttpURLConnection? = null //HttpsURLConnection? = null

                try {
                    if (url != null) {
                        connection = url.openConnection() as HttpURLConnection //as HttpsURLConnection
                    }
                    if (connection != null) {
                        connection.requestMethod = "GET"
                    }
                    //reading responses from the connection to get the batch id
                    val r = BufferedReader(InputStreamReader(connection?.inputStream))
                    val batchId: String = r.readLine()

                    connection?.connect() //Opens a communications link to the resource referenced by this URL

                    //sending the Handler a message to start the stitching process with the batchId
                    sendMessage(StitchMessage.START_BATCH_SUCCESS, batchId)

                //catching any errors and sending them to Handler
                } catch (e: Exception) {
                    sendMessage(StitchMessage.START_BATCH_FAILURE, e.message)

                //once the thread is complete, close the connection
                } finally {
                    connection?.disconnect()
                }
            }
        }
    }


    /* Add a list of images to the batch by sending it over the network */
    fun addImage(file: File, batchId: String) {
        activity.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val boundary = "Boundary-${System.currentTimeMillis()}"
                val url: URL? =
                    getURLforPath(add_image_url + batchId) //NetworkInformation.PATH_STITCH_ADD_IMAGE
                var connection: HttpURLConnection? = null //HttpsURLConnection? = null

                try {
                    if (url != null) {
                        connection = url.openConnection() as HttpURLConnection //= url.openConnection() as HttpsURLConnection
                    }
                    if (connection != null) {

                        connection.addRequestProperty(
                            "Content-Type",
                            "multipart/form-data; boundary=$boundary"
                        )
                        connection.requestMethod = "POST"
                        connection.doInput = true
                        connection.doOutput = true

                        val output = connection.outputStream
                        val writer = BufferedWriter(OutputStreamWriter(output))

                        writer.write("\n--$boundary\n")
                        writer.write("Content-Disposition: form-data;"
                                + "name=\"myFile\";"
                                + "filename=\"" + file.name + "\""
                                + "\nContent-Type: image/png\n\n")
                        writer.flush()

                        val inputStreamToFile = FileInputStream(file)
                        var bytesRead: Int
                        val dataBuffer = ByteArray(1024)
                        while (inputStreamToFile.read(dataBuffer).also { bytesRead = it } != -1) {
                            output.write(dataBuffer, 0, bytesRead)
                        }
                        output.flush()

                        // End of the multipart request
                        writer.write("\n--$boundary--\n")
                        writer.flush()

                        // Close the streams
                        output.close()
                        writer.close()
                    }


                    if (connection != null) {
                        if (connection.responseCode == 200) {
                            val r = BufferedReader(InputStreamReader(connection?.inputStream))
                            val uploadedImageName: String = r.readLine()
                            sendMessage(StitchMessage.STITCH_IMAGE_ADD_SUCCESS, uploadedImageName)
                        } else {
                            sendMessage(
                                StitchMessage.STITCH_IMAGE_ADD_FAILURE,
                                "Response code: " + connection.responseCode
                            )
                        }
                    }
                //catching any errors and sending them to Handler
                } catch (e: Exception) {
                    sendMessage(StitchMessage.START_BATCH_FAILURE, e.message)

                    //once the thread is complete, close the connection
                } finally {
                    connection?.disconnect()
                }
            }
        }
    }




    @Nullable
    private fun getURLforPath(path: String): URL? {
        try {
            return URL(path)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun sendMessage(msg: StitchMessage, @Nullable companion: Any?) {
        val value = msg.value
        val m: Message? = mHandler?.obtainMessage()
        if (m != null) {
            m.what = value
        }
        if (companion != null) if (m != null) {
            m.obj = companion
        }
        m?.sendToTarget()
    }

}

