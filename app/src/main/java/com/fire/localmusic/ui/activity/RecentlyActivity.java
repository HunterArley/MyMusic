package com.fire.localmusic.ui.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.view.View;

import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.fire.localmusic.R;
import com.fire.localmusic.adapter.SongAdapter;
import com.fire.localmusic.asynctask.AppWrappedAsyncTaskLoader;
import com.fire.localmusic.bean.mp3.Song;
import com.fire.localmusic.helper.UpdateHelper;
import com.fire.localmusic.interfaces.LoaderIds;
import com.fire.localmusic.interfaces.OnItemClickListener;
import com.fire.localmusic.misc.handler.MsgHandler;
import com.fire.localmusic.misc.handler.OnHandleMessage;
import com.fire.localmusic.service.MusicService;
import com.fire.localmusic.ui.customview.fastcroll_recyclerview.FastScrollRecyclerView;
import com.fire.localmusic.util.Constants;
import com.fire.localmusic.util.Global;
import com.fire.localmusic.util.MediaStoreUtil;

/**
 * Created by taeja on 16-3-4.
 */

/**
 * 最近添加歌曲的界面
 * 目前为最近7天添加
 */
public class RecentlyActivity extends PermissionActivity<Song,SongAdapter> implements UpdateHelper.Callback{
    public static final String TAG = RecentlyActivity.class.getSimpleName();

    @BindView(R.id.recently_placeholder)
    View mPlaceHolder;
    @BindView(R.id.recyclerview)
    FastScrollRecyclerView mRecyclerView;
    private ArrayList<Integer> mIdList = new ArrayList<>();

    private MsgHandler mHandler;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        MobclickAgent.onEvent(this,"RecentlyAdd");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recently);
        ButterKnife.bind(this);

        mHandler = new MsgHandler(this);

        mAdapter = new SongAdapter(this, R.layout.item_song_recycle,mMultiChoice,SongAdapter.RECENTLY,mRecyclerView);
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                int id = getSongId(position);
                if(id > 0 && !mMultiChoice.itemAddorRemoveWithClick(view,position,id,TAG)){
                    Intent intent = new Intent(MusicService.ACTION_CMD);
                    Bundle arg = new Bundle();
                    arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                    arg.putInt("Position", position);
                    intent.putExtras(arg);
                    Global.setPlayQueue(mIdList,mContext,intent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                int id = getSongId(position);
                if(id > 0) {
                    mMultiChoice.itemAddorRemoveWithLongClick(view, position, id, TAG,
                            Constants.SONG);
                }
            }
        });

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);

        setUpToolbar(mToolBar,getString(R.string.recently));
    }

    /**
     * 获得歌曲id
     * @param position
     * @return
     */
    private int getSongId(int position){
        int id = -1;
        if(mAdapter.getDatas() != null && mAdapter.getDatas().size() > position - 1){
            id = mAdapter.getDatas().get(position).getId();
        }
        return id;
    }

    @Override
    public void onBackPressed() {
        if(mMultiChoice.isShow()) {
            onMultiBackPress();
        } else {
            finish();
        }
    }

    @Override
    public void UpdateUI(Song Song, boolean isplay) {
//        if(mAdapter != null)
//            mAdapter.onUpdateHighLight();
    }

    @Override
    public void onLoadFinished(android.content.Loader<List<Song>> loader, List<Song> data) {
        super.onLoadFinished(loader, data);
        if(data != null){
            mIdList = new ArrayList<>();
            for(Song song : data){
                mIdList.add(song.getId());
            }
            mRecyclerView.setVisibility(data.size() > 0 ? View.VISIBLE : View.GONE);
            mPlaceHolder.setVisibility(data.size() > 0 ? View.GONE : View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.GONE);
            mPlaceHolder.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        MobclickAgent.onPageStart(RecentlyActivity.class.getSimpleName());
        super.onResume();
        if(mMultiChoice.isShow()){
            mHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
        }
    }

    @Override
    protected void onPause() {
        MobclickAgent.onPageEnd(RecentlyActivity.class.getSimpleName());
        super.onPause();
        if(mMultiChoice.isShow()){
            mHandler.sendEmptyMessageDelayed(Constants.CLEAR_MULTI,500);
        }
    }

    @OnHandleMessage
    public void handleMessage(Message msg){
        switch (msg.what){
            case Constants.CLEAR_MULTI:
                mMultiChoice.clearSelectedViews();
                break;
            case Constants.UPDATE_ADAPTER:
                if(mAdapter != null)
                    mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected android.content.Loader<List<Song>> getLoader() {
        return new AsyncRecentlySongLoader(this);
    }

    @Override
    protected int getLoaderId() {
        return LoaderIds.RECENTLY_ACTIVITY;
    }

    private static class AsyncRecentlySongLoader extends AppWrappedAsyncTaskLoader<List<Song>> {
        private AsyncRecentlySongLoader(Context context) {
            super(context);
        }

        @Override
        public List<Song> loadInBackground() {
            return getLastAddedSongs();
        }

        @NonNull
        private List<Song> getLastAddedSongs() {
           return MediaStoreUtil.getLastAddedSong();
        }
    }
}
