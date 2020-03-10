package com.fire.localmusic.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.umeng.analytics.MobclickAgent;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import com.fire.localmusic.R;
import com.fire.localmusic.adapter.ChildHolderAdapter;
import com.fire.localmusic.bean.mp3.Song;
import com.fire.localmusic.helper.SortOrder;
import com.fire.localmusic.helper.UpdateHelper;
import com.fire.localmusic.interfaces.LoaderIds;
import com.fire.localmusic.interfaces.OnItemClickListener;
import com.fire.localmusic.misc.handler.MsgHandler;
import com.fire.localmusic.misc.handler.OnHandleMessage;
import com.fire.localmusic.service.MusicService;
import com.fire.localmusic.theme.ThemeStore;
import com.fire.localmusic.ui.customview.fastcroll_recyclerview.FastScrollRecyclerView;
import com.fire.localmusic.ui.fragment.BottomActionBarFragment;
import com.fire.localmusic.util.ColorUtil;
import com.fire.localmusic.util.Constants;
import com.fire.localmusic.util.Global;
import com.fire.localmusic.util.MediaStoreUtil;
import com.fire.localmusic.util.PlayListUtil;
import com.fire.localmusic.util.SPUtil;

/**
 * Created by Remix on 2015/12/4.
 */

/**
 * 专辑、艺术家、文件夹、播放列表详情
 */
public class ChildHolderActivity extends PermissionActivity<Song,ChildHolderAdapter> implements UpdateHelper.Callback{
    public final static String TAG = ChildHolderActivity.class.getSimpleName();
    public final static String TAG_PLAYLIST_SONG = ChildHolderActivity.class.getSimpleName() + "Song";
    private boolean mIsRunning = false;
    //获得歌曲信息列表的参数
    public static int mId;
    private int mType;
    private String mArg;
    private List<Song> mInfoList;

    //歌曲数目与标题
    @BindView(R.id.childholder_item_num)
    TextView mNum;
    @BindView(R.id.child_holder_recyclerView)
    FastScrollRecyclerView mRecyclerView;
    @BindView(R.id.toolbar)
    Toolbar mToolBar;

    private String Title;
    private BottomActionBarFragment mBottombar;

    private MaterialDialog mMDDialog;

