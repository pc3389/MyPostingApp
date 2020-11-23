package boyoung.myposting.activities

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import boyoung.myposting.PostAdapters
import boyoung.myposting.R
import boyoung.myposting.utilities.Constants
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.result.StorageDownloadFileResult
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.activity_post.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import java.io.File

class PostActivity : AppCompatActivity() {
    private val context = this
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)

        val id = intent.getStringExtra(Constants.POST_ID)
        val date = intent.getStringExtra(Constants.POST_DATE)
        val image = intent.getStringExtra(Constants.POST_IMAGE)
        val name = intent.getStringExtra(Constants.POST_NAME)
        val title = intent.getStringExtra(Constants.POST_TITLE)
        val content = intent.getStringExtra(Constants.POST_CONTENT)
        val profileImagePath = intent.getStringExtra(Constants.PROFILE_IMAGE_PATH)

        title_tv_post.text = title
        date_tv_post.text = date
        name_tv_post.text = name
        content_tv_post.text = content

        if(profileImagePath != null) {
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

        back_bt_iv_post.setOnClickListener {
            onBackPressed()
        }
    }

    private suspend fun loadProfileImage(filePath: String, imageView: ImageView, context: Context) =
        withContext(Main) {
            val file = File(filePath)
            if(file.exists()) {
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
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                return false
                            }
                        })
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
}
