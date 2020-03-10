package com.fire.localmusic.ui.activity;

import android.content.Context;
import android.content.Loader;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.fire.localmusic.R;
import com.fire.localmusic.adapter.SongChooseAdaper;
import com.fire.localmusic.asynctask.AppWrappedAsyncTaskLoader;
import com.fire.localmusic.bean.mp3.Song;
import com.fire.localmusic.interfaces.LoaderIds;
import com.fire.localmusic.theme.ThemeStore;
import com.fire.localmusic.util.ColorUtil;
import com.fire.localmusic.util.MediaStoreUtil;
import com.fire.localmusic.util.PlayListUtil;
import com.fire.localmusic.util.ToastUtil;

/**
 * @ClassName SongChooseActivity
 * @Description 新建列表后添加歌曲
 * @Author Xiaoborui
 * @Date 2016/10/21 09:34
 */

public class SongChooseActivity extends PermissionActivity<Song,SongChooseAdaper> {
    public static final String TAG = SongChooseActivity.class.getSimpleName();

    private int mPlayListID;
    private String mPlayListName;
    @BindView(R.id.confirm)
    TextView mConfirm;
    @BindView(R.id.recyclerview)
    RecyclerView mRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_song_choose);
        ButterKnife.bind(this);

        mPlayListID = getIntent().getIntExtra("PlayListID",-1);
        if(mPlayListID <= 0){
            ToastUtil.show(this, R.string.add_error, Toast.LENGTH_SHORT);
            return;
        }
        mPlayListName = getIntent().getStringExtra("PlayListName");

        TextView cancel = findView(R.id.cancel);
        cancel.setTextColor(ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));
        mConfirm.setTextColor(ColorUtil.getColor(ThemeStore.isLightTheme() ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));
        mAdapter = new SongChooseAdaper(this,R.layout.item_song_choose, isValid -> {
            mConfirm.setAlpha(isValid ? 1.0f : 0.6f);
            mConfirm.setClickable(isValid);
        });

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mConfirm.setAlpha(0.6f);

    }

    @OnClick({R.id.confirm,R.id.cancel})
    public void onClick(View v){
        switch (v.getId()){
            case R.id.cancel:
                finish();
                break;
            case R.id.confirm:
                if(mAdapter.getCheckedSong() == null || mAdapter.getCheckedSong().size() == 0){
                    ToastUtil.show(this,R.string.choose_no_song);
                    return;
                }
                final int num = PlayListUtil.addMultiSongs(mAdapter.getCheckedSong(),mPlayListName,mPlayListID);
                ToastUtil.show(this,getString(R.string.add_song_playlist_success, num,mPlayListName));
                finish();
        }
    }

    @Override
    protected int getLoaderId() {
        return LoaderIds.SONGCHOOSE_ACTIVITY;
    }

    @Override
    protected void setUpToolbar(Toolbar toolbar, String title) {
    }

    @Override
    protected void setUpMultiChoice() {
    }

    @Override
    protected Loader<List<Song>> getLoader() {
        return new AsyncSongLoader(mContext);
    }

    private static class AsyncSongLoader extends AppWrappedAsyncTaskLoader<List<Song>> {
        private AsyncSongLoader(Context context) {
            super(context);
        }

        @Override
        public List<Song> loadInBackground() {
            return MediaStoreUtil.getAllSong();
        }
    }

}
