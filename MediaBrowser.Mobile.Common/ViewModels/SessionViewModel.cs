using MediaBrowser.Model.ApiClient;
using MediaBrowser.Model.Connect;
using MediaBrowser.Model.Dto;
using MediaBrowser.Model.Events;
using System;
using Xamarin.Forms.Labs.Services;

namespace MediaBrowser.Mobile.Common.ViewModels
{
    public class SessionViewModel : BaseViewModel
    {
        private UserDto _localUser;
        private ConnectUser _connectUser;

        private string _username = string.Empty;
        public string Username
        {
            get { return _username; }
            set
            {
                _username = value;
                OnPropertyChanged();
            }
        }

        public SessionViewModel()
        {
            var connectionManager = Resolver.Resolve<IConnectionManager>();

            connectionManager.LocalUserSignIn += connectionManager_LocalUserSignIn;
            connectionManager.LocalUserSignOut += connectionManager_LocalUserSignOut;
            connectionManager.ConnectUserSignIn += connectionManager_ConnectUserSignIn;
            connectionManager.ConnectUserSignOut += connectionManager_ConnectUserSignOut;

            UpdateValues();
        }

        void connectionManager_ConnectUserSignOut(object sender, EventArgs e)
        {
            _connectUser = null;
            UpdateValues();
        }

        void connectionManager_ConnectUserSignIn(object sender, GenericEventArgs<ConnectUser> e)
        {
            _connectUser = e.Argument;
            UpdateValues();
        }

        void connectionManager_LocalUserSignOut(object sender, EventArgs e)
        {
            _localUser = null;
            UpdateValues();
        }

        void connectionManager_LocalUserSignIn(object sender, GenericEventArgs<UserDto> e)
        {
            _localUser = e.Argument;
            UpdateValues();
        }

        private void UpdateValues()
        {
            Username = _connectUser == null
                ? (_localUser == null ? "ddd" : _localUser.Name)
                : _connectUser.Name;
        }
    }
}
