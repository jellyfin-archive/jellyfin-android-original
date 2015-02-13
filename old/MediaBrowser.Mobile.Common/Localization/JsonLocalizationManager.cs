using MediaBrowser.Model.Globalization;
using MediaBrowser.Model.Serialization;
using System;
using System.Collections.Generic;
using System.Reflection;

namespace MediaBrowser.Mobile.Common.Localization
{
    public abstract class JsonLocalizationManager : ILocalizationManager
    {
        private readonly IJsonSerializer _jsonSerializer;

        private readonly Dictionary<string, Dictionary<string, string>> _dictionaries = new Dictionary<string, Dictionary<string, string>>(StringComparer.OrdinalIgnoreCase);

        protected JsonLocalizationManager(IJsonSerializer jsonSerializer)
        {
            _jsonSerializer = jsonSerializer;

            CurrentCulture = "en-US";
        }

        public string CurrentCulture { get; private set; }

        public string GetLocalizedString(string phrase)
        {
            return GetLocalizedString(phrase, CurrentCulture);
        }

        public string GetLocalizedString(string phrase, string culture)
        {
            var dictionary = GetLocalizationDictionary(culture);

            string value;

            if (dictionary.TryGetValue(phrase, out value))
            {
                return value;
            }

            return phrase;
        }

        private Dictionary<string, string> GetLocalizationDictionary(string culture)
        {
            var key = culture;

            return _dictionaries.GetOrAdd(key, k => GetDictionary(culture, "app.json"));
        }

        private Dictionary<string, string> GetDictionary(string culture, string baseFilename)
        {
            var dictionary = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);

            var assembly = EmbeddedResourceAssembly;
            var namespaceName = EmbeddedResourceNamespace;

            CopyInto(dictionary, namespaceName + "." + baseFilename, assembly);
            CopyInto(dictionary, namespaceName + "." + GetResourceFilename(culture), assembly);

            return dictionary;
        }

        private string GetResourceFilename(string culture)
        {
            var parts = culture.Split('-');

            if (parts.Length == 2)
            {
                culture = parts[0].ToLower() + "_" + parts[1].ToUpper();
            }
            else
            {
                culture = culture.ToLower();
            }

            return culture + ".json";
        }

        protected abstract string EmbeddedResourceNamespace
        {
            get;
        }

        protected abstract Assembly EmbeddedResourceAssembly { get; }

        private void CopyInto(IDictionary<string, string> dictionary, string resourcePath, Assembly assembly)
        {
            using (var stream = assembly.GetManifestResourceStream(resourcePath))
            {
                if (stream != null)
                {
                    var dict = _jsonSerializer.DeserializeFromStream<Dictionary<string, string>>(stream);

                    foreach (var key in dict.Keys)
                    {
                        dictionary[key] = dict[key];
                    }
                }
            }
        }

        public abstract IEnumerable<LocalizatonOption> GetLocalizationOptions();

        public void SetCurrentCulture(LocalizatonOption option)
        {
            CurrentCulture = option.Value;
        }
    }
}
