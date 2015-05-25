package mediabrowser.model.dlna;

public enum CodecType
{
	Video(0),
	VideoAudio(1),
	Audio(2);

	private int intValue;
	private static java.util.HashMap<Integer, CodecType> mappings;
	private static java.util.HashMap<Integer, CodecType> getMappings()
	{
		if (mappings == null)
		{
			synchronized (CodecType.class)
			{
				if (mappings == null)
				{
					mappings = new java.util.HashMap<Integer, CodecType>();
				}
			}
		}
		return mappings;
	}

	private CodecType(int value)
	{
		intValue = value;
		getMappings().put(value, this);
	}

	public int getValue()
	{
		return intValue;
	}

	public static CodecType forValue(int value)
	{
		return getMappings().get(value);
	}
}