using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using MediaBrowser.Mobile.Extensions;
using MediaBrowser.Mobile.Master;
using MediaBrowser.Model.ApiClient;
using Xamarin.Forms;
using Xamarin.Forms.Labs.Services;

namespace MediaBrowser.Mobile.Startup
{
    public class ServerEntryPage : ContentPage
    {
        private Entry _addressEntry;
        private Entry _portEntry;
        private readonly View _layout;
        private readonly MasterPage _master;

        public ServerEntryPage(MasterPage master)
        {
            _master = master;
            Title = "Media Browser";

            Content = _layout = GetLayout();
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
                Text = "Connect to Media Browser Server"
            });

            stackLayout.Children.Add(new Label()
            {
                Text = "Please enter the address and port of your Media Browser Server."
            });

            _addressEntry = new Entry
            {
                HorizontalOptions = LayoutOptions.FillAndExpand,
                Placeholder = "Address: 192.168.1.1"
            };

            stackLayout.Children.Add(_addressEntry);

            _portEntry = new Entry
            {
                HorizontalOptions = LayoutOptions.FillAndExpand,
                Placeholder = "Port",
                Text = "8096",
                Keyboard = Keyboard.Numeric
            };

            stackLayout.Children.Add(_portEntry);

            var nextButton = new Button
            {
                Text = "Connect",
                HorizontalOptions = LayoutOptions.FillAndExpand,
                TextColor = Color.White,
                BackgroundColor = Color.FromHex("77D065")
            };

            nextButton.Clicked += nextButton_Clicked;

            stackLayout.Children.Add(nextButton);

            return stackLayout;
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();

            _addressEntry.Focus();
        }

        private View GetLoadingContent()
        {
            return new ActivityIndicator
            {
                IsRunning = true,
                HorizontalOptions = LayoutOptions.Center,
                VerticalOptions = LayoutOptions.Center
            };
        }

        async void nextButton_Clicked(object sender, EventArgs e)
        {
            if (Validate())
            {
                var connectionManager = Resolver.Resolve<IConnectionManager>();

                Content = GetLoadingContent();

                try
                {
                    var address = _addressEntry.Text + ":" + _portEntry.Text;

                    var result = await connectionManager.Connect(address, CancellationToken.None);

                    if (result.State == ConnectionState.Unavailable)
                    {
                        this.ShowGeneralErrorMessage();
                    }
                    else
                    {
                        await this.ProcessConnectionResult(result, _master);
                    }
                }
                catch
                {
                    this.ShowGeneralErrorMessage();
                }
                finally
                {
                    Content = _layout;
                }
            }
        }

        private bool Validate()
        {
            if (string.IsNullOrWhiteSpace(_addressEntry.Text))
            {
                _addressEntry.Focus();
                return false;
            }

            if (string.IsNullOrEmpty(_portEntry.Text))
            {
                _portEntry.Focus();
                return false;
            }

            return true;
        }
    }
}
