(function (globalScope) {

    function showCollectionPanel(items) {

    }

    globalScope.BoxSetEditor = {

        showPanel: function (items) {
            showCollectionPanel(items);
        },

        onNewCollectionSubmit: function () {

        },

        supportsAddingToCollection: function (item) {

            var invalidTypes = ['Person', 'Genre', 'MusicGenre', 'Studio', 'GameGenre', 'BoxSet', 'Playlist', 'UserView', 'CollectionFolder', 'Audio', 'Episode'];

            return item.LocationType == 'FileSystem' && !item.CollectionType && invalidTypes.indexOf(item.Type) == -1;
        }
    };

})(this);