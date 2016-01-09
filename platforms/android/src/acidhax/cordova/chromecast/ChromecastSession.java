package acidhax.cordova.chromecast;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;

import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.Cast.ApplicationConnectionResult;
import com.google.android.gms.cast.CastDevice;
import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaStatus;
import com.google.android.gms.cast.RemoteMediaPlayer;
import com.google.android.gms.cast.RemoteMediaPlayer.MediaChannelResult;
import com.google.android.gms.cast.RemoteMediaPlayer.OnMetadataUpdatedListener;
import com.google.android.gms.cast.RemoteMediaPlayer.OnStatusUpdatedListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.images.WebImage;

import android.os.Bundle;
import android.support.v7.media.MediaRouter.RouteInfo;

/*
 * All of the Chromecast session specific functions should start here. 
 */
public class ChromecastSession 
	extends Cast.Listener 
	implements 
		GoogleApiClient.ConnectionCallbacks, 
		GoogleApiClient.OnConnectionFailedListener, 
		OnMetadataUpdatedListener, 
		OnStatusUpdatedListener,
		Cast.MessageReceivedCallback {

	private RouteInfo routeInfo = null;
	private volatile GoogleApiClient mApiClient = null;	
	private volatile RemoteMediaPlayer mRemoteMediaPlayer;
	private CordovaInterface cordova = null;
	private CastDevice device = null;
	private ChromecastMediaController chromecastMediaController;
	private ChromecastOnMediaUpdatedListener onMediaUpdatedListener;
	private ChromecastOnSessionUpdatedListener onSessionUpdatedListener;
	
	private volatile String appId;
	private volatile String displayName;
	private volatile List<WebImage> appImages;
	private volatile String sessionId = null;
	private volatile String lastSessionId = null;
	private boolean isConnected = false;
	
	private ChromecastSessionCallback launchCallback;
	private ChromecastSessionCallback joinSessionCallback; 
	
	private boolean joinInsteadOfConnecting = false;
	private HashSet<String> messageNamespaces = new HashSet<String>();
	
	public ChromecastSession(RouteInfo routeInfo, CordovaInterface cordovaInterface, 
			ChromecastOnMediaUpdatedListener onMediaUpdatedListener, ChromecastOnSessionUpdatedListener onSessionUpdatedListener) {
		this.cordova = cordovaInterface;
        this.onMediaUpdatedListener = onMediaUpdatedListener;
        this.onSessionUpdatedListener = onSessionUpdatedListener;
        this.routeInfo = routeInfo;
		this.device = CastDevice.getFromBundle(this.routeInfo.getExtras());
		
		this.mRemoteMediaPlayer = new RemoteMediaPlayer();
		this.mRemoteMediaPlayer.setOnMetadataUpdatedListener(this);
		this.mRemoteMediaPlayer.setOnStatusUpdatedListener(this);
		
		this.chromecastMediaController = new ChromecastMediaController(mRemoteMediaPlayer);
	}

	
	/**
	 * Sets the wheels in motion - connects to the Chromecast and launches the given app
	 * @param appId
	 */
	public void launch(String appId, ChromecastSessionCallback launchCallback) {
		this.appId = appId;
		this.launchCallback = launchCallback;
		this.connectToDevice();
	}
	
	public boolean isConnected() { return this.isConnected; }
	
	/**
	 * Adds a message listener if one does not already exist
	 * @param namespace
	 */
	public void addMessageListener(String namespace) {
		if (messageNamespaces.contains(namespace) == false) {
			try {
				 Cast.CastApi.setMessageReceivedCallbacks(mApiClient, namespace, this);
				 messageNamespaces.add(namespace);
			} catch(Exception e) {
				
			}
		}
	}
	
	/**
	 * Sends a message to a specified namespace
	 * @param namespace
	 * @param message
	 * @param callback
	 */
	public void sendMessage(String namespace, String message, final ChromecastSessionCallback callback) {
		try {
			Cast.CastApi.sendMessage(mApiClient, namespace, message).setResultCallback(new ResultCallback<Status>() {
		        @Override
		        public void onResult(Status result) {
		          if (!result.isSuccess()) {
		        	  callback.onSuccess();
		          } else {
		        	  callback.onError(result.toString());
		          }
		        }
		      });
		} catch(Exception e) {
			callback.onError(e.getMessage());
		}
	}
	
	/**
	 * Join a currently running app with an appId and a session
	 * @param appId
	 * @param sessionId
	 * @param joinSessionCallback
	 */
	public void join (String appId, String sessionId, ChromecastSessionCallback joinSessionCallback) {
		this.appId = appId;
		this.joinSessionCallback = joinSessionCallback;
		this.joinInsteadOfConnecting = true;
		this.lastSessionId = sessionId;
		this.connectToDevice();
	}
	
	/**
	 * Kills a session and it's underlying media player
	 * @param callback
	 */
	public void kill (final ChromecastSessionCallback callback) {
//		this.mRemoteMediaPlayer.stop(mApiClient).setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
//			@Override
//			public void onResult(MediaChannelResult result) {
//				try {
//					Cast.CastApi.stopApplication(mApiClient);
//					mApiClient.disconnect();
//				} catch(Exception e) {
//					
//				}
//				
//				callback.onSuccess();
//			}
//		});
		try {
			Cast.CastApi.stopApplication(mApiClient);
			mApiClient.disconnect();
		} catch(Exception e) {
			
		}
		
		callback.onSuccess();
//		Cast.CastApi.stopApplication(mApiClient);
	}
	
	/**
	 * Leaves the session.
	 * @param callback
	 */
	public void leave (final ChromecastSessionCallback callback) {
		try {
			Cast.CastApi.leaveApplication(mApiClient);
		} catch(Exception e) {
			
		}
		
		callback.onSuccess();
	}	
	
	/**
	 * Loads media over the media API
	 * @param contentId - The URL of the content
	 * @param contentType - The MIME type of the content
	 * @param duration - The length of the video (if known)
	 * @param streamType
	 * @param autoPlay - Whether or not to start the video playing or not
	 * @param currentTime - Where in the video to begin playing from
	 * @param callback
	 * @return
	 */
	public boolean loadMedia(String contentId, String contentType, long duration, String streamType, boolean autoPlay, double currentTime, JSONObject metadata, final ChromecastSessionCallback callback) {
		try {
			MediaInfo mediaInfo = chromecastMediaController.createLoadUrlRequest(contentId, contentType, duration, streamType, metadata);
			
			mRemoteMediaPlayer.load(mApiClient, mediaInfo, autoPlay, (long)(currentTime * 1000))
				.setResultCallback(new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
					@Override
					public void onResult(MediaChannelResult result) {
						if (result.getStatus().isSuccess()) {
							System.out.println("Media loaded successfully");

                            ChromecastSession.this.onMediaUpdatedListener.onMediaLoaded(ChromecastSession.this.createMediaObject());
							callback.onSuccess(ChromecastSession.this.createMediaObject());
						
						} else {
							callback.onError("session_error");
						}
				    }
				});
    	} catch (IllegalStateException e) {
    		e.printStackTrace();
    		System.out.println("Problem occurred with media during loading");
    		callback.onError("session_error");
    		return false;
    	} catch (Exception e) {
    		e.printStackTrace();
    		callback.onError("session_error");
    		System.out.println("Problem opening media during loading");
    		return false;
    	}
    	return true;
	}
	
	/**
	 * Media API - Calls play on the current media
	 * @param callback
	 */
	public void mediaPlay(ChromecastSessionCallback callback) {
		chromecastMediaController.play(mApiClient, callback);
	}
	
	/**
	 * Media API - Calls pause on the current media
	 * @param callback
	 */
	public void mediaPause(ChromecastSessionCallback callback) {
		chromecastMediaController.pause(mApiClient, callback);
	}
	
	/**
	 * Media API - Seeks the current playing media
	 * @param seekPosition - Seconds to seek to
	 * @param resumeState - Resume state once seeking is complete: PLAYBACK_PAUSE or PLAYBACK_START
	 * @param callback
	 */
	public void mediaSeek(long seekPosition, String resumeState, ChromecastSessionCallback callback) {
		chromecastMediaController.seek(seekPosition, resumeState, mApiClient, callback);
	}
	
	/**
	 * Media API - Sets the volume on the current playing media object NOT ON THE CHROMECAST DIRECTLY
	 * @param level
	 * @param callback
	 */
	public void mediaSetVolume(double level, ChromecastSessionCallback callback) {
		chromecastMediaController.setVolume(level, mApiClient, callback);
	}
	
	/**
	 * Media API - Sets the muted state on the current playing media NOT THE CHROMECAST DIRECTLY
	 * @param muted
	 * @param callback
	 */
	public void mediaSetMuted(boolean muted, ChromecastSessionCallback callback) {
		chromecastMediaController.setMuted(muted, mApiClient, callback);
	}
	
	/**
	 * Media API - Stops and unloads the current playing media
	 * @param callback
	 */
	public void mediaStop(ChromecastSessionCallback callback) {
		chromecastMediaController.stop(mApiClient, callback);
	}
	
	
	/**
	 * Sets the receiver volume level
	 * @param volume
	 * @param callback
	 */
	public void setVolume(double volume, ChromecastSessionCallback callback) {
		try {
			Cast.CastApi.setVolume(mApiClient, volume);
			callback.onSuccess();
		} catch (Exception e) {
			e.printStackTrace();
			callback.onError(e.getMessage());
		}
	}
	
	/**
	 * Mutes the receiver
	 * @param muted
	 * @param callback
	 */
	public void setMute(boolean muted, ChromecastSessionCallback callback) {
		try{
			Cast.CastApi.setMute(mApiClient, muted);
			callback.onSuccess();
		} catch (Exception e) {
			e.printStackTrace();
			callback.onError(e.getMessage());
		}
	}
	
	
	/**
	 * Connects to the device with all callbacks and things
	 */
	private void connectToDevice() {
		try {
			Cast.CastOptions.Builder apiOptionsBuilder = Cast.CastOptions.builder(this.device, this);
			
			this.mApiClient = new GoogleApiClient.Builder(this.cordova.getActivity().getApplicationContext())
				.addApi(Cast.API, apiOptionsBuilder.build())
		        .addConnectionCallbacks(this)
		        .addOnConnectionFailedListener(this)
		        .build();
			
			this.mApiClient.connect();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Launches the application and gets a new session
	 */
	private void launchApplication() {
		Cast.CastApi.launchApplication(mApiClient, this.appId, false)
			.setResultCallback(launchApplicationResultCallback);
	}
	
	/**
	 * Attemps to join an already running session
	 */
	private void joinApplication() {
		Cast.CastApi.joinApplication(this.mApiClient, this.appId, this.lastSessionId)
			.setResultCallback(joinApplicationResultCallback);
	}
	
	/**
	 * Connects to the remote media player on the receiver
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	private void connectRemoteMediaPlayer() throws IllegalStateException, IOException {
		Cast.CastApi.setMessageReceivedCallbacks(mApiClient, mRemoteMediaPlayer.getNamespace(), mRemoteMediaPlayer);
		mRemoteMediaPlayer.requestStatus(mApiClient)
		.setResultCallback(connectRemoteMediaPlayerCallback);
	}
	
	
	/**
	 * launchApplication callback
	 */
	private ResultCallback<Cast.ApplicationConnectionResult> launchApplicationResultCallback = new ResultCallback<Cast.ApplicationConnectionResult>() {
		@Override
		public void onResult(ApplicationConnectionResult result) {
			
			ApplicationMetadata metadata = result.getApplicationMetadata();
			ChromecastSession.this.sessionId = result.getSessionId();
			ChromecastSession.this.displayName = metadata.getName();
			ChromecastSession.this.appImages = metadata.getImages();
		
			Status status = result.getStatus();
			
			if (status.isSuccess()) {
				try {
					ChromecastSession.this.launchCallback.onSuccess(ChromecastSession.this);
					connectRemoteMediaPlayer();
					ChromecastSession.this.isConnected = true;
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				ChromecastSession.this.isConnected = false;
			}
		}
	};
	
	/**
	 * joinApplication callback
	 */
	private ResultCallback<Cast.ApplicationConnectionResult> joinApplicationResultCallback = new ResultCallback<Cast.ApplicationConnectionResult>() {
		@Override
		public void onResult(ApplicationConnectionResult result) {
		
			Status status = result.getStatus();
			
			if (status.isSuccess()) {
				try {
					ApplicationMetadata metadata = result.getApplicationMetadata();
					ChromecastSession.this.sessionId = result.getSessionId();
					ChromecastSession.this.displayName = metadata.getName();
					ChromecastSession.this.appImages = metadata.getImages();
					
					ChromecastSession.this.joinSessionCallback.onSuccess(ChromecastSession.this);
					connectRemoteMediaPlayer();
					ChromecastSession.this.isConnected = true;
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				ChromecastSession.this.joinSessionCallback.onError(status.toString());
				ChromecastSession.this.isConnected = false;
			}
		}
	};
	
	/**
	 * connectRemoteMediaPlayer callback
	 */
	private ResultCallback<RemoteMediaPlayer.MediaChannelResult> connectRemoteMediaPlayerCallback = new ResultCallback<RemoteMediaPlayer.MediaChannelResult>() {
		@Override
		public void onResult(MediaChannelResult result) {
			if (result.getStatus().isSuccess()) {
                ChromecastSession.this.onMediaUpdatedListener.onMediaUpdated(true, ChromecastSession.this.createMediaObject());
				/*ChromecastSession.this.onMediaUpdatedListener.onMediaLoaded(ChromecastSession.this.createMediaObject());*/
			} else {
				System.out.println("Failed to request status.");
			}
		}
	};
	
	/**
	 * Creates a JSON representation of this session
	 * @return
	 */
	public JSONObject createSessionObject() {		
		JSONObject out = new JSONObject();
		try {
			out.put("appId", this.appId);
			out.put("media", createMediaObject());
		
			if (this.appImages != null) {
				JSONArray appImages = new JSONArray();
				for(WebImage o : this.appImages) {
					appImages.put(o.toString());
				}
			}
			
			out.put("appImages", appImages);
			out.put("sessionId", this.sessionId);
			out.put("displayName", this.displayName);
			
			JSONObject receiver = new JSONObject();
			receiver.put("friendlyName", this.device.getFriendlyName());
			receiver.put("label", this.device.getDeviceId());
			
			JSONObject volume = new JSONObject();
			try {
				volume.put("level", Cast.CastApi.getVolume(mApiClient));
				volume.put("muted", Cast.CastApi.isMute(mApiClient));
			} catch(Exception e) {
				
			}
			
			receiver.put("volume", volume);
			
			out.put("receiver", receiver);
			
		} catch(JSONException e) {
			e.printStackTrace();
		}
		
		return out;
	}
	
	/**
	 * Creates a JSON representation of the current playing media
	 * @return
	 */
	private JSONObject createMediaObject() {
		JSONObject out = new JSONObject();
		JSONObject objInfo = new JSONObject();
		
		MediaStatus mediaStatus = mRemoteMediaPlayer.getMediaStatus();
		if (mediaStatus == null) {
			return out;
		}
		
		MediaInfo mediaInfo = mediaStatus.getMediaInfo();
		try {
			out.put("media", objInfo);
			out.put("mediaSessionId", 1);
			out.put("sessionId", this.sessionId);
			out.put("currentTime", mediaStatus.getStreamPosition() / 1000.0);
			out.put("playbackRate", mediaStatus.getPlaybackRate());
			out.put("customData", mediaStatus.getCustomData());
			
			switch(mediaStatus.getPlayerState()) {
				case MediaStatus.PLAYER_STATE_BUFFERING:
					out.put("playerState", "BUFFERING"); break;
				case MediaStatus.PLAYER_STATE_IDLE:
					out.put("playerState", "IDLE"); break;
				case MediaStatus.PLAYER_STATE_PAUSED:
					out.put("playerState", "PAUSED"); break;
				case MediaStatus.PLAYER_STATE_PLAYING:
					out.put("playerState", "PLAYING"); break;
				case MediaStatus.PLAYER_STATE_UNKNOWN:
					out.put("playerState", "UNKNOWN"); break;
			}
			
			switch(mediaStatus.getIdleReason()) {
				case MediaStatus.IDLE_REASON_CANCELED:
					out.put("idleReason", "canceled"); break;
				case MediaStatus.IDLE_REASON_ERROR:
					out.put("idleReason", "error"); break;
				case MediaStatus.IDLE_REASON_FINISHED:
					out.put("idleReason", "finished"); break;
				case MediaStatus.IDLE_REASON_INTERRUPTED:
					out.put("idleReason", "iterrupted"); break;
				case MediaStatus.IDLE_REASON_NONE:
					out.put("idleReason", "none"); break;
			}
			
			JSONObject volume = new JSONObject();
			volume.put("level", mediaStatus.getStreamVolume());
			volume.put("muted", mediaStatus.isMute());
			
			out.put("volume", volume);
			
			try {
				objInfo.put("duration", mediaInfo.getStreamDuration() / 1000.0);
				switch(mediaInfo.getStreamType()) {
					case MediaInfo.STREAM_TYPE_BUFFERED:
						objInfo.put("streamType", "buffered"); break;
					case MediaInfo.STREAM_TYPE_LIVE:
						objInfo.put("streamType", "live"); break;
					case MediaInfo.STREAM_TYPE_NONE:
						objInfo.put("streamType", "other"); break;
				}
			} catch (Exception e) {
				
			}
			
		} catch(JSONException e) {
			
		}
		
		return out;
	}
	
	

	/* GoogleApiClient.ConnectionCallbacks implementation
	 * Called when we successfully connect to the API
	 * (non-Javadoc)
	 * @see com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks#onConnected(android.os.Bundle)
	 */
	@Override
	public void onConnected(Bundle connectionHint) {
		if (this.joinInsteadOfConnecting) {
			this.joinApplication();
		} else {
			this.launchApplication();
		}
	}
	
	
	/* GoogleApiClient.ConnectionCallbacks implementation
	 * (non-Javadoc)
	 * @see com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks#onConnectionSuspended(android.os.Bundle)
	 */
	@Override
	public void onConnectionSuspended(int cause) {
		if (this.onSessionUpdatedListener != null) {
			this.isConnected = false;
			this.onSessionUpdatedListener.onSessionUpdated(false, this.createSessionObject());
		}
	}
	
	/*
	 * GoogleApiClient.OnConnectionFailedListener implementation
	 * When Google API fails to connect.
	 * (non-Javadoc)
	 * @see com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener#onConnectionFailed(com.google.android.gms.common.ConnectionResult)
	 */
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		if (this.launchCallback != null) {
			this.isConnected = false;
			this.launchCallback.onError("channel_error");
		}
	}
	
	/**
	 * Cast.Listener implementation
	 * When Chromecast application status changed
	 */
	@Override
	public void onApplicationStatusChanged() {
		if (this.onSessionUpdatedListener != null) {
			ChromecastSession.this.isConnected = true;
			this.onSessionUpdatedListener.onSessionUpdated(true, createSessionObject());
		}
	}
	
	/**
	 * Cast.Listener implementation
	 * When the volume is changed on the Chromecast
	 */
	@Override
	public void onVolumeChanged() {
		if (this.onSessionUpdatedListener != null) {
			this.onSessionUpdatedListener.onSessionUpdated(true, createSessionObject());
		}
	}
	
	/**
	 * Cast.Listener implementation
	 * When the application is disconnected
	 */
	@Override
	public void onApplicationDisconnected(int errorCode) {
		if (this.onSessionUpdatedListener != null) {
			this.isConnected = false;
			this.onSessionUpdatedListener.onSessionUpdated(false, this.createSessionObject());
		}
	}


	@Override
	public void onMetadataUpdated() {
		if (this.onMediaUpdatedListener != null) {
			this.onMediaUpdatedListener.onMediaUpdated(true, this.createMediaObject());
		}
	}


	@Override
	public void onStatusUpdated() {
		if (this.onMediaUpdatedListener != null) {
			this.onMediaUpdatedListener.onMediaUpdated(true, this.createMediaObject());
		}
	}
	
	
	/// GETTERS
	public String getSessionId() {
		return this.sessionId;
	}


	@Override
	public void onMessageReceived(CastDevice castDevice, String namespace, String message) {
		if (this.onSessionUpdatedListener != null) {
			this.onSessionUpdatedListener.onMessage(this, namespace, message);
		}
	}
}
