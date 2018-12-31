package com.mb.android.media;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Presentation;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaRouter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.text.Html;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.mb.android.api.ApiClientBridge;
import com.mb.android.logging.AppLogger;
import com.mb.android.preferences.PreferencesActivity;

import org.jellyfin.mobile.BuildConfig;
import org.jellyfin.mobile.R;
import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.AndroidUtil;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import mediabrowser.apiinteraction.ApiClient;
import mediabrowser.apiinteraction.ApiEventListener;
import mediabrowser.apiinteraction.android.AndroidApiClient;
import mediabrowser.apiinteraction.android.AndroidDevice;
import mediabrowser.apiinteraction.android.GsonJsonSerializer;
import mediabrowser.apiinteraction.android.VolleyHttpClient;
import mediabrowser.apiinteraction.android.mediabrowser.Constants;
import mediabrowser.apiinteraction.device.IDevice;
import mediabrowser.apiinteraction.http.IAsyncHttpClient;
import mediabrowser.model.dlna.DeviceProfile;
import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.dto.NameValuePair;
import mediabrowser.model.entities.MediaStream;
import mediabrowser.model.entities.MediaStreamType;
import mediabrowser.model.extensions.StringHelper;
import mediabrowser.model.logging.ILogger;
import mediabrowser.model.mediainfo.SubtitleTrackEvent;
import mediabrowser.model.mediainfo.SubtitleTrackInfo;
import mediabrowser.model.serialization.IJsonSerializer;
import mediabrowser.model.session.PlayMethod;
import mediabrowser.model.session.PlaybackProgressInfo;