    //当前排序
    private String mSortOrder;
    //更新
    private static final int START = 0;
    private static final int END = 1;
    private MsgHandler mRefreshHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_child_holder);
        ButterKnife.bind(this);

        mRefreshHandler = new MsgHandler(this);
        //参数id，类型，标题
        mId = getIntent().getIntExtra("Id", -1);
        mType = getIntent().getIntExtra("Type", -1);
        mArg = getIntent().getStringExtra("Title");

        mAdapter = new ChildHolderAdapter(this,R.layout.item_child_holder,mType,mArg,mMultiChoice,mRecyclerView);
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                if(position < 0 || mInfoList == null || position >= mInfoList.size())
                    return;
                int songId = mInfoList.get(position).getId();
                if(!mMultiChoice.itemAddorRemoveWithClick(view,position,songId,mType == Constants.PLAYLISTSONG ? TAG_PLAYLIST_SONG : TAG)){
                    if (mInfoList != null && mInfoList.size() == 0)
                        return;
                    ArrayList<Integer> idList = new ArrayList<>();
                    for (Song info : mInfoList) {
                        if(info != null && info.getId() > 0)
                            idList.add(info.getId());
                    }
                    //设置正在播放列表
                    Intent intent = new Intent(MusicService.ACTION_CMD);
                    Bundle arg = new Bundle();
                    arg.putInt("Control", Constants.PLAYSELECTEDSONG);
                    arg.putInt("Position", position);
                    intent.putExtras(arg);
                    Global.setPlayQueue(idList,mContext,intent);

//                    startActivity(new Intent(mContext,CustomSortActivity.class)
//                            .putExtra("list",new ArrayList<>(mInfoList))
//                            .putExtra("name",mArg)
//                            .putExtra("id",mId);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                mMultiChoice.itemAddorRemoveWithLongClick(view,position,mInfoList.get(position).getId(), TAG,mType == Constants.PLAYLIST ? Constants.PLAYLISTSONG : Constants.SONG);
            }
        });

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setBubbleTextColor(ThemeStore.isLightTheme()
                ? ColorUtil.getColor(R.color.white)
                : ThemeStore.getTextColorPrimary());

        //歌曲数目与标题
        if(mType != Constants.FOLDER) {
            if(mArg.contains("unknown")){
                if(mType == Constants.ARTIST)
                    Title = getString(R.string.unknown_artist);
                else if(mType == Constants.ALBUM){
                    Title = getString(R.string.unknown_album);
                }
            } else {
                Title = mArg;
            }
        } else
            Title = mArg.substring(mArg.lastIndexOf("/") + 1,mArg.length());
        //初始化toolbar
        setUpToolbar(mToolBar,Title);

        //加载歌曲列表

        mMDDialog = new MaterialDialog.Builder(this)
                .title(R.string.loading)
                .titleColorAttr(R.attr.text_color_primary)
                .content(R.string.please_wait)
                .contentColorAttr(R.attr.text_color_primary)
                .progress(true, 0)
                .backgroundColorAttr(R.attr.background_color_3)
                .progressIndeterminateStyle(false).build();

        //初始化底部状态栏
        mBottombar = (BottomActionBarFragment) getSupportFragmentManager().findFragmentById(R.id.bottom_actionbar_new);
        if(Global.PlayQueue == null || Global.PlayQueue.size() == 0)
            return;
        mBottombar.updateBottomStatus(MusicService.getCurrentMP3(), MusicService.isPlay());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        if(mType == Constants.PLAYLIST){
            mSortOrder = SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER, SortOrder.PlayListSongSortOrder.SONG_A_Z);
        } else if(mType == Constants.ALBUM){
            mSortOrder = SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.CHILD_ALBUM_SONG_SORT_ORDER, SortOrder.ChildHolderSongSortOrder.SONG_A_Z);
        } else if(mType == Constants.ARTIST){
            mSortOrder = SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.CHILD_ARTIST_SONG_SORT_ORDER, SortOrder.ChildHolderSongSortOrder.SONG_A_Z);
        } else {
            mSortOrder = SPUtil.getValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.CHILD_FOLDER_SONG_SORT_ORDER, SortOrder.ChildHolderSongSortOrder.SONG_A_Z);
        }
        if(TextUtils.isEmpty(mSortOrder))
            return true;
        setUpMenuItem(menu,mSortOrder);
        return true;
    }


    @Override
    protected void saveSortOrder(String sortOrder) {
        boolean update = false;
        if(mType == Constants.PLAYLIST){
            //手动排序或者排序发生变化
            if(sortOrder.equalsIgnoreCase(SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM) ||
                    !mSortOrder.equalsIgnoreCase(sortOrder)){
                //选择的是手动排序
                if(sortOrder.equalsIgnoreCase(SortOrder.PlayListSongSortOrder.PLAYLIST_SONG_CUSTOM)){
                    startActivity(new Intent(mContext,CustomSortActivity.class)
                            .putExtra("list",new ArrayList<>(mInfoList))
                            .putExtra("id",mId)
                            .putExtra("name",mArg));
                } else {
                    update = true;
                }
            }
        } else{
            //排序发生变化
            if(!mSortOrder.equalsIgnoreCase(sortOrder)){
                update = true;
            }
        }
        if(mType == Constants.PLAYLIST){
            SPUtil.putValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.CHILD_PLAYLIST_SONG_SORT_ORDER,sortOrder);
        } else if(mType == Constants.ALBUM){
            SPUtil.putValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.CHILD_ALBUM_SONG_SORT_ORDER,sortOrder);
        } else if(mType == Constants.ARTIST){
            SPUtil.putValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.CHILD_ARTIST_SONG_SORT_ORDER,sortOrder);
        } else {
            SPUtil.putValue(mContext,SPUtil.SETTING_KEY.SETTING_NAME,SPUtil.SETTING_KEY.CHILD_FOLDER_SONG_SORT_ORDER,sortOrder);
        }
        mSortOrder = sortOrder;
        if(update)
            updateList(true);

    }

    @Override
    protected int getMenuLayoutId() {
        return mType == Constants.PLAYLIST ? R.menu.menu_child_for_playlist :
                mType == Constants.ALBUM ? R.menu.menu_child_for_album :
                mType == Constants.ARTIST ? R.menu.menu_child_for_artist : R.menu.menu_child_for_folder;
    }

    @Override
    protected int getLoaderId() {
        return LoaderIds.CHILDHOLDER_ACTIVITY;
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
    public void onMediaStoreChanged() {
        updateList(true);
    }

    @Override
    public void onPlayListChanged() {
        updateList(true);
    }

    private class GetSongThread extends Thread{
        //是否需要重新查询歌曲列表
        private boolean mNeedReset = true;
        GetSongThread(boolean needReset) {
            this.mNeedReset = needReset;
        }

        @Override
        public void run() {
            mRefreshHandler.sendEmptyMessage(START);
            if(mNeedReset)
                mInfoList = getMP3List();
            mRefreshHandler.sendEmptyMessage(END);
            mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
        }
    }

    /**
     * 根据条件排序
     */
    private void sortList(){
//        Collections.sort(mInfoList, (o1, o2) -> {
//            boolean isAsc = ChildHolderAdapter.ASC_DESC == ChildHolderAdapter.ASC;
//            if(o1 == null && o2 == null)
//                return 0;
//            if(o1 == null)
//                return isAsc ? -1 : 1;
//            if(o2 == null)
//                return isAsc? 1 : -1;
//            //当前是按名字排序
//            if(ChildHolderAdapter.SORT == ChildHolderAdapter.NAME){
//                if(TextUtils.isEmpty(o1.getTitleKey()) && TextUtils.isEmpty(o2.getTitleKey()))
//                    return 0;
//                if(TextUtils.isEmpty(o1.getTitleKey()))
//                    return isAsc ? -1 : 1;
//                if(TextUtils.isEmpty(o2.getTitleKey()))
//                    return isAsc ? 1 : -1;
//                return isAsc ? o1.getTitleKey().compareTo(o2.getTitleKey()) : o2.getTitleKey().compareTo(o1.getTitleKey());
//            } else if(ChildHolderAdapter.SORT == ChildHolderAdapter.ADDTIME){
//                //当前是按添加时间排序
//                return isAsc ? Long.valueOf(o1.getAddTime()).compareTo(o2.getAddTime()) : Long.valueOf(o2.getAddTime()).compareTo(o1.getAddTime());
//            } else {
//                return 0;
//            }
//        });
    }

    /**
     * 根据参数(专辑id 歌手id 文件夹名 播放列表名)获得对应的歌曲信息列表
     * @return 对应歌曲信息列表
     */
    private List<Song> getMP3List(){
        if(mId < 0)
            return  null;
        switch (mType) {
            //专辑id
            case Constants.ALBUM:
                mInfoList = MediaStoreUtil.getMP3InfoByArg(mId, Constants.ALBUM);
                break;
            //歌手id
            case Constants.ARTIST:
                mInfoList = MediaStoreUtil.getMP3InfoByArg(mId, Constants.ARTIST);
                break;
            //文件夹名
            case Constants.FOLDER:
                mInfoList = MediaStoreUtil.getMP3ListByFolderName(mArg);
                break;
            //播放列表名
            case Constants.PLAYLIST:
                /* 播放列表歌曲id列表 */
                List<Integer> playListSongIDList = PlayListUtil.getIDList(mId);
                if(playListSongIDList == null)
                    return mInfoList;
                mInfoList = PlayListUtil.getMP3ListByIds(playListSongIDList,mId);
                break;
        }
        return mInfoList;
    }

    //更新界面
    @Override
    public void UpdateUI(Song Song, boolean isplay) {
        //底部状态兰
        mBottombar.updateBottomStatus(Song, isplay);
        //更新高亮歌曲
//        mAdapter.onUpdateHighLight();
    }

    @Override
    protected void onPause() {
        MobclickAgent.onPageEnd(ChildHolderActivity.class.getSimpleName());
        super.onPause();
        if(mMultiChoice.isShow()){
            mRefreshHandler.sendEmptyMessageDelayed(Constants.CLEAR_MULTI,500);
        }
    }

    @Override
    protected void onResume() {
        MobclickAgent.onPageStart(ChildHolderActivity.class.getSimpleName());
        super.onResume();
        mIsRunning = true;
        updateList(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsRunning = false;
    }

    private void updateList(boolean reset) {
        if(mIsRunning){
            if(mHasPermission){
                new GetSongThread(reset).start();
            }
            else {
                mInfoList = null;
                mRefreshHandler.sendEmptyMessage(Constants.UPDATE_ADAPTER);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRefreshHandler.remove();
    }

    @OnHandleMessage
    public void handleInternal(Message msg){
        switch (msg.what){
            case Constants.CLEAR_MULTI:
                mMultiChoice.clearSelectedViews();
                break;
            case Constants.UPDATE_ADAPTER:
                mAdapter.setData(mInfoList);
                mNum.setText(getString(R.string.song_count,mInfoList.size()));
                break;
            case START:
                if(mMDDialog != null && !mMDDialog.isShowing()){
                    mMDDialog.show();
                }
                break;
            case END:
                if(mMDDialog != null && mMDDialog.isShowing()){
                    mMDDialog.dismiss();
                }
                break;
        }
    }

}