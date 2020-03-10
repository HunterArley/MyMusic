package com.fire.localmusic.adapter;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import butterknife.BindView;
import com.fire.localmusic.R;
import com.fire.localmusic.adapter.holder.BaseViewHolder;
import com.fire.localmusic.bean.mp3.Song;

/**
 * Created by remix on 2018/5/2.
 * Date：2018/5/2
 * Author: remix
 * Description:
 */

public class NotifyAdapter extends BaseAdapter<Song,NotifyAdapter.NotifyHolder> {

    public NotifyAdapter(Context context, int layoutId) {
        super(context, layoutId);
    }

    @Override
    protected void convert(NotifyHolder holder, Song song, int position) {
        holder.mName.setText(String.format("歌曲名：%s",song.getTitle()));
        holder.mTvPlayCount.setText(String.format("播放：%d次",song.getCount()));
        holder.mOther.setText(String.format("%s-%s", song.getArtist(), song.getAlbum()));
        if(mOnItemClickLitener != null && holder.mRooView != null){
            holder.mRooView.setOnClickListener(v -> mOnItemClickLitener.onItemClick(v,holder.getAdapterPosition()));
        }
    }

    static class NotifyHolder extends BaseViewHolder {
        @BindView(R.id.search_name)
        TextView mName;
        @BindView(R.id.search_detail)
        TextView mOther;
        @BindView(R.id.tv_playcount)
        TextView mTvPlayCount;
        @BindView(R.id.notify_item)
        RelativeLayout mRooView;

        public NotifyHolder(View itemView) {
            super(itemView);
        }
    }
}
