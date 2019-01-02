define(['playbackManager', 'nowPlayingHelper', 'events', 'connectionManager'], function (playbackManager, nowPlayingHelper, events, connectionManager) {

    // Reports media playback to the device for lock screen control

    var currentPlayer;
    var lastUpdateTime = 0;

    function allowLocalPlayer(playerId, isVideo) {

        // It has the notifications built-in
        //if (playerId === 'vlcplayer') {
        //    return false;
        //}

        return !isVideo;
    }

    function seriesImageUrl(item, options) {

        if (item.Type !== 'Episode') {
            return null;
        }

        options = options || {};
        options.type = options.type || "Primary";

        if (options.type === 'Primary') {

            if (item.SeriesPrimaryImageTag) {

                options.tag = item.SeriesPrimaryImageTag;

                return connectionManager.getApiClient(item.ServerId).getScaledImageUrl(item.SeriesId, options);
            }
        }

        if (options.type === 'Thumb') {

            if (item.SeriesThumbImageTag) {

                options.tag = item.SeriesThumbImageTag;

                return connectionManager.getApiClient(item.ServerId).getScaledImageUrl(item.SeriesId, options);
            }
            if (item.ParentThumbImageTag) {

                options.tag = item.ParentThumbImageTag;

                return connectionManager.getApiClient(item.ServerId).getScaledImageUrl(item.ParentThumbItemId, options);
            }
        }

        return null;
    }

    function imageUrl(item, options) {

        options = options || {};
        options.type = options.type || "Primary";

        if (item.ImageTags && item.ImageTags[options.type]) {

            options.tag = item.ImageTags[options.type];
            return connectionManager.getApiClient(item.ServerId).getScaledImageUrl(item.Id, options);
        }

        if (item.AlbumId && item.AlbumPrimaryImageTag) {

            options.tag = item.AlbumPrimaryImageTag;
            return connectionManager.getApiClient(item.ServerId).getScaledImageUrl(item.AlbumId, options);
        }

        return null;
    }

    function updatePlayerState(player, state, eventName) {

        if (!state.NowPlayingItem) {
            hideMediaControls();
            return;
        }

        // dummy this up
        if (eventName == 'init') {
            eventName = 'timeupdate';
        }

        var isLocalPlayer = player.isLocalPlayer || false;

        var isVideo = state.NowPlayingItem.MediaType == 'Video';

        // Local players do their own notifications
        if (isLocalPlayer && !allowLocalPlayer(player.id, isVideo)) {
            return;
        }

        var playState = state.PlayState || {};

        var parts = nowPlayingHelper.getNowPlayingNames(state.NowPlayingItem);

        var artist = parts.length == 1 ? '' : parts[0].text;
        var title = parts[parts.length - 1].text;

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

        var nowPlayingItem = state.NowPlayingItem;

        var imageOptions = {
            maxHeight: 400
        };
        var url = seriesImageUrl(nowPlayingItem, imageOptions) || imageUrl(nowPlayingItem, imageOptions);

        var now = new Date().getTime();

        // Don't go crazy reporting position changes
        if (eventName == 'timeupdate') {
            if ((now - lastUpdateTime) < 5000) {
                // Only report if this item hasn't been reported yet, or if there's an actual playback change.
                // Don't report on simple time updates
                return;
            }
        }

        MainActivity.updateMediaSession(eventName, isLocalPlayer, itemId, title, artist, album, parseInt(duration), parseInt(position), url, canSeek, isPaused);
        lastUpdateTime = now;
    }

    function onGeneralEvent(e, state) {

        var player = this;
        playbackManager.getPlayerState(player).then(function (state) {

            updatePlayerState(player, state, e.type);
        });
    }

    function onStateChanged(e, state) {

        var player = this;
        updatePlayerState(player, state, 'statechange');
    }

    function onPlaybackStart(e, state) {

        console.log('nowplaying event: ' + e.type);

        var player = this;

        updatePlayerState(player, state, e.type);
    }

    function onPlaybackStopped(e, state) {

        console.log('nowplaying event: ' + e.type);
        var player = this;

        hideMediaControls();
    }

    function releaseCurrentPlayer() {

        if (currentPlayer) {

            events.off(currentPlayer, 'playbackstart', onPlaybackStart);
            events.off(currentPlayer, 'playbackstop', onPlaybackStopped);
            events.off(currentPlayer, 'play', onGeneralEvent);
            events.off(currentPlayer, 'pause', onGeneralEvent);
            events.off(currentPlayer, 'statechange', onStateChanged);
            events.off(currentPlayer, 'timeupdate', onGeneralEvent);

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

        if (!player) {
            return;
        }

        if (player.isLocalPlayer && !allowLocalPlayer(player.id)) {
            return;
        }

        currentPlayer = player;

        console.log('binding remotecontrols to ' + player.name);

        playbackManager.getPlayerState(player).then(function (state) {

            updatePlayerState(player, state, 'init');
        });

        events.on(currentPlayer, 'playbackstart', onPlaybackStart);
        events.on(currentPlayer, 'playbackstop', onPlaybackStopped);
        events.on(currentPlayer, 'play', onGeneralEvent);
        events.on(currentPlayer, 'pause', onGeneralEvent);
        events.on(currentPlayer, 'statechange', onStateChanged);
        events.on(currentPlayer, 'timeupdate', onGeneralEvent);
    }

    console.log('binding remotecontrols to playbackManager');

    events.on(playbackManager, 'playerchange', function () {

        bindToPlayer(playbackManager.getCurrentPlayer());
    });

    bindToPlayer(playbackManager.getCurrentPlayer());
});