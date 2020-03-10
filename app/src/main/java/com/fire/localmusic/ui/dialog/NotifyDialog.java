package com.fire.localmusic.ui.dialog;

import android.app.DialogFragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.fire.localmusic.R;
import com.fire.localmusic.adapter.NotifyAdapter;
import com.fire.localmusic.bean.NotifyCount;
import com.fire.localmusic.bean.SongPlay;
import com.fire.localmusic.bean.mp3.Song;
import com.fire.localmusic.db.DbService;
import com.fire.localmusic.interfaces.OnItemClickListener;
import com.fire.localmusic.service.MusicService;
import com.fire.localmusic.ui.activity.MainActivity;
import com.fire.localmusic.util.Constants;
import com.fire.localmusic.util.DateUtils;
import com.fire.localmusic.util.MediaStoreUtil;
import com.fire.localmusic.util.ToastUtil;

/**
 * Created by remix on 2018/5/2.
 * Date：2018/5/2
 * Author: remix
 * Description:
 */

public class NotifyDialog extends DialogFragment {

    RecyclerView mRvNotify;
    private NotifyAdapter mAdapter;
    private List<Song> mSongList = new ArrayList<>();
    private MainActivity mMainActivity;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            Bundle savedInstanceState) {
        inits();
        View view = inflater.inflate(R.layout.fragment_notify, container, false);
        mRvNotify = view.findViewById(R.id.rv_notify);
        init();
        return view;
    }

    private void init() {
        mAdapter = new NotifyAdapter(getActivity(),R.layout.item_notify_reulst);
        mRvNotify.setAdapter(mAdapter);
        mRvNotify.setLayoutManager(new LinearLayoutManager(getActivity()));
        mRvNotify.setItemAnimator(new DefaultItemAnimator());
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if(mAdapter != null && mAdapter.getDatas() != null){
                    Intent intent = new Intent(MusicService.ACTION_CMD);
                    intent.putExtra("Control", Constants.PLAY_TEMP);
                    intent.putExtra("Song",  mAdapter.getDatas().get(position));
                    getActivity().sendBroadcast(intent);
                    if (mMainActivity != null) {
                        mMainActivity.update();
                    }
                    dismiss();
                }else {
                    ToastUtil.show(getActivity(),R.string.illegal_arg);
                }
            }
            @Override
            public void onItemLongClick(View view, int position) {
            }
        });
    }

    private void inits() {
        //无标题
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        //点击边际可消失
        getDialog().setCanceledOnTouchOutside(true);
        Window window = getDialog().getWindow();
        window.getDecorView().setPadding(0, 0, 0, 0);
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams
                .FLAG_FULLSCREEN);
        window.setBackgroundDrawableResource(android.R.color.transparent);
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        lp.gravity = Gravity.CENTER;
        lp.windowAnimations = R.style.dialogAnim;
        window.setAttributes(lp);
    }

    public void setData() {
        Observable.just("")
                .map(s -> DbService.getInstance()
                        .getSongPlay())
                .subscribeOn(Schedulers.io())
                .map(songPlays -> {
                    List<Song> list = new ArrayList<>();
                    for (SongPlay songPlay : songPlays) {
                        Song song = MediaStoreUtil.getMP3InfoById(
                                Integer.parseInt(songPlay.getSongId()));
                        song.setCount(songPlay.getCount());
                        list.add(song);
                    }
                    DbService.getInstance()
                            .insertNotifyCount(
                                    new NotifyCount(DateUtils.formatDateToString(new Date(),
                                            DateUtils.dateFormat1)));
                    return list;
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    mSongList = songs;
                    mAdapter.setData(mSongList);
                }, throwable -> {
                    Log.e("TAGTAG",throwable.getMessage());
                });
    }

    @Override
    public void onResume() {
        super.onResume();
       setData();
    }

    public void setActivity(MainActivity mainActivity) {
        mMainActivity = mainActivity;
    }
}
