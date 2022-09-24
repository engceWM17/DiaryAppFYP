package com.example.diaryapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.VolleyLog
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import java.io.*

private const val SIGNINURL = "https://gooddiary.000webhostapp.com/api/user/signinbyface.php"
private const val PARAM_USERNAME="username"
private const val PARAM_FACEVALUE="facevalue"
private const val RESPONSE_STATUS_KEY="status"
private const val RESPONSE_MESSAGE_KEY="message"
private const val RESPONSE_USERNAME_KEY="username"

class CameraActivity2 : AppCompatActivity() , CameraBridgeViewBase.CvCameraViewListener2 {
    private val TAG: String = "MainActivity"
    private lateinit var mRgba: Mat
    private lateinit var mGrey: Mat
    private lateinit var mOpenCVCameraView: CameraBridgeViewBase
    private lateinit var faceRecognition: face_Recognition
    private var cascadeClassifier: CascadeClassifier = CascadeClassifier()
    private lateinit var mCascadeFile: File
    private lateinit var username:String

    private val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV loaded successfully")
                    /*
                    try {
                        val mis: InputStream = resources.openRawResource(R.raw.haarcascade_frontalface_alt2)
                        val cascadeDir = getDir("cascade", Context.MODE_PRIVATE) as File
                        mCascadeFile = File(cascadeDir, "haarcascade_frontalface_alt2.xml")

                        /*//writing that file from raw folder
                        mis.bufferedReader().useLines {
                            it.map { line ->
                                mCascadeFile.bufferedWriter().use { out ->
                                    out.append(line)
                                }
                            }
                        }*/
                        //writing that file fromraw folder
                        val os = FileOutputStream(mCascadeFile)
                        val buffer = ByteArray(4096)
                        var byteRead:Int

                        do {
                            byteRead = mis.read(buffer)
                            if(byteRead != -1 ){
                                os.write(buffer,0, byteRead)
                            }
                        } while(byteRead != -1 )

                        mis.close()
                        os.close()


                        //loading file from cascade folder created above
                        cascadeClassifier = CascadeClassifier(mCascadeFile.absolutePath)
                        cascadeClassifier.load(mCascadeFile.absolutePath)
                        //model loaded
                        if (cascadeClassifier.empty() == true) {
                            Log.e(TAG, "Failed to load cascade classifier")
                        } else {
                            Log.i(
                                TAG,
                                "Loaded cascade classifier from " + mCascadeFile.absolutePath
                            )
                        }
                        cascadeDir.delete()
                        Log.i(TAG, "Cascade file found.")

                    } catch (e: IOException) {
                        Log.i(TAG, "Cascade file not found.")
                    }*/
                    mOpenCVCameraView.enableView()
                }
                else -> {
                    super.onManagerConnected(status)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "called onCreate")
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_camera2)

        username= intent.getStringExtra("username").toString()

        val MY_PERMISSION_REQUEST = 0
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_DENIED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                MY_PERMISSION_REQUEST
            )
        }

        mOpenCVCameraView = findViewById(R.id.frame_surface) as CameraBridgeViewBase
        mOpenCVCameraView.visibility = SurfaceView.VISIBLE
        mOpenCVCameraView.setCvCameraViewListener(this)
        // mOpenCVCameraView.enableFpsMeter()
        try{
            faceRecognition= face_Recognition(assets,this,"efficientnet.tflite", input_size = 96)
        }catch (e:IOException){
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV initiation is done")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        } else {
            Log.d(TAG, "OpenCV is not loaded, try again")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback)
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

    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba = Mat()
        mGrey = Mat()
    }

    override fun onCameraViewStopped() {
        mRgba.release()
        mGrey.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        mRgba = inputFrame!!.rgba()
        mGrey = inputFrame.gray()
        Core.flip(mRgba, mRgba, 1)

        //mRgba = CascadeRec(mRgba)
        val (rgb, fv) = faceRecognition.recognizeImage(mRgba)
        mRgba= rgb
        if (fv!=0.0f){
            signinbyface(fv)
        }

        return mRgba
    }

    private fun CascadeRec(mRgba: Mat): Mat {
        //original frame is -90 degree so we have to rotate is to get proper face for detection
        Core.flip(mRgba.t(), mRgba, 0)
        //convert it to RBG
        val mRgb= Mat()
        Imgproc.cvtColor(mRgba, mRgb, Imgproc.COLOR_RGB2BGR)

        val faces = MatOfRect()// store output
        cascadeClassifier.detectMultiScale(mRgb, faces)
        Log.d(TAG, "face to Array")

        for (rect in faces.toArray()) {
            //draw face on original frame mRgba
            Imgproc.rectangle(
                mRgba,Point((rect.x).toDouble(), (rect.y).toDouble()),
                Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble()),
                Scalar(255.0,0.0,0.0)
            )
        }

        //rotate back original frame
        Core.flip(mRgba.t(), mRgba, 1)

        return mRgba
    }

    private fun signinbyface(faceValue:Float){
        val requestQueue = Volley.newRequestQueue(this)
        val signInURL = Uri.parse(SIGNINURL).buildUpon().appendQueryParameter(PARAM_USERNAME,username).appendQueryParameter(
            PARAM_FACEVALUE,faceValue.toString()).build()
        Log.i(VolleyLog.TAG,"Sign In URL:$signInURL")

        val signInRequest = StringRequest(Request.Method.POST,signInURL.toString(),{ response->
            try{
                val jsonResponseObject = JSONObject(response)
                val response_status = jsonResponseObject.getString(RESPONSE_STATUS_KEY)
                Log.i(VolleyLog.TAG,"Response Status is: $response_status")
                val response_message = jsonResponseObject.getString(RESPONSE_MESSAGE_KEY)
                Log.i(VolleyLog.TAG, "Response Message is: $response_message")
                val response_username = jsonResponseObject.getString(RESPONSE_USERNAME_KEY)
                Log.i(VolleyLog.TAG, "Response Message is: $response_username")
                if (response_status=="1"){
                    val intent = Intent(this,MainActivity::class.java)
                    intent.putExtra("username",response_username)
                    finish()
                    startActivity(intent)
                }
            }catch (e: JSONException){
                e.printStackTrace()
                Log.e(VolleyLog.TAG,"PARSING ERROR:${e.message}")
            }
        },{error->
            Log.e(VolleyLog.TAG,error.message?:"No Error message")
        })
        requestQueue.add(signInRequest)

    }
}