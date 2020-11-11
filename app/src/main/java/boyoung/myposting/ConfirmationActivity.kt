package boyoung.myposting

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import boyoung.myposting.Utility.CognitoHelper
import boyoung.myposting.Utility.Constants
import kotlinx.android.synthetic.main.activity_confirmation.*

class ConfirmationActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation)
        val username = intent.getStringExtra(Constants.KEY_USERNAME)
        textView_username.text = username
        val cognitoHelper = CognitoHelper(this)
        textView_confirm.setOnClickListener {
            if (username != null) {
                val isConfirmed = cognitoHelper.confirmSignup(username, editText_Confirmation.text.toString())
                if(isConfirmed) {
                    startMainActivity()
                    finish()
                }
            }
        }

    }
    fun startMainActivity() {
        val intent = Intent(this, MainActivity:: class.java)
        startActivity(intent)
    }
}