using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using MediaBrowser.Mobile.Api;
using MediaBrowser.Mobile.Common.Networking;
using MonoTouch.Foundation;
using MonoTouch.UIKit;

namespace MediaBrowser.Mobile.iOS.Api
{
    public class WakeOnLan : IWakeOnLan
    {
        public Task SendWakeOnLan(string macAddress, string ipAddress, int port, CancellationToken cancellationToken)
        {
            throw new NotImplementedException();
        }

        public Task SendWakeOnLan(string macAddress, int port, CancellationToken cancellationToken)
        {
            throw new NotImplementedException();
        }
    }
}