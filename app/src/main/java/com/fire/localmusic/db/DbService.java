package com.fire.localmusic.db;

import com.pushtorefresh.storio3.sqlite.StorIOSQLite;
import com.pushtorefresh.storio3.sqlite.operations.put.PutResult;
import com.pushtorefresh.storio3.sqlite.queries.Query;
import com.pushtorefresh.storio3.sqlite.queries.RawQuery;
import java.util.List;
import com.fire.localmusic.App;
import com.fire.localmusic.bean.NotifyCount;
import com.fire.localmusic.bean.SongPlay;

/**
 * Created by remix on 2018/5/2.
 * Date：2018/5/2
 * Author: remix
 * Description:
 */

public class DbService {

    private static final DbService ourInstance = new DbService();
    private final StorIOSQLite mStorIOSQLite;

    public static DbService getInstance() {
        return ourInstance;
    }

    private DbService() {
        mStorIOSQLite = App.getInstance().getStorIOSQLite();
    }

    public SongPlay getSongPlay(String key) {
            return mStorIOSQLite.get()
                    .object(SongPlay.class)
                    .withQuery(Query.builder()
                    .table(SongPlay.Companion.get__TABLE__())
                    .where(String.format("%s = ?", SongPlay.Companion.getSONG_ID()))
                    .whereArgs(key)
                    .build())
                    .prepare()
                    .executeAsBlocking();
    }

    public List<SongPlay> getSongPlay() {
        String query = String.format("SELECT * FROM %s ORDER BY %s DESC",SongPlay.Companion.get__TABLE__(),SongPlay.Companion.getC_COUNT());
        return mStorIOSQLite.get()
                .listOfObjects(SongPlay.class)
                .withQuery(RawQuery.builder().query(query).build())
                .prepare()
                .executeAsBlocking();
    }

    public PutResult insertSongPlay(SongPlay songPlay){
        return mStorIOSQLite.put()
                .object(songPlay)
                .prepare()
                .executeAsBlocking();
    }

    public PutResult insertNotifyCount(NotifyCount notifyCount) {
        return mStorIOSQLite.put()
                .object(notifyCount)
                .prepare()
                .executeAsBlocking();
    }

    public NotifyCount getNotifyCount(String time) {
        return mStorIOSQLite.get()
                .object(NotifyCount.class)
                .withQuery(Query.builder()
                        .table(NotifyCount.Companion.get__TABLE__())
                        .where(String.format("%s = ?", NotifyCount.Companion.getTIME()))
                        .whereArgs(time)
                        .build())
                .prepare()
                .executeAsBlocking();
    }
}
