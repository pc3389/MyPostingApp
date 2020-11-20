package boyoung.myposting.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
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
                if (result.isSignedIn) {
                    progressbar.visibility = View.VISIBLE
                    login_layout.visibility = View.GONE
                    startMainActivity()
                }
                Log.i("MyAmplifyApp", result.toString())
            },
            { error -> Log.e("MyAmplifyApp", error.toString()) }
        )

        textView_guest.setOnClickListener {
            startMainActivity()
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
    }

    private suspend fun logIn(username: String, password: String) = withContext(Main) {
        progressbar.visibility = View.VISIBLE
        login_layout.visibility = View.GONE
        withContext(IO) {
            Amplify.Auth.signIn(
                username,
                password,
                {
                    startMainActivity()
                    Log.e("MyAmplifyApp", "login")
                    finish()
                },
                { error ->
                    Log.e("MyAmplifyApp", error.toString())
                    if (error.recoverySuggestion == "Please confirm user first and then retry operation") {
                        startConfirmationActivity(username)
                    }
                    runOnUiThread {
                        Toast.makeText(context, error.recoverySuggestion, Toast.LENGTH_SHORT).show()
                        progressbar.visibility = View.GONE
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