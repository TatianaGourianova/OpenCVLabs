package io.kotelliada.playground

import android.Manifest
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import org.jetbrains.anko.*
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.JavaCameraView
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier

class MotionActivity : BasePhotoActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private var cameraView : JavaCameraView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isGranted(Manifest.permission.CAMERA) || !isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            finish()
            return
        }

        cameraView = JavaCameraView(this, JavaCameraView.CAMERA_ID_BACK)
        cameraView?.let {
            it.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            it.id = View.generateViewId()
            it.setCvCameraViewListener(this)
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        relativeLayout {
            addView(cameraView)
        }


    }

    override fun onPause() {
        super.onPause()
        cameraView?.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraView?.disableView()
    }

    override fun onResume() {
        super.onResume()
        OpenCVLoader.initDebug(true)
        cameraView?.enableView()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat? {
        inputFrame?.let { frame ->
            val gray = frame.gray()
            val original = frame.rgba()

            return original
        }
        return null
    }


    override fun onCameraViewStarted(width: Int, height: Int) {

    }

    override fun onCameraViewStopped() {

    }
}