using MediaBrowser.Mobile.Api;
using System;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;

namespace MediaBrowser.Mobile.Droid.Api
{
    public class UdpService : IUdpClient
    {
        public Task<byte[]> SendAndReceive(byte[] bytes, int timeoutMs, int port)
        {
            var taskCompletionSource = new TaskCompletionSource<byte[]>();

            var timeoutToken = new CancellationTokenSource(timeoutMs).Token;

            timeoutToken.Register(() => taskCompletionSource.TrySetCanceled());

            SendAndReceive(bytes, timeoutMs, port, taskCompletionSource);

            return taskCompletionSource.Task;
        }

        private async void SendAndReceive(byte[] bytes, int timeoutMs, int port, TaskCompletionSource<byte[]> taskCompletionSource)
        {
            using (var client = new UdpClient(new IPEndPoint(IPAddress.Any, GetRandomUnusedPort())))
            {
                client.Client.ReceiveTimeout = timeoutMs;

                // Send it - must be IPAddress.Broadcast, 7359
                var targetEndPoint = new IPEndPoint(IPAddress.Broadcast, port);

                try
                {
                    // Send the broadcast
                    await client.SendAsync(bytes, bytes.Length, targetEndPoint).ConfigureAwait(false);

                    // Get a result back
                    var result = await client.ReceiveAsync().ConfigureAwait(false);

                    if (result.RemoteEndPoint.Port == targetEndPoint.Port)
                    {
                        taskCompletionSource.SetResult(result.Buffer);
                    }

                    taskCompletionSource.SetException(new ArgumentException("Unexpected response"));
                }
                catch (Exception ex)
                {
                    taskCompletionSource.TrySetException(ex);
                }
            }
        }

        /// <summary>
        /// Gets a random port number that is currently available
        /// </summary>
        private static int GetRandomUnusedPort()
        {
            var listener = new TcpListener(IPAddress.Any, 0);
            listener.Start();
            var port = ((IPEndPoint)listener.LocalEndpoint).Port;
            listener.Stop();
            return port;
        }
    }
}