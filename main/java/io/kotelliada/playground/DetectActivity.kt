package io.kotelliada.playground

import android.Manifest
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.JavaCameraView
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier

class DetectActivity:  BasePhotoActivity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private var cameraView : JavaCameraView? = null

    private lateinit var haarcascadeEye : CascadeClassifier
    private lateinit var haarcascadeFullBody : CascadeClassifier
    private lateinit var haarcascadeSmile : CascadeClassifier
    private lateinit var haarcascadeFrontalCatFace : CascadeClassifier
    private lateinit var haarcascadeFrontalFace : CascadeClassifier

    private lateinit var textMode : TextView

    private val smiles = MatOfRect()
    private val faces = MatOfRect()
    private val eyes = MatOfRect()
    private val bodies = MatOfRect()
    private val cats = MatOfRect()

    private val faceColor = Scalar(0.0, 255.0, 0.0)
    private val eyesColor = Scalar(0.0, 255.0, 255.0)
    private val smileColor = Scalar(0.0, 0.0, 255.0)
    private val bodyColor = Scalar(255.0, 255.0, 255.0)

    private var mode = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isGranted(Manifest.permission.CAMERA) || !isGranted(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            finish()
            return
        }

        haarcascadeEye = CascadeClassifier(getFileFromResource(R.raw.haarcascade_eye, "eye.xml").absolutePath)
        haarcascadeFrontalCatFace = CascadeClassifier(getFileFromResource(R.raw.haarcascade_frontalcatface, "frontalcatface.xml").absolutePath)
        haarcascadeFullBody = CascadeClassifier(getFileFromResource(R.raw.haarcascade_fullbody, "body.xml").absolutePath)
        haarcascadeSmile = CascadeClassifier(getFileFromResource(R.raw.haarcascade_smile, "smile.xml").absolutePath)
        haarcascadeFrontalFace= CascadeClassifier(getFileFromResource(R.raw.haarcascade_frontalface_default, "faces.xml").absolutePath)

        cameraView = JavaCameraView(this, JavaCameraView.CAMERA_ID_BACK)
        cameraView?.let {
            it.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            it.id = View.generateViewId()
            it.setCvCameraViewListener(this)
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        relativeLayout {
            addView(cameraView)
            textMode = textView {
                textResource = R.string.mode1
                textAlignment = TextView.TEXT_ALIGNMENT_CENTER
                setOnClickListener {
                    mode++
                    if(mode == 3){
                        mode = 1
                    }
                    textResource = when(mode){
                        2 -> R.string.mode2
                        else -> R.string.mode1
                    }
                }
            }.lparams(width = matchParent){
                alignParentBottom()
            }
            textMode.bringToFront()
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

            var scale = if(mode == 1) 0.15 else 0.25
            Imgproc.resize(gray, gray, Size(gray.cols() * scale, gray.rows() * scale), 0.0, 0.0, Imgproc.INTER_LINEAR)
            scale = 1 / scale

            if(mode == 1){

                haarcascadeFrontalCatFace.detectMultiScale(gray, cats, 1.1, 3)
                drawMatOfRect(original, cats, faceColor, 5, scale, 0, 0)

                haarcascadeFrontalFace.detectMultiScale(gray, faces, 1.1, 3)
                drawMatOfRect(original, faces, faceColor, 5, scale, 0, 0)
                for(f in 0 until faces.rows()){
                    val face = faces.get(f, 0)
                    if(face != null){
                        val rect = Rect(Point(face[0], face[1]), Point(face[0] + face[2], face[1] + face[3]))
                        val sub = gray.submat(rect)
                        haarcascadeEye.detectMultiScale(sub, eyes, 1.2)
                        haarcascadeSmile.detectMultiScale(sub, smiles, 1.3)
                        drawMatOfRect(original, eyes, eyesColor, 2, scale, rect.x, rect.y)
                        drawMatOfRect(original, smiles, smileColor, 2, scale, rect.x, rect.y)
                    }
                }

            }else{

                haarcascadeFullBody.detectMultiScale(gray, bodies, 1.1)
                drawMatOfRect(original, bodies, bodyColor, 3, scale, 0, 0)

            }

            return original
        }
        return null
    }

    private fun drawMatOfRect(img: Mat, rect: MatOfRect, color: Scalar, thickness: Int = 2, factor : Double = 1.0, offsetX: Int = 0, offsetY: Int = 0){
        for(i in 0 until rect.rows()){
            val target = rect.get(i, 0)
            if(target != null){
                Imgproc.rectangle(img,
                        Point((offsetX + target[0]) * factor,(offsetY + target[1])  * factor),
                        Point((offsetX + target[0] + target[2]) * factor,(offsetY + target[1] + target[3]) * factor),
                        color, thickness)
            }
        }
    }


    override fun onCameraViewStarted(width: Int, height: Int) {

    }

    override fun onCameraViewStopped() {

    }

}