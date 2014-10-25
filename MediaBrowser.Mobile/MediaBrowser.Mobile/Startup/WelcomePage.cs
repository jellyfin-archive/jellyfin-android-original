using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace MediaBrowser.Mobile.Startup
{
    public class WelcomePage : ContentPage
    {
        private readonly MasterDetailPage _master;
        public WelcomePage(MasterDetailPage master)
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
                Text = "Welcome to Media Browser"
            });

            stackLayout.Children.Add(new Label()
            {
                Text = "With Media Browser you can easily stream videos, music and photos to phone, tablet and other devices from your Media Browser Server."
            });

            stackLayout.Children.Add(new Label()
            {
                Text = "To download and install Media Browser Server visit http://mediabrowser.tv"
            });

            var nextButton = new Button()
            {
                Text = "Next",
                HorizontalOptions = LayoutOptions.End
            };

            nextButton.Clicked += nextButton_Clicked;

            stackLayout.Children.Add(nextButton);

            Title = "Media Browser";

            Content = stackLayout;
        }

        async void nextButton_Clicked(object sender, EventArgs e)
        {
            await Navigation.PushAsync(new ConnectPage(_master));
        }
    }
}
