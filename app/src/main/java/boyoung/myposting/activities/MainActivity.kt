package boyoung.myposting.activities

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.PopupMenu
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
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main

class MainActivity : AppCompatActivity() {
    private val context = this

    private val posts: ArrayList<Post> = ArrayList()
    private val profile: ArrayList<Profile> = ArrayList()
    private val coroutineScope = CoroutineScope(Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        coroutineScope.launch {
            showProgressBar()
            val username = getUsername()
            val linearLayoutManager = LinearLayoutManager(context)
            mainAct_rc_posts.layoutManager = linearLayoutManager
            getPostPermission(username)
            queryProfile(username)
        }

        mainAct_itemsswipetorefresh.setProgressBackgroundColorSchemeColor(
            ContextCompat.getColor(
                this,
                R.color.colorPrimary
            )
        )

        mainAct_itemsswipetorefresh.setColorSchemeColors(Color.WHITE)

        mainAct_itemsswipetorefresh.setOnRefreshListener {
            showProgressBar()
            coroutineScope.launch {
                queryPost()
            }
        }

        mainAct_image_uploadPost_bt.setOnClickListener {
            if (profile.size != 0) {
                toUploadActivity(profile[0].id)
            } else {
                Toast.makeText(this, "Profile is not loaded", Toast.LENGTH_SHORT).show()
            }
        }

    }

    override fun onResume() {
        showProgressBar()
        CoroutineScope(Main).launch {
            queryPost()
        }
        super.onResume()
    }

    private fun toUploadActivity(profileId: String) {
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
                    if (profileItem != null) {
                        if (profileItem.username == username) {
                            profile.add(profileItem)
                        }
                        Log.i("MyAmplifyApp", profileItem.username + "is added")
                    }
                }
                if (profile.size == 0) {
                    startProfileActivity()
                } else {
                    coroutineScope.launch {
                        queryPost()
                        setupMenu()
                    }
                }
            },
            { error ->
                Log.e("MyAmplifyApp", "Query failure", error)
                runOnUiThread {
                    hideProgressBar()
                }
            }
        )
    }

    private suspend fun queryPost() = withContext(IO) {

        Amplify.API.query(
            ModelQuery.list(Post::class.java, Post.TITLE.contains("")),
            { response ->
                posts.clear()
                for (post in response.data) {
                    posts.add(post)
                    Log.i("MyAmplifyApp", post.title)
                }
                CoroutineScope(Main).launch {
                    withContext(Default) {
                        posts.sortByDescending { it.date }
                    }
                    mainAct_rc_posts.adapter = MainAdapters(posts, context)
                    mainAct_itemsswipetorefresh.isRefreshing = false
                    hideProgressBar()
                }
            },
            { error ->
                Log.e("MyAmplifyApp", "Query failure", error)
                runOnUiThread {
                    hideProgressBar()
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

    private fun setupMenu() {
        mainAct_image_menu_bt.setOnClickListener {
            val popupMenu = PopupMenu(this@MainActivity, mainAct_image_menu_bt)
            popupMenu.menuInflater.inflate(R.menu.menu_main, popupMenu.menu)
            popupMenu.menu.findItem(R.id.action_providePostPermission).isVisible =
                getUsername() == "pc3389"
            if (getUsername() == "guest") {
                popupMenu.menu.findItem(R.id.action_profile).isVisible = false
            }

            popupMenu.setOnMenuItemClickListener {
                if (it.itemId == R.id.action_settings) {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                }
                if (it.itemId == R.id.action_profile) {
                    startProfileActivity(profile[0].id)
                }
                if (it.itemId == R.id.action_signOut) {
                    CoroutineScope(IO).launch {
                        signOut()
                    }
                }
                if (it.itemId == R.id.action_providePostPermission) {
                    val intent = Intent(this, ProvidingPostPermissionActivity::class.java)
                    startActivity(intent)
                }
                true
            }
            popupMenu.show()
        }
    }

    private suspend fun getPostPermission(name: String) = withContext(IO) {
        Amplify.API.query(
            ModelQuery.list(PostPermission::class.java, PostPermission.USERNAME.contains("")),
            { response ->
                var ispossible = false
                runOnUiThread { mainAct_image_uploadPost_bt.visibility = View.GONE }
                for (usernameItem in response.data) {
                    if (usernameItem.username == name) {
                        if (usernameItem.permission == true) {
                            runOnUiThread { mainAct_image_uploadPost_bt.visibility = View.VISIBLE }
                            ispossible = true
                            Log.e("MyAmplifyApp", "This user can post items")
                        } else {
                            runOnUiThread { mainAct_image_uploadPost_bt.visibility = View.GONE }
                        }
                    }
                }
                if (!ispossible) {
                    Log.e("MyAmplifyApp", "This user can't post items")
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

    private fun showProgressBar() {
        runOnUiThread {
            mainAct_itemsswipetorefresh.visibility = View.INVISIBLE
            mainAct_rc_posts.visibility = View.GONE
            mainAct_progressbar.visibility = View.VISIBLE
        }
    }

    private fun startProfileActivity() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
    }

    private fun startProfileActivity(profileId: String) {
        val intent = Intent(this, ProfileActivity::class.java).apply {
            putExtra(Constants.PROFILE_ID, profileId)
        }
        startActivity(intent)
    }

    private fun hideProgressBar() {
        runOnUiThread {
            mainAct_itemsswipetorefresh.visibility = View.VISIBLE
            mainAct_rc_posts.visibility = View.VISIBLE
            mainAct_progressbar.visibility = View.GONE
        }
    }
}