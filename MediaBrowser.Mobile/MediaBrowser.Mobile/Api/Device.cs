using MediaBrowser.Model.ApiClient;
using MediaBrowser.Model.Devices;
using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace MediaBrowser.Mobile.Api
{
    public class Device : IDevice
    {
        public event EventHandler<EventArgs> ResumeFromSleep;

        public Device(Xamarin.Forms.Labs.IDevice device)
        {
            DeviceId = device.Id;
            DeviceName = device.Name;
        }

        public string DeviceId { get; private set; }

        public string DeviceName { get; private set; }

        public IEnumerable<LocalFileInfo> GetLocalPhotos()
        {
            throw new NotImplementedException();
        }

        public IEnumerable<LocalFileInfo> GetLocalVideos()
        {
            throw new NotImplementedException();
        }

        public Task UploadFile(LocalFileInfo file, IApiClient apiClient, CancellationToken cancellationToken)
        {
            throw new NotImplementedException();
        }
    }
}
