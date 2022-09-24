package com.example.diaryapp

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.VolleyLog
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

private const val ADDDIARYURL = "https://gooddiary.000webhostapp.com/api/diary/create.php"
private const val PARAM_USERNAME="username"
private const val PARAM_SUBJECT="subject"
private const val PARAM_DIARYDATE="diarydate"
private const val PARAM_DIARYTIME="diarytime"
private const val PARAM_LOCATION="location"
private const val PARAM_CONTENT="content"
private const val PARAM_EMOTIONALRESULT="emotionalresult"
private const val RESPONSE_SUCCESS_KEY="success"


class AddTimeLocationDiary : AppCompatActivity() {
    private lateinit var diarydate: EditText
    private lateinit var diarytime: EditText
    private lateinit var etLocation: EditText
    private lateinit var etsubject: EditText
    private lateinit var etcontent: EditText
    private lateinit var tvEmotionalResult: TextView
    private lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_time_location_diary)
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        username = intent.getStringExtra("username").toString()
        val calender = Calendar.getInstance()
        diarydate = findViewById(R.id.etDate)
        diarytime = findViewById(R.id.ettime)
        etLocation = findViewById(R.id.etlocation)
        etsubject = findViewById(R.id.etSubject)
        etcontent = findViewById(R.id.etContent)
        tvEmotionalResult = findViewById(R.id.tvemotional)
        val addbtn: Button = findViewById(R.id.btAdd)

        if (savedInstanceState==null){
            updateDate(calender)
            updateTime(calender)
        }

        val resultLauncher = registerForActivityResult(StartActivityForResult()){ result ->
            if(result.resultCode == Activity.RESULT_OK){
                val data = result.data
                val backresult = data?.getStringExtra("location")
                Log.i("", "Response address is: $backresult")
                etLocation.setText(backresult)

            }
        }

        val date = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            calender.set(Calendar.YEAR, year)
            calender.set(Calendar.MONTH, month)
            calender.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDate(calender)
        }

        val time = TimePickerDialog.OnTimeSetListener { view, hourofDay, minute ->
            calender.set(Calendar.HOUR_OF_DAY, hourofDay)
            calender.set(Calendar.MINUTE, minute)
            updateTime(calender)

        }

        diarydate.setOnClickListener {
            DatePickerDialog(this, date, calender.get(Calendar.YEAR),calender.get(Calendar.MONTH),calender.get(
                Calendar.DAY_OF_MONTH)).show()
        }

        diarytime.setOnClickListener{
            TimePickerDialog(this, time, calender.get(Calendar.HOUR_OF_DAY), calender.get(Calendar.MINUTE),
                true).show()
        }

        etLocation.setOnClickListener{
            val intent =Intent(this,LocationMapsActivity::class.java)
            intent.putExtra("location", etLocation.text.toString())
            resultLauncher.launch(intent)
        }

        addbtn.setOnClickListener {
            requestAddDiary()
        }

    }

    override fun onBackPressed() {
        //super.onBackPressed()
        finish()
    }

    private fun requestAddDiary(){
        val format="dd-MM-yyyy"
        val format2="yyyy-MM-dd"
        val sdf = SimpleDateFormat(format,Locale.UK)
        val sdf2 = SimpleDateFormat(format2,Locale.UK)
        val date =sdf.parse(diarydate.text.toString())

        val timeformat= "hh:mm a"
        val timeformat2="hh:mm:ss"
        val timesdf = SimpleDateFormat(timeformat,Locale.UK)
        val timesdf2 = SimpleDateFormat(timeformat2,Locale.UK)
        val time =timesdf.parse(diarytime.text.toString())

        val requestQueue = Volley.newRequestQueue(this)
        val addDiaryURL = Uri.parse(ADDDIARYURL).buildUpon().appendQueryParameter(PARAM_USERNAME,username).appendQueryParameter(
            PARAM_DIARYDATE,sdf2.format(date as Date)).appendQueryParameter(PARAM_DIARYTIME,timesdf2.format(time as Date))
            .appendQueryParameter(PARAM_LOCATION,etLocation.text.toString()).appendQueryParameter(PARAM_SUBJECT,etsubject.text.toString())
            .appendQueryParameter(PARAM_CONTENT,etcontent.text.toString()).appendQueryParameter(PARAM_EMOTIONALRESULT,tvEmotionalResult.text.toString()).build()
        Log.i(VolleyLog.TAG,"Add Diary URL:$addDiaryURL")
        val addDiaryRequest = StringRequest(Request.Method.POST,addDiaryURL.toString(),{ response->
            try{
                val jsonResponseObject = JSONObject(response)
                val responseSuccess = jsonResponseObject.getString(RESPONSE_SUCCESS_KEY)
                Log.i(VolleyLog.TAG, "Response Message is: $responseSuccess")
                if (responseSuccess == "1"){
                    Toast.makeText(this, "Successful added.", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }else{
                    Toast.makeText(this, "Failed add diary.", Toast.LENGTH_SHORT)
                        .show()
                }

            }catch (e: JSONException){
                e.printStackTrace()
                Log.e(VolleyLog.TAG,"PARSING ERROR:${e.message}")
                Toast.makeText(this, "Failed add diary.", Toast.LENGTH_SHORT)
                    .show()
            }
        },{error->
            Log.e(VolleyLog.TAG,error.message?:"No Error message")
            Toast.makeText(this, "Failed add diary.", Toast.LENGTH_SHORT)
                .show()
        })
        requestQueue.add(addDiaryRequest)
    }

    private fun updateTime(myCalender:Calendar){
        val format="hh:mm a"
        val sdf = SimpleDateFormat(format,Locale.UK)
        diarytime.setText(sdf.format(myCalender.time))
    }

    private fun updateDate(myCalender:Calendar){
        val format="dd-MM-yyyy"
        val sdf = SimpleDateFormat(format,Locale.UK)
        diarydate.setText(sdf.format(myCalender.time))
    }

}