package com.example.diaryapp

import android.os.Bundle
import android.view.Window
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.example.diaryapp.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    var binding: ActivityMainBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)
        val username= intent.getStringExtra("username")
        val data =Bundle()
        data.putString("username",username)

        val homefrag = HomeFragment()
        val profilefrag = ProfileFragment()
        val chartfrag = ChartFragment()

        if (savedInstanceState == null){
            replaceFragment(homefrag,data)
            binding!!.bottomNavigationView.menu.findItem(R.id.home).setChecked(true)
        }

        binding!!.bottomNavigationView.setOnItemSelectedListener{ item->
            when(item.itemId){
                R.id.profile ->{
                    replaceFragment(profilefrag,data)
                }
                R.id.home ->{
                    replaceFragment(homefrag,data)
                }
                R.id.chart ->{
                    replaceFragment(chartfrag,data)
                }
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment, data:Bundle) {
        val fragmentManager: FragmentManager = supportFragmentManager
        val fragmentTransaction: FragmentTransaction = fragmentManager.beginTransaction()

        fragment.arguments= data
        fragmentTransaction.replace(R.id.frame_layout, fragment)
        fragmentTransaction.commit()
    }


}