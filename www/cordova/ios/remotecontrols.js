define(['events'], function (events) {

    var currentPlayer;
    var lastPlayerState;

    function updatePlayerState(state) {

        if (!state.NowPlayingItem) {
            hideNowPlayingBar();
            return;
        }

        lastPlayerState = state;
        var playState = state.PlayState || {};
        var nameHtml = MediaController.getNowPlayingNameHtml(state.NowPlayingItem) || '';
        var parts = nameHtml.split('<br/>');
        var artist = parts.length == 1 ? '' : parts[0];
        var title = parts[parts.length - 1];
        var album = state.NowPlayingItem.Album || '';
        var duration = state.NowPlayingItem.RunTimeTicks ? (state.NowPlayingItem.RunTimeTicks / 10000000) : 0;
        var elapsedTime = playState.PositionTicks ? (playState.PositionTicks / 10000000) : 0;
        var url = '';
        var imgHeight = 600;
        var nowPlayingItem = state.NowPlayingItem;
        if (nowPlayingItem.PrimaryImageTag) {
            url = ApiClient.getScaledImageUrl(nowPlayingItem.PrimaryImageItemId, { type: "Primary", height: imgHeight, tag: nowPlayingItem.PrimaryImageTag });
        }
        else if (nowPlayingItem.BackdropImageTag) {
            url = ApiClient.getScaledImageUrl(nowPlayingItem.BackdropItemId, { type: "Backdrop", height: imgHeight, tag: nowPlayingItem.BackdropImageTag, index: 0 });
        }
        else if (nowPlayingItem.ThumbImageTag) {
            url = ApiClient.getScaledImageUrl(nowPlayingItem.ThumbImageItemId, { type: "Thumb", height: imgHeight, tag: nowPlayingItem.ThumbImageTag });
        }

        var params = [artist, title, album, url, duration, elapsedTime];

        try {
            window.remoteControls.updateMetas(onUpdateMetasSuccess, onUpdateMetasFail, params);
        } catch (err) {
            onUpdateMetasFail(err);
        }
    }

    var lastUpdateTime = 0; function onStateChanged(e, state) {

        if (e.type == 'positionchange') {
            var now = new Date().getTime();
            if ((now - lastUpdateTime) < 700) {
                return;
            }
            lastUpdateTime = now;
        }
        updatePlayerState(state);
    }

    function onPlaybackStart(e, state) {
        console.log('nowplaying event: ' + e.type);
        var player = this;
        player.beginPlayerUpdates();
        onStateChanged.call(player, e, state);
    }

    function onPlaybackStopped(e, state) {

        console.log('nowplaying event: ' + e.type);
        var player = this;
        player.endPlayerUpdates();
        hideNowPlayingBar();
    }

    function releaseCurrentPlayer() {

        var player = currentPlayer;

        if (player) {
            events.off(player, 'playbackstart', onPlaybackStart);
            events.off(player, 'playbackstop', onPlaybackStopped);
            events.off(player, 'playstatechange', onStateChanged);
            events.off(player, 'positionchange', onStateChanged);
            player.endPlayerUpdates();
            currentPlayer = null;
            hideNowPlayingBar();
        }
    }

    function hideNowPlayingBar() {

        var artist = "";
        var title = "";
        var album = "";
        var image = "";
        var duration = 0;
        var elapsedTime = 0;
        var params = [artist, title, album, image, duration, elapsedTime];
        try {
            window.remoteControls.updateMetas(onUpdateMetasSuccess, onUpdateMetasFail, params);
        } catch (err) {
            onUpdateMetasFail(err);
        }
    }

    function onUpdateMetasSuccess() {
        console.log('onUpdateMetasSuccess');
    }

    function onUpdateMetasFail(fail) {
        console.log('onUpdateMetasFail: ' + fail);
    }

    function bindToPlayer(player) {

        releaseCurrentPlayer();
        currentPlayer = player;

        if (!player.isLocalPlayer) { return; }

        console.log('binding remotecontrols to MediaPlayer');

        player.getPlayerState().then(function (state) {
            if (state.NowPlayingItem) {
                player.beginPlayerUpdates();
            }
            onStateChanged.call(player, { type: 'init' }, state);
        });

        events.on(player, 'playbackstart', onPlaybackStart);
        events.on(player, 'playbackstop', onPlaybackStopped);
        events.on(player, 'playstatechange', onStateChanged);
        events.on(player, 'positionchange', onStateChanged);
    }

    events.on(MediaController, 'playerchange', function () {
        bindToPlayer(MediaController.getCurrentPlayer());
    });

    bindToPlayer(MediaController.getCurrentPlayer());

    //listen for the event
    document.addEventListener("remote-event", function (event) {
        var remoteEvent = event.remoteEvent;

        switch (remoteEvent.subtype) {
            case 'playpause':
                MediaController.pause();
                break;
            case 'play':
                MediaController.unpause();
                break;
            case 'pause':
                MediaController.pause();
                break;
            case 'prevTrack':
                MediaController.previousTrack();
                break;
            case 'nextTrack':
                MediaController.nextTrack();
                break;
            default:
                break;
        }
    });

});