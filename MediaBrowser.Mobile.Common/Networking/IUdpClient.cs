using System.Threading.Tasks;

namespace MediaBrowser.Mobile.Common.Networking
{
    public interface IUdpClient
    {
        Task<byte[]> SendAndReceive(byte[] bytes, int timeoutMs, int port);
    }
}
