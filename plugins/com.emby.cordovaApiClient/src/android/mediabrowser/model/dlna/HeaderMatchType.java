package mediabrowser.model.dlna;

public enum HeaderMatchType
{
	Equals(0),
	Regex(1),
	Substring(2);

	private int intValue;
	private static java.util.HashMap<Integer, HeaderMatchType> mappings;
	private static java.util.HashMap<Integer, HeaderMatchType> getMappings()
	{
		if (mappings == null)
		{
			synchronized (HeaderMatchType.class)
			{
				if (mappings == null)
				{
					mappings = new java.util.HashMap<Integer, HeaderMatchType>();
				}
			}
		}
		return mappings;
	}

	private HeaderMatchType(int value)
	{
		intValue = value;
		getMappings().put(value, this);
	}

	public int getValue()
	{
		return intValue;
	}

	public static HeaderMatchType forValue(int value)
	{
		return getMappings().get(value);
	}
}