package mediabrowser.model.dlna;

public enum EncodingContext
{
	Streaming(0),
	Static(1);

	private int intValue;
	private static java.util.HashMap<Integer, EncodingContext> mappings;
	private static java.util.HashMap<Integer, EncodingContext> getMappings()
	{
		if (mappings == null)
		{
			synchronized (EncodingContext.class)
			{
				if (mappings == null)
				{
					mappings = new java.util.HashMap<Integer, EncodingContext>();
				}
			}
		}
		return mappings;
	}

	private EncodingContext(int value)
	{
		intValue = value;
		getMappings().put(value, this);
	}

	public int getValue()
	{
		return intValue;
	}

	public static EncodingContext forValue(int value)
	{
		return getMappings().get(value);
	}
}