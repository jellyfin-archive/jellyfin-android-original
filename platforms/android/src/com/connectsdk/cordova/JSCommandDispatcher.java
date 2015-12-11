/*
 *  JSCommandDispatcher.java
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.connectsdk.core.AppInfo;
import com.connectsdk.core.ChannelInfo;
import com.connectsdk.core.ExternalInputInfo;
import com.connectsdk.core.MediaInfo;
import com.connectsdk.core.SubtitleInfo;
import com.connectsdk.core.TextInputStatusInfo;
import com.connectsdk.device.ConnectableDevice;
import com.connectsdk.service.DeviceService;
import com.connectsdk.service.WebOSTVService;
import com.connectsdk.service.capability.ExternalInputControl;
import com.connectsdk.service.capability.ExternalInputControl.ExternalInputListListener;
import com.connectsdk.service.capability.Launcher;
import com.connectsdk.service.capability.KeyControl.KeyCode;
import com.connectsdk.service.capability.MediaControl;
import com.connectsdk.service.capability.MediaControl.DurationListener;
import com.connectsdk.service.capability.MediaControl.PositionListener;
import com.connectsdk.service.capability.MediaPlayer;
import com.connectsdk.service.capability.PlaylistControl;
import com.connectsdk.service.capability.TextInputControl;
import com.connectsdk.service.capability.WebAppLauncher;
import com.connectsdk.service.command.ServiceCommandError;
import com.connectsdk.service.command.ServiceSubscription;
import com.connectsdk.service.sessions.LaunchSession;
import com.connectsdk.service.sessions.WebAppSession;
import com.connectsdk.service.sessions.WebAppSessionListener;

public class JSCommandDispatcher {
    ConnectSDKCordova plugin;
    ConnectableDeviceWrapper deviceWrapper;
    ConnectableDevice device;
    HashMap<String, Method> methodCache = new HashMap<String, Method>();

    // Annotation used to mark handlers
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface CommandMethod {
    }

    class DispatcherException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public DispatcherException(String message) {
            super(message);
        }
    }

    JSCommandDispatcher(ConnectableDeviceWrapper deviceWrapper) {
        this.deviceWrapper = deviceWrapper;
        this.device = deviceWrapper.device;
        this.plugin = deviceWrapper.plugin;
    }

    Method getMethod(String interfaceName, String methodName) {
        String name = interfaceName + "_" + methodName;

        Method method = methodCache.get(name);

        if (method == null) {
            try {
                method = this.getClass().getMethod(name, JSCommand.class, JSONObject.class);

                if (method.isAnnotationPresent(CommandMethod.class)) {
                    methodCache.put(name, method);
                } else {
                    Log.d("ConnectSDKCordova", "attempted to access method " + method.getName() + " but method is not a command handler");
                    method = null;
                }
            } catch (NoSuchMethodException e) {
            }
        }

        return method;
    }

    public void dispatchCommand(String interfaceName, String methodName, JSCommand command, JSONObject args, boolean subscribe) {
        Method method = getMethod(interfaceName, methodName);

        if (method != null) {
            try {
                Object returnValue = method.invoke(this, command, args);

                if (returnValue != null && returnValue instanceof ServiceSubscription) {
                    command.serviceSubscription = (ServiceSubscription<?>) returnValue;
                }
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                Exception wrappedException = new DispatcherException("Exception calling " + methodName + ": " + cause.toString());
                wrappedException.initCause(cause);

                wrappedException.printStackTrace();
                command.error(wrappedException);
            } catch (Exception e) {
                e.printStackTrace();
                command.error(e);
            }
        } else {
            Log.d("ConnectSDKCordova", "Method not implemented: " + interfaceName + "." + methodName);
            command.error("method not implemented");
        }
    }

    /* ExternalInputControl methods */

    @CommandMethod
    public void externalInputControl_getExternalInputList(final JSCommand command, JSONObject args) throws JSONException {
        device.getExternalInputControl().getExternalInputList(new ExternalInputListListener() {
            @Override
            public void onSuccess(List<ExternalInputInfo> list) {
                command.success(list);
            }

            @Override
            public void onError(ServiceCommandError error) {
                command.error(error);
            }
        });
    }

    @CommandMethod
    public void externalInputControl_setExternalInput(JSCommand command, JSONObject args) throws JSONException {
        JSONObject externalInputInfoObj = args.getJSONObject("externalInputInfo");
        String inputId = externalInputInfoObj.getString("id");

        // FIXME
        ExternalInputInfo info = new ExternalInputInfo();
        info.setId(inputId);

        device.getExternalInputControl().setExternalInput(info, command.getResponseListener());
    }

    @CommandMethod
    public void externalInputControl_showExternalInputPicker(JSCommand command, JSONObject args) {
        device.getCapability(ExternalInputControl.class)
                .launchInputPicker(command.getAppLaunchListener());
    }

    /* FivewayControl methods */

    @CommandMethod
    public void keyControl_up(JSCommand command, JSONObject args) throws JSONException {
        device.getKeyControl().up(command.getResponseListener());
    }

    @CommandMethod
    public void keyControl_down(JSCommand command, JSONObject args) throws JSONException {
        device.getKeyControl().down(command.getResponseListener());
    }

    @CommandMethod
    public void keyControl_left(JSCommand command, JSONObject args) throws JSONException {
        device.getKeyControl().left(command.getResponseListener());
    }

    @CommandMethod
    public void keyControl_right(JSCommand command, JSONObject args) throws JSONException {
        device.getKeyControl().right(command.getResponseListener());
    }

    @CommandMethod
    public void keyControl_ok(JSCommand command, JSONObject args) throws JSONException {
        device.getKeyControl().ok(command.getResponseListener());
    }

    @CommandMethod
    public void keyControl_back(JSCommand command, JSONObject args) throws JSONException {
        device.getKeyControl().back(command.getResponseListener());
    }

    @CommandMethod
    public void keyControl_home(JSCommand command, JSONObject args) throws JSONException {
        device.getKeyControl().home(command.getResponseListener());
    }

    @CommandMethod
    public void keyControl_sendKeyCode(JSCommand command, JSONObject args) throws JSONException {
        int keyCode = args.getInt("keyCode");
        device.getKeyControl().sendKeyCode(KeyCode.createFromInteger(keyCode),
                command.getResponseListener());
    }

    /* TextInputControl methods */

    @CommandMethod
    public void textInputControl_sendText(JSCommand command, JSONObject args) throws JSONException {
        String text = args.getString("input");
        device.getTextInputControl().sendText(text);

        // FIXME keyboardControl needs a ResponseListener
        command.success();
    }

    @CommandMethod
    public void textInputControl_sendEnter(JSCommand command, JSONObject args) throws JSONException {
        device.getTextInputControl().sendEnter();

        // FIXME keyboardControl needs a ResponseListener
        command.success();
    }

    @CommandMethod
    public void textInputControl_sendDelete(JSCommand command, JSONObject args) throws JSONException {
        device.getTextInputControl().sendDelete();

        // FIXME keyboardControl needs a ResponseListener
        command.success();
    }

    @CommandMethod
    public ServiceSubscription<?> textInputControl_subscribeTextInputStatus(final JSCommand command, JSONObject args) throws JSONException {
        return device.getTextInputControl().subscribeTextInputStatus(new TextInputControl.TextInputStatusListener() {
            @Override
            public void onError(ServiceCommandError error) {
                command.error(error);
            }

            @Override
            public void onSuccess(TextInputStatusInfo status) {
                JSONObject response = new JSONObject();

                try {
                    response.put("isVisible", status.isFocused());
                    response.put("rawData", status.getRawData());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                command.success(response);
            }
        });
    }

    /* Launcher methods */

    @CommandMethod
    public void launcher_launchApp(JSCommand command, JSONObject args) throws JSONException {
        String appId = args.getString("appId");
        JSONObject params = args.optJSONObject("params");

        AppInfo appInfo = new AppInfo();
        appInfo.setId(appId);

        device.getLauncher().launchAppWithInfo(appInfo, params, command.getAppLaunchListener());
    }

    @CommandMethod
    public void launcher_launchAppStore(JSCommand command, JSONObject args) throws JSONException {
        String appId = args.optString("appId");
        device.getLauncher().launchAppStore(appId, command.getAppLaunchListener());
    }

    @CommandMethod
    public void launcher_launchBrowser(JSCommand command, JSONObject args) throws JSONException {
        String url = args.optString("url", null);

        device.getLauncher().launchBrowser(url, command.getAppLaunchListener());
    }

    @CommandMethod
    public void launcher_launchHulu(JSCommand command, JSONObject args) throws JSONException {
        String contentId = args.optString("contentId");
        device.getLauncher().launchHulu(contentId, command.getAppLaunchListener());
    }

    @CommandMethod
    public void launcher_launchNetflix(JSCommand command, JSONObject args) throws JSONException {
        String contentId = args.optString("contentId");
        device.getLauncher().launchNetflix(contentId, command.getAppLaunchListener());
    }

    @CommandMethod
    public void launcher_launchYouTube(JSCommand command, JSONObject args) throws JSONException {
        String contentId = args.optString("contentId");
        device.getLauncher().launchYouTube(contentId, command.getAppLaunchListener());
    }

    @CommandMethod
    public void launcher_getAppList(JSCommand command, JSONObject args) throws JSONException {
        device.getLauncher().getAppList(command.getAppListListener());
    }

    /* MediaControl methods */

    @CommandMethod
    public void mediaControl_play(JSCommand command, JSONObject args) throws JSONException {
        MediaControl mediaControl = getMediaControl(command, args);

        mediaControl.play(command.getResponseListener());
    }

    @CommandMethod
    public void mediaControl_pause(JSCommand command, JSONObject args) throws JSONException {
        MediaControl mediaControl = getMediaControl(command, args);

        mediaControl.pause(command.getResponseListener());
    }

    @CommandMethod
    public void mediaControl_stop(JSCommand command, JSONObject args) throws JSONException {
        MediaControl mediaControl = getMediaControl(command, args);

        mediaControl.stop(command.getResponseListener());
    }

    @CommandMethod
    public void mediaControl_fastForward(JSCommand command, JSONObject args) throws JSONException {
        MediaControl mediaControl = getMediaControl(command, args);

        mediaControl.fastForward(command.getResponseListener());
    }

    @CommandMethod
    public void mediaControl_rewind(JSCommand command, JSONObject args) throws JSONException {
        MediaControl mediaControl = getMediaControl(command, args);

        mediaControl.rewind(command.getResponseListener());
    }

    @CommandMethod
    public void mediaControl_seek(JSCommand command, JSONObject args) throws JSONException {
        MediaControl mediaControl = getMediaControl(command, args);

        double positionSeconds = args.getDouble("position");
        long positionMillis = (long) (1000L * positionSeconds);

        mediaControl.seek(positionMillis, command.getResponseListener());
    }

    @CommandMethod
    public void mediaControl_getDuration(final JSCommand command, JSONObject args) throws JSONException {
        MediaControl mediaControl = getMediaControl(command, args);

        mediaControl.getDuration(new DurationListener() {
            @Override
            public void onSuccess(Long durationMillis) {
                command.success((double) durationMillis / 1000.0);
            }

            @Override
            public void onError(ServiceCommandError error) {
                command.error(error);
            }
        });
    }

    @CommandMethod
    public void mediaControl_getPosition(final JSCommand command, JSONObject args) throws JSONException {
        MediaControl mediaControl = getMediaControl(command, args);

        mediaControl.getPosition(new PositionListener() {
            @Override
            public void onSuccess(Long positionMillis) {
                command.success((double) positionMillis / 1000.0);
            }

            @Override
            public void onError(ServiceCommandError error) {
                command.error(error);
            }
        });
    }

    @CommandMethod
    public void mediaControl_subscribePlayState(final JSCommand command, JSONObject args) throws JSONException {
        MediaControl mediaControl = getMediaControl(command, args);

        mediaControl.subscribePlayState(command.getPlayStateListener());
    }

    /* PlaylistControl methods */

    @CommandMethod
    public void playlistControl_next(JSCommand command, JSONObject args) throws JSONException {
        PlaylistControl playlistControl = getPlaylistControl(command, args);

        if (playlistControl != null) {
            playlistControl.next(command.getResponseListener());
        } else {
            firePlaylistControlNotAvailableError(command);
        }
    }

    @CommandMethod
    public void playlistControl_previous(JSCommand command, JSONObject args) throws JSONException {
        PlaylistControl playlistControl = getPlaylistControl(command, args);

        if (playlistControl != null) {
            playlistControl.previous(command.getResponseListener());
        } else {
            firePlaylistControlNotAvailableError(command);
        }
    }

    @CommandMethod
    public void playlistControl_jumpToTrack(JSCommand command, JSONObject args) throws JSONException {
        PlaylistControl playlistControl = getPlaylistControl(command, args);

        if (playlistControl != null) {
            long position = args.getLong("index");
            playlistControl.jumpToTrack(position, command.getResponseListener());
        } else {
            firePlaylistControlNotAvailableError(command);
        }
    }

    /* MediaPlayer methods */

    @CommandMethod
    public void mediaPlayer_displayImage(JSCommand command, JSONObject args) throws JSONException {
        displayMedia(command, args, "image");
    }

    @CommandMethod
    public void mediaPlayer_playMedia(JSCommand command, JSONObject args) throws JSONException {
        displayMedia(command, args, "video");
    }

    /* MouseControl methods */

    @CommandMethod
    public void mouseControl_connectMouse(JSCommand command, JSONObject args) throws JSONException {
        device.getMouseControl().connectMouse();

        // FIXME check if this actually worked
        command.success();
    }

    @CommandMethod
    public void mouseControl_move(JSCommand command, JSONObject args) throws JSONException {
        double dx = args.getDouble("dx");
        double dy = args.getDouble("dy");

        device.getMouseControl().move(dx, dy);

        // FIXME check if this actually worked
        command.success();
    }

    @CommandMethod
    public void mouseControl_scroll(JSCommand command, JSONObject args) throws JSONException {
        double dx = args.getDouble("dx");
        double dy = args.getDouble("dy");

        device.getMouseControl().scroll(dx, dy);

        // FIXME check if this actually worked
        command.success();
    }

    @CommandMethod
    public void mouseControl_click(JSCommand command, JSONObject args) throws JSONException {
        device.getMouseControl().click();

        // FIXME check if this actually worked
        command.success();
    }

    /* PowerControl methods */
    @CommandMethod
    public void powerControl_powerOff(JSCommand command, JSONObject args) throws JSONException {
        device.getPowerControl().powerOff(command.getResponseListener());
    }

    /* ToastControl methods */

    @CommandMethod
    public void toastControl_showToast(JSCommand command, JSONObject args) throws JSONException {
        showToast(command, args, false);
    }

    @CommandMethod
    public void toastControl_showClickableToast(JSCommand command, JSONObject args) throws JSONException {
        showToast(command, args, true);
    }

    /* TVControl methods */

    @CommandMethod
    public void TVControl_channelUp(JSCommand command, JSONObject args) throws JSONException {
        device.getTVControl().channelUp(command.getResponseListener());
    }

    @CommandMethod
    public void TVControl_channelDown(JSCommand command, JSONObject args) throws JSONException {
        device.getTVControl().channelDown(command.getResponseListener());
    }

    @CommandMethod
    public void TVControl_setChannel(JSCommand command, JSONObject args) throws JSONException {
        JSONObject channelInfoObj = args.getJSONObject("channelInfo");
        String channelId = channelInfoObj.optString("id");
        String channelNumber = channelInfoObj.optString("number");

        ChannelInfo channelInfo = new ChannelInfo();
        channelInfo.setId(channelId);
        channelInfo.setNumber(channelNumber);

        device.getTVControl().setChannel(channelInfo, command.getResponseListener());
    }

    @CommandMethod
    public void TVControl_getChannelList(JSCommand command, JSONObject args) throws JSONException {
        device.getTVControl().getChannelList(command.getChannelListListener());
    }

    @CommandMethod
    public void TVControl_getCurrentChannel(JSCommand command, JSONObject args) throws JSONException {
        device.getTVControl().getCurrentChannel(command.getChannelListener());
    }

    @CommandMethod
    public void TVControl_subscribeCurrentChannel(JSCommand command, JSONObject args) throws JSONException {
        device.getTVControl().subscribeCurrentChannel(command.getChannelListener());
    }

    /* VolumeControl methods */

    @CommandMethod
    public void volumeControl_getVolume(JSCommand command, JSONObject args) throws JSONException {
        device.getVolumeControl().getVolume(command.getVolumeListener());
    }

    @CommandMethod
    public void volumeControl_setVolume(JSCommand command, JSONObject args) throws JSONException {
        float volume = (float) args.getDouble("volume");

        device.getVolumeControl().setVolume(volume, command.getResponseListener());
    }

    @CommandMethod
    public void volumeControl_volumeUp(JSCommand command, JSONObject args) throws JSONException {
        device.getVolumeControl().volumeUp(command.getResponseListener());
    }

    @CommandMethod
    public void volumeControl_volumeDown(JSCommand command, JSONObject args) throws JSONException {
        device.getVolumeControl().volumeDown(command.getResponseListener());
    }

    @CommandMethod
    public void volumeControl_getMute(JSCommand command, JSONObject args) throws JSONException {
        device.getVolumeControl().getMute(command.getMuteListener());
    }

    @CommandMethod
    public void volumeControl_setMute(JSCommand command, JSONObject args) throws JSONException {
        boolean mute = args.getBoolean("mute");
        device.getVolumeControl().setMute(mute, command.getResponseListener());
    }

    @CommandMethod
    public ServiceSubscription<?> volumeControl_subscribeVolume(JSCommand command, JSONObject args) throws JSONException {
        return device.getVolumeControl().subscribeVolume(command.getVolumeListener());
    }

    @CommandMethod
    public ServiceSubscription<?> volumeControl_subscribeMute(JSCommand command, JSONObject args) throws JSONException {
        return device.getVolumeControl().subscribeMute(command.getMuteListener());
    }

    /* WebAppLauncher methods */
    @CommandMethod
    public void webAppLauncher_launchWebApp(JSCommand command, JSONObject args) throws JSONException {
        String webAppId = args.getString("webAppId");
        JSONObject params = args.optJSONObject("params");

        if (params != null && device.hasCapability(WebAppLauncher.Launch_Params))
            device.getWebAppLauncher().launchWebApp(webAppId, params, command.getWebAppLaunchListener());
        else
            device.getWebAppLauncher().launchWebApp(webAppId, command.getWebAppLaunchListener());
    }

    @CommandMethod
    public void webAppLauncher_joinWebApp(JSCommand command, JSONObject args) throws JSONException {
        String webAppId = args.getString("webAppId");

        device.getWebAppLauncher().joinWebApp(webAppId, command.getWebAppLaunchListener());
    }

    @CommandMethod
    public void webAppLauncher_pinWebApp(JSCommand command, JSONObject args) throws JSONException {
        String webAppId = args.getString("webAppId");

        device.getWebAppLauncher().pinWebApp(webAppId, command.getResponseListener());
    }

    @CommandMethod
    public void webAppLauncher_unPinWebApp(JSCommand command, JSONObject args) throws JSONException {
        String webAppId = args.getString("webAppId");

        device.getWebAppLauncher().unPinWebApp(webAppId, command.getResponseListener());
    }

    @CommandMethod
    public void webAppLauncher_isWebAppPinned(final JSCommand command, JSONObject args) throws JSONException {
        String webAppId = args.getString("webAppId");

        device.getWebAppLauncher().isWebAppPinned(webAppId, new WebAppSession
                .WebAppPinStatusListener() {
            @Override
            public void onSuccess(Boolean object) {
                command.success(object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                command.error(error);
            }
        });
    }

    @CommandMethod
    public ServiceSubscription<?> webAppLauncher_subscribeIsWebAppPinned(final JSCommand command, JSONObject args) throws JSONException {
        String webAppId = args.getString("webAppId");

        return device.getWebAppLauncher().subscribeIsWebAppPinned(webAppId, new WebAppSession.WebAppPinStatusListener() {

            @Override
            public void onSuccess(Boolean object) {
                command.success(object);
            }

            @Override
            public void onError(ServiceCommandError error) {
                command.error(error);
            }
        });
    }


    /* WebAppSession methods */

    WebAppSessionWrapper getWebAppSessionWrapper(String objectId) {
        WebAppSessionWrapper wrapper = (WebAppSessionWrapper) plugin.getObjectWrapper(objectId);

        if (wrapper == null) {
            throw new DispatcherException("WebAppSession no longer exists. Make sure to acquire() it during the success callback.");
        }

        return wrapper;
    }

    @CommandMethod
    public void webAppSession_connect(JSCommand command, JSONObject args) throws JSONException {
        String objectId = args.getString("objectId");
        final WebAppSessionWrapper wrapper = getWebAppSessionWrapper(objectId);

        wrapper.session.connect(command.getResponseListener());
    }

    @CommandMethod
    public void webAppSession_disconnect(JSCommand command, JSONObject args) throws JSONException {
        String objectId = args.getString("objectId");
        final WebAppSessionWrapper wrapper = getWebAppSessionWrapper(objectId);

        wrapper.session.disconnectFromWebApp();

        command.success();
    }

    @CommandMethod
    public void webAppSession_setWebAppSessionListener(JSCommand command, JSONObject args) throws JSONException {
        String objectId = args.getString("objectId");
        final WebAppSessionWrapper wrapper = getWebAppSessionWrapper(objectId);

        wrapper.session.setWebAppSessionListener(new WebAppSessionListener() {
            @Override
            public void onReceiveMessage(WebAppSession webAppSession, Object message) {
                wrapper.sendEvent("message", message);
            }

            @Override
            public void onWebAppSessionDisconnect(WebAppSession webAppSession) {
                wrapper.sendEvent("disconnect");
            }
        });

        command.success();
    }

    @CommandMethod
    public void webAppSession_sendText(JSCommand command, JSONObject args) throws JSONException {
        String text = args.getString("text");
        String objectId = args.getString("objectId");
        final WebAppSessionWrapper wrapper = getWebAppSessionWrapper(objectId);

        wrapper.session.sendMessage(text, command.getResponseListener());
    }

    @CommandMethod
    public void webAppSession_sendJSON(JSCommand command, JSONObject args) throws JSONException {
        JSONObject json = args.getJSONObject("jsonObject");
        String objectId = args.getString("objectId");
        final WebAppSessionWrapper wrapper = getWebAppSessionWrapper(objectId);

        wrapper.session.sendMessage(json, command.getResponseListener());
    }

    /* Service-specific methods */
    @CommandMethod
    public void webOSTVService_connectToApp(JSCommand command, JSONObject args) throws JSONException {
        WebOSTVService service = (WebOSTVService) device.getServiceByName(WebOSTVService.ID);

        String appId = args.getString("appId");

        if (service != null) {
            service.connectToApp(appId, command.getWebAppLaunchListener());
        } else {
            command.error("WebOSTVService not available for this device");
        }
    }

    @CommandMethod
    public void webOSTVService_joinApp(JSCommand command, JSONObject args) throws JSONException {
        WebOSTVService service = (WebOSTVService) device.getServiceByName(WebOSTVService.ID);

        String appId = args.getString("appId");

        if (service != null) {
            service.joinApp(appId, command.getWebAppLaunchListener());
        } else {
            command.error("WebOSTVService not available for this device");
        }
    }

    /* Special methods */

    @CommandMethod
    public void CORDOVAPLUGIN_closeLaunchSession(JSCommand command, JSONObject args) throws JSONException {
        JSONObject obj = args.getJSONObject("launchSession");
        LaunchSession session = getLaunchSession(obj);

        if (session != null) {
            session.close(command.getResponseListener());
        } else {
            command.error("unknown launch session type; could not close");
        }
    }

    /* Helper methods */

    void displayMedia(JSCommand command, JSONObject args, String type) throws JSONException {
        String url = args.getString("url");
        String mimeType = args.getString("mimeType");

        String title = null;
        String description = null;
        String iconSrc = null;
        boolean shouldLoop = false;
        SubtitleInfo subtitles = null;

        JSONObject options = args.optJSONObject("options");
        if (options != null) {
            title = options.optString("title");
            description = options.optString("description");
            iconSrc = options.optString("iconUrl");
            shouldLoop = options.optBoolean("shouldLoop");
            JSONObject subtitlesJson = options.optJSONObject("subtitles");
            if (subtitlesJson != null) {
                subtitles = new SubtitleInfo.Builder(subtitlesJson.getString("url"))
                        .setLabel(subtitlesJson.optString("label"))
                        .setLanguage(subtitlesJson.optString("language"))
                        .setMimeType(subtitlesJson.optString("mimeType"))
                        .build();
            }
        }

        MediaInfo mediaInfo = new MediaInfo.Builder(url, mimeType)
                .setTitle(title)
                .setDescription(description)
                .setIcon(iconSrc)
                .setSubtitleInfo(subtitles)
                .build();

        if ("image".equals(type)) {
            device.getCapability(MediaPlayer.class).displayImage(mediaInfo,
                    command.getMediaLaunchListener());
        } else {
            device.getCapability(MediaPlayer.class).playMedia(mediaInfo, shouldLoop,
                    command.getMediaLaunchListener());
        }
    }

    void showToast(JSCommand command, JSONObject args, boolean clickable) throws JSONException {
        String message = args.getString("message");
        JSONObject options = args.optJSONObject("options");

        String url = null;
        String appId = null;
        String iconData = null;
        String iconExtension = null;
        JSONObject params = null;

        if (options != null) {
            iconData = options.optString("iconData");
            iconExtension = options.optString("iconExtension");

            if (clickable) {
                url = options.optString("url");
                appId = options.optString("appId");
                params = options.optJSONObject("params");
            }
        }

        if (clickable) {
            if (url != null) {
                device.getToastControl().showClickableToastForURL(message, url, iconData, iconExtension, command.getResponseListener());
            } else if (appId != null) {
                AppInfo appInfo = new AppInfo();
                appInfo.setId(appId);
                device.getToastControl().showClickableToastForApp(message, appInfo, params, iconData, iconExtension, command.getResponseListener());
            } else {
                command.error("toast options must include url or appId when showing a clickable toast");
            }
        } else {
            device.getToastControl().showToast(message, iconData, iconExtension, command
                    .getResponseListener());
        }
    }

    LaunchSession getLaunchSession(JSONObject obj) {
        LaunchSession session = LaunchSession.launchSessionFromJSONObject(obj);
        String serviceName = obj.optString("serviceName");
        DeviceService service = serviceName != null ? device.getServiceByName(serviceName) : null;

        if (service != null) {
            session.setService(service);
        }

        return session;
    }

    MediaControl getMediaControl(JSCommand command, JSONObject args) throws JSONException {
        if (args.has("objectId")) {
            String objectId = args.optString("objectId");
            MediaControlWrapper wrapper = (MediaControlWrapper) plugin.getObjectWrapper(objectId);

            if (wrapper != null) {
                return wrapper.mediaControl;
            } else {
                throw new DispatcherException("MediaControl session no longer exists");
            }
        } else {
            return device.getMediaControl();
        }
    }

    PlaylistControl getPlaylistControl(JSCommand command, JSONObject args) throws JSONException {
        if (args.has("objectId")) {
            String objectId = args.optString("objectId");
            PlaylistControlWrapper wrapper = (PlaylistControlWrapper) plugin.getObjectWrapper(objectId);

            if (wrapper != null) {
                return wrapper.playlistControl;
            } else {
                throw new DispatcherException("PlaylistControlWrapper session no longer exists");
            }
        } else {
            return device.getCapability(PlaylistControl.class);
        }
    }

    private void firePlaylistControlNotAvailableError(JSCommand command) {
        command.getResponseListener().onError(new ServiceCommandError("PlaylistControl is not available"));
    }
}
