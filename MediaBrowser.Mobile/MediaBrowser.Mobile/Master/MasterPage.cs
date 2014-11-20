using MediaBrowser.Mobile.Common.ViewModels;
using MediaBrowser.Mobile.Extensions;
using MediaBrowser.Mobile.Home;
using MediaBrowser.Mobile.Startup;
using MediaBrowser.Model.ApiClient;
using Xamarin.Forms;

namespace MediaBrowser.Mobile.Master
{
    public class MasterPage : MasterDetailPage
    {
        private readonly HomePage _homePage;

        public MasterPage()
        {
            Master = new MasterMenu(new SessionViewModel());

            _homePage = new HomePage();
            Detail = new NavigationPage(_homePage)
            {
                Title = this.GetLocalizedString("TitleAppName")
            };

            Title = this.GetLocalizedString("TitleAppName");
        }

        protected override void OnAppearing()
        {
            base.OnAppearing();

            _homePage.ShowLoggedOut();
            ShowStartupFlow();
        }

        private async void ShowStartupFlow()
        {
            var splash = new SplashPage(this);

            await Detail.Navigation.PushModalAsync(splash);
        }

        public async void ProcessSplashResult(ConnectionResult result)
        {
            if (result.State == ConnectionState.Unavailable)
            {
                await Detail.Navigation.PushModalAsync(new NavigationPage(new WelcomePage(this)));
            }
            else if (result.State == ConnectionState.ServerSelection)
            {
                if (result.Servers.Count == 0)
                {
                    await Detail.Navigation.PushModalAsync(new NavigationPage(new ServerEntryPage(this)));
                }
                else
                {
                    await Detail.Navigation.PushModalAsync(new NavigationPage(new ServerSelectionPage(this)));
                }
            }
            else if (result.State == ConnectionState.ServerSignIn)
            {
                await Detail.Navigation.PushModalAsync(new NavigationPage(new ServerSignInPage(result.Servers[0], result.ApiClient, this)));
            }
            else if (result.State == ConnectionState.SignedIn)
            {
                OnStartupFlowComplete();
            }
            else if (result.State == ConnectionState.ConnectSignIn)
            {
                await Detail.Navigation.PushModalAsync(new NavigationPage(new ConnectPage(this)));
            }
        }

        public async void OnStartupFlowComplete()
        {
            await Detail.Navigation.PopModalAsync();

            _homePage.ShowLoggedIn();
        }
    }
}
