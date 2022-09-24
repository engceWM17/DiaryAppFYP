package com.example.diaryapp

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import com.android.volley.Request
import com.android.volley.VolleyLog
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

private const val SIGNUPURL = "https://gooddiary.000webhostapp.com/api/user/create.php"
private const val PARAM_USERNAME="username"
private const val PARAM_PASSWORD="password"
private const val PARAM_USEDFACEID="usedfaceid"
private const val RESPONSE_STATUS_KEY="status"
private const val RESPONSE_MESSAGE_KEY="message"
private const val RESPONSE_SUCCESS_KEY="success"

class Register : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val username = findViewById<View>(R.id.usernameSignup) as EditText
        val password1 = findViewById<View>(R.id.passwordSignUp1) as EditText
        val password2 = findViewById<View>(R.id.passwordSignUp2) as EditText
        val registerbtn = findViewById<View>(R.id.registerbtn) as Button
        val gosignin = findViewById<View>(R.id.gosignin) as TextView

        gosignin.setOnClickListener {
            redirectToSignIn()
        }

        registerbtn.setOnClickListener {
            if (username.text.toString().isNotEmpty() && password1.text.toString().isNotEmpty() && password2.text.toString().isNotEmpty()) {
                val inputUsername:String = username.text.toString()
                val inputPassword1:String = password1.text.toString()
                val inputPassword2:String = password2.text.toString()
                if (inputPassword1 == inputPassword2){
                    if (currentFocus!= null) {
                        val inputMethodManager =
                            getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(currentFocus!!.windowToken, 0)
                    }
                    requestSignUp(inputUsername,inputPassword1)
                }else{
                    Toast.makeText(applicationContext, "Password & Confirmed Password is different.", Toast.LENGTH_SHORT)
                        .show()
                    password1.text.clear()
                    password2.text.clear()
                }

            }else{
                Toast.makeText(applicationContext, "Username & Password cannot be blank.", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun redirectToSignIn(){
        val intent =Intent(this,LoginActivity::class.java)
        finish()
        startActivity(intent)
    }

    private fun requestSignUp(username:String, password:String){
        val requestQueue = Volley.newRequestQueue(this)
        val signUpURL = Uri.parse(SIGNUPURL).buildUpon().appendQueryParameter(PARAM_USERNAME,username).appendQueryParameter(
            PARAM_PASSWORD,password).appendQueryParameter(PARAM_USEDFACEID,"N").build()
        Log.i(VolleyLog.TAG,"Sign In URL:$signUpURL")
        val signUpRequest = StringRequest(Request.Method.POST,signUpURL.toString(),{ response->
            try{
                val jsonResponseObject = JSONObject(response)

                if (jsonResponseObject.has(RESPONSE_STATUS_KEY)){
                    val response_status = jsonResponseObject.getString(RESPONSE_STATUS_KEY)
                    Log.i(VolleyLog.TAG,"Response Status is: $response_status")
                    val response_message = jsonResponseObject.getString(RESPONSE_MESSAGE_KEY)
                    Log.i(VolleyLog.TAG, "Response Message is: $response_message")
                    if (response_status=="0"){
                        Toast.makeText(applicationContext, response_message, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                else if (jsonResponseObject.has(RESPONSE_SUCCESS_KEY)){
                    val response_success = jsonResponseObject.getString(RESPONSE_SUCCESS_KEY)
                    Log.i(VolleyLog.TAG, "Response Message is: $response_success")
                    if (response_success == "1"){
                        Toast.makeText(applicationContext, "Registration success.", Toast.LENGTH_SHORT)
                            .show()
                        finish()
                        redirectToSignIn()
                    }else{
                        Toast.makeText(applicationContext, "Registration failed.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

            }catch (e: JSONException){
                e.printStackTrace()
                Log.e(VolleyLog.TAG,"PARSING ERROR:${e.message}")
                Toast.makeText(applicationContext, "Registration failed.", Toast.LENGTH_SHORT)
                    .show()
            }
        },{error->
            Log.e(VolleyLog.TAG,error.message?:"No Error message")
            Toast.makeText(applicationContext, "Registration failed.", Toast.LENGTH_SHORT)
                .show()
        })
        requestQueue.add(signUpRequest)
    }
}