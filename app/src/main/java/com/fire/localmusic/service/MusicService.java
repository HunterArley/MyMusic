package com.fire.localmusic.service;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.media.audiofx.AudioEffect;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.ViewGroup;
import android.view.WindowManager;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;
import com.fire.localmusic.R;
import com.fire.localmusic.appshortcuts.DynamicShortcutManager;
import com.fire.localmusic.appwidgets.AppWidgetBig;
import com.fire.localmusic.appwidgets.AppWidgetMedium;
import com.fire.localmusic.appwidgets.AppWidgetSmall;
import com.fire.localmusic.bean.FloatLrcContent;
import com.fire.localmusic.bean.SongPlay;
import com.fire.localmusic.bean.mp3.PlayListSong;
import com.fire.localmusic.bean.mp3.Song;
import com.fire.localmusic.db.DbService;
import com.fire.localmusic.db.PlayListSongs;
import com.fire.localmusic.db.PlayLists;
import com.fire.localmusic.helper.MusicEventHelper;
import com.fire.localmusic.helper.ShakeDetector;
import com.fire.localmusic.helper.UpdateHelper;
import com.fire.localmusic.lyric.SearchLrc;
import com.fire.localmusic.lyric.bean.LrcRow;
import com.fire.localmusic.misc.floatpermission.FloatWindowManager;
import com.fire.localmusic.misc.observer.DBObserver;
import com.fire.localmusic.misc.observer.MediaStoreObserver;
import com.fire.localmusic.misc.receiver.HeadsetPlugReceiver;
import com.fire.localmusic.request.RequestConfig;
import com.fire.localmusic.request.network.RemoteUriRequest;
import com.fire.localmusic.service.notification.Notify;
import com.fire.localmusic.service.notification.NotifyImpl;
import com.fire.localmusic.service.notification.NotifyImpl24;
import com.fire.localmusic.theme.ThemeStore;
import com.fire.localmusic.ui.activity.EQActivity;
import com.fire.localmusic.ui.activity.LockScreenActivity;
import com.fire.localmusic.ui.customview.floatwidget.FloatLrcView;
import com.fire.localmusic.util.Constants;
import com.fire.localmusic.util.Global;
import com.fire.localmusic.util.LogUtil;
import com.fire.localmusic.util.MediaStoreUtil;
import com.fire.localmusic.util.PlayListUtil;
import com.fire.localmusic.util.SPUtil;
import com.fire.localmusic.util.ToastUtil;
import com.fire.localmusic.util.Util;

import static com.fire.localmusic.util.ImageUriUtil.getSearchRequestWithAlbumType;



/**
 * 播放Service
 * 歌曲的播放 控制
 * 回调相关activity的界面更新
 * 通知栏的控制
 */
