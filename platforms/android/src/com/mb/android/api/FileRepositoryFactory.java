package com.mb.android.api;

import mediabrowser.apiinteraction.android.sync.ISyncRepositoryFactory;
import mediabrowser.apiinteraction.sync.data.IFileRepository;

/**
 * Created by Luke on 6/9/2015.
 */
public class FileRepositoryFactory implements ISyncRepositoryFactory {

    private IFileRepository fileRepository;

    public FileRepositoryFactory(IFileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    @Override
    public IFileRepository getFileRepository() {
        return fileRepository;
    }
}
