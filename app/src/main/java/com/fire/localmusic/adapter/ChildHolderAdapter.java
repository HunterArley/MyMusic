package com.fire.localmusic.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
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

import com.github.promeg.pinyinhelper.Pinyin;

import java.util.ArrayList;

import butterknife.BindView;
import com.fire.localmusic.R;
import com.fire.localmusic.adapter.holder.BaseViewHolder;
import com.fire.localmusic.bean.mp3.Song;
import com.fire.localmusic.interfaces.OnUpdateHighLightListener;
import com.fire.localmusic.menu.SongPopupListener;
import com.fire.localmusic.service.MusicService;
import com.fire.localmusic.theme.Theme;
import com.fire.localmusic.theme.ThemeStore;
import com.fire.localmusic.ui.MultiChoice;
import com.fire.localmusic.ui.activity.ChildHolderActivity;
import com.fire.localmusic.ui.customview.ColumnView;
import com.fire.localmusic.ui.customview.fastcroll_recyclerview.FastScroller;
import com.fire.localmusic.util.ColorUtil;
import com.fire.localmusic.util.Constants;
import com.fire.localmusic.util.DensityUtil;
import com.fire.localmusic.util.Global;
import com.fire.localmusic.util.LogUtil;
import com.fire.localmusic.util.ToastUtil;

/**
 * Created by taeja on 16-6-24.
 */
@SuppressLint("RestrictedApi")
public class ChildHolderAdapter extends HeaderAdapter<Song,BaseViewHolder> implements FastScroller.SectionIndexer,OnUpdateHighLightListener{
    private int mType;
    private String mArg;
    private MultiChoice mMultiChoice;
    private Drawable mDefaultDrawable;
    private Drawable mSelectDrawable;
    private RecyclerView mRecyclerView;
    private int mLastIndex;

