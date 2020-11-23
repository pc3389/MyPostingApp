package boyoung.myposting.activities

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import boyoung.myposting.R
import boyoung.myposting.adapters.PostAdapter
import boyoung.myposting.utilities.Constants
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Post
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.activity_post.*
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import java.io.File

class PostActivity : AppCompatActivity() {
    private val context = this
    companion object {
        private val posts: ArrayList<Post> = ArrayList()
        private var postNumber = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        postNumber = 0
        val id = intent.getStringExtra(Constants.POST_ID)
        val date = intent.getStringExtra(Constants.POST_DATE)
        val image = intent.getStringExtra(Constants.POST_IMAGE)
        val name = intent.getStringExtra(Constants.POST_NAME)
        val username = intent.getStringExtra(Constants.POST_USERNAME)
        val title = intent.getStringExtra(Constants.POST_TITLE)
        val content = intent.getStringExtra(Constants.POST_CONTENT)
        val profileImagePath = intent.getStringExtra(Constants.PROFILE_IMAGE_PATH)

        title_tv_post.text = title
        date_tv_post.text = date
        name_tv_post.text = name
        content_tv_post.text = content

        val recyclerTitle = "Other posts from $name"
        recycler_title_post.text = recyclerTitle

        if (profileImagePath != null) {
            CoroutineScope(Main).launch {
                loadProfileImage(profileImagePath, profile_image_iv_post, context)
            }

        }
        if (image != null) {
            val file = File(image)
            if (file.exists()) {
                Glide.with(this)
                    .load(file)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            postImage_iv_post.visibility = View.VISIBLE
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            postImage_iv_post.visibility = View.VISIBLE
                            return false
                        }
                    })
                    .into(postImage_iv_post)
            }
        }

        if (username != null && id != null) {
            setupRecycler(username, id)
            setupMenu(username, id)
        }

        back_bt_iv_post.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupMenu (username:String, id: String) {
        if(username == Amplify.Auth.currentUser.username) {
            menu_bt_iv_post.setOnClickListener {
                val popupMenu = PopupMenu(this@PostActivity, menu_bt_profile)
                popupMenu.menuInflater.inflate(R.menu.menu_profile, popupMenu.menu)
                popupMenu.setOnMenuItemClickListener {
                    if (it.itemId == R.id.action_edit) {

                    }
                    if (it.itemId == R.id.action_delete) {

                    }
                    true
                }
                popupMenu.show()
            }
        } else {
            menu_bt_iv_post.visibility = View.GONE
        }
    }
    private fun setupRecycler(username: String, id: String) {
        val linearLayoutManager = LinearLayoutManager(context)
        post_rc.layoutManager = linearLayoutManager
        CoroutineScope(Main).launch {
            queryPost(username, id)
        }
    }

    private suspend fun loadProfileImage(filePath: String, imageView: ImageView, context: Context) =
        withContext(Main) {
            val file = File(filePath)
            if (file.exists()) {
                val glideWork = CoroutineScope(Main).launch {
                    Glide.with(context)
                        .load(file)
                        .into(imageView)
                }
                var time = 0
                while (glideWork.isActive && time < 5) {
                    delay(200L)
                    time += 1
                }
                glideWork.cancel()
            }
        }

    private suspend fun queryPost(username: String, id: String) = withContext(Dispatchers.IO) {
        Amplify.API.query(
            ModelQuery.list(Post::class.java, Post.USERNAME.contains(username)),
            { response ->
                posts.clear()
                for (post in response.data) {
                    if (post.id != id) {
                        posts.add(post)
                    }
                    Log.i("MyAmplifyApp", post.title)
                }
                CoroutineScope(Main).launch {
                    withContext(Default) {
                        posts.sortByDescending { it.date }
                    }
                    withContext(Main) {
                        runOnUiThread {
                            val fivePosts = getFivePosts(posts)
                            post_rc.adapter = PostAdapter(fivePosts, context, username)
                            pageHelper(username, id, posts)
                        }
                    }
                }
            },
            { error ->
                Log.e("MyAmplifyApp", "Query failure", error)
            }
        )
    }

    private fun getFivePosts(posts: ArrayList<Post>): ArrayList<Post> {
        val end = postNumber + 5
        val fivePosts: ArrayList<Post> = ArrayList()
        while (postNumber < end && posts.size > postNumber) {
            fivePosts.add(posts[postNumber])
            postNumber += 1
        }
        if(postNumber % 5 != 0) {
            postNumber += 5 - postNumber % 5
        }
        return fivePosts
    }

    private fun pageHelper(username: String?, id: String?, posts: ArrayList<Post>) {
        if (postNumber - 5 <= 0) {
            Glide.with(context)
                .load(R.drawable.previous_page_unavailable_24)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        previous_page_frame_post.isClickable = false
                        return false
                    }
                })
                .into(previous_page_image_post)
        } else {
            Glide.with(context)
                .load(R.drawable.previous_page_available)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        previous_page_frame_post.setOnClickListener {
                            if (username != null && id != null) {
                                postNumber -= 10
                                val fivePosts = getFivePosts(posts)
                                post_rc.adapter = PostAdapter(fivePosts, context, username)
                                pageHelper(username, id, posts)
                            }
                        }
                        return false
                    }
                })
                .into(previous_page_image_post)
        }
        if (postNumber >= posts.size) {
            Glide.with(context)
                .load(R.drawable.next_page_unavailable_24)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        next_page_frame_post.isClickable = false
                        return false
                    }
                })
                .into(next_page_image_post)
        } else {
            Glide.with(context)
                .load(R.drawable.next_page_available_24)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        next_page_frame_post.setOnClickListener {
                            if (username != null && id != null) {
                                val fivePosts = getFivePosts(posts)
                                post_rc.adapter = PostAdapter(fivePosts, context, username)
                                pageHelper(username, id, posts)
                            }
                        }
                        return false
                    }
                })
                .into(next_page_image_post)
        }
    }

    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }
}
