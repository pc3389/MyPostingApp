package boyoung.myposting.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import boyoung.myposting.R
import boyoung.myposting.utilities.Constants
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.core.Amplify
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignUpActivity : AppCompatActivity() {
    private val context = this
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        textView_signUp.setOnClickListener {
            val username:String = editText_signUpId.text.toString()
            val password: String = editText_signUpPassword.text.toString()
            val emailAddress: String = editText_signUpEmailAddress.text.toString()
            val phoneNumber = editTextPhone.text.toString()
            if(username.isEmpty() || password.isEmpty() || emailAddress.isEmpty()) {
                if(username.isEmpty()) {
                    Toast.makeText(this, "Username is required", Toast.LENGTH_SHORT).show()
                } else if (password.isEmpty()) {
                    Toast.makeText(this, "password is required", Toast.LENGTH_SHORT).show()
                } else if (emailAddress.isEmpty()) {
                    Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show()
                }
            } else {
                CoroutineScope(IO).launch {
                    signUp(username, password, emailAddress, phoneNumber)
                }
            }
        }
    }

    private suspend fun signUp(userName: String, password: String, email: String, phoneNumber: String) {
        withContext(IO) {
            val attributes: ArrayList<AuthUserAttribute> = ArrayList()
            //"my@email.com"
            attributes.add(AuthUserAttribute(AuthUserAttributeKey.email(), email))
            //"+15551234567"
            attributes.add(AuthUserAttribute(AuthUserAttributeKey.phoneNumber(), phoneNumber))
            Amplify.Auth.signUp(
                userName,
                password,
                AuthSignUpOptions.builder().userAttributes(attributes).build(),
                { result -> Log.i("MyAmplifyApp", "Result: $result")
                    if(result.isSignUpComplete) {
                        startConfirmationActivity(userName)
                    }

                },
                { error -> Log.e("MyAmplifyApp", "Sign up failed", error)
                    runOnUiThread {
                        Toast.makeText(context,  error.recoverySuggestion, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun startConfirmationActivity(username: String) {
        val intent = Intent(this, ConfirmationActivity::class.java).apply {
            putExtra(Constants.KEY_USERNAME, username)
        }
        startActivity(intent)
        finish()
    }

}