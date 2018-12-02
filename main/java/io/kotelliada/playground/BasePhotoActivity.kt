package io.kotelliada.playground

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import org.jetbrains.anko.alert
import org.jetbrains.anko.yesButton
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

abstract class BasePhotoActivity : AppCompatActivity() {

    var currentPhotoPath : String? = null

    fun isGranted(permission: String) : Boolean{
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun createFile() : File {
        return File.createTempFile("$TEMP_FILE_NAME${SimpleDateFormat("yyyMMdd-HHmmss", Locale.US).format(Date())}", ".jpg", getExternalFilesDir(Environment.DIRECTORY_PICTURES))
    }

    fun takePhoto(code: Int){
        if(!isGranted(Manifest.permission.CAMERA) || !isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            if(!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)){
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA), code)
            }
        }else{
            handleAction(code)
        }
    }

    fun getFileFromResource(r: Int, name: String): File{
        val file = File(applicationContext.filesDir, name)
        if(file.exists()){
            file.delete()
        }
        file.createNewFile()
        val inputStream = resources.openRawResource(r)
        val fileOutputStream = FileOutputStream(file)
        val buffer = ByteArray(4096)
        var read = 0
        read = inputStream.read(buffer)
        while (read != -1){
            fileOutputStream.write(buffer, 0, read)
            read = inputStream.read(buffer)
        }
        inputStream.close()
        fileOutputStream.close()
        return file
    }

    fun handleAction(code: Int){
        when (code){
            CAMERA -> {
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                try{
                    if(cameraIntent.resolveActivity(packageManager) == null) return
                    val file = createFile()
                    val photoUri = FileProvider.getUriForFile(this, "io.kotelliada.ocv.provider", file)
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    currentPhotoPath = file.absolutePath
                    startActivityForResult(cameraIntent, code)
                }catch (e: Exception){
                    Log.d("camera", e.toString())
                }
            }
            GALLERY -> {
                val openGalleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
                openGalleryIntent.type = "image/*"
                if (openGalleryIntent.resolveActivity(packageManager) != null) {
                    startActivityForResult(openGalleryIntent, code)
                }
            }
            else -> {
                alert(R.string.error, R.string.something_went_wrong){
                    yesButton {
                        it.dismiss()
                    }
                }.show()
            }
        }
    }

    fun rotateBitmap(bitmap: Bitmap, orientation: Int) : Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_NORMAL -> return bitmap
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                matrix.setRotate(180f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        try {
            val bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            return bmRotated
        }
        catch (e: OutOfMemoryError) {
            return bitmap
        }
    }

    fun getOrientation(context: Context, uri: Uri) : Int {
        var result = -1
        try{
            val cursor = context.contentResolver.query(uri, arrayOf(MediaStore.Images.ImageColumns.ORIENTATION), null, null, null)
            if (cursor.count != 1) {
                return -1
            }
            cursor.moveToFirst()
            result = cursor.getInt(0)
            cursor.close()
        }catch (e: Exception){

        }
        return  result
    }

    fun getImage(context: Context, uri: Uri, maxSize: Int = 1920) : Bitmap? {
        try{
            var inputStream = context.contentResolver.openInputStream(uri)
            var options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            var rotatedWidth = options.outWidth
            var rotatedHeight = options.outHeight

            val orientation = getOrientation(context, uri)
            if(orientation == 90 || orientation == 270){
                rotatedWidth = options.outHeight
                rotatedHeight = options.outWidth
            }

            var bitmap: Bitmap
            inputStream = context.contentResolver.openInputStream(uri)
            if(rotatedHeight > maxSize || rotatedWidth > maxSize){
                val ratio = Math.max(rotatedWidth / maxSize, rotatedHeight / maxSize)
                options = BitmapFactory.Options()
                options.inSampleSize = ratio
                bitmap = BitmapFactory.decodeStream(inputStream, null, options)
            }else{
                bitmap = BitmapFactory.decodeStream(inputStream)
            }
            inputStream.close()

            if(orientation > 0){
                val matrix = Matrix()
                matrix.postRotate(orientation.toFloat())
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }

            return bitmap

        }catch (e: Exception){
            Log.d("openBitmap", e.toString())
            return null
        }
    }


    companion object {
        const val CAMERA = 0
        const val GALLERY = 1
        const val TEMP_FILE_NAME = "photo-temp-"
    }

}