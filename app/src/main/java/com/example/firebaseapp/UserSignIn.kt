package com.example.firebaseapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import com.example.firebaseapp.userDao.UserDao
import com.example.firebaseapp.userModels.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.internal.SignInButtonImpl
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.test.withTestContext
import kotlinx.coroutines.withContext

class UserSignIn : AppCompatActivity() {

    private val RC_SIGN_IN: Int = 123
    private val TAG = "SignInActivity Tag"
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth:FirebaseAuth
    private lateinit var signInButton:SignInButton
    private lateinit var progressBar:ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_sign_in)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this,gso)
        auth = Firebase.auth
        signInButton  = findViewById(R.id.signIn)
        progressBar  = findViewById(R.id.progressBar)

        signInButton.setOnClickListener {
            signIn()
        }
    }

    override fun onStart() {
        super.onStart()

        val currentUser = auth.currentUser;
        updateUI(currentUser)
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
            if (requestCode == RC_SIGN_IN) {
                val task: Task<GoogleSignInAccount> =
                    GoogleSignIn.getSignedInAccountFromIntent(data)

                    Log.e(TAG,"null mere bhai " + data.toString())
                //val account = task.getResult(ApiException::class.java)!!

                handleSignInResult(task)
            }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>){
        try {
            // Google Sign In was successful, authenticate with Firebase
            //Log.e(TAG, "failed!!!!!!!!!!!!" )
            val account = completedTask.getResult(ApiException::class.java)!!
            Log.e(TAG, "firebaseAuthWithGoogle:" + account.id)
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            // Google Sign In failed, update UI appropriately
            Log.e(TAG, "Google sign in failed", e)
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken,null)
        signInButton.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
        GlobalScope.launch(Dispatchers.IO){
            val auth = auth.signInWithCredential(credential).await()
            val firebaseUser = auth.user
            Log.e(TAG, "failed!!!!!!!!!!!!" )
            withContext(Dispatchers.Main){
                updateUI(firebaseUser)

            }
        }
    }

    private fun updateUI(firebaseUser: FirebaseUser?){
        if (firebaseUser != null) {

            val user = User(firebaseUser.uid,firebaseUser.displayName,firebaseUser.photoUrl.toString())
            val usersData = UserDao()
            usersData.addUser(user)

            val mainActivityIntent = Intent(this,MainActivity::class.java)
            startActivity(mainActivityIntent)
            finish()
        }else{
            //Toast.makeText(applicationContext,"failed",Toast.LENGTH_LONG).show()
            signInButton.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
        }
    }


}