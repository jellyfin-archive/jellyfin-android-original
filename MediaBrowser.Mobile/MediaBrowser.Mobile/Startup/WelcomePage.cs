using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using MediaBrowser.Mobile.Extensions;
using MediaBrowser.Mobile.Master;
using Xamarin.Forms;

namespace MediaBrowser.Mobile.Startup
{
    public class WelcomePage : ContentPage
    {
        private readonly MasterPage _master;
        public WelcomePage(MasterPage master)
        {
            _master = master;
            var stackLayout = new StackLayout
            {
                Orientation = StackOrientation.Vertical,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                Spacing = 15,
                Padding = 20
            };

            stackLayout.Children.Add(new Label()
            {
                Font = Font.SystemFontOfSize(NamedSize.Large),
                Text = this.GetLocalizedString("HeaderWelcomeToMediaBrowser")
            });

            stackLayout.Children.Add(new Label()
            {
                Text = this.GetLocalizedString("WelcomePageBodyText")
            });

            stackLayout.Children.Add(new Label()
            {
                Text = this.GetLocalizedString("ToDownloadAndInstallVisit")
            });

            var nextButton = new Button()
            {
                Text = this.GetLocalizedString("ButtonNext"),
                HorizontalOptions = LayoutOptions.End
            };

            nextButton.Clicked += nextButton_Clicked;

            stackLayout.Children.Add(nextButton);

            Title = this.GetLocalizedString("TitleMediaBrowser");

            Content = stackLayout;
        }

        async void nextButton_Clicked(object sender, EventArgs e)
        {
            await Navigation.PushAsync(new ConnectPage(_master));
        }
    }
}
