package com.fire.localmusic.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.github.promeg.pinyinhelper.Pinyin;

import java.util.ArrayList;

import butterknife.BindView;
import com.fire.localmusic.R;
import com.fire.localmusic.adapter.holder.BaseViewHolder;
import com.fire.localmusic.bean.mp3.Song;
import com.fire.localmusic.interfaces.OnUpdateHighLightListener;
import com.fire.localmusic.menu.SongPopupListener;
import com.fire.localmusic.request.LibraryUriRequest;
import com.fire.localmusic.request.RequestConfig;
import com.fire.localmusic.service.MusicService;
import com.fire.localmusic.theme.Theme;
import com.fire.localmusic.theme.ThemeStore;
import com.fire.localmusic.ui.MultiChoice;
import com.fire.localmusic.ui.activity.RecentlyActivity;
import com.fire.localmusic.ui.customview.ColumnView;
import com.fire.localmusic.ui.customview.fastcroll_recyclerview.FastScroller;
import com.fire.localmusic.ui.fragment.SongFragment;
import com.fire.localmusic.util.ColorUtil;
import com.fire.localmusic.util.Constants;
import com.fire.localmusic.util.DensityUtil;
import com.fire.localmusic.util.Global;
import com.fire.localmusic.util.ToastUtil;

import static com.fire.localmusic.request.ImageUriRequest.SMALL_IMAGE_SIZE;
import static com.fire.localmusic.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * 全部歌曲和最近添加页面所用adapter
 */

/**
 * Created by Remix on 2016/4/11.
 */
public class SongAdapter extends HeaderAdapter<Song,BaseViewHolder> implements FastScroller.SectionIndexer,OnUpdateHighLightListener {
    protected MultiChoice mMultiChoice;
    private int mType;
    public static final int ALLSONG = 0;
    public static final int RECENTLY = 1;
    private final Drawable mDefaultDrawable;
    private final Drawable mSelectDrawable;

    private final RecyclerView mRecyclerView;
    private int mLastIndex = 1;

