package com.fire.localmusic.menu;

import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.audiofx.AudioEffect;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.common.util.ByteConstants;
import com.umeng.analytics.MobclickAgent;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.fire.localmusic.R;
import com.fire.localmusic.bean.mp3.Genre;
import com.fire.localmusic.bean.mp3.PlayListSong;
import com.fire.localmusic.bean.mp3.Song;
import com.fire.localmusic.service.MusicService;
import com.fire.localmusic.theme.ThemeStore;
import com.fire.localmusic.ui.activity.EQActivity;
import com.fire.localmusic.ui.activity.PlayerActivity;
import com.fire.localmusic.ui.dialog.FileChooserDialog;
import com.fire.localmusic.ui.dialog.TimerDialog;
import com.fire.localmusic.ui.fragment.LyricFragment;
import com.fire.localmusic.util.Constants;
import com.fire.localmusic.util.Global;
import com.fire.localmusic.util.MediaStoreUtil;
import com.fire.localmusic.util.PlayListUtil;
import com.fire.localmusic.util.SPUtil;
import com.fire.localmusic.util.ToastUtil;
import com.fire.localmusic.util.Util;

import static com.afollestad.materialdialogs.DialogAction.POSITIVE;

/**
 * @ClassName AudioPopupListener
 * @Description
 * @Author Xiaoborui
 * @Date 2016/8/29 15:33
 */
public class AudioPopupListener extends ContextWrapper implements PopupMenu.OnMenuItemClickListener{
    private PlayerActivity mActivity;
    private Song mInfo;
    private View mEditRootView;
    @BindView(R.id.song_layout)
    @Nullable
    TextInputLayout mSongLayout;
    @BindView(R.id.album_layout)
    @Nullable
    TextInputLayout mAlbumLayout;
    @BindView(R.id.artist_layout)
    @Nullable
    TextInputLayout mArtistLayout;
    @BindView(R.id.year_layout)
    @Nullable
    TextInputLayout mYearLayout;
    @BindView(R.id.genre_layout)
    @Nullable
    TextInputLayout mGenreLayout;

    private View mDetailRootView;
    @BindView(R.id.song_detail_path)
    @Nullable
    TextView mDetailPath;
    @BindView(R.id.song_detail_name)
    @Nullable
    TextView mDetailName;
    @BindView(R.id.song_detail_size)
    @Nullable
    TextView mDetailSize;
    @BindView(R.id.song_detail_mime)
    @Nullable
    TextView mDetailMime;
    @BindView(R.id.song_detail_duration)
    @Nullable
    TextView mDetailDuration;
    @BindView(R.id.song_detail_bit_rate)
    @Nullable
    TextView mDetailBitRate;
    @BindView(R.id.song_detail_sample_rate)
    @Nullable
    TextView mDetailSampleRate;

    private Genre mGenreInfo;

