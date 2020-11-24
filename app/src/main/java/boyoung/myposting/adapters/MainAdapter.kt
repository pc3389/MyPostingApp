package boyoung.myposting.adapters

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
import boyoung.myposting.R
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
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.main_list_item.view.*
import kotlinx.android.synthetic.main.post_list_item.view.layout_item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.StringBuilder

class MainAdapters(private val items: ArrayList<Post>, val context: Context) :
    RecyclerView.Adapter<MainAdapters.ViewHolder>() {

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
        CoroutineScope(Main).launch {
            turnOnProgressBar(holder)
            val username = items[position].profile.username
            val name = if (items[position].profile.nickname == null) {
                username
            } else {
                items[position].profile.nickname
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
                holder.imageImageView.visibility = View.GONE
                turnOffProgressBar(holder)
            }
            holder.itemView.setOnClickListener {
                val intent = Intent(context, PostActivity::class.java).apply {
                    putExtra(Constants.PROFILE_ID, items[position].profile.id)
                    putExtra(Constants.POST_ID, items[position].id)
                    putExtra(Constants.POST_DATE, date)
                    putExtra(Constants.POST_IMAGE, image)
                    putExtra(Constants.PROFILE_USERNAME, items[position].profile.username)
                    putExtra(Constants.PROFILE_NICKNAME, items[position].profile.nickname)
                    putExtra(Constants.POST_TITLE, title)
                    putExtra(Constants.POST_CONTENT, items[position].contents)
                    putExtra(Constants.PROFILE_IMAGE, items[position].profile.profileImage)
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
                                        turnOffProgressBar(holder)
                                        holder.imageImageView.visibility = View.VISIBLE
                                        return false
                                    }

                                    override fun onResourceReady(
                                        resource: Drawable?,
                                        model: Any?,
                                        target: Target<Drawable>?,
                                        dataSource: DataSource?,
                                        isFirstResource: Boolean
                                    ): Boolean {
                                        turnOffProgressBar(holder)
                                        holder.imageImageView.visibility = View.VISIBLE
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
                            turnOffProgressBar(holder)
                            holder.imageImageView.visibility = View.GONE
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
                                turnOffProgressBar(holder)
                                holder.imageImageView.visibility = View.GONE
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable?,
                                model: Any?,
                                target: Target<Drawable>?,
                                dataSource: DataSource?,
                                isFirstResource: Boolean
                            ): Boolean {
                                turnOffProgressBar(holder)
                                holder.imageImageView.visibility = View.VISIBLE
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
                    turnOffProgressBar(holder)
                    holder.imageImageView.visibility = View.VISIBLE
                } else {
                    turnOffProgressBar(holder)
                    holder.imageImageView.visibility = View.VISIBLE
                }
            }
        }

    private suspend fun loadProfileImage(filePath: String, imageKey: String, holder: ViewHolder) =
        withContext(Main) {
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
                    if (time == 5) {
                        glideWork.cancel()
                    }
                }
            }
        }

    private fun turnOnProgressBar(holder: ViewHolder) {
        holder.progressbar.visibility = View.VISIBLE
        holder.itemLayout.visibility = View.GONE
    }

    private fun turnOffProgressBar(holder: ViewHolder) {
        holder.progressbar.visibility = View.GONE
        holder.itemLayout.visibility = View.VISIBLE
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

