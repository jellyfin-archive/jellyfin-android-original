using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using Android.App;
using Android.Content;
using Android.OS;
using Android.Runtime;
using Android.Views;
using Android.Widget;
using MediaBrowser.Mobile.Api;

namespace MediaBrowser.Mobile.Droid.Api
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