package boyoung.myposting

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import boyoung.myposting.activities.PostActivity
import boyoung.myposting.utilities.Constants
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Post
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.result.StorageDownloadFileResult
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.post_list_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.StringBuilder

class PostAdapters(private val items: ArrayList<Post>, val context: Context) :
    RecyclerView.Adapter<PostAdapters.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Holds the TextView that will add each animal to
        val nameTextView: TextView = view.name_tv_item
        val dateTextView: TextView = view.date_tv_item
        val titleTextView: TextView = view.title_tv_item
        val contentTextView: TextView = view.content_tv_item
        val commentsTextView: TextView = view.comments_tv_item
        val imageImageView: ImageView = view.postImage_iv_item
        val progressbar: ConstraintLayout = view.progressbar_item
        val itemLayout: ConstraintLayout = view.layout_item
        val profileImageView: ImageView = view.profile_image_iv_item
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.main_list_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val a = CoroutineScope(Main).launch {
            holder.progressbar.visibility = View.VISIBLE
            holder.itemLayout.visibility = View.GONE
            val username = items[position].username
            val name = if (items[position].nickName == null) {
                username
            } else {
                items[position].nickName
            }

            holder.nameTextView.text = name
            val date = items[position].date
            holder.dateTextView.text = date
            val title = items[position].title
            holder.titleTextView.text = title
            val content = items[position].contents
            holder.contentTextView.text = content
            val comments = items[position].comments
            val numberOfComments = if (comments == null) {
                "0"
            } else {
                items[position].comments.size.toString()
            } + "Comments"
            holder.commentsTextView.text = numberOfComments

            val profileImagePath = context.cacheDir.toString() + "/$username"
            val profileImageKey = getProfileImageKey(username)
            loadProfileImage(profileImagePath, profileImageKey, holder)

            val image = items[position].image
            val filepath = context.cacheDir.toString() + "/$image"
            if (items[position].image != null) {
                loadImageFromS3(filepath, image, holder)
            } else {
                holder.progressbar.visibility = View.GONE
                holder.imageImageView.visibility = View.GONE
                holder.itemLayout.visibility = View.VISIBLE
            }
            holder.itemView.setOnClickListener {
                val intent = Intent(context, PostActivity::class.java).apply {
                    putExtra(Constants.POST_ID, items[position].id)
                    putExtra(Constants.POST_DATE, date)
                    if (File(filepath).exists()) {
                        putExtra(Constants.POST_IMAGE, filepath)
                    }
                    putExtra(Constants.POST_NAME, name)
                    putExtra(Constants.POST_TITLE, title)
                    putExtra(Constants.POST_CONTENT, content)
                    putExtra(Constants.PROFILE_IMAGE_PATH, profileImagePath)
                }
                context.startActivity(intent)
            }
        }
    }

    private suspend fun loadImageFromS3(filepath: String, image: String, holder: ViewHolder) =
        withContext(Main) {
            val file = File(filepath)
            if (!file.exists()) {
                withContext(IO) {
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
                                        holder.progressbar.visibility = View.GONE
                                        holder.imageImageView.visibility = View.VISIBLE
                                        holder.itemLayout.visibility = View.VISIBLE
                                        return false
                                    }

                                    override fun onResourceReady(
                                        resource: Drawable?,
                                        model: Any?,
                                        target: Target<Drawable>?,
                                        dataSource: DataSource?,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        holder.progressbar.visibility = View.GONE
                                        holder.imageImageView.visibility = View.VISIBLE
                                        holder.itemLayout.visibility = View.VISIBLE
                                        return false
                                    }
                                })
                                .into(holder.imageImageView)
                        },
                        { error: StorageException? ->
                            Log.e(
                                "MyAmplifyApp",
                                "Download Failure",
                                error
                            )
                            holder.progressbar.visibility = View.GONE
                            holder.imageImageView.visibility = View.VISIBLE
                            holder.itemLayout.visibility = View.VISIBLE
                        }
                    )
                }

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
                                holder.progressbar.visibility = View.GONE
                                holder.imageImageView.visibility = View.VISIBLE
                                holder.itemLayout.visibility = View.VISIBLE
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                holder.progressbar.visibility = View.GONE
                                holder.imageImageView.visibility = View.VISIBLE
                                holder.itemLayout.visibility = View.VISIBLE
                                return false
                            }
                        })
                        .into(holder.imageImageView)
                }
                var time: Int = 0
                while (glideWork.isActive && time < 5) {
                    delay(200L)
                    time += 1
                }
                if (glideWork.isActive) {
                    glideWork.cancel()
                    holder.progressbar.visibility = View.GONE
                    holder.imageImageView.visibility = View.VISIBLE
                    holder.itemLayout.visibility = View.VISIBLE
                } else {
                    holder.progressbar.visibility = View.GONE
                    holder.imageImageView.visibility = View.VISIBLE
                    holder.itemLayout.visibility = View.VISIBLE
                }
            }
        }

    private suspend fun loadProfileImage(filePath: String, imageKey: String, holder: ViewHolder) = withContext(Main) {
        val file = File(filePath)
        if (!file.exists()) {
            Amplify.Storage.downloadFile(
                imageKey,
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
                        .into(holder.profileImageView)
                },
                { error: StorageException? ->
                    Log.e(
                        "MyAmplifyApp",
                        "Download Failure",
                        error
                    )
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
                    .into(holder.profileImageView)
            }
            var time = 0
            while (glideWork.isActive && time < 5) {
                delay(200L)
                time += 1
            }
            glideWork.cancel()
        }
    }
    private suspend fun getProfileImageKey(username: String): String = withContext(Main) {
        val builder = StringBuilder()
        builder.append(username)
        builder.append("_profile.jpg")

        return@withContext builder.toString()
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

