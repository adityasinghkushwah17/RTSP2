package com.adityacodes.rtsp2

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File

class StreamViewModel(application: Application) : AndroidViewModel(application) {

    private val recordingsDir: File =
        File(application.getExternalFilesDir(null), "recordings").apply {
            if (!exists()) mkdirs()
        }

    private var currentSession: FFmpegSession? = null
    private var lastOutputPath: String? = null

    private fun getOutputPath(): String {
        val fileName = "stream_${System.currentTimeMillis()}.mp4"
        return File(recordingsDir, fileName).absolutePath
    }

    fun startRecording(rtspUrl: String, onComplete: (Boolean, String) -> Unit) {
        val context = getApplication<Application>()

        val output = createMediaStoreOutputFile()
        if (output == null) {
            onComplete(false, "Failed to create MediaStore file ❌")
            return
        }

        val (uriPath, pfd) = output
        val outputFdPath = "/proc/self/fd/${pfd.fd}"

        val command = "-y -rtsp_transport tcp -i \"$rtspUrl\" -c copy -f mp4 \"$outputFdPath\""

        currentSession = FFmpegKit.executeAsync(command) { session ->
            val returnCode = session.returnCode

            if (returnCode.isValueSuccess) {
                // Mark IS_PENDING = 0 to show in gallery
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                context.contentResolver.update(android.net.Uri.parse(uriPath), values, null, null)
                onComplete(true, "Recording saved to gallery ✅")
            } else {
                context.contentResolver.delete(android.net.Uri.parse(uriPath), null, null)
                onComplete(false, "Recording failed ❌")
            }

            pfd.close()
        }
    }

    fun createMediaStoreOutputFile(): Pair<String, ParcelFileDescriptor>? {
        val resolver = getApplication<Application>().contentResolver
        val fileName = "stream_${System.currentTimeMillis()}.mp4"

        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/RTSPRecordings")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val videoUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
        val pfd = videoUri?.let { resolver.openFileDescriptor(it, "w") }

        return if (videoUri != null && pfd != null) {
            videoUri.toString() to pfd
        } else {
            null
        }
    }


    fun stopRecording() {
        currentSession?.cancel()
        currentSession = null
    }

}
