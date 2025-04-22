package com.example.login_register

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import  android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var welcomeText: Button
    private lateinit var logoutButton: Button

    @SuppressLint("SetText18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        welcomeText = findViewById(R.id.welcomeText)
        logoutButton = findViewById(R.id.logoutButton)

        //Ambil username dari sharedPreferences
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val username = sharedPreferences.getString("username","")

        //Jika username ada, tampilkan pada TextView
        if (!username.isNullOrEmpty()) {
            welcomeText.text = "Selamat datang, $username!"
            welcomeText.visibility = View.VISIBLE
        }

        //logout button action
        logoutButton.setOnClickListener{
            //logout action
            val editor = sharedPreferences.edit()
            editor.remove("username")
            editor.apply()

            //Menampilkan pesan dan kembali
            Toast.makeText(this,"logout Berhasil", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}