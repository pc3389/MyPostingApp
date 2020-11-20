package boyoung.myposting.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import boyoung.myposting.PostAdapters
import boyoung.myposting.R
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Post
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private val context = this

    private val posts: ArrayList<Post> = ArrayList()
    private lateinit var linearLayoutManager: LinearLayoutManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button_add.setOnClickListener {
            toPostActivity()
        }
        CoroutineScope(Main).launch {
            queryPost()
            linearLayoutManager = LinearLayoutManager(context)
            post_rc.layoutManager = linearLayoutManager
            post_rc.adapter = PostAdapters(posts, context)
        }

    }

    private fun toPostActivity() {
        val intent = Intent(this, UploadActivity::class.java)
        startActivity(intent)
    }


    private suspend fun queryPost() {
        withContext(IO) {
            Amplify.DataStore.query(Post::class.java,
                {
                    while (it.hasNext()) {
                        posts.add(0,it.next())
                    }
                    Log.i("MyAmplifyApp", "Query $it")
                },
                { Log.e("MyAmplifyApp", "Query failed.", it) }
            )
        }

    }

    private suspend fun signOut() {
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
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                return true
            }
            R.id.action_signOut -> {
                CoroutineScope(IO).launch {
                    signOut()
                }
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}