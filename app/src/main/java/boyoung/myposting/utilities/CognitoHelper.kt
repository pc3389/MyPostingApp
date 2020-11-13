package boyoung.myposting.utilities

import android.content.Context
import android.util.Log
import boyoung.myposting.R
import com.amplifyframework.core.Amplify

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