public class MusicService extends BaseService
        implements Playback, MusicEventHelper.MusicEventCallback {
    private final static String TAG = "MusicService";
    private static MusicService mInstance;
    /** 是否第一次准备完成 */
    private boolean mFirstPrepared = true;

    /** 是否正在设置mediapplayer的datasource */
    private static boolean mIsInitialized = false;

    /** 数据是否加载完成 */
    private boolean mLoadFinished = false;

    /** 播放模式 */
    private static int mPlayModel = Constants.PLAY_LOOP;

    /** 当前是否正在播放 */
    private static Boolean mIsplay = false;

    /** 当前播放的索引 */
    private static int mCurrentIndex = 0;
    /** 当前正在播放的歌曲id */
    private static int mCurrentId = -1;
    /** 当前正在播放的mp3 */
    private static Song mCurrentSong = null;

    /** 下一首歌曲的索引 */
    private static int mNextIndex = 0;
    /** 下一首播放歌曲的id */
    private static int mNextId = -1;
    /** 下一首播放的mp3 */
    private static Song mNextSong = null;

    /** MediaExtractor 获得码率等信息 */
    private static MediaExtractor mMediaExtractor;

    /** MediaPlayer 负责歌曲的播放等 */
    private MediaPlayer mMediaPlayer;

    /** 桌面部件 */
    private AppWidgetMedium mAppWidgetMedium;
    private AppWidgetSmall mAppWidgetSmall;
    private AppWidgetBig mAppWidgetBig;

    /** AudiaoManager */
    private AudioManager mAudioManager;

    /** 播放控制的Receiver */
    private ControlReceiver mControlRecevier;

    /** 事件 */
    private MusicEventReceiver mMusicEventReceiver;

    /** 监测耳机拔出的Receiver */
    private HeadsetPlugReceiver mHeadSetReceiver;

    /** 接收桌面部件 */
    private WidgetReceiver mWidgetReceiver;

    /** 监听AudioFocus的改变 */
    private AudioManager.OnAudioFocusChangeListener mAudioFocusListener;

    /** MediaSession */
    private MediaSessionCompat mMediaSession;

    /** 当前是否获得AudioFocus */
    private boolean mAudioFocus = false;

    /** 计时器 */
    private TimerUpdater mTimerUpdater;

    /** 定时关闭剩余时间 */
    private long mMillisUntilFinish;

    /** 更新相关Activity的Handler */
    private UpdateUIHandler mUpdateUIHandler;
    /** 电源锁 */
    private PowerManager.WakeLock mWakeLock;
    /** 通知栏 */
    private Notify mNotify;
    /** 当前控制命令 */
    private int mControl;
    /** WindowManager 控制悬浮窗 */
    private WindowManager mWindowManager;
    /** 是否显示桌面歌词 */
    private boolean mShowFloatLrc = false;
    /** 桌面歌词控件 */
    private FloatLrcView mFloatLrcView;
    /** 当前歌词 */
    private volatile List<LrcRow> mLrcRows = null;
    /** 已经生成过的随机数 用于随机播放模式 */
    private ArrayList<Integer> mRandomList = new ArrayList<>();
    /** service是否停止运行 */
    private boolean mIsServiceStop = false;
    /** handlerThread */
    private HandlerThread mPlaybackThread;
    private PlaybackHandler mPlaybackHandler;
    /** 监听锁屏 */
    private ScreenReceiver mScreenReceiver;

    /** shortcut */
    private DynamicShortcutManager mShortcutManager;

    /** 音量控制 */
    private VolumeController mVolumeController;

    /** 退出时播放的进度 */
    private int mLastProgress;

    /** 是否开启断点播放 */
    private boolean mPlayAtBreakPoint;

    private MediaStoreObserver mMediaStoreObserver;
    private DBObserver mPlayListObserver;
    private DBObserver mPlayListSongObserver;
    private Context mContext;

    protected boolean mHasPermission = false;

    public static final String APLAYER_PACKAGE_NAME = "com.fire.localmusic";
    public static final String ACTION_MEDIA_CHANGE = APLAYER_PACKAGE_NAME + ".media.change";
    public static final String ACTION_PERMISSION_CHANGE = APLAYER_PACKAGE_NAME
                                                          + ".permission.change";
    public static final String ACTION_PLAYLIST_CHANGE = APLAYER_PACKAGE_NAME + ".playlist.change";
    public static final String ACTION_APPWIDGET_OPERATE = APLAYER_PACKAGE_NAME
                                                          + "appwidget.operate";
    public static final String ACTION_SHORTCUT_SHUFFLE = APLAYER_PACKAGE_NAME + ".shortcut.shuffle";
    public static final String ACTION_SHORTCUT_MYLOVE = APLAYER_PACKAGE_NAME + ".shortcut.my_love";
    public static final String ACTION_SHORTCUT_LASTADDED = APLAYER_PACKAGE_NAME
                                                           + "shortcut.last_added";
    public static final String ACTION_SHORTCUT_CONTINUE_PLAY = APLAYER_PACKAGE_NAME
                                                               + "shortcut.continue_play";
    public static final String ACTION_LOAD_FINISH = APLAYER_PACKAGE_NAME + "load.finish";
    public final static String ACTION_CMD = APLAYER_PACKAGE_NAME + ".cmd";
    public final static String ACTION_WIDGET_UPDATE = APLAYER_PACKAGE_NAME + ".widget_update";

    public synchronized static MusicService getInstance() {
        return mInstance;
    }

    private boolean mAlreadyUnInit;

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        LogUtil.d("ServiceLifeCycle", "onTaskRemoved");
        unInit();
        stopSelf();
        System.exit(0);
    }

    @Override
    public void onDestroy() {
        LogUtil.d("ServiceLifeCycle", "onDestroy");
        super.onDestroy();
        mIsServiceStop = true;
        unInit();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.d("ServiceLifeCycle", "onCreate");
        mContext = this;
        mInstance = this;
        init();
    }

    @Override
    public int onStartCommand(Intent commandIntent, int flags, int startId) {
        LogUtil.d("ServiceLifeCycle", "onStartCommand");
        mIsServiceStop = false;

        if (!mLoadFinished && (mHasPermission = Util.hasPermissions(new String[] {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }))) {
            loadSync();
        }

        String action = commandIntent != null ? commandIntent.getAction() : "";
        if (TextUtils.isEmpty(action)) {
            return START_STICKY;
        }
        mPlaybackHandler.postDelayed(() -> handleStartCommandIntent(commandIntent, action), 200);
        return START_STICKY;
    }

    private void init() {
        MusicEventHelper.addCallback(this);

        mShortcutManager = new DynamicShortcutManager(mContext);
        mVolumeController = new VolumeController();
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        Global.setHeadsetOn(mAudioManager.isWiredHeadsetOn());

        mPlaybackThread = new HandlerThread("IO");
        mPlaybackThread.start();
        mPlaybackHandler = new PlaybackHandler(this, mPlaybackThread.getLooper());

        mUpdateUIHandler = new UpdateUIHandler(this);

        //电源锁
        mWakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, getClass().getSimpleName());
        mWakeLock.setReferenceCounted(false);
        //通知栏
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1 & !SPUtil.getValue(mContext,
                SPUtil.SETTING_KEY.SETTING_NAME, SPUtil.SETTING_KEY.NOTIFY_STYLE_CLASSIC, false)) {
            mNotify = new NotifyImpl24(this);
        } else {
            mNotify = new NotifyImpl(this);
        }

        //监听audiofocus
        mAudioFocusListener = new AudioFocusChangeListener();

        //桌面部件
        mAppWidgetBig = new AppWidgetBig();
        mAppWidgetMedium = new AppWidgetMedium();
        mAppWidgetSmall = new AppWidgetSmall();
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        //初始化Receiver
        mMusicEventReceiver = new MusicEventReceiver();
        IntentFilter eventFilter = new IntentFilter();
        eventFilter.addAction(ACTION_MEDIA_CHANGE);
        eventFilter.addAction(ACTION_PERMISSION_CHANGE);
        eventFilter.addAction(ACTION_PLAYLIST_CHANGE);
        registerReceiver(mMusicEventReceiver, eventFilter);

        mControlRecevier = new ControlReceiver();
        registerReceiver(mControlRecevier, new IntentFilter(ACTION_CMD));

        mHeadSetReceiver = new HeadsetPlugReceiver();
        IntentFilter noisyFilter = new IntentFilter();
        noisyFilter.addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        noisyFilter.addAction(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(mHeadSetReceiver, noisyFilter);

        mWidgetReceiver = new WidgetReceiver();
        registerReceiver(mWidgetReceiver, new IntentFilter(ACTION_WIDGET_UPDATE));

        mScreenReceiver = new ScreenReceiver();
        IntentFilter screenFilter = new IntentFilter();
        screenFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mScreenReceiver, screenFilter);

        //监听数据库变化
        mMediaStoreObserver = new MediaStoreObserver(mUpdateUIHandler);
        mPlayListObserver = new DBObserver(mUpdateUIHandler);
        mPlayListSongObserver = new DBObserver(mUpdateUIHandler);
        getContentResolver().registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true, mMediaStoreObserver);
        getContentResolver().registerContentObserver(PlayLists.CONTENT_URI, true,
                mPlayListObserver);
        getContentResolver().registerContentObserver(PlayListSongs.CONTENT_URI, true,
                mPlayListSongObserver);

        setUpMediaPlayer();
        setUpMediaSession();

        //初始化MediaExtractor
        mMediaExtractor = new MediaExtractor();

        //初始化音效设置
        Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
        i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, mMediaPlayer.getAudioSessionId());
        if (!Util.isIntentAvailable(this, i)) {
            EQActivity.Init();
        }
    }

    /**
     * 初始化mediasession
     */
    private void setUpMediaSession() {
        //初始化MediaSesson 用于监听线控操作
        mMediaSession = new MediaSessionCompat(getApplicationContext(), "APlayer");
        mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                               | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mMediaSession.setCallback(new SessionCallBack());
        mMediaSession.setPlaybackToLocal(AudioManager.STREAM_MUSIC);
        mMediaSession.setActive(true);
    }

    /**
     * 初始化Mediaplayer
     */
    private void setUpMediaPlayer() {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mMediaPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

        mMediaPlayer.setOnCompletionListener(mp -> {
            if (mCloseAfter) {
                sendBroadcast(new Intent(Constants.EXIT));
            } else {
                if (mPlayModel == Constants.PLAY_REPEATONE) {
                    prepare(mCurrentSong);
                } else {
                    playNextOrPrev(true);
                }
                Global.setOperation(Constants.NEXT);
                acquireWakeLock();
            }
        });
        mMediaPlayer.setOnPreparedListener(mp -> {
            LogUtil.d(TAG, "准备完成:" + mFirstPrepared);
            if (mFirstPrepared) {
                mFirstPrepared = false;
                if (mLastProgress > 0) {
                    mMediaPlayer.seekTo(mLastProgress);
                }
                return;
            }
            LogUtil.d(TAG, "开始播放");
            play(false);
        });

        mMediaPlayer.setOnErrorListener((mp, what, extra) -> {
            try {
                mIsInitialized = false;
                if (mMediaPlayer != null) {
                    mMediaPlayer.release();
                }
                setUpMediaPlayer();
                ToastUtil.show(mContext, R.string.mediaplayer_error, what, extra);
                return true;
            } catch (Exception e) {

            }
            return false;
        });
    }

    /**
     * 初始化mediaplayer
     */
    public void setUpDataSource(Song item, int pos) {
        if (item == null) {
            return;
        }
        //初始化当前播放歌曲
        LogUtil.d(TAG, "当前歌曲:" + item.getTitle());
        mCurrentSong = item;
        mCurrentId = mCurrentSong.getId();
        mCurrentIndex = pos;
        try {
            if (mMediaPlayer == null) {
                setUpMediaPlayer();
            }
            prepare(mCurrentSong.getUrl(), false);
        } catch (Exception e) {
            mUpdateUIHandler.post(() -> ToastUtil.show(mContext, e.toString()));
        }
        //桌面歌词
        //        updateFloatLrc();
        //初始化下一首歌曲
        //        updateNextSong();
        if (mPlayModel == Constants.PLAY_SHUFFLE) {
            makeShuffleList(mCurrentId);
        }
        //查找上次退出时保存的下一首歌曲是否还存在
        //如果不存在，重新设置下一首歌曲
        mNextId = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                SPUtil.SETTING_KEY.NEXT_SONG_ID, -1);
        if (mNextId == -1) {
            mNextIndex = mCurrentIndex;
            updateNextSong();
        } else {
            mNextIndex = mPlayModel != Constants.PLAY_SHUFFLE ? Global.PlayQueue.indexOf(mNextId)
                    : mRandomList.indexOf(mNextId);
            mNextSong = MediaStoreUtil.getMP3InfoById(mNextId);
            if (mNextSong != null) {
                return;
            }
            updateNextSong();
        }
    }

    private void unInit() {
        if (mAlreadyUnInit) {
            return;
        }
        MusicEventHelper.removeCallback(this);
        closeAudioEffectSession();
        if (mMediaPlayer != null) {
            if (isPlay()) {
                pause(false);
            }
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
        mLoadFinished = false;
        mIsInitialized = false;
        mShortcutManager.updateContinueShortcut();

        mNotify.cancelPlayingNotify();

        updateAppwidget();
        removeFloatLrc();
        if (mUpdateFloatLrcThread != null) {
            mUpdateFloatLrcThread.quitImmediately();
        }

        if (mMediaExtractor != null) {
            mMediaExtractor.release();
        }

        mUpdateUIHandler.removeCallbacksAndMessages(null);
        mShowFloatLrc = false;

        if (Build.VERSION.SDK_INT >= 18) {
            mPlaybackThread.quitSafely();
        } else {
            mPlaybackThread.quit();
        }

        mAudioManager.abandonAudioFocus(mAudioFocusListener);
        mMediaSession.setActive(false);
        mMediaSession.release();

        Util.unregisterReceiver(this, mControlRecevier);
        Util.unregisterReceiver(this, mHeadSetReceiver);
        Util.unregisterReceiver(this, mWidgetReceiver);
        Util.unregisterReceiver(this, mMusicEventReceiver);
        Util.unregisterReceiver(this, mScreenReceiver);

        releaseWakeLock();
        getContentResolver().unregisterContentObserver(mMediaStoreObserver);
        getContentResolver().unregisterContentObserver(mPlayListObserver);
        getContentResolver().unregisterContentObserver(mPlayListSongObserver);

        ShakeDetector.getInstance().stopListen();

        mAlreadyUnInit = true;
    }

    private void closeAudioEffectSession() {
        final Intent audioEffectsIntent = new Intent(
                AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION,
                mMediaPlayer.getAudioSessionId());
        audioEffectsIntent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        sendBroadcast(audioEffectsIntent);
    }

    /**
     * 播放下一首
     */
    @Override
    public void playNext() {
        playNextOrPrev(true);
    }

    /**
     * 播放上一首
     */
    @Override
    public void playPrevious() {
        playNextOrPrev(false);
    }

    /**
     * 开始播放
     */
    @Override
    public void play(boolean fadeIn) {
        mAudioFocus = mAudioManager.requestAudioFocus(
                mAudioFocusListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        if (!mAudioFocus) {
            return;
        }
        mIsplay = true; //更新所有界面
        update(Global.getOperation());
        mMediaPlayer.start();
        if (fadeIn) {
            mVolumeController.fadeIn();
        } else {
            mVolumeController.to(1);
        }

        mPlaybackHandler.post(() -> {
            //保存当前播放和下一首播放的歌曲的id
            SPUtil.putValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                    SPUtil.SETTING_KEY.LAST_SONG_ID, mCurrentId);
            SPUtil.putValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                    SPUtil.SETTING_KEY.NEXT_SONG_ID, mNextId);
        });
    }

    /**
     * 根据当前播放状态暂停或者继续播放
     */
    @Override
    public void toggle() {
        if (mMediaPlayer.isPlaying()) {
            pause(false);
        } else {
            if (mMediaPlayer.getCurrentPosition() == 0) {
                setData();
            }
            play(true);
        }
    }

    /**
     * 暂停
     */
    @Override
    public void pause(boolean updateMediaSessionOnly) {
        if (updateMediaSessionOnly) {
            updateMediaSession(Global.Operation);
        } else {
            mIsplay = false;
            update(Global.Operation);
            mVolumeController.fadeOut();
        }
    }

    /**
     * 播放选中的歌曲
     * 比如在全部歌曲或者专辑详情里面选中某一首歌曲
     *
     * @param position 播放位置
     */
    @Override
    public void playSelectSong(int position) {
        if ((mCurrentIndex = position) == -1 || (mCurrentIndex > Global.PlayQueue.size() - 1)) {
            ToastUtil.show(mContext, R.string.illegal_arg);
            return;
        }
        mCurrentId = Global.PlayQueue.get(mCurrentIndex);
        mCurrentSong = MediaStoreUtil.getMP3InfoById(mCurrentId);

        mNextIndex = mCurrentIndex;
        mNextId = mCurrentId;

        //如果是随机播放 需要调整下RandomList
        //保证正常播放队列和随机播放队列中当前歌曲的索引一致
        int index = mRandomList.indexOf(mCurrentId);
        if (mPlayModel == Constants.PLAY_SHUFFLE && index != mCurrentIndex) {
            Collections.swap(mRandomList, mCurrentIndex, index);
        }

        if (mCurrentSong == null) {
            ToastUtil.show(mContext, R.string.song_lose_effect);
            return;
        }
        //        mIsplay = true;
        prepare(mCurrentSong);
        updateNextSong();
    }

    @Override
    public void onMediaStoreChanged() {

    }

    @Override
    public void onPermissionChanged(boolean has) {
        if (has != mHasPermission && has) {
            mHasPermission = true;
            loadSync();
        }
    }

    @Override
    public void onPlayListChanged() {
    }

    public class WidgetReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String str = intent.getStringExtra("WidgetName");
            int[] appIds = intent.getIntArrayExtra("WidgetIds");
            switch (str) {
                case "BigWidget":
                    if (mAppWidgetBig != null) {
                        mAppWidgetBig.updateWidget(context, appIds, true);
                    }
                    break;
                case "MediumWidget":
                    if (mAppWidgetMedium != null) {
                        mAppWidgetMedium.updateWidget(context, appIds, true);
                    }
                    break;
                case "SmallWidget":
                    if (mAppWidgetSmall != null) {
                        mAppWidgetSmall.updateWidget(context, appIds, true);
                    }
                    break;
            }
        }
    }

    public class MusicEventReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleMusicEvent(intent);
        }
    }

    private void handleStartCommandIntent(Intent commandIntent, String action) {
        mFirstPrepared = false;
        switch (action) {
            case ACTION_APPWIDGET_OPERATE:
                Intent appwidgetIntent = new Intent(ACTION_CMD);
                appwidgetIntent.putExtra("Control", commandIntent.getIntExtra("Control", -1));
                sendBroadcast(appwidgetIntent);
                break;
            case ACTION_SHORTCUT_CONTINUE_PLAY:
                Intent continueIntent = new Intent(ACTION_CMD);
                continueIntent.putExtra("Control", Constants.TOGGLE);
                sendBroadcast(continueIntent);
                break;
            case ACTION_SHORTCUT_SHUFFLE:
                if (mPlayModel != Constants.PLAY_SHUFFLE) {
                    setPlayModel(Constants.PLAY_SHUFFLE);
                }
                Intent shuffleIntent = new Intent(ACTION_CMD);
                shuffleIntent.putExtra("Control", Constants.NEXT);
                sendBroadcast(shuffleIntent);
                break;
            case ACTION_SHORTCUT_MYLOVE:
                List<Integer> myLoveIds = PlayListUtil.getIDList(Global.MyLoveID);
                if (myLoveIds == null || myLoveIds.size() == 0) {
                    ToastUtil.show(mContext, R.string.list_is_empty);
                    return;
                }
                Intent myloveIntent = new Intent(ACTION_CMD);
                myloveIntent.putExtra("Control", Constants.PLAYSELECTEDSONG);
                myloveIntent.putExtra("Position", 0);
                Global.setPlayQueue(myLoveIds, mContext, myloveIntent);
                break;
            case ACTION_SHORTCUT_LASTADDED:
                List<Song> songs = MediaStoreUtil.getLastAddedSong();
                List<Integer> lastAddIds = new ArrayList<>();
                if (songs == null || songs.size() == 0) {
                    ToastUtil.show(mContext, R.string.list_is_empty);
                    return;
                }
                for (Song song : songs) {
                    lastAddIds.add(song.getId());
                }
                Intent lastedIntent = new Intent(ACTION_CMD);
                lastedIntent.putExtra("Control", Constants.PLAYSELECTEDSONG);
                lastedIntent.putExtra("Position", 0);
                Global.setPlayQueue(lastAddIds, mContext, lastedIntent);
                break;
            default:
                if (action.equalsIgnoreCase(ACTION_CMD)) {
                    mControlRecevier.onReceive(this, commandIntent);
                }
        }
    }

    private void handleMusicEvent(Intent intent) {
        if (intent == null) {
            return;
        }
        String action = intent.getAction();
        List<MusicEventHelper.MusicEventCallback> callbacks = MusicEventHelper.getCallbacks();
        if (callbacks == null) {
            return;
        }
        if (ACTION_MEDIA_CHANGE.equals(action)) {
            for (MusicEventHelper.MusicEventCallback callback : callbacks) {
                callback.onMediaStoreChanged();
            }
        } else if (ACTION_PERMISSION_CHANGE.equals(action)) {
            for (MusicEventHelper.MusicEventCallback callback : callbacks) {
                callback.onPermissionChanged(intent.getBooleanExtra("permission", false));
            }
        } else if (ACTION_PLAYLIST_CHANGE.equals(action)) {
            for (MusicEventHelper.MusicEventCallback callback : callbacks) {
                callback.onPlayListChanged();
            }
        }
    }

    /**
     * 接受控制命令
     * 包括暂停、播放、上下首、改版播放模式等
     */
    public class ControlReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getExtras() == null) {
                return;
            }
            int control = intent.getIntExtra("Control", -1);
            mControl = control;

            if (control == Constants.PLAYSELECTEDSONG
                || control == Constants.PREV
                || control == Constants.NEXT
                || control == Constants.TOGGLE
                || control == Constants.PAUSE
                || control == Constants.START) {
                //保存控制命令,用于播放界面判断动画
                Global.setOperation(control);
                if (Global.PlayQueue == null || Global.PlayQueue.size() == 0) {
                    //列表为空，尝试读取
                    Global.PlayQueueID = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                            "PlayQueueID", -1);
                    Global.PlayQueue = PlayListUtil.getIDList(Global.PlayQueueID);
                }
            }

            switch (control) {
                //关闭通知栏
                case Constants.CLOSE_NOTIFY:
                    Global.setNotifyShowing(false);
                    pause(false);
                    if (mUpdateFloatLrcThread != null) {
                        mUpdateFloatLrcThread.quitByNotification();
                    }
                    mUpdateUIHandler.postDelayed(() -> mNotify.cancelPlayingNotify(), 100);
                    break;
                //播放选中的歌曲
                case Constants.PLAYSELECTEDSONG:
                    playSelectSong(intent.getIntExtra("Position", -1));
                    setData();
                    break;
                //播放上一首
                case Constants.PREV:
                    playPrevious();
                    setData();
                    break;
                //播放下一首
                case Constants.NEXT:
                    playNext();
                    setData();
                    break;
                //暂停或者继续播放
                case Constants.TOGGLE:
                    toggle();
                    break;
                //暂停
                case Constants.PAUSE:
                    pause(false);
                    break;
                //继续播放
                case Constants.START:
                    play(false);
                    break;
                //改变播放模式
                case Constants.CHANGE_MODEL:
                    mPlayModel = (mPlayModel == Constants.PLAY_REPEATONE ? Constants.PLAY_LOOP
                                          : ++mPlayModel);
                    setPlayModel(mPlayModel);
                    break;
                //取消或者添加收藏
                case Constants.LOVE:
                    int exist = PlayListUtil.isLove(mCurrentId);
                    if (exist == PlayListUtil.EXIST) {
                        PlayListUtil.deleteSong(mCurrentId, Global.MyLoveID);
                    } else if (exist == PlayListUtil.NONEXIST) {
                        PlayListUtil.addSong(new PlayListSong(mCurrentSong.getId(), Global.MyLoveID,
                                Constants.MYLOVE));
                    }
                    updateAppwidget();
                    break;
                //桌面歌词
                case Constants.TOGGLE_FLOAT_LRC:
                    boolean open = intent.getBooleanExtra("FloatLrc", false);
                    if (mShowFloatLrc != open) {
                        mShowFloatLrc = open;
                        if (mShowFloatLrc) {
                            updateFloatLrc(false);
                        } else {
                            closeFloatLrc();
                        }
                    }
                    break;
                case Constants.TOGGLE_MEDIASESSION:
                    switch (SPUtil.getValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                            SPUtil.SETTING_KEY.LOCKSCREEN, Constants.APLAYER_LOCKSCREEN)) {
                        case Constants.APLAYER_LOCKSCREEN:
                        case Constants.CLOSE_LOCKSCREEN:
                            cleanMetaData();
                            break;
                        case Constants.SYSTEM_LOCKSCREEN:
                            updateMediaSession(Constants.NEXT);
                            break;
                    }
                    break;
                //临时播放一首歌曲
                case Constants.PLAY_TEMP:
                    Song tempSong = intent.getParcelableExtra("Song");
                    if (tempSong != null) {
                        mCurrentSong = tempSong;
                        prepare(mCurrentSong);
                    }
                    setData();
                    break;
                //切换通知栏样式
                case Constants.TOGGLE_NOTIFY:
                    mNotify.cancelPlayingNotify();
                    boolean classic = intent.getBooleanExtra(
                            SPUtil.SETTING_KEY.NOTIFY_STYLE_CLASSIC, false);
                    if (classic) {
                        mNotify = new NotifyImpl(MusicService.this);
                    } else {
                        mNotify = new NotifyImpl24(MusicService.this);
                    }
                    if (Global.isNotifyShowing()) {
                        mNotify.updateForPlaying();
                    }
                    break;
                //解锁通知栏
                case Constants.UNLOCK_DESTOP_LYRIC:
                    if (mFloatLrcView != null) {
                        mFloatLrcView.saveLock(false, true);
                    }
                    break;
                //某一首歌曲添加至下一首播放
                case Constants.ADD_TO_NEXT_SONG:
                    Song nextSong = intent.getParcelableExtra("song");
                    if (nextSong == null) {
                        return;
                    }
                    //添加到播放队列
                    if (mNextId == nextSong.getId()) {
                        ToastUtil.show(mContext, R.string.already_add_to_next_song);
                        return;
                    }
                    //根据当前播放模式，添加到队列
                    if (mPlayModel == Constants.PLAY_SHUFFLE) {
                        if (mRandomList.contains(nextSong.getId())) {
                            mRandomList.remove(Integer.valueOf(nextSong.getId()));
                            mRandomList.add(
                                    mCurrentIndex + 1 < mRandomList.size() ? mCurrentIndex + 1 : 0,
                                    nextSong.getId());
                        } else {
                            mRandomList.add(mRandomList.indexOf(mCurrentId) + 1, nextSong.getId());
                        }
                    } else {
                        if (Global.PlayQueue.contains(nextSong.getId())) {
                            Global.PlayQueue.remove(Integer.valueOf(nextSong.getId()));
                            Global.PlayQueue.add(
                                    mCurrentIndex + 1 < Global.PlayQueue.size() ? mCurrentIndex + 1
                                            : 0, nextSong.getId());
                        } else {
                            Global.PlayQueue.add(Global.PlayQueue.indexOf(mCurrentId) + 1,
                                    nextSong.getId());
                        }
                    }

                    //更新下一首
                    mNextIndex = mCurrentIndex;
                    updateNextSong();
                    //保存到数据库
                    if (mPlayModel != Constants.PLAY_SHUFFLE) {
                        mPlaybackHandler.post(() -> {
                            PlayListUtil.clearTable(Constants.PLAY_QUEUE);
                            PlayListUtil.addMultiSongs(Global.PlayQueue, Constants.PLAY_QUEUE,
                                    Global.PlayQueueID);
                        });
                    }
                    ToastUtil.show(mContext, R.string.already_add_to_next_song);
                    setData();
                    break;
                //改变歌词源
                case Constants.CHANGE_LYRIC:
                    if (mShowFloatLrc) {
                        updateFloatLrc(true);
                    }
                    break;
                //断点播放
                case Constants.PLAY_AT_BREAKPOINT:
                    mPlayAtBreakPoint = intent.getBooleanExtra(
                            SPUtil.SETTING_KEY.PLAY_AT_BREAKPOINT, false);
                    break;
                default:
                    break;
            }
        }
    }

    private void setData() {
        if (mMediaPlayer.getCurrentPosition() == 0) {
            Observable.just(mCurrentSong)
                    .map(song -> {
                        SongPlay songPlay = DbService.getInstance()
                                .getSongPlay(String.format("%d", song.getId()));
                        if (songPlay == null) {
                            songPlay = new SongPlay(String.format("%d", song.getId()), 1);
                        } else {
                            int i = songPlay.getCount() + 1;
                            songPlay.setCount(i);
                        }
                        DbService.getInstance().insertSongPlay(songPlay);
                        return song;
                    })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe();
        }
    }

    /**
     * 清除锁屏显示的内容
     */
    private void cleanMetaData() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mMediaSession.setMetadata(new MediaMetadataCompat.Builder().build());
            mMediaSession.setPlaybackState(
                    new PlaybackStateCompat.Builder().setState(PlaybackState.STATE_NONE, 0, 1f)
                            .build());
        }
    }

    /**
     * 更新
     */
    private void update(int control) {
        if (control == Constants.PLAYSELECTEDSONG
            || control == Constants.PREV
            || control == Constants.NEXT
            || control == Constants.TOGGLE
            || control == Constants.PAUSE
            || control == Constants.START
            || control == Constants.PLAY_TEMP) {
            //更新ui
            mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_UI);
            //更新通知栏
            mNotify.updateForPlaying();
            //更新桌面歌词播放按钮
            if (mFloatLrcView != null) {
                mFloatLrcView.setPlayIcon(MusicService.isPlay());
            }
            updateMediaSession(control);
            mShortcutManager.updateContinueShortcut();
        }
    }

    public MediaSessionCompat getMediaSession() {
        return mMediaSession;
    }

    /**
     * 更新锁屏
     */
    private void updateMediaSession(int control) {
        boolean isSmartisan = Build.MANUFACTURER.equalsIgnoreCase("smartisan");
        if ((!isSmartisan
             && SPUtil.getValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                SPUtil.SETTING_KEY.LOCKSCREEN, Constants.APLAYER_LOCKSCREEN)
                == Constants.CLOSE_LOCKSCREEN) ||
            mCurrentSong == null) {
            return;
        }

        int playState = mIsplay
                ? PlaybackStateCompat.STATE_PLAYING
                : PlaybackStateCompat.STATE_PAUSED;
        if (control == Constants.TOGGLE
            || control == Constants.PAUSE
            || control == Constants.START) {
            mMediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(playState, getProgress(), 1.0f)
                    .setActions(PlaybackStateCompat.ACTION_PLAY
                                | PlaybackStateCompat.ACTION_PAUSE
                                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                                |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS).build());
        } else {
            mMediaSession.setPlaybackState(new PlaybackStateCompat.Builder()
                    .setState(playState, getProgress(), 1.0f)
                    .setActions(PlaybackStateCompat.ACTION_PLAY
                                | PlaybackStateCompat.ACTION_PAUSE
                                | PlaybackStateCompat.ACTION_PLAY_PAUSE
                                |
                                PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                                | PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS).build());

            new RemoteUriRequest(getSearchRequestWithAlbumType(mCurrentSong),
                    new RequestConfig.Builder(400, 400).build()) {
                @Override
                public void onError(String errMsg) {
                    setMediaSessionData(null);
                }

                @Override
                public void onSuccess(Bitmap result) {
                    setMediaSessionData(result);
                }

                private void setMediaSessionData(Bitmap result) {
                    mMediaSession.setMetadata(new MediaMetadataCompat.Builder()
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,
                                    mCurrentSong.getAlbum())
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,
                                    mCurrentSong.getArtist())
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ARTIST,
                                    mCurrentSong.getArtist())
                            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,
                                    mCurrentSong.getDisplayname())
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION,
                                    mCurrentSong.getDuration())
                            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER,
                                    Global.PlayQueue != null ? Global.PlayQueue.size() : 0)
                            .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, mCurrentIndex)
                            .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, result)
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE,
                                    mCurrentSong.getTitle())
                            .build());
                }
            }.load();
        }
    }

    /**
     * 准备播放
     *
     * @param path 播放歌曲的路径
     */
    private void prepare(final String path, final boolean requestFocus) {
        try {
            LogUtil.d(TAG, "准备播放");
            if (TextUtils.isEmpty(path)) {
                mUpdateUIHandler.post(
                        () -> ToastUtil.show(mContext, getString(R.string.path_empty)));
                return;
            }
            if (requestFocus) {
                mAudioFocus = mAudioManager.requestAudioFocus(mAudioFocusListener,
                        AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN) ==
                              AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
                if (!mAudioFocus) {
                    mUpdateUIHandler.post(() -> ToastUtil.show(mContext,
                            getString(R.string.cant_request_audio_focus)));
                    return;
                }
            }

            if (isPlay()) {
                pause(true);
            }
            LogUtil.d("setUpDataSource", "prepare");
            mIsInitialized = false;
            mMediaPlayer.reset();
            mMediaPlayer.setDataSource(path);
            mMediaPlayer.prepareAsync();
            //            mIsplay = true;
            mIsInitialized = true;
        } catch (Exception e) {
            mUpdateUIHandler.post(
                    () -> ToastUtil.show(mContext, getString(R.string.play_failed) + e.toString()));
            mIsInitialized = false;
        }
    }

    /**
     * 准备播放
     *
     * @param songs 播放歌曲的路径
     */
    private void prepare(final Song songs) {
        prepare(songs.getUrl(), true);
    }

    /**
     * 根据当前播放模式，切换到上一首或者下一首
     *
     * @param isNext 是否是播放下一首
     */
    public void playNextOrPrev(boolean isNext) {
        if (Global.PlayQueue == null || Global.PlayQueue.size() == 0) {
            ToastUtil.show(mContext, getString(R.string.list_is_empty));
            return;
        }
        LogUtil.d(TAG, "播放下一首");
        if (isNext) {
            //如果是点击下一首 播放预先设置好的下一首歌曲
            mCurrentId = mNextId;
            mCurrentIndex = mNextIndex;
            mCurrentSong = new Song(mNextSong);
        } else {
            //如果点击上一首
            if ((--mCurrentIndex) < 0) {
                mCurrentIndex = Global.PlayQueue.size() - 1;
            }
            if (mCurrentIndex == -1 || (mCurrentIndex > Global.PlayQueue.size() - 1)) {
                return;
            }
            mCurrentId = mPlayModel == Constants.PLAY_SHUFFLE ? mRandomList.get(mCurrentIndex)
                    : Global.PlayQueue.get(mCurrentIndex);
            mCurrentSong = MediaStoreUtil.getMP3InfoById(mCurrentId);
            mNextIndex = mCurrentIndex;
            mNextId = mCurrentId;
        }
        updateNextSong();
        if (mCurrentSong == null) {
            ToastUtil.show(mContext, R.string.song_lose_effect);
            return;
        }
        mIsplay = true;
        prepare(mCurrentSong);
    }

    /**
     * 更新下一首歌曲
     */
    public void updateNextSong() {
        if (Global.PlayQueue == null || Global.PlayQueue.size() == 0) {
            ToastUtil.show(mContext, R.string.list_is_empty);
            return;
        }

        if (mPlayModel == Constants.PLAY_SHUFFLE) {
            if (mRandomList.size() == 0) {
                makeShuffleList(mCurrentId);
            }
            if ((++mNextIndex) > mRandomList.size() - 1) {
                mNextIndex = 0;
            }
            mNextId = mRandomList.get(mNextIndex);
        } else {
            if ((++mNextIndex) > Global.PlayQueue.size() - 1) {
                mNextIndex = 0;
            }
            mNextId = Global.PlayQueue.get(mNextIndex);
        }
        mNextSong = MediaStoreUtil.getMP3InfoById(mNextId);
    }

    /**
     * 获得MediaPlayer
     */
    public static MediaPlayer getMediaPlayer() {
        return mInstance.mMediaPlayer;
    }

    /**
     * 获得播放模式
     */
    public static int getPlayModel() {
        return mPlayModel;
    }

    /**
     * 设置播放模式并更新下一首歌曲
     */
    public void setPlayModel(int playModel) {
        mPlayModel = playModel;
        updateAppwidget();
        SPUtil.putValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME, SPUtil.SETTING_KEY.PLAY_MODEL,
                mPlayModel);
        //保存正在播放和下一首歌曲
        SPUtil.putValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME, SPUtil.SETTING_KEY.NEXT_SONG_ID,
                mNextId);
        SPUtil.putValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME, SPUtil.SETTING_KEY.LAST_SONG_ID,
                mCurrentId);
        if (mPlayModel == Constants.PLAY_SHUFFLE) {
            mRandomList.clear();
            makeShuffleList(mCurrentId);
        }
    }

    /**
     * 生成随机播放列表
     */
    public void makeShuffleList(int current) {
        if (mRandomList == null) {
            mRandomList = new ArrayList<>();
        }
        mRandomList.clear();
        mRandomList.addAll(Global.PlayQueue);
        if (mRandomList.isEmpty()) {
            return;
        }
        Collections.shuffle(mRandomList);
    }

    /**
     * 获得是否正在播放
     */
    public static boolean isPlay() {
        return mIsplay;
    }

    /**
     * 设置MediaPlayer播放进度
     */
    public static void setProgress(int current) {
        if (getMediaPlayer() != null) {
            getMediaPlayer().seekTo(current);
        }
    }

    /**
     * 返回当前播放歌曲
     */
    public static Song getCurrentMP3() {
        return mCurrentSong;
    }

    /**
     * 返回下一首播放歌曲
     */
    public static Song getNextMP3() {
        return mNextSong;
    }

    public static MediaFormat getMediaFormat() {
        if (mCurrentSong == null) {
            return null;
        }
        try {
            mMediaExtractor = new MediaExtractor();
            mMediaExtractor.setDataSource(mCurrentSong.getUrl());
            return mMediaExtractor.getTrackFormat(0);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 获得歌曲码率信息
     *
     * @return type 0:码率 1:采样率 2:格式
     */
    public static String getRateInfo(int type) {
        MediaFormat mf = getMediaFormat();
        if (mf == null) {
            return "";
        }
        switch (type) {
            case Constants.BIT_RATE:
                if (mf.containsKey(MediaFormat.KEY_BIT_RATE)) {
                    return mf.getInteger(MediaFormat.KEY_BIT_RATE) / 1024 + "";
                } else {
                    long durationUs = mf.containsKey(MediaFormat.KEY_DURATION) ? mf.getLong(
                            MediaFormat.KEY_DURATION) : mCurrentSong.getDuration();
                    return mCurrentSong.getSize() * 8 / (durationUs / 1024) + "";
                }
            case Constants.SAMPLE_RATE:
                return mf.containsKey(MediaFormat.KEY_SAMPLE_RATE) ?
                        mf.getInteger(MediaFormat.KEY_SAMPLE_RATE) + "" :
                        "";
            case Constants.MIME:
                return mf.containsKey(MediaFormat.KEY_MIME) ?
                        mf.getString(MediaFormat.KEY_MIME) + "" :
                        "";
            default:
                return "";
        }
    }

    /**
     * 获得当前播放进度
     */
    public static int getProgress() {
        try {
            if (getMediaPlayer() != null && mIsInitialized) {
                return getMediaPlayer().getCurrentPosition();
            }
        } catch (IllegalStateException e) {
            LogUtil.d(TAG, "getProgress Error: " + e);
        }
        return 0;
    }

    public static long getDuration() {
        if (getMediaPlayer() != null && mIsInitialized) {
            return getMediaPlayer().getDuration();
        }
        return 0;
    }

    /**
     * 获得当前播放索引
     */
    public static int getCurrentPos() {
        return mCurrentIndex;
    }

    /**
     * 设置当前索引
     */
    public static void setCurrentPos(int pos) {
        mCurrentIndex = pos;
    }

    /**
     * 读取歌曲id列表与播放队列
     */
    private void loadSync() {
        mPlaybackHandler.post(this::loadAsync);
    }

    private void loadAsync() {
        final boolean isFirst = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME, "First",
                true);
        SPUtil.putValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME, "First", false);
        //读取sd卡歌曲id
        Global.AllSongList = MediaStoreUtil.getAllSongsId();
        //第一次启动软件
        if (isFirst) {
            try {
                //默认全部歌曲为播放队列
                Global.PlayQueueID = PlayListUtil.addPlayList(Constants.PLAY_QUEUE);
                Global.setPlayQueue(Global.AllSongList);
                SPUtil.putValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME, "PlayQueueID",
                        Global.PlayQueueID);
                //添加我的收藏列表
                Global.MyLoveID = PlayListUtil.addPlayList(getString(R.string.my_favorite));
                SPUtil.putValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME, "MyLoveID",
                        Global.MyLoveID);
                //保存默认主题设置
                SPUtil.putValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME, "ThemeMode",
                        ThemeStore.DAY);
                SPUtil.putValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME, "ThemeColor",
                        ThemeStore.THEME_BLUE);
                //通知栏样式
                SPUtil.putValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                        SPUtil.SETTING_KEY.NOTIFY_STYLE_CLASSIC,
                        !(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O));
            } catch (Exception e) {
                LogUtil.d(TAG, e.toString());
            }
        } else {
            //播放模式
            mPlayModel = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                    SPUtil.SETTING_KEY.PLAY_MODEL, Constants.PLAY_LOOP);
            Global.PlayQueueID = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                    "PlayQueueID", -1);
            Global.MyLoveID = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME, "MyLoveID",
                    -1);
            Global.PlayQueue = PlayListUtil.getIDList(Global.PlayQueueID);
            Global.PlayList = PlayListUtil.getAllPlayListInfo();
            mShowFloatLrc = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                    SPUtil.SETTING_KEY.FLOAT_LYRIC_SHOW, false);
        }

        //摇一摇
        if (SPUtil.getValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME, SPUtil.SETTING_KEY.SHAKE,
                false)) {
            ShakeDetector.getInstance().beginListen();
        }
        restoreLastSong();
        mLoadFinished = true;
        sendBroadcast(new Intent(ACTION_LOAD_FINISH));
    }

    /**
     * 初始化上一次退出时时正在播放的歌曲
     */
    private void restoreLastSong() {
        if (Global.PlayQueue == null || Global.PlayQueue.size() == 0) {
            return;
        }
        //读取上次退出时正在播放的歌曲的id
        int lastId = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                SPUtil.SETTING_KEY.LAST_SONG_ID, -1);
        //上次退出时正在播放的歌曲是否还存在
        boolean isLastSongExist = false;
        //上次退出时正在播放的歌曲的pos
        int pos = 0;
        //查找上次退出时的歌曲是否还存在
        if (lastId != -1) {
            for (int i = 0; i < Global.PlayQueue.size(); i++) {
                if (lastId == Global.PlayQueue.get(i)) {
                    isLastSongExist = true;
                    pos = i;
                    break;
                }
            }
        }

        Song item;
        mPlayAtBreakPoint = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                SPUtil.SETTING_KEY.PLAY_AT_BREAKPOINT, false);
        mLastProgress = mPlayAtBreakPoint ?
                SPUtil.getValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                        SPUtil.SETTING_KEY.LAST_PLAY_PROGRESS, 0) :
                0;
        //上次退出时保存的正在播放的歌曲未失效
        if (isLastSongExist && (item = MediaStoreUtil.getMP3InfoById(lastId)) != null) {
            setUpDataSource(item, pos);
        } else {
            mLastProgress = 0;
            SPUtil.putValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                    SPUtil.SETTING_KEY.LAST_PLAY_PROGRESS, 0);
            //重新找到一个歌曲id
            int id = Global.PlayQueue.get(0);
            for (int i = 0; i < Global.PlayQueue.size(); i++) {
                id = Global.PlayQueue.get(i);
                if (id != lastId) {
                    break;
                }
            }
            item = MediaStoreUtil.getMP3InfoById(id);
            SPUtil.putValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                    SPUtil.SETTING_KEY.LAST_SONG_ID, id);
            setUpDataSource(item, 0);
        }
    }

    /**
     * 开始或者停止计时
     */
    public void toggleTimer(boolean start, long duration) {
        if (start) {
            if (duration <= 0) {
                return;
            }
            mTimerUpdater = new TimerUpdater(duration, 1000);
            mTimerUpdater.start();
        } else {
            if (mTimerUpdater != null) {
                mTimerUpdater.cancel();
                mTimerUpdater = null;
            }
        }
    }

    /**
     * 剩余的计时时间
     */
    public long getMillUntilFinish() {
        synchronized (TimerUpdater.class) {
            return mMillisUntilFinish;
        }
    }

    /**
     * 播放完毕后是否关闭
     */
    private boolean mCloseAfter;

    /**
     * 定时关闭计时器
     */
    private class TimerUpdater extends CountDownTimer {
        TimerUpdater(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            synchronized (TimerUpdater.class) {
                mMillisUntilFinish = millisUntilFinished;
            }
        }

        @Override
        public void onFinish() {
            //如果当前正在播放 播放完当前歌曲再关闭
            if (mIsplay) {
                mCloseAfter = true;
            } else {
                sendBroadcast(new Intent(Constants.EXIT));
            }
        }
    }

    /**
     * 释放电源锁
     */
    private void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }

    /**
     * 获得电源锁
     */
    private void acquireWakeLock() {
        if (mWakeLock != null) {
            mWakeLock.acquire(30000L);
        }
    }

    /**
     * 更新桌面歌词
     */
    @SuppressLint("CheckResult")
    private void updateFloatLrc(boolean force) {
        final int control = Global.Operation;
        Observable.just(!checkNoPermission() && mShowFloatLrc && (control != Constants.TOGGLE
                                                                  && control != Constants.PAUSE
                                                                  && control != Constants.START
                                                                  || mLrcRows == null
                                                                  || force))
                .flatMap((Function<Boolean, ObservableSource<List<LrcRow>>>) filter -> filter
                        ? new SearchLrc(mCurrentSong).getLyric() : Observable.empty())
                .doOnSubscribe(disposable -> createFloatLrcThreadIfNeed())
                .subscribe(lrcRows -> mLrcRows = lrcRows, throwable -> mLrcRows = null);
    }

    /**
     * 创建更新桌面歌词的线程
     */
    private void createFloatLrcThreadIfNeed() {
        if (mShowFloatLrc && !isFloatLrcShowing()) {
            mUpdateFloatLrcThread = new UpdateFloatLrcThread();
            mUpdateFloatLrcThread.start();
        }
    }

    /**
     * 判断是否有悬浮窗权限
     * 没有权限关闭桌面歌词
     */
    private boolean checkNoPermission() {
        if (!FloatWindowManager.getInstance().checkPermission(mContext)) {
            closeFloatLrc();
            return true;
        }
        return false;
    }

    /**
     * 复制bitmap
     */
    public static Bitmap copy(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.RGB_565;
        }
        try {
            return bitmap.copy(config, false);
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 更新桌面部件
     */
    private void updateAppwidget() {
        if (mAppWidgetMedium != null) {
            mAppWidgetMedium.updateWidget(mContext, null, true);
        }
        if (mAppWidgetSmall != null) {
            mAppWidgetSmall.updateWidget(mContext, null, true);
        }
        if (mAppWidgetBig != null) {
            mAppWidgetBig.updateWidget(mContext, null, true);
        }
        //暂停停止更新进度条和时间
        if (!isPlay()) {
            if (mWidgetTimer != null) {
                mWidgetTimer.cancel();
                mWidgetTimer = null;
            }
            if (mWidgetTask != null) {
                mWidgetTask.cancel();
                mWidgetTask = null;
            }
        } else {
            //开始播放后持续更新进度条和时间
            if (mWidgetTimer != null) {
                return;
            }
            mWidgetTimer = new Timer();
            mWidgetTask = new WidgetTask();
            mWidgetTimer.schedule(mWidgetTask, 1000, 1000);
        }
    }

    /** 更新桌面歌词 */
    private static final int LRC_THRESHOLD = 400;
    private static final int LRC_INTERVAL = 400;
    //    public String mCurrentLrc;
    private UpdateFloatLrcThread mUpdateFloatLrcThread;
    private boolean mNeedShowFloatLrc;
    private FloatLrcContent mCurrentLrc = new FloatLrcContent();
    private LrcRow EMPTY_ROW = new LrcRow("", 0, "");

    private class UpdateFloatLrcThread extends Thread {
        UpdateFloatLrcThread() {
            LogUtil.d("DesktopLrc", "创建线程");
        }

        void quitByNotification() {
            interrupt();
            mNeedShowFloatLrc = true;
            mShowFloatLrc = false;
        }

        void quitImmediately() {
            interrupt();
        }

        void quitDelay() {
            mPlaybackHandler.postDelayed(this::quitImmediately, LRC_INTERVAL);
        }

        @Override
        public void run() {
            while (mShowFloatLrc) {
                try {
                    //                    int interval = getInterval();
                    //                    LogUtil.d("DesktopLrc","间隔:" + interval);
                    Thread.sleep(LRC_INTERVAL);
                } catch (InterruptedException e) {
                    LogUtil.d("DesktopLrc", "捕获异常,线程退出");
                    mUpdateUIHandler.sendEmptyMessage(Constants.REMOVE_FLOAT_LRC);
                    return;
                }
                LogUtil.d("DesktopLrc", "Thread:" + Thread.currentThread());
                //判断权限
                if (checkNoPermission()) {
                    return;
                }
                if (mIsServiceStop) {
                    mUpdateUIHandler.sendEmptyMessage(Constants.REMOVE_FLOAT_LRC);
                    return;
                }
                //当前应用在前台
                if (Util.isAppOnForeground(mContext)) {
                    if (isFloatLrcShowing()) {
                        mUpdateUIHandler.sendEmptyMessage(Constants.REMOVE_FLOAT_LRC);
                    }
                } else {
                    if (!isFloatLrcShowing()) {
                        mUpdateUIHandler.removeMessages(Constants.CREATE_FLOAT_LRC);
                        LogUtil.d("DesktopLrc", "请求创建桌面歌词");
                        mUpdateUIHandler.sendEmptyMessageDelayed(Constants.CREATE_FLOAT_LRC, 50);
                    } else {
                        if (mLrcRows == null || mLrcRows.size() == 0) {
                            mCurrentLrc.Line1 = new LrcRow("", 0,
                                    getResources().getString(R.string.no_lrc));
                            mCurrentLrc.Line2 = EMPTY_ROW;
                            mUpdateUIHandler.obtainMessage(Constants.UPDATE_FLOAT_LRC_CONTENT)
                                    .sendToTarget();
                            continue;
                        }
                        findCurrentLyric();
                    }
                }
            }
        }

        private void findCurrentLyric() {
            int progress = getProgress();
            if (mLrcRows == null || mLrcRows.size() == 0) {
                return;
            }
            for (int i = mLrcRows.size() - 1; i >= 0; i--) {
                LrcRow lrcRow = mLrcRows.get(i);
                int interval = progress - lrcRow.getTime();
                if (i == 0 && interval < 0) {
                    //未开始歌唱前显示歌曲信息
                    mCurrentLrc.Line1 = new LrcRow("", 0, mCurrentSong.getTitle());
                    mCurrentLrc.Line2 = new LrcRow("", 0,
                            mCurrentSong.getArtist() + " - " + mCurrentSong.getAlbum());
                    mUpdateUIHandler.obtainMessage(Constants.UPDATE_FLOAT_LRC_CONTENT)
                            .sendToTarget();
                    break;
                } else if (progress >= lrcRow.getTime()) {
                    if (lrcRow.hasTranslate()) {
                        mCurrentLrc.Line1 = new LrcRow(lrcRow);
                        mCurrentLrc.Line1.setContent(lrcRow.getContent());
                        mCurrentLrc.Line2 = new LrcRow(lrcRow);
                        mCurrentLrc.Line2.setContent(lrcRow.getTranslate());
                    } else {
                        mCurrentLrc.Line1 = lrcRow;
                        mCurrentLrc.Line2 = new LrcRow(
                                i + 1 < mLrcRows.size() ? mLrcRows.get(i + 1) : EMPTY_ROW);
                    }
                    mUpdateUIHandler.obtainMessage(Constants.UPDATE_FLOAT_LRC_CONTENT)
                            .sendToTarget();
                    break;
                }
            }
        }

        /**
         * 根据当前歌词获得间隔时间
         */
        private int getInterval() {
            return mCurrentLrc != null
                   && mCurrentLrc.Line1 != null
                   && mCurrentLrc.Line1.getTotalTime() > 0 ? (int) mCurrentLrc.Line1.getTotalTime()
                    : LRC_INTERVAL;
        }
    }

    /**
     * 创建桌面歌词悬浮窗
     */
    private boolean mIsFloatLrcInitializing = false;

    private void createFloatLrc() {
        if (checkNoPermission()) {
            return;
        }
        if (mIsFloatLrcInitializing) {
            return;
        }
        mIsFloatLrcInitializing = true;

        final WindowManager.LayoutParams param = new WindowManager.LayoutParams();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            param.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            param.type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        param.format = PixelFormat.RGBA_8888;
        param.gravity = Gravity.TOP;
        param.width = mContext.getResources().getDisplayMetrics().widthPixels;
        param.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        param.x = 0;
        param.y = SPUtil.getValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                SPUtil.SETTING_KEY.FLOAT_Y, 0);

        if (mFloatLrcView != null) {
            mWindowManager.removeView(mFloatLrcView);
            mFloatLrcView = null;
        }

        mFloatLrcView = new FloatLrcView(mContext);
        mWindowManager.addView(mFloatLrcView, param);
        mIsFloatLrcInitializing = false;
        LogUtil.d("DesktopLrc", "创建桌面歌词");
    }

    /**
     * 移除桌面歌词
     */
    private void removeFloatLrc() {
        if (mFloatLrcView != null) {
            LogUtil.d("DesktopLrc", "移除桌面歌词");
            mFloatLrcView.cancelNotify();
            mWindowManager.removeView(mFloatLrcView);
            mFloatLrcView = null;
        }
    }

    /**
     * 桌面歌词是否显示
     */
    private boolean isFloatLrcShowing() {
        return mFloatLrcView != null;
    }

    /**
     * 关闭桌面歌词
     */
    private void closeFloatLrc() {
        SPUtil.putValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                SPUtil.SETTING_KEY.FLOAT_LYRIC_SHOW, false);
        mShowFloatLrc = false;
        mUpdateFloatLrcThread = null;
        if (mLrcRows != null) {
            mLrcRows.clear();
        }
        mUpdateUIHandler.removeMessages(Constants.CREATE_FLOAT_LRC);
        mUpdateUIHandler.sendEmptyMessageDelayed(Constants.REMOVE_FLOAT_LRC, LRC_INTERVAL);
    }

    /** 更新桌面部件进度 */
    private Timer mWidgetTimer;
    private TimerTask mWidgetTask;

    private class WidgetTask extends TimerTask {
        @Override
        public void run() {
            if (mAppWidgetSmall != null) {
                mAppWidgetSmall.updateWidget(mContext, null, false);
            }
            if (mAppWidgetMedium != null) {
                mAppWidgetMedium.updateWidget(mContext, null, false);
            }
            if (mAppWidgetBig != null) {
                mAppWidgetBig.updateWidget(mContext, null, false);
            }
            final int progress = getProgress();
            if (progress > 0 && mPlayAtBreakPoint) {
                SPUtil.putValue(mContext, SPUtil.SETTING_KEY.SETTING_NAME,
                        SPUtil.SETTING_KEY.LAST_PLAY_PROGRESS, progress);
            }
        }
    }

    private class AudioFocusChangeListener implements AudioManager.OnAudioFocusChangeListener {
        //记录焦点变化之前是否在播放;
        private boolean mNeedContinue = false;

        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN://获得AudioFocus
                    mAudioFocus = true;
                    if (mMediaPlayer == null) {
                        init();
                    } else if (mNeedContinue) {
                        play(true);
                        mNeedContinue = false;
                        Global.setOperation(Constants.TOGGLE);
                    }
                    mVolumeController.to(1);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT://短暂暂停
                    mNeedContinue = mIsplay;
                    if (mIsplay && mMediaPlayer != null) {
                        Global.setOperation(Constants.TOGGLE);
                        pause(false);
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK://减小音量
                    mVolumeController.to(.1f);
                    break;
                case AudioManager.AUDIOFOCUS_LOSS://暂停
                    mAudioFocus = false;
                    if (mIsplay && mMediaPlayer != null) {
                        Global.setOperation(Constants.TOGGLE);
                        pause(false);
                    }
                    break;
            }
            //通知更新ui
            mUpdateUIHandler.sendEmptyMessage(Constants.UPDATE_UI);
        }
    }

    /**
     * 记录在一秒中线控按下的次数
     */
    private class SessionCallBack extends MediaSessionCompat.Callback {
        private HeadSetRunnable mHeadsetRunnable;
        private int mHeadSetHookCount = 0;

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            if (mediaButtonEvent == null) {
                return true;
            }
            KeyEvent event = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
            if (event == null) {
                return true;
            }

            boolean isActionUp = (event.getAction() == KeyEvent.ACTION_UP);
            if (!isActionUp) {
                return true;
            }

            Intent intent = new Intent(ACTION_CMD);
            int keyCode = event.getKeyCode();
            if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                ||
                keyCode == KeyEvent.KEYCODE_MEDIA_NEXT
                ||
                keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS
                || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY
                || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                intent.putExtra("Control",
                        keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                        || keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE
                        || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY ?
                                Constants.TOGGLE :
                                keyCode == KeyEvent.KEYCODE_MEDIA_NEXT ? Constants.NEXT
                                        : Constants.PREV);
                mContext.sendBroadcast(intent);
                return true;
            }
            //如果是第一次按下，开启一条线程去判断用户操作
            if (mHeadSetHookCount == 0) {
                if (mHeadsetRunnable == null) {
                    mHeadsetRunnable = new HeadSetRunnable();
                }
                mUpdateUIHandler.postDelayed(mHeadsetRunnable, 800);
            }
            if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK) {
                mHeadSetHookCount++;
            }
            return true;
        }

        private class HeadSetRunnable implements Runnable {
            @Override
            public void run() {
                Intent intent = new Intent(ACTION_CMD);
                intent.putExtra("Control", mHeadSetHookCount == 1 ? Constants.TOGGLE
                        : mHeadSetHookCount == 2 ? Constants.NEXT : Constants.PREV);
                mContext.sendBroadcast(intent);
                mHeadSetHookCount = 0;
            }
        }
    }

    private static class PlaybackHandler extends Handler {
        private final WeakReference<MusicService> mRef;

        PlaybackHandler(MusicService service, Looper looper) {
            super(looper);
            mRef = new WeakReference<>(service);
        }
    }

    private static class UpdateUIHandler extends Handler {
        private final WeakReference<MusicService> mRef;

        UpdateUIHandler(MusicService service) {
            super();
            mRef = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mRef.get() == null) {
                return;
            }
            MusicService musicService = mRef.get();
            switch (msg.what) {
                case Constants.UPDATE_UI:
                    musicService.updateAppwidget();
                    if (musicService.mNeedShowFloatLrc) {
                        musicService.mShowFloatLrc = true;
                        musicService.mNeedShowFloatLrc = false;
                    }
                    musicService.updateFloatLrc(false);
                    UpdateHelper.update(mCurrentSong, mIsplay);
                    break;
                case Constants.UPDATE_FLOAT_LRC_CONTENT:
                    if (musicService.mFloatLrcView != null) {
                        if (musicService.mCurrentLrc != null) {
                            musicService.mFloatLrcView.setText(musicService.mCurrentLrc.Line1,
                                    musicService.mCurrentLrc.Line2);
                        }
                    }
                    break;
                case Constants.REMOVE_FLOAT_LRC:
                    musicService.removeFloatLrc();
                    break;
                case Constants.CREATE_FLOAT_LRC:
                    musicService.createFloatLrc();
                    break;
                case Constants.UPDATE_ADAPTER:
                    musicService.handleMusicEvent(new Intent(ACTION_MEDIA_CHANGE));
                    break;
                case Constants.UPDATE_PLAYLIST:
                    musicService.handleMusicEvent(new Intent(ACTION_PLAYLIST_CHANGE));
                    break;
            }
        }
    }

    private class ScreenReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_ON.equals(action)) {
                //显示锁屏
                if (MusicService.isPlay()
                    && SPUtil.getValue(context, SPUtil.SETTING_KEY.SETTING_NAME,
                        SPUtil.SETTING_KEY.LOCKSCREEN, Constants.APLAYER_LOCKSCREEN)
                       == Constants.APLAYER_LOCKSCREEN) {
                    context.startActivity(new Intent(context, LockScreenActivity.class).addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK));
                }
                //重新显示桌面歌词
                createFloatLrcThreadIfNeed();
            } else {
                //屏幕熄灭 关闭桌面歌词
                if (mShowFloatLrc && isFloatLrcShowing()) {
                    mUpdateFloatLrcThread.quitImmediately();
                }
            }
        }
    }
}
