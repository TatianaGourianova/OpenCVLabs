package io.kotelliada.playground

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import org.jetbrains.anko.*
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.ArrayList

class MainActivity : BasePhotoActivity() {

    private var activeBitmap : Bitmap? = null

    private lateinit var pickLayout : LinearLayout
    private lateinit var imageDisplay : ImageView

    private var lastTouch = 0L
    private val hideDist = 100f
    private var startX = 0f
    private var startY = 0f

    private var mode = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        OpenCVLoader.initDebug()
        relativeLayout {
            imageDisplay = imageView {
                visibility = View.GONE
                scaleType = ImageView.ScaleType.FIT_CENTER
                setOnTouchListener { view, motionEvent ->
                    when(motionEvent.action){
                        MotionEvent.ACTION_DOWN -> {
                            startX = motionEvent.x
                            startY = motionEvent.y
                        }
                        MotionEvent.ACTION_UP -> {
                            view.animate().alpha(1f).setDuration(200).start()
                            if(lastTouch > System.currentTimeMillis()){
                                changeMode()
                            }
                            lastTouch = System.currentTimeMillis() + 250L
                        }
                        MotionEvent.ACTION_MOVE -> {
                            var dist = Math.pow((Math.pow(startX - motionEvent.x.toDouble(), 2.0) + Math.pow(startY - motionEvent.y.toDouble(), 2.0)) / 300f, 0.5)
                            if(dist > hideDist) dist = 1.0 else dist /= hideDist
                            view.alpha = 1 - dist.toFloat()
                        }
                    }
                    return@setOnTouchListener true
                }
            }.lparams(width = matchParent, height = matchParent){
                centerInParent()
            }
            pickLayout = linearLayout {
                id = View.generateViewId()
                orientation = LinearLayout.VERTICAL
                button {
                    id = View.generateViewId()
                    textResource = R.string.camera
                    setOnClickListener {
                        takePhoto(CAMERA)
                    }
                }
                button {
                    id = View.generateViewId()
                    textResource = R.string.gallery
                    setOnClickListener {
                        takePhoto(GALLERY)
                    }
                }
                button {
                    id = View.generateViewId()
                    textResource = R.string.blur
                    setOnClickListener {
                        startActivity<BlurActivity>()
                    }
                }
                button {
                    id = View.generateViewId()
                    textResource = R.string.detect
                    setOnClickListener {
                        startActivity<DetectActivity>()
                    }
                }
                button {
                    id = View.generateViewId()
                    textResource = R.string.motion
                    setOnClickListener {
                        startActivity<MotionActivity>()
                    }
                }
            }.lparams(width = wrapContent, height = wrapContent){
                clipToPadding = false
                centerInParent()
            }
        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if((requestCode == CAMERA || requestCode == GALLERY) && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            handleAction(requestCode)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode != Activity.RESULT_OK || data == null) return
        when(requestCode){
            GALLERY -> {
                activeBitmap = getImage(this, data.data, 1280)
            }
            CAMERA -> {
                val exif = ExifInterface(currentPhotoPath)
                activeBitmap = rotateBitmap(BitmapFactory.decodeFile(currentPhotoPath),
                        exif.getAttributeInt(
                                ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_UNDEFINED))
            }
            else -> {
                return
            }
        }
        processBitmap()
    }

    private fun changeMode(){
        mode++
        if(mode > 2){
            mode = 0
        }
        activeBitmap?.let {
            when(mode){
                0 -> {
                    imageDisplay.setImageBitmap(it)
                }
                1 -> {
                    val img = Mat()
                    Utils.bitmapToMat(it, img)
                    val mat = findContours(img)
                    val result = Bitmap.createBitmap(img.cols(), img.rows(), Bitmap.Config.RGB_565)
                    Utils.matToBitmap(mat, result)
                    imageDisplay.setImageBitmap(result)
                }
                2 -> {
                    val img = Mat()
                    Utils.bitmapToMat(it, img)
                    val mat = houghLines(img)
                    val result = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.RGB_565)
                    Utils.matToBitmap(mat, result)
                    imageDisplay.setImageBitmap(result)
                }
            }
        }
    }

    private fun findContours(mRGBA: Mat) : Mat{
        val cannyEdges = Mat()
        val h = Mat()
        val cList = ArrayList<MatOfPoint>()
        Imgproc.Canny(mRGBA, cannyEdges, 100.0, 255.0)
        Imgproc.findContours(cannyEdges, cList, h, Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE)
        val result = Mat()
        result.create(cannyEdges.rows(), cannyEdges.cols(), CvType.CV_8UC3)
        if(cList.isNotEmpty()){
            for(i in 0 until cList.size){
                Imgproc.drawContours(result, cList, i, Scalar(255.0, 0.0, 0.0), 4)
            }
        }
        return result
    }

    private fun houghLines(img: Mat) : Mat{
        val gray = Mat()
        Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.blur(gray, gray, Size(3.0, 3.0))

        val edges = Mat()
        Imgproc.Canny(gray, edges, 70.0, 100.0)

        val lines = Mat()
        Imgproc.HoughLinesP(edges, lines, 1.0, Math.PI / 180, 20, 0.0, 10.0)

        val result = Mat()
        result.create(edges.rows(), edges.cols(), CvType.CV_8UC3)
        
        for(z in 0 until  lines.rows()){
            for(i in 0 until  lines.cols()){
                val v = lines.get(z, i)
                Imgproc.line(result, Point(v[0], v[1]), Point(v[2], v[3]), Scalar(255.0, 0.0, 0.0), 3, Core.LINE_AA, 0)
            }
        }

        return result
    }

    private fun processBitmap(){
        pickLayout.visibility = View.GONE
        imageDisplay.visibility = View.VISIBLE
        imageDisplay.alpha = 1f
        changeMode()
    }

    private fun processBitmapEx(){
        activeBitmap?.let {
            pickLayout.visibility = View.GONE
            imageDisplay.visibility = View.VISIBLE
            val img = Mat()
            val grayMat = Mat()
            Utils.bitmapToMat(it, img)
            Imgproc.cvtColor(img, grayMat, Imgproc.COLOR_BGR2GRAY, CvType.CV_32S)
            Imgproc.equalizeHist(grayMat, grayMat)
            val result = Bitmap.createBitmap(grayMat.cols(), grayMat.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(grayMat, result)
            imageDisplay.setImageBitmap(result)
            imageDisplay.alpha = 1f
        }
    }


    override fun onBackPressed() {
        if(pickLayout.visibility != View.VISIBLE){
            pickLayout.visibility = View.VISIBLE
            imageDisplay.visibility = View.GONE
            imageDisplay.setImageBitmap(null)
            activeBitmap?.recycle()
            activeBitmap = null
        }else{
            super.onBackPressed()
        }
    }

}
