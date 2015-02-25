(function (globalScope) {

    function showNewPlaylistPanel(items) {

    }

    globalScope.PlaylistManager = {

        showPanel: function (items) {
            showNewPlaylistPanel(items);
        },

        supportsPlaylists: function (item) {

            return item.RunTimeTicks || item.IsFolder || item.Type == "Genre" || item.Type == "MusicGenre" || item.Type == "MusicArtist";
        }
    };

})(this);