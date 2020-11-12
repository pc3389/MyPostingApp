package boyoung.myposting

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import boyoung.myposting.Utility.CognitoHelper
import boyoung.myposting.Utility.Constants
import com.amplifyframework.core.Amplify
import kotlinx.android.synthetic.main.activity_confirmation.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ConfirmationActivity : AppCompatActivity() {
    private val context = this
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)
        val username = intent.getStringExtra(Constants.KEY_USERNAME)
        textView_username.text = username
        textView_confirm.setOnClickListener {
            if (username != null) {
                CoroutineScope(IO).launch {
                    confirmSignup(username, editText_Confirmation.text.toString())
                }
            }
        }
    }

    suspend fun confirmSignup(userName: String, confirmationCode: String) {
        withContext(IO) {
            Amplify.Auth.confirmSignUp(
                userName,
                confirmationCode,
                { result ->
                    if(result.isSignUpComplete) {
                        startMainActivity()
                    }
                    Log.i(
                        "MyAmplifyApp",
                        if (result.isSignUpComplete) "Confirm signUp succeeded" else "Confirm sign up not complete"
                    )
                },
                { error -> runOnUiThread {
                    Toast.makeText(context,  error.recoverySuggestion, Toast.LENGTH_SHORT).show()
                } }
            )
        }
    }

    fun startMainActivity() {
        val intent = Intent(this, MainActivity:: class.java)
        startActivity(intent)
        finish()
    }
}