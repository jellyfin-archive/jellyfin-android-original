package mediabrowser.model.dlna;

public enum TranscodeSeekInfo
{
	Auto(0),
	Bytes(1);

	private int intValue;
	private static java.util.HashMap<Integer, TranscodeSeekInfo> mappings;
	private static java.util.HashMap<Integer, TranscodeSeekInfo> getMappings()
	{
		if (mappings == null)
		{
			synchronized (TranscodeSeekInfo.class)
			{
				if (mappings == null)
				{
					mappings = new java.util.HashMap<Integer, TranscodeSeekInfo>();
				}
			}
		}
		return mappings;
	}

	private TranscodeSeekInfo(int value)
	{
		intValue = value;
		getMappings().put(value, this);
	}

	public int getValue()
	{
		return intValue;
	}

	public static TranscodeSeekInfo forValue(int value)
	{
		return getMappings().get(value);
	}
}