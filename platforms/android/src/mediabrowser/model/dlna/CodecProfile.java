package mediabrowser.model.dlna;

import mediabrowser.model.extensions.*;

public class CodecProfile
{
//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("type")] public CodecType Type {get;set;}
	private CodecType Type = CodecType.values()[0];
	public final CodecType getType()
	{
		return Type;
	}
	public final void setType(CodecType value)
	{
		Type = value;
	}

	private ProfileCondition[] Conditions;
	public final ProfileCondition[] getConditions()
	{
		return Conditions;
	}
	public final void setConditions(ProfileCondition[] value)
	{
		Conditions = value;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("codec")] public string Codec {get;set;}
	private String Codec;
	public final String getCodec()
	{
		return Codec;
	}
	public final void setCodec(String value)
	{
		Codec = value;
	}

	public CodecProfile()
	{
		setConditions(new ProfileCondition[] {});
	}

	public final java.util.ArrayList<String> GetCodecs()
	{
		java.util.ArrayList<String> list = new java.util.ArrayList<String>();
		for (String i : ((getCodec() != null) ? getCodec() : "").split("[,]", -1))
		{
			if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(i))
			{
				list.add(i);
			}
		}
		return list;
	}

	public final boolean ContainsCodec(String codec)
	{
		java.util.ArrayList<String> codecs = GetCodecs();

		return codecs.isEmpty() || ListHelper.ContainsIgnoreCase(codecs, codec);
	}
}