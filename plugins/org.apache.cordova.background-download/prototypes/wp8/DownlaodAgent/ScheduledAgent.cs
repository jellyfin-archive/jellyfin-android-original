#define DEBUG_AGENT

using System.Collections.Generic;
using System.IO.IsolatedStorage;
using System.Runtime.Serialization;
using Microsoft.Phone.Scheduler;
using Microsoft.Phone.Shell;
using System;
using System.Diagnostics;
using System.IO;
using System.Net;
using System.Threading.Tasks;
using System.Windows;
using Windows.Storage;

namespace DownloadAgent
{
    public class ScheduledAgent : ScheduledTaskAgent
    {
        private const string taskName = "DownloadAgent";
        private const string settingsName = "BackgroundDownloadOptions";

        private List<DownloadOptions> downloadOptions;

        [DataContract]
        public class DownloadOptions
        {
            [DataMember(Name = "id", IsRequired = true)]
            public string Id { get; set; }

            [DataMember(Name = "downloadUri", IsRequired = true)]
            public string DownloadUri { get; set; }

            [DataMember(Name = "resultFilePath", IsRequired = true)]
            public string ResultFilePath { get; set; }

            [DataMember(Name = "minInterval", IsRequired = false)]
            public int MinInterval { get; set; }
        }

        /// <remarks>
        /// ScheduledAgent constructor, initializes the UnhandledException handler
        /// </remarks>
        static ScheduledAgent()
        {
            // Subscribe to the managed exception handler
            Deployment.Current.Dispatcher.BeginInvoke(delegate
            {
                Application.Current.UnhandledException += UnhandledException;
            });
        }

        /// Code to execute on Unhandled Exceptions
        private static void UnhandledException(object sender, ApplicationUnhandledExceptionEventArgs e)
        {
            if (Debugger.IsAttached)
            {
                // An unhandled exception has occurred; break into the debugger
                Debugger.Break();
            }
        }

        /// <summary>
        /// Agent that runs a scheduled task
        /// </summary>
        /// <param name="task">
        /// The invoked task
        /// </param>
        /// <remarks>
        /// This method is called when a periodic or resource intensive task is invoked
        /// </remarks>
        protected override void OnInvoke(ScheduledTask task)
        {
            if (!(task is PeriodicTask) || (task.Name != taskName))
            {
                NotifyComplete();
                return;
            }

            downloadOptions = ReadDownloadOptions();

            if (downloadOptions == null)
            {
                NotifyComplete();
                return;
            }

            foreach (var opt in downloadOptions)
            {
                if ((DateTime.Now - task.LastScheduledTime).TotalMinutes > opt.MinInterval)
                {
                    Download(new Uri(opt.DownloadUri));    
                }                
            }                
        }        

        private List<DownloadOptions> ReadDownloadOptions()
        {
            try
            {
                IsolatedStorageSettings appSettings = IsolatedStorageSettings.ApplicationSettings;
                var options = (List<DownloadOptions>)appSettings[settingsName];
                
                return options;
            }
            catch (Exception ex)
            {
                return null;
            }            
        }

        private void Download(Uri uri)
        {          
            HttpWebRequest request = (HttpWebRequest)HttpWebRequest.Create(uri);
            request.BeginGetResponse(OnResponse, request);
        }

        private void OnResponse(IAsyncResult result)
        {
            HttpWebRequest request = result.AsyncState as HttpWebRequest;
            if (request != null)
            {
                try
                {
                    WebResponse response = request.EndGetResponse(result);                    
                    // Read the response into a Stream object.
                    Stream responseStream = response.GetResponseStream();

                    foreach (var opt in downloadOptions)
                    {
                        if (opt.DownloadUri == request.RequestUri.AbsoluteUri)
                        {
                            SaveToLocalFolderAsync(responseStream, opt.ResultFilePath);                            
                        }
                    }                    
                }
                catch (WebException e)
                {
                    
                }
            }
        }

        private void ShowAlert(string msg, string title)
        {
            try
            {
                // Launch a toast to show that the agent is running.
                // The toast will not be shown if the foreground application is running.
                ShellToast toast = new ShellToast();
                toast.Title = title;
                toast.Content = msg;
                toast.Show();
            }
            catch (Exception)
            {

            }            
        }

        private async Task ReadFile()
        {
            try
            {
                // Get the local folder.
                StorageFolder local = ApplicationData.Current.LocalFolder;

                if (local != null)
                {
                    // Get the file.
                    var file = await local.OpenStreamForReadAsync(downloadOptions[0].ResultFilePath);

                    // Read the data.
                    using (StreamReader streamReader = new StreamReader(file))
                    {
                        var content = streamReader.ReadToEnd();
                        ShowAlert(content, "Download completed");
                        
                        NotifyComplete();
                        // If debugging is enabled, launch the agent again in one minute.
#if DEBUG_AGENT
                        ScheduledActionService.LaunchForTest(taskName, TimeSpan.FromSeconds(15));
#endif
                    }
                }
            }
            catch (Exception)
            {

            }            
        }

        private async Task SaveToLocalFolderAsync(Stream file, string fileName)
        {
            try
            {
                StorageFolder localFolder = ApplicationData.Current.LocalFolder;
                StorageFile storageFile = await localFolder.CreateFileAsync(fileName, CreationCollisionOption.ReplaceExisting);
                using (Stream outputStream = await storageFile.OpenStreamForWriteAsync())
                {
                    await file.CopyToAsync(outputStream);
                    ReadFile();
                }
            }
            catch (Exception)
            {
                
            }            
        }
    }
}