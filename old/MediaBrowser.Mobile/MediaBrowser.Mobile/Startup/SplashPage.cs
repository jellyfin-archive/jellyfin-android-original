using System.Threading.Tasks;
using MediaBrowser.Mobile.Extensions;
using MediaBrowser.Mobile.Master;
using MediaBrowser.Model.ApiClient;
using System.Threading;
using Xamarin.Forms;
using Xamarin.Forms.Labs.Services;

namespace MediaBrowser.Mobile.Startup
{
    public class SplashPage : ContentPage
    {
        public ConnectionResult ConnectionResult { get; private set; }
        private readonly MasterPage _masterPage;

        public SplashPage(MasterPage masterPage)
        {
            _masterPage = masterPage;
            Title = this.GetLocalizedString("TitleAppName");
            Content = GetSplashContent();
        }

        private View GetSplashContent()
        {
            var stackLayout = new StackLayout
            {
                Orientation = StackOrientation.Vertical,
                VerticalOptions = LayoutOptions.Center,
                HorizontalOptions = LayoutOptions.Center,
                Spacing = 15
            };

            stackLayout.Children.Add(new Image
            {
                Source = "logo.png",
                VerticalOptions = LayoutOptions.Center,
                HorizontalOptions = LayoutOptions.Center,
                WidthRequest = 64
            });

            ActivityIndicator loading = new ActivityIndicator
            {
                IsRunning = true
            };

            stackLayout.Children.Add(loading);

            return new Frame
            {
                Content = stackLayout
            };
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();

            Connect();
        }

        private async void Connect()
        {
            var connectionManager = Resolver.Resolve<IConnectionManager>();

            try
            {
                ConnectionResult = await connectionManager.Connect(CancellationToken.None);
            }
            catch
            {
                ConnectionResult = new ConnectionResult
                {
                    State = ConnectionState.Unavailable
                };
            }

            await Task.Delay(1000);

            await Navigation.PopModalAsync();

            _masterPage.ProcessSplashResult(ConnectionResult);
        }
    }
}
