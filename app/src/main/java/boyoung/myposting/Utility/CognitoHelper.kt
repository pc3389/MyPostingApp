package boyoung.myposting.Utility

import android.content.Context
import android.util.Log
import android.widget.Toast
import boyoung.myposting.LoginActivity
import boyoung.myposting.R
import com.amplifyframework.auth.AuthChannelEventName
import com.amplifyframework.auth.AuthUserAttribute
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.InitializationStatus
import com.amplifyframework.hub.HubChannel
import com.amplifyframework.hub.HubEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class CognitoHelper(private val context: Context) {

    fun signOut() {
        Amplify.Auth.signOut(
            { Log.i("AuthQuickstart", "Signed out successfully") },
            { error -> Log.e("AuthQuickstart", error.toString()) }
        )
    }

    fun getUsername(): String {
        if (Amplify.Auth.currentUser != null) {
            return Amplify.Auth.currentUser.username
        } else return context.getString(R.string.no_user)
    }

    fun getUserId(): String {
        if (Amplify.Auth.currentUser != null) {
            return Amplify.Auth.currentUser.userId
        } else return context.getString(R.string.no_user)
    }

}