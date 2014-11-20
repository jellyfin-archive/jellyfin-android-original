using MediaBrowser.Mobile.Extensions;
using Xamarin.Forms;

namespace MediaBrowser.Mobile.Home
{
    public class HomePage : TabbedPage
    {
        public HomePage()
        {
            Title = this.GetLocalizedString("TitleAppName");
        }

        public void ShowLoggedIn()
        {
            Children.Clear();
            Children.Add(new HomeTab());
            Children.Add(new NextUpTab());
            Children.Add(new FavoriteTab());
            Children.Add(new UpcomingTab());
        }

        public void ShowLoggedOut()
        {
            Children.Clear();
            Children.Add(new PlaceholderTab());
        }
    }
}
