package com.example.diaryapp

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class RecycleViewAdapter(private var diaryList: ArrayList<Diary>) :
    RecyclerView.Adapter<RecycleViewAdapter.diaryViewHolder>() {

    private lateinit var mListener:OnItemClickListener

    interface OnItemClickListener{
        fun onItemClick(position:Int)
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        mListener=listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): diaryViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.recycleview_list_diary,parent,false)
        return diaryViewHolder(itemView, mListener)
    }

    override fun onBindViewHolder(holder: diaryViewHolder, position: Int) {
        val format="hh:mm a"
        val sdf = SimpleDateFormat(format, Locale.UK)
        val currentItem = diaryList[position]

        holder.tvLocationResult.setText(currentItem.location)
        holder.tvTimeResult.setText(sdf.format(currentItem.diarydatetime))
        holder.tvSubjectResult.setText(currentItem.subject)
        holder.id=currentItem.id

    }

    override fun getItemCount(): Int {
        return diaryList.size
    }

    class diaryViewHolder(itemView: View, listener: OnItemClickListener):RecyclerView.ViewHolder(itemView){
        var tvLocationResult:TextView
        var tvTimeResult:TextView
        var tvSubjectResult:TextView
        var id: Int?=null
        init {
            tvLocationResult= itemView.findViewById(R.id.tvLocationResult)
            tvTimeResult= itemView.findViewById(R.id.tvTimeResult)
            tvSubjectResult= itemView.findViewById(R.id.tvSubjectResult)

            itemView.setOnClickListener {
                listener.onItemClick(bindingAdapterPosition)
            }
        }
    }

}