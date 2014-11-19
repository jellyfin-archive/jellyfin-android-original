using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using MediaBrowser.Mobile.Master;
using Xamarin.Forms;

namespace MediaBrowser.Mobile.Startup
{
    public class ServerSelectionPage : ContentPage
    {
        private readonly MasterPage _master;

        public ServerSelectionPage(MasterPage master)
        {
            _master = master;
            Title = "Select Server";

            Content = GetLayout();
        }

        private View GetLayout()
        {
            var stackLayout = new StackLayout
            {
                Orientation = StackOrientation.Vertical,
                VerticalOptions = LayoutOptions.Start,
                HorizontalOptions = LayoutOptions.Center,
                Spacing = 15,
                Padding = 20
            };

            var addButton = new Button
            {
                Text = "New Server",
                HorizontalOptions = LayoutOptions.FillAndExpand,
                TextColor = Color.White,
                BackgroundColor = Color.FromHex("77D065")
            };

            addButton.Clicked += addButton_Clicked;
            stackLayout.Children.Add(addButton);

            return stackLayout;
        }

        async void addButton_Clicked(object sender, EventArgs e)
        {
            await Navigation.PushAsync(new ServerEntryPage(_master));
        }
    }
}
