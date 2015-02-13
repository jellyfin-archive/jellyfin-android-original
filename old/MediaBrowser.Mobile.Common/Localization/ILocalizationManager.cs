using MediaBrowser.Model.Globalization;
using System.Collections.Generic;

namespace MediaBrowser.Mobile.Common.Localization
{
    public interface ILocalizationManager
    {
        /// <summary>
        /// Gets the localized string.
        /// </summary>
        /// <param name="phrase">The phrase.</param>
        /// <returns>System.String.</returns>
        string GetLocalizedString(string phrase);

        /// <summary>
        /// Gets the localization options.
        /// </summary>
        /// <returns>IEnumerable&lt;LocalizatonOption&gt;.</returns>
        IEnumerable<LocalizatonOption> GetLocalizationOptions();

        /// <summary>
        /// Sets the current culture.
        /// </summary>
        /// <param name="option">The option.</param>
        void SetCurrentCulture(LocalizatonOption option);
    }
}
