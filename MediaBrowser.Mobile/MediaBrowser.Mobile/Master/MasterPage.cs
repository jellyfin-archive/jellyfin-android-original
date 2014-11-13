using MediaBrowser.Mobile.Common.ViewModels;
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

            Detail = new ContentPage()
            {
                Title = "Media Browser"
            };

            Title = "Media Browser";
        }


        protected override async void OnAppearing()
        {
            base.OnAppearing();

            ShowStartupFlow();
        }

        private async void ShowStartupFlow()
        {
            Detail = new SplashPage();

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
                Detail = new NavigationPage(new WelcomePage(this));
            }
            else if (result.State == ConnectionState.ServerSelection)
            {
                Detail = new NavigationPage(new ServerSelectionPage());
            }
            else if (result.State == ConnectionState.ServerSignIn)
            {
                Detail = new NavigationPage(new ServerSignInPage(result.Servers[0], result.ApiClient, this));
            }
            else if (result.State == ConnectionState.SignedIn)
            {
                Detail = new NavigationPage(new HomePage());
            }
            else if (result.State == ConnectionState.ConnectSignIn)
            {
                Detail = new NavigationPage(new ConnectPage(this));
            }
        }
    }
}
