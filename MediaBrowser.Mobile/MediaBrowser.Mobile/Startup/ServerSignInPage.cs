using MediaBrowser.Mobile.Extensions;
using MediaBrowser.Mobile.Master;
using MediaBrowser.Model.ApiClient;
using MediaBrowser.Model.Net;
using System;
using System.Net;
using System.Threading;
using Xamarin.Forms;
using Xamarin.Forms.Labs.Services;

namespace MediaBrowser.Mobile.Startup
{
    public class ServerSignInPage : ContentPage
    {
        private Entry _usernameEntry;
        private Entry _passwordEntry;
        private readonly View _layout;
        private readonly ServerInfo _server;
        private readonly IApiClient _apiClient;
        private readonly MasterPage _master;

        public ServerSignInPage(ServerInfo server, IApiClient apiClient, MasterPage master)
        {
            _server = server;
            _apiClient = apiClient;
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
                Text = "Sign in to " + _server.Name
            });

            _usernameEntry = new Entry
            {
                HorizontalOptions = LayoutOptions.FillAndExpand,
                Placeholder = "Username"
            };

            stackLayout.Children.Add(_usernameEntry);

            _passwordEntry = new Entry
            {
                HorizontalOptions = LayoutOptions.FillAndExpand,
                IsPassword = true,
                Placeholder = "Password"
            };

            stackLayout.Children.Add(_passwordEntry);

            var nextButton = new Button
            {
                Text = "Sign In",
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

            _usernameEntry.Focus();
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
                    await _apiClient.AuthenticateUserAsync(_usernameEntry.Text, _passwordEntry.Text);

                    var result = await connectionManager.Connect(_server, CancellationToken.None);

                    if (result.State == ConnectionState.SignedIn)
                    {
                        await this.ProcessConnectionResult(result, _master);
                    }
                    else
                    {
                        this.ShowUnauthorizedMessage();
                    }
                }
                catch (HttpException ex)
                {
                    if (ex.StatusCode.HasValue && ex.StatusCode.Value == HttpStatusCode.Unauthorized)
                    {
                        this.ShowUnauthorizedMessage();
                    }
                    else
                    {
                        this.ShowGeneralErrorMessage();
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
            if (string.IsNullOrWhiteSpace(_usernameEntry.Text))
            {
                _usernameEntry.Focus();
                return false;
            }

            return true;
        }
    }
}
