package com.fire.localmusic.ui.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import com.fire.localmusic.R;
import com.fire.localmusic.adapter.FolderAdapter;
import com.fire.localmusic.asynctask.WrappedAsyncTaskLoader;
import com.fire.localmusic.bean.mp3.Folder;
import com.fire.localmusic.interfaces.LoaderIds;
import com.fire.localmusic.interfaces.OnItemClickListener;
import com.fire.localmusic.ui.activity.ChildHolderActivity;
import com.fire.localmusic.ui.activity.MultiChoiceActivity;
import com.fire.localmusic.util.Constants;
import com.fire.localmusic.util.Global;
import com.fire.localmusic.util.MediaStoreUtil;

/**
 * Created by Remix on 2015/12/5.
 */

/**
 * 文件夹Fragment
 */
public class FolderFragment extends LibraryFragment<Folder,FolderAdapter>  {
    @BindView(R.id.folder_recyclerview)
    RecyclerView mRecyclerView;

    public static final String TAG = FolderFragment.class.getSimpleName();

    @Override
    protected int getLayoutID() {
        return R.layout.fragment_folder;
    }

    @Override
    protected void initAdapter() {
        mAdapter = new FolderAdapter(mContext,R.layout.item_folder_recycle,mMultiChoice);
        mAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                String path = mAdapter.getDatas().get(position).getPath();
                if(getUserVisibleHint() && !TextUtils.isEmpty(path) &&
                        !mMultiChoice.itemAddorRemoveWithClick(view,position,position,TAG)){
                    Intent intent = new Intent(mContext, ChildHolderActivity.class);
                    intent.putExtra("Id", position);
                    intent.putExtra("Type", Constants.FOLDER);
                    intent.putExtra("Title",path);
                    startActivity(intent);
                }
            }

            @Override
            public void onItemLongClick(View view, int position) {
                String path = mAdapter.getDatas().get(position).getPath();
                if(getUserVisibleHint() && !TextUtils.isEmpty(path)) {
                    mMultiChoice.itemAddorRemoveWithLongClick(view, position, position, TAG,
                            Constants.FOLDER);
                }
            }
        });
    }

    @Override
    protected void initView() {
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        if(mContext instanceof MultiChoiceActivity){
            mMultiChoice = ((MultiChoiceActivity) mContext).getMultiChoice();
        }
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected Loader<List<Folder>> getLoader() {
        return new AsyncFolderLoader(mContext);
    }

    @Override
    protected int getLoaderId() {
        return LoaderIds.FOLDER_FRAGMENT;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPageName = TAG;
    }

    @Override
    public FolderAdapter getAdapter() {
        return mAdapter;
    }

    private static class AsyncFolderLoader extends WrappedAsyncTaskLoader<List<Folder>> {
        private AsyncFolderLoader(Context context) {
            super(context);
        }

        @Override
        public List<Folder> loadInBackground() {
            List<Folder> folderList = new ArrayList<>();
            Global.FolderMap = MediaStoreUtil.getFolder();
            if(Global.FolderMap == null || Global.FolderMap.size() < 0) {
                return folderList;
            }

            for (String path : Global.FolderMap.keySet()) {
                String folderName = path.substring(path.lastIndexOf("/") + 1, path.length());
                int count = Global.FolderMap.get(path).size();
                folderList.add(new Folder(folderName, count, path));
            }

            return folderList;
        }
    }
}
