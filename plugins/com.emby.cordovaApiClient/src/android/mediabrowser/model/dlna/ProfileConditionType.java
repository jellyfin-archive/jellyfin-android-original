package mediabrowser.model.dlna;

public enum ProfileConditionType
{
	Equals(0),
	NotEquals(1),
	LessThanEqual(2),
	GreaterThanEqual(3),
	EqualsAny(4);

	private int intValue;
	private static java.util.HashMap<Integer, ProfileConditionType> mappings;
	private static java.util.HashMap<Integer, ProfileConditionType> getMappings()
	{
		if (mappings == null)
		{
			synchronized (ProfileConditionType.class)
			{
				if (mappings == null)
				{
					mappings = new java.util.HashMap<Integer, ProfileConditionType>();
				}
			}
		}
		return mappings;
	}

	private ProfileConditionType(int value)
	{
		intValue = value;
		getMappings().put(value, this);
	}

	public int getValue()
	{
		return intValue;
	}

	public static ProfileConditionType forValue(int value)
	{
		return getMappings().get(value);
	}
}