using MediaBrowser.Model.ApiClient;
using MediaBrowser.Model.Connect;
using MediaBrowser.Model.Dto;
using MediaBrowser.Model.Entities;
using MediaBrowser.Model.Events;
using System;
using Xamarin.Forms;
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

        private string _userImageUrl = string.Empty;
        public string UserImageUrl
        {
            get { return _userImageUrl; }
            set
            {
                _userImageUrl = value;
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
            Device.BeginInvokeOnMainThread(UpdateValuesInternal);
        }

        private void UpdateValuesInternal()
        {
            Username = _connectUser == null
                ? (_localUser == null ? null : _localUser.Name)
                : _connectUser.Name;

            UserImageUrl = _connectUser == null
                ? (_localUser == null ? null : GetLocalUserImageUrl(_localUser))
                : _connectUser.ImageUrl;
        }

        private string GetLocalUserImageUrl(UserDto user)
        {
            var connectionManager = Resolver.Resolve<IConnectionManager>();

            var apiClient = connectionManager.GetApiClient(user);

            if (user.HasPrimaryImage)
            {
                return apiClient.GetUserImageUrl(user, new ImageOptions
                {
                    ImageType = ImageType.Primary
                });
            }

            return null;
        }
    }
}
