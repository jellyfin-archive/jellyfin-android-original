using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Xamarin.Forms;

namespace MediaBrowser.Mobile.Startup
{
    public class ServerSelectionPage : ContentPage
    {
        public ServerSelectionPage()
        {
            Title = "Media Browser";

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

            stackLayout.Children.Add(new Label()
            {
                Font = Font.SystemFontOfSize(NamedSize.Large),
                Text = "Select Server"
            });

            var addButton = new Button
            {
                Text = "Add",
                HorizontalOptions = LayoutOptions.FillAndExpand
            };

            addButton.Clicked += addButton_Clicked;
            stackLayout.Children.Add(addButton);

            return stackLayout;
        }

        void addButton_Clicked(object sender, EventArgs e)
        {
        }
    }
}
