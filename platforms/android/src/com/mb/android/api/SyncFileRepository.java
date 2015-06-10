package com.mb.android.api;

import android.content.Context;
import android.os.Environment;

import com.mb.android.preferences.PreferencesProvider;

import java.io.File;

import mediabrowser.apiinteraction.android.sync.data.AndroidFileRepository;
import mediabrowser.model.logging.ILogger;

/**
 * Created by Luke on 6/9/2015.
 */
public class SyncFileRepository extends AndroidFileRepository {
    public SyncFileRepository(Context context, ILogger logger) {
        super(context, logger);
    }

    @Override
    protected String getBasePath() {

        String path = new PreferencesProvider(context, Logger).get("syncPath");

        if (path != null){
            File file = new File(path);

            if (file.exists()){
                return path;
            }
        }

        return super.getBasePath();
    }
}
