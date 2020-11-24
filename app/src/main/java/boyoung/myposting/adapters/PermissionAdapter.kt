package boyoung.myposting.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import boyoung.myposting.R
import com.amplifyframework.datastore.generated.model.Post
import com.amplifyframework.datastore.generated.model.PostPermission
import kotlinx.android.synthetic.main.userid_list_item.view.*

class PermissionAdapter(val items: ArrayList<PostPermission>, val context: Context) :
    RecyclerView.Adapter<PermissionAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val usernameTextView: TextView = view.username_item_permission
        val isPostableTextView : TextView = view.isPostable_item_permission
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(context).inflate(
                R.layout.userid_list_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.usernameTextView.text = items[position].username
        holder.isPostableTextView.text = items[position].permission.toString()
    }

    override fun getItemCount(): Int {
        return items.size
    }

}