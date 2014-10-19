using MediaBrowser.Mobile.Extensions;
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
            Master = new ContentPage()
            {
                Title = "Media Browser",

                Content = new Label { Text = "Please sign in"}
            };

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
                Detail = new NavigationPage(new WelcomePage());
            }
            else
            {
                await this.ProcessConnectionResult(result);
            }
        }
    }
}
