package com.example.diaryapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup


class ChartFragment : Fragment() {

    private var username: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view= inflater.inflate(R.layout.fragment_chart, container, false)
        val data= arguments as Bundle

        username =data.getString("username")

        return view
    }
}