package acidhax.cordova.chromecast;

import android.annotation.TargetApi;
import android.os.Build;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

import com.google.android.gms.cast.CastMediaControlIntent;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter.RouteInfo;
import android.util.Log;
import android.widget.ArrayAdapter;

public class Chromecast extends CordovaPlugin implements ChromecastOnMediaUpdatedListener, ChromecastOnSessionUpdatedListener {
	
	private static final String SETTINGS_NAME= "CordovaChromecastSettings";
	
    private MediaRouter mMediaRouter;
    private MediaRouteSelector mMediaRouteSelector;
    private volatile ChromecastMediaRouterCallback mMediaRouterCallback = new ChromecastMediaRouterCallback();
    private String appId;
    
    private boolean autoConnect = false;
    private String lastSessionId = null;
    private String lastAppId = null;
    
    private SharedPreferences settings;
   
    
    private volatile ChromecastSession currentSession;
    
    private void log(String s) {
    	sendJavascript("console.log('" + s + "');");
    }

    
    public void initialize(final CordovaInterface cordova, CordovaWebView webView) {
    	super.initialize(cordova, webView);
    	
    	// Restore preferences
        this.settings = this.cordova.getActivity().getSharedPreferences(SETTINGS_NAME, 0);
        this.lastSessionId = settings.getString("lastSessionId", "");
        this.lastAppId = settings.getString("lastAppId", "");
    }
    
