package com.example.diaryapp

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.VolleyLog
import com.android.volley.toolbox.StringRequest
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.json.JSONObject
import java.sql.Date
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

private const val READDIARYBYDATEURL = "https://gooddiary.000webhostapp.com/api/diary/read_by_date_username.php"
private const val PARAM_USERNAME="username"
private const val PARAM_DIARYDATE="diarydate"

private const val RESPONSE_RECORDS="records"
private const val RESPONSE_USERNAME="username"
private const val RESPONSE_ID="id"
private const val RESPONSE_SUBJECT="subject"
private const val RESPONSE_DIARYDATE="diarydate"
private const val RESPONSE_DIARYTIME="diarytime"
private const val RESPONSE_LOCATION="location"
private const val RESPONSE_CONTENT="content"
private const val RESPONSE_EMOTIONAL_RESULT="emotionalresult"
private const val RESPONSE_SUCCESS_KEY="success"

class HomeFragment : Fragment(){
    private lateinit var username: String
    private lateinit var datePicker: EditText
    private lateinit var diaryRecyclerView: RecyclerView
    private lateinit var adapter: RecycleViewAdapter
    private lateinit var diaryArrayList: ArrayList<Diary>
    private lateinit var addbtn: FloatingActionButton
    private lateinit var v:View
    val calender = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        v = inflater.inflate(R.layout.fragment_home, container, false)
        val data= arguments as Bundle

        username = data.getString("username").toString()
        addbtn = v.findViewById(R.id.fab)
        datePicker = v.findViewById(R.id.etDate)

        diaryArrayList =  arrayListOf<Diary>()
        syncDiaryList(calender)
        val layoutManager= LinearLayoutManager(v.context)
        diaryRecyclerView = v.findViewById(R.id.rvDiary)
        diaryRecyclerView.layoutManager = layoutManager
        adapter= RecycleViewAdapter(diaryArrayList)
        diaryRecyclerView.adapter= adapter

        val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result ->
            if(result.resultCode == Activity.RESULT_OK){
                syncDiaryList(calender)
            }
        }

        adapter.setOnItemClickListener(object:RecycleViewAdapter.OnItemClickListener{
            override fun onItemClick(position: Int) {
                val intent = Intent(context, SaveDeleteDiary::class.java).apply {
                   putExtra("id",diaryArrayList[position].id)
                }
                resultLauncher.launch(intent)
            }
        })

        if (savedInstanceState==null){
            updateEditText(calender)
        }

        val date = DatePickerDialog.OnDateSetListener { view, year, month, dayOfMonth ->
            calender.set(Calendar.YEAR, year)
            calender.set(Calendar.MONTH, month)
            calender.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateEditText(calender)
            syncDiaryList(calender)
        }

        datePicker.setOnClickListener {
            DatePickerDialog(v.context, date, calender.get(Calendar.YEAR),calender.get(Calendar.MONTH),calender.get(
                Calendar.DAY_OF_MONTH)).show()
        }

        addbtn.setOnClickListener {
            val intent = Intent(activity, AddTimeLocationDiary::class.java)
            intent.putExtra("username",username)
            startActivity(intent)
            syncDiaryList(calender)
        }

        return v
    }

    private fun updateEditText(myCalender:Calendar){
        val format="dd-MM-yyyy"
        val sdf = SimpleDateFormat(format,Locale.UK)
        datePicker.setText(sdf.format(myCalender.time))
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun syncDiaryList(myCalender:Calendar){

        val format="yyyy-MM-dd"
        val sdf = SimpleDateFormat(format,Locale.UK)
        val sdf2 = SimpleDateFormat("yyyy-MM-dd hh:mm:ss",Locale.UK)
        var diarydatetime : java.util.Date

        val readdiarylistURL = Uri.parse(READDIARYBYDATEURL).buildUpon().appendQueryParameter(PARAM_USERNAME,username).appendQueryParameter(
            PARAM_DIARYDATE,sdf.format(myCalender.time)).build()
        Log.i(VolleyLog.TAG,"Request DiaryURL:$readdiarylistURL")
        val getDiaryRequest = StringRequest(Request.Method.POST,readdiarylistURL.toString(), { response->
            try{
                diaryArrayList.clear()
                adapter.notifyDataSetChanged()
                if (response!=null) {
                    val jsonResponse = JSONObject(response.toString())
                    if (!jsonResponse.has(RESPONSE_SUCCESS_KEY)) {
                        val jsonArray = jsonResponse.getJSONArray(RESPONSE_RECORDS)
                        val size: Int = jsonArray.length()
                        var jsonDiary: JSONObject
                        var diary: Diary
                        for (i in 0..size - 1) {
                            jsonDiary = jsonArray.getJSONObject(i)
                            diarydatetime= sdf2.parse(jsonDiary.getString(RESPONSE_DIARYDATE)+" "+jsonDiary.getString(RESPONSE_DIARYTIME)) as java.util.Date
                            Diary(
                                jsonDiary.getInt(RESPONSE_ID),
                                jsonDiary.getString(RESPONSE_SUBJECT),
                                jsonDiary.getString(RESPONSE_LOCATION),
                                diarydatetime,
                                jsonDiary.getString(RESPONSE_CONTENT),
                                jsonDiary.getString(RESPONSE_EMOTIONAL_RESULT)
                            ).also { diary = it }
                            diaryArrayList.add(diary)
                            adapter.notifyItemInserted(diaryArrayList.size-1)
                        }

                        Toast.makeText(v.context, "Record Found:" + diaryArrayList.size, Toast.LENGTH_LONG)
                            .show()

                    }else {
                        Toast.makeText(v.context, "No Diary Found.", Toast.LENGTH_LONG)
                            .show()
                    }
                }

            }catch (e: Exception){
                Log.d("Home","ERROR:${e.message}")
                Toast.makeText(v.context, "No Diary Found.", Toast.LENGTH_LONG)
                    .show()
                diaryArrayList.clear()
                adapter.notifyDataSetChanged()
            }
        },{error->
            Log.d("Home",error.message?:"No Error message")
            Toast.makeText(v.context, "No Diary Found.", Toast.LENGTH_LONG)
                .show()
            diaryArrayList.clear()
            adapter.notifyDataSetChanged()
        })

        DiarySingleton.getInstance(v.context).addToRequestQueue(getDiaryRequest)
    }

}