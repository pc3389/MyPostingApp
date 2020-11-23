package boyoung.myposting.adapters

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import boyoung.myposting.R
import boyoung.myposting.activities.PostActivity
import boyoung.myposting.utilities.Constants
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Post
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.result.StorageDownloadFileResult
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.main_list_item.view.*
import kotlinx.android.synthetic.main.post_list_item.view.*
import kotlinx.coroutines.*
import java.io.File

class PostAdapter(private val items: ArrayList<Post>, val context: Context, val username: String) :
    RecyclerView.Adapter<PostAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTextView: TextView = view.date_tv_post_item
        val titleTextView: TextView = view.title_tv_post_item
        val commentsTextView: TextView = view.comments_tv_post_item
        val imageImageView: ImageView = view.postImage_iv_post_item
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
        val a = CoroutineScope(Dispatchers.Main).launch {
            val date = items[position].date
            holder.dateTextView.text = date
            val title = items[position].title
            holder.titleTextView.text = title
            val comments = items[position].comments
            val numberOfComments = if (comments == null) {
                "0"
            } else {
                items[position].comments.size.toString()
            } + "Comments"
            holder.commentsTextView.text = numberOfComments

            val image = items[position].image
            val filepath = context.cacheDir.toString() + "/$image"
            if (items[position].image != null) {
                loadImageFromS3(filepath, image, holder)
            } else {
                holder.imageImageView.visibility = View.GONE
            }
            holder.itemView.setOnClickListener {
                val intent = Intent(context, PostActivity::class.java).apply {
                    putExtra(Constants.POST_ID, items[position].id)
                    putExtra(Constants.POST_DATE, date)
                    if (File(filepath).exists()) {
                        putExtra(Constants.POST_IMAGE, filepath)
                    }
                    val name = if (items[position].nickName == null) {
                        username
                    } else {
                        items[position].nickName
                    }
                    putExtra(Constants.POST_USERNAME, username)
                    putExtra(Constants.POST_NAME, name)
                    putExtra(Constants.POST_TITLE, title)
                    putExtra(Constants.POST_CONTENT, items[position].contents)
                    val profileImagePath = context.cacheDir.toString() + "/$username"
                    putExtra(Constants.PROFILE_IMAGE_PATH, profileImagePath)
                }
                context.startActivity(intent)
            }
        }
    }

    private suspend fun loadImageFromS3(
        filepath: String,
        image: String,
        holder: ViewHolder
    ) =
        withContext(Dispatchers.Main) {
            val file = File(filepath)
            if (file.exists()) {
                val glideWork = CoroutineScope(Dispatchers.Main).launch {
                    Glide.with(context)
                        .load(file)
                        .into(holder.imageImageView)
                }
                var time: Int = 0
                while (glideWork.isActive && time < 5) {
                    delay(200L)
                    time += 1
                    if (time == 5) {
                        glideWork.cancel()
                    }
                }
            } else {
                holder.imageImageView.visibility = View.GONE
            }
        }


    override fun getItemCount(): Int {
        return items.size
    }
}