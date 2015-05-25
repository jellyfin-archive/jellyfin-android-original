package mediabrowser.model.dlna;

public class DirectPlayProfile
{
//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("container")] public string Container {get;set;}
	private String Container;
	public final String getContainer()
	{
		return Container;
	}
	public final void setContainer(String value)
	{
		Container = value;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("audioCodec")] public string AudioCodec {get;set;}
	private String AudioCodec;
	public final String getAudioCodec()
	{
		return AudioCodec;
	}
	public final void setAudioCodec(String value)
	{
		AudioCodec = value;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("videoCodec")] public string VideoCodec {get;set;}
	private String VideoCodec;
	public final String getVideoCodec()
	{
		return VideoCodec;
	}
	public final void setVideoCodec(String value)
	{
		VideoCodec = value;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("type")] public DlnaProfileType Type {get;set;}
	private DlnaProfileType Type = DlnaProfileType.values()[0];
	public final DlnaProfileType getType()
	{
		return Type;
	}
	public final void setType(DlnaProfileType value)
	{
		Type = value;
	}

	public final java.util.ArrayList<String> GetContainers()
	{
		java.util.ArrayList<String> list = new java.util.ArrayList<String>();
		for (String i : ((getContainer() != null) ? getContainer() : "").split("[,]", -1))
		{
			if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(i))
			{
				list.add(i);
			}
		}
		return list;
	}

	public final java.util.ArrayList<String> GetAudioCodecs()
	{
		java.util.ArrayList<String> list = new java.util.ArrayList<String>();
		for (String i : ((getAudioCodec() != null) ? getAudioCodec() : "").split("[,]", -1))
		{
			if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(i))
			{
				list.add(i);
			}
		}
		return list;
	}

	public final java.util.ArrayList<String> GetVideoCodecs()
	{
		java.util.ArrayList<String> list = new java.util.ArrayList<String>();
		for (String i : ((getVideoCodec() != null) ? getVideoCodec() : "").split("[,]", -1))
		{
			if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(i))
			{
				list.add(i);
			}
		}
		return list;
	}
}