public class VideoPlayerActivity extends AppCompatActivity implements IVLCVout.Callback,
        IPlaybackSettingsController, PlaybackService.Client.Callback, PlaybackService.Callback,
        OnClickListener, View.OnLongClickListener, ScaleGestureDetector.OnScaleGestureListener {

    public final static String TAG = "VLC/VideoPlayerActivity";
    // Internal intent identifier to distinguish between internal launch and
    // external intent.
    public final static String PLAY_FROM_VIDEOGRID = Strings.buildPkgString("gui.video.PLAY_FROM_VIDEOGRID");
    public final static String PLAY_FROM_SERVICE = Strings.buildPkgString("gui.video.PLAY_FROM_SERVICE");
    public final static String EXIT_PLAYER = Strings.buildPkgString("gui.video.EXIT_PLAYER");
    public final static String PLAY_EXTRA_ITEM_LOCATION = "item_location";
    public final static String PLAY_EXTRA_SUBTITLES_LOCATION = "subtitles_location";
    public final static String PLAY_EXTRA_ITEM_TITLE = "title";
    public final static String PLAY_EXTRA_FROM_START = "from_start";
    public final static String PLAY_EXTRA_START_TIME = "position";
    public final static String PLAY_EXTRA_OPENED_POSITION = "opened_position";
    public final static String PLAY_DISABLE_HARDWARE = "disable_hardware";
    public final static String ACTION_RESULT = Strings.buildPkgString("player.result");
    public final static String EXTRA_POSITION = "extra_position";
    public final static String EXTRA_DURATION = "extra_duration";
    public final static String EXTRA_URI = "extra_uri";
    public final static int RESULT_CONNECTION_FAILED = RESULT_FIRST_USER + 1;
    public final static int RESULT_PLAYBACK_ERROR = RESULT_FIRST_USER + 2;
    public final static int RESULT_HARDWARE_ACCELERATION_ERROR = RESULT_FIRST_USER + 3;
    public final static int RESULT_VIDEO_TRACK_LOST = RESULT_FIRST_USER + 4;

    private final PlaybackServiceActivity.Helper mHelper = new PlaybackServiceActivity.Helper(this, this);
    private PlaybackService mService;
    private SurfaceView mSurfaceView = null;
    private SurfaceView mSubtitlesSurfaceView = null;
    private View mRootView;
    private FrameLayout mSurfaceFrame;
    private MediaRouter mMediaRouter;
    private MediaRouter.SimpleCallback mMediaRouterCallback;
    private SecondaryDisplay mPresentation;
    private int mPresentationDisplayId = -1;
    private Uri mUri;
    private boolean mAskResume = true;
    private ScaleGestureDetector mScaleGestureDetector;
    private GestureDetectorCompat mDetector = null;
    //private ImageView mPlaylistToggle;
    //private RecyclerView mPlaylist;
    //private PlaylistAdapter mPlaylistAdapter;
    private ImageView mPlaylistNext;
    private ImageView mPlaylistPrevious;

    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_SCREEN = 1;
    private static final int SURFACE_FILL = 2;
    private static final int SURFACE_16_9 = 3;
    private static final int SURFACE_4_3 = 4;
    private static final int SURFACE_ORIGINAL = 5;
    private int mCurrentSize;
    private SharedPreferences mSettings;
    private int mTouchControls = 0;

    /** Overlay */
    private ActionBar mActionBar;
    private ViewGroup mActionBarView;
    private View mOverlayProgress;
    private View mOverlayBackground;
    private View mOverlayButtons;
    private static final int OVERLAY_TIMEOUT = 4000;
    private static final int OVERLAY_INFINITE = -1;
    private static final int FADE_OUT = 1;
    private static final int SHOW_PROGRESS = 2;
    private static final int FADE_OUT_INFO = 3;
    private static final int START_PLAYBACK = 4;
    private static final int AUDIO_SERVICE_CONNECTION_FAILED = 5;
    private static final int RESET_BACK_LOCK = 6;
    private static final int CHECK_VIDEO_TRACKS = 7;
    private static final int LOADING_ANIMATION = 8;

    private static final int LOADING_ANIMATION_DELAY = 1000;

    private boolean mDragging;
    private boolean mShowing;
    private DelayState mPlaybackSetting = DelayState.OFF;
    private SeekBar mSeekbar;
    private TextView mTitle;
    private TextView mSysTime;
    private TextView mBattery;
    private TextView mTime;
    private TextView mLength;
    private TextView mInfo;
    private View mOverlayInfo;
    private View mVerticalBar;
    private View mVerticalBarProgress;
    private boolean mIsLoading;
    private boolean mIsPlaying = false;
    private ImageView mLoading;
    private ImageView mTipsBackground;
    private ImageView mPlayPause;
    private ImageView mTracks;
    //private ImageView mNavMenu;
    private ImageView mRewind;
    private ImageView mForward;
   // private ImageView mAdvOptions;
    private ImageView mPlaybackSettingPlus;
    private ImageView mPlaybackSettingMinus;
    private View mObjectFocused;
    private boolean mEnableBrightnessGesture;
    private boolean mEnableCloneMode;
    private boolean mDisplayRemainingTime;
    private int mScreenOrientation;
    private int mScreenOrientationLock;
    private ImageView mLock;
    private ImageView mSize;
    private String KEY_REMAINING_TIME_DISPLAY = "remaining_time_display";
    private String KEY_BLUETOOTH_DELAY = "key_bluetooth_delay";
    private long mSpuDelay = 0;
    private long mAudioDelay = 0;
    private boolean mRateHasChanged = false;

    private boolean mIsLocked = false;
    /* -1 is a valid track (Disable) */
    private int mLastAudioTrack = -2;
    private int mLastSpuTrack = -2;
    private int mOverlayTimeout = 0;
    private boolean mLockBackButton = false;
    boolean mWasPaused = false;
    private long mSavedTime = -1;
    private float mSavedRate = 1.f;

    /**
     * For uninterrupted switching between audio and video mode
     */
    private boolean mSwitchingView;
    private boolean mSwitchToPopup;
    private boolean mHasSubItems = false;

    // Playlist
    private int savedIndexPosition = -1;

    // size of the video
    private int mVideoHeight;
    private int mVideoWidth;
    private int mVideoVisibleHeight;
    private int mVideoVisibleWidth;
    private int mSarNum;
    private int mSarDen;
    //Volume
    private AudioManager mAudioManager;
    private int mAudioMax;
    private boolean mMute = false;
    private int mVolSave;
    private float mVol;
    //Touch Events
    private static final int TOUCH_NONE = 0;
    private static final int TOUCH_VOLUME = 1;
    private static final int TOUCH_BRIGHTNESS = 2;
    private static final int TOUCH_MOVE = 3;
    private static final int TOUCH_SEEK = 4;
    private int mTouchAction = TOUCH_NONE;
    private int mSurfaceYDisplayRange, mSurfaceXDisplayRange;
    private float mFov;
    private float mInitTouchY, mTouchY =-1f, mTouchX=-1f;
    //stick event
    private static final int JOYSTICK_INPUT_DELAY = 300;
    private long mLastMove;
    // Brightness
    private boolean mIsFirstBrightnessGesture = true;
    private float mRestoreAutoBrightness = -1f;

    /**
     * Used to store a selected subtitle; see onActivityResult.
     * It is possible to have multiple custom subs in one session
     * (just like desktop VLC allows you as well.)
     */
    private final ArrayList<String> mSubtitleSelectedFiles = new ArrayList<String>();

    // Whether fallback from HW acceleration to SW decoding was done.
    private boolean mDisabledHardwareAcceleration = false;

    /**
     * Flag to indicate whether the media should be paused once loaded
     * (e.g. lock screen, or to restore the pause state)
     */
    private boolean mPlaybackStarted = false;
    // Tips
    private View mOverlayTips;
    private static final String PREF_TIPS_SHOWN = "video_player_tips_shown";

    // Navigation handling (DVD, Blu-Ray...)
    private int mMenuIdx = -1;
    private boolean mIsNavMenu = false;
    /* for getTime and seek */
    private long mForcedTime = -1;
    private long mLastTime = -1;
    private OnLayoutChangeListener mOnLayoutChangeListener;
    private AlertDialog mAlertDialog;

    DisplayMetrics mScreen = new DisplayMetrics();

    private String videoQualityOptionsJson;
    private ILogger logger;
    private IJsonSerializer jsonSerializer = new GsonJsonSerializer();
    private IAsyncHttpClient httpClient;
    private VideoApiHelper apiHelper;

    // Tracks & Subtitles
    private ArrayList<NameValuePair> mAudioTracksList;
    private ArrayList<NameValuePair> mSubtitleTracksList;
    private ArrayList<NameValuePair> mQualityList;

    private TextView subtitleText;

    private Timer timeLimitTimer;

    private static LibVLC LibVLC(Context context, ILogger logger) {
        return VLCInstance.get(context, logger);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!VLCInstance.testCompatibleCPU(this)) {
            exit(RESULT_CANCELED);
            return;
        }
        logger = AppLogger.getLogger(getApplicationContext());
        httpClient = getHttpClient();
        apiHelper = new VideoApiHelper(this, logger, jsonSerializer);

        if (AndroidUtil.isJellyBeanMR1OrLater()) {
            // Get the media router service (Miracast)
            mMediaRouter = (MediaRouter) getApplicationContext().getSystemService(Context.MEDIA_ROUTER_SERVICE);
            mMediaRouterCallback = new MediaRouter.SimpleCallback() {
                @Override
                public void onRoutePresentationDisplayChanged(
                        MediaRouter router, MediaRouter.RouteInfo info) {
                    logger.Debug("onRoutePresentationDisplayChanged: info=" + info);
                    final Display presentationDisplay = info.getPresentationDisplay();
                    final int newDisplayId = presentationDisplay != null ? presentationDisplay.getDisplayId() : -1;
                    if (newDisplayId != mPresentationDisplayId)
                        removePresentation();
                }
            };
            logger.Debug("MediaRouter information : " + mMediaRouter.toString());
        }

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        if (!false) {
            mTouchControls = (mSettings.getBoolean("enable_volume_gesture", true) ? 1 : 0)
                    + (mSettings.getBoolean("enable_brightness_gesture", true) ? 2 : 0);
        }
        /* Services and miscellaneous */
        mAudioManager = (AudioManager) getApplicationContext().getSystemService(AUDIO_SERVICE);
        mAudioMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mEnableCloneMode = mSettings.getBoolean("enable_clone_mode", false);
        createPresentation();
        setContentView(mPresentation == null ? R.layout.player : R.layout.player_remote_control);

        /** initialize Views an their Events */
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        mActionBar.setBackgroundDrawable(null);
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setCustomView(R.layout.player_action_bar);
        mRootView = findViewById(R.id.player_root);
        mActionBarView = (ViewGroup) mActionBar.getCustomView();
        mTitle = (TextView) mActionBarView.findViewById(R.id.player_overlay_title);
        if (!AndroidUtil.isJellyBeanOrLater()) {
            mSysTime = (TextView) findViewById(R.id.player_overlay_systime);
            mBattery = (TextView) findViewById(R.id.player_overlay_battery);
        }
        //mPlaylistToggle = (ImageView) findViewById(R.id.playlist_toggle);
        //mPlaylist = (RecyclerView) findViewById(R.id.video_playlist);
        mOverlayProgress = findViewById(R.id.progress_overlay);
        RelativeLayout.LayoutParams layoutParams =
                (RelativeLayout.LayoutParams)mOverlayProgress.getLayoutParams();
        if (AndroidDevices.isPhone(getApplicationContext()) || !AndroidDevices.hasNavBar()) {
            layoutParams.width = LayoutParams.MATCH_PARENT;
        } else {
            layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        }
        mOverlayProgress.setLayoutParams(layoutParams);
        mOverlayBackground = findViewById(R.id.player_overlay_background);
        mOverlayButtons =  findViewById(R.id.player_overlay_buttons);
        // Position and remaining time
        mTime = (TextView) findViewById(R.id.player_overlay_time);
        mLength = (TextView) findViewById(R.id.player_overlay_length);
        // the info textView is not on the overlay
        mInfo = (TextView) findViewById(R.id.player_overlay_textinfo);
        mOverlayInfo = findViewById(R.id.player_overlay_info);
        mVerticalBar = findViewById(R.id.verticalbar);
        mVerticalBarProgress = findViewById(R.id.verticalbar_progress);
        mScreenOrientation = Integer.valueOf(
                mSettings.getString("screen_orientation", "99" /*SCREEN ORIENTATION SENSOR*/));
        mPlayPause = (ImageView) findViewById(R.id.player_overlay_play);
        mTracks = (ImageView) findViewById(R.id.player_overlay_tracks);
        mTracks.setOnClickListener(this);
        //mAdvOptions = (ImageView) findViewById(R.id.player_overlay_adv_function);
        //mAdvOptions.setOnClickListener(this);
        mLock = (ImageView) findViewById(R.id.lock_overlay_button);
        mSize = (ImageView) findViewById(R.id.player_overlay_size);
        mCurrentSize = SURFACE_BEST_FIT;
        //mNavMenu = (ImageView) findViewById(R.id.player_overlay_navmenu);
        /*if (mSettings.getBoolean("enable_seek_buttons", false))
            initSeekButton();*/
        mPlaybackSettingPlus = (ImageView) findViewById(R.id.player_delay_plus);
        mPlaybackSettingMinus = (ImageView) findViewById(R.id.player_delay_minus);
        mSurfaceView = (SurfaceView) findViewById(R.id.player_surface);
        mSubtitlesSurfaceView = (SurfaceView) findViewById(R.id.subtitles_surface);
        mSubtitlesSurfaceView.setZOrderMediaOverlay(true);
        mSubtitlesSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        mSurfaceFrame = (FrameLayout) findViewById(R.id.player_surface_frame);
        mSeekbar = (SeekBar) findViewById(R.id.player_overlay_seekbar);
        /* Loading view */
        mLoading = (ImageView) findViewById(R.id.player_overlay_loading);
        if (mPresentation != null)
            mTipsBackground = (ImageView) findViewById(R.id.player_remote_tips_background);
        dimStatusBar(true);
        mHandler.sendEmptyMessageDelayed(LOADING_ANIMATION, LOADING_ANIMATION_DELAY);
        mSwitchingView = false;
        mAskResume = mSettings.getBoolean("dialog_confirm_resume", false);
        mDisplayRemainingTime = mSettings.getBoolean(KEY_REMAINING_TIME_DISPLAY, false);
        // Clear the resume time, since it is only used for resumes in external
        // videos.
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putLong(PreferencesActivity.VIDEO_RESUME_TIME, -1);
        // Also clear the subs list, because it is supposed to be per session
        // only (like desktop VLC). We don't want the custom subtitle files
        // to persist forever with this video.
        //editor.putString(PreferencesActivity.VIDEO_SUBTITLE_FILES, null);
        // Paused flag - per session too, like the subs list.
        //editor.remove(PreferencesActivity.VIDEO_PAUSED);
        editor.apply();
        IntentFilter filter = new IntentFilter();
        if (mBattery != null)
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Constants.SLEEP_INTENT);
        registerReceiver(mReceiver, filter);
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        // 100 is the value for screen_orientation_start_lock
        setRequestedOrientation(getScreenOrientation(mScreenOrientation));
        // Extra initialization when no secondary display is detected
        if (mPresentation == null) {
            // Orientation
            // Tips
            mOverlayTips = findViewById(R.id.player_overlay_tips);
            if(BuildConfig.DEBUG || false || mSettings.getBoolean(PREF_TIPS_SHOWN, false))
                mOverlayTips.setVisibility(View.GONE);
            else {
                mOverlayTips.bringToFront();
                mOverlayTips.invalidate();
            }
            //Set margins for TV overscan
            /*if (VLCApplication.showTvUi()) {
                int hm = getResources().getDimensionPixelSize(R.dimen.tv_overscan_horizontal);
                int vm = getResources().getDimensionPixelSize(R.dimen.tv_overscan_vertical);
                RelativeLayout uiContainer = (RelativeLayout) findViewById(R.id.player_ui_container);
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) uiContainer.getLayoutParams();
                lp.setMargins(hm, 0, hm, vm);
                uiContainer.setLayoutParams(lp);
                LinearLayout.LayoutParams titleParams = (LinearLayout.LayoutParams) mTitle.getLayoutParams();
                titleParams.setMargins(0, vm, 0, 0);
                mTitle.setLayoutParams(titleParams);
            }*/
        }
        resetHudLayout();
        getWindowManager().getDefaultDisplay().getMetrics(mScreen);

        subtitleText = (TextView) findViewById(R.id.offLine_subtitleText);
        mLoading.setVisibility(View.INVISIBLE);
    }

    private void updateManualSubtitlePosition(int topMargin) {

        TextView element = subtitleText;

        if (element == null) {
            logger.Error("Cannot proceed with updateManualSubtitlePosition because subtitleText element is null");
            return;
        }

        ViewGroup.LayoutParams layoutParams = element.getLayoutParams();

        if (layoutParams == null) {
            logger.Error("Cannot proceed with updateManualSubtitlePosition because layoutParams is null");
            return;
        }

        if (!(layoutParams instanceof FrameLayout.LayoutParams)) {
            logger.Error("Cannot convert ViewGroup.LayoutParams to FrameLayout.LayoutParams");
            return;
        }

        /*
		 * Adjust subtitles margin based on Screen dimes
		 */
        FrameLayout.LayoutParams rl2 = (FrameLayout.LayoutParams) layoutParams;
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        rl2.topMargin = topMargin;
        subtitleText.setLayoutParams(rl2);
    }

    protected IAsyncHttpClient getHttpClient() {

        IAsyncHttpClient httpClient = null;
        if (ApiClientBridge.Current != null) {
            httpClient = ApiClientBridge.Current.httpClient;
        }

        if (httpClient == null) {
            httpClient = new VolleyHttpClient(logger, getApplicationContext());
        }

        return httpClient;
    }
    @Override
    protected void onResume() {
        super.onResume();
        /*
         * Set listeners here to avoid NPE when activity is closing
         */
        mSeekbar.setOnSeekBarChangeListener(mSeekListener);
        mLock.setOnClickListener(this);
        mPlayPause.setOnClickListener(this);
        mPlayPause.setOnLongClickListener(this);
        mLength.setOnClickListener(this);
        mTime.setOnClickListener(this);
        mSize.setOnClickListener(this);
        //mNavMenu.setOnClickListener(this);
        if (mIsLocked && mScreenOrientation == 99)
            setRequestedOrientation(mScreenOrientationLock);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {

    }
    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        if (mPlaybackStarted && mService.getCurrentMediaWrapper() != null) {
            Uri uri = intent.hasExtra(PLAY_EXTRA_ITEM_LOCATION) ?
                    (Uri) intent.getExtras().getParcelable(PLAY_EXTRA_ITEM_LOCATION) : intent.getData();
            if (uri == null || uri.equals(mUri))
                return;
            if (TextUtils.equals("file", uri.getScheme()) && uri.getPath().startsWith("/sdcard")) {
                Uri convertedUri = FileUtils.convertLocalUri(uri);
                if (convertedUri == null || convertedUri.equals(mUri))
                    return;
                else
                    uri = convertedUri;
            }
            mUri = uri;
            mTitle.setText(mService.getCurrentMediaWrapper().getTitle());
            /*if (mPlaylist.getVisibility() == View.VISIBLE) {
                mPlaylistAdapter.setCurrentIndex(mService.getCurrentMediaPosition());
                mPlaylist.setVisibility(View.GONE);
            }*/
            showTitle();
            initUI();
            initVideoParams();
            mForcedTime = mLastTime = -1;
            setOverlayProgress();
        }
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onPause() {
        super.onPause();
        hideOverlay(true);
        mSeekbar.setOnSeekBarChangeListener(null);
        mLock.setOnClickListener(null);
        mPlayPause.setOnClickListener(null);
        mPlayPause.setOnLongClickListener(null);
        mLength.setOnClickListener(null);
        mTime.setOnClickListener(null);
        mSize.setOnClickListener(null);
        /* Stop the earliest possible to avoid vout error */
        if (isFinishing() || (AndroidDevices.isAndroidTv() && !requestVisibleBehind(true)))
            stopPlayback();
    }
    @Override
    public void onVisibleBehindCanceled() {
        super.onVisibleBehindCanceled();
        stopPlayback();
        exitOK();
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        if (!AndroidUtil.isHoneycombOrLater())
            changeSurfaceLayout();
        super.onConfigurationChanged(newConfig);
        resetHudLayout();
    }
    public void resetHudLayout() {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams)mOverlayButtons.getLayoutParams();
        int orientation = getScreenOrientation(100);
        boolean portrait = orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT ||
                orientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
        if (portrait) {
            layoutParams.addRule(RelativeLayout.BELOW, R.id.player_overlay_length);
            layoutParams.addRule(RelativeLayout.RIGHT_OF, 0);
            layoutParams.addRule(RelativeLayout.LEFT_OF, 0);
        } else {
            layoutParams.addRule(RelativeLayout.BELOW, R.id.player_overlay_seekbar);
            layoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.player_overlay_time);
            layoutParams.addRule(RelativeLayout.LEFT_OF, R.id.player_overlay_length);
        }
        mOverlayButtons.setLayoutParams(layoutParams);
    }
    @Override
    protected void onStart() {
        super.onStart();
        //VLCApplication.getMLInstance().pauseBackgroundOperations();
        mHelper.onStart();
        if (mSettings.getBoolean("save_brightness", false)) {
            float brightness = mSettings.getFloat("brightness_value", -1f);
            if (brightness != -1f)
                setWindowBrightness(brightness);
        }
        IntentFilter filter = new IntentFilter(PLAY_FROM_SERVICE);
        filter.addAction(EXIT_PLAYER);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                mServiceReceiver, filter);
        if (mBtReceiver != null) {
            IntentFilter btFilter = new IntentFilter(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
            btFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
            registerReceiver(mBtReceiver, btFilter);
        }
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onStop() {
        super.onStop();
        stopTimeLimitTimer();

        //VLCApplication.getMLInstance().resumeBackgroundOperations();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mServiceReceiver);
        if (mBtReceiver != null)
            unregisterReceiver(mBtReceiver);
        if (mAlertDialog != null && mAlertDialog.isShowing())
            mAlertDialog.dismiss();
        if (!isFinishing() && mService != null && mService.isPlaying() &&
                mSettings.getBoolean(PreferencesActivity.VIDEO_BACKGROUND, false)) {
            switchToAudioMode(false);
        }
        stopPlayback();
        SharedPreferences.Editor editor = mSettings.edit();
        if (mSavedTime != -1)
            editor.putLong(PreferencesActivity.VIDEO_RESUME_TIME, mSavedTime);
        //editor.putFloat(PreferencesActivity.VIDEO_RATE, mSavedRate);
        // Save selected subtitles
        String subtitleList_serialized = null;
        if(mSubtitleSelectedFiles.size() > 0) {
            Log.d(TAG, "Saving selected subtitle files");
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(mSubtitleSelectedFiles);
                subtitleList_serialized = bos.toString();
            } catch(IOException e) {}
        }
        editor.putString(PreferencesActivity.VIDEO_SUBTITLE_FILES, subtitleList_serialized);
        editor.apply();
        restoreBrightness();
        /*if (mSubtitlesGetTask != null)
            mSubtitlesGetTask.cancel(true);*/
        if (mService != null)
            mService.removeCallback(this);
        mHelper.onStop();
    }
    private void restoreBrightness() {
        /*if (mRestoreAutoBrightness != -1f) {
            int brightness = (int) (mRestoreAutoBrightness*255f);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    brightness);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        }
        // Save brightness if user wants to
        if (mSettings.getBoolean("save_brightness", false)) {
            float brightness = getWindow().getAttributes().screenBrightness;
            if (brightness != -1f) {
                SharedPreferences.Editor editor = mSettings.edit();
                editor.putFloat("brightness_value", brightness);
                editor.apply();
            }
        }*/
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiver != null)
            unregisterReceiver(mReceiver);
        // Dismiss the presentation when the activity is not visible.
        if (mPresentation != null) {
            Log.i(TAG, "Dismissing presentation because the activity is no longer visible.");
            mPresentation.dismiss();
            mPresentation = null;
        }
        mAudioManager = null;
    }
    /**
     * Add or remove MediaRouter callbacks. This is provided for version targeting.
     *
     * @param add true to add, false to remove
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void mediaRouterAddCallback(boolean add) {
        if(!AndroidUtil.isJellyBeanMR1OrLater() || mMediaRouter == null) return;
        if(add)
            mMediaRouter.addCallback(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, mMediaRouterCallback);
        else
            mMediaRouter.removeCallback(mMediaRouterCallback);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void startPlayback() {
        /* start playback only when audio service and both surfaces are ready */
        if (mPlaybackStarted || mService == null)
            return;
        mSavedRate = 1.0f;
        mSavedTime = -1;
        mPlaybackStarted = true;
        final IVLCVout vlcVout = mService.getVLCVout();
        if (vlcVout.areViewsAttached()) {
            if (mService.isPlayingPopup())
                mService.stop();
            vlcVout.detachViews();
        }
        if (mPresentation == null) {
            vlcVout.setVideoView(mSurfaceView);
            vlcVout.setSubtitlesView(mSubtitlesSurfaceView);
        } else {
            vlcVout.setVideoView(mPresentation.mSurfaceView);
            vlcVout.setSubtitlesView(mPresentation.mSubtitlesSurfaceView);
        }
        vlcVout.addCallback(this);
        vlcVout.attachViews();
        mService.setVideoTrackEnabled(true);
        initUI();
        loadMedia();
        //boolean ratePref = mSettings.getBoolean(PreferencesActivity.KEY_AUDIO_PLAYBACK_SPEED_PERSIST, true);
        mService.setRate(1.0F, false);
        initVideoParams();
        /*if (mService.hasPlaylist()) {
            mPlaylistPrevious = (ImageView) findViewById(R.id.playlist_previous);
            mPlaylistNext = (ImageView) findViewById(R.id.playlist_next);
            mPlaylistAdapter = new PlaylistAdapter(this);
            mPlaylistAdapter.setService(mService);
            final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
            mPlaylist.setLayoutManager(layoutManager);
            mPlaylistToggle.setVisibility(View.VISIBLE);
            mPlaylistPrevious.setVisibility(View.VISIBLE);
            mPlaylistNext.setVisibility(View.VISIBLE);
            mPlaylistToggle.setOnClickListener(VideoPlayerActivity.this);
            mPlaylistPrevious.setOnClickListener(VideoPlayerActivity.this);
            mPlaylistNext.setOnClickListener(VideoPlayerActivity.this);
            mSeekbar.setNextFocusUpId(mPlaylistToggle.getId());
            ItemTouchHelper.Callback callback =  new SwipeDragItemTouchHelperCallback(mPlaylistAdapter);
            ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
            touchHelper.attachToRecyclerView(mPlaylist);
            if (AndroidUtil.isJellyBeanMR1OrLater() && TextUtils.getLayoutDirectionFromLocale(Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL) {
                mPlaylistPrevious.setImageResource(R.drawable.ic_playlist_next_circle);
                mPlaylistNext.setImageResource(R.drawable.ic_playlist_previous_circle);
            }
        }*/
    }
    private void initUI() {
        cleanUI();
        /* Dispatch ActionBar touch events to the Activity */
        mActionBarView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                onTouchEvent(event);
                return true;
            }
        });
        if (AndroidUtil.isHoneycombOrLater()) {
            if (mOnLayoutChangeListener == null) {
                mOnLayoutChangeListener = new View.OnLayoutChangeListener() {
                    private final Runnable mRunnable = new Runnable() {
                        @Override
                        public void run() {
                            changeSurfaceLayout();
                        }
                    };
                    @Override
                    public void onLayoutChange(View v, int left, int top, int right,
                                               int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
                            /* changeSurfaceLayout need to be called after the layout changed */
                            mHandler.removeCallbacks(mRunnable);
                            mHandler.post(mRunnable);
                        }
                    }
                };
            }
            mSurfaceFrame.addOnLayoutChangeListener(mOnLayoutChangeListener);
        }
        changeSurfaceLayout();
        /* Listen for changes to media routes. */
        if (mMediaRouter != null)
            mediaRouterAddCallback(true);
        if (mRootView != null)
            mRootView.setKeepScreenOn(true);
    }

    private void initVideoParams() {
        if (mAudioDelay != 0l)
            mService.setAudioDelay(mAudioDelay);
        else if (mBtReceiver != null && (mAudioManager.isBluetoothA2dpOn() || mAudioManager.isBluetoothScoOn()))
            toggleBtDelay(true);
        mService.setSpuDelay(mSpuDelay);
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void stopPlayback() {
        if (!mPlaybackStarted)
            return;
        mWasPaused = !mService.isPlaying();
        if (mMute)
            mute(false);
        mPlaybackStarted = false;
        mService.setVideoTrackEnabled(false);
        mService.removeCallback(this);
        mHandler.removeCallbacksAndMessages(null);
        final IVLCVout vlcVout = mService.getVLCVout();
        vlcVout.removeCallback(this);
        vlcVout.detachViews();
        if (mSwitchingView && mService != null) {
            Log.d(TAG, "mLocation = \"" + mUri + "\"");
            if (mSwitchToPopup)
                mService.switchToPopup(mService.getCurrentMediaPosition());
            else {
                mService.getCurrentMediaWrapper().addFlags(MediaWrapper.MEDIA_FORCE_AUDIO);
                mService.showWithoutParse(mService.getCurrentMediaPosition());
            }
            return;
        }
        cleanUI();
        if (mService.isSeekable()) {
            mSavedTime = getTime();
            long length = mService.getLength();
            //remove saved position if in the last 5 seconds
            if (length - mSavedTime < 5000)
                mSavedTime = 0;
            else
                mSavedTime -= 2000; // go back 2 seconds, to compensate loading time
        }
        mSavedRate = mService.getRate();
        mRateHasChanged = mSavedRate != 1.0f;
        mService.setRate(1.0f, false);
        mService.stop();
    }
    private void cleanUI() {
        if (mRootView != null)
            mRootView.setKeepScreenOn(false);
        if (mDetector != null) {
            mDetector.setOnDoubleTapListener(null);
            mDetector = null;
        }
        /* Stop listening for changes to media routes. */
        if (mMediaRouter != null)
            mediaRouterAddCallback(false);
        if (mSurfaceFrame != null && AndroidUtil.isHoneycombOrLater() && mOnLayoutChangeListener != null)
            mSurfaceFrame.removeOnLayoutChangeListener(mOnLayoutChangeListener);
        if (AndroidUtil.isICSOrLater())
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(null);
        //mActionBarView.setOnTouchListener(null);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(data == null) return;

        if(data.getData() == null)
            logger.Debug("Subtitle selection dialog was cancelled");

        String subtitlesPath = data.getData().getPath();
        mSubtitleSelectedFiles.add(subtitlesPath);
        mService.addSubtitleTrack(subtitlesPath);
    }
    public static void start(Context context, Uri uri) {
        start(context, uri, null, false, -1);
    }
    public static void start(Context context, Uri uri, boolean fromStart) {
        start(context, uri, null, fromStart, -1);
    }
    public static void start(Context context, Uri uri, String title) {
        start(context, uri, title, false, -1);
    }
    public static void startOpened(Context context, Uri uri, int openedPosition) {
        start(context, uri, null, false, openedPosition);
    }
    private static void start(Context context, Uri uri, String title, boolean fromStart, int openedPosition) {
        Intent intent = getIntent(context, uri, title, fromStart, openedPosition);
        context.startActivity(intent);
    }
    @NonNull
    public static Intent getIntent(Context context, Uri uri, String title, boolean fromStart, int openedPosition) {
        return getIntent(PLAY_FROM_VIDEOGRID, context, uri, title, fromStart, openedPosition);
    }
    @NonNull
    public static Intent getIntent(String action, Context context, Uri uri, String title, boolean fromStart, int openedPosition) {
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.setAction(action);
        intent.putExtra(PLAY_EXTRA_ITEM_LOCATION, uri);
        intent.putExtra(PLAY_EXTRA_ITEM_TITLE, title);
        intent.putExtra(PLAY_EXTRA_FROM_START, fromStart);
        if (openedPosition != -1 || !(context instanceof Activity)) {
            if (openedPosition != -1)
                intent.putExtra(PLAY_EXTRA_OPENED_POSITION, openedPosition);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return intent;
    }
    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (action.equalsIgnoreCase(Intent.ACTION_BATTERY_CHANGED)) {
                if (mBattery == null)
                    return;
                int batteryLevel = intent.getIntExtra("level", 0);
                if (batteryLevel >= 50)
                    mBattery.setTextColor(Color.GREEN);
                else if (batteryLevel >= 30)
                    mBattery.setTextColor(Color.YELLOW);
                else
                    mBattery.setTextColor(Color.RED);
                mBattery.setText(String.format("%d%%", batteryLevel));
            }
            else if (action.equalsIgnoreCase(Constants.SLEEP_INTENT)) {
                exitOK();
            }
        }
    };
    private void exit(int resultCode){
        if (isFinishing())
            return;
        Intent resultIntent = new Intent(ACTION_RESULT);
        if (mUri != null && mService != null) {
            /*if (AndroidUtil.isNougatOrLater())
                resultIntent.putExtra(EXTRA_URI, mUri.toString());
            else
                resultIntent.setData(mUri);*/
            resultIntent.setData(mUri);
            resultIntent.putExtra(EXTRA_POSITION, mService.getTime());
            resultIntent.putExtra(EXTRA_DURATION, mService.getLength());
        }
        setResult(resultCode, resultIntent);
        finish();
    }
    private void exitOK() {
        exit(RESULT_OK);
    }
    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        if (mIsLoading)
            return false;
        showOverlay();
        return true;
    }
    @TargetApi(12) //only active for Android 3.1+
    public boolean dispatchGenericMotionEvent(MotionEvent event){
        if (mIsLoading)
            return  false;
        //Check for a joystick event
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) !=
                InputDevice.SOURCE_JOYSTICK ||
                event.getAction() != MotionEvent.ACTION_MOVE)
            return false;
        InputDevice mInputDevice = event.getDevice();
        float dpadx = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float dpady = event.getAxisValue(MotionEvent.AXIS_HAT_Y);
        if (mInputDevice == null || Math.abs(dpadx) == 1.0f || Math.abs(dpady) == 1.0f)
            return false;
        float x = AndroidDevices.getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_X);
        float y = AndroidDevices.getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_Y);
        float rz = AndroidDevices.getCenteredAxis(event, mInputDevice,
                MotionEvent.AXIS_RZ);
        if (System.currentTimeMillis() - mLastMove > JOYSTICK_INPUT_DELAY){
            if (Math.abs(x) > 0.3){
                if (false) {
                    navigateDvdMenu(x > 0.0f ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT);
                } else
                    seekDelta(x > 0.0f ? 10000 : -10000);
            } else if (Math.abs(y) > 0.3){
                if (false)
                    navigateDvdMenu(x > 0.0f ? KeyEvent.KEYCODE_DPAD_UP : KeyEvent.KEYCODE_DPAD_DOWN);
                else {
                    if (mIsFirstBrightnessGesture)
                        initBrightnessTouch();
                    changeBrightness(-y / 10f);
                }
            } else if (Math.abs(rz) > 0.3){
                mVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                int delta = -(int) ((rz / 7) * mAudioMax);
                int vol = (int) Math.min(Math.max(mVol + delta, 0), mAudioMax);
                setAudioVolume(vol);
            }
            mLastMove = System.currentTimeMillis();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (mLockBackButton) {
            mLockBackButton = false;
            mHandler.sendEmptyMessageDelayed(RESET_BACK_LOCK, 2000);
            Toast.makeText(getApplicationContext(), getString(R.string.back_quit_lock), Toast.LENGTH_SHORT).show();
        }
        /*else if(mPlaylist.getVisibility() == View.VISIBLE) {
            togglePlaylist();
        } */
        else if (mPlaybackSetting != DelayState.OFF){
            endPlaybackSetting();
        } else if (false && mShowing && !mIsLocked) {
            hideOverlay(true);
        } else {
            if (mService == null) {
                logger.Error("onBackPressed - mService is null");
            }
            else {
                mService.setResult(false, false);
            }
            exitOK();
            super.onBackPressed();
        }
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B)
            return super.onKeyDown(keyCode, event);
        if (mPlaybackSetting != DelayState.OFF)
            return false;
        if (mIsLoading) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_S:
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    if (mService == null) {
                        logger.Error("KEYCODE_MEDIA_STOP - mService is null");
                    }
                    else {
                        mService.setResult(false, false);
                    }
                    exitOK();
                    return true;
            }
            return false;
        }
        //Handle playlist d-pad navigation
        /*if (mPlaylist.hasFocus()) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    mPlaylistAdapter.setCurrentIndex(mPlaylistAdapter.getCurrentIndex() - 1);
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    mPlaylistAdapter.setCurrentIndex(mPlaylistAdapter.getCurrentIndex() + 1);
                    break;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_BUTTON_A:
                    mService.playIndex(mPlaylistAdapter.getCurrentIndex());
                    break;
            }
            return true;
        }*/
        if (mShowing || (mFov == 0f && keyCode == KeyEvent.KEYCODE_DPAD_DOWN))
            showOverlayTimeout(OVERLAY_TIMEOUT);
        switch (keyCode) {
            case KeyEvent.KEYCODE_F:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                seekDelta(10000);
                return true;
            case KeyEvent.KEYCODE_R:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                seekDelta(-10000);
                return true;
            case KeyEvent.KEYCODE_BUTTON_R1:
                seekDelta(60000);
                return true;
            case KeyEvent.KEYCODE_BUTTON_L1:
                seekDelta(-60000);
                return true;
            case KeyEvent.KEYCODE_BUTTON_A:
                if (mOverlayProgress.getVisibility() == View.VISIBLE)
                    return false;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_SPACE:
                if (mIsNavMenu)
                    return navigateDvdMenu(keyCode);
                else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) //prevent conflict with remote control
                    return super.onKeyDown(keyCode, event);
                else
                    doPlayPause();
                return true;
            case KeyEvent.KEYCODE_O:
            case KeyEvent.KEYCODE_BUTTON_Y:
            case KeyEvent.KEYCODE_MENU:
                showAdvancedOptions();
                return true;
            case KeyEvent.KEYCODE_V:
            case KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK:
            case KeyEvent.KEYCODE_BUTTON_X:
                onAudioSubClick(mTracks);
                return true;
            case KeyEvent.KEYCODE_N:
                showNavMenu();
                return true;
            case KeyEvent.KEYCODE_A:
                resizeVideo();
                return true;
            case KeyEvent.KEYCODE_M:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                updateMute();
                return true;
            case KeyEvent.KEYCODE_S:
            case KeyEvent.KEYCODE_MEDIA_STOP:
                exitOK();
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (!mShowing) {
                    if (mFov == 0f)
                        seekDelta(-10000);
                    else
                        mService.updateViewpoint(-5f, 0f, 0f, 0f, false);
                    return true;
                }
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (!mShowing) {
                    if (mFov == 0f)
                        seekDelta(10000);
                    else
                        mService.updateViewpoint(5f, 0f, 0f, 0f, false);
                    return true;
                }
            case KeyEvent.KEYCODE_DPAD_UP:
                if (!mShowing) {
                    if (mFov == 0f)
                        showAdvancedOptions();
                    else
                        mService.updateViewpoint(0f, -5f, 0f, 0f, false);
                    return true;
                }
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (!mShowing && mFov != 0f) {
                    mService.updateViewpoint(0f, 5f, 0f, 0f, false);
                    return true;
                }
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (!mShowing) {
                    doPlayPause();
                    return true;
                }
            case KeyEvent.KEYCODE_ENTER:
                if (mIsNavMenu)
                    return navigateDvdMenu(keyCode);
                else
                    return super.onKeyDown(keyCode, event);
            case KeyEvent.KEYCODE_J:
                delayAudio(-50000l);
                return true;
            case KeyEvent.KEYCODE_K:
                delayAudio(50000l);
                return true;
            case KeyEvent.KEYCODE_G:
                delaySubs(-50000l);
                return true;
            case KeyEvent.KEYCODE_H:
                delaySubs(50000l);
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (mMute) {
                    updateMute();
                    return true;
                } else
                    return false;
            case KeyEvent.KEYCODE_CAPTIONS:
                selectSubtitles();
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private boolean navigateDvdMenu(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                mService.navigate(MediaPlayer.Navigate.Up);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                mService.navigate(MediaPlayer.Navigate.Down);
                return true;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                mService.navigate(MediaPlayer.Navigate.Left);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                mService.navigate(MediaPlayer.Navigate.Right);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_BUTTON_X:
            case KeyEvent.KEYCODE_BUTTON_A:
                mService.navigate(MediaPlayer.Navigate.Activate);
                return true;
            default:
                return false;
        }
    }
    @Override
    public void showAudioDelaySetting() {
        mPlaybackSetting = DelayState.AUDIO;
        showDelayControls();
    }
    @Override
    public void showSubsDelaySetting() {
        mPlaybackSetting = DelayState.SUBS;
        showDelayControls();
    }
    @Override
    public void showPlaybackSpeedSetting() {
        mPlaybackSetting = DelayState.SPEED;
        showDelayControls();
    }
    public void showDelayControls(){
        mTouchAction = TOUCH_NONE;
        if (mPresentation != null)
            showOverlayTimeout(OVERLAY_INFINITE);
        mPlaybackSettingMinus.setOnClickListener(this);
        mPlaybackSettingPlus.setOnClickListener(this);
        mPlaybackSettingMinus.setOnTouchListener(new OnRepeatListener(this));
        mPlaybackSettingPlus.setOnTouchListener(new OnRepeatListener(this));
        mPlaybackSettingMinus.setVisibility(View.VISIBLE);
        mPlaybackSettingPlus.setVisibility(View.VISIBLE);
        mPlaybackSettingPlus.requestFocus();
        initPlaybackSettingInfo();
    }
    private void initPlaybackSettingInfo() {
        if (mPresentation == null) {
            mVerticalBar.setVisibility(View.GONE);
            mOverlayInfo.setVisibility(View.VISIBLE);
        } else
            mInfo.setVisibility(View.VISIBLE);
        String text = "";
        if (mPlaybackSetting == DelayState.AUDIO) {
            text += getString(R.string.audio_delay)+"\n";
            text += mService.getAudioDelay() / 1000l;
            text += " ms";
        } else if (mPlaybackSetting == DelayState.SUBS) {
            text += getString(R.string.spu_delay)+"\n";
            text += mService.getSpuDelay() / 1000l;
            text += " ms";
        } else if (mPlaybackSetting == DelayState.SPEED) {
            text += "Playback speed"+"\n";
            text += mService.getRate();
            text += " x";
        } else
            text += "0";
        mInfo.setText(text);
    }
    @Override
    public void endPlaybackSetting() {
        mTouchAction = TOUCH_NONE;
        if (mBtReceiver != null && mPlaybackSetting == DelayState.AUDIO
                && (mAudioManager.isBluetoothA2dpOn() || mAudioManager.isBluetoothScoOn())) {
            String msg = getString(R.string.audio_delay) + "\n"
                    + mService.getAudioDelay() / 1000l
                    + " ms";
            /*Snackbar sb = Snackbar.make(mInfo, msg, Snackbar.LENGTH_LONG);
            sb.setAction(R.string.save_bluetooth_delay, mBtSaveListener);
            sb.show();*/
        }
        mPlaybackSetting = DelayState.OFF;
        mPlaybackSettingMinus.setOnClickListener(null);
        mPlaybackSettingPlus.setOnClickListener(null);
        mPlaybackSettingMinus.setVisibility(View.INVISIBLE);
        mPlaybackSettingPlus.setVisibility(View.INVISIBLE);

        if (mPresentation == null)
            mOverlayInfo.setVisibility(View.INVISIBLE);
        else
            mInfo.setVisibility(View.INVISIBLE);
        mInfo.setText("");
        mPlayPause.requestFocus();
    }

    public void delayAudio(long delta){
        long delay = mService.getAudioDelay()+delta;
        mService.setAudioDelay(delay);
        mInfo.setText(getString(R.string.audio_delay)+"\n"+(delay/1000l)+" ms");
        mAudioDelay = delay;
        if (mPlaybackSetting == DelayState.OFF) {
            mPlaybackSetting = DelayState.AUDIO;
            initPlaybackSettingInfo();
        }
    }
    public void delaySubs(long delta){
        long delay = mService.getSpuDelay()+delta;
        mService.setSpuDelay(delay);
        mInfo.setText(getString(R.string.spu_delay) + "\n" + (delay / 1000l) + " ms");
        mSpuDelay = delay;
        if (mPlaybackSetting == DelayState.OFF) {
            mPlaybackSetting = DelayState.SUBS;
            initPlaybackSettingInfo();
        }
    }
    public void changeSpeed(float delta){
        float rate = Math.round((mService.getRate()+delta)*100f)/100f;
        if (rate < 0.25f || rate > 4f)
            return;
        mService.setRate(rate, false);
        mInfo.setText("Playback speed" + "\n" +rate + " x");
        if (mPlaybackSetting == DelayState.OFF) {
            mPlaybackSetting = DelayState.SPEED;
            initPlaybackSettingInfo();
        }
    }
    /**
     * Lock screen rotation
     */
    private void lockScreen() {
        if(mScreenOrientation != 100) {
            mScreenOrientationLock = getRequestedOrientation();
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
            else
                setRequestedOrientation(getScreenOrientation(100));
        }
        showInfo(R.string.locked, 1000);
        mLock.setImageResource(R.drawable.ic_locked_circle);
        mTime.setEnabled(false);
        mSeekbar.setEnabled(false);
        mLength.setEnabled(false);
        mSize.setEnabled(false);
        if (mPlaylistNext != null)
            mPlaylistNext.setEnabled(false);
        if (mPlaylistPrevious != null)
            mPlaylistPrevious.setEnabled(false);
        hideOverlay(true);
        mLockBackButton = true;
        mIsLocked = true;
    }
    /**
     * Remove screen lock
     */
    private void unlockScreen() {
        if(mScreenOrientation != 100)
            setRequestedOrientation(mScreenOrientationLock);
        showInfo(R.string.unlocked, 1000);
        mLock.setImageResource(R.drawable.ic_lock_circle);
        mTime.setEnabled(true);
        mSeekbar.setEnabled(mService == null || mService.isSeekable());
        mLength.setEnabled(true);
        mSize.setEnabled(true);
        if (mPlaylistNext != null)
            mPlaylistNext.setEnabled(true);
        if (mPlaylistPrevious != null)
            mPlaylistPrevious.setEnabled(true);
        mShowing = false;
        mIsLocked = false;
        showOverlay();
        mLockBackButton = false;
    }
    /**
     * Show text in the info view and vertical progress bar for "duration" milliseconds
     * @param text
     * @param duration
     * @param barNewValue new volume/brightness value (range: 0 - 15)
     */
    private void showInfoWithVerticalBar(String text, int duration, int barNewValue) {
        showInfo(text, duration);
        if (mVerticalBarProgress == null)
            return;
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) mVerticalBarProgress.getLayoutParams();
        layoutParams.weight = barNewValue;
        mVerticalBarProgress.setLayoutParams(layoutParams);
        mVerticalBar.setVisibility(View.VISIBLE);
    }
    /**
     * Show text in the info view for "duration" milliseconds
     * @param text
     * @param duration
     */
    private void showInfo(String text, int duration) {
        if (mPresentation == null)
            mVerticalBar.setVisibility(View.GONE);
        mInfo.setVisibility(View.VISIBLE);
        mInfo.setText(text);
        mHandler.removeMessages(FADE_OUT_INFO);
        mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, duration);
    }

    private void showInfo(int textid, int duration) {
        if (mPresentation == null)
            mVerticalBar.setVisibility(View.GONE);
        mInfo.setVisibility(View.VISIBLE);
        mInfo.setText(textid);
        mHandler.removeMessages(FADE_OUT_INFO);
        mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, duration);
    }
    /**
     * hide the info view with "delay" milliseconds delay
     * @param delay
     */
    private void hideInfo(int delay) {
        mHandler.sendEmptyMessageDelayed(FADE_OUT_INFO, delay);
    }
    /**
     * hide the info view
     */
    private void hideInfo() {
        hideInfo(0);
    }
    private void fadeOutInfo() {
        if (mInfo.getVisibility() == View.VISIBLE)
            mInfo.startAnimation(AnimationUtils.loadAnimation(
                    VideoPlayerActivity.this, android.R.anim.fade_out));
        mInfo.setVisibility(View.INVISIBLE);

        if (mPresentation == null) {
            if (mVerticalBar.getVisibility() == View.VISIBLE) {
                mVerticalBar.startAnimation(AnimationUtils.loadAnimation(
                        VideoPlayerActivity.this, android.R.anim.fade_out));
                mVerticalBar.setVisibility(View.INVISIBLE);
            }
        }
    }

    /* PlaybackService.Callback */
    @Override
    public void update() {
    }
    @Override
    public void updateProgress() {
    }
    @Override
    public void onMediaEvent(Media.Event event) {
        switch (event.type) {
            case Media.Event.ParsedChanged:
                updateNavStatus();
                break;
            case Media.Event.MetaChanged:
                break;
            case Media.Event.SubItemTreeAdded:
                mHasSubItems = true;
                break;
        }
    }
    @Override
    public void onMediaPlayerEvent(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaPlayer.Event.Opening:
                mHasSubItems = false;
                break;
            case MediaPlayer.Event.Playing:
                onPlaying();
                break;
            case MediaPlayer.Event.Paused:
                updateOverlayPausePlay();
                break;
            case MediaPlayer.Event.Stopped:
                stopTimeLimitTimer();
                exitOK();
                break;
            case MediaPlayer.Event.EndReached:
                stopTimeLimitTimer();
                /* Don't end the activity if the media has subitems since the next child will be
                 * loaded by the PlaybackService */
                if (!mHasSubItems)
                    endReached();
                break;
            case MediaPlayer.Event.EncounteredError:
                stopTimeLimitTimer();
                encounteredError();
                break;
            case MediaPlayer.Event.TimeChanged:
                updateSubtitles(mService.getTime());
                break;
            case MediaPlayer.Event.PositionChanged:
                break;
            case MediaPlayer.Event.Vout:
                updateNavStatus();
                if (mMenuIdx == -1)
                    handleVout(event.getVoutCount());
                break;
            case MediaPlayer.Event.ESAdded:
            case MediaPlayer.Event.ESDeleted:
                if (mMenuIdx == -1 && event.getEsChangedType() == Media.Track.Type.Video) {
                    mHandler.removeMessages(CHECK_VIDEO_TRACKS);
                    mHandler.sendEmptyMessageDelayed(CHECK_VIDEO_TRACKS, 1000);
                }
                invalidateESTracks(event.getEsChangedType());
                break;
            case MediaPlayer.Event.SeekableChanged:
                updateSeekable(event.getSeekable());
                break;
            case MediaPlayer.Event.PausableChanged:
                updatePausable(event.getPausable());
                break;
            case MediaPlayer.Event.Buffering:
                if (!mIsPlaying)
                    break;
                if (event.getBuffering() == 100f)
                    stopLoading();
                else if (!mHandler.hasMessages(LOADING_ANIMATION) && !mIsLoading
                        && mTouchAction != TOUCH_SEEK && !mDragging)
                {
                    //mHandler.sendEmptyMessageDelayed(LOADING_ANIMATION, LOADING_ANIMATION_DELAY);
                }
                break;
        }
    }
    /**
     * Handle resize of the surface and the overlay
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper(), new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (mService == null)
                return true;
            switch (msg.what) {
                case FADE_OUT:
                    hideOverlay(false);
                    break;
                case SHOW_PROGRESS:
                    int pos = setOverlayProgress();
                    if (canShowProgress()) {
                        msg = mHandler.obtainMessage(SHOW_PROGRESS);
                        mHandler.sendMessageDelayed(msg, 1000 - (pos % 1000));
                    }
                    break;
                case FADE_OUT_INFO:
                    fadeOutInfo();
                    break;
                case START_PLAYBACK:
                    startPlayback();
                    break;
                case AUDIO_SERVICE_CONNECTION_FAILED:
                    exit(RESULT_CONNECTION_FAILED);
                    break;
                case RESET_BACK_LOCK:
                    mLockBackButton = true;
                    break;
                case CHECK_VIDEO_TRACKS:
                    if (mService.getVideoTracksCount() < 1 && mService.getAudioTracksCount() > 0) {
                        logger.Info("No video track, open in audio mode");
                        switchToAudioMode(true);
                    }
                    break;
                case LOADING_ANIMATION:
                    startLoading();
                    break;
            }
            return true;
        }
    });
    private boolean canShowProgress() {
        return !mDragging && mShowing && mService != null &&  mService.isPlaying();
    }
    private void onPlaying() {
        mIsPlaying = true;
        stopLoading();
        showOverlay();
        setOverlayProgress();
        updateOverlayPausePlay();
        setESTracks();
        updateNavStatus();

        if (apiHelper.getMediaSource().getDefaultSubtitleStreamIndex() == null) {
            apiHelper.setSubtitleStreamIndex(mService.getMediaPlayer(),  mService, -1, true);
        } else {
            apiHelper.setSubtitleStreamIndex(mService.getMediaPlayer(), mService, apiHelper.getMediaSource().getDefaultSubtitleStreamIndex(), true);
        }
    }
    private void endReached() {
        if (mService == null)
            return;
        if (mService.getRepeatType() == PlaybackService.REPEAT_ONE){
            seek(0);
            return;
        }
        if (mService.expand(false) == 0) {
            mHandler.removeMessages(LOADING_ANIMATION);
            //mHandler.sendEmptyMessageDelayed(LOADING_ANIMATION, LOADING_ANIMATION_DELAY);
            Log.d(TAG, "Found a video playlist, expanding it");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    loadMedia();
                }
            });
        } else{
            mService.setResult(true, false);
        }
        //Ignore repeat
        if (mService.getRepeatType() == PlaybackService.REPEAT_ALL && mService.getMediaListSize() == 1)
            exitOK();
    }

    /*@Override
    public void eventHardwareAccelerationError() {
        mHandler.sendEmptyMessage(HW_ERROR);
    }*/

    private void encounteredError() {
        if (isFinishing())
            return;
        //We may not have the permission to access files
        /*if (AndroidUtil.isMarshMallowOrLater() && mUri != null &&
                TextUtils.equals(mUri.getScheme(), "file") &&
                !Permissions.canReadStorage()) {
            Permissions.checkReadStoragePermission(this, true);
            return;
        }*/
        /* Encountered Error, exit player with a message */
        mAlertDialog = new AlertDialog.Builder(VideoPlayerActivity.this)
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        exit(RESULT_PLAYBACK_ERROR);
                    }
                })
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mService.setResult(false, true);
                        exit(RESULT_PLAYBACK_ERROR);
                    }
                })
                .setTitle(R.string.encountered_error_title)
                .setMessage(R.string.encountered_error_message)
                .create();
        mAlertDialog.show();
    }
    private final Runnable mSwitchAudioRunnable = new Runnable() {
        @Override
        public void run() {
            if (mService.hasMedia()) {
                Log.i(TAG, "Video track lost, switching to audio");
                mSwitchingView = true;
            }
            exit(RESULT_VIDEO_TRACK_LOST);
        }
    };
    private void handleVout(int voutCount) {
        mHandler.removeCallbacks(mSwitchAudioRunnable);
        final IVLCVout vlcVout = mService.getVLCVout();
        if (vlcVout.areViewsAttached() && voutCount == 0) {
            mHandler.postDelayed(mSwitchAudioRunnable, 4000);
        } else if (voutCount > 0)
            mFov = mService.getCurrentVideoTrack().projection == Media.VideoTrack.Projection.Rectangular ? 0f : 80f;
    }
    public void switchToPopupMode() {
        if (mService == null)
            return;
        mSwitchingView = true;
        mSwitchToPopup = true;
        exitOK();
    }
    public void switchToAudioMode(boolean showUI) {
        if (mService == null)
            return;
        mSwitchingView = true;
        // Show the MainActivity if it is not in background.
        if (showUI) {
            /*Intent i = new Intent(this, VLCApplication.showTvUi() ? AudioPlayerActivity.class : MainActivity.class);
            startActivity(i);*/
        } else
            mSettings.edit().putBoolean(PreferencesActivity.VIDEO_RESTORE, true).apply();
        exitOK();
    }
    private void changeMediaPlayerLayout(int displayW, int displayH) {
        /* Change the video placement using MediaPlayer API */
        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                mService.setVideoAspectRatio(null);
                mService.setVideoScale(0);
                break;
            case SURFACE_FIT_SCREEN:
            case SURFACE_FILL: {
                Media.VideoTrack vtrack = mService.getCurrentVideoTrack();
                if (vtrack == null)
                    return;
                final boolean videoSwapped = vtrack.orientation == Media.VideoTrack.Orientation.LeftBottom
                        || vtrack.orientation == Media.VideoTrack.Orientation.RightTop;
                if (mCurrentSize == SURFACE_FIT_SCREEN) {
                    int videoW = vtrack.width;
                    int videoH = vtrack.height;
                    if (videoSwapped) {
                        int swap = videoW;
                        videoW = videoH;
                        videoH = swap;
                    }
                    if (vtrack.sarNum != vtrack.sarDen)
                        videoW = videoW * vtrack.sarNum / vtrack.sarDen;
                    float ar = videoW / (float) videoH;
                    float dar = displayW / (float) displayH;
                    float scale;
                    if (dar >= ar)
                        scale = displayW / (float) videoW; /* horizontal */
                    else
                        scale = displayH / (float) videoH; /* vertical */
                    mService.setVideoScale(scale);
                    mService.setVideoAspectRatio(null);
                } else {
                    mService.setVideoScale(0);
                    mService.setVideoAspectRatio(!videoSwapped ? ""+displayW+":"+displayH
                            : ""+displayH+":"+displayW);
                }
                break;
            }
            case SURFACE_16_9:
                mService.setVideoAspectRatio("16:9");
                mService.setVideoScale(0);
                break;
            case SURFACE_4_3:
                mService.setVideoAspectRatio("4:3");
                mService.setVideoScale(0);
                break;
            case SURFACE_ORIGINAL:
                mService.setVideoAspectRatio(null);
                mService.setVideoScale(1);
                break;
        }
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void changeSurfaceLayout() {
        int sw;
        int sh;
        // get screen size
        if (mPresentation == null) {
            sw = getWindow().getDecorView().getWidth();
            sh = getWindow().getDecorView().getHeight();
        } else {
            sw = mPresentation.getWindow().getDecorView().getWidth();
            sh = mPresentation.getWindow().getDecorView().getHeight();
        }
        // sanity check
        if (sw * sh == 0) {
            Log.e(TAG, "Invalid surface size");
            return;
        }
        if (mService != null) {
            final IVLCVout vlcVout = mService.getVLCVout();
            vlcVout.setWindowSize(sw, sh);
        }
        SurfaceView surface;
        SurfaceView subtitlesSurface;
        FrameLayout surfaceFrame;
        if (mPresentation == null) {
            surface = mSurfaceView;
            subtitlesSurface = mSubtitlesSurfaceView;
            surfaceFrame = mSurfaceFrame;
        } else {
            surface = mPresentation.mSurfaceView;
            subtitlesSurface = mPresentation.mSubtitlesSurfaceView;
            surfaceFrame = mPresentation.mSurfaceFrame;
        }
        LayoutParams lp = surface.getLayoutParams();
        if (mVideoWidth * mVideoHeight == 0) {
            /* Case of OpenGL vouts: handles the placement of the video using MediaPlayer API */
            lp.width  = LayoutParams.MATCH_PARENT;
            lp.height = LayoutParams.MATCH_PARENT;
            surface.setLayoutParams(lp);
            lp = surfaceFrame.getLayoutParams();
            lp.width  = LayoutParams.MATCH_PARENT;
            lp.height = LayoutParams.MATCH_PARENT;
            surfaceFrame.setLayoutParams(lp);
            if (mService != null)
                changeMediaPlayerLayout(sw, sh);
            return;
        }
        if (mService != null && lp.width == lp.height && lp.width == LayoutParams.MATCH_PARENT) {
            /* We handle the placement of the video using Android View LayoutParams */
            mService.setVideoAspectRatio(null);
            mService.setVideoScale(0);
        }
        double dw = sw, dh = sh;
        boolean isPortrait;
        if (mPresentation == null) {
            // getWindow().getDecorView() doesn't always take orientation into account, we have to correct the values
            isPortrait = getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        } else {
            isPortrait = false;
        }
        if (sw > sh && isPortrait || sw < sh && !isPortrait) {
            dw = sh;
            dh = sw;
        }
        // compute the aspect ratio
        double ar, vw;
        if (mSarDen == mSarNum) {
            /* No indication about the density, assuming 1:1 */
            vw = mVideoVisibleWidth;
            ar = (double)mVideoVisibleWidth / (double)mVideoVisibleHeight;
        } else {
            /* Use the specified aspect ratio */
            vw = mVideoVisibleWidth * (double)mSarNum / mSarDen;
            ar = vw / mVideoVisibleHeight;
        }
        // compute the display aspect ratio
        double dar = dw / dh;
        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_FIT_SCREEN:
                if (dar >= ar)
                    dh = dw / ar; /* horizontal */
                else
                    dw = dh * ar; /* vertical */
                break;
            case SURFACE_FILL:
                break;
            case SURFACE_16_9:
                ar = 16.0 / 9.0;
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_4_3:
                ar = 4.0 / 3.0;
                if (dar < ar)
                    dh = dw / ar;
                else
                    dw = dh * ar;
                break;
            case SURFACE_ORIGINAL:
                dh = mVideoVisibleHeight;
                dw = vw;
                break;
        }
        // set display size
        lp.width  = (int) Math.ceil(dw * mVideoWidth / mVideoVisibleWidth);
        lp.height = (int) Math.ceil(dh * mVideoHeight / mVideoVisibleHeight);
        surface.setLayoutParams(lp);
        subtitlesSurface.setLayoutParams(lp);

        int subtitleMargin = (lp.height / 2) - 70;
        updateManualSubtitlePosition(subtitleMargin);

        // set frame size (crop if necessary)
        lp = surfaceFrame.getLayoutParams();
        lp.width = (int) Math.floor(dw);
        lp.height = (int) Math.floor(dh);
        surfaceFrame.setLayoutParams(lp);
        surface.invalidate();
        subtitlesSurface.invalidate();
    }
    private void sendMouseEvent(int action, int button, int x, int y) {
        if (mService == null)
            return;
        final IVLCVout vlcVout = mService.getVLCVout();
        vlcVout.sendMouseEvent(action, button, x, y);
    }

    /**
     * show/hide the overlay
     */

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mService == null)
            return false;
        if (mPlaybackSetting != DelayState.OFF) {
            if (event.getAction() == MotionEvent.ACTION_UP)
                endPlaybackSetting();
            return true;
        }
        /*else if (mPlaylist.getVisibility() == View.VISIBLE) {
            togglePlaylist();
            return true;
        }*/
        if (mTouchControls == 0 || mIsLocked) {
            // locked or swipe disabled, only handle show/hide & ignore all actions
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (!mShowing) {
                    showOverlay();
                } else {
                    hideOverlay(true);
                }
            }
            return false;
        }
        if (mFov != 0f && mScaleGestureDetector != null)
            mScaleGestureDetector.onTouchEvent(event);
        if ((mScaleGestureDetector != null && mScaleGestureDetector.isInProgress()) ||
                (mDetector != null && mDetector.onTouchEvent(event)))
        {
            return true;
        }

        if ((mDetector != null && mDetector.onTouchEvent(event)))
            return true;
        if (mSurfaceYDisplayRange == 0)
            mSurfaceYDisplayRange = Math.min(mScreen.widthPixels, mScreen.heightPixels);
        if (mSurfaceXDisplayRange == 0)
            mSurfaceXDisplayRange = Math.max(mScreen.widthPixels, mScreen.heightPixels);
        float x_changed, y_changed;
        if (mTouchX != -1f && mTouchY != -1f) {
            y_changed = event.getRawY() - mTouchY;
            x_changed = event.getRawX() - mTouchX;
        } else {
            x_changed = 0f;
            y_changed = 0f;
        }
        // coef is the gradient's move to determine a neutral zone
        float coef = Math.abs (y_changed / x_changed);
        float xgesturesize = ((x_changed / mScreen.xdpi) * 2.54f);
        float delta_y = Math.max(1f, (Math.abs(mInitTouchY - event.getRawY()) / mScreen.xdpi + 0.5f) * 2f);
        int xTouch = Math.round(event.getRawX());
        int yTouch = Math.round(event.getRawY());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Audio
                mTouchY = mInitTouchY = event.getRawY();
                mVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                mTouchAction = TOUCH_NONE;
                // Seek
                mTouchX = event.getRawX();
                // Mouse events for the core
                sendMouseEvent(MotionEvent.ACTION_DOWN, 0, xTouch, yTouch);
                break;
            case MotionEvent.ACTION_MOVE:
                // Mouse events for the core
                sendMouseEvent(MotionEvent.ACTION_MOVE, 0, xTouch, yTouch);
                if (mFov == 0f) {
                    // No volume/brightness action if coef < 2 or a secondary display is connected
                    //TODO : Volume action when a secondary display is connected
                    if (mTouchAction != TOUCH_SEEK && coef > 2 && mPresentation == null) {
                        if (Math.abs(y_changed/mSurfaceYDisplayRange) < 0.05)
                            return false;
                        mTouchY = event.getRawY();
                        mTouchX = event.getRawX();
                        // Volume (Up or Down - Right side)
                        if (mTouchControls == 1 || (mTouchControls == 3 && (int)mTouchX > (4 * mSurfaceXDisplayRange / 7))){
                            doVolumeTouch(y_changed);
                            hideOverlay(true);
                        }
                        // Brightness (Up or Down - Left side)
                        if (mTouchControls == 2 || (mTouchControls == 3 && (int)mTouchX < (3 * mSurfaceXDisplayRange / 7))){
                            doBrightnessTouch(y_changed);
                            hideOverlay(true);
                        }
                    } else {
                        // Seek (Right or Left move)
                        doSeekTouch(Math.round(delta_y), xgesturesize, false);
                    }
                } else {
                    mTouchY = event.getRawY();
                    mTouchX = event.getRawX();
                    mTouchAction = TOUCH_MOVE;
                    float yaw = mFov * -x_changed/(float)mSurfaceXDisplayRange;
                    float pitch = mFov * -y_changed/(float)mSurfaceXDisplayRange;
                    mService.updateViewpoint(yaw, pitch, 0, 0, false);
                }
                break;
            case MotionEvent.ACTION_UP:
                // Mouse events for the core
                sendMouseEvent(MotionEvent.ACTION_UP, 0, xTouch, yTouch);
                // Seek
                if (mTouchAction == TOUCH_SEEK)
                    doSeekTouch(Math.round(delta_y), xgesturesize, true);
                mTouchX = -1f;
                mTouchY = -1f;
                break;
        }
        return mTouchAction != TOUCH_NONE;
    }
    private void doSeekTouch(int coef, float gesturesize, boolean seek) {
        if (coef == 0)
            coef = 1;
        // No seek action if coef > 0.5 and gesturesize < 1cm
        if (Math.abs(gesturesize) < 1 || !mService.isSeekable())
            return;
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_SEEK)
            return;
        mTouchAction = TOUCH_SEEK;
        long length = mService.getLength();
        long time = getTime();
        // Size of the jump, 10 minutes max (600000), with a bi-cubic progression, for a 8cm gesture
        int jump = (int) ((Math.signum(gesturesize) * ((600000 * Math.pow((gesturesize / 8), 4)) + 3000)) / coef);
        // Adjust the jump
        if ((jump > 0) && ((time + jump) > length))
            jump = (int) (length - time);
        if ((jump < 0) && ((time + jump) < 0))
            jump = (int) -time;
        //Jump !
        if (seek && length > 0)
            seek(time + jump, length);
        if (length > 0)
            //Show the jump's size
            showInfo(String.format("%s%s (%s)%s",
                    jump >= 0 ? "+" : "",
                    Strings.millisToString(jump),
                    Strings.millisToString(time + jump),
                    coef > 1 ? String.format(" x%.1g", 1.0/coef) : ""), 50);
        else
            showInfo(R.string.unseekable_stream, 1000);
    }
    private void doVolumeTouch(float y_changed) {
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_VOLUME)
            return;
        float delta = - ((y_changed / mSurfaceYDisplayRange) * mAudioMax);
        mVol += delta;
        int vol = (int) Math.min(Math.max(mVol, 0), mAudioMax);
        if (delta != 0f) {
            setAudioVolume(vol);
        }
    }
    private void setAudioVolume(int vol) {
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
        /* Since android 4.3, the safe volume warning dialog is displayed only with the FLAG_SHOW_UI flag.
         * We don't want to always show the default UI volume, so show it only when volume is not set. */
        int newVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (vol != newVol)
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_SHOW_UI);
        mTouchAction = TOUCH_VOLUME;
        vol = vol * 100 / mAudioMax;
        showInfoWithVerticalBar(getString(R.string.volume) + "\n" + Integer.toString(vol) + '%', 1000, vol);
    }
    private void mute(boolean mute) {
        mMute = mute;
        if (mMute)
            mVolSave = mService.getVolume();
        mService.setVolume(mMute ? 0 : mVolSave);
    }
    private void updateMute () {
        mute(!mMute);
        showInfo(mMute ? R.string.sound_off : R.string.sound_on, 1000);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.player_overlay_play:
                doPlayPause();
                break;
            /*case R.id.playlist_toggle:
                togglePlaylist();
                break;
            case R.id.playlist_next:
                mService.next();
                break;
            case R.id.playlist_previous:
                mService.previous(false);
                break;
            case R.id.player_overlay_forward:
                seekDelta(10000);
                break;
            case R.id.player_overlay_rewind:
                seekDelta(-10000);
                break;*/
            case R.id.lock_overlay_button:
                if (mIsLocked)
                    unlockScreen();
                else
                    lockScreen();
                break;
            case R.id.player_overlay_size:
                resizeVideo();
                break;
            /*case R.id.player_overlay_navmenu:
                showNavMenu();
                break;*/
            case R.id.player_overlay_length:
            case R.id.player_overlay_time:
                mDisplayRemainingTime = !mDisplayRemainingTime;
                showOverlay();
                mSettings.edit().putBoolean(KEY_REMAINING_TIME_DISPLAY, mDisplayRemainingTime).apply();
                break;
            case R.id.player_delay_minus:
                if (mPlaybackSetting == DelayState.AUDIO)
                    delayAudio(-50000);
                else if (mPlaybackSetting == DelayState.SUBS)
                    delaySubs(-50000);
                else if (mPlaybackSetting == DelayState.SPEED)
                    changeSpeed(-0.05f);
                break;
            case R.id.player_delay_plus:
                if (mPlaybackSetting == DelayState.AUDIO)
                    delayAudio(50000);
                else if (mPlaybackSetting == DelayState.SUBS)
                    delaySubs(50000);
                else if (mPlaybackSetting == DelayState.SPEED)
                    changeSpeed(0.05f);
                break;
            /*case R.id.player_overlay_adv_function:
                showAdvancedOptions();
                break;*/
            case R.id.player_overlay_tracks:
                onAudioSubClick(v);
        }
    }
    public boolean onLongClick(View v) {
        switch (v.getId()){
            case R.id.player_overlay_play:
                if (mService == null)
                    return false;
                if (mService.getRepeatType() == PlaybackService.REPEAT_ONE) {
                    //showInfo(getString(R.string.repeat), 1000);
                    mService.setRepeatType(PlaybackService.REPEAT_NONE);
                } else {
                    mService.setRepeatType(PlaybackService.REPEAT_ONE);
                    //showInfo(getString(R.string.repeat_single), 1000);
                }
                return true;
            default:
                return false;
        }
    }
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float fov_changed = mFov * (detector.getPreviousSpan()-detector.getCurrentSpan())/(float)mSurfaceXDisplayRange;
        if (mService.updateViewpoint(0, 0, 0, fov_changed, false)) {
            mFov += fov_changed;
            return true;
        }
        return false;
    }
    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return mSurfaceXDisplayRange!= 0 && mFov != 0f;
    }
    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {}
    private void initBrightnessTouch() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        float brightnesstemp = lp.screenBrightness != -1f ? lp.screenBrightness : 0.6f;
        // Initialize the layoutParams screen brightness
        try {
            if (Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                /*if (!Permissions.canWriteSettings(this)) {
                    Permissions.checkWriteSettingsPermission(this, Permissions.PERMISSION_SYSTEM_BRIGHTNESS);
                    return;
                }
                Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);*/
                mRestoreAutoBrightness = android.provider.Settings.System.getInt(getContentResolver(),
                        android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
            } else if (brightnesstemp == 0.6f) {
                brightnesstemp = android.provider.Settings.System.getInt(getContentResolver(),
                        android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
            }
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        lp.screenBrightness = brightnesstemp;
        getWindow().setAttributes(lp);
        mIsFirstBrightnessGesture = false;
    }
    private void doBrightnessTouch(float y_changed) {
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_BRIGHTNESS)
            return;
        if (mIsFirstBrightnessGesture) initBrightnessTouch();
        mTouchAction = TOUCH_BRIGHTNESS;
        // Set delta : 2f is arbitrary for now, it possibly will change in the future
        float delta = - y_changed / mSurfaceYDisplayRange;
        changeBrightness(delta);
    }
    private void changeBrightness(float delta) {
        // Estimate and adjust Brightness
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        float brightness =  Math.min(Math.max(lp.screenBrightness + delta, 0.01f), 1f);
        setWindowBrightness(brightness);
        brightness = Math.round(brightness * 100);
        showInfoWithVerticalBar(getString(R.string.brightness) + "\n" + (int) brightness + '%', 1000, (int) brightness);
    }
    private void setWindowBrightness(float brightness) {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness =  brightness;
        // Set Brightness
        getWindow().setAttributes(lp);
    }

    /**
     * handle changes of the seekbar (slicer)
     */
    private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mDragging = true;
            showOverlayTimeout(OVERLAY_INFINITE);
        }
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mDragging = false;
            showOverlay(true);
        }
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!isFinishing() && fromUser && mService.isSeekable()) {
                seek(progress);
                setOverlayProgress();
                mTime.setText(Strings.millisToString(progress));
                showInfo(Strings.millisToString(progress), 1000);
            }
        }
    };

    public void onAudioSubClick(View anchor){

        setESTrackLists();

        final AppCompatActivity context = this;
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.audiosub_tracks, popupMenu.getMenu());
        popupMenu.getMenu().findItem(R.id.video_menu_audio_track).setEnabled(mAudioTracksList != null && mAudioTracksList.size() > 0);
        popupMenu.getMenu().findItem(R.id.video_menu_subtitles).setEnabled(mSubtitleTracksList != null && mSubtitleTracksList.size() > 1);
        popupMenu.getMenu().findItem(R.id.video_menu_quality).setEnabled(apiHelper.getPlaybackProgressInfo().getPlayMethod() != PlayMethod.DirectPlay);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.video_menu_audio_track) {
                    selectAudioTrack();
                    return true;
                } else if (item.getItemId() == R.id.video_menu_subtitles) {
                    selectSubtitles();
                    return true;
                }else if (item.getItemId() == R.id.video_menu_quality) {
                    selectQuality();
                    return true;
                }
                return false;
            }
        });
        popupMenu.show();
    }

    private interface TrackSelectedListener {
        public boolean onTrackSelected(String trackID);
    }
    private void selectTrack(final ArrayList<NameValuePair> trackMap, String currentTrack, boolean checkCurrentTrackPrefix, int titleId,
                             final TrackSelectedListener listener) {
        if (listener == null)
            throw new IllegalArgumentException("listener must not be null");
        if (trackMap == null)
            return;
        final String[] nameList = new String[trackMap.size()];
        final String[] idList = new String[trackMap.size()];
        int i = 0;
        int listPosition = 0;
        for(NameValuePair entry : trackMap) {
            idList[i] = entry.getValue();
            nameList[i] = entry.getName();
            // map the track position to the list position
            if(StringHelper.EqualsIgnoreCase(entry.getValue(), currentTrack))
                listPosition = i;
            else if (checkCurrentTrackPrefix && entry.getValue().indexOf(currentTrack+"-") == 0){
                listPosition = i;
            }
            i++;
        }

        mAlertDialog = new AlertDialog.Builder(VideoPlayerActivity.this)
                .setTitle(titleId)
                .setSingleChoiceItems(nameList, listPosition, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int listPosition) {
                        String trackID = null;
                        // Reverse map search...
                        for (NameValuePair entry : trackMap) {
                            if (StringHelper.EqualsIgnoreCase(idList[listPosition], entry.getValue())) {
                                trackID = entry.getValue();
                                break;
                            }
                        }
                        listener.onTrackSelected(trackID);
                        dialog.dismiss();
                    }
                })
                .create();
        mAlertDialog.setCanceledOnTouchOutside(true);
        mAlertDialog.setOwnerActivity(VideoPlayerActivity.this);
        mAlertDialog.show();
    }

    private void selectAudioTrack() {
        setESTrackLists();
        int currentIndex = apiHelper.getPlaybackProgressInfo().getAudioStreamIndex() == null ? -1 : apiHelper.getPlaybackProgressInfo().getAudioStreamIndex();

        selectTrack(mAudioTracksList, String.valueOf(currentIndex), false, R.string.track_audio,
                new TrackSelectedListener() {
                    @Override
                    public boolean onTrackSelected(String value) {
                        if (value == null) return false;
                        int trackID = Integer.parseInt(value);
                        if (trackID < 0)
                            return false;
                        apiHelper.setAudioStreamIndex(mService.getMediaPlayer(),  mService, trackID);
                        return true;
                    }
                });
    }

    private void selectSubtitles() {

        setESTrackLists();
        int currentIndex = apiHelper.getPlaybackProgressInfo().getSubtitleStreamIndex() == null ? -1 : apiHelper.getPlaybackProgressInfo().getSubtitleStreamIndex();

        selectTrack(mSubtitleTracksList, String.valueOf(currentIndex), false, R.string.track_text,
                new TrackSelectedListener() {
                    @Override
                    public boolean onTrackSelected(String value) {
                        if (value == null) return  false;
                        int trackID = Integer.parseInt(value);
                        if (trackID < -1)
                            return false;
                        apiHelper.setSubtitleStreamIndex(mService.getMediaPlayer(),  mService, trackID, false);
                        return true;
                    }
                });
    }

    private void selectQuality() {
        setESTrackLists();

        int maxBitrate= apiHelper.getMaxBitrate();

        String selectedValue = "";
        for (NameValuePair pair : mQualityList) {
            String[] parts = pair.getValue().split("-");
            if (maxBitrate >= Integer.parseInt(parts[0])) {
                selectedValue = parts[0];
                break;
            }
        }

        selectTrack(mQualityList, selectedValue, true, R.string.quality_text,
                new TrackSelectedListener() {
                    @Override
                    public boolean onTrackSelected(String value) {
                        if (value == null) return  false;
                        String[] parts = value.split("-");
                        apiHelper.setQuality(mService, Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                        return true;
                    }
                });
    }

    public void restartVideo() {
        changeLocation(mUri.toString(), apiHelper.getMediaSource(), apiHelper.getPlaybackProgressInfo().getPlayMethod(), 0);
    }

    public void changeLocation(String location, MediaSourceInfo mediaSourceInfo, PlayMethod playMethod, long startPositionTicks) {

        location = normalizeLocation(location, mediaSourceInfo, playMethod, startPositionTicks);

        int playlistIndex = savedIndexPosition;
        //long currentTime = mService.getTime();

        Uri newUri = createUri(location);
        logger.Debug("changeLocation Parsed uri: %s", newUri.toString());

        MediaWrapper wrapper = mService.setMedia(newUri, 0);

        if (mDisabledHardwareAcceleration){
            wrapper.addFlags(MediaWrapper.MEDIA_NO_HWACCEL);
        }

        //resumePositionMs = currentTime;

        mAudioTracksList = null;
        mSubtitleTracksList = null;
        mQualityList = null;

        mService.restartIndex(playlistIndex, 0);
        mUri = newUri;
        mService.addCallback(this);

        long startPositionMs = startPositionTicks / 10000;
        if (startPositionMs >= 0) {
            if (!mService.getEnableServerSeek()) {
                seek(startPositionMs);
            }
        }
    }

    private String normalizeLocation(String location, MediaSourceInfo mediaSourceInfo, PlayMethod playMethod, long startPositionTicks) {

        logger.Info("normalizeLocation location %s, playMethod %s, startPositionTicks %s", location, playMethod.toString(), String.valueOf(startPositionTicks));
        if (playMethod == PlayMethod.Transcode) {

            mService.setEnableServerSeek(true);
            mService.setTranscodingOffsetPositionTicks(startPositionTicks);

            return location;
        }

        mService.setEnableServerSeek(false);
        mService.setTranscodingOffsetPositionTicks(0);
        return location;
    }

    /**
     *
     */
    private final OnClickListener mPlayPauseListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            doPlayPause();
        }
    };

    private final View.OnLongClickListener mPlayPauseLongListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            if (mService == null)
                return false;
            /*if (mService.getRepeatType() == PlaybackService.RepeatType.Once) {
                showInfo(getString(R.string.repeat));
                mService.setRepeatType(PlaybackService.RepeatType.None);
            } else {
                mService.setRepeatType(PlaybackService.RepeatType.Once);
                showInfo(getString(R.string.repeat_single));
            }*/
            return true;
        }
    };
    private void showNavMenu() {
        /*if (mMenuIdx >= 0)
            mService.setTitleIdx(mMenuIdx);*/
    }
    private void updateSeekable(boolean seekable) {
        /*if (mRewind != null) {
            mRewind.setEnabled(seekable);
            mRewind.setImageResource(seekable
                    ? R.drawable.ic_rewind_circle
                    : R.drawable.ic_rewind_circle_disable_o);
        }
        if (mForward != null){
            mForward.setEnabled(seekable);
            mForward.setImageResource(seekable
                    ? R.drawable.ic_forward_circle
                    : R.drawable.ic_forward_circle_disable_o);
        }
        if (!mIsLocked)
            mSeekbar.setEnabled(seekable);*/
    }

    private void updatePausable(boolean pausable) {
        mPlayPause.setEnabled(pausable);
        if (!pausable)
        {
            //mPlayPause.setImageResource(R.drawable.ic_play_circle_disable_o);
        }
        else {
            mDetector = new GestureDetectorCompat(this, mGestureListener);
            mDetector.setOnDoubleTapListener(mGestureListener);
        }
        mScaleGestureDetector = new ScaleGestureDetector(this, this);
    }

    private void doPlayPause() {
        if (!mService.isPausable())
            return;
        if (mService.isPlaying()) {
            pause();
            showOverlayTimeout(OVERLAY_INFINITE);
        } else {
            play();
            showOverlayTimeout(OVERLAY_TIMEOUT);
        }
        mPlayPause.requestFocus();
    }

    private long getTime() {
        long time = mService.getTime();

        if (mForcedTime != -1 && mLastTime != -1) {
            /* XXX: After a seek, mService.getTime can return the position before or after
             * the seek position. Therefore we return mForcedTime in order to avoid the seekBar
             * to move between seek position and the actual position.
             * We have to wait for a valid position (that is after the seek position).
             * to re-init mLastTime and mForcedTime to -1 and return the actual position.
             */
            if (mLastTime > mForcedTime) {
                if (time <= mLastTime && time > mForcedTime || time > mLastTime)
                    mLastTime = mForcedTime = -1;
            } else {
                if (time > mForcedTime)
                    mLastTime = mForcedTime = -1;
            }
        }
        return mForcedTime == -1 ? time : mForcedTime;
    }

    private void seek(long position) {
        seek(position, mService.getLength());
    }

    private long lastServerSeekTime = 0;
    private void seek(long position, float length) {

        mForcedTime = position;
        mLastTime = mService.getTime();

        if (mService.getEnableServerSeek()) {

            long now = System.currentTimeMillis();

            if ((now - lastServerSeekTime) > 50) {
                apiHelper.seekTranscode(position * 10000);
                lastServerSeekTime = now;
                //mForcedTime = -1;
            }
            return;
        }
        if (length == 0f)
            mService.setTime(position);
        else
            mService.setPosition(position / length);
    }
    private void seekDelta(int delta) {
        // unseekable stream
        if(mService.getLength() <= 0 || !mService.isSeekable()) return;
        long position = getTime() + delta;
        if (position < 0) position = 0;
        seek(position);
        showInfo(Strings.millisToString(mService.getTime())+"/"+Strings.millisToString(mService.getLength()), 1000);
    }

    /**
     *
     */
    private final OnClickListener mLockListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (mIsLocked) {
                mIsLocked = false;
                unlockScreen();
            } else {
                mIsLocked = true;
                lockScreen();
            }
        }
    };

    /**
     *
     */
    private final OnClickListener mSizeListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            resizeVideo();
        }
    };
    private void resizeVideo() {
        if (mCurrentSize < SURFACE_ORIGINAL) {
            mCurrentSize++;
        } else {
            mCurrentSize = 0;
        }
        changeSurfaceLayout();
        switch (mCurrentSize) {
            case SURFACE_BEST_FIT:
                showInfo(R.string.surface_best_fit, 1000);
                break;
            case SURFACE_FIT_SCREEN:
                showInfo("Fit to screen", 1000);
                break;
            case SURFACE_FILL:
                showInfo(R.string.surface_fill, 1000);
                break;
            case SURFACE_16_9:
                showInfo("16:9", 1000);
                break;
            case SURFACE_4_3:
                showInfo("4:3", 1000);
                break;
            case SURFACE_ORIGINAL:
                showInfo(R.string.surface_original, 1000);
                break;
        }
        /*SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt(PreferencesActivity.VIDEO_RATIO, mCurrentSize);
        editor.apply();*/
        showOverlay();
    }
    /**
     * show overlay
     * @param forceCheck: adjust the timeout in function of playing state
     */
    private void showOverlay(boolean forceCheck) {
        if (forceCheck)
            mOverlayTimeout = 0;
        showOverlayTimeout(0);
    }
    /**
     * show overlay with the previous timeout value
     */
    private void showOverlay() {
        showOverlay(false);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setActionBarVisibility(boolean show) {
        if (show)
            mActionBar.show();
        else
            mActionBar.hide();
    }
    /**
     * show overlay
     */
    private void showOverlayTimeout(int timeout) {
        if (mService == null)
            return;
        if (timeout != 0)
            mOverlayTimeout = timeout;
        if (mOverlayTimeout == 0)
            mOverlayTimeout = mService.isPlaying() ? OVERLAY_TIMEOUT : OVERLAY_INFINITE;
        if (mIsNavMenu){
            mShowing = true;
            return;
        }
        mHandler.sendEmptyMessage(SHOW_PROGRESS);
        if (!mShowing) {
            mShowing = true;
            if (!mIsLocked) {
                setActionBarVisibility(true);
                mPlayPause.setVisibility(View.VISIBLE);
                if (mTracks != null)
                    mTracks.setVisibility(View.VISIBLE);
                mSize.setVisibility(View.VISIBLE);
                dimStatusBar(false);
            }
            mOverlayProgress.setVisibility(View.VISIBLE);
            if (mPresentation != null) mOverlayBackground.setVisibility(View.VISIBLE);
        }
        mHandler.removeMessages(FADE_OUT);
        if (mOverlayTimeout != OVERLAY_INFINITE)
            mHandler.sendMessageDelayed(mHandler.obtainMessage(FADE_OUT), mOverlayTimeout);
        updateOverlayPausePlay();
    }

    /**
     * hider overlay
     */
    private void hideOverlay(boolean fromUser) {
        if (mShowing) {
            mHandler.removeMessages(FADE_OUT);
            mHandler.removeMessages(SHOW_PROGRESS);
            logger.Info("remove View!");
            if (mOverlayTips != null) mOverlayTips.setVisibility(View.INVISIBLE);
            if (!fromUser && !mIsLocked) {
                mOverlayProgress.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                mPlayPause.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                if (mTracks != null)
                    mTracks.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
            } else
                mSize.setVisibility(View.INVISIBLE);
            if (mPresentation != null) {
                mOverlayBackground.startAnimation(AnimationUtils.loadAnimation(this, android.R.anim.fade_out));
                mOverlayBackground.setVisibility(View.INVISIBLE);
            }
            setActionBarVisibility(false);
            mOverlayProgress.setVisibility(View.INVISIBLE);
            mPlayPause.setVisibility(View.INVISIBLE);
            if (mTracks != null)
                mTracks.setVisibility(View.INVISIBLE);
            mShowing = false;
            dimStatusBar(true);
        } else if (!fromUser) {
            /*
             * Try to hide the Nav Bar again.
             * It seems that you can't hide the Nav Bar if you previously
             * showed it in the last 1-2 seconds.
             */
            dimStatusBar(true);
        }
    }
    /**
     * Dim the status bar and/or navigation icons when needed on Android 3.x.
     * Hide it on Android 4.0 and later
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void dimStatusBar(boolean dim) {
        if (dim || mIsLocked)
            mActionBar.hide();
        else
            mActionBar.show();
        if (!AndroidUtil.isHoneycombOrLater() || mIsNavMenu)
            return;
        int visibility = 0;
        int navbar = 0;
        if (AndroidUtil.isJellyBeanOrLater()) {
            visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            navbar = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }
        if (dim || mIsLocked) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (AndroidUtil.isICSOrLater())
                navbar |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
            else
                visibility |= View.STATUS_BAR_HIDDEN;
            if (!AndroidDevices.hasCombBar(getApplicationContext())) {
                navbar |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                if (AndroidUtil.isKitKatOrLater())
                    visibility |= View.SYSTEM_UI_FLAG_IMMERSIVE;
                if (AndroidUtil.isJellyBeanOrLater())
                    visibility |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            }
        } else {
            mActionBar.show();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            if (AndroidUtil.isICSOrLater())
                visibility |= View.SYSTEM_UI_FLAG_VISIBLE;
            else
                visibility |= View.STATUS_BAR_VISIBLE;
        }
        if (AndroidDevices.hasNavBar())
            visibility |= navbar;
        getWindow().getDecorView().setSystemUiVisibility(visibility);
    }
    private void showTitle() {
        if (!AndroidUtil.isHoneycombOrLater() || mIsNavMenu)
            return;
        int visibility = 0;
        int navbar = 0;
        mActionBar.show();
        if (AndroidUtil.isJellyBeanOrLater()) {
            visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            navbar = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }
        if (AndroidUtil.isICSOrLater())
            navbar |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        if (AndroidDevices.hasNavBar())
            visibility |= navbar;
        getWindow().getDecorView().setSystemUiVisibility(visibility);
    }
    private void updateOverlayPausePlay() {
        if (mService == null)
            return;
        if (mService.isPausable())
            mPlayPause.setImageResource(mService.isPlaying() ? R.drawable.ic_pause_circle
                    : R.drawable.ic_play_circle);
    }
    /**
     * update the overlay
     */
    private int setOverlayProgress() {
        if (mService == null) {
            return 0;
        }
        int time = (int) getTime();
        int length = (int) mService.getLength();
        // Update all view elements
        mSeekbar.setMax(length);
        mSeekbar.setProgress(time);
        if (mSysTime != null)
            mSysTime.setText(DateFormat.getTimeFormat(this).format(new Date(System.currentTimeMillis())));
        if (time >= 0) mTime.setText(Strings.millisToString(time));
        if (length >= 0) mLength.setText(mDisplayRemainingTime && length > 0
                ? "-" + '\u00A0' + Strings.millisToString(length - time)
                : Strings.millisToString(length));
        return time;
    }

    private void invalidateESTracks(int type) {
        switch (type) {
            case Media.Track.Type.Audio:
                mAudioTracksList = null;
                break;
            case Media.Track.Type.Text:
                mSubtitleTracksList = null;
                break;
        }
    }

    private void setESTracks() {
        if (mLastAudioTrack >= -1) {
            mService.setAudioTrack(mLastAudioTrack);
            mLastAudioTrack = -2;
        }
        if (mLastSpuTrack >= -1) {
            mService.setSpuTrack(mLastSpuTrack);
            mLastSpuTrack = -2;
        }
    }

    private void setESTrackLists() {
        if (mAudioTracksList == null)
        {
            ArrayList<NameValuePair> audioTracks = new ArrayList<NameValuePair>();

            for (MediaStream stream : apiHelper.getMediaSource().getMediaStreams()){
                if (stream.getType() == MediaStreamType.Audio){

                    audioTracks.add(new NameValuePair(stream.getDisplayTitle(), String.valueOf(stream.getIndex())));
                }
            }
            mAudioTracksList = audioTracks;
        }

        if (mSubtitleTracksList == null) {
            ArrayList<NameValuePair> subs = new ArrayList<NameValuePair>();
            subs.add(new NameValuePair("None", "-1"));
            for (MediaStream stream : apiHelper.getMediaSource().getMediaStreams()){
                if (stream.getType() == MediaStreamType.Subtitle){

                    subs.add(new NameValuePair(stream.getDisplayTitle(), String.valueOf(stream.getIndex())));
                }
            }
            mSubtitleTracksList = subs;
        }

        if (mQualityList == null)
        {
            mQualityList = getQualityOptions();
        }
    }

    private ArrayList<NameValuePair> GetNameValuePairs(Map<Integer,String> tracks) {

        ArrayList<NameValuePair> list = new ArrayList<NameValuePair>();

        for (Map.Entry<Integer,String> entry : tracks.entrySet()) {

            list.add(new NameValuePair(entry.getValue(), String.valueOf(entry.getKey())));
        }

        return list;
    }

    private ArrayList<NameValuePair> getQualityOptions() {

        NameValuePair[] result = jsonSerializer.DeserializeFromString(videoQualityOptionsJson, NameValuePair[].class);

        ArrayList<NameValuePair> list = new ArrayList<NameValuePair>();

        for (NameValuePair i : result){
            list.add(i);
        }

        return list;
    }
    /**
     *
     */
    private void play() {
        mService.play();
        if (mRootView != null)
            mRootView.setKeepScreenOn(true);
    }
    /**
     *
     */
    private void pause() {
        mService.pause();
        if (mRootView != null)
            mRootView.setKeepScreenOn(false);
    }
    /*
     * Additionnal method to prevent alert dialog to pop up
     */
    @SuppressWarnings({ "unchecked" })
    private void loadMedia(boolean fromStart) {
        mAskResume = false;
        getIntent().putExtra(PLAY_EXTRA_FROM_START, fromStart);
        loadMedia();
    }

    private Uri createUri(String location) {

        return Uri.parse(location);
    }

    /**
     * External extras:
     * - position (long) - position of the video to start with (in ms)
     * - subtitles_location (String) - location of a subtitles file to load
     * - from_start (boolean) - Whether playback should start from start or from resume point
     * - title (String) - video title, will be guessed from file if not set.
     */
    @TargetApi(12)
    @SuppressWarnings({ "unchecked" })
    private void loadMedia() {
        if (mService == null)
            return;
        mUri = null;
        mIsPlaying = false;
        String title = getResources().getString(R.string.title);
        boolean fromStart = false;
        String itemTitle = null;
        int positionInPlaylist = -1;
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        long savedTime = extras != null ? extras.getLong(PLAY_EXTRA_START_TIME) : 0L; // position passed in by intent (ms)
        if (extras != null && savedTime == 0L)
            savedTime = extras.getInt(PLAY_EXTRA_START_TIME);
        /*
         * If the activity has been paused by pressing the power button, then
         * pressing it again will show the lock screen.
         * But onResume will also be called, even if vlc-android is still in
         * the background.
         * To workaround this, pause playback if the lockscreen is displayed.
         */
        final KeyguardManager km = (KeyguardManager) getApplicationContext().getSystemService(KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode())
            mWasPaused = true;
        if (mWasPaused)
            Log.d(TAG, "Video was previously paused, resuming in paused mode");
        if (intent.getData() != null)
            mUri = intent.getData();
        if (extras != null) {
            if (intent.hasExtra(PLAY_EXTRA_ITEM_LOCATION))
                mUri = extras.getParcelable(PLAY_EXTRA_ITEM_LOCATION);
            fromStart = extras.getBoolean(PLAY_EXTRA_FROM_START, true);
            mAskResume &= !fromStart;
            positionInPlaylist = extras.getInt(PLAY_EXTRA_OPENED_POSITION, -1);
        }
        if (intent.hasExtra(PLAY_EXTRA_SUBTITLES_LOCATION))
            mSubtitleSelectedFiles.add(extras.getString(PLAY_EXTRA_SUBTITLES_LOCATION));
        if (intent.hasExtra(PLAY_EXTRA_ITEM_TITLE))
            itemTitle = extras.getString(PLAY_EXTRA_ITEM_TITLE);
        MediaWrapper openedMedia = null;
        if (positionInPlaylist != -1 && mService.hasMedia()) {
            // Provided externally from AudioService
            Log.d(TAG, "Continuing playback from PlaybackService at index " + positionInPlaylist);
            openedMedia = mService.getMedias().get(positionInPlaylist);
            if (openedMedia == null) {
                encounteredError();
                return;
            }
            mUri = openedMedia.getUri();
            itemTitle = openedMedia.getTitle();
            updateSeekable(mService.isSeekable());
            updatePausable(mService.isPausable());
        }

        String mediaSourceJson = getIntent().getExtras().getString("mediaSourceJson");
        MediaSourceInfo mediaSourceInfo = (MediaSourceInfo)jsonSerializer.DeserializeFromString(mediaSourceJson, MediaSourceInfo.class);

        String playbackStartInfoJson = getIntent().getExtras().getString("playbackStartInfoJson");
        PlaybackProgressInfo playbackStartInfo = (PlaybackProgressInfo)jsonSerializer.DeserializeFromString(playbackStartInfoJson, PlaybackProgressInfo.class);

        long intentPosition = 0; // position passed in by intent (ms)
        long timeLimitMs = 0;

        intentPosition = getIntent().getExtras().getLong("position", 0);
        timeLimitMs = getIntent().getExtras().getLong("timeLimitMs", 0);

        String location = getIntent().getExtras().getString(PLAY_EXTRA_ITEM_LOCATION);
        location = normalizeLocation(location, mediaSourceInfo, playbackStartInfo.getPlayMethod(), intentPosition * 10000);

        if (playbackStartInfo.getPlayMethod() != PlayMethod.Transcode){
            savedTime = intentPosition;
        }

        mUri = createUri(location);
        logger.Debug("Parsed uri: %s", mUri.toString());

        itemTitle = getIntent().getExtras().getString(PLAY_EXTRA_ITEM_TITLE);

        String deviceProfileJson = getIntent().getExtras().getString("deviceProfileJson");
        DeviceProfile deviceProfile = (DeviceProfile)jsonSerializer.DeserializeFromString(deviceProfileJson, DeviceProfile.class);

        String apiAppName = getIntent().getExtras().getString("appName");
        String apiAppVersion = getIntent().getExtras().getString("appVersion");
        String apiDeviceId = getIntent().getExtras().getString("deviceId");
        String apiDeviceName = getIntent().getExtras().getString("deviceName");
        String apiUserId = getIntent().getExtras().getString("userId");
        String apiAccessToken = getIntent().getExtras().getString("accessToken");
        String apiServerUrl = getIntent().getExtras().getString("serverUrl");
        String apiServerId = getIntent().getExtras().getString("serverId");
        videoQualityOptionsJson = getIntent().getExtras().getString("videoQualityOptionsJson");

        IDevice device = new AndroidDevice(getApplicationContext(), apiDeviceId, apiDeviceName);
        ApiClient apiClient = new AndroidApiClient(httpClient, jsonSerializer, logger, apiServerUrl, apiAppName, device, apiAppVersion, new ApiEventListener());
        apiClient.SetAuthenticationInfo(apiAccessToken, apiUserId);

        updateExternalSubtitles(null);
        apiHelper.setInitialInfo(apiServerId, false, apiClient, deviceProfile, playbackStartInfo, mediaSourceInfo);

        if (mUri != null) {
            if (mService.hasMedia() && !mUri.equals(mService.getCurrentMediaWrapper().getUri()))
                mService.stop();
            // restore last position
            MediaWrapper media = openedMedia;
            /*if (openedMedia == null || openedMedia.getId() <= 0L) {
                Medialibrary ml = VLCApplication.getMLInstance();
                media = ml.getMedia(mUri.getPath());
                if (media == null && TextUtils.equals(mUri.getScheme(), "file") &&
                        mUri.getPath() != null && mUri.getPath().startsWith("/sdcard")) {
                    mUri = FileUtils.convertLocalUri(mUri);
                    media = ml.getMedia(mUri.getPath());
                }
            } else
                media = openedMedia;*/
            if (media != null) {
                // in media library
                if (media.getTime() > 0 && !fromStart && positionInPlaylist == -1) {
                    if (mAskResume) {
                        showConfirmResumeDialog();
                        return;
                    }
                }
                // Consume fromStart option after first use to prevent
                // restarting again when playback is paused.
                intent.putExtra(PLAY_EXTRA_FROM_START, false);
                if (fromStart || mService.isPlaying())
                    media.setTime(0L);
                else if (savedTime <= 0L)
                    savedTime = media.getTime();
                mLastAudioTrack = media.getAudioTrack();
                mLastSpuTrack = media.getSpuTrack();
            } else {
                // not in media library
                if (savedTime > 0L && mAskResume) {
                    showConfirmResumeDialog();
                    return;
                } else {
                    long rTime = mSettings.getLong(PreferencesActivity.VIDEO_RESUME_TIME, -1);

                    if (rTime > 0 && !fromStart) {
                        if (mAskResume) {
                            showConfirmResumeDialog();
                            return;
                        } else {
                            Editor editor = mSettings.edit();
                            editor.putLong(PreferencesActivity.VIDEO_RESUME_TIME, -1);
                            editor.apply();
                            savedTime = rTime;
                        }
                    }
                }
            }
            // Start playback & seek
            mService.addCallback(this);
            /* prepare playback */
            boolean hasMedia = mService.hasMedia();
            if (hasMedia)
                media = mService.getCurrentMediaWrapper();
            else if (media == null)
                media = new MediaWrapper(mUri);
            if (mWasPaused)
                media.addFlags(MediaWrapper.MEDIA_PAUSED);
            if (intent.hasExtra(PLAY_DISABLE_HARDWARE))
                media.addFlags(MediaWrapper.MEDIA_NO_HWACCEL);
            media.removeFlags(MediaWrapper.MEDIA_FORCE_AUDIO);
            media.addFlags(MediaWrapper.MEDIA_VIDEO);
            if (savedTime <= 0L && media.getTime() > 0L)
                savedTime = media.getTime();
            if (savedTime > 0L && !mService.isPlaying())
                mService.saveTimeToSeek(savedTime);
            // Handle playback
            if (!hasMedia)
                mService.load(media);
            else if (!mService.isPlaying())
                mService.playIndex(positionInPlaylist);
            else
                onPlaying();
            savedIndexPosition = mService.getCurrentMediaPosition();
            // Get possible subtitles
            //getSubtitles();
            // Get the title
            if (itemTitle == null)
                title = mUri.getLastPathSegment();
        } else {
            mService.addCallback(this);
            mService.loadLastPlaylist(PlaybackService.TYPE_VIDEO);
            MediaWrapper mw = mService.getCurrentMediaWrapper();
            if (mw == null) {
                finish();
                return;
            }
            mUri = mService.getCurrentMediaWrapper().getUri();
        }
        if (itemTitle != null)
            title = itemTitle;
        mTitle.setText(title);
        if (mWasPaused) {
            // XXX: Workaround to update the seekbar position
            mForcedTime = savedTime;
            setOverlayProgress();
            mForcedTime = -1;
            showOverlay(true);
        }

        stopTimeLimitTimer();

        if (timeLimitMs > 0){
            startTimeLimitTimer(timeLimitMs);
        }
    }

    @SuppressWarnings("deprecation")
    private int getScreenRotation(){
        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        try {
            Method m = display.getClass().getDeclaredMethod("getRotation");
            return (Integer) m.invoke(display);
        } catch (Exception e) {
            return Surface.ROTATION_0;
        }
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private int getScreenOrientation(int mode){
        switch(mode) {
            case 99: //screen orientation user
                return AndroidUtil.isJellyBeanMR2OrLater() ?
                        ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR :
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR;
            case 101: //screen orientation landscape
                return ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
            case 102: //screen orientation portrait
                return ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT;
        }
        /*
         mScreenOrientation = 100, we lock screen at its current orientation
         */
        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int rot = getScreenRotation();
        /*
         * Since getRotation() returns the screen's "natural" orientation,
         * which is not guaranteed to be SCREEN_ORIENTATION_PORTRAIT,
         * we have to invert the SCREEN_ORIENTATION value if it is "naturally"
         * landscape.
         */
        @SuppressWarnings("deprecation")
        boolean defaultWide = display.getWidth() > display.getHeight();
        if(rot == Surface.ROTATION_90 || rot == Surface.ROTATION_270)
            defaultWide = !defaultWide;
        if(defaultWide) {
            switch (rot) {
                case Surface.ROTATION_0:
                    return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                case Surface.ROTATION_90:
                    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                case Surface.ROTATION_180:
                    // SCREEN_ORIENTATION_REVERSE_PORTRAIT only available since API
                    // Level 9+
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                case Surface.ROTATION_270:
                    // SCREEN_ORIENTATION_REVERSE_LANDSCAPE only available since API
                    // Level 9+
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                default:
                    return 0;
            }
        } else {
            switch (rot) {
                case Surface.ROTATION_0:
                    return ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                case Surface.ROTATION_90:
                    return ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                case Surface.ROTATION_180:
                    // SCREEN_ORIENTATION_REVERSE_PORTRAIT only available since API
                    // Level 9+
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                case Surface.ROTATION_270:
                    // SCREEN_ORIENTATION_REVERSE_LANDSCAPE only available since API
                    // Level 9+
                    return ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                default:
                    return 0;
            }
        }
    }
    public void showConfirmResumeDialog() {
        if (isFinishing())
            return;
        mService.pause();
        /* Encountered Error, exit player with a message */
        mAlertDialog = new AlertDialog.Builder(VideoPlayerActivity.this)
                .setMessage(R.string.confirm_resume)
                .setPositiveButton(R.string.resume_from_position, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        loadMedia(false);
                    }
                })
                .setNegativeButton(R.string.play_from_start, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        loadMedia(true);
                    }
                })
                .create();
        mAlertDialog.setCancelable(false);
        mAlertDialog.show();
    }
    public void showAdvancedOptions() {
        /*FragmentManager fm = getSupportFragmentManager();
        AdvOptionsDialog advOptionsDialog = new AdvOptionsDialog();
        advOptionsDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dimStatusBar(true);
            }
        });
        advOptionsDialog.show(fm, "fragment_adv_options");
        hideOverlay(false);*/
    }
    private void togglePlaylist() {
        /*if (mPlaylist.getVisibility() == View.VISIBLE) {
            mPlaylist.setVisibility(View.GONE);
            mPlaylist.setOnClickListener(null);
            return;
        }
        hideOverlay(true);
        mPlaylist.setVisibility(View.VISIBLE);
        mPlaylist.setAdapter(mPlaylistAdapter);
        updateList();*/
    }
    private BroadcastReceiver mBtReceiver = AndroidUtil.isICSOrLater() ? new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED:
                case BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED:
                    long savedDelay = mSettings.getLong(KEY_BLUETOOTH_DELAY, 0l);
                    long currentDelay = mService.getAudioDelay();
                    if (savedDelay != 0l) {
                        boolean connected = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, -1) == BluetoothA2dp.STATE_CONNECTED;
                        if (connected && currentDelay == 0l)
                            toggleBtDelay(true);
                        else if (!connected && savedDelay == currentDelay)
                            toggleBtDelay(false);
                    }
            }
        }
    } : null;
    private void toggleBtDelay(boolean connected) {
        mService.setAudioDelay(connected ? mSettings.getLong(KEY_BLUETOOTH_DELAY, 0) : 0l);
    }
    private OnClickListener mBtSaveListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            mSettings.edit().putLong(KEY_BLUETOOTH_DELAY, mService.getAudioDelay()).apply();
        }
    };

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void createPresentation() {
        if (mMediaRouter == null || mEnableCloneMode)
            return;
        // Get the current route and its presentation display.
        MediaRouter.RouteInfo route = mMediaRouter.getSelectedRoute(
                MediaRouter.ROUTE_TYPE_LIVE_VIDEO);
        Display presentationDisplay = route != null ? route.getPresentationDisplay() : null;
        if (presentationDisplay != null) {
            // Show a new presentation if possible.
            logger.Info("Showing presentation on display: " + presentationDisplay);
            mPresentation = new SecondaryDisplay(this, LibVLC(getApplicationContext(), logger), presentationDisplay, logger);
            mPresentation.setOnDismissListener(mOnDismissListener);
            try {
                mPresentation.show();
                mPresentationDisplayId = presentationDisplay.getDisplayId();
            } catch (WindowManager.InvalidDisplayException ex) {
                logger.Warn("Couldn't show presentation!  Display was removed in "
                        + "the meantime.", ex);
                mPresentation = null;
            }
        } else
            logger.Info("No secondary display detected");
    }
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void removePresentation() {
        if (mMediaRouter == null)
            return;
        // Dismiss the current presentation if the display has changed.
        logger.Info("Dismissing presentation because the current route no longer "
                + "has a presentation display.");
        if (mPresentation != null) mPresentation.dismiss();
        mPresentation = null;
        mPresentationDisplayId = -1;
        stopPlayback();
        recreate();
    }

    /**
     * Listens for when presentations are dismissed.
     */
    private final DialogInterface.OnDismissListener mOnDismissListener = new DialogInterface.OnDismissListener() {
        @Override
        public void onDismiss(DialogInterface dialog) {
            if (dialog == mPresentation) {
                logger.Info("Presentation was dismissed.");
                mPresentation = null;
            }
        }
    };

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private static final class SecondaryDisplay extends Presentation {
        public final static String TAG = "VLC/SecondaryDisplay";
        private SurfaceView mSurfaceView;
        private SurfaceView mSubtitlesSurfaceView;
        private FrameLayout mSurfaceFrame;
        private ILogger logger;

        public SecondaryDisplay(Context context, LibVLC libVLC, Display display, ILogger logger) {
            super(context, display);
            this.logger = logger;
            if (context instanceof AppCompatActivity) {
                setOwnerActivity((AppCompatActivity) context);
            }
        }
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.player_remote);
            mSurfaceView = (SurfaceView) findViewById(R.id.remote_player_surface);
            mSubtitlesSurfaceView = (SurfaceView) findViewById(R.id.remote_subtitles_surface);
            mSurfaceFrame = (FrameLayout) findViewById(R.id.remote_player_surface_frame);
            mSubtitlesSurfaceView.setZOrderMediaOverlay(true);
            mSubtitlesSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
            VideoPlayerActivity activity = (VideoPlayerActivity)getOwnerActivity();
            if (activity == null) {
                logger.Error("Failed to get the VideoPlayerActivity instance, secondary display won't work");
                return;
            }
            logger.Info("Secondary display created");
        }
    }


    /**
     * Start the video loading animation.
     */
    private void startLoading() {
        if (mIsLoading)
            return;
        mIsLoading = true;
        /*AnimationSet anim = new AnimationSet(true);
        RotateAnimation rotate = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(800);
        rotate.setInterpolator(new DecelerateInterpolator());
        rotate.setRepeatCount(RotateAnimation.INFINITE);
        anim.addAnimation(rotate);
        mLoading.setVisibility(View.VISIBLE);
        mLoading.startAnimation(anim);*/
    }
    /**
     * Stop the video loading animation.
     */
    private void stopLoading() {
        mHandler.removeMessages(LOADING_ANIMATION);
        if (!mIsLoading)
            return;
        mIsLoading = false;
        mLoading.setVisibility(View.INVISIBLE);
        mLoading.clearAnimation();
        if (mPresentation != null) {
            mTipsBackground.setVisibility(View.VISIBLE);
        }
    }
    public void onClickOverlayTips(View v) {
        mOverlayTips.setVisibility(View.GONE);
    }
    public void onClickDismissTips(View v) {
        mOverlayTips.setVisibility(View.GONE);
        Editor editor = mSettings.edit();
        editor.putBoolean(PREF_TIPS_SHOWN, true);
        editor.apply();
    }
    private void updateNavStatus() {
        mIsNavMenu = false;
        mMenuIdx = -1;
        final MediaPlayer.Title[] titles = mService.getTitles();
        if (titles != null) {
            final int currentIdx = mService.getTitleIdx();
            for (int i = 0; i < titles.length; ++i) {
                final MediaPlayer.Title title = titles[i];
                if (title.isMenu()) {
                    mMenuIdx = i;
                    break;
                }
            }
            mIsNavMenu = mMenuIdx == currentIdx;
        }
        if (mIsNavMenu) {
            /*
             * Keep the overlay hidden in order to have touch events directly
             * transmitted to navigation handling.
             */
            hideOverlay(false);
        }
        else if (mMenuIdx != -1)
            setESTracks();
        //mNavMenu.setVisibility(mMenuIdx >= 0 && mNavMenu != null ? View.VISIBLE : View.GONE);
        supportInvalidateOptionsMenu();
    }

    private GestureDetector.SimpleOnGestureListener mGestureListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (mShowing)
                hideOverlay(true);
            else
                showOverlay();
            return true;
        }
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (mService == null)
                return false;
            if (!mIsLocked) {
                doPlayPause();
                return true;
            }
            return false;
        }
    };
    public PlaybackServiceActivity.Helper getHelper() {
        return mHelper;
    }
    @Override
    public void onConnected(PlaybackService service) {
        mService = service;
        mService.setApiHelper(apiHelper);
        if (!mSwitchingView)
            mHandler.sendEmptyMessage(START_PLAYBACK);
        mSwitchingView = false;
        mSettings.edit().putBoolean(PreferencesActivity.VIDEO_RESTORE, false).apply();
    }
    @Override
    public void onDisconnected() {
        mService = null;
        mHandler.sendEmptyMessage(AUDIO_SERVICE_CONNECTION_FAILED);
    }

    @Override
    public void onNewLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
        // store video size
        mVideoWidth = width;
        mVideoHeight = height;
        mVideoVisibleWidth  = visibleWidth;
        mVideoVisibleHeight = visibleHeight;
        mSarNum = sarNum;
        mSarDen = sarDen;
        changeSurfaceLayout();
    }
    @Override
    public void onSurfacesCreated(IVLCVout vlcVout) {
    }
    @Override
    public void onSurfacesDestroyed(IVLCVout vlcVout) {
    }
    private BroadcastReceiver mServiceReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (TextUtils.equals(intent.getAction(), PLAY_FROM_SERVICE))
                onNewIntent(intent);
            else if (TextUtils.equals(intent.getAction(), EXIT_PLAYER))
                exitOK();
        }
    };

    private SubtitleTrackInfo timedTextObject;
    private long lastReportedPositionMs = 0;

    public void updateExternalSubtitles(SubtitleTrackInfo timedTextObject) {

        lastReportedPositionMs = 0;
        this.timedTextObject = timedTextObject;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (subtitleText != null) {
                    subtitleText.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    public long getTranscodingOffsetTicks(){
        return mService.getTranscodingOffsetPositionTicks();
    }

    private void updateSubtitles(long positionMs) {

        if (lastReportedPositionMs > 0){
            if (Math.abs(lastReportedPositionMs - positionMs) < 500) {
                return;
            }
        }
        SubtitleTrackInfo info = timedTextObject;

        if (info == null) {
            return;
        }

        long positionTicks = positionMs * 10000;

        for (SubtitleTrackEvent caption : info.getTrackEvents()) {
            if (positionTicks >= caption.getStartPositionTicks() && positionTicks <= caption.getEndPositionTicks()) {
                setTimedText(caption);
                return;
            }
        }

        setTimedText(null);
    }

    private void setTimedText(final SubtitleTrackEvent textObj) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (subtitleText == null){
                    return;
                }

                if (textObj == null) {
                    subtitleText.setVisibility(View.INVISIBLE);
                    return;
                }

                String text = textObj.getText();

                if (text == null || text.length() == 0) {
                    subtitleText.setVisibility(View.INVISIBLE);
                    return;
                }

                subtitleText.setText(Html.fromHtml(text));
                subtitleText.setVisibility(View.VISIBLE);
            }
        });
    }

    private void startTimeLimitTimer(long durationMs){

        timeLimitTimer = new Timer(true);

        timeLimitTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        onBackPressed();
                    }
                });
            }
        }, durationMs);
    }

    private void stopTimeLimitTimer(){

        if (timeLimitTimer != null){
            timeLimitTimer.cancel();
            timeLimitTimer = null;
        }
    }
}