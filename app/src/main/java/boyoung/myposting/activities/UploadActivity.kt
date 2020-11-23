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
import boyoung.myposting.utilities.UploadHelper
import com.amplifyframework.api.graphql.model.ModelMutation
import com.amplifyframework.core.Amplify
import com.amplifyframework.datastore.generated.model.Post
import com.amplifyframework.datastore.generated.model.PostStatus
import com.amplifyframework.storage.StorageException
import com.amplifyframework.storage.options.StorageUploadFileOptions
import com.amplifyframework.storage.result.StorageUploadFileResult
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_upload.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.StringBuilder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class UploadActivity : AppCompatActivity() {
    private val context = this

    companion object {
        private var file: File? = null
        private var hasImage: Boolean = false

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)
        back_tv_upload.setOnClickListener {
            finish()
        }
        savePost_tv_upload.setOnClickListener {
            CoroutineScope(Main).launch {
                if (title_et_upload.text.toString() == "") {
                    Toast.makeText(context, "Title should not be empty", Toast.LENGTH_SHORT).show()
                } else {
                    val title = title_et_upload.text.toString()
                    val imageKey = if (hasImage) {
                        getImageKey(title)
                    } else {
                        null
                    }
                    val content = content_et_upload.text.toString()
                    post(file, title, content, PostStatus.PUBLISHED, imageKey)
                    finish()
                }
            }

        }
        add_photo_tv_upload.setOnClickListener {
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

            val uploadHelper = UploadHelper()
            if(data?.data != null) {
                file = File(uploadHelper.getRealPath(this, data?.data!!))
            }
            Glide.with(this)
                .load(data?.data)
                .into(image_iv_upload)
            hasImage = true
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private suspend fun post(
        file: File?,
        title: String,
        content: String,
        status: PostStatus,
        imageKey: String?
    ) =
        withContext(IO) {
            if (file != null && imageKey != null) {
                imageToS3(file, imageKey)
            }

            val post = Post.builder()
                .username(getUsername())
                .title(title)
                .status(status)
                .date(todayDate())
                .contents(content)
                .image(imageKey)
                .build()

            Amplify.API.mutate(
                ModelMutation.create(post),
                { response -> Log.i("MyAmplifyApp", "Todo with id: " + response.data.id) },
                { error -> Log.e("MyAmplifyApp", "Create failed", error) }
            )
        }

    private suspend fun getUsername(): String = withContext(IO) {
        return@withContext Amplify.Auth.currentUser.username
    }

    private suspend fun getImageKey(title: String): String = withContext(Main) {
        val builder = StringBuilder()
        builder.append(getUsername())
        builder.append("_$title.jpg")

        return@withContext builder.toString()
    }
    private fun todayDate(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy. MM. dd. HH:mm")).toString()
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