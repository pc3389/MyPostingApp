package boyoung.myposting.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import boyoung.myposting.R
import boyoung.myposting.utilities.Constants
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Post
import com.amplifyframework.datastore.generated.model.PostStatus
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.options.StorageUploadFileOptions
import com.amplifyframework.storage.result.StorageUploadFileResult
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_post.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.StringBuilder

class PostActivity : AppCompatActivity() {
    private var file: File? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post)
        back_tv.setOnClickListener {
            finish()
        }
        savePost_tv.setOnClickListener {
            CoroutineScope(Main).launch {
                if (title_et != null) {
                    val a = getPhotoKey(title_et.text.toString())
                    val b = a
                }
            }

        }
        add_photo_tv.setOnClickListener {
            getImage()
        }
    }

    private fun getImage() {
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            Constants.PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    //permission from popup granted
                    pickImageFromGallery()
                } else {
                    //permission from popup denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, Constants.IMAGE_PICK_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK && requestCode == Constants.IMAGE_PICK_CODE) {

            file = File(data?.data?.path)

            Glide.with(this)
                .load(data?.data)
                .into(post_iv)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private suspend fun post(title: String, content: String, status: PostStatus, imageKey: String) =
        withContext(IO) {
            imageToS3(file, imageKey)

            val post = Post.builder()
                .title(title)
                .contents(content)
                .status(status)
                .image(imageKey)
                .build()

            Amplify.DataStore.save(post,
                { Log.i("MyAmplifyApp", "Saved a post.") },
                { Log.e("MyAmplifyApp", "Save failed.", it) }
            )
        }

    private suspend fun getUsername(): String = withContext(IO) {
        return@withContext Amplify.Auth.currentUser.username
    }

    private suspend fun getPhotoKey(title: String): String = withContext(Main) {
        val builder = StringBuilder()
        builder.append(getUsername())
        builder.append("_$title.jpg")

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
}