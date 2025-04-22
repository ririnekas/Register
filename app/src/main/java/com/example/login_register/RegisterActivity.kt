package com.example.login_register

import android.content.Intent
import android.os.Bundle
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

class RegisterActivity : AppCompatActivity() {

    //View components
    private lateinit var usernameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        //Initialize views
        usernameEditText = findViewById(R.id.usernameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        registerButton = findViewById(R.id.registerButton)

        //Set click listener for register button
        registerButton.setOnClickListener{
            registerUser()
        }
    }

    //Data class for User
    data class User(
        val full_name: String,
        val email: String,
        val password: String
    )

    //Data class for User Response
    data class UserResponse(
        val message: String
    )

    //Api Service Interface
    interface ApiService{
        @POST("register")
        fun registerUser(@Body user: User): Call<UserResponse>
    }

    //Function to register user
    private fun registerUser(){
        try {
            //Get data from EditText
            val full_name = usernameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            //Validasi Input
            if(full_name.isEmpty()|| email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Pleas fill all fields", Toast.LENGTH_SHORT).show()
                return
            }

            //Buat User Object
            val user = User(full_name, email, password)

            //Creat Retrofit instance
            val retrofit = Retrofit.Builder()
                .baseUrl("http://192.168.180.130:5000/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)

            val call = apiService.registerUser(user)
            call.enqueue(object : Callback<UserResponse>{
                override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                    if (response.isSuccessful){
                        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString("username", full_name)
                        editor.apply()

                        val serverMessage = response.body()?.message ?: "Registrasi Berhasil"
                        //Registration successful, navigate to MainActivity
                        Toast.makeText(this@RegisterActivity, serverMessage, Toast.LENGTH_SHORT).show()
                        //Start MainActivity
                        val intent = Intent(this@RegisterActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish() //optionali close the RegisterActivity
                    } else {
                        val errorMessage = response.errorBody()?.string()?: "Resgistrasi Gagal"
                        val jsonError = JSONObject(errorMessage)
                        val serverMessage = jsonError.getString("message")
                        //show error from the server if registration failed
                        Toast.makeText(this@RegisterActivity, serverMessage, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<UserResponse>, t: Throwable){
                    //show error message if network request fails
                    Toast.makeText(this@RegisterActivity, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
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