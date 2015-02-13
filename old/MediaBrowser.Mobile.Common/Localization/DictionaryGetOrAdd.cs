using System;
using System.Collections.Generic;

namespace MediaBrowser.Mobile.Common.Localization
{
    internal static partial class DictionaryGetOrAdd
    {
        /// <summary>
        /// Adds a key/value pair to the <see cref="IDictionary{TKey, TValue}"/> if the key does not already exist. 
        /// No locking occurs, so the value may be calculated twice on concurrent scenarios. If you need 
        /// concurrency assurances, use a concurrent dictionary instead.
        /// </summary>
        /// <nuget id="netfx-System.Collections.Generic.DictionaryGetOrAdd" />
        /// <param name="dictionary" this="true">The dictionary where the key/value pair will be added</param>
        /// <param name="key">The key to be added to the dictionary</param>
        /// <param name="valueFactory">The value factory</param>
        public static TValue GetOrAdd<TKey, TValue>(this IDictionary<TKey, TValue> dictionary, TKey key, Func<TKey, TValue> valueFactory)
        {
            var value = default(TValue);
            if (!dictionary.TryGetValue(key, out value))
            {
                // ConcurrentDictionary does a bucket-level lock, which is more efficient.
                // We don't have access to the inner buckets, so we have to look the entire 
                // dictionary.
                lock (dictionary)
                {
                    if (!dictionary.TryGetValue(key, out value))
                    {
                        value = valueFactory(key);
                        dictionary[key] = value;
                    }
                }
            }

            return value;
        }
    }
}
