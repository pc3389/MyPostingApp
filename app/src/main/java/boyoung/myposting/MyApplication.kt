package boyoung.myposting

import android.app.Application
import android.util.Log
import com.amplifyframework.AmplifyException
import com.amplifyframework.api.aws.AWSApiPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.datastore.AWSDataStorePlugin
import com.amplifyframework.storage.s3.AWSS3StoragePlugin

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = AmplifyConfiguration.builder(applicationContext).devMenuEnabled(false).build()
        try {
            // Add these lines to add the AWSCognitoAuthPlugin and AWSS3StoragePlugin plugins
            Amplify.addPlugin(AWSCognitoAuthPlugin())
            Amplify.addPlugin(AWSS3StoragePlugin())
            Amplify.addPlugin(AWSDataStorePlugin())
            Amplify.addPlugin(AWSApiPlugin())
            Amplify.configure(config, applicationContext)
            Log.i("MyAmplify", "Initialized Amplify")
        } catch (error: AmplifyException) {
            Log.e("MyAmplify", "Could not initialize Amplify", error)
        }
    }
}