    public ChildHolderAdapter(Context context,int layoutId, int type, String arg,MultiChoice multiChoice,RecyclerView recyclerView){
        super(context,layoutId,multiChoice);
        this.mContext = context;
        this.mType = type;
        this.mArg = arg;
        this.mMultiChoice = multiChoice;
        this.mRecyclerView = recyclerView;
        int size = DensityUtil.dip2px(mContext,60);
        mDefaultDrawable = Theme.getShape(GradientDrawable.RECTANGLE,Color.TRANSPARENT,size,size);
        mSelectDrawable = Theme.getShape(GradientDrawable.RECTANGLE,ThemeStore.getSelectColor(),size,size);
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return viewType == TYPE_HEADER ?
                new SongAdapter.HeaderHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_topbar_1,parent,false)) :
                new ChildHolderViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_child_holder,parent,false));
    }

    @Override
    protected void convert(final BaseViewHolder baseHolder,final Song song, int position) {
        if(position == 0){
            final SongAdapter.HeaderHolder headerHolder = (SongAdapter.HeaderHolder) baseHolder;
            //没有歌曲时隐藏
            if(mDatas == null || mDatas.size() == 0){
                headerHolder.mRoot.setVisibility(View.GONE);
                return;
            }
            //显示当前排序方式
            headerHolder.mShuffle.setOnClickListener(v -> {
                Intent intent = new Intent(MusicService.ACTION_CMD);
                intent.putExtra("Control", Constants.NEXT);
                intent.putExtra("shuffle",true);
                //设置正在播放列表
                ArrayList<Integer> IDList = new ArrayList<>();
                for (Song info : mDatas) {
                    IDList.add(info.getId());
                }
                if(IDList.size() == 0){
                    ToastUtil.show(mContext,R.string.no_song);
                    return;
                }
                Global.setPlayQueue(IDList,mContext,intent);
            });
            return;
        }

        final ChildHolderViewHolder holder = (ChildHolderViewHolder) baseHolder;
        if(song == null || song.getId() < 0 || song.Title.equals(mContext.getString(R.string.song_lose_effect))) {
            holder.mTitle.setText(R.string.song_lose_effect);
            holder.mColumnView.setVisibility(View.INVISIBLE);
            holder.mButton.setVisibility(View.INVISIBLE);
        } else {
            holder.mButton.setVisibility(View.VISIBLE);

            //是否无损
            if(!TextUtils.isEmpty(song.getDisplayname())){
                String prefix = song.getDisplayname().substring(song.getDisplayname().lastIndexOf(".") + 1);
                holder.mSQ.setVisibility(!TextUtils.isEmpty(prefix) && (prefix.equals("flac") || prefix.equals("ape") || prefix.equals("wav")) ? View.VISIBLE : View.GONE);
            } else {
                holder.mSQ.setVisibility(View.GONE);
            }

            //设置标题
            holder.mTitle.setText(song.getTitle());

            if(holder.mButton != null) {
                //设置按钮着色
                int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
                Theme.TintDrawable(holder.mButton,R.drawable.icon_player_more,tintColor);

                //item点击效果
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
                    popupMenu.setOnMenuItemClickListener(new SongPopupListener(mContext,song,mType == Constants.PLAYLIST,mArg));
                    popupMenu.show();
                });
            }
        }

        //背景点击效果
        holder.mContainer.setBackground(Theme.getPressAndSelectedStateListRippleDrawable(Constants.LIST_MODEL,mContext));

        if(holder.mContainer != null && mOnItemClickLitener != null){
            holder.mContainer.setOnClickListener(v -> {
                if(holder.getAdapterPosition() - 1 < 0){
                    ToastUtil.show(mContext,R.string.illegal_arg);
                    return;
                }
                if(song != null && song.getId() > 0) {
                    mOnItemClickLitener.onItemClick(v, holder.getAdapterPosition() - 1);
                }
            });
            holder.mContainer.setOnLongClickListener(v -> {
                if(holder.getAdapterPosition() - 1 < 0){
                    ToastUtil.show(mContext,R.string.illegal_arg);
                    return true;
                }
                mOnItemClickLitener.onItemLongClick(v,holder.getAdapterPosition() - 1);
                return true;
            });
        }

        if(MultiChoice.TAG.equals(ChildHolderActivity.TAG) &&
                mMultiChoice.mSelectedPosition.contains(position - 1)){
            LogUtil.d("ChildHolderAdapter","选中:" + position);
            mMultiChoice.addView(holder.mContainer);
        } else {
            holder.mContainer.setSelected(false);
        }
    }

    @Override
    public String getSectionText(int position) {
        if(position == 0) {
            return "";
        }
        if(mDatas != null && mDatas.size() > 0 && position < mDatas.size() && mDatas.get(position - 1) != null){
            String title = mDatas.get(position - 1).getTitle();
            return !TextUtils.isEmpty(title) ? (Pinyin.toPinyin(title.charAt(0))).toUpperCase().substring(0,1)  : "";
        }
        return "";
    }

    @Override
    public void onUpdateHighLight() {
        Song currentSong = MusicService.getCurrentMP3();
        if(currentSong != null && mDatas != null && mDatas.indexOf(currentSong) >= 0){
            int index = mDatas.indexOf(currentSong) + 1;

            //播放的是同一首歌曲
            if(index == mLastIndex){
                return;
            }
            ChildHolderViewHolder newHolder = null;
            if(mRecyclerView.findViewHolderForAdapterPosition(index) instanceof ChildHolderViewHolder){
                newHolder = (ChildHolderViewHolder) mRecyclerView.findViewHolderForAdapterPosition(index);
            }
            ChildHolderViewHolder oldHolder = null;
            if(mRecyclerView.findViewHolderForAdapterPosition(mLastIndex) instanceof ChildHolderViewHolder){
                oldHolder = (ChildHolderViewHolder) mRecyclerView.findViewHolderForAdapterPosition(mLastIndex);
            }

            if(newHolder != null){
                newHolder.mTitle.setTextColor(ThemeStore.getAccentColor());
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
                oldHolder.mTitle.setTextColor(ColorUtil.getColor(ThemeStore.isDay() ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));
                oldHolder.mColumnView.stopAnim();
                oldHolder.mColumnView.setVisibility(View.GONE);
            }
            mLastIndex = index;
        }
    }

    static class ChildHolderViewHolder extends BaseViewHolder {
        @BindView(R.id.sq)
        View mSQ;
        @BindView(R.id.album_holder_item_title)
        TextView mTitle;
        @BindView(R.id.song_item_button)
        public ImageButton mButton;
        @BindView(R.id.song_columnview)
        ColumnView mColumnView;
        public View mContainer;
        ChildHolderViewHolder(View itemView) {
            super(itemView);
            mContainer = itemView;
        }
    }
}