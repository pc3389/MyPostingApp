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
import kotlinx.coroutines.withContext

class CognitoHelper(private val context: Context) {

    fun signUp(userName: String, password: String, email: String, phoneNumber: String): String {
        var returnString: String = ""
        val attributes: ArrayList<AuthUserAttribute> = ArrayList()
        //"my@email.com"
        attributes.add(AuthUserAttribute(AuthUserAttributeKey.email(), email))
        //"+15551234567"
        attributes.add(AuthUserAttribute(AuthUserAttributeKey.phoneNumber(), phoneNumber))
        Amplify.Auth.signUp(
            userName,
            password,
            AuthSignUpOptions.builder().userAttributes(attributes).build(),
            { result -> Log.i("MyAmplifyApp", "Result: $result")
                returnString = result.nextStep.toString()

            },
            { error -> Log.e("MyAmplifyApp", "Sign up failed", error)
            returnString = error.recoverySuggestion}
        )
        Thread.sleep(500L)
        return returnString
    }

    fun confirmSignup(userName: String, confirmationCode: String): Boolean {
        var isComplete = false
        Amplify.Auth.confirmSignUp(
            userName,
            confirmationCode,
            { result ->
                isComplete = result.isSignUpComplete
                Log.i(
                    "MyAmplifyApp",
                    if (result.isSignUpComplete) "Confirm signUp succeeded" else "Confirm sign up not complete"
                )
            },
            { error -> Log.e("MyAmplifyApp", error.toString()) }
        )
        return isComplete
    }

    fun signIn(userName: String, password: String): String {
        var returnString: String = ""
        Amplify.Auth.signIn(
            userName,
            password,
            { result ->
                if (result.isSignInComplete) {
                    returnString = "success"
                }
            },
            { error ->
                Log.e("MyAmplifyApp", error.toString())
                returnString = error.recoverySuggestion
            }
        )
        Thread.sleep(500L)
        return returnString
    }

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