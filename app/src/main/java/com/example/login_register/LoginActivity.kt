package com.example.login_register

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST


class LoginActivity : AppCompatActivity() {

    //View components
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //Initialize views
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)

        //Ktika diketik akan login
        loginButton.setOnClickListener{
            registerUser()
        }
    }

    //Data class for User
    data class User(
        val email: String,
        val password: String
    )

    //Data respon
    data class UserResponse(
        val message: String,
        val accessToken: String,
        val refreshToken: String,
        val data: Data
    )

    data class Data(
        val role: String
    )

    //Api Service Interface
    interface ApiService{
        @POST("login")
        fun registerUser(@Body user: User): Call<UserResponse>
    }

    //Function to register user
    private fun registerUser(){
        try {
            //Get data from EditText
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            //Validasi Input
            if(email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Pleas fill all fields", Toast.LENGTH_SHORT).show()
                return
            }

            //Buat User Object
            val user = User(email, password)

            //Creat Retrofit instance
            val retrofit = Retrofit.Builder()
                .baseUrl("http://192.168.111.43:5000/api/") //IP diganti sesuai WIFI kita
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java) //Manggil dari Api service

            //Pemanggilan API
            val call = apiService.registerUser(user)
            call.enqueue(object : Callback<UserResponse>{
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                    if (response.isSuccessful){
                        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        val role =  response.body()?.data?.role
                        val accessToken = response.body()?.accessToken
                        val refreshToken = response.body()?.refreshToken

                        editor.putString("username", email) //logika selamat datang email
                        editor.putString("role", role)
                        editor.putString("accessToken", accessToken)
                        editor.putString("refreshToken", refreshToken)
                        editor.apply()

                        //Operator pengganti pesan success
                        val serverMessage = response.body()?.message ?: "Login Berhasil"

                        //Registration successful, navigate to MainActivity
                        Toast.makeText(this@LoginActivity, serverMessage, Toast.LENGTH_SHORT).show()

                        //Start MainActivity
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish() //optionali close the RegisterActivity
                    } else {
                        val errorMessage = response.errorBody()?.string()?: "Login Gagal"
                        val jsonError = JSONObject(errorMessage)
                        val serverMessage = jsonError.getString("message")
                        //show error from the server if registration failed
                        Toast.makeText(this@LoginActivity, serverMessage, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<UserResponse>, t: Throwable){
                    //show error message if network request fails
                    Toast.makeText(this@LoginActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                    Log.e("RegisterActivity", "Error: ${t.message}", t) //Log Error
                }
            })
        }catch (e: Exception){
            //Hadle unexpeted exceptions
            Toast.makeText(this,"AN error occurred: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("RegisterActivity","Exeception: ${e.message}",e)
        }
    }

}