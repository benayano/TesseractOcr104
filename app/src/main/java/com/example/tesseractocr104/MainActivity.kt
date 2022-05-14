package com.example.tesseractocr104

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream


class MainActivity : AppCompatActivity() {

    private val permissionArray = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    companion object {
        private const val CAMERA_REQUEST_ID = 10
        private const val TESS_DATA = "/tessdata"
    }


    private val tess = TessBaseAPI()

    // Given path must contain subdirectory `tessdata` where are `*.traineddata` language files
    private val tvFirst: TextView by lazy { findViewById(R.id.tvFirst) }
    private val ivMain: ImageView by lazy { findViewById(R.id.ivMain) }
    private val capture: Button by lazy { findViewById(R.id.capture) }
    private val fraLanguageFile: String = "tessdata/fra.traineddata"

    private lateinit var dataPath: String
    private val trainData = "tesseract/tessdata"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // dataPath = this.externalCacheDir!!.absolutePath//.toString()
        dataPath = this.getExternalFilesDir("/")!!.path + "/"

        val drawable: Drawable = ivMain.drawable
        val bitmap: Bitmap = drawable.toBitmap(config = Bitmap.Config.ARGB_8888)

        val uri:Uri = Uri.parse("android.resource://com.example.tesseractocr104/" + R.drawable.test_for_ocr.toString())

        capture.setOnClickListener {
            if (permissionCameraGranted()) {

                 prepareTessData(bitmap)
              //  prepareTessData(uri)
            } else {
                Toast.makeText(this, " אין הרשאת גישה למצלמה ויותר מזה!!!", Toast.LENGTH_LONG)
                    .show()
                requestCameraPermission()
            }
        }


    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            permissionArray,
            CAMERA_REQUEST_ID
        )
    }

    private fun permissionCameraGranted(): Boolean {
        return ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) ==
                PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun getTranslate(bitmap: Bitmap) {
        tvFirst.text = dataPath
        tess.setImage(bitmap)
        Log.d("MainActivity", "pathToDataFile is $dataPath")
        try {
            tess.init(dataPath , "eng")
            Log.d("MainActivity", "init Tesseract  is Successful!!!! ")
            tvFirst.text = tess.utF8Text

        } catch (e: Exception) {
            Log.e("MainActivity", "$e")
        }
        Log.d("MainActivity", "${bitmap.config}")
        Log.d("MainActivity", "image text is : ${tess.utF8Text}")

        tess.clear()
    }

    private fun prepareTessData(bitmap: Bitmap) {
        try {
           // val dir = getExternalFilesDir(TESS_DATA)
            val dir = getExternalFilesDir("/")
            if (!dir!!.exists()) {
                if (!dir.mkdir()) {
                    Toast.makeText(
                        applicationContext,
                        "The folder " + dir.path + "was not created",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            Log.d("tag","the path of dir is ${dir.path}")

            val fileList = assets.list("")
            Log.d("tag", "0000000\n\n\nthe list file asset  is: \n ${fileList?.toList()}")
            Log.d("tag", "dir is $dir")

            for (fileName in fileList!!) {
                if (fileName.contains("tess")) {
                    Log.d("tag", "file name is : $fileName")
                    val pathToDataFile = "$dir/$fileName"
                    Log.d("tag", " pathToDataFile is : $pathToDataFile")
                    if (File(pathToDataFile).exists()) {
                        Log.d("tag", "file is exists!!!")
                        val inputStream = assets.open(fraLanguageFile)
                        // val input =this.resources.assets.open("fra.traineddata")
                        Log.d("tag", "input stream is: ${inputStream}")
                        val outputStream: OutputStream = FileOutputStream("${pathToDataFile}/eng.traineddata")
                        Log.d("tag", "outputStream is: ${outputStream}")
                        val buffer = ByteArray(1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                        }
                       getTranslate(bitmap)
                        inputStream.close()
                        outputStream.close()
                        Log.d("tag", "222222222222222222222222222")
                    }
                }
            }

        } catch (e: java.lang.Exception) {
            Log.e("tag", "ERROR\n\nfile is not valid ${e.message!!}")
        }
    }



}
