package mediabrowser.model.dlna;

public class TranscodingProfile
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
//ORIGINAL LINE: [XmlAttribute("protocol")] public string Protocol {get;set;}
	private String Protocol;
	public final String getProtocol()
	{
		return Protocol;
	}
	public final void setProtocol(String value)
	{
		Protocol = value;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("estimateContentLength")] public bool EstimateContentLength {get;set;}
	private boolean EstimateContentLength;
	public final boolean getEstimateContentLength()
	{
		return EstimateContentLength;
	}
	public final void setEstimateContentLength(boolean value)
	{
		EstimateContentLength = value;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("enableMpegtsM2TsMode")] public bool EnableMpegtsM2TsMode {get;set;}
	private boolean EnableMpegtsM2TsMode;
	public final boolean getEnableMpegtsM2TsMode()
	{
		return EnableMpegtsM2TsMode;
	}
	public final void setEnableMpegtsM2TsMode(boolean value)
	{
		EnableMpegtsM2TsMode = value;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("transcodeSeekInfo")] public TranscodeSeekInfo TranscodeSeekInfo {get;set;}
	private TranscodeSeekInfo TranscodeSeekInfo = getTranscodeSeekInfo().values()[0];
	public final TranscodeSeekInfo getTranscodeSeekInfo()
	{
		return TranscodeSeekInfo;
	}
	public final void setTranscodeSeekInfo(TranscodeSeekInfo value)
	{
		TranscodeSeekInfo = value;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("context")] public EncodingContext Context {get;set;}
	private EncodingContext Context = EncodingContext.values()[0];
	public final EncodingContext getContext()
	{
		return Context;
	}
	public final void setContext(EncodingContext value)
	{
		Context = value;
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
}