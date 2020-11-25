package boyoung.myposting.utilities

import com.amplifyframework.datastore.generated.model.Profile


class ProfileListener {
    interface ProfileListener {
        fun onDataLoaded(profileList: ArrayList<Profile>)
    }

    private var listener: ProfileListener? = null

    fun MyListener() {
        listener = null
    }

    fun setMyListener(listener: ProfileListener?) {
        this.listener = listener
    }

    fun ready(profileList: ArrayList<Profile>) {
        listener?.onDataLoaded(profileList)
    }
}