package app.cleanlaunch.tv.data

import android.service.notification.NotificationListenerService

/**
 * An empty notification listener. Android only lets an app read the active media
 * sessions of other apps when it holds notification access, so this component exists
 * to be granted that access. It reads no notifications.
 */
class NowPlayingListener : NotificationListenerService() {
    override fun onListenerConnected() {
        NowPlayingHub.connected?.invoke()
    }

    override fun onListenerDisconnected() {
        NowPlayingHub.connected?.invoke()
    }
}

/** Lets the listener service nudge the view model without holding a reference to it. */
object NowPlayingHub {
    @Volatile
    var connected: (() -> Unit)? = null
}
