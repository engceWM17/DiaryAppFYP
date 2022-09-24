package com.example.diaryapp

import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.VolleyLog.TAG
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import org.opencv.android.OpenCVLoader

private const val SIGNINURL = "https://gooddiary.000webhostapp.com/api/user/sign-in.php"
private const val VERIFYUSERURL = "https://gooddiary.000webhostapp.com/api/user/verifyuser.php"
private const val PARAM_USERNAME="username"
private const val PARAM_PASSWORD="password"
private const val RESPONSE_STATUS_KEY="status"
private const val RESPONSE_MESSAGE_KEY="message"
private const val RESPONSE_USERNAME_KEY="username"

class LoginActivity : AppCompatActivity() {

    private var cancellationSignal:CancellationSignal? = null
    private lateinit var username: EditText
    private var result :Boolean = false
    private lateinit var fingerprintbtn: Button
    private val authenticationCallback:BiometricPrompt.AuthenticationCallback
    get()=
        @RequiresApi(Build.VERSION_CODES.P)
        object :BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                super.onAuthenticationError(errorCode, errString)
                notifyUser("Authentication Error: $errString")
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                super.onAuthenticationSucceeded(result)
                notifyUser("Authentication success.")
                val intent = Intent(this@LoginActivity, MainActivity::class.java)
                intent.putExtra("username", username.text.toString())
                finish()
                startActivity(intent)

            }
        }


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        fingerprintbtn = findViewById<View>(R.id.fingerprintbtn) as Button
        username = findViewById<View>(R.id.username) as EditText
        val password = findViewById<View>(R.id.password) as EditText
        val loginbtn = findViewById<View>(R.id.loginbtn) as Button
        val loginwihfacebtn = findViewById<View>(R.id.loginwithfacebtn) as Button

        val gosignup = findViewById<View>(R.id.registernew) as TextView

        loginbtn.setOnClickListener {
            if (username.text.toString().isNotEmpty() && password.text.toString().isNotEmpty()) {
                val inputUsername: String = username.text.toString()
                val inputPassword: String = password.text.toString()
                requestSignIn(inputUsername, inputPassword)
            } else {
                Toast.makeText(applicationContext, "Username & Password cannot be blank.", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        loginwihfacebtn.setOnClickListener {
            if (OpenCVLoader.initDebug() ){
                if(username.text.toString()!=""){
                    val intent = Intent(
                        this,
                        CameraActivity2::class.java
                    ).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent.putExtra("username", username.text.toString())
                    startActivity(intent)
                }else{
                    Toast.makeText(applicationContext, "Please enter the username first.", Toast.LENGTH_SHORT)
                        .show()
                }
            }else{
                Log.d(TAG,"Unable to open camera")
            }
        }

        gosignup.setOnClickListener{
            redirectToSignUp()
        }

        //check biometric support
        checkBiometricSupport()

        fingerprintbtn.setOnClickListener {
            if ( username.text.toString()=="") {
                Toast.makeText(applicationContext, "Please enter the username first.", Toast.LENGTH_SHORT)
                    .show()
            }else {

                val biometricPrompt = BiometricPrompt.Builder(this)
                    .setTitle("Login Diary App")
                    .setDescription("This app use fingerprint protection to keep your data secure")
                    .setNegativeButton(
                        "Cancel",
                        this.mainExecutor,
                        DialogInterface.OnClickListener { dialog, which ->
                            notifyUser("Authentication cancelled")
                        }).build()

                biometricPrompt.authenticate(
                    getCancellationSignal(),
                    mainExecutor,
                    authenticationCallback
                )

            }
        }

    }

    private fun getCancellationSignal():CancellationSignal{
        cancellationSignal = CancellationSignal()
        cancellationSignal?.setOnCancelListener {
            notifyUser("Authentication is cancelled")
        }
        return cancellationSignal as CancellationSignal
    }

    private fun checkBiometricSupport(): Boolean {
        val keyguardManager:KeyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        if (!keyguardManager.isKeyguardSecure){
            notifyUser("Fingerprint authernticaton has been enabled in settings")
            return false
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.USE_BIOMETRIC) != PackageManager.PERMISSION_GRANTED){
            notifyUser("Fingerprint authernticaton permission is not enabled")
            return false
        }

        return if(packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)){
            true
        }else true

    }

    private fun redirectToSignUp(){
        val intent =Intent(this,Register::class.java)
        finish()
        startActivity(intent)
    }

    private fun requestSignIn(username:String, password:String){
        val requestQueue = Volley.newRequestQueue(this)
        val signInURL = Uri.parse(SIGNINURL).buildUpon().appendQueryParameter(PARAM_USERNAME,username).appendQueryParameter(
            PARAM_PASSWORD,password).build()
        Log.i(TAG,"Sign In URL:$signInURL")

        val signInRequest = StringRequest(Request.Method.POST,signInURL.toString(),{ response->
            try{
                val jsonResponseObject =JSONObject(response)
                val response_status = jsonResponseObject.getString(RESPONSE_STATUS_KEY)
                Log.i(TAG,"Response Status is: $response_status")
                val response_message = jsonResponseObject.getString(RESPONSE_MESSAGE_KEY)
                Log.i(TAG, "Response Message is: $response_message")
                val response_username = jsonResponseObject.getString(RESPONSE_USERNAME_KEY)
                Log.i(TAG, "Response Message is: $response_username")
                if (response_status=="1"){
                    val intent =Intent(this,MainActivity::class.java)
                    intent.putExtra("username",response_username)
                    finish()
                    startActivity(intent)
                } else{
                    Toast.makeText(applicationContext, "Username Or Password is incorrect.", Toast.LENGTH_SHORT)
                        .show()
                }
            }catch (e:JSONException){
                e.printStackTrace()
                Log.e(TAG,"PARSING ERROR:${e.message}")
            }
        },{error->
            Log.e(TAG,error.message?:"No Error message")
        })
        requestQueue.add(signInRequest)
    }

    private fun verifyUsername(username:String){
        val requestQueue = Volley.newRequestQueue(this)
        val VerifyUsernameURL = Uri.parse(VERIFYUSERURL).buildUpon().appendQueryParameter(PARAM_USERNAME,username).build()
        Log.i(TAG,"Verify User URL:$VerifyUsernameURL")

        val verifyUserRequest = StringRequest(Request.Method.POST,VerifyUsernameURL.toString(),{ response->
            try{
                val jsonResponseObject =JSONObject(response)
                val response_status = jsonResponseObject.getString(RESPONSE_STATUS_KEY)
                Log.i(TAG,"Response Status is: $response_status")
                val response_message = jsonResponseObject.getString(RESPONSE_MESSAGE_KEY)
                Log.i(TAG, "Response Message is: $response_message")
                val response_username = jsonResponseObject.getString(RESPONSE_USERNAME_KEY)
                Log.i(TAG, "Response Message is: $response_username")
                if (response_status=="1"){
                    result = true

                } else{
                    Toast.makeText(applicationContext, "Username is not found or Biomatric Aunthentication is not enabled.", Toast.LENGTH_SHORT)
                        .show()
                }
            }catch (e:JSONException){
                e.printStackTrace()
                Log.e(TAG,"PARSING ERROR:${e.message}")
            }
        },{error->
            Log.e(TAG,error.message?:"No Error message")
        })

        requestQueue.add(verifyUserRequest)

    }

    private fun notifyUser(message:String){
        Toast.makeText(this,message, Toast.LENGTH_SHORT).show()
    }
}