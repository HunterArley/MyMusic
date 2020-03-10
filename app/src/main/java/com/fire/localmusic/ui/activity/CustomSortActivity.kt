package com.fire.localmusic.ui.activity

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.View
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.afollestad.materialdialogs.MaterialDialog
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import com.fire.localmusic.R
import com.fire.localmusic.adapter.CustomSortAdapter
import com.fire.localmusic.bean.mp3.Song
import com.fire.localmusic.interfaces.OnItemClickListener
import com.fire.localmusic.theme.ThemeStore
import com.fire.localmusic.ui.customview.fastcroll_recyclerview.FastScrollRecyclerView
import com.fire.localmusic.util.*
import java.util.*

class CustomSortActivity : ToolbarActivity() {
    @BindView(R.id.custom_sort_recyclerView)
    lateinit var mRecyclerView: FastScrollRecyclerView
    @BindView(R.id.custom_sort_save)
    lateinit var mSave: FloatingActionButton
    lateinit var mAdapter: CustomSortAdapter
    lateinit var mMDDialog: MaterialDialog

    private var mInfoList: List<Song>? = null
    private var mPlayListID: Int = 0
    private var mPlayListName: String = ""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_sort)
        ButterKnife.bind(this@CustomSortActivity)

        mPlayListID = intent.getIntExtra("id",-1)
        mPlayListName = intent.getStringExtra("name")
        mInfoList = intent.getSerializableExtra("list") as List<Song>

        setUpToolbar(findViewById(R.id.toolbar),mPlayListName)

        mAdapter = CustomSortAdapter(mContext,R.layout.item_custom_sort)
        mAdapter.setHasStableIds(true)
        mAdapter.setData(mInfoList)
        mAdapter.setOnItemClickListener(object : OnItemClickListener{
            override fun onItemLongClick(view: View?, position: Int) {
                Util.vibrate(mContext,150)
            }

            override fun onItemClick(view: View?, position: Int) {

            }

        })

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {
            override fun getMovementFlags(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val dragFlag = ItemTouchHelper.LEFT or ItemTouchHelper.DOWN or ItemTouchHelper.UP or ItemTouchHelper.RIGHT
                return makeMovementFlags(dragFlag, 0)
            }

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                LogUtil.d("ChildHolderAdapter", "from: " + viewHolder.adapterPosition + " to: " + target.adapterPosition)
                Collections.swap(mAdapter.datas, if(viewHolder.adapterPosition  >= 0) viewHolder.adapterPosition  else 0 ,
                        if(target.adapterPosition >= 0) target.adapterPosition else 0)
                mAdapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

            }
        })
        itemTouchHelper.attachToRecyclerView(mRecyclerView)

        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.itemAnimator = DefaultItemAnimator()
        mRecyclerView.adapter = mAdapter
        mRecyclerView.setBubbleTextColor(if (ThemeStore.isLightTheme())
            ColorUtil.getColor(R.color.white)
        else
            ThemeStore.getTextColorPrimary())

        mMDDialog = MaterialDialog.Builder(this)
                .title("保存中")
                .titleColorAttr(R.attr.text_color_primary)
                .content(R.string.please_wait)
                .contentColorAttr(R.attr.text_color_primary)
                .progress(true, 0)
                .backgroundColorAttr(R.attr.background_color_3)
                .progressIndeterminateStyle(false).build()
    }


    @OnClick(R.id.custom_sort_save)
    fun onClick(){
        doAsync {
            uiThread {
                mMDDialog.show()
            }
            Thread.sleep(1000)
            val result = PlayListUtil.clearTable(mPlayListName) + PlayListUtil.addMultiSongs(mInfoList?.map {it.Id }, mPlayListName, mPlayListID)
            uiThread {
                ToastUtil.show(mContext,if(result > 0) R.string.save_success else R.string.save_error)
                mMDDialog.dismiss()
                finish()
            }
        }
    }

}

