package ds.meterscanner.ui

import L
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import com.evernote.android.job.scheduledTo
import com.github.salomonbrys.kodein.Kodein
import com.github.salomonbrys.kodein.KodeinAware
import com.github.salomonbrys.kodein.android.appKodein
import com.github.salomonbrys.kodein.instance
import ds.bindingtools.startActivity
import ds.meterscanner.db.FirebaseDb
import ds.meterscanner.db.model.Snapshot
import ds.meterscanner.mvvm.view.MainActivity
import ds.meterscanner.net.NetLayer
import ds.meterscanner.scheduler.Scheduler
import ds.meterscanner.util.*
import io.palaima.debugdrawer.DebugDrawer
import io.palaima.debugdrawer.actions.ActionsModule
import io.palaima.debugdrawer.actions.ButtonAction
import io.palaima.debugdrawer.actions.SpinnerAction
import io.palaima.debugdrawer.actions.SwitchAction
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.launch
import java.util.*


class DebugDrawerController(private val activity: AppCompatActivity) : KodeinAware {
    override val kodein: Kodein = activity.appKodein()

    private val debugDrawer: DebugDrawer
    val db: FirebaseDb = instance()
    private val netLayer: NetLayer = instance()
    val scheduler: Scheduler = instance()

    init {
        val keepSyncedAction = SwitchAction("Keep synced", {
            db.keepSynced(it)
        })

        val spinnerAction = SpinnerAction(
            arrayListOf("First", "Second", "Third"),
            { value -> activity.toast("Spinner item selected - $value") }
        )

        val createRecordAction = ButtonAction("Create Record", {
            val rnd = Random()
            val s = Snapshot(
                //rawValue = "12345",
                value = 12345.0,
                outsideTemp = -20 + rnd.nextInt(50),
                boilerTemp = rnd.nextInt(30) + 50
            )

            db.saveSnapshot(s)

        })

        val getWeatherAction = ButtonAction("Get Weather", {
            launch(UI) {
                val weather = netLayer.getWeather().main.temp
                L.v("temp=$weather°С")
                activity.toast("temp=$weather")
            }
        })

        val activeTasks = TextAction(getTasksInfo())

        val simulateTaskAction = ButtonAction("Run Job", {
            activity.toast("todo")
        })

        val clearJobsListAction = ButtonAction("Clear Jobs List", {
            scheduler.clearAllJobs()
        })

        val runMainActivityAction = ButtonAction("Run MainActivity", {
            postDelayed(100) {
                activity.app.startActivity<MainActivity>(flags = Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        })

        debugDrawer = DebugDrawer.Builder(activity)
            .modules(
                ActionsModule(createRecordAction, getWeatherAction, simulateTaskAction, clearJobsListAction, activeTasks, runMainActivityAction)
            ).build()
    }

    private fun getTasksInfo(): String {
        val jobs = scheduler.getScheduledJobs()
        val sb = StringBuilder()
        sb.append("Active tasks: ${jobs.size}\n")
        for (job in jobs) {
            sb.append("id=${job.jobId} interval=${job.startMs / 1000 / 60} scheduled bind=${formatTimeDate(job.scheduledTo())}\n")
        }
        return sb.toString()
    }

}