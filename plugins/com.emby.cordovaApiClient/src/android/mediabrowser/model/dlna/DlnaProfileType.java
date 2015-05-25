package mediabrowser.model.dlna;

public enum DlnaProfileType
{
	Audio(0),
	Video(1),
	Photo(2);

	private int intValue;
	private static java.util.HashMap<Integer, DlnaProfileType> mappings;
	private static java.util.HashMap<Integer, DlnaProfileType> getMappings()
	{
		if (mappings == null)
		{
			synchronized (DlnaProfileType.class)
			{
				if (mappings == null)
				{
					mappings = new java.util.HashMap<Integer, DlnaProfileType>();
				}
			}
		}
		return mappings;
	}

	private DlnaProfileType(int value)
	{
		intValue = value;
		getMappings().put(value, this);
	}

	public int getValue()
	{
		return intValue;
	}

	public static DlnaProfileType forValue(int value)
	{
		return getMappings().get(value);
	}
}