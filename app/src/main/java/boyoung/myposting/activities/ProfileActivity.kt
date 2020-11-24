package boyoung.myposting.activities

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.PopupMenu
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import boyoung.myposting.R
import boyoung.myposting.adapters.PostAdapter
import boyoung.myposting.utilities.Constants
import boyoung.myposting.utilities.UploadHelper
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Post
import com.amplifyframework.datastore.generated.model.Profile
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.options.StorageUploadFileOptions
import com.amplifyframework.storage.result.StorageDownloadFileResult
import com.amplifyframework.storage.result.StorageUploadFileResult
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.activity_profile.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import java.io.File
import java.lang.StringBuilder

class ProfileActivity : AppCompatActivity() {
    private val context: Context = this

    companion object {
        private var file: File? = null
        private val profiles = ArrayList<Profile>()
        private val posts: ArrayList<Post> = ArrayList()
        private var postNumber = 0
        private var profileUpdated = false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val profileId = intent.getStringExtra(Constants.PROFILE_ID)
        CoroutineScope(Main).launch {
            showProgressBar()
            val a = CoroutineScope(IO).launch {
                if (profileId != null) {
                    queryProfile(profileId)
                }
            }
            var i = 0
            while (!profileUpdated && i < 20) {
                delay(100L)
                i++
            }
            if (a.isActive) {
                a.cancel()
            }
            if (profileUpdated) {
                profileUpdated = if (profiles.size == 0) {
                    Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
                    false
                } else {
                    setupRecycler(profiles[0].id)
                    false
                }
            }

        }

        camera_profile.setOnClickListener {
            getImageFromGallery()
        }

        setupMenu()

        cancel_bt_image_update.setOnClickListener {
            hideEditProfile()
        }

        save_bt_image_update.setOnClickListener {
            CoroutineScope(Main).launch {

                val username = getUsername()

                val name = if (name_et_profile.text.toString() == "") {
                    username
                } else {
                    name_et_profile.text.toString()
                }

                val imageKey = if (file != null) {
                    getImageKey(username)
                } else null

                saveProfile(file, username, name, imageKey)
                showProgressBar()
                if (profileId != null) {
                    queryProfile(profileId)
                }
                hideEditProfile()
            }
        }


        back_bt_update.setOnClickListener {
            onBackPressed()
        }
    }

