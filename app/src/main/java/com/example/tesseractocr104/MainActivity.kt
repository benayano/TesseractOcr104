package com.example.tesseractocr104

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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

    private var hebrew = false
    private var english = false
    private var french = false
    private var lasteLanguag: String = "eng"

    private fun languageIsInitialized(language: String): Boolean {
        return when (language) {
            "eng" -> english
            "fra" -> french
            "heb" -> hebrew
            else -> {false}
        }
    }

    private fun initializedLanguage(language: String) {
        when (language) {
            "eng" -> english = true
            "fra" -> french = true
            "heb" -> hebrew = true
            //else -> {}
        }
    }

    companion object {
        private const val CAMERA_REQUEST_ID = 10
    }


    private val tess = TessBaseAPI()

    private val tvFirst: TextView by lazy { findViewById(R.id.tvFirst) }
    private val ivMain: ImageView by lazy { findViewById(R.id.ivMain) }
    private val btnExtract: Button by lazy { findViewById(R.id.btnExtract) }
    private val btnGetCamera: Button by lazy { findViewById(R.id.btnGetCamera) }
    private val engLanguageFile: String = "tessdata/eng.traineddata"


    private fun saveLanguage(language: String) = "tessdata/$language.traineddata"

    private lateinit var dataPath: String
    private val trainData = "tesseract/tessdata"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dataPath = this.getExternalFilesDir("/")!!.path + "/"

        val bitmap: Bitmap = bitmapFromImageView(ivMain)

        btnExtract.setOnClickListener {
            if (permissionCameraGranted()) {
                prepareTessData(bitmap)
            } else {
                Toast.makeText(this, " אין הרשאת גישה למצלמה ויותר מזה!!!", Toast.LENGTH_LONG)
                    .show()
                requestCameraPermission()
            }
        }
        btnGetCamera.setOnClickListener {
            if (permissionCameraGranted()) {
                getCamera()
            } else {
                Toast.makeText(this, " אין הרשאת גישה למצלמה ויותר מזה!!!", Toast.LENGTH_LONG)
                    .show()
                requestCameraPermission()
            }
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_language, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menIdHebrew -> {
                lasteLanguag = "heb"
                return true
            }
            R.id.menIdEnglish -> {
                lasteLanguag = "eng"
                return true
            }
            R.id.menIdFrench -> {
                lasteLanguag = "fra"
                return true
            }
            else -> {
                return true
            }
        }
    }

    private fun bitmapFromImageView(imageView: ImageView): Bitmap {
        val drawable: Drawable = imageView.drawable
        return drawable.toBitmap(config = Bitmap.Config.ARGB_8888)
    }

    private fun getCamera() {
        val cameraInt = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        getResult.launch(cameraInt)
    }

    private val getResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == Activity.RESULT_OK) {
                changeImage(it.data)
            }
        }

    private fun changeImage(data: Intent?) {
        val images: Bitmap = data?.extras?.get("data") as Bitmap
        ivMain.setImageBitmap(images)
        if (!languageIsInitialized(lasteLanguag)) {
            prepareTessData(bitmapFromImageView(ivMain), language = lasteLanguag)
            initializedLanguage(lasteLanguag)
        } else {
            startOcr(bitmapFromImageView(ivMain), language = lasteLanguag)
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

    private fun startOcr(bitmap: Bitmap, language: String = "eng") {
        tess.setImage(bitmap)
        try {
            tess.init(dataPath, language)
            CoroutineScope(Dispatchers.Unconfined).launch {
                tvFirst.text = tess.utF8Text
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "$e")
        }
        Log.d("MainActivity", "image text is : ${tess.utF8Text}")
        tess.clear()
    }

    private fun prepareTessData(bitmap: Bitmap, language: String = "eng") {
        try {
            val dir = getExternalFilesDir("/")
            checkDirExist(dir)

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
                        val inputStream = assets.open(saveLanguage(language))
                        Log.d("tag", "input stream is: $inputStream")
                        val outputStream: OutputStream =
                            FileOutputStream("${pathToDataFile}/$language.traineddata")
                        Log.d("tag", "outputStream is: $outputStream")
                        val buffer = ByteArray(1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                        }
                        startOcr(bitmap, language = language)
                        inputStream.close()
                        outputStream.close()
                    }
                }
            }
        } catch (e: java.lang.Exception) {
            Log.e("tag", "ERROR\n\nfile is not valid ${e.message!!}")
        }
    }

    private fun checkDirExist(dir: File?) {
        if (!dir!!.exists()) {
            if (!dir.mkdir()) {
                Toast.makeText(
                    applicationContext,
                    "The folder " + dir.path + "was not created",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Log.d("tag", "the path of dir is ${dir.path}")
            }
        }
    }


}
