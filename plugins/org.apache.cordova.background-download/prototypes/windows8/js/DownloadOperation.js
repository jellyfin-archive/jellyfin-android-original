"use strict";

var config = {
    BackgroundTaskEntryPoint: "js\\backgroundtask.js",
    BackgroundTaskName: "BackgroundDownloaderTask",
    DownloadOperationsConfigName: "DownloadOperationsConfig"
};

//
// TODO
//
function isTaskAlreadyRegistered(taskName) {
    //
    // Loop through all background tasks and unregister any with SampleBackgroundTaskName or
    // SampleBackgroundTaskWithConditionName or timeTriggerTaskName.
    //
    var iter = Windows.ApplicationModel.Background.BackgroundTaskRegistration.allTasks.first();
    var hascur = iter.hasCurrent;
    while (hascur) {
        var cur = iter.current.value;
        if (cur.name === taskName) {
            return true;
        }
        hascur = iter.moveNext();
    }

    return false;
};

//
// Register a background task with the specified taskEntryPoint, taskName, trigger,
// and condition (optional).
//
function registerBackgroundTask(taskEntryPoint, taskName, trigger) {


    if (isTaskAlreadyRegistered(taskName)) {
        return;
    }
    
    var builder = new Windows.ApplicationModel.Background.BackgroundTaskBuilder();

    builder.name = taskName;
    builder.taskEntryPoint = taskEntryPoint;
    
    builder.setTrigger(trigger);
    builder.register();


};

var DownloadOperation = function (url, location, minInterval) {

    
    this.url = url;
    this.location = location;
    this.minInterval = minInterval;
    // id generated using url and target file location is used to make sure download operation is not scheduled twice
    this.id = url + '|' + location;

};

DownloadOperation.prototype.StartAsync = function() {

    var settings = Windows.Storage.ApplicationData.current.localSettings,
        operationsConfig = settings.values[config.DownloadOperationsConfigName] || '{}',
        operations = JSON.parse(operationsConfig);

    // already exists, skip
    if (operations[this.id]) {
        return;
    }

    operations[this.id] = this;

    settings.values[config.DownloadOperationsConfigName] = JSON.stringify(operations);
    
    //
    // Time triggered tasks can only run when the application is on the lock screen.
    // Time triggered tasks can be registered even if the application is not on the lockscreen.
    // 
    Windows.ApplicationModel.Background.BackgroundExecutionManager.requestAccessAsync();

    registerBackgroundTask(config.BackgroundTaskEntryPoint, config.BackgroundTaskName,
        new Windows.ApplicationModel.Background.TimeTrigger(15, false), null);
};

DownloadOperation.prototype.Stop = function() {

    var settings = Windows.Storage.ApplicationData.current.localSettings,
        operationsConfig = settings.values[config.DownloadOperationsConfigName] || '{}',
        operations = JSON.parse(operationsConfig);

    // not exist
    if (!operations[this.id]) {
        return;
    }

    delete operations[this.id];
    settings.values[config.DownloadOperationsConfigName] = JSON.stringify(operations);

};

window.DownloadOperation = DownloadOperation;