package mediabrowser.model.dlna;

public class ProfileCondition
{
//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("condition")] public ProfileConditionType Condition {get;set;}
	private ProfileConditionType Condition = ProfileConditionType.values()[0];
	public final ProfileConditionType getCondition()
	{
		return Condition;
	}
	public final void setCondition(ProfileConditionType value)
	{
		Condition = value;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("property")] public ProfileConditionValue Property {get;set;}
	private ProfileConditionValue Property = ProfileConditionValue.values()[0];
	public final ProfileConditionValue getProperty()
	{
		return Property;
	}
	public final void setProperty(ProfileConditionValue value)
	{
		Property = value;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("value")] public string Value {get;set;}
	private String Value;
	public final String getValue()
	{
		return Value;
	}
	public final void setValue(String value)
	{
		Value = value;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("isRequired")] public bool IsRequired {get;set;}
	private boolean IsRequired;
	public final boolean getIsRequired()
	{
		return IsRequired;
	}
	public final void setIsRequired(boolean value)
	{
		IsRequired = value;
	}

	public ProfileCondition()
	{
		setIsRequired(true);
	}

	public ProfileCondition(ProfileConditionType condition, ProfileConditionValue property, String value)
	{
		this(condition, property, value, false);

	}

	public ProfileCondition(ProfileConditionType condition, ProfileConditionValue property, String value, boolean isRequired)
	{
		setCondition(condition);
		setProperty(property);
		setValue(value);
		setIsRequired(isRequired);
	}
}