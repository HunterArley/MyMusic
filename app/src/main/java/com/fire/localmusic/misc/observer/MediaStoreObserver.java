package com.fire.localmusic.misc.observer;

import android.net.Uri;
import android.os.Handler;

import com.fire.localmusic.util.Constants;
import com.fire.localmusic.util.Global;
import com.fire.localmusic.util.MediaStoreUtil;

/**
 * Created by taeja on 16-3-30.
 */
public class MediaStoreObserver extends BaseObserver {
    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public MediaStoreObserver(Handler handler) {
        super(handler);
    }

    @Override
    void onAccept(Uri uri) {
        Global.AllSongList = MediaStoreUtil.getAllSongsId();
//        Global.FolderMap = MediaStoreUtil.getFolder();
//        Global.AllSongList = MediaStoreUtil.getAllSongsIdWithFolder();
        mHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
    }

    @Override
    boolean onFilter(Uri uri) {
        return uri != null && uri.toString().contains("content://media/");
    }
}
