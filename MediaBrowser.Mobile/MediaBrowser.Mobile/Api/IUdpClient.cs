using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace MediaBrowser.Mobile.Api
{
    public interface IUdpClient
    {
        Task<byte[]> SendAndReceive(byte[] bytes, int timeoutMs, int port);
    }
}
