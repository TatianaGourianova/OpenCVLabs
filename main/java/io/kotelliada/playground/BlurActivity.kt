package io.kotelliada.playground

import android.Manifest
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import org.jetbrains.anko.relativeLayout
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.JavaCameraView
import org.opencv.android.OpenCVLoader
import android.annotation.SuppressLint
import android.view.MotionEvent
import org.jetbrains.anko.accountManager
import org.jetbrains.anko.toast
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import java.util.ArrayList


class BlurActivity : BasePhotoActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private lateinit var cameraView : JavaCameraView
    private var startX = -1f
    private var startY = -1f
    private var filterSize = 0.0

    private var filterMat : Mat = Mat(3, 3, CvType.CV_32F)

    init {

        filterMat.put(0, 0, -0.1)
        filterMat.put(0, 1, 0.2)
        filterMat.put(0, 2, -0.1)

        filterMat.put(1, 0, 0.2)
        filterMat.put(1, 1, 3.0)
        filterMat.put(1, 2, 0.2)

        filterMat.put(2, 0, -0.1)
        filterMat.put(2, 1, 0.2)
        filterMat.put(2, 2, -0.1)

    }

    private var filter = 9
    private var lastTouch = 0L

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isGranted(Manifest.permission.CAMERA) || !isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            finish()
            return
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)


        cameraView = JavaCameraView(this, JavaCameraView.CAMERA_ID_BACK)
        cameraView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        cameraView.id = View.generateViewId()
        cameraView.setCvCameraViewListener(this)
        cameraView.setOnTouchListener { _, motionEvent ->
            when(motionEvent.action){
                MotionEvent.ACTION_DOWN -> {
                    startX = motionEvent.x
                    startY = motionEvent.y
                    if(lastTouch > System.currentTimeMillis()){
                        if(filter == 13){
                            filter = 0
                        }else{
                            filter++
                        }
                        filterSize = 0.0
                        displayFilter()
                    }
                    lastTouch = System.currentTimeMillis() + 250L
                }
                MotionEvent.ACTION_UP -> {
                    startX = -1f
                    startY = -1f
                }
                MotionEvent.ACTION_MOVE -> {
                    if(Math.abs(startX - motionEvent.x) > 10){
                        val delta = ((startX - motionEvent.x) / 32).toDouble()
                        val change = filterSize + delta
                        filterSize = when {
                            change > 128.0 -> 128.0
                            change < 0 -> 0.0
                            else -> change
                        }
                    }
                }
            }
            return@setOnTouchListener true
        }


        relativeLayout {
            addView(cameraView)
        }
    }

    override fun onPause() {
        super.onPause()
        cameraView.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraView.disableView()
    }

    override fun onResume() {
        super.onResume()
        OpenCVLoader.initDebug(true)
        cameraView.enableView()
    }

    private fun displayFilter(){
        when (filter) {
            0 -> toast(R.string.noFilter)
            1 -> toast(R.string.gaussianBlur)
            2 -> toast(R.string.boxFilter)
            3 -> toast(R.string.blurFilter)
            4 -> toast(R.string.medianBlurFilter)
            5 -> toast(R.string.linearFilter)
            6 -> toast(R.string.erosion)
            7 -> toast(R.string.dilatation)
            8 -> toast(R.string.canny)
            9 -> toast(R.string.sobel)
            10 -> toast(R.string.laplas)
            11 -> toast(R.string.edges)
            12 -> toast(R.string.edges2)
            13 -> toast(R.string.edges3)
        }
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat? {
        inputFrame?.let {
            val mRGBA = it.rgba()
            try{
                if(filter == 1){
                    if(filterSize > 1.0){
                        Imgproc.blur(mRGBA, mRGBA, Size(filterSize, filterSize))
                    }
                }else if(filter == 2){
                    if(filterSize > 1.0){
                        Imgproc.boxFilter(mRGBA, mRGBA, -1, Size(filterSize, filterSize))
                    }
                }else if(filter == 3){
                    if(filterSize > 1.0){
                        Imgproc.blur(mRGBA, mRGBA, Size(filterSize, filterSize))
                    }
                }else if(filter == 4){
                    if(filterSize >= 1.0){
                        val v = if(filterSize.toInt() % 2 == 0) filterSize.toInt() + 1 else filterSize.toInt()
                        Imgproc.medianBlur(mRGBA, mRGBA, v)
                    }
                }else if(filter == 5){
                    Imgproc.filter2D(mRGBA, mRGBA, -1, filterMat)
                }else if(filter == 6){
                    Imgproc.threshold(it.gray(), mRGBA, 200.0, 255.0, Imgproc.THRESH_BINARY)
                    Imgproc.erode(mRGBA, mRGBA, Mat())
                }else if(filter == 7){
                    Imgproc.threshold(it.gray(), mRGBA, 200.0, 255.0, Imgproc.THRESH_BINARY)
                    Imgproc.dilate(mRGBA, mRGBA, Mat())
                }else if(filter == 8){
                    Imgproc.Canny(it.gray(), mRGBA, 100.0, 200.0)
                }else if(filter == 9){
                    Imgproc.blur(mRGBA, mRGBA, Size(3.0, 3.0))
                    val grayMat = Mat()
                    val xGrad = Mat()
                    val yGrad = Mat()
                    val xGradAbs = Mat()
                    val yGradAbs = Mat()
                    val gradMat = Mat()
                    Imgproc.cvtColor(mRGBA, grayMat, Imgproc.COLOR_BGR2GRAY)
                    Imgproc.Sobel(grayMat, xGrad, CvType.CV_16S, 1, 0)
                    Imgproc.Sobel(grayMat, yGrad, CvType.CV_16S, 0, 1)
                    Core.convertScaleAbs(xGrad, xGradAbs)
                    Core.convertScaleAbs(yGrad, yGradAbs)
                    Core.addWeighted(xGradAbs, 0.5, yGradAbs, 0.5, 0.0, gradMat)
                    grayMat.release()
                    xGrad.release()
                    yGrad.release()
                    xGradAbs.release()
                    yGradAbs.release()
                    return gradMat
                }else if(filter == 10){
                    Imgproc.blur(mRGBA, mRGBA, Size(3.0, 3.0))
                    val grayMat = Mat()
                    val laplacianMat = Mat()
                    val laplacianMatAbs = Mat()
                    Imgproc.cvtColor(mRGBA, grayMat, Imgproc.COLOR_BGR2GRAY)
                    Imgproc.Laplacian(grayMat, laplacianMat, CvType.CV_16S)
                    Core.convertScaleAbs(laplacianMat, laplacianMatAbs)
                    grayMat.release()
                    laplacianMat.release()
                    mRGBA.release()
                    return laplacianMatAbs
                }else if(filter == 11){

                }else if(filter == 12){

                }else if(filter == 13){
                    val result = Mat(mRGBA.rows(), mRGBA.cols(), mRGBA.type())
                    mRGBA.copyTo(result)
                    Imgproc.cvtColor(result, result, Imgproc.COLOR_BGR2GRAY)
                    Imgproc.blur(result, result, Size(8.0, 8.0))
                    val circles = Mat()
                    Imgproc.HoughCircles(result, circles, Imgproc.HOUGH_GRADIENT, 2.0, (result.rows() / 8).toDouble(), 100.0, 300.0, 20, 700)
                    if(circles.cols() > 0){
                        for(x in 0 until circles.cols()){
                            val circle = circles.get(0, x)
                            if(circle != null){
                                val point = Point(Math.round(circle[0]).toDouble(), Math.round(circle[1]).toDouble())
                                val radius = Math.round(circle[2]).toInt()
                                Imgproc.circle(mRGBA, point, radius, Scalar(0.0, 255.0, 0.0), 24)
                            }
                        }
                    }
                    circles.release()
                    result.release()
                    return mRGBA
                }
            }catch (e: Exception){
                toast(e.toString())
                filterSize = 0.0
                return it.rgba()
            }
            return mRGBA
        }
        return null
    }

    override fun onCameraViewStarted(width: Int, height: Int) {

    }

    override fun onCameraViewStopped() {
    }

}
