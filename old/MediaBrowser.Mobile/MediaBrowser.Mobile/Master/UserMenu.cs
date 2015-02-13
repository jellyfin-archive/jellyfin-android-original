using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using MediaBrowser.Model.ApiClient;
using Xamarin.Forms;

namespace MediaBrowser.Mobile.Master
{
    class UserMenu : Grid
    {
        public UserMenu()
        {
            VerticalOptions = LayoutOptions.FillAndExpand;
            HorizontalOptions = LayoutOptions.FillAndExpand;

            RowDefinitions = new RowDefinitionCollection
            {
                // User image
                new RowDefinition {Height = GridLength.Auto},

                // User name
                new RowDefinition {Height = GridLength.Auto},

                // My Library label
                new RowDefinition {Height = GridLength.Auto},

                // My Library label
                new RowDefinition {Height = new GridLength(1, GridUnitType.Star)},
            };

            ColumnDefinitions = new ColumnDefinitionCollection
            {
                new ColumnDefinition {Width = new GridLength(1, GridUnitType.Star)}
            };

            Children.Add(new Label
            {
                Text = "Grid",
                Font = Font.SystemFontOfSize(NamedSize.Small),
                HorizontalOptions = LayoutOptions.Start

            }, 0, 0);

            Children.Add(new Label
            {
                Text = "Grid2",
                Font = Font.SystemFontOfSize(NamedSize.Small),
                HorizontalOptions = LayoutOptions.Start

            }, 0, 1);

            Children.Add(new Label
            {
                Font = Font.SystemFontOfSize(NamedSize.Small),
                Text = "My Library",
                TextColor = Color.FromHex("666666")

            }, 0, 2);

            Children.Add(GetUserViews(), 0, 3);
        }

        private ListView GetUserViews()
        {
            var listview = new ListView();

            return listview;
        }
    }
}
