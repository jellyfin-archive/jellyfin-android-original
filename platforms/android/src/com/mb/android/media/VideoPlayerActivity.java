package com.mb.android.media;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.app.Presentation;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
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
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.MenuItemCompat;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnSystemUiVisibilityChangeListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.mb.android.BuildConfig;
import com.mb.android.R;
import com.mb.android.api.ApiClientBridge;
import com.mb.android.logging.AppLogger;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.AndroidUtil;
import org.videolan.libvlc.util.HWDecoderUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
import mediabrowser.model.dto.NameIdPair;
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
        GestureDetector.OnDoubleTapListener, IDelayController, LibVLC.HardwareAccelerationError,
        PlaybackService.Client.Callback, PlaybackService.Callback {

    public final static String TAG = "VLC/VideoPlayerActivity";

    // Internal intent identifier to distinguish between internal launch and
    // external intent.
    public final static String PLAY_FROM_VIDEOGRID = "org.videolan.vlc.gui.video.PLAY_FROM_VIDEOGRID";

    public final static String PLAY_EXTRA_ITEM_LOCATION = "item_location";
    public final static String PLAY_EXTRA_SUBTITLES_LOCATION = "subtitles_location";
    public final static String PLAY_EXTRA_ITEM_TITLE = "title";
    public final static String PLAY_EXTRA_FROM_START = "from_start";
    public final static String PLAY_EXTRA_OPENED_POSITION = "opened_position";

    public final static String ACTION_RESULT = "org.videolan.vlc.player.result";
    public final static String EXTRA_POSITION = "extra_position";
    public final static String EXTRA_DURATION = "extra_duration";
    public final static int RESULT_CONNECTION_FAILED = RESULT_FIRST_USER + 1;
    public final static int RESULT_PLAYBACK_ERROR = RESULT_FIRST_USER + 2;
    public final static int RESULT_HARDWARE_ACCELERATION_ERROR = RESULT_FIRST_USER + 3;
    public final static int RESULT_VIDEO_TRACK_LOST = RESULT_FIRST_USER + 4;

    private final PlaybackServiceActivity.Helper mHelper = new PlaybackServiceActivity.Helper(this, this);
    private PlaybackService mService;
    private SurfaceView mSurfaceView = null;
    private SurfaceView mSubtitlesSurfaceView = null;
    private FrameLayout mSurfaceFrame;
    private MediaRouter mMediaRouter;
    private MediaRouter.SimpleCallback mMediaRouterCallback;
    private SecondaryDisplay mPresentation;
    private int mPresentationDisplayId = -1;
    private Uri mUri;
    private boolean mAskResume = true;
    private GestureDetectorCompat mDetector;

    private static final int SURFACE_BEST_FIT = 0;
    private static final int SURFACE_FIT_HORIZONTAL = 1;
    private static final int SURFACE_FIT_VERTICAL = 2;
    private static final int SURFACE_FILL = 3;
    private static final int SURFACE_16_9 = 4;
    private static final int SURFACE_4_3 = 5;
    private static final int SURFACE_ORIGINAL = 6;
    private int mCurrentSize = SURFACE_BEST_FIT;

    private SharedPreferences mSettings;

    /** Overlay */
    private ActionBar mActionBar;
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
    private static final int HW_ERROR = 1000; // TODO REMOVE

    private boolean mDragging;
    private boolean mShowing;
    private DelayState mDelay = DelayState.OFF;
    private int mUiVisibility = -1;
    private SeekBar mSeekbar;
    private TextView mTitle;
    private TextView mSysTime;
    private TextView mBattery;
    private TextView mTime;
    private TextView mLength;
    private TextView mInfo;
    private View mVerticalBar;
    private View mVerticalBarProgress;
    private boolean mIsLoading;
    private ImageView mLoading;
    private ImageView mTipsBackground;
    private ImageView mPlayPause;
    private ImageView mTracks;
    private ImageView mDelayPlus;
    private ImageView mDelayMinus;
    private boolean mEnableBrightnessGesture;
    private boolean mEnableCloneMode;
    private boolean mDisplayRemainingTime = false;
    private int mScreenOrientation;
    private int mScreenOrientationLock;
    private ImageView mLock;
    private ImageView mSize;
    private boolean mIsLocked = false;
    /* -1 is a valid track (Disable) */
    private int mLastAudioTrack = -2;
    private int mLastSpuTrack = -2;
    private int mOverlayTimeout = 0;
    private boolean mLockBackButton = false;

    /**
     * For uninterrupted switching between audio and video mode
     */
    private boolean mSwitchingView;
    private boolean mHardwareAccelerationError;
    private boolean mEndReached;
    private boolean mCanSeek;

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
    private static final int TOUCH_SEEK = 3;
    private int mTouchAction = TOUCH_NONE;
    private int mSurfaceYDisplayRange;
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

        /* Services and miscellaneous */
        mAudioManager = (AudioManager) getApplicationContext().getSystemService(AUDIO_SERVICE);
        mAudioMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        mEnableCloneMode = mSettings.getBoolean("enable_clone_mode", false);
        createPresentation();
        setContentView(mPresentation == null ? R.layout.player : R.layout.player_remote_control);

        if (AndroidUtil.isICSOrLater())
            getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(
                    new OnSystemUiVisibilityChangeListener() {
                        @Override
                        public void onSystemUiVisibilityChange(int visibility) {
                            if (visibility == mUiVisibility)
                                return;
                            if (visibility == View.SYSTEM_UI_FLAG_VISIBLE && !mShowing && !isFinishing()) {
                                showOverlay();
                            }
                            mUiVisibility = visibility;
                        }
                    }
            );

        /** initialize Views an their Events */
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(false);
        mActionBar.setDisplayShowTitleEnabled(false);
        mActionBar.setBackgroundDrawable(null);
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setCustomView(R.layout.player_action_bar);

        ViewGroup view = (ViewGroup) mActionBar.getCustomView();
        /* Dispatch ActionBar touch events to the Activity */
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                onTouchEvent(event);
                return true;
            }
        });
        mTitle = (TextView) view.findViewById(R.id.player_overlay_title);
        mSysTime = (TextView) findViewById(R.id.player_overlay_systime);
        mBattery = (TextView) findViewById(R.id.player_overlay_battery);
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
        mVerticalBar = findViewById(R.id.verticalbar);
        mVerticalBarProgress = findViewById(R.id.verticalbar_progress);

        mEnableBrightnessGesture = mSettings.getBoolean("enable_brightness_gesture", true);
        mScreenOrientation = Integer.valueOf(
                mSettings.getString("screen_orientation_value", "4" /*SCREEN_ORIENTATION_SENSOR*/));

        mPlayPause = (ImageView) findViewById(R.id.player_overlay_play);

        mTracks = (ImageView) findViewById(R.id.player_overlay_tracks);
        mLock = (ImageView) findViewById(R.id.lock_overlay_button);

        mSize = (ImageView) findViewById(R.id.player_overlay_size);

        mDelayPlus = (ImageView) findViewById(R.id.player_delay_plus);
        mDelayMinus = (ImageView) findViewById(R.id.player_delay_minus);

        mSurfaceView = (SurfaceView) findViewById(R.id.player_surface);
        mSubtitlesSurfaceView = (SurfaceView) findViewById(R.id.subtitles_surface);

        if (HWDecoderUtil.HAS_SUBTITLES_SURFACE) {
            mSubtitlesSurfaceView.setZOrderMediaOverlay(true);
            mSubtitlesSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        } else
            mSubtitlesSurfaceView.setVisibility(View.GONE);

        mSurfaceFrame = (FrameLayout) findViewById(R.id.player_surface_frame);

        mSeekbar = (SeekBar) findViewById(R.id.player_overlay_seekbar);

        /* Loading view */
        mLoading = (ImageView) findViewById(R.id.player_overlay_loading);
        if (mPresentation != null)
            mTipsBackground = (ImageView) findViewById(R.id.player_remote_tips_background);
        dimStatusBar(false);
        startLoading();

        mSwitchingView = false;
        mHardwareAccelerationError = false;
        mEndReached = false;

        mAskResume = mSettings.getBoolean("dialog_confirm_resume", false);
        // Clear the resume time, since it is only used for resumes in external
        // videos.
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putLong(PreferencesActivity.VIDEO_RESUME_TIME, -1);
        // Also clear the subs list, because it is supposed to be per session
        // only (like desktop VLC). We don't want the custom subtitle files
        // to persist forever with this video.
        editor.putString(PreferencesActivity.VIDEO_SUBTITLE_FILES, null);
        // Paused flag - per session too, like the subs list.
        editor.remove(PreferencesActivity.VIDEO_PAUSED);
        Util.commitPreferences(editor);

        IntentFilter filter = new IntentFilter();
        if (mBattery != null)
            filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(Constants.SLEEP_INTENT);
        registerReceiver(mReceiver, filter);

        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Extra initialization when no secondary display is detected
        if (mPresentation == null) {
            // Orientation
            // 100 is the value for screen_orientation_start_lock
            setRequestedOrientation(mScreenOrientation != 100
                    ? mScreenOrientation
                    : getScreenOrientation());
            // Tips
            mOverlayTips = findViewById(R.id.player_overlay_tips);
            if(mSettings.getBoolean(PREF_TIPS_SHOWN, false))
                mOverlayTips.setVisibility(View.GONE);
            else {
                mOverlayTips.bringToFront();
                mOverlayTips.invalidate();
            }
        } else
            setRequestedOrientation(getScreenOrientation());

        resetHudLayout();
        mDetector = new GestureDetectorCompat(this, mGestureListener);
        mDetector.setOnDoubleTapListener(this);

        subtitleText = (TextView) findViewById(R.id.offLine_subtitleText);
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
        mSwitchingView = false;

        /*
         * Set listeners here to avoid NPE when activity is closing
         */
        mSeekbar.setOnSeekBarChangeListener(mSeekListener);
        mLock.setOnClickListener(mLockListener);
        mPlayPause.setOnClickListener(mPlayPauseListener);
        mPlayPause.setOnLongClickListener(mPlayPauseLongListener);
        mLength.setOnClickListener(mRemainingTimeListener);
        mTime.setOnClickListener(mRemainingTimeListener);
        mSize.setOnClickListener(mSizeListener);

        if (mIsLocked && mScreenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR)
            setRequestedOrientation(mScreenOrientationLock);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onPause() {
        super.onPause();
        mSeekbar.setOnSeekBarChangeListener(null);
        mLock.setOnClickListener(null);
        mPlayPause.setOnClickListener(null);
        mPlayPause.setOnLongClickListener(null);
        mLength.setOnClickListener(null);
        mTime.setOnClickListener(null);
        mSize.setOnClickListener(null);

        /* Stop the earliest possible to avoid vout error */
        if (isFinishing())
            stopPlayback();
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            requestVisibleBehind(true);
    }

    @Override
    public void onVisibleBehindCanceled() {
        super.onVisibleBehindCanceled();
        pause();
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
        if (getScreenOrientation() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
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
        mHelper.onStart();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onStop() {
        super.onStop();

        if (mAlertDialog != null && mAlertDialog.isShowing())
            mAlertDialog.dismiss();
        if (!isFinishing() && mSettings.getBoolean(PreferencesActivity.VIDEO_BACKGROUND, false)) {
            Util.commitPreferences(mSettings.edit().putBoolean(PreferencesActivity.VIDEO_RESTORE, true));
            switchToAudioMode(false);
        }
        stopPlayback();

        // Dismiss the presentation when the activity is not visible.
        if (mPresentation != null) {
            logger.Info("Dismissing presentation because the activity is no longer visible.");
            mPresentation.dismiss();
            mPresentation = null;
        }
        restoreBrightness();
        if (mService != null)
            mService.removeCallback(this);
        mHelper.onStop();
    }

    @TargetApi(android.os.Build.VERSION_CODES.FROYO)
    private void restoreBrightness() {
        /*if (mRestoreAutoBrightness != -1f) {
            int brightness = (int) (mRestoreAutoBrightness*255f);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    brightness);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
        }*/
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mReceiver != null)
            unregisterReceiver(mReceiver);

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

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void startPlayback() {
        /* start playback only when audio service and both surfaces are ready */
        if (mPlaybackStarted || mService == null)
            return;

        logger.Debug("Begining startPlayback");

        LibVLC(getApplicationContext(), logger).setOnHardwareAccelerationError(this);
        final IVLCVout vlcVout = mService.getVLCVout();
        if (mPresentation == null) {
            vlcVout.setVideoView(mSurfaceView);
            if (mSubtitlesSurfaceView.getVisibility() != View.GONE)
                vlcVout.setSubtitlesView(mSubtitlesSurfaceView);
        } else {
            vlcVout.setVideoView(mPresentation.mSurfaceView);
            if (mSubtitlesSurfaceView.getVisibility() != View.GONE)
                vlcVout.setSubtitlesView(mPresentation.mSubtitlesSurfaceView);
        }
        vlcVout.addCallback(this);
        vlcVout.attachViews();

        mPlaybackStarted = true;

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

        if (mMediaRouter != null) {
            // Listen for changes to media routes.
            mediaRouterAddCallback(true);
        }

        loadMedia();

        mSurfaceView.setKeepScreenOn(true);

        // Add any selected subtitle file from the file picker
        if(mSubtitleSelectedFiles.size() > 0) {
            for(String file : mSubtitleSelectedFiles) {
                logger.Info("Adding user-selected subtitle " + file);
                mService.addSubtitleTrack(file);
            }
        }

        // Set user playback speed
        mService.setRate(mSettings.getFloat(PreferencesActivity.VIDEO_SPEED, 1));
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void stopPlayback() {
        if (!mPlaybackStarted)
            return;

        LibVLC(getApplicationContext(), logger).setOnHardwareAccelerationError(null);

        mPlaybackStarted = false;

        mService.removeCallback(this);
        final IVLCVout vlcVout = mService.getVLCVout();
        vlcVout.removeCallback(this);
        vlcVout.detachViews();
        if(mSwitchingView && mService != null) {
            logger.Debug("mLocation = \"" + mUri + "\"");
            mService.showWithoutParse(savedIndexPosition);
            return;
        }

        mHandler.removeCallbacksAndMessages(null);

        mSurfaceView.setKeepScreenOn(false);

        if (mMediaRouter != null) {
            // Stop listening for changes to media routes.
            mediaRouterAddCallback(false);
        }

        final boolean isPaused = !mService.isPlaying();
        long time = getTime();
        long length = mService.getLength();
        //remove saved position if in the last 5 seconds
        if (length - time < 5000)
            time = 0;
        else
            time -= 5000; // go back 5 seconds, to compensate loading time

        mService.stop();

        mService.setRate(1.0f);

        if (AndroidUtil.isHoneycombOrLater() && mOnLayoutChangeListener != null)
            mSurfaceFrame.removeOnLayoutChangeListener(mOnLayoutChangeListener);
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
    public static void startOpened(Context context, int openedPosition) {
        start(context, null, null, false, openedPosition);
    }

    private static void start(Context context, Uri uri, String title, boolean fromStart, int openedPosition) {
        Intent intent = new Intent(context, VideoPlayerActivity.class);
        intent.setAction(PLAY_FROM_VIDEOGRID);
        intent.putExtra(PLAY_EXTRA_ITEM_LOCATION, uri);
        intent.putExtra(PLAY_EXTRA_ITEM_TITLE, title);
        intent.putExtra(PLAY_EXTRA_FROM_START, fromStart);
        intent.putExtra(PLAY_EXTRA_OPENED_POSITION, openedPosition);

        if (openedPosition != -1)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);

        context.startActivity(intent);
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
                seekDelta(x > 0.0f ? 10000 : -10000);
            } else if (Math.abs(y) > 0.3){
                if (mIsFirstBrightnessGesture)
                    initBrightnessTouch();
                changeBrightness(-y / 10f);
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
            Toast.makeText(this, getString(R.string.back_quit_lock), Toast.LENGTH_SHORT).show();
        } else if (mDelay != DelayState.OFF){
            endDelaySetting();
        } else
        {
            mService.setResult(false, false);
            exitOK();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_BUTTON_B)
            return super.onKeyDown(keyCode, event);
        if (mIsLoading) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_S:
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    mService.setResult(false, false);
                    exitOK();
                    return true;
            }
            return false;
        }
        showOverlayTimeout(OVERLAY_TIMEOUT);
        switch (keyCode) {
            case KeyEvent.KEYCODE_F:
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                seekDelta(10000);
                return true;
            case KeyEvent.KEYCODE_R:
            case KeyEvent.KEYCODE_MEDIA_REWIND:
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
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
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (mIsNavMenu)
                    return navigateDvdMenu(keyCode);
                else
                    return super.onKeyDown(keyCode, event);
            case KeyEvent.KEYCODE_J:
                delayAudio(-50000l);
                break;
            case KeyEvent.KEYCODE_K:
                delayAudio(50000l);
                break;
            case KeyEvent.KEYCODE_G:
                delaySubs(-50000l);
                break;
            case KeyEvent.KEYCODE_H:
                delaySubs(50000l);
                break;
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
        mDelay = DelayState.AUDIO;
        showDelayControls();
    }

    @Override
    public void showSubsDelaySetting() {
        mDelay = DelayState.SUBS;
        showDelayControls();
    }

    public void showDelayControls(){
        mTouchAction = TOUCH_NONE;
        showOverlayTimeout(OVERLAY_INFINITE);
        mDelayMinus.setOnClickListener(mAudioDelayListener);
        mDelayPlus.setOnClickListener(mAudioDelayListener);
        mDelayMinus.setOnTouchListener(new OnRepeatListener(mAudioDelayListener));
        mDelayPlus.setOnTouchListener(new OnRepeatListener(mAudioDelayListener));
        mDelayMinus.setVisibility(View.VISIBLE);
        mDelayPlus.setVisibility(View.VISIBLE);
        mDelayPlus.requestFocus();
        initDelayInfo();
    }

    private void initDelayInfo() {
        if (mPresentation == null)
            mVerticalBar.setVisibility(View.GONE);
        mInfo.setVisibility(View.VISIBLE);
        String text = "";
        if (mDelay == DelayState.AUDIO) {
            text += getString(R.string.audio_delay)+"\n";
            text += mService.getAudioDelay() / 1000l;
        } else if (mDelay == DelayState.SUBS) {
            text += getString(R.string.spu_delay)+"\n";
            text += mService.getSpuDelay() / 1000l;
        } else
            text += "0";
        text += " ms";
        mInfo.setText(text);
    }

    @Override
    public void endDelaySetting() {
        mTouchAction = TOUCH_NONE;
        mDelay = DelayState.OFF;
        mDelayMinus.setOnClickListener(null);
        mDelayPlus.setOnClickListener(null);
        mDelayMinus.setVisibility(View.INVISIBLE);
        mDelayPlus.setVisibility(View.INVISIBLE);
        mInfo.setVisibility(View.INVISIBLE);
        mInfo.setText("");
        mPlayPause.requestFocus();
    }

    private OnClickListener mAudioDelayListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.player_delay_minus:
                    if (mDelay == DelayState.AUDIO)
                        delayAudio(-50000);
                    else if (mDelay == DelayState.SUBS)
                        delaySubs(-50000);
                    break;
                case R.id.player_delay_plus:
                    if (mDelay == DelayState.AUDIO)
                        delayAudio(50000);
                    else if (mDelay == DelayState.SUBS)
                        delaySubs(50000);
                    break;
            }
        }
    };

    public void delayAudio(long delta){
        long delay = mService.getAudioDelay()+delta;
        mService.setAudioDelay(delay);
        mInfo.setText(getString(R.string.audio_delay)+"\n"+(delay/1000l)+" ms");
        if (mDelay == DelayState.OFF) {
            mDelay = DelayState.AUDIO;
            initDelayInfo();
        }
    }

    public void delaySubs(long delta){
        logger.Debug("delaySubs " + delta);
        long delay = mService.getSpuDelay()+delta;
        mService.setSpuDelay(delay);
        mInfo.setText(getString(R.string.spu_delay) + "\n" + (delay / 1000l) + " ms");
        if (mDelay == DelayState.OFF) {
            mDelay = DelayState.SUBS;
            initDelayInfo();
        }
    }

    /**
     * Lock screen rotation
     */
    private void lockScreen() {
        if(mScreenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR) {
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                setRequestedOrientation(14 /* SCREEN_ORIENTATION_LOCKED */);
            else
                setRequestedOrientation(getScreenOrientation());
            mScreenOrientationLock = getScreenOrientation();
        }
        showInfo(R.string.locked, 1000);
        mLock.setImageResource(R.drawable.ic_locked_circle);
        mTime.setEnabled(false);
        mSeekbar.setEnabled(false);
        mLength.setEnabled(false);
        mSize.setEnabled(false);
        hideOverlay(true);
        mLockBackButton = true;
    }

    /**
     * Remove screen lock
     */
    private void unlockScreen() {
        if(mScreenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        showInfo(R.string.unlocked, 1000);
        mLock.setImageResource(R.drawable.ic_lock_circle);
        mTime.setEnabled(true);
        mSeekbar.setEnabled(true);
        mLength.setEnabled(true);
        mSize.setEnabled(true);
        mShowing = false;
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
     * Show text in the info view
     * @param text
     */
    private void showInfo(String text) {
        if (mPresentation == null)
            mVerticalBar.setVisibility(View.GONE);
        mHandler.removeMessages(FADE_OUT_INFO);
        mInfo.setVisibility(View.VISIBLE);
        mInfo.setText(text);
        hideInfo();
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

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
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

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
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
        }
    }

    //FIXME hack to workaround playlist loading fail.
    boolean mIgnorePlaylistEnd = false;
    @Override
    public void onMediaPlayerEvent(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaPlayer.Event.Playing:
                mForcedTime = -1;
                mLastTime = -1;
                if (mService.getCurrentMediaWrapper().getType() == MediaWrapper.TYPE_PLAYLIST){
                    mIgnorePlaylistEnd = true;
                } else
                    onPlaying();
                break;
            case MediaPlayer.Event.Paused:
                updateOverlayPausePlay();
                break;
            case MediaPlayer.Event.Stopped:
                exitOK();
                break;
            case MediaPlayer.Event.EndReached:
                if(mIgnorePlaylistEnd)
                    mIgnorePlaylistEnd = false;
                else
                    endReached();
                break;
            case MediaPlayer.Event.EncounteredError:
                encounteredError();
                break;
            case MediaPlayer.Event.TimeChanged:
                updateSubtitles(mService.getTime());
                break;
            case MediaPlayer.Event.PositionChanged:
                if (!mCanSeek)
                    mCanSeek = true;
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
                        //logger.Info("No video track, open in audio mode");
                        //switchToAudioMode(true);
                    }
                    break;
                case HW_ERROR:
                    handleHardwareAccelerationError();
                    break;
            }
            return true;
        }
    });

    private boolean canShowProgress() {
        return !mDragging && mShowing && mService != null &&  mService.isPlaying();
    }

    private void onPlaying() {
        stopLoading();
        showOverlay();
        setESTracks();
        updateNavStatus();

        if (apiHelper.getMediaSource().getDefaultSubtitleStreamIndex() == null) {
            apiHelper.setSubtitleStreamIndex(mService.getMediaPlayer(), -1);
        } else {
            apiHelper.setSubtitleStreamIndex(mService.getMediaPlayer(), apiHelper.getMediaSource().getDefaultSubtitleStreamIndex());
        }
    }

    private void endReached() {
        if (mService == null)
            return;
        if (mService.getRepeatType() == PlaybackService.RepeatType.Once){
            seek(0);
            return;
        }
        if(mService.expand() == 0) {
            startLoading();
            logger.Debug("Found a video playlist, expanding it");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    loadMedia();
                }
            });
        } else {
            /* Exit player when reaching the end */
            mEndReached = true;
            mService.setResult(true, false);
            exitOK();
        }
    }

    private void encounteredError() {
        if (isFinishing())
            return;
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

    @Override
    public void eventHardwareAccelerationError() {
        mHandler.sendEmptyMessage(HW_ERROR);
    }

    private void handleHardwareAccelerationError() {
        mHardwareAccelerationError = true;
        if (mSwitchingView)
            return;
        mService.removeCallback(this);
        mService.stop();
        mAlertDialog = new AlertDialog.Builder(VideoPlayerActivity.this)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        mDisabledHardwareAcceleration = true;
                        loadMedia();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        exit(RESULT_HARDWARE_ACCELERATION_ERROR);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        exit(RESULT_HARDWARE_ACCELERATION_ERROR);
                    }
                })
                .setTitle(R.string.hardware_acceleration_error_title)
                .setMessage(R.string.hardware_acceleration_error_message)
                .create();
        if(!isFinishing())
            mAlertDialog.show();
    }

    private void handleVout(int voutCount) {
        final IVLCVout vlcVout = mService.getVLCVout();
        if (vlcVout.areViewsAttached() && voutCount == 0 && !mEndReached) {
            /* Video track lost, open in audio mode */
            logger.Info("Video track lost, switching to audio");
            mSwitchingView = true;
            exit(RESULT_VIDEO_TRACK_LOST);
        }
    }

    public void switchToAudioMode(boolean showUI) {
        if (mHardwareAccelerationError || mService == null)
            return;
        mSwitchingView = true;
        // Show the MainActivity if it is not in background.
        if (showUI && getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            /*Intent i = new Intent(this, MainActivity.class);
            if (!Util.isCallable(i)){
                try {
                    i = new Intent(this, Class.forName("org.videolan.vlc.gui.tv.audioplayer.AudioPlayerActivity"));
                } catch (ClassNotFoundException e) {
                    return;
                }
            }
            startActivity(i);*/
        }
        exitOK();
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

        if (mService != null) {
            final IVLCVout vlcVout = mService.getVLCVout();
            vlcVout.setWindowSize(sw, sh);
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

        // sanity check
        if (dw * dh == 0 || mVideoWidth * mVideoHeight == 0) {
            logger.Error("Invalid surface size");
            return;
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
            case SURFACE_FIT_HORIZONTAL:
                dh = dw / ar;
                break;
            case SURFACE_FIT_VERTICAL:
                dw = dh * ar;
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

        // set display size
        LayoutParams lp = surface.getLayoutParams();
        lp.width  = (int) Math.ceil(dw * mVideoWidth / mVideoVisibleWidth);
        lp.height = (int) Math.ceil(dh * mVideoHeight / mVideoVisibleHeight);
        surface.setLayoutParams(lp);
        if (subtitlesSurface != null)
            subtitlesSurface.setLayoutParams(lp);

        int subtitleMargin = (lp.height / 2) - 70;
        updateManualSubtitlePosition(subtitleMargin);

        // set frame size (crop if necessary)
        lp = surfaceFrame.getLayoutParams();
        lp.width = (int) Math.floor(dw);
        lp.height = (int) Math.floor(dh);
        surfaceFrame.setLayoutParams(lp);

        surface.invalidate();
        if (subtitlesSurface != null)
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
        if (mService == null || mIsLoading)
            return false;
        if (mDelay != DelayState.OFF){
            endDelaySetting();
            return true;
        }
        if (mDetector.onTouchEvent(event))
            return true;
        if (mIsLocked) {
            // locked, only handle show/hide & ignore all actions
            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (!mShowing) {
                    showOverlay();
                } else {
                    hideOverlay(true);
                }
            }
            return false;
        }

        DisplayMetrics screen = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(screen);

        if (mSurfaceYDisplayRange == 0)
            mSurfaceYDisplayRange = Math.min(screen.widthPixels, screen.heightPixels);

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
        float xgesturesize = ((x_changed / screen.xdpi) * 2.54f);
        float delta_y = Math.max(1f,((mInitTouchY - event.getRawY()) / screen.xdpi + 0.5f)*2f);

        /* Offset for Mouse Events */
        int[] offset = new int[2];
        mSurfaceView.getLocationOnScreen(offset);
        int xTouch = Math.round((event.getRawX() - offset[0]) * mVideoWidth / mSurfaceView.getWidth());
        int yTouch = Math.round((event.getRawY() - offset[1]) * mVideoHeight / mSurfaceView.getHeight());

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

                // No volume/brightness action if coef < 2 or a secondary display is connected
                //TODO : Volume action when a secondary display is connected
                if (mTouchAction != TOUCH_SEEK && coef > 2 && mPresentation == null) {
                    if (Math.abs(y_changed/mSurfaceYDisplayRange) < 0.05)
                        return false;
                    mTouchY = event.getRawY();
                    mTouchX = event.getRawX();
                    // Volume (Up or Down - Right side)
                    if (!mEnableBrightnessGesture || (int)mTouchX > (3 * screen.widthPixels / 5)){
                        doVolumeTouch(y_changed);
                        hideOverlay(true);
                    }
                    // Brightness (Up or Down - Left side)
                    if (mEnableBrightnessGesture && (int)mTouchX < (2 * screen.widthPixels / 5)){
                        doBrightnessTouch(y_changed);
                        hideOverlay(true);
                    }
                } else {
                    // Seek (Right or Left move)
                    doSeekTouch(Math.round(delta_y), xgesturesize, false);
                }
                break;

            case MotionEvent.ACTION_UP:
                // Mouse events for the core
                sendMouseEvent(MotionEvent.ACTION_UP, 0, xTouch, yTouch);

                if (mTouchAction == TOUCH_NONE) {
                    if (!mShowing) {
                        showOverlay();
                    } else {
                        hideOverlay(true);
                    }
                }
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
        if (Math.abs(gesturesize) < 1 || !mCanSeek)
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
            showInfo(String.format("%s%s (%s) x%d",
                    jump >= 0 ? "+" : "",
                    Strings.millisToString(jump),
                    Strings.millisToString(time + jump), coef), 1000);
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
        showInfo(mMute ? R.string.sound_off : R.string.sound_on,1000);
    }

    @TargetApi(android.os.Build.VERSION_CODES.FROYO)
    private void initBrightnessTouch() {
        float brightnesstemp = 0.6f;
        // Initialize the layoutParams screen brightness
        try {
            if (AndroidUtil.isFroyoOrLater() &&
                    Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                /*Settings.System.putInt(getContentResolver(),
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);*/
                mRestoreAutoBrightness = android.provider.Settings.System.getInt(getContentResolver(),
                        android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
            } else {
                brightnesstemp = android.provider.Settings.System.getInt(getContentResolver(),
                        android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
            }
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        WindowManager.LayoutParams lp = getWindow().getAttributes();
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
        lp.screenBrightness =  Math.min(Math.max(lp.screenBrightness + delta, 0.01f), 1);
        // Set Brightness
        getWindow().setAttributes(lp);
        int brightness = Math.round(lp.screenBrightness * 100);
        showInfoWithVerticalBar(getString(R.string.brightness) + "\n" + brightness + '%', 1000, brightness);
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
            if (fromUser && mCanSeek) {
                seek(progress);
                setOverlayProgress();
                mTime.setText(Strings.millisToString(progress));
                showInfo(Strings.millisToString(progress));
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
                        apiHelper.setAudioStreamIndex(mService.getMediaPlayer(), trackID);
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
                        apiHelper.setSubtitleStreamIndex(mService.getMediaPlayer(), trackID);
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
                        apiHelper.setQuality(mService.getMediaPlayer(), Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
                        return true;
                    }
                });
    }

    public void changeLocation(String location, MediaSourceInfo mediaSourceInfo, PlayMethod playMethod, long startPositionTicks) {

        location = normalizeLocation(location, mediaSourceInfo, playMethod, startPositionTicks);

        int playlistIndex = savedIndexPosition;
        //long currentTime = mService.getTime();

        Uri newUri = Uri.parse(location);

        mService.setMedia(newUri, 0);

        //resumePositionMs = currentTime;

        mAudioTracksList = null;
        mSubtitleTracksList = null;
        mQualityList = null;

        mService.restartIndex(playlistIndex, 0);
        mUri = newUri;
        mService.addCallback(this);
    }

    private String normalizeLocation(String location, MediaSourceInfo mediaSourceInfo, PlayMethod playMethod, long startPositionTicks) {

        if (playMethod == PlayMethod.Transcode && mediaSourceInfo.getRunTimeTicks() != null && mediaSourceInfo.getRunTimeTicks() > 0) {

            mService.setEnableServerSeek(true);
            mService.setTranscodingOffsetPositionTicks(startPositionTicks);

            // If transcoding with a known runtime, switch to .ts to work around the Vlc seek issue with HLS
            return location.replace("master.m3u8", "stream.ts") + "&StartTimeTicks=" + String.valueOf(startPositionTicks);
        }

        mService.setEnableServerSeek(false);
        mService.setTranscodingOffsetPositionTicks(0);
        return location;
    }

    private void showNavMenu() {
        /*if (mMenuIdx >= 0)
            mService.setTitleIdx(mMenuIdx);*/
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

    private final void doPlayPause() {
        if (mService.isPlaying()) {
            pause();
            showOverlayTimeout(OVERLAY_INFINITE);
        } else {
            play();
            showOverlayTimeout(OVERLAY_TIMEOUT);
        }
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
        if(mService.getLength() <= 0 || !mCanSeek) return;

        long position = getTime() + delta;
        if (position < 0) position = 0;
        seek(position);
        showOverlay();
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
            case SURFACE_FIT_HORIZONTAL:
                showInfo(R.string.surface_fit_horizontal, 1000);
                break;
            case SURFACE_FIT_VERTICAL:
                showInfo(R.string.surface_fit_vertical, 1000);
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
        showOverlay();
    }

    private final OnClickListener mRemainingTimeListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            mDisplayRemainingTime = !mDisplayRemainingTime;
            showOverlay();
        }
    };

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
        if (!AndroidUtil.isHoneycombOrLater() || mIsNavMenu)
            return;
        int visibility = 0;
        int navbar = 0;

        if (!AndroidDevices.hasCombBar(getApplicationContext()) && AndroidUtil.isJellyBeanOrLater()) {
            visibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            navbar = View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }
        visibility |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        if (dim) {
            navbar |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
            if (!AndroidDevices.hasCombBar(getApplicationContext())) {
                navbar |= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
                if (AndroidUtil.isKitKatOrLater())
                    visibility |= View.SYSTEM_UI_FLAG_IMMERSIVE;
                visibility |= View.SYSTEM_UI_FLAG_FULLSCREEN;
            }
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            visibility |= View.SYSTEM_UI_FLAG_VISIBLE;
        }

        if (AndroidDevices.hasNavBar())
            visibility |= navbar;
        getWindow().getDecorView().setSystemUiVisibility(visibility);
    }

    private void updateOverlayPausePlay() {
        if (mService == null)
            return;
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
            Map<Integer,String> audioTracks = new HashMap<Integer,String>();
            for (MediaStream stream : apiHelper.getMediaSource().getMediaStreams()){
                if (stream.getType() == MediaStreamType.Audio){

                    audioTracks.put(stream.getIndex(), getAudioTrackName(stream));
                }
            }

            mAudioTracksList = GetNameValuePairs(audioTracks);
        }

        if (mSubtitleTracksList == null) {
            ArrayList<NameValuePair> subs = new ArrayList<NameValuePair>();
            subs.add(new NameValuePair("None", "-1"));
            for (MediaStream stream : apiHelper.getMediaSource().getMediaStreams()){
                if (stream.getType() == MediaStreamType.Subtitle){

                    subs.add(new NameValuePair(getSubtitleTrackName(stream), String.valueOf(stream.getIndex())));
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

    private String getAudioTrackName(MediaStream stream) {

        ArrayList<String> attributes = new ArrayList<String>();

        if (stream.getLanguage() != null){
            attributes.add(stream.getLanguage());
        }

        if (stream.getCodec() != null) {
            attributes.add(stream.getCodec());
        }
        if (stream.getProfile() != null) {
            attributes.add(stream.getProfile());
        }

        if (stream.getChannels() != null) {
            attributes.add(stream.getChannels() + " ch");
        }

        String name = tangible.DotNetToJavaStringHelper.join(" - ", attributes.toArray(new String[attributes.size()]));

        if (stream.getIsDefault()) {
            name += " (D)";
        }

        return name;
    }

    private String getSubtitleTrackName(MediaStream stream) {

        ArrayList<String> attributes = new ArrayList<String>();

        if (stream.getLanguage() != null){
            attributes.add(stream.getLanguage());
        }

        if (stream.getCodec() != null) {
            attributes.add(stream.getCodec());
        }

        String name = tangible.DotNetToJavaStringHelper.join(" - ", attributes.toArray(new String[attributes.size()]));

        if (stream.getIsDefault()) {
            name += " (D)";
        }

        if (stream.getIsForced()) {
            name += " (F)";
        }

        return name;
    }

    /**
     *
     */
    private void play() {
        mService.play();
        mSurfaceView.setKeepScreenOn(true);
    }

    /**
     *
     */
    private void pause() {
        mService.pause();
        mSurfaceView.setKeepScreenOn(false);
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

    /**
     * External extras:
     * - position (long) - position of the video to start with (in ms)
     */
    @TargetApi(12)
    @SuppressWarnings({ "unchecked" })
    private void loadMedia() {
        if (mService == null){
            logger.Debug("mService is null. Cannot proceed");
            return;
        }
        mUri = null;
        String title = getResources().getString(R.string.title);
        int openedPosition = -1;
        Uri data;
        String itemTitle = null;
        long intentPosition = -1; // position passed in by intent (ms)
        long mediaLength = 0l;
        Intent intent = getIntent();
        String action = intent.getAction();
        Bundle extras = getIntent().getExtras();

        boolean wasPaused;
        /*
         * If the activity has been paused by pressing the power button, then
         * pressing it again will show the lock screen.
         * But onResume will also be called, even if vlc-android is still in
         * the background.
         * To workaround this, pause playback if the lockscreen is displayed.
         */
        final KeyguardManager km = (KeyguardManager) getApplicationContext().getSystemService(KEYGUARD_SERVICE);
        if (km.inKeyguardRestrictedInputMode())
            wasPaused = true;
        else
            wasPaused = mSettings.getBoolean(PreferencesActivity.VIDEO_PAUSED, false);
        if (wasPaused)
            logger.Debug("Video was previously paused, resuming in paused mode");

        if(TextUtils.equals(action, PLAY_FROM_VIDEOGRID) && extras != null) {

            String mediaSourceJson = getIntent().getExtras().getString("mediaSourceJson");
            MediaSourceInfo mediaSourceInfo = (MediaSourceInfo)jsonSerializer.DeserializeFromString(mediaSourceJson, MediaSourceInfo.class);

            String playbackStartInfoJson = getIntent().getExtras().getString("playbackStartInfoJson");
            PlaybackProgressInfo playbackStartInfo = (PlaybackProgressInfo)jsonSerializer.DeserializeFromString(playbackStartInfoJson, PlaybackProgressInfo.class);

            intentPosition = getIntent().getExtras().getLong("position", 0);

            String location = getIntent().getExtras().getString(PLAY_EXTRA_ITEM_LOCATION);
            location = normalizeLocation(location, mediaSourceInfo, playbackStartInfo.getPlayMethod(), intentPosition * 10000);
            mUri = Uri.parse(location);

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

            if (getIntent().hasExtra(PLAY_EXTRA_SUBTITLES_LOCATION))
                mSubtitleSelectedFiles.add(getIntent().getExtras().getString(PLAY_EXTRA_SUBTITLES_LOCATION));
            openedPosition = getIntent().getExtras().getInt(PLAY_EXTRA_OPENED_POSITION, -1);
        }

        if (intent.hasExtra(PLAY_EXTRA_SUBTITLES_LOCATION))
            mSubtitleSelectedFiles.add(extras.getString(PLAY_EXTRA_SUBTITLES_LOCATION));
        if (intent.hasExtra(PLAY_EXTRA_ITEM_TITLE))
            itemTitle = extras.getString(PLAY_EXTRA_ITEM_TITLE);

        mCanSeek = false;

        if (mUri != null) {

            // Start playback & seek
            if (openedPosition == -1) {
                /* prepare playback */
                mService.stop();
                final MediaWrapper mw = new MediaWrapper(mUri);
                if (wasPaused)
                    mw.addFlags(MediaWrapper.MEDIA_PAUSED);
                if (mHardwareAccelerationError)
                    mw.addFlags(MediaWrapper.MEDIA_NO_HWACCEL);
                mw.removeFlags(MediaWrapper.MEDIA_FORCE_AUDIO);
                mw.addFlags(MediaWrapper.MEDIA_VIDEO);
                mService.addCallback(this);
                mService.load(mw);
                savedIndexPosition = mService.getCurrentMediaPosition();
                if (intentPosition > 0 && mediaLength >= 0l) {
                    if (!mService.getEnableServerSeek()) {
                        seek(intentPosition, mediaLength);
                    }
                }
            } else {
                mService.addCallback(this);
                // AudioService-transitioned playback for item after sleep and resume
                if(!mService.isPlaying())
                    mService.playIndex(savedIndexPosition);
                else
                    onPlaying();
            }

            // Get the title
            if (itemTitle == null)
                title = mUri.getLastPathSegment();
        }
        else {
            logger.Debug("mUri is null cannot proceed with video");
        }
        if (itemTitle != null)
            title = itemTitle;
        mTitle.setText(title);
    }

    @SuppressWarnings("deprecation")
    private int getScreenRotation(){
        WindowManager wm = (WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO /* Android 2.2 has getRotation */) {
            try {
                Method m = display.getClass().getDeclaredMethod("getRotation");
                return (Integer) m.invoke(display);
            } catch (Exception e) {
                return Surface.ROTATION_0;
            }
        } else {
            return display.getOrientation();
        }
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    private int getScreenOrientation(){
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
                    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                            : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                case Surface.ROTATION_270:
                    // SCREEN_ORIENTATION_REVERSE_LANDSCAPE only available since API
                    // Level 9+
                    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                            : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
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
                    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
                            : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                case Surface.ROTATION_270:
                    // SCREEN_ORIENTATION_REVERSE_LANDSCAPE only available since API
                    // Level 9+
                    return (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
                            : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                default:
                    return 0;
            }
        }
    }

    public void showConfirmResumeDialog() {
        if (isFinishing())
            return;
        pause();
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

    public void showAdvancedOptions(View v) {
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

            if (HWDecoderUtil.HAS_SUBTITLES_SURFACE) {
                mSubtitlesSurfaceView.setZOrderMediaOverlay(true);
                mSubtitlesSurfaceView.getHolder().setFormat(PixelFormat.TRANSLUCENT);
            } else
                mSubtitlesSurfaceView.setVisibility(View.GONE);
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
        mIsLoading = true;
        mOverlayProgress.setVisibility(View.INVISIBLE);
        AnimationSet anim = new AnimationSet(true);
        RotateAnimation rotate = new RotateAnimation(0f, 360f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(800);
        rotate.setInterpolator(new DecelerateInterpolator());
        rotate.setRepeatCount(RotateAnimation.INFINITE);
        anim.addAnimation(rotate);
        mLoading.startAnimation(anim);
    }

    /**
     * Stop the video loading animation.
     */
    private void stopLoading() {
        mIsLoading = false;
        mOverlayProgress.setVisibility(View.VISIBLE);
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
        Util.commitPreferences(editor);
    }

    private void updateNavStatus() {
        mIsNavMenu = false;
        mMenuIdx = -1;

        final MediaPlayer.Title[] titles = mService.getTitles();
        if (titles != null) {
            final int currentIdx = mService.getTitleIdx();
            for (int i = 0; i < titles.length; ++i) {
                final MediaPlayer.Title title = titles[i];
                if (title.menu) {
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
        supportInvalidateOptionsMenu();
    }

    private GestureDetector.OnGestureListener mGestureListener = new GestureDetector.OnGestureListener() {
        @Override
        public boolean onDown(MotionEvent e) {
            return false;
        }

        @Override
        public void onShowPress(MotionEvent e) {}

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            return false;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {}

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
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
        logger.Debug("Connected to PlaybackService");
        mHandler.sendEmptyMessage(START_PLAYBACK);
    }

    @Override
    public void onDisconnected() {
        mService = null;
        mHandler.sendEmptyMessage(AUDIO_SERVICE_CONNECTION_FAILED);
    }

    @Override
    public void onNewLayout(IVLCVout vlcVout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {

        logger.Debug("VideoPlayerActivity.OnNewLayout width=%1 height=%2 visibleWidth=%3 visibleHeight=%4 sarNum=%5 sarDen=%6");

        if (width * height == 0)
            return;

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
}