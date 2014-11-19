using MediaBrowser.Mobile.Common.ViewModels;
using MediaBrowser.Mobile.Extensions;
using MediaBrowser.Mobile.Home;
using MediaBrowser.Mobile.Startup;
using MediaBrowser.Model.ApiClient;
using System.Threading;
using Xamarin.Forms;
using Xamarin.Forms.Labs.Services;

namespace MediaBrowser.Mobile.Master
{
    public class MasterPage : MasterDetailPage
    {
        public MasterPage()
        {
            Master = new MasterMenu(new SessionViewModel());

            Detail = new NavigationPage(new HomePage())
            {
                Title = this.GetLocalizedString("TitleMediaBrowser")
            };

            Title = this.GetLocalizedString("TitleMediaBrowser");
        }

        protected override async void OnAppearing()
        {
            base.OnAppearing();

            ShowStartupFlow();
        }

        private async void ShowStartupFlow()
        {
            await Navigation.PushModalAsync(new NavigationPage(new SplashPage()));
            return;
            var connectionManager = Resolver.Resolve<IConnectionManager>();
            ConnectionResult result;

            try
            {
                result = await connectionManager.Connect(CancellationToken.None);
            }
            catch
            {
                result = new ConnectionResult
                {
                    State = ConnectionState.Unavailable
                };
            }

            if (result.State == ConnectionState.Unavailable)
            {
                await Detail.Navigation.PushAsync(new WelcomePage(this));
            }
            else if (result.State == ConnectionState.ServerSelection)
            {
                if (result.Servers.Count == 0)
                {
                    Detail = new NavigationPage(new ServerEntryPage(this));
                }
                else
                {
                    Detail = new NavigationPage(new ServerSelectionPage(this));
                }
            }
            else if (result.State == ConnectionState.ServerSignIn)
            {
                await Detail.Navigation.PushAsync(new ServerSignInPage(result.Servers[0], result.ApiClient, this));
            }
            else if (result.State == ConnectionState.SignedIn)
            {
                await Detail.Navigation.PushAsync(new HomePage());
            }
            else if (result.State == ConnectionState.ConnectSignIn)
            {
                await Detail.Navigation.PushAsync(new ConnectPage(this));
            }
        }

        public void OnStartupFlowComplete()
        {

        }
    }
}
