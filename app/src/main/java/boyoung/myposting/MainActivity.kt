package boyoung.myposting

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import boyoung.myposting.Utility.CognitoHelper
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val cognitoHelper = CognitoHelper(this)
        button_signout.setOnClickListener {
            cognitoHelper.signOut()
            val intent = Intent(this, LoginActivity:: class.java)
            startActivity(intent)
        }
    }
}