package boyoung.myposting

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import boyoung.myposting.Utility.CognitoHelper
import boyoung.myposting.Utility.Constants
import com.amplifyframework.core.Amplify
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        Amplify.Auth.fetchAuthSession(
            { result -> Log.i("MyAmplifyApp", result.toString())
                if(result.isSignedIn) {
                    startMainActivity()
                }
            },
            { error -> Log.e("MyAmplifyApp", error.toString()) }
        )
        val cognitoHelper = CognitoHelper(this)

        textView_signUp.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        textView_logIn.setOnClickListener {
            val username: String = editText_signIn_id.text.toString()
            val password: String = editText_signIn_password.text.toString()
            val loginSt: String = cognitoHelper.signIn(username, password)
            Toast.makeText(this, loginSt, Toast.LENGTH_SHORT).show()
            if (loginSt == "Please confirm user first and then retry operation") {
                val intent = Intent(this, ConfirmationActivity::class.java).apply {
                    putExtra(Constants.KEY_USERNAME, username)
                }
                startActivity(intent)
            } else if (loginSt == "success") {
                startMainActivity()
                finish()
            }
        }
    }

    fun startMainActivity() {
        val intent = Intent(this, MainActivity:: class.java)
        startActivity(intent)
    }
}