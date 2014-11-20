using MediaBrowser.Mobile.Common.ViewModels;
using MediaBrowser.Mobile.Extensions;
using MediaBrowser.Model.ApiClient;
using MediaBrowser.Model.Dto;
using MediaBrowser.Model.Events;
using System;
using System.Threading.Tasks;
using Xamarin.Forms;
using Xamarin.Forms.Labs.Services;

namespace MediaBrowser.Mobile.Master
{
    public class MasterMenu : ContentPage
    {
        public MasterMenu(SessionViewModel viewModel)
        {
            Title = this.GetLocalizedString("TitleAppName");

            BindingContext = viewModel;

            LoadAnonymousContent();

            var connectionManager = Resolver.Resolve<IConnectionManager>();

            connectionManager.LocalUserSignIn += connectionManager_LocalUserSignIn;
            connectionManager.LocalUserSignOut += connectionManager_LocalUserSignOut;
        }

        void connectionManager_LocalUserSignOut(object sender, EventArgs e)
        {
            Device.BeginInvokeOnMainThread(LoadAnonymousContent);
        }

        void connectionManager_LocalUserSignIn(object sender, GenericEventArgs<UserDto> e)
        {
            Device.BeginInvokeOnMainThread(LoadUserContent);
        }

        private void LoadUserContent()
        {
            Content = new UserMenu();
        }

        private async void LoadAnonymousContent()
        {
            Content = new Label
            {
                Text = this.GetLocalizedString("LabelPleaseSignIn")
            };
        }
    }
}