    public SongAdapter(Context context,int layoutId, MultiChoice multiChoice, int type,RecyclerView recyclerView) {
        super(context,layoutId,multiChoice);
        mMultiChoice = multiChoice;
        mType = type;
        mRecyclerView = recyclerView;
        int size = DensityUtil.dip2px(mContext,60);
        mDefaultDrawable = Theme.getShape(GradientDrawable.OVAL,Color.TRANSPARENT,size,size);
        mSelectDrawable = Theme.getShape(GradientDrawable.OVAL,ThemeStore.getSelectColor(),size,size);
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return viewType == TYPE_HEADER ?
                new HeaderHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_topbar_1,parent,false)) :
                new SongViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_song_recycle,parent,false));
    }

    @Override
    public void onViewRecycled(BaseViewHolder holder) {
        super.onViewRecycled(holder);
        if(holder instanceof SongViewHolder){
            ((SongViewHolder) holder).mImage.setImageURI(Uri.EMPTY);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void convert(BaseViewHolder baseHolder, final Song song, int position) {
        if(position == 0){
            final HeaderHolder headerHolder = (HeaderHolder) baseHolder;
            //没有歌曲时隐藏
            if(mDatas == null || mDatas.size() == 0){
                headerHolder.mRoot.setVisibility(View.GONE);
                return;
            } else {
                headerHolder.mRoot.setVisibility(View.VISIBLE);
            }

            headerHolder.mShuffle.setOnClickListener(v -> {
                Intent intent = new Intent(MusicService.ACTION_CMD);
                intent.putExtra("Control", Constants.NEXT);
                intent.putExtra("shuffle",true);
                if(mType == ALLSONG){
                    if(Global.AllSongList == null || Global.AllSongList.size() == 0){
                        ToastUtil.show(mContext,R.string.no_song);
                        return;
                    }
                    Global.setPlayQueue(Global.AllSongList,mContext,intent);
                } else {
                    ArrayList<Integer> IdList = new ArrayList<>();
                    for(int i = 0 ; i < mDatas.size();i++){
                        IdList.add(mDatas.get(i).getId());
                    }
                    if(IdList.size() == 0){
                        ToastUtil.show(mContext,R.string.no_song);
                        return;
                    }
                    Global.setPlayQueue(IdList,mContext,intent);
                }
            });
            return;
        }

        if(!(baseHolder instanceof SongViewHolder)) {
            return;
        }
        final SongViewHolder holder = (SongViewHolder) baseHolder;


        //封面
        new LibraryUriRequest(holder.mImage, getSearchRequestWithAlbumType(song),new RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()).load();

        //是否为无损
        if(!TextUtils.isEmpty(song.getDisplayname())){
            String prefix = song.getDisplayname().substring(song.getDisplayname().lastIndexOf(".") + 1);
            holder.mSQ.setVisibility(prefix.equals("flac") || prefix.equals("ape") || prefix.equals("wav")? View.VISIBLE : View.GONE);
        }

        //设置歌曲名
        holder.mName.setText(song.getTitle());

        //艺术家与专辑
        holder.mOther.setText(String.format("%s-%s", song.getArtist(), song.getAlbum()));

        //背景点击效果
        holder.mContainer.setBackground(Theme.getPressAndSelectedStateListRippleDrawable(Constants.LIST_MODEL,mContext));

        //设置按钮着色
        int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
        Theme.TintDrawable(holder.mButton,R.drawable.icon_player_more,tintColor);

        //按钮点击效果
        holder.mButton.setBackground(Theme.getPressDrawable(
                mDefaultDrawable,
                mSelectDrawable,
                ThemeStore.getRippleColor(),
                null,null));

        holder.mButton.setOnClickListener(v -> {
            if(mMultiChoice.isShow()) {
                return;
            }
            Context wrapper = new ContextThemeWrapper(mContext,Theme.getPopupMenuStyle());
            final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton, Gravity.END);
            popupMenu.getMenuInflater().inflate(R.menu.menu_song_item, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new SongPopupListener(mContext,song,false,""));
            popupMenu.show();
        });

        holder.mContainer.setOnClickListener(v -> {
            if(holder.getAdapterPosition() - 1 < 0){
                ToastUtil.show(mContext,R.string.illegal_arg);
                return;
            }
            mOnItemClickLitener.onItemClick(v, holder.getAdapterPosition() - 1);
        });
        holder.mContainer.setOnLongClickListener(v -> {
            if(holder.getAdapterPosition() - 1 < 0){
                ToastUtil.show(mContext,R.string.illegal_arg);
                return true;
            }
            mOnItemClickLitener.onItemLongClick(v,holder.getAdapterPosition() - 1);
            return true;
        });

        if(mType == ALLSONG){
            if(MultiChoice.TAG.equals(SongFragment.TAG) &&
                    mMultiChoice.mSelectedPosition.contains(position - 1)){
                mMultiChoice.addView(holder.mContainer);
            } else {
                holder.mContainer.setSelected(false);
            }
        } else {
            if(MultiChoice.TAG.equals(RecentlyActivity.TAG) &&
                    mMultiChoice.mSelectedPosition.contains(position - 1)){
                mMultiChoice.addView(holder.mContainer);
            } else {
                holder.mContainer.setSelected(false);
            }
        }
    }
    @Override
    public String getSectionText(int position) {
        if(position == 0) {
            return "";
        }
        if(mDatas != null && position - 1 < mDatas.size()){
            String title = mDatas.get(position - 1).getTitle();
            return !TextUtils.isEmpty(title) ? (Pinyin.toPinyin(title.charAt(0))).toUpperCase().substring(0,1)  : "";
        }
        return "";
    }

    /**
     * 更新高亮歌曲
     */
    @Override
    public void onUpdateHighLight() {
        Song currentSong = MusicService.getCurrentMP3();
        if(currentSong != null && mDatas != null && mDatas.indexOf(currentSong) >= 0){
            int index = mDatas.indexOf(currentSong) + 1;

            //播放的是同一首歌曲
            if(index == mLastIndex){
                return;
            }
            SongViewHolder newHolder = null;
            if(mRecyclerView.findViewHolderForAdapterPosition(index) instanceof SongViewHolder){
                newHolder = (SongViewHolder) mRecyclerView.findViewHolderForAdapterPosition(index);
            }
            SongViewHolder oldHolder = null;
            if(mRecyclerView.findViewHolderForAdapterPosition(mLastIndex) instanceof SongViewHolder){
                oldHolder = (SongViewHolder) mRecyclerView.findViewHolderForAdapterPosition(mLastIndex);
            }

            if(newHolder != null){
                newHolder.mName.setTextColor(ThemeStore.getAccentColor());
                newHolder.mColumnView.setVisibility(View.VISIBLE);
                //根据当前播放状态以及动画是否在播放，开启或者暂停的高亮动画
                if(MusicService.isPlay() && !newHolder.mColumnView.getStatus()){
                    newHolder.mColumnView.startAnim();
                }
                else if(!MusicService.isPlay() && newHolder.mColumnView.getStatus()){
                    newHolder.mColumnView.stopAnim();
                }
            }
            if(oldHolder != null){
                oldHolder.mName.setTextColor(ColorUtil.getColor(ThemeStore.isDay() ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));
                oldHolder.mColumnView.stopAnim();
                oldHolder.mColumnView.setVisibility(View.GONE);
            }
            mLastIndex = index;
        }
    }

    static class SongViewHolder extends BaseViewHolder{
        @BindView(R.id.sq)
        View mSQ;
        @BindView(R.id.song_title)
        TextView mName;
        @BindView(R.id.song_other)
        TextView mOther;
        @BindView(R.id.song_head_image)
        SimpleDraweeView mImage;
        @BindView(R.id.song_columnview)
        ColumnView mColumnView;
        @BindView(R.id.song_button)
        ImageButton mButton;
        @BindView(R.id.item_root)
        View mContainer;
        SongViewHolder(View itemView) {
            super(itemView);
        }
    }

    static class HeaderHolder extends BaseViewHolder{
        View mRoot;
        @BindView(R.id.divider)
        View mDivider;
        @BindView(R.id.play_shuffle)
        View mShuffle;

        HeaderHolder(View itemView) {
            super(itemView);
            mRoot = itemView;
        }
    }
}
