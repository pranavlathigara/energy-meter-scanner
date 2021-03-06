package ds.meterscanner.mvvm

import L
import android.arch.lifecycle.ViewModel
import android.support.annotation.StringRes
import com.bumptech.glide.RequestManager
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.KodeinAware
import com.github.salomonbrys.kodein.instance
import com.google.firebase.auth.FirebaseUser
import ds.bindingtools.Bindable
import ds.bindingtools.binding
import ds.meterscanner.R
import ds.meterscanner.auth.Authenticator
import ds.meterscanner.data.Prefs
import ds.meterscanner.data.ResourceProvider
import ds.meterscanner.db.FirebaseDb
import ds.meterscanner.net.NetLayer
import ds.meterscanner.scheduler.Scheduler
import ds.meterscanner.ui.Progressable
import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch

abstract class BindableViewModel(override val kodein: Kodein) : ViewModel(), KodeinAware, Progressable, Bindable {

    val restService: NetLayer = instance()
    val prefs: Prefs = instance()
    val authenticator: Authenticator = instance()
    val db: FirebaseDb = instance()
    val scheduler: Scheduler = instance()
    val resources: ResourceProvider = instance()
    val glide: RequestManager = instance()

    var showProgress by binding(false)

    // commands
    open val runAuthScreenCommand: Command<Unit>? = Command()
    val finishCommand = Command<Unit>()
    val showSnackbarCommand = SnackBarCommand()

    var lifecycleJob = Job() // create a job object bind manage lifecycle

    init {
        initAuthenticator()
    }

    private fun initAuthenticator() {
        authenticator.startListen(this, { logged ->
            if (logged)
                onLoggedIn(authenticator.getUser()!!)
            else
                runAuthScreenCommand?.invoke()
        })
    }

    override fun onCleared() {
        super.onCleared()
        authenticator.stopListen(this)
        lifecycleJob.cancel()
    }

    protected open fun onLoggedIn(user: FirebaseUser) {}

    override fun toggleProgress(enabled: Boolean) {
        L.i("toggle progress: $enabled")
        showProgress = enabled
    }

    fun async(withProgress: Boolean = true, block: suspend CoroutineScope.() -> Unit) {
        if (withProgress)
            toggleProgress(true)

        launch(UI + lifecycleJob, block = {
            try {
                block()
            } catch (e: Exception) {
                L.w(e.message)
                onErrorSnack(e)
            } finally {
                if (withProgress)
                    toggleProgress(false)
            }
        })
    }

    protected fun onErrorSnack(t: Throwable) = showSnackbarCommand(t.message ?: getString(R.string.error_unknown))

    protected fun getString(@StringRes id: Int, vararg args: Any): String = resources.getString(id, *args)

}
