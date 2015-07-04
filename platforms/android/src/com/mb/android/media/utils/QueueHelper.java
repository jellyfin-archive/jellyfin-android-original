package com.mb.android.media.utils;

import android.media.MediaMetadata;
import android.media.session.MediaSession;

import com.mb.android.media.provider.MusicProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import mediabrowser.model.dto.BaseItemDto;
import mediabrowser.model.dto.MediaSourceInfo;
import mediabrowser.model.logging.ILogger;

/**
 * Utility class to help on queue related tasks.
 */
public class QueueHelper {

    public static List<MediaSession.QueueItem> getPlayingQueue(String mediaId, MusicProvider musicProvider, ILogger logger) {

        return null;
    }

    public static List<MediaSession.QueueItem> getPlayingQueue(BaseItemDto item, MediaSourceInfo mediaSource, String path, String posterUrl, MusicProvider provider, ILogger logger) {

        String mediaId = item.getId();

        List<MediaMetadata> tracks = new ArrayList<MediaMetadata>();

        String album = item.getAlbum() == null ? "" : item.getAlbum();
        String artist = item.getArtistItems() != null && item.getArtistItems().size() > 0 ? item.getArtistItems().get(0).getName() : "";
        String title = item.getName() == null ? "" : item.getName();
        String genre = item.getGenres() != null && item.getGenres().size() > 0 ? item.getGenres().get(0) : "";

        Long duration = mediaSource.getRunTimeTicks() == null ?
                null :
                mediaSource.getRunTimeTicks() / 10000;

        MediaMetadata metadata =new MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, mediaId)
                .putString(MusicProvider.CUSTOM_METADATA_TRACK_SOURCE, path)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, artist)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, duration)
                .putString(MediaMetadata.METADATA_KEY_GENRE, genre)
                .putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, posterUrl)
                .putString(MediaMetadata.METADATA_KEY_TITLE, title)
                .putLong(MediaMetadata.METADATA_KEY_TRACK_NUMBER, item.getIndexNumber())
                //.putLong(MediaMetadata.METADATA_KEY_NUM_TRACKS, totalTrackCount)
                .build();

        provider.addOrUpdate(mediaId, metadata);

        tracks.add(metadata);

        return convertToQueue(tracks);
    }

    public static List<MediaSession.QueueItem> getPlayingQueueFromSearch(String query, MusicProvider musicProvider, ILogger logger) {

        logger.Debug("Creating playing queue for musics from search %s", query);

        return null;
    }

    public static int getMusicIndexOnQueue(Iterable<MediaSession.QueueItem> queue,
                                           String mediaId) {
        int index = 0;
        for (MediaSession.QueueItem item : queue) {
            if (mediaId.equals(item.getDescription().getMediaId())) {
                return index;
            }
            index++;
        }
        return -1;
    }

    public static int getMusicIndexOnQueue(Iterable<MediaSession.QueueItem> queue,
                                           long queueId) {
        int index = 0;
        for (MediaSession.QueueItem item : queue) {
            if (queueId == item.getQueueId()) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static List<MediaSession.QueueItem> convertToQueue(
            Iterable<MediaMetadata> tracks, String... categories) {
        List<MediaSession.QueueItem> queue = new ArrayList<MediaSession.QueueItem>();
        int count = 0;
        for (MediaMetadata track : tracks) {

            // We create a hierarchy-aware mediaID, so we know what the queue is about by looking
            // at the QueueItem media IDs.
            String hierarchyAwareMediaID = track.getDescription().getMediaId();

            MediaMetadata trackCopy = new MediaMetadata.Builder(track)
                    .putString(MediaMetadata.METADATA_KEY_MEDIA_ID, hierarchyAwareMediaID)
                    .build();

            // We don't expect queues to change after created, so we use the item index as the
            // queueId. Any other number unique in the queue would work.
            MediaSession.QueueItem item = new MediaSession.QueueItem(
                    trackCopy.getDescription(), count++);
            queue.add(item);
        }
        return queue;

    }

    public static boolean isIndexPlayable(int index, List<MediaSession.QueueItem> queue) {
        return (queue != null && index >= 0 && index < queue.size());
    }
}