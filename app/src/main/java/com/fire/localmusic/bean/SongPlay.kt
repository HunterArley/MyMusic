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
@StorIOSQLiteType(table = "song_play")
class SongPlay @StorIOSQLiteCreator constructor(
        @StorIOSQLiteColumn(name = "song_id", key = true)
        var songId: String,
        @StorIOSQLiteColumn(name = "count")
        var count: Int) : Serializable {

    override fun toString(): String {
        return "SongPlay{" +
                "songId='" + songId + '\''.toString() +
                ", count=" + count +
                '}'.toString()
    }
    companion object {
        val __TABLE__ = "song_play"
        val SONG_ID = "song_id"
        val C_COUNT = "count"
    }
}
