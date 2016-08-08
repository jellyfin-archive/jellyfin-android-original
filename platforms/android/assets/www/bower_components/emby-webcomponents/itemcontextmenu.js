define(["apphost","globalize","connectionManager","itemHelper","embyRouter","playbackManager","loading","appSettings"],function(e,n,t,a,r,s,o,i){function l(r){var o=r.item,l=o.ServerId,d=t.getApiClient(l),c=s.canPlay(o);return d.getCurrentUser().then(function(t){var l=[];if(a.supportsAddingToCollection(o)&&l.push({name:n.translate("sharedcomponents#AddToCollection"),id:"addtocollection"}),a.supportsAddingToPlaylist(o)&&l.push({name:n.translate("sharedcomponents#AddToPlaylist"),id:"addtoplaylist"}),"Timer"==o.Type&&t.Policy.EnableLiveTvManagement&&l.push({name:n.translate("sharedcomponents#ButtonCancel"),id:"canceltimer"}),o.CanDelete&&l.push({name:n.translate("sharedcomponents#Delete"),id:"delete"}),a.canEdit(t,o.Type)&&r.edit!==!1){var d=n.translate("Timer"==o.Type?"sharedcomponents#Edit":"sharedcomponents#EditInfo");l.push({name:d,id:"edit"})}return a.canEditImages(t,o.Type)&&I&&r.editImages!==!1&&l.push({name:n.translate("sharedcomponents#EditImages"),id:"editimages"}),a.canEdit(t,o.Type)&&"Video"==o.MediaType&&"TvChannel"!=o.Type&&"Program"!=o.Type&&"Virtual"!=o.LocationType&&r.editSubtitles!==!1&&l.push({name:n.translate("sharedcomponents#EditSubtitles"),id:"editsubtitles"}),o.CanDownload&&e.supports("filedownload")&&l.push({name:n.translate("sharedcomponents#Download"),id:"download"}),r.identify!==!1&&a.canIdentify(t,o.Type)&&l.push({name:n.translate("sharedcomponents#Identify"),id:"identify"}),("Audio"==o.MediaType||"MusicAlbum"==o.Type||"MusicArtist"==o.Type||"MusicGenre"==o.Type||"music"==o.CollectionType)&&r.instantMix!==!1&&l.push({name:n.translate("sharedcomponents#InstantMix"),id:"instantmix"}),r.open!==!1&&"Timer"!=o.Type&&"Audio"!=o.Type&&l.push({name:n.translate("sharedcomponents#Open"),id:"open"}),c&&(r.play!==!1&&(l.push({name:n.translate("sharedcomponents#Play"),id:"resume"}),I&&i.enableExternalPlayers()&&l.push({name:n.translate("ButtonPlayExternalPlayer"),id:"externalplayer"})),r.playAllFromHere&&"Program"!=o.Type&&"TvChannel"!=o.Type&&l.push({name:n.translate("sharedcomponents#PlayAllFromHere"),id:"playallfromhere"}),s.canQueue(o)&&(r.queue!==!1&&l.push({name:n.translate("sharedcomponents#Queue"),id:"queue"}),r.queueAllFromHere&&l.push({name:n.translate("sharedcomponents#QueueAllFromHere"),id:"queueallfromhere"}))),"Program"!=o.Type||o.TimerId||o.SeriesTimerId||l.push({name:Globalize.translate("sharedcomponents#Record"),id:"record"}),t.Policy.IsAdministrator&&"Timer"!=o.Type&&"Program"!=o.Type&&l.push({name:n.translate("sharedcomponents#Refresh"),id:"refresh"}),o.PlaylistItemId&&r.playlistId&&l.push({name:n.translate("sharedcomponents#RemoveFromPlaylist"),id:"removefromplaylist"}),r.collectionId&&l.push({name:n.translate("sharedcomponents#RemoveFromCollection"),id:"removefromcollection"}),r.share!==!1&&a.canShare(t,o)&&l.push({name:n.translate("sharedcomponents#Share"),id:"share"}),(o.IsFolder||"MusicArtist"==o.Type||"MusicGenre"==o.Type)&&r.shuffle!==!1&&l.push({name:n.translate("sharedcomponents#Shuffle"),id:"shuffle"}),I&&r.sync!==!1&&a.canSync(t,o)&&l.push({name:n.translate("sharedcomponents#Sync"),id:"sync"}),r.openAlbum!==!1&&o.AlbumId&&l.push({name:Globalize.translate("sharedcomponents#ViewAlbum"),id:"album"}),r.openArtist!==!1&&o.ArtistItems&&o.ArtistItems.length&&l.push({name:Globalize.translate("sharedcomponents#ViewArtist"),id:"artist"}),l})}function d(e,n,t,a){return function(){e({command:n,updated:t,deleted:a})}}function c(e,n,a){var o=e.Id,i=e.ServerId,l=t.getApiClient(i);return new Promise(function(t,c){switch(n){case"addtocollection":require(["collectionEditor"],function(e){(new e).show({items:[o],serverId:i}).then(d(t,n,!0),d(t,n))});break;case"addtoplaylist":require(["playlistEditor"],function(e){(new e).show({items:[o],serverId:i}).then(d(t,n,!0),d(t,n))});break;case"download":require(["fileDownloader"],function(e){var a=l.getUrl("Items/"+o+"/Download",{api_key:l.accessToken()});e.download([{url:a,itemId:o,serverId:i}]),d(d(t,n),n)()});break;case"editsubtitles":require(["subtitleEditor"],function(e){e.show(o,i).then(d(t,n,!0),d(t,n))});break;case"edit":p(l,e).then(d(t,n,!0),d(t,n));break;case"editimages":require(["components/imageeditor/imageeditor"],function(e){e.show(o).then(d(t,n,!0),d(t,n))});break;case"identify":require(["itemIdentifier"],function(e){e.show(o,i).then(d(t,n,!0),d(t,n))});break;case"refresh":f(l,o),d(t,n)();break;case"open":r.showItem(e),d(t,n)();break;case"play":u(e,!1),d(t,n)();break;case"resume":u(e,!0),d(t,n)();break;case"queue":u(e,!1,!0),d(t,n)();break;case"record":require(["recordingCreator"],function(e){e.show(o,i).then(d(t,n,!0),d(t,n))});break;case"shuffle":s.shuffle(e),d(t,n)();break;case"instantmix":s.instantMix(e),d(t,n)();break;case"delete":h(l,o).then(d(t,n,!0,!0),d(t,n));break;case"share":require(["sharingmanager"],function(e){e.showMenu({serverId:i,itemId:o}).then(d(t,n))});break;case"externalplayer":LibraryBrowser.playInExternalPlayer(o),d(t,n)();break;case"album":r.showItem(e.AlbumId,e.ServerId),d(t,n)();break;case"artist":r.showItem(e.ArtistItems[0].Id,e.ServerId),d(t,n)();break;case"playallfromhere":d(t,n)();break;case"queueallfromhere":d(t,n)();break;case"sync":require(["syncDialog"],function(e){e.showMenu({items:[{Id:o}]})}),d(t,n)();break;case"removefromplaylist":l.ajax({url:l.getUrl("Playlists/"+a.playlistId+"/Items",{EntryIds:[e.PlaylistItemId].join(",")}),type:"DELETE"}).then(function(){d(t,n,!0)()});break;case"removefromcollection":l.ajax({type:"DELETE",url:l.getUrl("Collections/"+a.collectionId+"/Items",{Ids:[e.Id].join(",")})}).then(function(){d(t,n,!0)()});break;case"canceltimer":m(l,e,t,n);break;default:c()}})}function m(e,t,a,r){require(["confirm"],function(s){s(n.translate("sharedcomponents#MessageConfirmRecordingCancellation"),n.translate("sharedcomponents#HeaderConfirmRecordingCancellation")).then(function(){o.show(),e.cancelLiveTvTimer(t.Id).then(function(){require(["toast"],function(e){e(n.translate("sharedcomponents#RecordingCancelled"))}),o.hide(),d(a,r,!0)()})})})}function u(e,n,t){var a=t?"queue":"play",r=0;n&&e.UserData&&e.UserData.PlaybackPositionTicks&&(r=e.UserData.PlaybackPositionTicks),s[a]("Program"==e.Type?{ids:[e.ChannelId],startPositionTicks:r}:{items:[e],startPositionTicks:r})}function p(e,n){return new Promise(function(t,a){var r=e.serverInfo().Id;"Timer"==n.Type?require(["recordingEditor"],function(e){e.show(n.Id,r).then(t,a)}):require(["metadataEditor"],function(e){e.show(n.Id,r).then(t,a)})})}function h(e,t){return new Promise(function(a,r){var s=n.translate("sharedcomponents#ConfirmDeleteItem"),o=n.translate("sharedcomponents#HeaderDeleteItem");require(["confirm"],function(n){n(s,o).then(function(){e.deleteItem(t).then(function(){a(!0)})},r)})})}function f(e,n){require(["refreshDialog"],function(t){new t({itemIds:[n],serverId:e.serverInfo().Id}).show()})}function y(e){return l(e).then(function(n){return new Promise(function(t,a){require(["actionsheet"],function(r){r.show({items:n,positionTo:e.positionTo}).then(function(n){c(e.item,n,e).then(t)},a)})})})}var I=null!=window.Dashboard;return{getCommands:l,show:y}});