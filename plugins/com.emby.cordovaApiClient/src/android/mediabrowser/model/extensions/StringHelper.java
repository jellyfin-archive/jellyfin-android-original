package mediabrowser.model.extensions;

import java.util.Locale;

/**
 Isolating these helpers allow this entire project to be easily converted to Java
*/
public final class StringHelper
{
	/** 
	 Equalses the ignore case.
	 
	 @param str1 The STR1.
	 @param str2 The STR2.
	 @return <c>true</c> if XXXX, <c>false</c> otherwise.
	*/
	public static boolean EqualsIgnoreCase(String str1, String str2)
	{
		if (str1 == null && str2 == null) return true;

        if (str1 == null || str2 == null) return false;

        return str1.equalsIgnoreCase(str2);
	}

	/** 
	 Indexes the of ignore case.
	 
	 @param str The string.
	 @param value The value.
	 @return System.Int32.
	*/
	public static int IndexOfIgnoreCase(String str, String value)
	{
		return str.toLowerCase(Locale.US).indexOf(value.toLowerCase(Locale.US));
	}

	/** 
	 To the string culture invariant.
	 
	 @param val The value.
	 @return System.String.
	*/
	public static String ToStringCultureInvariant(int val)
	{
        return String.valueOf(val);
	}

	/** 
	 To the string culture invariant.
	 
	 @param val The value.
	 @return System.String.
	*/
	public static String ToStringCultureInvariant(long val)
	{
		return String.valueOf(val);
	}

	/** 
	 To the string culture invariant.
	 
	 @param val The value.
	 @return System.String.
	*/
	public static String ToStringCultureInvariant(double val)
	{
		return String.valueOf(val);
	}

	/**
	 Trims the start.

	 @param str The string.
	 @param c The c.
	 @return System.String.
	*/
	public static String TrimStart(String str, char c)
	{
		return tangible.DotNetToJavaStringHelper.trimStart(str, c);
	}



    /// <summary>
    /// Splits the specified string.
    /// </summary>
    /// <param name="str">The string.</param>
    /// <param name="term">The term.</param>
    /// <returns>System.String[].</returns>
    public static String[] RegexSplit(String str, String term)
    {
        return str.split(term);
    }

    /// <summary>
    /// Splits the specified string.
    /// </summary>
    /// <param name="str">The string.</param>
    /// <param name="term">The term.</param>
    /// <param name="limit">The limit.</param>
    /// <returns>System.String[].</returns>
    public static String[] RegexSplit(String str, String term, int limit)
    {
        return str.split(term, limit);
    }
}