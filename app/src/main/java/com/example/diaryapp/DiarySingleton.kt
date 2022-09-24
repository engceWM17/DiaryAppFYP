package com.example.diaryapp

import android.app.Application
import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

class DiarySingleton constructor(context: Context){

    val requestQueue: RequestQueue by lazy{
        Volley.newRequestQueue(context.applicationContext)
    }

    fun<T> addToRequestQueue(request: Request<T>){
        request.tag= TAG
        requestQueue?.add(request)
    }

    companion object {
        @Volatile
        private var INSTANCE: DiarySingleton? = null

        fun getInstance(context: Context) = INSTANCE?: synchronized(this){
            INSTANCE?:DiarySingleton(context).also {
                INSTANCE = it
            }
        }
        val TAG = DiarySingleton::class.java.simpleName

    }

}