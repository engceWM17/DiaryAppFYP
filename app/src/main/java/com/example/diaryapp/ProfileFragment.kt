package com.example.diaryapp

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.android.volley.Request
import com.android.volley.VolleyLog
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.switchmaterial.SwitchMaterial
import org.json.JSONException
import org.json.JSONObject
import org.opencv.android.OpenCVLoader

private const val READUSERURL = "https://gooddiary.000webhostapp.com/api/user/read_one.php"
private const val UPDATEUSERURL = "https://gooddiary.000webhostapp.com/api/user/update.php"
private const val PARAM_USERNAME="username"
private const val RESPONSE_MESSAGE_KEY="message"
private const val RESPONSE_USERNAME="username"
private const val RESPONSE_EMAIL="email"
private const val RESPONSE_USEDFACEID="usedfaceid"
private const val RESPONSE_SUCCESS_KEY="success"

class ProfileFragment : Fragment() {
    private val TAG: String = "MainActivity"
    private lateinit var username: String
    private lateinit var edUsername: EditText
    private lateinit var edEmail: EditText
    private lateinit var switchsusedfaceid: SwitchMaterial
    private lateinit var v:View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_profile, container, false)
        val data= arguments as Bundle
        val editbtn = v.findViewById<View>(R.id.editbtn) as Button
        val setupbtn = v.findViewById<View>(R.id.setupfaceid) as Button
        val logoutbtn = v.findViewById<View>(R.id.logoutbtn) as Button
        edUsername = v.findViewById(R.id.usernameprofile)
        username = data.getString("username").toString()
        edEmail = v.findViewById(R.id.emailprofile)
        switchsusedfaceid = v.findViewById(R.id.switchfaceid)
        if (savedInstanceState == null) {
            username.let { requestUserProfile(it) }
        }

        editbtn.setOnClickListener {
            editbtn.requestFocus()
            requestEditProfile(username, edEmail.text.toString(),switchsusedfaceid.isChecked )
        }

        setupbtn.setOnClickListener {
            if (OpenCVLoader.initDebug()){
                val intent = Intent(activity,CameraActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent.putExtra("username",username)
                startActivity(intent)
            }else{
                Log.d(TAG,"Unable to open camera")
            }
        }

        switchsusedfaceid.setOnCheckedChangeListener { bottonView, ischecked ->
            if (ischecked){
                setupbtn.visibility=View.VISIBLE
            }else{
                setupbtn.visibility=View.INVISIBLE
            }
        }

        logoutbtn.setOnClickListener {
            val intent = Intent(v.context,LoginActivity::class.java)
            activity?.finish()
            startActivity(intent)
        }

        return v
    }

    private fun requestEditProfile(paramusername:String, email:String, usedfaceid:Boolean){
        var usedfaceidstr:String
        if (usedfaceid){
            usedfaceidstr = "Y"
        }else{
            usedfaceidstr = "N"
        }
        val requestQueue = Volley.newRequestQueue(v.context)
        val updateprofileURL = Uri.parse(UPDATEUSERURL).buildUpon().appendQueryParameter(PARAM_USERNAME,paramusername).
        appendQueryParameter(RESPONSE_EMAIL,email).appendQueryParameter(RESPONSE_USEDFACEID,usedfaceidstr).build()
        Log.i(VolleyLog.TAG,"Request Edit User Profile URL:$updateprofileURL")

        val UpdateProfileRequest = StringRequest(Request.Method.POST,updateprofileURL.toString(),{ response->
            try{
                val jsonResponseObject = JSONObject(response)
                val responseSuccess = jsonResponseObject.getString(RESPONSE_SUCCESS_KEY)
                Log.i(VolleyLog.TAG, "Response Sucess is: $responseSuccess")
                if (responseSuccess=="1"){
                    Toast.makeText(v.context, "Profile Updated", Toast.LENGTH_SHORT)
                        .show()
                }else{
                    Toast.makeText(v.context, "Profile Update failed.", Toast.LENGTH_SHORT)
                        .show()
                }

            }catch (e: JSONException){
                e.printStackTrace()
                Log.e(VolleyLog.TAG,"PARSING ERROR:${e.message}")
                Toast.makeText(v.context, "Profile Update failed", Toast.LENGTH_SHORT)
                    .show()
            }
        },{error->
            Log.e(VolleyLog.TAG,error.message?:"No Error message")
            Toast.makeText(v.context, "Profile Update failed", Toast.LENGTH_SHORT)
                .show()
        })
        requestQueue.add(UpdateProfileRequest)
    }

    private fun requestUserProfile(username:String){
        val requestQueue = Volley.newRequestQueue(v.context)
        val profileURL = Uri.parse(READUSERURL).buildUpon().appendQueryParameter(PARAM_USERNAME,username).build()
        Log.i(VolleyLog.TAG,"Request User Profile URL:$profileURL")

        val getProfileRequest = StringRequest(Request.Method.POST,profileURL.toString(),{ response->
            try{
                val jsonResponseObject = JSONObject(response)

                if (jsonResponseObject.has(RESPONSE_MESSAGE_KEY)){
                    val response_message = jsonResponseObject.getString(RESPONSE_MESSAGE_KEY)
                    Log.i(VolleyLog.TAG, "Response Message is: $response_message")
                    Toast.makeText(v.context, response_message, Toast.LENGTH_SHORT)
                        .show()
                }
                else {
                    val responseUsername = jsonResponseObject.getString(RESPONSE_USERNAME)
                    Log.i(VolleyLog.TAG, "Response Username is: $responseUsername")
                    val responseEmail = jsonResponseObject.getString(RESPONSE_EMAIL)
                    Log.i(VolleyLog.TAG, "Response Email is: $responseEmail")
                    val responseUsedFaceID = jsonResponseObject.getString(RESPONSE_USEDFACEID)
                    Log.i(VolleyLog.TAG, "Response UsedFaceID is: $responseUsedFaceID")

                    edUsername.setText(responseUsername)
                    if (responseEmail!="null"){
                        edEmail.setText(responseEmail)
                    }
                    switchsusedfaceid.isChecked = (responseUsedFaceID=="Y")
                }
            }catch (e: JSONException){
                e.printStackTrace()
                Log.e(VolleyLog.TAG,"PARSING ERROR:${e.message}")
                Toast.makeText(v.context, "Loading failed.", Toast.LENGTH_SHORT)
                    .show()
            }
        },{error->
            Log.e(VolleyLog.TAG,error.message?:"No Error message")
            Toast.makeText(v.context, "Loading failed.", Toast.LENGTH_SHORT)
                .show()
        })
        requestQueue.add(getProfileRequest)
    }
}