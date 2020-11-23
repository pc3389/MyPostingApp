package boyoung.myposting.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import boyoung.myposting.R
import boyoung.myposting.utilities.Constants
import com.amplifyframework.core.Amplify
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class LoginActivity : AppCompatActivity() {

    val context = this
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Amplify.Auth.fetchAuthSession(
            { result ->
                if (result.isSignedIn && Amplify.Auth.currentUser.username == "guest") {
                    Amplify.Auth.signOut(
                        {
                            Log.i("MyAmplifyApp", "Signed out successfully")
                        },
                        { error ->
                            runOnUiThread {
                                Toast.makeText(
                                    context,
                                    error.recoverySuggestion,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                } else if (result.isSignedIn) {
                    progressbar_item.visibility = View.VISIBLE
                    login_layout.visibility = View.GONE
                    startMainActivity()
                    finish()
                }
                Log.i("MyAmplifyApp", result.toString())
            },
            { error -> Log.e("MyAmplifyApp", error.toString()) }
        )

        textView_guest.setOnClickListener {
            CoroutineScope(IO).launch { logIn("guest", "djaak123") }
        }

        textView_signUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }

        textView_logIn.setOnClickListener {
            val username: String = editText_signIn_id.text.toString()
            val password: String = editText_signIn_password.text.toString()
            CoroutineScope(Main).launch {
                logIn(username, password)
            }
        }
        editText_signIn_password.setOnEditorActionListener { view, actionId, event ->
            var handled = false
            val username: String = editText_signIn_id.text.toString()
            val password: String = editText_signIn_password.text.toString()
            if (actionId === EditorInfo.IME_ACTION_DONE) {
                CoroutineScope(Main).launch {
                    logIn(username, password)
                }
                handled = true
            }
            return@setOnEditorActionListener handled
        }
    }

    private suspend fun logIn(username: String, password: String) = withContext(Main) {
        progressbar_item.visibility = View.VISIBLE
        login_layout.visibility = View.GONE
        withContext(IO) {
            Amplify.Auth.signIn(
                username,
                password,
                {
                    startMainActivity()
                    Log.i("MyAmplifyApp", "login")
                    finish()
                },
                { error ->
                    Log.e("MyAmplifyApp", error.toString())
                    val recoverySuggestion = error.recoverySuggestion
                    if (recoverySuggestion == "Please confirm user first and then retry operation") {
                        startConfirmationActivity(username)
                    }
                    runOnUiThread {
                        if(recoverySuggestion != "See attached exception for more details.") {
                            Toast.makeText(context, error.recoverySuggestion, Toast.LENGTH_SHORT).show()
                        } else if (error.cause.toString().contains("Incorrect username or password")) {
                            Toast.makeText(context, "Incorrect username or password", Toast.LENGTH_SHORT).show()
                        }

                        progressbar_item.visibility = View.GONE
                        login_layout.visibility = View.VISIBLE
                    }
                }
            )
        }

    }

    private fun startMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun startConfirmationActivity(username: String) {
        val intent = Intent(this, ConfirmationActivity::class.java).apply {
            putExtra(Constants.KEY_USERNAME, username)
        }
        startActivity(intent)
    }
}