using System.Threading;
using System.Threading.Tasks;

namespace MediaBrowser.Mobile.Api
{
    public interface IWakeOnLan
    {
        Task SendWakeOnLan(string macAddress, int port, CancellationToken cancellationToken);
        Task SendWakeOnLan(string macAddress, string ipAddress, int port, CancellationToken cancellationToken);
    }
}
