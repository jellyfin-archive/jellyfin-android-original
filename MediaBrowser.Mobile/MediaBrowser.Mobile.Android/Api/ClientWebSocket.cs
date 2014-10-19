using MediaBrowser.Model.ApiClient;
using System;
using System.Threading;
using System.Threading.Tasks;
using WebSocket4Net;

namespace MediaBrowser.Mobile.Droid.Api
{
    public class ClientWebSocket : IClientWebSocket
    {
        /// <summary>
        /// The _socket
        /// </summary>
        private WebSocket _socket;

        /// <summary>
        /// Gets or sets the state.
        /// </summary>
        /// <value>The state.</value>
        public Model.Net.WebSocketState State
        {
            get
            {

                switch (_socket.State)
                {
                    case WebSocketState.Closed:
                        return Model.Net.WebSocketState.Closed;
                    case WebSocketState.Closing:
                        return Model.Net.WebSocketState.Closed;
                    case WebSocketState.Connecting:
                        return Model.Net.WebSocketState.Connecting;
                    case WebSocketState.None:
                        return Model.Net.WebSocketState.None;
                    case WebSocketState.Open:
                        return Model.Net.WebSocketState.Open;
                    default:
                        return Model.Net.WebSocketState.None;
                }
            }
        }

        /// <summary>
        /// Connects the async.
        /// </summary>
        /// <param name="url">The URL.</param>
        /// <param name="cancellationToken">The cancellation token.</param>
        /// <returns>Task.</returns>
        public Task ConnectAsync(string url, CancellationToken cancellationToken)
        {
            var taskCompletionSource = new TaskCompletionSource<bool>();

            try
            {
                _socket = new WebSocket(url);

                _socket.MessageReceived += websocket_MessageReceived;

                _socket.Open();

                _socket.Opened += (sender, args) => taskCompletionSource.TrySetResult(true);
                _socket.Closed += _socket_Closed;
            }
            catch (Exception ex)
            {
                _socket = null;

                taskCompletionSource.TrySetException(ex);
            }

            return taskCompletionSource.Task;
        }

        /// <summary>
        /// Handles the WebSocketClosed event of the _socket control.
        /// </summary>
        /// <param name="sender">The source of the event.</param>
        /// <param name="e">The <see cref="EventArgs"/> instance containing the event data.</param>
        void _socket_Closed(object sender, EventArgs e)
        {
            if (Closed != null)
            {
                Closed(this, EventArgs.Empty);
            }
        }

        /// <summary>
        /// Handles the MessageReceived event of the websocket control.
        /// </summary>
        /// <param name="sender">The source of the event.</param>
        /// <param name="e">The <see cref="MessageReceivedEventArgs" /> instance containing the event data.</param>
        void websocket_MessageReceived(object sender, MessageReceivedEventArgs e)
        {
            if (OnReceive != null)
            {
                OnReceive(e.Message);
            }
        }

        /// <summary>
        /// Gets or sets the receive action.
        /// </summary>
        /// <value>The receive action.</value>
        public Action<byte[]> OnReceiveBytes { get; set; }

        /// <summary>
        /// Gets or sets the on receive.
        /// </summary>
        /// <value>The on receive.</value>
        public Action<string> OnReceive { get; set; }

        /// <summary>
        /// Sends the async.
        /// </summary>
        /// <param name="bytes">The bytes.</param>
        /// <param name="type">The type.</param>
        /// <param name="endOfMessage">if set to <c>true</c> [end of message].</param>
        /// <param name="cancellationToken">The cancellation token.</param>
        /// <returns>Task.</returns>
        public Task SendAsync(byte[] bytes, Model.Net.WebSocketMessageType type, bool endOfMessage, CancellationToken cancellationToken)
        {
            return Task.Run(() => _socket.Send(bytes, 0, bytes.Length), cancellationToken);
        }

        /// <summary>
        /// Performs application-defined tasks associated with freeing, releasing, or resetting unmanaged resources.
        /// </summary>
        public void Dispose()
        {
            if (_socket != null)
            {
                var state = State;

                if (state == Model.Net.WebSocketState.Open || state == Model.Net.WebSocketState.Connecting)
                {
                    _socket.Close();
                }

                _socket = null;
            }
        }

        /// <summary>
        /// Occurs when [closed].
        /// </summary>
        public event EventHandler Closed;
   }
}