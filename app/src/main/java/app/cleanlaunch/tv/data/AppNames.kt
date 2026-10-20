package app.cleanlaunch.tv.data

import java.util.Locale

/**
 * English names for the index. A custom rename from Edit page wins over this map,
 * and a label that is not listed here is shown as the app reports it.
 */
object AppNames {
    fun english(packageName: String?, label: String): String {
        packageName?.let { byPackage[it] }?.let { return it }
        byLabel[normalize(label)]?.let { return it }
        return label
    }

    fun normalize(label: String): String {
        return label.lowercase(Locale.US).replace(Regex("[\\s·•\\-_/]+"), "")
    }

    private val byPackage = mapOf(
        "com.google.android.youtube.tv" to "YouTube",
        "com.google.android.youtube" to "YouTube",
        "com.google.android.youtube.tvmusic" to "YouTube Music",
        "com.google.android.apps.youtube.music" to "YouTube Music",
        "com.android.vending" to "Play Store",
        "com.google.android.play.games" to "Play Games",
        "com.android.tv.settings" to "Settings",
        "com.android.settings" to "Settings",
        "com.google.android.tv.remote.service" to "TV Remote",
        "com.google.android.tv.remote" to "TV Remote",
        "com.sony.dtv.notificationcenter" to "Notifications",
        "com.sony.dtv.bravialibrary" to "Notifications",
        "com.sony.dtv.sonyselect" to "Sony Select",
        "com.cibn.tv" to "Kumiao",
        "com.cibn.tv.ott" to "Kumiao",
        "com.youku.tv" to "Youku",
        "com.yunos.tv.youku" to "Youku",
        "com.hunantv.imgo.activity" to "Mango TV",
        "com.mgtv.tv" to "Mango TV",
        "com.starcor.mango" to "Mango TV",
        "com.spocky.projengmenu" to "Projectivy",
        "com.hpplay.happyplay.aw" to "Lebo",
        "com.hpplay.happyplay" to "Lebo",
        "com.lebo.cast" to "Lebo",
        "com.ktcp.video" to "Tencent Video",
        "com.ktcp.csvideo" to "Tencent Video",
        "com.gitvdemo.video" to "iQIYI",
        "com.gala.video" to "iQIYI",
        "tv.danmaku.bili" to "Bilibili",
        "com.xiaodianshi.tv.yst" to "Bilibili",
        "com.cctv.yangshipin.app.androidp" to "CCTV",
        "com.wasu.wasutv" to "Wasu TV",
        "com.dangbeimarket" to "Dangbei Market",
        "com.cmvideo.cmcc.tv" to "Migu Video",
        "com.google.android.videos" to "Google TV",
        "com.android.documentsui" to "Files",
    )

    private val byLabel = mapOf(
        "cibn酷喵" to "Kumiao",
        "酷喵" to "Kumiao",
        "mgtv芒果tv" to "Mango TV",
        "芒果tv" to "Mango TV",
        "bravia通知" to "Notifications",
        "通知" to "Notifications",
        "androidtvremoteservice" to "TV Remote",
        "云视听极光" to "Tencent Video",
        "雲視聽極光" to "Tencent Video",
        "腾讯视频" to "Tencent Video",
        "騰訊視頻" to "Tencent Video",
        "银河奇异果" to "iQIYI",
        "銀河奇異果" to "iQIYI",
        "爱奇艺" to "iQIYI",
        "愛奇藝" to "iQIYI",
        "优酷" to "Youku",
        "優酷" to "Youku",
        "哔哩哔哩" to "Bilibili",
        "嗶哩嗶哩" to "Bilibili",
        "央视频" to "CCTV",
        "央視頻" to "CCTV",
        "咪咕视频" to "Migu Video",
        "咪咕視頻" to "Migu Video",
        "华数tv" to "Wasu TV",
        "華數tv" to "Wasu TV",
        "当贝市场" to "Dangbei Market",
        "當貝市場" to "Dangbei Market",
        "索尼精选" to "Sony Select",
        "索尼精選" to "Sony Select",
        "设置" to "Settings",
        "設定" to "Settings",
        "文件" to "Files",
        "檔案" to "Files",
        "相册" to "Photos",
        "相冊" to "Photos",
        "屏幕镜像" to "Cast",
        "螢幕鏡像" to "Cast",
        "googleplay游戏" to "Play Games",
        "googleplay商店" to "Play Store",
    )
}
