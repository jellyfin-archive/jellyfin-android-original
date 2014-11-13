using MediaBrowser.Mobile.Extensions;
using MediaBrowser.Model.ApiClient;
using MediaBrowser.Model.Net;
using System;
using System.Net;
using System.Threading;
using Xamarin.Forms;
using Xamarin.Forms.Labs.Services;

namespace MediaBrowser.Mobile.Startup
{
    public class ConnectPage : ContentPage
    {
        private  Entry _usernameEntry;
        private  Entry _passwordEntry;
        private readonly View _layout;
        private readonly MasterDetailPage _master;

        public ConnectPage(MasterDetailPage master)
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
                Text = "Sign in with Media Browser Connect"
            });

            stackLayout.Children.Add(new Label()
            {
                Text = "With Media Browser Connect you can easily access your Media Browser Server wherever you are and share with your family and friends."
            });

            _usernameEntry = new Entry
            {
                HorizontalOptions = LayoutOptions.FillAndExpand,
                Placeholder = "Username or email"
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
                HorizontalOptions = LayoutOptions.FillAndExpand
            };

            nextButton.Clicked += nextButton_Clicked;

            stackLayout.Children.Add(nextButton);

            var skipButton = new Button
            {
                Text = "Skip",
                HorizontalOptions = LayoutOptions.FillAndExpand
            };
            skipButton.Clicked += skipButton_Clicked;
            stackLayout.Children.Add(skipButton);

            stackLayout.Children.Add(new Label()
            {
                Text = "Skip to connect to your server manually",
                HorizontalOptions = LayoutOptions.Center
            });

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

        async void skipButton_Clicked(object sender, EventArgs e)
        {
            var connectionManager = Resolver.Resolve<IConnectionManager>();

            Content = GetLoadingContent();

            try
            {
                var result = await connectionManager.Connect(CancellationToken.None);

                if (result.State == ConnectionState.ConnectSignIn)
                {
                    result.State = ConnectionState.ServerSelection;
                }

                if (result.State == ConnectionState.Unavailable)
                {
                    await Navigation.PushAsync(new ServerEntryPage(_master));
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

        async void nextButton_Clicked(object sender, EventArgs e)
        {
            if (Validate())
            {
                var connectionManager = Resolver.Resolve<IConnectionManager>();

                Content = GetLoadingContent();

                try
                {
                    await connectionManager.LoginToConnect(_usernameEntry.Text, _passwordEntry.Text);
                    
                    var result = await connectionManager.Connect(CancellationToken.None);

                    await this.ProcessConnectionResult(result, _master);
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

            if (string.IsNullOrEmpty(_passwordEntry.Text))
            {
                _passwordEntry.Focus();
                return false;
            }

            return true;
        }
    }
}
