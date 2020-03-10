package com.fire.localmusic.bean

import com.pushtorefresh.storio3.sqlite.annotations.StorIOSQLiteColumn
import com.pushtorefresh.storio3.sqlite.annotations.StorIOSQLiteCreator
import com.pushtorefresh.storio3.sqlite.annotations.StorIOSQLiteType
import java.io.Serializable

/**
 * Created by remix on 2018/5/2.
 * Date：2018/5/2
 * Author: remix
 * Description:
 */
@StorIOSQLiteType(table = "notify_count")
class NotifyCount @StorIOSQLiteCreator constructor(
        @StorIOSQLiteColumn(name = "time", key = true)
        var time: String) : Serializable{

    companion object {
        val __TABLE__ = "notify_count"
        val TIME = "time"
    }

}