package mediabrowser.model.dlna;

public enum SubtitleDeliveryMethod
{
	/** 
	 The encode
	*/
	Encode(0),
	/** 
	 The embed
	*/
	Embed(1),
	/** 
	 The external
	*/
	External(2),
	/** 
	 The HLS
	*/
	Hls(3);

	private int intValue;
	private static java.util.HashMap<Integer, SubtitleDeliveryMethod> mappings;
	private static java.util.HashMap<Integer, SubtitleDeliveryMethod> getMappings()
	{
		if (mappings == null)
		{
			synchronized (SubtitleDeliveryMethod.class)
			{
				if (mappings == null)
				{
					mappings = new java.util.HashMap<Integer, SubtitleDeliveryMethod>();
				}
			}
		}
		return mappings;
	}

	private SubtitleDeliveryMethod(int value)
	{
		intValue = value;
		getMappings().put(value, this);
	}

	public int getValue()
	{
		return intValue;
	}

	public static SubtitleDeliveryMethod forValue(int value)
	{
		return getMappings().get(value);
	}
}