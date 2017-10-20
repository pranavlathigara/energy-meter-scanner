package ds.meterscanner.mvvm.viewmodel

import L
import ds.databinding.binding
import ds.meterscanner.coroutines.listenValues
import ds.meterscanner.db.model.Snapshot
import ds.meterscanner.mvvm.BindableViewModel
import ds.meterscanner.mvvm.Command
import ds.meterscanner.mvvm.ListsView

class HistoryViewModel : BindableViewModel() {

    var listItems: List<Snapshot> by binding(emptyList())
    var isActionMode: Boolean by binding()

    val scrollToPositionCommand = Command<Int>()

    init {
        listenSnapshots()
    }

    private fun listenSnapshots() = async {
        try {
            toggleProgress(true)
            val channel = db.getSnapshots().listenValues<Snapshot>()
            for (data in channel) {
                L.d("list updated! size=${data.size}")
                toggleProgress(false)
                listItems = data
                scrollToPositionCommand(data.size - 1)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onErrorSnack(e)
        }
    }

    fun onNewSnapshot(view: ListsView) = view.navigateDetails(null)

    fun deleteSelectedItems() = db.deleteSnapshots(listItems.filter { it.selected })

    fun getSeledtedItemsCount() = listItems.filter { it.selected }.size

}
