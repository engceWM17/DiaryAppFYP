package com.example.diaryapp

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyLog
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.properties.Delegates

private const val EDITDIARYURL = "https://gooddiary.000webhostapp.com/api/diary/update.php"
private const val DELETEDIARYURL = "https://gooddiary.000webhostapp.com/api/diary/delete.php"
private const val READONEDIARYURL = "https://gooddiary.000webhostapp.com/api/diary/read_one.php"
private const val PARAM_ID="id"
private const val PARAM_USERNAME="username"
private const val PARAM_SUBJECT="subject"
private const val PARAM_DIARYDATE="diarydate"
private const val PARAM_DIARYTIME="diarytime"
private const val PARAM_LOCATION="location"
private const val PARAM_CONTENT="content"
private const val PARAM_EMOTIONALRESULT="emotionalresult"
private const val RESPONSE_SUCCESS_KEY="success"
private const val RESPONSE_MESSAGE_KEY="message"

class SaveDeleteDiary : AppCompatActivity() {
    private lateinit var diarydate: EditText
    private lateinit var diarytime: EditText
    private lateinit var etLocation: EditText
    private lateinit var etsubject: EditText
    private lateinit var etcontent: EditText
    private lateinit var tvEmotionalResult: TextView
    private var id :Int?=null
    private lateinit var calender:Calendar
    private lateinit var requestQueue : RequestQueue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save_delete_diary)
        this.supportActionBar?.setDisplayHomeAsUpEnabled(true)
        requestQueue= Volley.newRequestQueue(this)
        id = intent?.getIntExtra("id",0)
        diarydate = findViewById(R.id.etDate)
        diarytime = findViewById(R.id.ettime)
        etLocation = findViewById(R.id.etlocation)
        etsubject = findViewById(R.id.etSubject)
        etcontent = findViewById(R.id.etContent)
        tvEmotionalResult = findViewById(R.id.tvemotional)
        val savebtn: Button = findViewById(R.id.btEdit)
        val deletebtn: Button = findViewById(R.id.btDelete)
        calender = Calendar.getInstance()
        val builder = AlertDialog.Builder(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            InitiateDiary()
        }
        else{
            finish()
        }

        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == Activity.RESULT_OK){
                val data: Intent? = result.data
                val backresult = result.data?.getStringExtra("location")
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
            DatePickerDialog(this, date, calender.get(Calendar.YEAR),calender.get(Calendar.MONTH),calender.get(Calendar.DAY_OF_MONTH)).show()
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

        savebtn.setOnClickListener {
            builder.setTitle("Comfirmation Alert").setMessage("Confirm to edit diary?").setCancelable(true)
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    requestEditDiary()
                }.setNegativeButton("No"){ dialogInterface, _ ->
                    dialogInterface.cancel()
                }.show()
        }

        deletebtn.setOnClickListener {
            builder.setTitle("Comfirmation Alert").setMessage("Confirm to delete diary?").setCancelable(true)
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    requestDeleteDiary()
                }.setNegativeButton("No"){ dialogInterface, _ ->
                    dialogInterface.cancel()
                }.show()
        }
    }
    override fun onBackPressed() {
        //super.onBackPressed()
        finish()
    }

    private fun requestDeleteDiary(){
        val deleteDiaryURL = Uri.parse(DELETEDIARYURL).buildUpon().appendQueryParameter(PARAM_ID,id.toString()).build()
        Log.i(VolleyLog.TAG,"delete diary URL:$deleteDiaryURL")
        val deleteDiaryRequest = StringRequest(Request.Method.GET,deleteDiaryURL.toString(),{ response->
            try{
                val jsonResponseObject = JSONObject(response)
                val responseSuccess = jsonResponseObject.getString(RESPONSE_SUCCESS_KEY)

                Log.i(VolleyLog.TAG, "Response Message is: $responseSuccess")
                if (responseSuccess == "1"){
                    Toast.makeText(this, "Deletion is success.", Toast.LENGTH_SHORT)
                        .show()
                    setResult(RESULT_OK, intent)
                    finish()
                }else{
                    Toast.makeText(this, "Deletion is failed.", Toast.LENGTH_SHORT)
                        .show()
                }

            }catch (e: JSONException){
                e.printStackTrace()
                Log.e(VolleyLog.TAG,"PARSING ERROR:${e.message}")
                Toast.makeText(this, "Parsing Error: Deletion is failed.", Toast.LENGTH_SHORT)
                    .show()
            }
        },{error->
            Log.e(VolleyLog.TAG,error.message?:"No Error message")
            Toast.makeText(this, "Deletion is failed.", Toast.LENGTH_SHORT)
                .show()
        })
        requestQueue.add(deleteDiaryRequest)
    }

    private fun requestEditDiary(){
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

        val editDiaryURL = Uri.parse(EDITDIARYURL).buildUpon().appendQueryParameter(PARAM_ID,id.toString()).appendQueryParameter(
            PARAM_DIARYDATE,sdf2.format(date as Date)).appendQueryParameter(PARAM_DIARYTIME,timesdf2.format(time as Date))
            .appendQueryParameter(PARAM_LOCATION,etLocation.text.toString()).appendQueryParameter(PARAM_SUBJECT,etsubject.text.toString())
            .appendQueryParameter(PARAM_CONTENT,etcontent.text.toString()).appendQueryParameter(PARAM_EMOTIONALRESULT,tvEmotionalResult.text.toString()).build()
        Log.i(VolleyLog.TAG,"edit diary URL:$editDiaryURL")
        val editDiaryRequest = StringRequest(Request.Method.POST,editDiaryURL.toString(),{ response->
            try{
                val jsonResponseObject = JSONObject(response)
                val responseSuccess = jsonResponseObject.getString(RESPONSE_SUCCESS_KEY)

                Log.i(VolleyLog.TAG, "Response Message is: $responseSuccess")
                if (responseSuccess == "1"){
                    Toast.makeText(this, "Update is success.", Toast.LENGTH_SHORT)
                        .show()
                    setResult(RESULT_OK, intent)
                    finish()
                }else{
                    Toast.makeText(this, "Update is failed.", Toast.LENGTH_SHORT)
                        .show()
                }

            }catch (e: JSONException){
                e.printStackTrace()
                Log.e(VolleyLog.TAG,"PARSING ERROR:${e.message}")
                Toast.makeText(this, "Parsing Error: Update is failed.", Toast.LENGTH_SHORT)
                    .show()
            }
        },{error->
            Log.e(VolleyLog.TAG,error.message?:"No Error message")
            Toast.makeText(this, "Update is failed.", Toast.LENGTH_SHORT)
                .show()
        })
        requestQueue.add(editDiaryRequest)
    }

    private fun InitiateDiary(){
        val readOneDiaryURL = Uri.parse(READONEDIARYURL).buildUpon().appendQueryParameter(PARAM_ID,id.toString()).build()
        Log.i(VolleyLog.TAG,"read diary URL:$readOneDiaryURL")
        val redOneDiaryRequest = StringRequest(Request.Method.GET,readOneDiaryURL.toString(),{ response->
            try{
                val jsonResponseObject = JSONObject(response)

                if (jsonResponseObject.has(RESPONSE_MESSAGE_KEY)){
                    val responseMessage = jsonResponseObject.getString(RESPONSE_MESSAGE_KEY)
                    Toast.makeText(this, responseMessage, Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }else{
                    val responsediarydate = jsonResponseObject.getString(PARAM_DIARYDATE)
                    Log.i(VolleyLog.TAG, "Response Diary date is: $responsediarydate")
                    val responsediarytime = jsonResponseObject.getString(PARAM_DIARYTIME)
                    Log.i(VolleyLog.TAG, "Response Diary time is: $responsediarytime")
                    val responsediarylocation = jsonResponseObject.getString(PARAM_LOCATION)
                    Log.i(VolleyLog.TAG, "Response Diary location is: $responsediarylocation")
                    val responsediarycontent = jsonResponseObject.getString(PARAM_CONTENT)
                    Log.i(VolleyLog.TAG, "Response Diary content is: $responsediarycontent")
                    val responsediarysubject = jsonResponseObject.getString(PARAM_SUBJECT)
                    Log.i(VolleyLog.TAG, "Response Diary subject is: $responsediarysubject")
                    val responsediaryemotionalresult = jsonResponseObject.getString(PARAM_EMOTIONALRESULT)
                    Log.i(VolleyLog.TAG, "Response Diary emotion is: $responsediaryemotionalresult")

                    val dateformatter = SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.UK)
                    val date = dateformatter.parse(responsediarydate+" "+responsediarytime) as Date
                    calender.time = date

                    updateDate(calender)
                    updateTime(calender)

                    etLocation.setText(responsediarylocation)
                    etcontent.setText(responsediarycontent)
                    etsubject.setText(responsediarysubject)
                    tvEmotionalResult.setText(responsediaryemotionalresult)
                }
            }catch (e: JSONException){
                e.printStackTrace()
                Log.e(VolleyLog.TAG,"PARSING ERROR:${e.message}")
                Toast.makeText(this, "Parsing Error: Loading is failed.", Toast.LENGTH_SHORT)
                    .show()
                finish()
            }
        },{error->
            Log.e(VolleyLog.TAG,error.message?:"No Error message")
            Toast.makeText(this, "Loading is failed.", Toast.LENGTH_SHORT)
                .show()
            finish()
        })
        requestQueue.add(redOneDiaryRequest)
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