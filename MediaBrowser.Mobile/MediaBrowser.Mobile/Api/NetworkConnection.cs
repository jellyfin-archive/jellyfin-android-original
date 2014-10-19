using MediaBrowser.ApiInteraction;
using System.Threading;
using System.Threading.Tasks;
using Xamarin.Forms.Labs;
using NetworkStatus = MediaBrowser.ApiInteraction.NetworkStatus;

namespace MediaBrowser.Mobile.Api
{
    public class NetworkConnection : INetworkConnection
    {
        private readonly IDevice _device;
        private readonly IWakeOnLan _wakeOnLan;

        public NetworkConnection(IDevice device, IWakeOnLan wakeOnLan)
        {
            _device = device;
            _wakeOnLan = wakeOnLan;
        }

        public NetworkStatus GetNetworkStatus()
        {
            var network = _device.Network;

            var status = new NetworkStatus();

            var deviceStatus = network.InternetConnectionStatus();

            status.IsNetworkAvailable = deviceStatus != Xamarin.Forms.Labs.Services.NetworkStatus.NotReachable;
            //status.IsLocalNetworkAvailable = deviceStatus == Xamarin.Forms.Labs.Services.NetworkStatus.ReachableViaWiFiNetwork;

            return status;
        }

        public Task SendWakeOnLan(string macAddress, int port, CancellationToken cancellationToken)
        {
            return _wakeOnLan.SendWakeOnLan(macAddress, port, cancellationToken);
        }

        public Task SendWakeOnLan(string macAddress, string ipAddress, int port, CancellationToken cancellationToken)
        {
            return _wakeOnLan.SendWakeOnLan(macAddress, ipAddress, port, cancellationToken);
        }
    }
}
