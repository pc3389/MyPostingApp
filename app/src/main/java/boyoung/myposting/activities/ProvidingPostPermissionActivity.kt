package boyoung.myposting.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import boyoung.myposting.R
import boyoung.myposting.adapters.PermissionAdapter
import boyoung.myposting.adapters.PostAdapter
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.api.graphql.model.ModelQuery
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Post
import com.amplifyframework.datastore.generated.model.PostPermission
import com.amplifyframework.datastore.generated.model.PostStatus
import kotlinx.android.synthetic.main.activity_post.*
import kotlinx.android.synthetic.main.activity_providing_post_permission.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ProvidingPostPermissionActivity : AppCompatActivity() {
    private val context = this
    val usernameList: ArrayList<PostPermission> = ArrayList()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_providing_post_permission)
        setupRecycler()
    }

    private fun setupRecycler() {
        val linearLayoutManager = LinearLayoutManager(context)
        username_rc_permission.layoutManager = linearLayoutManager
        CoroutineScope(Main).launch {
            queryPost()
        }
        add_permision_bt.setOnClickListener {
            if (getUsername() != "") {
                CoroutineScope(Main).launch { addUser() }
                username_rc_permission.adapter = PermissionAdapter(usernameList, context)
            } else {
                Toast.makeText(context, "Enter Username", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun queryPost() = withContext(Dispatchers.IO) {
        Amplify.API.query(
            ModelQuery.list(PostPermission::class.java, PostPermission.USERNAME.contains("")),
            { response ->
                usernameList.clear()
                for (username in response.data) {
                    usernameList.add(username)
                }
                Log.i("MyAmplifyApp", "Usernames updated in recyclerview")
                runOnUiThread {
                    username_rc_permission.adapter = PermissionAdapter(usernameList, context)
                }
            },
            { error ->
                Log.e("MyAmplifyApp", "Query failure", error)
            }
        )
    }

    private suspend fun addUser() = withContext(Dispatchers.IO) {

        val username = PostPermission.builder()
            .username(getUsername())
            .permission(true)
            .build()

        Amplify.API.mutate(
            ModelMutation.create(username),
            { response -> Log.i("MyAmplifyApp", "Todo with id: " + response.data.id) },
            { error -> Log.e("MyAmplifyApp", "Create failed", error) }
        )
        usernameList.add(username)
        username_rc_permission.adapter = PermissionAdapter(usernameList, context)
    }

    private fun getUsername(): String {
        return add_username_permission.text.toString()
    }

}