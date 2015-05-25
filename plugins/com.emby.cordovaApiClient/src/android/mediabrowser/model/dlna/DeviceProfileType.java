package mediabrowser.model.dlna;

public enum DeviceProfileType
{
	System(0),
	User(1);

	private int intValue;
	private static java.util.HashMap<Integer, DeviceProfileType> mappings;
	private static java.util.HashMap<Integer, DeviceProfileType> getMappings()
	{
		if (mappings == null)
		{
			synchronized (DeviceProfileType.class)
			{
				if (mappings == null)
				{
					mappings = new java.util.HashMap<Integer, DeviceProfileType>();
				}
			}
		}
		return mappings;
	}

	private DeviceProfileType(int value)
	{
		intValue = value;
		getMappings().put(value, this);
	}

	public int getValue()
	{
		return intValue;
	}

	public static DeviceProfileType forValue(int value)
	{
		return getMappings().get(value);
	}
}