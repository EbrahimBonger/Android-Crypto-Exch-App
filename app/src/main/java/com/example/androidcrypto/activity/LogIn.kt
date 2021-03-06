package com.example.androidcrypto.activity

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import com.example.androidcrypto.R
import com.example.androidcrypto.data.User
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase

class LogIn : AppCompatActivity() {

    // For an explaination of why lateinit var is needed, see:
    // https://docs.google.com/presentation/d/1icewQjn-fkd-wTepzRoqXOjaKWtGUrx0o0Us2anJz3w/edit#slide=id.g615c45607e_0_156
    private lateinit var username: EditText
    private lateinit var password: EditText
    private lateinit var login: Button
    private lateinit var create_account: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: FirebaseDatabase
    private lateinit var user: User

    // onCreate is called the first time the Activity is to be shown to the user, so it a good spot
    // to put initialization logic.
    // https://developer.android.com/guide/components/activities/activity-lifecycle
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        firebaseAuth = FirebaseAuth.getInstance()


        val preferences: SharedPreferences = getSharedPreferences("android-crypto", Context.MODE_PRIVATE)

        username = findViewById(R.id.username)
        password = findViewById(R.id.password)
        login = findViewById(R.id.login)
        create_account = findViewById(R.id.create_account)
        progressBar = findViewById(R.id.authenticationProgressBar)

        // Kotlin shorthand for login.setEnabled(false).
        // If the getter / setter is unambiguous, Kotlin lets you use the property-style syntax
        login.isEnabled = false
        create_account.isEnabled = true

        // Restore the saved username from SharedPreferences and display it to the user when the screen loads.
        // Default to the empty string if there is no saved username.
        val savedUsername = preferences.getString("USERNAME", "")
        val savedPassword = preferences.getString("PASSWORD", "")
        username.setText(savedUsername)
        password.setText(savedPassword)


        if(preferences.contains("USERNAME") && preferences.contains("PASSWORD")) {
            login.isEnabled = true
        }

        // Using a lambda to implement a View.OnClickListener interface. We can do this because
        // an OnClickListener is an interface that only requires *one* function.
        login.setOnClickListener {
            // Save the username to SharedPreferences
            val inputtedUsername = username.text.toString()
            val inputtedPassword = password.text.toString()

            showLoading()

            firebaseAuth
                .signInWithEmailAndPassword(inputtedUsername, inputtedPassword)
                .addOnCompleteListener { task: Task<AuthResult> ->
                    hideLoading()

                    if (task.isSuccessful) {
                        val currentUser: FirebaseUser = firebaseAuth.currentUser!!

                        Toast.makeText(
                            this,
                            getString(R.string.login_success, currentUser.email),
                            Toast.LENGTH_LONG
                        ).show()

                        val editor = preferences.edit()
                        editor.putString("USERNAME", inputtedUsername)
                        editor.putString("PASSWORD", inputtedPassword)
                        editor.apply()


                        // An Intent is used to start a new Activity.
                        // 1st param == a "Context" which is a reference point into the Android system. All Activities are Contexts by inheritance.
                        // 2nd param == the Class-type of the Activity you want to navigate to.

                        val intent: Intent = Intent(this, Dashboard::class.java)
//                        val intent: Intent = Intent(this, Portfolio::class.java)

                        intent.putExtra("USER_UID", currentUser.uid)

                        // An Intent can also be used like a Map (key-value pairs) to pass data between Activities.
                        // intent.putExtra("LOCATION", "Washington")

                        // "Executes" our Intent to start a new Activity
                        startActivity(intent)
                    } else {
                        val exception: Exception? = task.exception
                        when (exception) {
                            is FirebaseAuthInvalidUserException -> {
                                Toast.makeText(
                                    this,
                                    R.string.login_failure_doesnt_exist,
                                    Toast.LENGTH_LONG
                                ).show()

                                val intent: Intent = Intent(this, SignUp::class.java)
                                startActivity(intent)
                            }
                            is FirebaseAuthInvalidCredentialsException -> {
                                Toast.makeText(
                                    this,
                                    R.string.login_failure_wrong_credentials,
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                            else -> {
                                Toast.makeText(
                                    this,
                                    getString(R.string.login_failure_generic, exception),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
        }

        create_account.setOnClickListener {
            val intent: Intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }
        // Using the same TextWatcher instance for both EditTexts so the same block of code runs on each character.
        username.addTextChangedListener(textWatcher)
        password.addTextChangedListener(textWatcher)
    }

    // Displays the loading indicator and disables user input
    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        login.isEnabled = false
        create_account.isEnabled = false
        username.isEnabled = false
        password.isEnabled = false
    }

    // Hides the loading indicator and enables user input
    private fun hideLoading() {
        progressBar.visibility = View.INVISIBLE
        login.isEnabled = true
        create_account.isEnabled = true
        username.isEnabled = true
        password.isEnabled = true
    }

    private val textWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            // Kotlin shorthand for username.getText().toString()
            // .toString() is needed because getText() returns an Editable (basically a char array).
            val inputtedUsername: String = username.text.toString()
            val inputtedPassword: String = password.text.toString()
            val enableButton: Boolean = inputtedUsername.isNotBlank() && inputtedPassword.isNotBlank()

            // Kotlin shorthand for login.setEnabled(enableButton)
            login.isEnabled = enableButton
            // create_account.isEnabled = enableButton
        }

        override fun afterTextChanged(p0: Editable?) {}
    }
}