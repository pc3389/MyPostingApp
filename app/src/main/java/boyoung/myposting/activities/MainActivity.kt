package boyoung.myposting.activities

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import boyoung.myposting.adapters.MainAdapters
import boyoung.myposting.R
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Post
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.internal.wait

class MainActivity : AppCompatActivity() {
    private val context = this

    private val posts: ArrayList<Post> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val linearLayoutManager = LinearLayoutManager(context)
        main_rc.layoutManager = linearLayoutManager
        main_rc.adapter = MainAdapters(posts, context)

        button_add.setOnClickListener {
            toPostActivity()
        }

        turnOnProgressBar()
        val a = CoroutineScope(Main).launch {
            queryPost()
        }

        itemsswipetorefresh.setProgressBackgroundColorSchemeColor(
            ContextCompat.getColor(
                this,
                R.color.colorPrimary
            )
        )
        itemsswipetorefresh.setColorSchemeColors(Color.WHITE)

        itemsswipetorefresh.setOnRefreshListener {
            a.cancel()
            turnOnProgressBar()
            CoroutineScope(Main).launch {
                posts.clear()
                queryPost()
            }
        }

    }

    private fun toPostActivity() {
        val intent = Intent(this, UploadActivity::class.java)
        startActivity(intent)
    }


    private suspend fun queryPost() = withContext(IO) {

        Amplify.API.query(
            ModelQuery.list(Post::class.java, Post.TITLE.contains("")),
            { response ->
                for (post in response.data) {
                    posts.add(post)
                    Log.i("MyAmplifyApp", post.title)
                }
                CoroutineScope(Main).launch {
                    withContext(Default) {
                        posts.sortByDescending { it.date }
                    }
                    runOnUiThread {
                        main_rc.adapter = MainAdapters(posts, context)
                        itemsswipetorefresh.isRefreshing = false
                        turnOffProgressBar()
                    }
                }
            },
            { error ->
                Log.e("MyAmplifyApp", "Query failure", error)
                runOnUiThread {
                    turnOffProgressBar()
                }
            }
        )
    }

    private suspend fun signOut() = withContext(IO) {
        Amplify.Auth.signOut(
            {
                startLoginActivity()
            },
            { error ->
                runOnUiThread {
                    Toast.makeText(context, error.recoverySuggestion, Toast.LENGTH_SHORT).show()
                }
            }
        )
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

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val menuItem = menu?.findItem(R.id.action_providePostPermission)
        menuItem?.isVisible = getUsername() == "pc3389"
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

            R.id.action_providePostPermission -> {
                val intent = Intent(this, ProvidingPostPermissionActivity::class.java)
                startActivity(intent)
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getUsername(): String {
        return Amplify.Auth.currentUser.username
    }
    private fun turnOnProgressBar() {
        itemsswipetorefresh.visibility = View.INVISIBLE
        main_rc.visibility = View.GONE
        progressbar_main.visibility = View.VISIBLE
    }

    private fun turnOffProgressBar() {
        itemsswipetorefresh.visibility = View.VISIBLE
        main_rc.visibility = View.VISIBLE
        progressbar_main.visibility = View.GONE
    }
}