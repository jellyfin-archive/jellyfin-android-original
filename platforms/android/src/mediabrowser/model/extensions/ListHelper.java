package mediabrowser.model.extensions;

public final class ListHelper
{
	public static boolean ContainsIgnoreCase(java.util.ArrayList<String> list, String value)
	{
		if (value == null)
		{
			throw new IllegalArgumentException("value");
		}

        for  (String s : list) {
            if (s.equalsIgnoreCase(value)) return true;
        }

		return false;
	}
	public static boolean ContainsIgnoreCase(String[] list, String value)
	{
		if (value == null)
		{
			throw new IllegalArgumentException("value");
		}

        for  (String s : list) {
            if (s.equalsIgnoreCase(value)) return true;
        }

        return false;
	}
}