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
import boyoung.myposting.utilities.Constants
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Post
import com.amplifyframework.datastore.generated.model.PostPermission
import com.amplifyframework.datastore.generated.model.Profile
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main

class MainActivity : AppCompatActivity() {
    private val context = this

    private val posts: ArrayList<Post> = ArrayList()
    private val profile: ArrayList<Profile> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val coroutineScope = CoroutineScope(Main)
        coroutineScope.launch {
            val username = getUsername()
            getPostPermission(username)
            queryProfile(username)
            queryPost()
        }
        val linearLayoutManager = LinearLayoutManager(context)
        main_rc.layoutManager = linearLayoutManager
        main_rc.adapter = MainAdapters(posts, context)

        turnOnProgressBar()


        itemsswipetorefresh.setProgressBackgroundColorSchemeColor(
            ContextCompat.getColor(
                this,
                R.color.colorPrimary
            )
        )
        itemsswipetorefresh.setColorSchemeColors(Color.WHITE)

        itemsswipetorefresh.setOnRefreshListener {
            turnOnProgressBar()
            coroutineScope.launch {
                posts.clear()
                queryPost()
            }
        }

        button_add.setOnClickListener {
            if (profile.size != 0) {
                toPostActivity(profile[0].id)
            } else {
                Toast.makeText(this, "Profile is not loaded", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onResume() {
        turnOnProgressBar()
        CoroutineScope(Main).launch {
            posts.clear()
            queryPost()
        }
        super.onResume()
    }

    private fun toPostActivity(profileId: String) {
        val intent = Intent(this, UploadActivity::class.java).apply {
            putExtra(Constants.PROFILE_ID, profileId)
        }
        startActivity(intent)
    }


    private suspend fun queryProfile(username: String) = withContext(IO) {
        Amplify.API.query(
            ModelQuery.list(Profile::class.java, Profile.USERNAME.contains(username)),
            { response ->
                for (profileItem in response.data) {
                    if (profileItem.username == username) {
                        profile.add(profileItem)
                    }
                    Log.i("MyAmplifyApp", profileItem.username + "is added")
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
                    main_rc.adapter = MainAdapters(posts, context)
                    itemsswipetorefresh.isRefreshing = false
                    turnOffProgressBar()
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
                val intent = Intent(this, ProfileActivity::class.java).apply {
                    putExtra(Constants.PROFILE_ID, profile[0].id)
                }
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

    private suspend fun getPostPermission(name: String) = withContext(IO) {
        Amplify.API.query(
            ModelQuery.list(PostPermission::class.java, PostPermission.USERNAME.contains("")),
            { response ->
                val usernameList: ArrayList<PostPermission> = ArrayList()
                val postPermission = PostPermission.builder()
                    .username(getUsername())
                    .permission(true)
                    .build()
                for (usernameItem in response.data) {
                    if (usernameItem.username == name)
                        usernameList.add(usernameItem)
                }
                Log.i("MyAmplifyApp", "Usernames updated in recyclerview")
                if (usernameList.isNotEmpty()) {
                    if (usernameList[0].permission == true) {
                        runOnUiThread { button_add.visibility = View.VISIBLE }
                    } else {
                        runOnUiThread { button_add.visibility = View.GONE }
                    }
                } else {
                    runOnUiThread { button_add.visibility = View.GONE }
                }
            },
            { error ->
                Log.e("MyAmplifyApp", "Query failure", error)
            }
        )
    }

    private fun getUsername(): String {
        return Amplify.Auth.currentUser.username
    }

    private fun turnOnProgressBar() {
        runOnUiThread {
            itemsswipetorefresh.visibility = View.INVISIBLE
            main_rc.visibility = View.GONE
            progressbar_main.visibility = View.VISIBLE
        }
    }

    private fun turnOffProgressBar() {
        runOnUiThread {
            itemsswipetorefresh.visibility = View.VISIBLE
            main_rc.visibility = View.VISIBLE
            progressbar_main.visibility = View.GONE
        }
    }
}