    public void onDestroy() {
    	super.onDestroy();
    	
    	if (this.currentSession != null) {
//    		this.currentSession.kill(new ChromecastSessionCallback() {
//				void onSuccess(Object object) {	}
//				void onError(String reason) {}
//    		});
    	}
    }
    
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext cbContext) throws JSONException {
    	try {
    		Method[] list = this.getClass().getMethods();
    		Method methodToExecute = null;
    		for (Method method : list) {
    			if (method.getName().equals(action)) {
    				Type[] types = method.getGenericParameterTypes();
    				if (args.length() + 1 == types.length) { // +1 is the cbContext
    					boolean isValid = true;
        				for (int i = 0; i < args.length(); i++) {
            				Class arg = args.get(i).getClass();
            				if (types[i] == arg) {
            					isValid = true;
            				} else {
            					isValid = false;
            					break;
            				}
        				}
        				if (isValid) {
            				methodToExecute = method;
            				break;
        				}
    				}
    			}
    		}
    		if (methodToExecute != null) {
    			Type[] types = methodToExecute.getGenericParameterTypes();
    			Object[] variableArgs = new Object[types.length];
    			for (int i = 0; i < args.length(); i++) {
    				variableArgs[i] = args.get(i);
    			}
    			variableArgs[variableArgs.length-1] = cbContext;
        		Class<?> r = methodToExecute.getReturnType();
        		if (r == boolean.class) {
            		return (Boolean) methodToExecute.invoke(this, variableArgs);
        		} else {
        			methodToExecute.invoke(this, variableArgs);
        			return true;
        		}
    		} else {
    			return false;
    		}
		} catch (IllegalAccessException e) {
            e.printStackTrace();
            return false;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    private void setLastSessionId(String sessionId) {
    	this.lastSessionId = sessionId;
    	this.settings.edit().putString("lastSessionId", sessionId).apply();
    }
    
   
   /**
    * Do everything you need to for "setup" - calling back sets the isAvailable and lets every function on the
    * javascript side actually do stuff.
    * @param  callbackContext
    */
    public boolean setup (CallbackContext callbackContext) {
        callbackContext.success();
        
        return true;
    }

    /**
     * Initialize all of the MediaRouter stuff with the AppId
     * For now, ignore the autoJoinPolicy and defaultActionPolicy; those will come later
     * @param  appId               The appId we're going to use for ALL session requests
     * @param  autoJoinPolicy      tab_and_origin_scoped | origin_scoped | page_scoped
     * @param  defaultActionPolicy create_session | cast_this_tab
     * @param  callbackContext
     */
    public boolean initialize (final String appId, String autoJoinPolicy, String defaultActionPolicy, final CallbackContext callbackContext) {
        final Activity activity = cordova.getActivity();
        final Chromecast that = this;
        this.appId = appId;
        
        log("initialize " + autoJoinPolicy + " " + appId + " " + this.lastAppId);
        if (autoJoinPolicy.equals("origin_scoped") && appId.equals(this.lastAppId)) {
        	log("lastAppId " + lastAppId);
        	autoConnect = true;
        } else if (autoJoinPolicy.equals("origin_scoped")) {
        	log("setting lastAppId " + lastAppId);
        	this.settings.edit().putString("lastAppId", appId).apply();
        }
        
        activity.runOnUiThread(new Runnable() {
            public void run() {
                mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
                mMediaRouteSelector = new MediaRouteSelector.Builder()
                .addControlCategory(CastMediaControlIntent.categoryForCast(appId))
                .build();
                mMediaRouterCallback.registerCallbacks(that);
                mMediaRouter.addCallback(mMediaRouteSelector, mMediaRouterCallback, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
                callbackContext.success();
                
                Chromecast.this.checkReceiverAvailable();
                Chromecast.this.emitAllRoutes(null);
            }
        });
       
        return true;
    }

    /**
     * Request the session for the previously sent appId
     * THIS IS WHAT LAUNCHES THE CHROMECAST PICKER
     * NOTE: Make a request session that is automatic - it'll do most of this code - refactor will be required
     * @param  callbackContext
     */
    public boolean requestSession (final CallbackContext callbackContext) {
        if (this.currentSession != null) {
            callbackContext.success(this.currentSession.createSessionObject());
            return true;
        }

        this.setLastSessionId("");

        final Activity activity = cordova.getActivity();
        activity.runOnUiThread(new Runnable() {
            public void run() {
                mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
                final List<RouteInfo> routeList = mMediaRouter.getRoutes();

                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle("Choose a Chromecast");
                //CharSequence[] seq = new CharSequence[routeList.size() -1];
                ArrayList<String> seq_tmp1 = new ArrayList<String>();

                final ArrayList<Integer> seq_tmp_cnt_final = new ArrayList<Integer>();

                for (int n = 1; n < routeList.size(); n++) {
                    RouteInfo route = routeList.get(n);
                    if (!route.getName().equals("Phone") && route.getId().indexOf("Cast") > -1) {
                            seq_tmp1.add(route.getName());
                            seq_tmp_cnt_final.add(n);
                            //seq[n-1] = route.getName();
                    }
                }

                CharSequence[] seq;
                seq = seq_tmp1.toArray(new CharSequence[seq_tmp1.size()]);

                builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        callbackContext.error("cancel");
                    }
                });

                builder.setItems(seq, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        which = seq_tmp_cnt_final.get(which);
                        RouteInfo selectedRoute = routeList.get(which);
                        //RouteInfo selectedRoute = routeList.get(which + 1);
                        Chromecast.this.createSession(selectedRoute, callbackContext);
                    }
                });

                builder.show();

            }
        });

        return true;
    }

    
    /**
     * Selects a route by its id
     * @param routeId
     * @param callbackContext
     * @return
     */
    public boolean selectRoute (final String routeId, final CallbackContext callbackContext) {
    	if (this.currentSession != null) {
    		callbackContext.success(this.currentSession.createSessionObject());
    		return true;
    	}
    	
    	this.setLastSessionId("");
    	
    	final Activity activity = cordova.getActivity();
        activity.runOnUiThread(new Runnable() {
            public void run() {
                mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
                final List<RouteInfo> routeList = mMediaRouter.getRoutes();
                
                for (RouteInfo route : routeList) {
                	if (route.getId().equals(routeId)) {
                		Chromecast.this.createSession(route, callbackContext);
                		return;
                	}
                }
                
                callbackContext.error("No route found");
                
            }
        });
        
        return true;
    }

	/**
	 * Helper for the creating of a session! The user-selected RouteInfo needs to be passed to a new ChromecastSession 
	 * @param routeInfo
	 * @param callbackContext
	 */
    private void createSession(RouteInfo routeInfo, final CallbackContext callbackContext) {
    	this.currentSession = new ChromecastSession(routeInfo, this.cordova, this, this);
        
        // Launch the app.
        this.currentSession.launch(this.appId, new ChromecastSessionCallback() {

			@Override
			void onSuccess(Object object) {
				ChromecastSession session = (ChromecastSession) object;
				if (object == null) {
					onError("unknown");
				} else if (session == Chromecast.this.currentSession){
					Chromecast.this.setLastSessionId(Chromecast.this.currentSession.getSessionId());
					
					if (callbackContext != null) {
						callbackContext.success(session.createSessionObject());
					} else {
						sendJavascript("chrome.cast._.sessionJoined(" + Chromecast.this.currentSession.createSessionObject().toString() + ");");
					}
				}
			}

			@Override
			void onError(String reason) {
				if (reason != null) {
					Chromecast.this.log("createSession onError " + reason);
					if (callbackContext != null) {
						callbackContext.error(reason);
					}
				} else {
					if (callbackContext != null) {
						callbackContext.error("unknown");
					}
				}
			}
        	
        });
    }
    
    private void joinSession(RouteInfo routeInfo) {
    	ChromecastSession sessionJoinAttempt = new ChromecastSession(routeInfo, this.cordova, this, this);
    	sessionJoinAttempt.join(this.appId, this.lastSessionId, new ChromecastSessionCallback() {

			@Override
			void onSuccess(Object object) {
				if (Chromecast.this.currentSession == null) {
					try {
						Chromecast.this.currentSession = (ChromecastSession) object;
						Chromecast.this.setLastSessionId(Chromecast.this.currentSession.getSessionId());
						sendJavascript("chrome.cast._.sessionJoined(" + Chromecast.this.currentSession.createSessionObject().toString() + ");");
					} catch (Exception e) {
						log("wut.... " + e.getMessage() + e.getStackTrace());
					}
				}
			}

			@Override
			void onError(String reason) {
				log("sessionJoinAttempt error " +reason);
			}
    		
    	});
    }
    
    /**
     * Set the volume level on the receiver - this is a Chromecast volume, not a Media volume
     * @param  newLevel
     */
    public boolean setReceiverVolumeLevel (Double newLevel, CallbackContext callbackContext) {
    	if (this.currentSession != null) {
        	this.currentSession.setVolume(newLevel, genericCallback(callbackContext));
        } else {
        	callbackContext.error("session_error");
        }
        return true;
    }
    
    public boolean setReceiverVolumeLevel (Integer newLevel, CallbackContext callbackContext) {
    	return this.setReceiverVolumeLevel(newLevel.doubleValue(), callbackContext);
    }

    /**
     * Sets the muted boolean on the receiver - this is a Chromecast mute, not a Media mute
     * @param  muted           
     * @param  callbackContext 
     */
    public boolean setReceiverMuted (Boolean muted, CallbackContext callbackContext) {
        if (this.currentSession != null) {
        	this.currentSession.setMute(muted, genericCallback(callbackContext));
        } else {
        	callbackContext.error("session_error");
        }
        return true;
    }

    /**
     * Stop the session! Disconnect! All of that jazz!
     * @param  callbackContext [description]
     */
    public boolean stopSession(CallbackContext callbackContext) {
        callbackContext.error("not_implemented");
        return true;
    }

    /**
     * Send a custom message to the receiver - we don't need this just yet... it was just simple to implement on the js side
     * @param  namespace       
     * @param  message         
     * @param  callbackContext
     */
    public boolean sendMessage (String namespace, String message, final CallbackContext callbackContext) {
        if (this.currentSession != null) {
        	this.currentSession.sendMessage(namespace, message, new ChromecastSessionCallback() {

				@Override
				void onSuccess(Object object) {
					callbackContext.success();
				}

				@Override
				void onError(String reason) {
					callbackContext.error(reason);					
				}
        	});
        }
        return true;
    }
    
    
    /**
     * Adds a listener to a specific namespace
     * @param namespace
     * @param callbackContext
     * @return
     */
    public boolean addMessageListener(String namespace, CallbackContext callbackContext) {
    	if (this.currentSession != null) {
    		this.currentSession.addMessageListener(namespace);
    		callbackContext.success();
    	}
    	return true;
    }

    /**
     * Loads some media on the Chromecast using the media APIs
     * @param  contentId               The URL of the media item
     * @param  contentType             MIME type of the content
     * @param  duration                Duration of the content
     * @param  streamType              buffered | live | other
     * @param  loadRequest.autoPlay    Whether or not to automatically start playing the media
     * @param  loadRequest.currentTime Where to begin playing from
     * @param  callbackContext 
     */
    public boolean loadMedia (String contentId, String contentType, Integer duration, String streamType, Boolean autoPlay, Double currentTime, JSONObject metadata, final CallbackContext callbackContext) {
        
    	if (this.currentSession != null) {
    		return this.currentSession.loadMedia(contentId, contentType, duration, streamType, autoPlay, currentTime, metadata,
    				new ChromecastSessionCallback() {

						@Override
						void onSuccess(Object object) {
							if (object == null) {
								onError("unknown");
							} else {
								callbackContext.success((JSONObject) object);
							}
						}

						@Override
						void onError(String reason) {
							callbackContext.error(reason);
						}
    			
    		});
    	} else {
    		callbackContext.error("session_error");
    		return false;
    	}
    }
    public boolean loadMedia (String contentId, String contentType, Integer duration, String streamType, Boolean autoPlay, Integer currentTime, JSONObject metadata, final CallbackContext callbackContext) {
    	return this.loadMedia (contentId, contentType, duration, streamType, autoPlay, new Double(currentTime.doubleValue()), metadata, callbackContext);
    }
    
    /**
     * Play on the current media in the current session
     * @param callbackContext
     * @return
     */
    public boolean mediaPlay(CallbackContext callbackContext) {
    	if (currentSession != null) {
    		currentSession.mediaPlay(genericCallback(callbackContext));
    	} else {
    		callbackContext.error("session_error");
    	}
    	return true;
    }
    
    /**
     * Pause on the current media in the current session
     * @param callbackContext
     * @return
     */
    public boolean mediaPause(CallbackContext callbackContext) {
    	if (currentSession != null) {
    		currentSession.mediaPause(genericCallback(callbackContext));
    	} else {
    		callbackContext.error("session_error");
    	}
    	return true;
    }
    
    
    /**
     * Seeks the current media in the current session
     * @param seekTime
     * @param resumeState
     * @param callbackContext
     * @return
     */
    public boolean mediaSeek(Integer seekTime, String resumeState, CallbackContext callbackContext) {
    	if (currentSession != null) {
    		currentSession.mediaSeek(seekTime.longValue() * 1000, resumeState, genericCallback(callbackContext));
    	} else {
    		callbackContext.error("session_error");
    	}
    	return true;
    }
    
    
    /**
     * Set the volume on the media
     * @param level
     * @param callbackContext
     * @return
     */
    public boolean setMediaVolume(Double level, CallbackContext callbackContext) {
    	if (currentSession != null) {
    		currentSession.mediaSetVolume(level, genericCallback(callbackContext));
    	} else {
    		callbackContext.error("session_error");
    	}
    	
    	return true;
    }
    
    /**
     * Set the muted on the media
     * @param muted
     * @param callbackContext
     * @return
     */
    public boolean setMediaMuted(Boolean muted, CallbackContext callbackContext) {
    	if (currentSession != null) {
    		currentSession.mediaSetMuted(muted, genericCallback(callbackContext));
    	} else {
    		callbackContext.error("session_error");
    	}
    	
    	return true;
    }
    
    /**
     * Stops the current media!
     * @param callbackContext
     * @return
     */
    public boolean mediaStop(CallbackContext callbackContext) {
    	if (currentSession != null) {
    		currentSession.mediaStop(genericCallback(callbackContext));
    	} else {
    		callbackContext.error("session_error");
    	}
    	
    	return true;
    }
    
    /**
     * Stops the session
     * @param callbackContext
     * @return
     */
    public boolean sessionStop (CallbackContext callbackContext) {
    	if (this.currentSession != null) {
    		this.currentSession.kill(genericCallback(callbackContext));
    		this.currentSession = null;
    		this.setLastSessionId("");
    	} else {
    		callbackContext.success();
    	}
    	
    	return true;
    }

    /**
     * Stops the session
     * @param callbackContext
     * @return
     */
    public boolean sessionLeave (CallbackContext callbackContext) {
        if (this.currentSession != null) {
            this.currentSession.leave(genericCallback(callbackContext));
            this.currentSession = null;
            this.setLastSessionId("");
        } else {
            callbackContext.success();
        }
        
        return true;
    }

    public boolean emitAllRoutes(CallbackContext callbackContext) {
    	final Activity activity = cordova.getActivity();
    	
        activity.runOnUiThread(new Runnable() {
            public void run() {
                mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
                List<RouteInfo> routeList = mMediaRouter.getRoutes();
                
                for (RouteInfo route : routeList) {
                	if (!route.getName().equals("Phone") && route.getId().indexOf("Cast") > -1) {
	        			sendJavascript("chrome.cast._.routeAdded(" + routeToJSON(route) + ")");
	        		}
                }
            }
        });
        
        if (callbackContext != null) {
        	callbackContext.success();
        }
        
        return true;
    }
    
    /**
     * Checks to see how many receivers are available - emits the receiver status down to Javascript
     */
    private void checkReceiverAvailable() {
    	final Activity activity = cordova.getActivity();
    	
        activity.runOnUiThread(new Runnable() {
            public void run() {
                mMediaRouter = MediaRouter.getInstance(activity.getApplicationContext());
                List<RouteInfo> routeList = mMediaRouter.getRoutes();
                boolean available = false;
                
                for (RouteInfo route: routeList) {
                	if (!route.getName().equals("Phone") && route.getId().indexOf("Cast") > -1) {
                		available = true;
                		break;
                	}
                }
                if (available || (Chromecast.this.currentSession != null && Chromecast.this.currentSession.isConnected())) {
                	sendJavascript("chrome.cast._.receiverAvailable()");
                } else {
                	sendJavascript("chrome.cast._.receiverUnavailable()");
                }
            }
        });
    }
    
    /**
     * Creates a ChromecastSessionCallback that's generic for a CallbackContext 
     * @param callbackContext
     * @return
     */
    private ChromecastSessionCallback genericCallback (final CallbackContext callbackContext) {
    	return new ChromecastSessionCallback() {

			@Override
			public void onSuccess(Object object) {
				callbackContext.success();
			}

			@Override
			public void onError(String reason) {
				callbackContext.error(reason);
			}
    		
    	};
    };
    
    /**
     * Called when a route is discovered
     * @param router
     * @param route
     */
    protected void onRouteAdded(MediaRouter router, final RouteInfo route) {
    	if (this.autoConnect && this.currentSession == null && !route.getName().equals("Phone")) {
    		log("Attempting to join route " + route.getName());
    		this.joinSession(route);
    	} else {
    		log("For some reason, not attempting to join route " + route.getName() + ", " + this.currentSession + ", " + this.autoConnect);
    	}
    	if (!route.getName().equals("Phone") && route.getId().indexOf("Cast") > -1) {
			sendJavascript("chrome.cast._.routeAdded(" + routeToJSON(route) + ")");
		}
    	this.checkReceiverAvailable();
    }

    /**
     * Called when a discovered route is lost
     * @param router
     * @param route
     */
	protected void onRouteRemoved(MediaRouter router, RouteInfo route) {
		this.checkReceiverAvailable();
		if (!route.getName().equals("Phone") && route.getId().indexOf("Cast") > -1) {
			sendJavascript("chrome.cast._.routeRemoved(" + routeToJSON(route) + ")");
		}
	}

	/**
	 * Called when a route is selected through the MediaRouter
	 * @param router
	 * @param route
	 */
	protected void onRouteSelected(MediaRouter router, RouteInfo route) {	
		this.createSession(route, null);
	}

	/**
	 * Called when a route is unselected through the MediaRouter
	 * @param router
	 * @param route
	 */
	protected void onRouteUnselected(MediaRouter router, RouteInfo route) {}
	
	/**
	 * Simple helper to convert a route to JSON for passing down to the javascript side
	 * @param route
	 * @return
	 */
	private JSONObject routeToJSON(RouteInfo route) {
		JSONObject obj = new JSONObject();
		
		try {
			obj.put("name", route.getName());
			obj.put("id", route.getId());
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return obj;
	}

    @Override
    public void onMediaUpdated(boolean isAlive, JSONObject media) {
        if (isAlive) {
            sendJavascript("chrome.cast._.mediaUpdated(true, " + media.toString() +");");
        } else {
            sendJavascript("chrome.cast._.mediaUpdated(false, " + media.toString() +");");
        }
    }

	@Override
	public void onSessionUpdated(boolean isAlive, JSONObject session) {
		if (isAlive) {
			sendJavascript("chrome.cast._.sessionUpdated(true, " + session.toString() + ");");
		} else {
			log("SESSION DESTROYYYY");
			sendJavascript("chrome.cast._.sessionUpdated(false, " + session.toString() + ");");
			this.currentSession = null;
		}
	}

	@Override
	public void onMediaLoaded(JSONObject media) {
		sendJavascript("chrome.cast._.mediaLoaded(true, " + media.toString() +");");
	}

	@Override
	public void onMessage(ChromecastSession session, String namespace, String message) {
		sendJavascript("chrome.cast._.onMessage('" + session.getSessionId() +"', '" + namespace + "', '" + message  + "')");
	}

    //Change all @deprecated this.webView.sendJavascript(String) to this local function sendJavascript(String)
    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void sendJavascript(final String javascript) {

        webView.post(new Runnable() {
            @Override
            public void run() {
                // See: https://github.com/GoogleChrome/chromium-webview-samples/blob/master/jsinterface-example/app/src/main/java/jsinterfacesample/android/chrome/google/com/jsinterface_example/MainFragment.java
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    webView.evaluateJavascript(javascript, null);
                } else {
                    webView.loadUrl("javascript:" + javascript);
                }
            }
        });
    }

}
