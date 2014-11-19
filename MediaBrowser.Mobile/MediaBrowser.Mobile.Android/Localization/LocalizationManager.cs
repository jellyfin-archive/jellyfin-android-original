using MediaBrowser.Mobile.Common.Localization;
using MediaBrowser.Mobile.Localization;
using MediaBrowser.Model.Globalization;
using MediaBrowser.Model.Serialization;
using System.Collections.Generic;
using System.Reflection;

namespace MediaBrowser.Mobile.Droid.Localization
{
    public class LocalizationManager : JsonLocalizationManager
    {
        public LocalizationManager(IJsonSerializer jsonSerializer)
            : base(jsonSerializer)
        {
        }

        protected override string EmbeddedResourceNamespace
        {
            get { return typeof(LocalizationCommon).Namespace; }
        }

        protected override Assembly EmbeddedResourceAssembly
        {
            get { return typeof(LocalizationCommon).Assembly; }
        }

        public override IEnumerable<LocalizatonOption> GetLocalizationOptions()
        {
            return LocalizationCommon.GetLocalizationOptions();
        }
    }
}