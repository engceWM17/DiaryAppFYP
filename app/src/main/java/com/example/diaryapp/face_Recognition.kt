package com.example.diaryapp

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.content.res.AssetManager
import android.graphics.Bitmap
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.CascadeClassifier
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.io.*
import java.lang.reflect.Array
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class face_Recognition
    (assetManager: AssetManager, context: Context, modelpath: String, input_size: Int) {
    //import interpreter
    private var interpreter: Interpreter
    private var height = 0
    private var width = 0
    private var cascadeClassifier: CascadeClassifier= CascadeClassifier()
    private val TAG="face_recognition"
    private var INPUT_SIZE: Int
    private val compatList = CompatibilityList()

    init {
        INPUT_SIZE = input_size
        val options= Interpreter.Options().apply{
            if(compatList.isDelegateSupportedOnThisDevice){
                val delegatOptions=compatList.bestOptionsForThisDevice
                this.addDelegate(GpuDelegate(delegatOptions))
            }else{
                this.setNumThreads(4)
            }
        }

        interpreter= Interpreter(loadModel(assetManager,modelpath),options)
        try {
            val mis: InputStream = context.resources.openRawResource(R.raw.haarcascade_frontalface_alt2)
            val cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE)
            val mCascadeFile = File(cascadeDir, "haarcascade_frontalface_alt2.xml")

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
        }
    }

    fun recognizeImage(mat_Image: Mat): Pair <Mat,Float>{
        //original frame is -90 degree so we have to rotate is to get proper face for detection
        Core.flip(mat_Image.t(), mat_Image, 1)
        //convert it to RBG
        val mRgb= Mat()
        Imgproc.cvtColor(mat_Image, mRgb, Imgproc.COLOR_RGBA2GRAY)
        height =mRgb.height()
        width = mRgb.width()

        val faces = MatOfRect()// store output

        cascadeClassifier!!.detectMultiScale(mRgb, faces)
        Log.d(TAG, "face to Array")
        val face_value= Array(1){FloatArray(1)}
        var facevaluef=0.0f
        var faceslength=0

        for (rect in faces.toArray()) {
            //draw face on original frame mRgba
            Imgproc.rectangle(
                mat_Image, Point((rect.x).toDouble(), (rect.y).toDouble()),
                Point((rect.x + rect.width).toDouble(), (rect.y + rect.height).toDouble()),
                Scalar(255.0, 0.0, 0.0)
            )

            val roi = Rect(rect.x, rect.y,(rect.width), (rect.height))
            val cropped_rgb = Mat(mat_Image, roi)
            var bitmap: Bitmap

            bitmap= Bitmap.createBitmap(cropped_rgb.cols(),cropped_rgb.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(cropped_rgb, bitmap)

            val scaledBitmap = Bitmap.createScaledBitmap(bitmap,INPUT_SIZE,INPUT_SIZE,false)

            val bytebuffer:ByteBuffer=convertBitmapToByteBuffer(scaledBitmap)
            interpreter.run(bytebuffer,face_value)

            Log.d(TAG,"Out: "+ Array.get(Array.get(face_value,0)!!,0))
            faceslength=faceslength+1
            // based on the value declared face is found or not inside database.
        }

        if (faceslength==1){
            facevaluef=Array.get(Array.get(face_value,0)!!,0)?.toString()!!.toFloat()
        }

        //rotate back original frame
        Core.flip(mat_Image.t(), mat_Image, 0)

        return Pair<Mat,Float>(mat_Image, facevaluef)
    }

    private fun convertBitmapToByteBuffer(scaledBitmap: Bitmap?): ByteBuffer {
        val byteBuffer:ByteBuffer
        byteBuffer = ByteBuffer.allocateDirect(4*1*INPUT_SIZE*INPUT_SIZE*3)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(INPUT_SIZE*INPUT_SIZE)
        scaledBitmap!!.getPixels(intValues,0,scaledBitmap.width,0,0,scaledBitmap.width,scaledBitmap.height)

        for(i in intValues){
            byteBuffer.putFloat(((i shr 16 )and 0xff)/255.0f)
            byteBuffer.putFloat(((i shr 8 )and 0xff)/255.0f)
            byteBuffer.putFloat((i and 0xff)/255.0f)
        }

        return byteBuffer
    }

    private fun loadModel(assetManager: AssetManager, modelpath: String): MappedByteBuffer {
        val assetFileDiscriptor: AssetFileDescriptor = assetManager.openFd(modelpath)
        val inputStream = FileInputStream(assetFileDiscriptor.fileDescriptor)
        val fileChannel:FileChannel = inputStream.channel
        val startOffset = assetFileDiscriptor.startOffset
        val declaredLength = assetFileDiscriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startOffset,declaredLength)
    }

}