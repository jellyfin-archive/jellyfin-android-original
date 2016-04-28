(function () {

    // Reports media playback to the device for lock screen control

    var currentPlayer;
    var lastUpdateTime = 0;

    function allowLocalPlayer(isVideo) {

        return !window.VlcAudio && !isVideo;
    }

    function updatePlayerState(state, eventName) {

        if (!state.NowPlayingItem) {
            hideMediaControls();
            return;
        }

        // dummy this up
        if (eventName == 'init') {
            eventName = 'positionchange';
        }

        var isLocalPlayer = MediaController.getPlayerInfo().isLocalPlayer || false;

        var isVideo = state.NowPlayingItem.MediaType == 'Video';

        // Local players do their own notifications
        if (isLocalPlayer && !allowLocalPlayer(isVideo)) {
            return;
        }

        var playState = state.PlayState || {};

        var nameHtml = MediaController.getNowPlayingNameHtml(state.NowPlayingItem) || '';
        var parts = nameHtml.split('<br/>');

        var artist = parts.length == 1 ? '' : parts[0];
        var title = parts[parts.length - 1];

        // Switch these two around for video
        if (isVideo && parts.length > 1) {
            var temp = artist;
            artist = title;
            title = temp;
        }

        var album = state.NowPlayingItem.Album || '';
        var itemId = state.NowPlayingItem.Id;

        // Convert to ms
        var duration = state.NowPlayingItem.RunTimeTicks ? (state.NowPlayingItem.RunTimeTicks / 10000) : 0;
        var position = playState.PositionTicks ? (playState.PositionTicks / 10000) : 0;

        var isPaused = playState.IsPaused || false;
        var canSeek = playState.CanSeek || false;

        var url = '';
        var imgHeight = 400;

        var nowPlayingItem = state.NowPlayingItem;

        if (nowPlayingItem.PrimaryImageTag) {

            url = ApiClient.getScaledImageUrl(nowPlayingItem.PrimaryImageItemId, {
                type: "Primary",
                height: imgHeight,
                tag: nowPlayingItem.PrimaryImageTag
            });
        } else if (nowPlayingItem.ThumbImageTag) {

            url = ApiClient.getScaledImageUrl(nowPlayingItem.ThumbImageItemId, {
                type: "Thumb",
                height: imgHeight,
                tag: nowPlayingItem.ThumbImageTag
            });
        }
        else if (nowPlayingItem.BackdropImageTag) {

            url = ApiClient.getScaledImageUrl(nowPlayingItem.BackdropItemId, {
                type: "Backdrop",
                height: imgHeight,
                tag: nowPlayingItem.BackdropImageTag,
                index: 0
            });

        }

		var now = new Date().getTime();
		
        // Don't go crazy reporting position changes
        if (eventName == 'positionchange') {
            if ((now - lastUpdateTime) < 5000) {
                // Only report if this item hasn't been reported yet, or if there's an actual playback change.
                // Don't report on simple time updates
                return;
            }
        }

        MainActivity.updateMediaSession(eventName, isLocalPlayer, itemId, title, artist, album, parseInt(duration), parseInt(position), url, canSeek, isPaused);
        lastUpdateTime = now;
    }

    function onStateChanged(e, state) {

        updatePlayerState(state, e.type);
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

        hideMediaControls();
    }

    function releaseCurrentPlayer() {

        if (currentPlayer) {

            Events.off(currentPlayer, 'playbackstart', onPlaybackStart);
            Events.off(currentPlayer, 'playbackstop', onPlaybackStopped);
            Events.off(currentPlayer, 'playstatechange', onStateChanged);
            Events.off(currentPlayer, 'positionchange', onStateChanged);

            currentPlayer.endPlayerUpdates();
            currentPlayer = null;

            hideMediaControls();
        }
    }

    function hideMediaControls() {
        MainActivity.hideMediaSession();
        lastUpdateTime = 0;
    }

    function bindToPlayer(player) {

        releaseCurrentPlayer();

        if (player.isLocalPlayer && !allowLocalPlayer()) {
            return;
        }

        currentPlayer = player;

        console.log('binding remotecontrols to ' + player.name);

        player.getPlayerState().then(function (state) {

            if (state.NowPlayingItem) {
                player.beginPlayerUpdates();
            }

            onStateChanged.call(player, { type: 'init' }, state);
        });

        Events.on(currentPlayer, 'playbackstart', onPlaybackStart);
        Events.on(currentPlayer, 'playbackstop', onPlaybackStopped);
        Events.on(currentPlayer, 'playstatechange', onStateChanged);
        Events.on(currentPlayer, 'positionchange', onStateChanged);
    }

    console.log('binding remotecontrols to MediaController');

    Events.on(MediaController, 'playerchange', function () {

        bindToPlayer(MediaController.getCurrentPlayer());
    });

    bindToPlayer(MediaController.getCurrentPlayer());

})();