/*
 *  JSCommand.java
 *  Connect SDK
 *
 *  Copyright (c) 2014 LG Electronics.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.connectsdk.cordova;

import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.connectsdk.core.AppInfo;
import com.connectsdk.core.ChannelInfo;
import com.connectsdk.core.JSONSerializable;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.MediaControl.PlayStateListener;
import com.connectsdk.service.capability.MediaControl.PlayStateStatus;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.MediaPlayer.MediaLaunchObject;
import com.connectsdk.service.capability.TVControl;
import com.connectsdk.service.capability.VolumeControl;
import com.connectsdk.service.capability.listeners.ResponseListener;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.WebAppSession;

public class JSCommand {
    ConnectableDeviceWrapper deviceWrapper;
    String commandId;
    boolean subscription;
    CallbackContext callbackContext;
    ServiceSubscription<?> serviceSubscription;

    JSCommand(ConnectableDeviceWrapper deviceWrapper, String commandId, boolean subscription, CallbackContext callbackContext) {
        this.deviceWrapper = deviceWrapper;
        this.commandId = commandId;
        this.subscription = subscription;
        this.callbackContext = callbackContext;
    }

    void checkDone() {
        if (subscription) {
            destroy();
        }
    }

    void destroy() {
        deviceWrapper.cancelCommand(commandId);
    }

    public void cancel() {
        callbackContext = null;

        if (serviceSubscription != null) {
            serviceSubscription.unsubscribe();
        }
    }

    void sendSuccessEvent(Object ... objs) {
        if (callbackContext == null) return;

        JSONArray arr = new JSONArray();
        arr.put("success");

        for (Object o : objs) {
            arr.put(o);
        }

        if (subscription) {
            PluginResult pendingResult = new PluginResult(PluginResult.Status.OK, arr);
            pendingResult.setKeepCallback(true);
            callbackContext.sendPluginResult(pendingResult);
        } else {
            callbackContext.success(arr);
            checkDone();
        }
    }

    public void success() {
        sendSuccessEvent();
    }

    public void success(JSONObject obj) {
        sendSuccessEvent(obj);
    }

    public void success(JSONArray arr) {
        sendSuccessEvent(arr);
    }

    public void success(JSONSerializable obj) {
        JSONObject response = null;

        try {
            response = obj.toJSONObject();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (response != null) {
            success(response);
        }
    }

    public <T extends JSONSerializable> void success(List<T> list) {
        JSONArray response = listToJSON(list);

        success(response);
    }

    public void success(Number obj) {
        sendSuccessEvent(obj);
    }

    public void success(Boolean obj) {
        sendSuccessEvent(obj);
    }

    public void error(String errorMessage) {
        if (callbackContext == null) return;

        JSONObject errorObj = new JSONObject();

        try {
            errorObj.put("message", errorMessage);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        callbackContext.error(errorObj);
        destroy();
    }

    public void error(Exception ex) {
        if (callbackContext == null) return;

        JSONObject errorObj = new JSONObject();

        try {
            errorObj.put("message", ex.getMessage());
            errorObj.put("detail", ex.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        callbackContext.error(errorObj);
        destroy();
    }

    public void error(ServiceCommandError error) {
        if (callbackContext == null) return;

        JSONObject errorObj = new JSONObject();

        try {
            errorObj.put("code", error.getCode());
            errorObj.put("message", error.getMessage());
            errorObj.put("detail", error.getPayload());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        callbackContext.error(errorObj);
        destroy();
    }

    public ResponseListener<Object> getResponseListener() {
        return new ResponseListener<Object>() {

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }

            @Override
            public void onSuccess(Object object) {
                success();
            }
        };
    }

    JSONArray listToJSON(Iterable<? extends JSONSerializable> list) {
        JSONArray arr = new JSONArray();

        try {
            for (JSONSerializable item : list) {
                arr.put(item.toJSONObject());
            }
        } catch (JSONException e) {
        }

        return arr;
    }

    public Launcher.AppLaunchListener getAppLaunchListener() {
        return new Launcher.AppLaunchListener() {
            @Override
            public void onSuccess(LaunchSession object) {
                success(object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }

    public Launcher.AppListListener getAppListListener() {
        return new Launcher.AppListListener() {
            @Override
            public void onSuccess(List<AppInfo> object) {
                success(object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }

    public TVControl.ChannelListListener getChannelListListener() {
        return new TVControl.ChannelListListener() {
            @Override
            public void onSuccess(List<ChannelInfo> object) {
                success(object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }

    public TVControl.ChannelListener getChannelListener() {
        return new TVControl.ChannelListener() {
            @Override
            public void onSuccess(ChannelInfo object) {
                success(object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }

    public VolumeControl.VolumeListener getVolumeListener() {
        return new VolumeControl.VolumeListener() {
            @Override
            public void onSuccess(Float object) {
                success(object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }

    public VolumeControl.MuteListener getMuteListener() {
        return new VolumeControl.MuteListener() {
            @Override
            public void onSuccess(Boolean object) {
                success(object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }

    public MediaPlayer.LaunchListener getMediaLaunchListener() {
        return new MediaPlayer.LaunchListener() {
            @Override
            public void onSuccess(MediaLaunchObject object) {
                // FIXME include media control
                JSONObject launchSessionObj = null;
                JSONObject mediaControlObj = null;
                JSONObject playlistControlObj = null;

                try {
                    launchSessionObj = object.launchSession.toJSONObject();
                    launchSessionObj.put("serviceName", object.launchSession.getService().getServiceName());

                    if (object.mediaControl != null) {
                        MediaControlWrapper mediaControlWrapper = new MediaControlWrapper(deviceWrapper.plugin, object.mediaControl);
                        deviceWrapper.plugin.addObjectWrapper(mediaControlWrapper);
                        mediaControlObj = mediaControlWrapper.toJSONObject();
                    }

                    if (object.playlistControl != null) {
                        PlaylistControlWrapper playlistControlWrapper = new PlaylistControlWrapper(deviceWrapper.plugin, object.playlistControl);
                        deviceWrapper.plugin.addObjectWrapper(playlistControlWrapper);
                        playlistControlObj = playlistControlWrapper.toJSONObject();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                sendSuccessEvent(launchSessionObj, mediaControlObj, playlistControlObj);
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }

    public WebAppSession.LaunchListener getWebAppLaunchListener() {
        return new WebAppSession.LaunchListener() {
            @Override
            public void onSuccess(WebAppSession session) {
                ConnectSDKCordova plugin = deviceWrapper.plugin;
                WebAppSessionWrapper wrapper = new WebAppSessionWrapper(plugin, session);
                deviceWrapper.plugin.addObjectWrapper(wrapper);

                success(wrapper);
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }

    public PlayStateListener getPlayStateListener() {
        return new PlayStateListener() {
            @Override
            public void onSuccess(PlayStateStatus state) {
                String name = "";

                switch (state) {
                    case Buffering:
                        name = "buffering"; break;
                    case Finished:
                        name = "finished"; break;
                    case Idle:
                        name = "idle"; break;
                    case Paused:
                        name = "pause"; break;
                    case Playing:
                        name = "playing"; break;
                    case Unknown:
                        name = "unknown"; break;
                    default:
                        break;
                }

                sendSuccessEvent(name);
            }

            @Override
            public void onError(ServiceCommandError error) {
                error(error);
            }
        };
    }
}