    public AudioPopupListener(PlayerActivity activity,Song info){
        super(activity);
        mActivity = activity;
        mInfo = info;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        MobclickAgent.onEvent(mActivity,item.getItemId() == R.id.menu_edit ? "SongEdit" : "SongDetail" );
        switch (item.getItemId()){
            case R.id.menu_lyric:
                final boolean alreadyIgnore = SPUtil.getValue(mActivity,SPUtil.LYRIC_KEY.LYRIC_NAME,mInfo.getId() + "",SPUtil.LYRIC_KEY.LYRIC_DEFAULT) == SPUtil.LYRIC_KEY.LYRIC_IGNORE;
                final LyricFragment lyricFragment = mActivity.getLyricFragment();
                new MaterialDialog.Builder(mActivity)
                        .items(getString(R.string.netease), getString(R.string.kugou), getString(R.string.select_lrc), getString(!alreadyIgnore ? R.string.ignore_lrc : R.string.cancel_ignore_lrc))
                        .itemsColorAttr(R.attr.text_color_primary)
                        .backgroundColorAttr(R.attr.background_color_3)
                        .itemsCallback((dialog, itemView, position, text) -> {
                            switch (position){
                                case 0: //网易 酷狗
                                case 1:
                                    SPUtil.putValue(mActivity,SPUtil.LYRIC_KEY.LYRIC_NAME,mInfo.getId() + "",position == 0 ? SPUtil.LYRIC_KEY.LYRIC_NETEASE : SPUtil.LYRIC_KEY.LYRIC_KUGOU);
                                    lyricFragment.updateLrc(mInfo,true);
                                    sendBroadcast(new Intent(MusicService.ACTION_CMD).putExtra("Control",Constants.CHANGE_LYRIC));
                                    break;
                                case 2: //手动选择歌词
                                    new FileChooserDialog.Builder(mActivity)
                                            .extensionsFilter(".lrc")
                                            .show();
                                    break;
                                case 3: //忽略或者取消忽略
                                    new MaterialDialog.Builder(mActivity)
                                            .negativeText(R.string.cancel)
                                            .negativeColorAttr(R.attr.text_color_primary)
                                            .positiveText(R.string.confirm)
                                            .positiveColorAttr(R.attr.text_color_primary)
                                            .title(!alreadyIgnore ? R.string.confirm_ignore_lrc : R.string.confirm_cancel_ignore_lrc)
                                            .titleColorAttr(R.attr.text_color_primary)
                                            .backgroundColorAttr(R.attr.background_color_3)
                                            .onPositive((dialog1, which) -> {
                                                if(!alreadyIgnore){//忽略
                                                    if (mInfo != null) {
                                                        SPUtil.putValue(mActivity,SPUtil.LYRIC_KEY.LYRIC_NAME,mInfo.getId() + "",SPUtil.LYRIC_KEY.LYRIC_IGNORE);
                                                        lyricFragment.updateLrc(mInfo);
                                                    }
                                                } else {//取消忽略
                                                    SPUtil.putValue(mActivity,SPUtil.LYRIC_KEY.LYRIC_NAME,mInfo.getId() + "",SPUtil.LYRIC_KEY.LYRIC_DEFAULT);
                                                    lyricFragment.updateLrc(mInfo);
                                                }
                                            })
                                            .show();
                                    sendBroadcast(new Intent(MusicService.ACTION_CMD).putExtra("Control",Constants.CHANGE_LYRIC));
                                    break;
                            }

                        })
                        .show();
                break;
            case R.id.menu_edit:
                MaterialDialog editDialog = new MaterialDialog.Builder(mActivity)
                        .title(R.string.song_edit)
                        .titleColorAttr(R.attr.text_color_primary)
                        .customView(R.layout.dialog_song_edit,true)
                        .negativeText(R.string.cancel)
                        .negativeColorAttr(R.attr.text_color_primary)
                        .positiveText(R.string.confirm)
                        .positiveColorAttr(R.attr.text_color_primary)
                        .backgroundColorAttr(R.attr.background_color_3)
                        .onPositive((dialog, which) -> {
                            String title = "",artist = "",album = "",genre = "",year = "";
                            title = mSongLayout.getEditText() != null ? mSongLayout.getEditText().getText().toString() : mInfo.getTitle();
                            if(TextUtils.isEmpty(title)){
                                ToastUtil.show(mActivity,R.string.song_not_empty);
                                return;
                            }
                            artist = mArtistLayout.getEditText() != null ? mArtistLayout.getEditText().getText().toString() : getString(R.string.unknown_artist);
                            album = mAlbumLayout.getEditText() != null ? mAlbumLayout.getEditText().getText().toString() : getString(R.string.unknown_album);
                            year = mYearLayout.getEditText() != null ? mYearLayout.getEditText().getText().toString() : " ";
                            genre = mGenreLayout.getEditText() != null ? mGenreLayout.getEditText().getText().toString() : "";

                            int updateRow = -1;
                            int updateGenreRow = -1;
                            try {
                                //更新歌曲等信息
                                updateRow = MediaStoreUtil.updateMP3Info(mInfo.getId(),title,artist,album,year);
                                //更新流派信息
                                //先判断是否存在该流派，如果不存在先新建该流派，再建立歌曲与流派的映射
                                if(mGenreInfo.GenreID > 0){
                                    updateGenreRow = MediaStoreUtil.updateGenre(mGenreInfo.GenreID,genre);
                                }
                                else {
                                    long genreId = MediaStoreUtil.insertGenre(genre);
                                    if(genreId != -1){
                                        updateGenreRow = MediaStoreUtil.insertGenreMap(mInfo.getId(),(int)genreId) ? 1 : -1;
                                    }
                                }
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                            if(updateGenreRow > 0 && updateRow > 0){
                                ToastUtil.show(mActivity,R.string.save_success);
                                mInfo.setAlbum(album);
                                mInfo.setArtist(artist);
                                mInfo.setTitle(title);
                                mInfo.setYear(year);
                                mActivity.updateTopStatus(mInfo);
                                mActivity.setMP3Item(mInfo);
                            } else {
                                ToastUtil.show(mActivity,R.string.save_error);
                            }
                        }).build();
                editDialog.show();

                mEditRootView = editDialog.getCustomView();
                if(mEditRootView != null){
                    ButterKnife.bind(AudioPopupListener.this, mEditRootView);

                    if(mSongLayout.getEditText() != null){
                        if(!ThemeStore.isDay()){
                            mSongLayout.getEditText().setTextColor(ThemeStore.getTextColorPrimary());
                            mSongLayout.getEditText().getBackground().setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
                        }
                        mSongLayout.getEditText().addTextChangedListener(new TextInputEditWatcher(mSongLayout,getString(R.string.song_not_empty)));
                        mSongLayout.getEditText().setText(mInfo.getTitle());
                    }
                    if(mAlbumLayout.getEditText() != null) {
                        if(!ThemeStore.isDay()){
                            mAlbumLayout.getEditText().setTextColor(ThemeStore.getTextColorPrimary());
                        }
                        mAlbumLayout.getEditText().setText(mInfo.getAlbum());
                    }
                    if(mArtistLayout.getEditText() != null) {
                        if(!ThemeStore.isDay()){
                            mArtistLayout.getEditText().setTextColor(ThemeStore.getTextColorPrimary());
                        }
                        mArtistLayout.getEditText().setText(mInfo.getArtist());
                    }
                    if(mYearLayout.getEditText() != null){
                        if(!ThemeStore.isDay()){
                            mYearLayout.getEditText().setTextColor(ThemeStore.getTextColorPrimary());
                        }
                        mYearLayout.getEditText().setText(mInfo.getYear());
                    }
                    mGenreInfo = MediaStoreUtil.getGenre(mInfo.getId());
                    if(mGenreLayout.getEditText() != null){
                        if(!ThemeStore.isDay()){
                            mGenreLayout.getEditText().setTextColor(ThemeStore.getTextColorPrimary());
                        }
                        mGenreLayout.getEditText().setText(mGenreInfo.GenreName);
                    }
                }
                break;

            case R.id.menu_detail:
                MaterialDialog detailDialog = new MaterialDialog.Builder(mActivity)
                        .title(R.string.song_detail)
                        .titleColorAttr(R.attr.text_color_primary)
                        .customView(R.layout.dialog_song_detail,true)
                        .positiveText(R.string.confirm)
                        .positiveColorAttr(R.attr.text_color_primary)
                        .backgroundColorAttr(R.attr.background_color_3)
                        .build();
                detailDialog.show();
                mDetailRootView = detailDialog.getCustomView();
                if(mDetailRootView != null){
                    ButterKnife.bind(AudioPopupListener.this,mDetailRootView);

                    //歌曲路径
                    if(mDetailPath != null)
                        mDetailPath.setText(mInfo.getUrl());
                    //歌曲名称
                    if(mDetailName != null)
                        mDetailName.setText(mInfo.getDisplayname());
                    //歌曲大小
                    if(mDetailSize != null)
                        mDetailSize.setText(getString(R.string.cache_size,1.0f * mInfo.getSize() / ByteConstants.MB));
                    //歌曲格式
                    if(mDetailMime != null){
                        String path = mInfo.getUrl();
                        if(!TextUtils.isEmpty(path)){
                            String extension;
                            if(path.lastIndexOf('.') > -1 && path.lastIndexOf('.') < path.length() - 1){
                                extension = mInfo.getUrl().substring(mInfo.getUrl().lastIndexOf('.') + 1 ,mInfo.getUrl().length());
                            } else {
                                extension = Util.getType(MusicService.getRateInfo(Constants.MIME));
                            }
                            mDetailMime.setText(extension);
                        }
                    }
                    //歌曲时长
                    if(mDetailDuration != null)
                        mDetailDuration.setText(Util.getTime(mInfo.getDuration()));
                    //歌曲码率
                    if(mDetailBitRate != null)
                        mDetailBitRate.setText(String.format("%s kb/s", MusicService.getRateInfo(Constants.BIT_RATE)));
                    //歌曲采样率
                    if(mDetailSampleRate != null)
                        mDetailSampleRate.setText(String.format("%s Hz", MusicService.getRateInfo(Constants.SAMPLE_RATE)));

                }
                break;
            case R.id.menu_timer:
                mActivity.startActivity(new Intent(mActivity, TimerDialog.class));
                break;
            case R.id.menu_eq:
                MobclickAgent.onEvent(mActivity,"EQ");
                Intent audioEffectIntent = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
                audioEffectIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, MusicService.getMediaPlayer().getAudioSessionId());
                if(Util.isIntentAvailable(mActivity,audioEffectIntent)){
                    mActivity.startActivityForResult(audioEffectIntent, 0);
                } else {
                    mActivity.startActivity(new Intent(mActivity,EQActivity.class));
                }
                break;
            case R.id.menu_collect:
                PlayListSong info = new PlayListSong(mInfo.getId(), Global.MyLoveID,Constants.MYLOVE);
                ToastUtil.show(mActivity,
                        PlayListUtil.addSong(info) > 0 ? getString(R.string.add_song_playlist_success, 1,Constants.MYLOVE) : getString(R.string.add_song_playlist_error));
                break;
            case R.id.menu_delete:
                new MaterialDialog.Builder(mActivity)
                        .content(R.string.confirm_delete_from_library)
                        .positiveText(R.string.confirm)
                        .negativeText(R.string.cancel)
                        .checkBoxPromptRes(R.string.delete_source, false, null)
                        .onAny((dialog, which) -> {
                            if(which == POSITIVE){
                                if(MediaStoreUtil.delete(mInfo.getId() , Constants.SONG,dialog.isPromptCheckBoxChecked()) > 0){
                                    if(PlayListUtil.deleteSong(mInfo.getId(), Global.PlayQueueID)){
                                        ToastUtil.show(mActivity, getString(R.string.delete_success));
                                        //移除的是正在播放的歌曲
                                        if(MusicService.getCurrentMP3() == null)
                                            return;
                                        if(mInfo.getId() == MusicService.getCurrentMP3().getId() && Global.PlayQueue.size() >= 2){
                                            Intent intent = new Intent(MusicService.ACTION_CMD);
                                            intent.putExtra("Control", Constants.NEXT);
                                            mActivity.sendBroadcast(intent);
                                        }
                                    }
                                } else {
                                    ToastUtil.show(mActivity, getString(R.string.delete_error));
                                }
                            }
                        })
                        .backgroundColorAttr(R.attr.background_color_3)
                        .positiveColorAttr(R.attr.text_color_primary)
                        .negativeColorAttr(R.attr.text_color_primary)
                        .contentColorAttr(R.attr.text_color_primary)
                        .show();
                break;
//            case R.id.menu_vol:
//                AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
//                if(audioManager != null){
//                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,audioManager.getStreamVolume(AudioManager.STREAM_MUSIC),AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
//                }
        }
        return true;
    }

    private class TextInputEditWatcher implements TextWatcher{
        private TextInputLayout mInputLayout;
        private String mError;
        TextInputEditWatcher(TextInputLayout layout,String error){
            mError = error;
            mInputLayout = layout;
        }
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if(s == null || TextUtils.isEmpty(s.toString())){
                mInputLayout.setError(mError);
            }else {
                mInputLayout.setError("");
            }
        }
    }
}
