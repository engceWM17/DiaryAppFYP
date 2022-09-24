package com.example.diaryapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.VolleyLog
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.*
import java.util.*
import org.opencv.android.BaseLoaderCallback as BaseLoaderCallback

private const val UPDATEUSERURL = "https://gooddiary.000webhostapp.com/api/user/updatefacevalue.php"
private const val PARAM_USERNAME="username"
private const val PARAM_FACEVALUE="facevalue"
private const val RESPONSE_SUCCESS_KEY="success"

class CameraActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private val TAG: String = "MainActivity"
    private lateinit var username:String
    private lateinit var mRgba: Mat
    private lateinit var mGrey: Mat
    private lateinit var mOpenCVCameraView: CameraBridgeViewBase
    private lateinit var setupfacebtn: Button
    private lateinit var facesCropMat: Array<Rect>
    private lateinit var cascadeClassifier: CascadeClassifier
    private lateinit var faceRecognition: face_Recognition
    private var faceValue= 0.0f

    private val mLoaderCallback = object: BaseLoaderCallback(this){
        override fun onManagerConnected(status: Int) {
            when(status){
                LoaderCallbackInterface.SUCCESS ->{
                    Log.i(TAG, "OpenCv is loaded")
                    mOpenCVCameraView.enableView()
                }
                else ->{
                    super.onManagerConnected(status)
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val MY_PERMISSION_REQUEST = 0
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)== PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA),MY_PERMISSION_REQUEST)
        }

        setContentView(R.layout.activity_camera)
        username= intent.getStringExtra("username").toString()

        mOpenCVCameraView = findViewById(R.id.frame_surface) as CameraBridgeViewBase
        mOpenCVCameraView.visibility = SurfaceView.VISIBLE
        mOpenCVCameraView.setCvCameraViewListener(this)

        setupfacebtn = findViewById(R.id.setupfacebtn) as Button
        setupfacebtn.setOnClickListener {
            //save image
            if(faceValue!=0.0f) {
                updatefaceid()
            }else{
                Toast.makeText(applicationContext, "Faces set up Failed.", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        try{
            faceRecognition= face_Recognition(assets,this,"efficientnet.tflite", input_size = 96)
        }catch (e:IOException){
            e.printStackTrace()
        }

    }

    override fun onResume() {
        super.onResume()
        if(OpenCVLoader.initDebug()){
            Log.d(TAG, "OpenCV initiation is done")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
        else{
            Log.d(TAG, "OpenCV is not loaded, try again")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this,mLoaderCallback)
        }
    }

    override fun onPause() {
        super.onPause()
        mOpenCVCameraView.disableView()
    }
    override fun onDestroy() {
        super.onDestroy()
        mOpenCVCameraView.disableView()
    }

    override fun onCameraViewStarted(width:Int, height:Int){
        mRgba = Mat(height,width, CvType.CV_8UC4)
        mGrey = Mat(height,width, CvType.CV_8UC1)
    }

    override fun onCameraViewStopped() {
        mRgba.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        mRgba = inputFrame!!.rgba()
        mGrey = inputFrame.gray()
        Core.flip(mRgba, mRgba, 1)
        //in processing pass mRgba to cascaderec class
        //mRgba = CascadeRec(mRgba)
        val (rgb, fv) = faceRecognition.recognizeImage(mRgba)
        mRgba= rgb
        faceValue= fv

        return  mRgba
    }

    private fun CascadeRec(mRgba:Mat):Mat{
        //original frame is -90 degree so we have to rotate is to get proper face for detection
        //Core.flip(mRgba.t(), mRgba,1)
        //convert it to RBG
        val mRgb= Mat()
        Imgproc.cvtColor(mRgba,mRgb,Imgproc.COLOR_RGB2BGR)

        val height:Int = mRgb.height()
        val absoluteFaceSize = (height*0.1)

        val faces = MatOfRect()// store output
        //cascadeClassifier.detectMultiScale(mRgb, faces, 1.1,2,2, Size(absoluteFaceSize,absoluteFaceSize), Size())
        cascadeClassifier.detectMultiScale(mRgb, faces)
        val facesArray = faces.toArray()

        for(rect in faces.toArray()){
            //draw face on original fame mRgba
            //Imgproc.rectangle(mRgba,facesArray[i].tl(),facesArray[i].br(), Scalar(0.0,255.0,0.0,255.0),2)
            Imgproc.rectangle(
                mRgba,Point((rect.x).toDouble(), (rect.y).toDouble()),
                Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble()),
                Scalar(255.0,0.0,0.0)
            )
        }

        if(facesArray.size==1){
            facesCropMat = arrayOf(Rect(facesArray[0].x,facesArray[0].y,facesArray[0].height,facesArray[0].width))
        }
        else{
            facesCropMat= emptyArray()
        }

        //rotate back original frame
        //Core.flip(mRgba.t(),mRgba,0)
        return mRgba
    }

    private fun updatefaceid(){
        val requestQueue = Volley.newRequestQueue(this)
        val updatefacevalueURL = Uri.parse(UPDATEUSERURL).buildUpon().appendQueryParameter(PARAM_USERNAME,username)
            .appendQueryParameter(PARAM_FACEVALUE,faceValue.toString()).build()
        Log.i(VolleyLog.TAG,"Request Edit User Profile URL:$updatefacevalueURL")

        val UpdateProfileRequest = StringRequest(Request.Method.POST,updatefacevalueURL.toString(),{ response->
            try{
                val jsonResponseObject = JSONObject(response)
                val responseSuccess = jsonResponseObject.getString(RESPONSE_SUCCESS_KEY)
                Log.i(VolleyLog.TAG, "Response Sucess is: $responseSuccess")
                if (responseSuccess=="1"){
                    Toast.makeText(this, "Face Set up success.", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }else{
                    Toast.makeText(this, "Face Set up failed.", Toast.LENGTH_SHORT)
                        .show()
                }

            }catch (e: JSONException){
                e.printStackTrace()
                Log.e(VolleyLog.TAG,"PARSING ERROR:${e.message}")
                Toast.makeText(this, "Face Set up failed", Toast.LENGTH_SHORT)
                    .show()
            }
        },{error->
            Log.e(VolleyLog.TAG,error.message?:"No Error message")
            Toast.makeText(this, "Face Set up failed", Toast.LENGTH_SHORT)
                .show()
        })
        requestQueue.add(UpdateProfileRequest)
    }
}