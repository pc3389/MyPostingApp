package boyoung.myposting

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import boyoung.myposting.Utility.CognitoHelper
import com.amplifyframework.core.Amplify
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val context = this
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button_signout.setOnClickListener {
            CoroutineScope(IO).launch {
                signOut()
            }
        }
    }

    suspend fun signOut() {
        withContext(IO) {
            Amplify.Auth.signOut(
                { startLoginActivity() },
                { error ->
                    runOnUiThread {
                        Toast.makeText(context, error.recoverySuggestion, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
    private fun startLoginActivity() {
        val intent = Intent(this, LoginActivity:: class.java)
        startActivity(intent)
        finish()
    }
}