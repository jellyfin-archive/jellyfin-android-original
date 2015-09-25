using System;
using System.IO.IsolatedStorage;
using System.Linq;
using Microsoft.Phone.BackgroundTransfer;
using System.Collections;
using System.Collections.Generic;

namespace WPCordovaClassLib.Cordova.Commands
{
    class Download
    {
        private string _uriString;
        private string _filePath;
        private string _callbackId;

        public string UriString
        {
            get { return _uriString; }
        }
        public string FilePath
        {
            get { return _filePath; }
        }
        public string CallbackId
        {
            get { return _callbackId; }
        }

        public Download(string uriString, string filePath, string callbackId)
        {
            _uriString = uriString;
            _filePath = filePath;
            _callbackId = callbackId;
        }
    }

    /// <summary>
    /// TODO comments
    /// TODO concurrent operations support
    /// </summary>
    class BackgroundDownload : BaseCommand
    {
        private Dictionary<string, Download> _activDownloads = new Dictionary<string, Download>();

        public void startAsync(string options)
        {
            try
            {
                var optStings = JSON.JsonHelper.Deserialize<string[]>(options);
                
                var uriString = optStings[0];
                var filePath = optStings[1];


                if (_activDownloads.ContainsKey(uriString))
                {
                    return;
                }
    
                _activDownloads.Add(uriString, new Download(uriString, filePath, optStings[2]));
                
               
                var requestUri = new Uri(uriString);

                BackgroundTransferRequest transfer = FindTransferByUri(requestUri);

                if (transfer == null)
                {
                    // "shared\transfers" is the only working location for BackgroundTransferService download
                    // we use temporary file name to download content and then move downloaded file to the requested location
                    var downloadLocation = new Uri(@"\shared\transfers\" + Guid.NewGuid(), UriKind.Relative);

                    transfer = new BackgroundTransferRequest(requestUri, downloadLocation);

                    // Tag is used to make sure we run single background transfer for this file
                    transfer.Tag = uriString;

                    BackgroundTransferService.Add(transfer);
                }

                if (transfer.TransferStatus == TransferStatus.Completed)
                {
                    // file was already downloaded while we were in background and we didn't report this
                    MoveFile(transfer);
                    BackgroundTransferService.Remove(transfer);
                    DispatchCommandResult(new PluginResult(PluginResult.Status.OK));
                }
                else
                {
                        transfer.TransferProgressChanged += ProgressChanged;
                        transfer.TransferStatusChanged += TransferStatusChanged;
                }
                
            }
            catch (Exception ex)
            {               
                DispatchCommandResult(new PluginResult(PluginResult.Status.ERROR, ex.Message));
            }
        }


        public void stop(string options)
        {
            var optStings = JSON.JsonHelper.Deserialize<string[]>(options);
            var transfer = FindTransferByUri(new Uri(optStings[0]));
            try
            {

                if (transfer != null)
                {
                    var request = BackgroundTransferService.Find(transfer.RequestId);
                    if (request != null)
                    {
                        // stops transfer and triggers TransferStatusChanged event
                        BackgroundTransferService.Remove(request);
                    }
                }

                DispatchCommandResult(new PluginResult(PluginResult.Status.OK));
            }
            catch (Exception ex)
            {
                DispatchCommandResult(new PluginResult(PluginResult.Status.ERROR, ex.Message));
            }
        }

        private void TransferStatusChanged(object sender, BackgroundTransferEventArgs backgroundTransferEventArgs)
        {
            var transfer = backgroundTransferEventArgs.Request;

            if (transfer.TransferStatus == TransferStatus.Completed)
            {
                // If the status code of a completed _transfer is 200 or 206, the _transfer was successful
                if (transfer.StatusCode == 200 || transfer.StatusCode == 206)
                {
                    MoveFile(transfer);
                    DispatchCommandResult(new PluginResult(PluginResult.Status.OK), _activDownloads[transfer.Tag].CallbackId);

                    _activDownloads.Remove(transfer.Tag);
                }
                else
                {
                    var strErrorMessage = transfer.TransferError != null ? transfer.TransferError.Message : "Unspecified transfer error";
                    DispatchCommandResult(new PluginResult(PluginResult.Status.ERROR, strErrorMessage), _activDownloads[transfer.Tag].CallbackId);
                    //_activDownload.Remove(transfer.Tag);
                }
                CleanUp(transfer);
            }
        }

        private static BackgroundTransferRequest FindTransferByLocalPath(string tag)
        {
            return BackgroundTransferService.Requests.FirstOrDefault(r => r.Tag == tag);
        }

        private static BackgroundTransferRequest FindTransferByUri(Uri uri)
        {
            return BackgroundTransferService.Requests.FirstOrDefault(r => r.RequestUri.OriginalString == uri.OriginalString);
        }

        void ProgressChanged(object sender, BackgroundTransferEventArgs e)
        {
            var progressUpdate = new PluginResult(PluginResult.Status.OK);

            progressUpdate.KeepCallback = true;
            progressUpdate.Message = String.Format("{{\"progress\":{{\"bytesReceived\":{0}, \"totalBytesToReceive\":{1} }}}}", e.Request.BytesReceived, e.Request.TotalBytesToReceive);

            DispatchCommandResult(progressUpdate, _activDownloads[((BackgroundTransferRequest)sender).Tag].CallbackId);
        }

        private void MoveFile(BackgroundTransferRequest transfer)
        {
            // The downloaded content is moved into the right place
            using (var isoStore = IsolatedStorageFile.GetUserStoreForApplication())
            {
                string filename = _activDownloads[transfer.Tag].FilePath;
                if (isoStore.FileExists(filename))
                {
                    isoStore.DeleteFile(filename);
                }
                isoStore.MoveFile(transfer.DownloadLocation.OriginalString, filename);
            }
        }

        private void CleanUp( BackgroundTransferRequest transfer)
        {
            if (transfer != null)
            {
                transfer.TransferProgressChanged -= ProgressChanged;
                transfer.TransferStatusChanged -= TransferStatusChanged;

                if (BackgroundTransferService.Find(transfer.RequestId) != null)
                {
                    BackgroundTransferService.Remove(transfer);
                }

                transfer = null;
            }
        }
    }
}