    private fun setupMenu() {
        menu_bt_profile.setOnClickListener {
            val popupMenu = PopupMenu(this@ProfileActivity, menu_bt_profile)
            popupMenu.menuInflater.inflate(R.menu.menu_profile, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener {
                if (it.itemId == R.id.action_edit) {
                    val name = name_profile.text.toString()
                    name_et_profile.setText(name)
                    showEditProfile()
                }
                true
            }
            popupMenu.show()
        }
    }

    private suspend fun queryProfile(profileId: String) {
        withContext(IO) {
            Amplify.API.query(
                ModelQuery.list(Profile::class.java, Profile.ID.contains(profileId)),
                { response ->
                    for (profile in response.data) {
                        profiles.add(profile)
                        Log.i("MyAmplifyApp", profile.username)
                    }
                    if (profiles.isNotEmpty()) {
                        CoroutineScope(Main).launch {
                            updateUI(profiles[0])
                        }
                    }
                    runOnUiThread {
                        hideProgressBar()
                        profileUpdated = true
                    }
                },
                { error -> Log.e("MyAmplifyApp", "Query failure", error) }
            )
        }

    }

    private suspend fun updateUI(profile: Profile) = withContext(Main) {
        name_profile.text = profile.nickname
        if (profile.profileImage != null && !isDestroyed) {
            val profileImage = profile.profileImage
            val file = File("$cacheDir/$profileImage")
            loadProfileImage(file, profileImage)
        }
    }

    private suspend fun saveProfile(
        file: File?,
        username: String,
        name: String,
        imageKey: String?
    ) =
        withContext(IO) {
            if (imageKey != null) {
                imageToS3(file, imageKey)
            }

            val profile = Profile.builder()
                .username(username)
                .nickname(name)
                .profileImage(imageKey)
                .build()

            Amplify.API.mutate(
                ModelMutation.create(profile),
                { response ->
                    Log.i(
                        "MyAmplifyApp",
                        "Profile with name: " + response.data.nickname
                    )
                },
                { error -> Log.e("MyAmplifyApp", "Create failed", error) }
            )
        }

    private suspend fun getUsername(): String = withContext(IO) {
        return@withContext Amplify.Auth.currentUser.username
    }

    private suspend fun getImageKey(username: String): String = withContext(Main) {
        val builder = StringBuilder()
        builder.append(username)
        builder.append("_profile.jpg")

        return@withContext builder.toString()
    }

    private suspend fun imageToS3(file: File?, imageKey: String) = withContext(IO) {
        if (file != null) {
            Amplify.Storage.uploadFile(
                imageKey,
                file,
                StorageUploadFileOptions.defaultInstance(),
                { result: StorageUploadFileResult ->
                    Log.i(
                        "MyAmplifyApp",
                        "Successfully uploaded: " + result.key
                    )
                },
                { error: StorageException? ->
                    Log.e(
                        "MyAmplifyApp",
                        "Upload failed",
                        error
                    )
                }
            )
        }
    }

    private suspend fun loadProfileImage(file: File, image: String) = withContext(IO) {
        if (!file.exists()) {
            Amplify.Storage.downloadFile(
                image,
                file,
                { result: StorageDownloadFileResult ->
                    Glide.with(context)
                        .load(result.file)
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any?,
                                target: Target<Drawable>?,
                                isFirstResource: Boolean
                            ): Boolean {
                                hideProgressBar()
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                hideProgressBar()
                                return false
                            }
                        })
                        .into(profileImage_iv_profile)
                },
                { error: StorageException? ->
                    Log.e(
                        "MyAmplifyApp",
                        "Download Failure",
                        error
                    )
                    hideProgressBar()
                }
            )
        } else {
            val glideWork = CoroutineScope(Main).launch {
                Glide.with(context)
                    .load(file)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            hideProgressBar()
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            hideProgressBar()
                            return false
                        }
                    })
                    .into(profileImage_iv_profile)
            }
            var time: Int = 0
            while (glideWork.isActive && time < 5) {
                delay(200L)
                time += 1
            }
            if (glideWork.isActive) {
                glideWork.cancel()
            }
            hideProgressBar()
        }
    }

    private fun getImageFromGallery() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_DENIED
            ) {
                //permission denied
                val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE);
                //show popup to request runtime permission
                requestPermissions(permissions, Constants.PERMISSION_CODE);
            } else {
                //permission already granted
                pickImageFromGallery();
            }
        } else {
            //system OS is < Marshmallow
            pickImageFromGallery();
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, Constants.IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == Constants.IMAGE_PICK_CODE) {

            val uploadHelper = UploadHelper()
            if (data?.data != null) {
                file = File(uploadHelper.getRealPath(this, data?.data!!))
            }
            Glide.with(this)
                .load(data?.data)
                .into(profileImage_iv_profile)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun setupRecycler(profileId: String) {
        val linearLayoutManager = LinearLayoutManager(context)
        profile_rc.layoutManager = linearLayoutManager
        CoroutineScope(Main).launch {
            queryPost(profileId)
        }
    }

    private suspend fun queryPost(profileId: String) = withContext(Default) {
        withContext(Default) {
            for (postItem in profiles[0].posts) {
                posts.add(postItem)
            }
            posts.sortByDescending { it.date }
        }
        withContext(Main) {
            val fivePosts = getFivePosts(posts)
            profile_rc.adapter = PostAdapter(fivePosts, context, profileId)
            pageHelper(profileId, posts)
        }
    }


    private fun getFivePosts(posts: ArrayList<Post>): ArrayList<Post> {
        val end = postNumber + 5
        val fivePosts: ArrayList<Post> = ArrayList()
        while (postNumber < end && posts.size > postNumber) {
            fivePosts.add(posts[postNumber])
            postNumber += 1
        }
        if (postNumber % 5 != 0) {
            postNumber += 5 - postNumber % 5
        }
        return fivePosts
    }

    private fun pageHelper(username: String?, posts: ArrayList<Post>) {
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
                        previous_page_frame_profile.isClickable = false
                        return false
                    }
                })
                .into(previous_page_image_profile)
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
                        previous_page_frame_profile.setOnClickListener {
                            if (username != null) {
                                postNumber -= 10
                                val fivePosts = getFivePosts(posts)
                                profile_rc.adapter = PostAdapter(fivePosts, context, username)
                                pageHelper(username, posts)
                            }
                        }
                        return false
                    }
                })
                .into(previous_page_image_profile)
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
                        next_page_frame_profile.isClickable = false
                        return false
                    }
                })
                .into(next_page_image_profile)
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
                        next_page_frame_profile.setOnClickListener {
                            if (username != null) {
                                val fivePosts = getFivePosts(posts)
                                profile_rc.adapter = PostAdapter(fivePosts, context, username)
                                pageHelper(username, posts)
                            }
                        }
                        return false
                    }
                })
                .into(next_page_image_profile)
        }
    }

    override fun onBackPressed() {
        if (save_and_cancel.visibility == View.VISIBLE) {
            hideEditProfile()
            return
        } else {
            finish()
        }
        super.onBackPressed()
    }


    private fun showEditProfile() {
        name_profile.visibility = View.GONE
        name_et_profile.visibility = View.VISIBLE
        save_and_cancel.visibility = View.VISIBLE
        camera_profile.visibility = View.VISIBLE
    }

    private fun hideEditProfile() {
        name_profile.visibility = View.VISIBLE
        name_et_profile.visibility = View.GONE
        save_and_cancel.visibility = View.GONE
        camera_profile.visibility = View.GONE
    }

    private fun showProgressBar() {
        progressbar_profile.visibility = View.VISIBLE
        layout_all_profile.visibility = View.GONE
    }

    private fun hideProgressBar() {
        progressbar_profile.visibility = View.GONE
        layout_all_profile.visibility = View.VISIBLE
    }
}