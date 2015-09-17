#define DEBUG_AGENT

using System.Collections.Generic;
using System.IO.IsolatedStorage;
using System.Runtime.Serialization;
using DownloadAgent;
using Microsoft.Phone.Scheduler;
using System;
using WPCordovaClassLib.Cordova.Commands;
using WPCordovaClassLib.Cordova.JSON;

namespace CordovaWP8_3._3._02.cordovalib.Plugins
{       
    class BackgroundDownloader : BaseCommand
    {
        private PeriodicTask periodicTask;

        private const string periodicTaskName = "DownloadAgent";
        private const string periodicTaskDescription = "Downloads files from background";
        private const string settingsName = "BackgroundDownloadOptions";

        

        private void StartPeriodicDownloadAgent()
        {
            // Obtain a reference to the period task, if one exists
            this.periodicTask = ScheduledActionService.Find(periodicTaskName) as PeriodicTask;

            // If the task already exists and background agents are enabled for the
            // application, you must remove the task and then add it again to update 
            // the schedule
            if (this.periodicTask != null)
            {
                BackgroundDownloader.RemoveAgent(periodicTaskName);
            }

            this.periodicTask = new PeriodicTask(periodicTaskName);

            // The description is required for periodic agents. This is the string that the user
            // will see in the background services Settings page on the device.
            this.periodicTask.Description = periodicTaskDescription;

            // Place the call to Add in a try block in case the user has disabled agents.
            try
            {
                ScheduledActionService.Add(this.periodicTask);
                // If debugging is enabled, use LaunchForTest to launch the agent in one minute.
#if(DEBUG_AGENT)
    ScheduledActionService.LaunchForTest(periodicTaskName, TimeSpan.FromSeconds(15));
#endif
            }
            catch (InvalidOperationException exception)
            {
                if (exception.Message.Contains("BNS Error: The maximum number of ScheduledActions of this type have already been added."))
                {
                    // No user action required. The system prompts the user when the hard limit of periodic tasks has been reached.

                }
            }
            catch (SchedulerServiceException)
            {
                // No user action required.
            }
        }

        private static void RemoveAgent(string name)
        {
            try
            {
                ScheduledActionService.Remove(name);
            }
            catch (Exception)
            {

            }
        }

        private void SaveDownloadOptions(string options)
        {
            try
            {
                ScheduledAgent.DownloadOptions downloadOptions;

                //string[] args = JsonHelper.Deserialize<string[]>(options);
                downloadOptions = JsonHelper.Deserialize<ScheduledAgent.DownloadOptions>(options);

                IsolatedStorageSettings settings = IsolatedStorageSettings.ApplicationSettings;
                if (!settings.Contains(settingsName))
                {
                    List<ScheduledAgent.DownloadOptions> allDownloadOptions = new List<ScheduledAgent.DownloadOptions>() {downloadOptions};
                    settings.Add(settingsName, allDownloadOptions);
                }
                else
                {
                    List<ScheduledAgent.DownloadOptions> allDownloadOptions = (List<ScheduledAgent.DownloadOptions>)settings[settingsName];

                    foreach (var opt in allDownloadOptions)
                    {
                        if (opt.Id == downloadOptions.Id)
                        {
                            return;
                        }
                    }

                    allDownloadOptions.Add(downloadOptions);
                    settings[settingsName] = allDownloadOptions;
                }
                settings.Save();
            }
            catch (Exception ex)
            {                
                
            }            
        }

        public void CreateDownload(string options)
        {
            try
            {
                this.SaveDownloadOptions(options);
                this.StartPeriodicDownloadAgent();
            }
            catch (Exception)
            {
                
            }            
        }
    }
}
