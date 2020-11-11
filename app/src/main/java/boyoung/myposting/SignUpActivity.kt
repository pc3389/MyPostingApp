package boyoung.myposting

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import boyoung.myposting.Utility.CognitoHelper
import kotlinx.android.synthetic.main.activity_sign_up.*

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        val cognitoHelper = CognitoHelper(this)

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
                val signUp = cognitoHelper.signUp(username, password, emailAddress, phoneNumber)
                if(signUp == "") {
                    //TODO Confirmation
                }
            }
        }
    }
}