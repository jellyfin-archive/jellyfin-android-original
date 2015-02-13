using MediaBrowser.Model.Globalization;
using System.Collections.Generic;
using System.Linq;

namespace MediaBrowser.Mobile.Localization
{
    public static class LocalizationCommon
    {
        public static IEnumerable<LocalizatonOption> GetLocalizationOptions()
        {
            return new List<LocalizatonOption>
            {
                new LocalizatonOption{ Name="English (United States)", Value="en-us"}

            }.OrderBy(i => i.Name);
        }
    }
}
