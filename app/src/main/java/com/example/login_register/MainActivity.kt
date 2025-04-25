package com.example.login_register

import android.annotation.SuppressLint
import android.content.Intent
import android.media.session.MediaSession.Token
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.recyclerview.widget.SortedList
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

class MainActivity : AppCompatActivity() {

    private lateinit var welcomeText: TextView
    private lateinit var logoutButton: Button
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var nav_view: NavigationView
    private val handler = Handler(Looper.getMainLooper())

    //Interval pengecekan token dalam milidetik(5 detik)
    private val checkTokenInterval: Long = TimeUnit.SECONDS.toMillis(5)

    @SuppressLint("SetText18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        welcomeText = findViewById(R.id.welcomeText)
        logoutButton = findViewById(R.id.logoutButton)
        drawerLayout = findViewById(R.id.drawer_layout)
        nav_view = findViewById(R.id.nav_view)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)

        toolbar.setNavigationOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // Ambil username dari sharedPreferences
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", "")
        val role = sharedPreferences.getString("role", "")

        if (role == "admin") {
            toolbar.visibility = View.VISIBLE
        }else{
            toolbar.visibility = View.GONE
        }

        // Jika username ada, tampilkan pada TextView
        if (!username.isNullOrEmpty()) {
            welcomeText.text = "Selamat datang, $username!"
            welcomeText.visibility = View.VISIBLE
        }

        // Handle Side Bar
        nav_view.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_settings -> {
                    Toast.makeText(this, "Settings ditekan!", Toast.LENGTH_SHORT).show()
                    val settingsIntent = Intent(this, SettingsActivity::class.java)
                    startActivity(settingsIntent)
                }
                R.id.nav_register_data -> Toast.makeText(this, "Register Data ditekan!", Toast.LENGTH_SHORT).show()
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Logout button action
        logoutButton.setOnClickListener {
            // Logout action
            val editor = sharedPreferences.edit()
            editor.remove("username") //Hapus username
            editor.remove("role") ///Hapus role
            editor.remove("accessToken") //Hapus accesToken
            editor.remove("refreshToken") //Hapus refreshToken
            editor.apply()

            // Menampilkan pesan dan kembali
            Toast.makeText(this, "Logout Berhasil", Toast.LENGTH_SHORT).show()
            handler.removeCallbacksAndMessages(null)
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

        //Pengganti fungsi untuk mulai mengecek token periodik
        startTokenChecker()
    }

    //Fungsi untuk memeriksa apakah acce stoken sudah kadaluarsa
    private fun isTokenExpired(token: String): Boolean {
        return try {
            val parts = token.split(".")
            if (parts.size ==3) {
                val payload = String(Base64.decode(parts[1], Base64.DEFAULT))
                val jsonObject = JSONObject(payload)
                val expTime = jsonObject.getLong("exp") * 1000
                System.currentTimeMillis() > expTime
            }else true
        }catch (e: Exception){
            true
        }
    }

    //Fungsi untuk mengecek token setiap 5 menit
    private fun startTokenChecker(){
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkAndRefreshToken() //paggil pengecekan token
                handler.postDelayed(this, checkTokenInterval) //ulangi setiap 5 detik
            }
        }, checkTokenInterval)
    }

    private var isLoggedOut = false

    //fungsi untuk memeriksa apakah tooken kadaluarsa dan melakukan refresh atau logout
    private fun checkAndRefreshToken() {
        if (isLoggedOut) return
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val accesToken = sharedPreferences.getString("accesToken", "")
        val refreshToken = sharedPreferences.getString("refreshToken", "")

        if (accesToken != null && isTokenExpired(accesToken)) {
            if (refreshToken != null) {
                if (isTokenExpired(refreshToken)) {
                    //jika token juga expires, langsung logout
                    logoutAndRedirectToLogin()
                } else {
                    refreshAccessToken(refreshToken)
                }
            } else {
                logoutAndRedirectToLogin()
            }
        }
    }

        //Data class untuk refereshToken
        data class RefreshTokenRequest(val refreshToken: String)

        //Interface API untuk refresh Token
        interface ApiService {
            @POST("token-refresh")
            fun refreshToken(@Body request: RefreshTokenRequest): Call<RefreshTokenResponse>
        }

        //Data class untuk response Refresh Token
        data class RefreshTokenResponse(val accesToken: String)

        //Fungsi untuk memanggil API refresh token
        private fun refreshAccessToken(refreshToken: String) {
            //Panggil API untuk refresh token menggunakan refreshToken
            val retrofit = Retrofit.Builder()
                .baseUrl("http://192.168.111.43:5000/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)
            val request = RefreshTokenRequest(refreshToken)
            val call = apiService.refreshToken(request)

            call.enqueue(object : Callback<RefreshTokenResponse> {
                override fun onResponse(call: Call<RefreshTokenResponse>, response: Response<RefreshTokenResponse>) {
                    if (response.isSuccessful) {
                        val newAccesToken = response.body()?.accesToken
                        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString("accesToken", newAccesToken) //simpan acces tken yang baru
                        editor.apply()
                    }else {
                        //Jika refresh token gagal, logout dan arahkan ke LoginActivity
                        logoutAndRedirectToLogin()
                    }
                }

                override fun onFailure(call: Call<RefreshTokenResponse>, t: Throwable) {
                     Toast.makeText(this@MainActivity, "Refresh token gagal", Toast.LENGTH_SHORT).show()
                }
            })
        }

        //Fungsi untuk loogout dan mengarahkan ke halaman login
        private fun logoutAndRedirectToLogin() {
            val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.clear() //hapus semua data pengguna
            editor.apply()

            Toast.makeText(this, "Token kadaluarsa. Andaa telah logout", Toast.LENGTH_SHORT).show()
            handler.removeCallbacksAndMessages(null)
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() //menutup MainActivity agar tidak bisa kembali
        }
}