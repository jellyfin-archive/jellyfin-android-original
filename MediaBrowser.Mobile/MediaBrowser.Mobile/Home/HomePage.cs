using MediaBrowser.Mobile.Extensions;
using Xamarin.Forms;

namespace MediaBrowser.Mobile.Home
{
    public class HomePage : TabbedPage
    {
        public HomePage()
        {
            Title = this.GetLocalizedString("TitleMediaBrowser");

            Children.Add(new HomeTab());
            Children.Add(new NextUpTab());
            Children.Add(new FavoriteTab());
            Children.Add(new UpcomingTab());
        }
    }
}
