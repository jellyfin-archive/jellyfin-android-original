using MediaBrowser.ApiInteraction;
using MediaBrowser.Model.ApiClient;
using MediaBrowser.Model.Serialization;
using System;
using System.Collections.Generic;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace MediaBrowser.Mobile.Api
{
    public class ServerLocator : IServerLocator
    {
        private readonly IJsonSerializer _jsonSerializer;
        private readonly Func<IUdpClient> _udpClientFactory;

        public ServerLocator(IJsonSerializer jsonSerializer, Func<IUdpClient> udpClientFactory)
        {
            _jsonSerializer = jsonSerializer;
            _udpClientFactory = udpClientFactory;
        }

        public async Task<List<ServerDiscoveryInfo>> FindServers(int timeoutMs, CancellationToken cancellationToken)
        {
            var client = _udpClientFactory();

            // Construct the message the server is expecting
            var bytes = Encoding.UTF8.GetBytes("who is MediaBrowserServer_v2?");

            var result = await client.SendAndReceive(bytes, timeoutMs, 7359).ConfigureAwait(false);

            // Convert bytes to text
            var json = Encoding.UTF8.GetString(result, 0, result.Length);

            if (!string.IsNullOrEmpty(json))
            {
                var info = _jsonSerializer.DeserializeFromString<ServerDiscoveryInfo>(json);

                return new List<ServerDiscoveryInfo> { info };
            }

            return new List<ServerDiscoveryInfo>();
        }
    }
}
