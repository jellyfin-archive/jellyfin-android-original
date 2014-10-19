using MediaBrowser.Model.ApiClient;
using System.Threading;
using Xamarin.Forms;
using Xamarin.Forms.Labs.Services;

namespace MediaBrowser.Mobile.Startup
{
    public class SplashPage : ContentPage
    {
        public ConnectionResult Result { get; set; }

        public SplashPage()
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

            Content = new Frame
            {
                Content = stackLayout
            };
        }
    }
}
