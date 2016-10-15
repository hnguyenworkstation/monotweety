package net.yslibrary.monotweety.status.adapter

import android.support.v7.util.DiffUtil
import com.hannesdorfmann.adapterdelegates3.ListDelegationAdapter
import com.twitter.sdk.android.core.models.Tweet
import rx.Single
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import timber.log.Timber

/**
 * Created by yshrsmz on 2016/10/13.
 */
class ComposeStatusAdapter(private val listener: Listener) : ListDelegationAdapter<List<ComposeStatusAdapter.Item>>() {

  var editorInitialized = false

  init {
    delegatesManager.addDelegate(PreviousStatusAdapterDelegate())

    delegatesManager.addDelegate(EditorAdapterDelegate(object : EditorAdapterDelegate.Listener {
      override fun onStatusChanged(status: String) {
        listener.onStatusChanged(status)
      }

      override fun onEnableThreadChanged(enabled: Boolean) {
        listener.onEnableThreadChanged(enabled)
      }

      override fun onKeepDialogOpenChanged(enabled: Boolean) {
        listener.onKeepDialogOpenChanged(enabled)
      }
    }))

    items = emptyList()
  }

  private fun editorItem(): EditorAdapterDelegate.Item {
    return items.last() as EditorAdapterDelegate.Item
  }

  fun setPreviousStatus(tweets: List<Tweet>) {
    val tweetItems = tweets.map {
      PreviousStatusAdapterDelegate.Item(
          id = it.id,
          status = it.text,
          createdAt = it.createdAt)
    }

    notifyChange(items, tweetItems + items.last())
  }

  fun updateEditor(item: EditorAdapterDelegate.Item) {
    notifyChange(items, items.dropLast(1) + item)
  }

  fun initializeEditor(status: String, keepDialogOpen: Boolean) {
    if (editorInitialized) {
      Timber.w("Editor is already initialized")
      return
    }
    editorInitialized = true

    val item = EditorAdapterDelegate.Item(
        status = status,
        statusLength = 0,
        maxLength = 0,
        keepDialogOpen = keepDialogOpen,
        enableThread = false,
        valid = false,
        initialValue = true,
        clear = false)

    if (items.isEmpty() || items.last().viewType != ViewType.EDITOR) {
      notifyChange(items, items + item)
    } else {
      notifyChange(items, items.dropLast(1) + item)
    }
  }

  fun clearEditor() {
    updateEditor(editorItem()
        .copy(status = "",
            statusLength = 0,
            valid = false,
            initialValue = false,
            clear = true))
  }

  fun updateStatusCounter(valid: Boolean, length: Int, maxLength: Int) {
    updateEditor(editorItem()
        .copy(valid = valid,
            statusLength = length,
            maxLength = maxLength))
  }

  fun notifyChange(oldList: List<Item>, newList: List<Item>) {
    Single.fromCallable {
      DiffUtil.calculateDiff(object : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
          val oldItem = oldList[oldItemPosition]
          val newItem = newList[newItemPosition]

          if (oldItem.viewType == newItem.viewType) {
            if (newItem.viewType == ComposeStatusAdapter.ViewType.EDITOR) {
              return true
            } else if (oldItem is PreviousStatusAdapterDelegate.Item && newItem is PreviousStatusAdapterDelegate.Item) {
              return oldItem.id == newItem.id
            }
          }

          return false
        }

        override fun getOldListSize(): Int {
          return oldList.size
        }

        override fun getNewListSize(): Int {
          return newList.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
          val oldItem = oldList[oldItemPosition]
          val newItem = newList[newItemPosition]

          if (oldItem.viewType == newItem.viewType) {
            if (oldItem is EditorAdapterDelegate.Item && newItem is EditorAdapterDelegate.Item) {
              return oldItem == newItem
            } else if (oldItem is PreviousStatusAdapterDelegate.Item && newItem is PreviousStatusAdapterDelegate.Item) {
              return oldItem.id == newItem.id
            }
          }
          return false
        }
      })
    }
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe {
          items = newList
          it.dispatchUpdatesTo(this)
        }
  }

  interface Item {
    val viewType: ViewType
  }

  enum class ViewType {
    PREVIOUS_STATUS,
    EDITOR
  }

  interface Listener {
    fun onStatusChanged(status: String)
    fun onEnableThreadChanged(enabled: Boolean)
    fun onKeepDialogOpenChanged(enabled: Boolean)
  }
}