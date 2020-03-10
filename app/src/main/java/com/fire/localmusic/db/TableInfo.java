package com.fire.localmusic.db;

import com.pushtorefresh.storio3.sqlite.StorIOSQLite;
import com.pushtorefresh.storio3.sqlite.impl.DefaultStorIOSQLite;
import java.util.Arrays;
import java.util.HashSet;
import com.fire.localmusic.bean.NotifyCount;
import com.fire.localmusic.bean.NotifyCountSQLiteTypeMapping;
import com.fire.localmusic.bean.SongPlay;
import com.fire.localmusic.bean.SongPlaySQLiteTypeMapping;

/**
 * Created by remix on 2018/1/12.
 * Date：2018/1/12
 * Author: remix
 * Description:
 */

public class TableInfo {

    /**
     * 生成StorIOSQLite对象的所有数据库表名
     */
    public static HashSet<String> getAllTableNameSet() {
        return new HashSet<String>(
                Arrays.asList("song_play"));
    }

    /**
     * 生成StorIOSQLite对象时配置TypeMapping
     *
     * @param {[Builder]} Builder builder
     * @return {Builder}
     */
    public static StorIOSQLite buildTypeMapping(DefaultStorIOSQLite.CompleteBuilder builder) {
        builder.addTypeMapping(SongPlay.class,new SongPlaySQLiteTypeMapping());
        builder.addTypeMapping(NotifyCount.class,new NotifyCountSQLiteTypeMapping());
        //NotifyCount
        return builder.build();
    }
}
