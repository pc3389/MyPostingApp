package boyoung.myposting

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.amplifyframework.datastore.generated.model.Post
import kotlinx.android.synthetic.main.post_list_item.view.*

class PostAdapters(val items: ArrayList<Post>, val context: Context) : RecyclerView.Adapter<PostAdapters.ViewHolder>() {

    class ViewHolder (view: View) : RecyclerView.ViewHolder(view) {
        // Holds the TextView that will add each animal to
        val tvTitle = view.title_tv
        val tvContent = view.content_tv
        val tvComments = view.comments_tv
        val ivImage = view.postImage_iv
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.post_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvTitle?.text = items.get(position).title
        holder.tvContent?.text = items.get(position).contents
        val comment = items.get(position).comments
        if(comment != null) {
            holder.tvComments?.text = comment.size.toString()
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

