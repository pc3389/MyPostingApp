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
import com.amplifyframework.core.model.query.Where
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PostAdapters(private val items: ArrayList<Post>, val context: Context) :
    RecyclerView.Adapter<PostAdapters.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // Holds the TextView that will add each animal to
        val userNameTextView: TextView = view.username_tv
        val titleTextView: TextView = view.title_tv
        val contentTextView: TextView = view.content_tv
        val commentsTextView: TextView = view.comments_tv
        val imageImageView: ImageView = view.postImage_iv
        val progressbar: ConstraintLayout = view.progressbar
        val itemLayout: ConstraintLayout = view.item_layout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.post_list_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        CoroutineScope(Main).launch {
            val image = items[position].image
            holder.progressbar.visibility = View.VISIBLE
            holder.itemLayout.visibility = View.GONE
            withContext(IO) {
                if (items[position].image != null) {
                    loadImageFromS3(image, holder)
                } else {
                    withContext(Main) {
                        holder.progressbar.visibility = View.GONE
                        holder.itemLayout.visibility = View.VISIBLE
                    }
                }
            }
            val username = items[position].username
            holder.userNameTextView.text = username
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
            holder.itemView.setOnClickListener {
                val intent = Intent(context, PostActivity::class.java).apply {
                    putExtra(Constants.POST_ID, items[position].id)
                    putExtra(Constants.POST_IMAGE, image)
                    putExtra(Constants.POST_USERNAME, username)
                    putExtra(Constants.POST_TITLE, title)
                    putExtra(Constants.POST_CONTENT, content)
                }
                context.startActivity(intent)
            }

        }
    }

    fun loadImageFromS3(image: String, holder:ViewHolder) {
        Amplify.Storage.downloadFile(
            image,
            File(context.cacheDir.toString() + "/$image"),
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
                            holder.imageImageView.visibility = View.GONE
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
                holder.itemLayout.visibility = View.VISIBLE
            }
        )
    }

    //Delete
    /*CoroutineScope(IO).launch {
                    Amplify.DataStore.query(Post::class.java, Where.matches(Post.ID.eq(items[position].id)),
                        { matches ->
                            if (matches.hasNext()) {
                                val post = matches.next()
                                Amplify.DataStore.delete(post,
                                    { Log.i("MyAmplifyApp", "Deleted a post.") },
                                    { Log.e("MyAmplifyApp", "Delete failed.", it) }
                                )
                            }
                        },
                        { Log.e("MyAmplifyApp", "Query failed.", it) })
                }*/

    override fun getItemCount(): Int {
        return items.size
    }
}

