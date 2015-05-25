package mediabrowser.model.dlna;

import mediabrowser.model.extensions.*;

public class SubtitleProfile
{
//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("format")] public string Format {get;set;}
	private String Format;
	public final String getFormat()
	{
		return Format;
	}
	public final void setFormat(String value)
	{
		Format = value;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("method")] public SubtitleDeliveryMethod Method {get;set;}
	private SubtitleDeliveryMethod Method = SubtitleDeliveryMethod.values()[0];
	public final SubtitleDeliveryMethod getMethod()
	{
		return Method;
	}
	public final void setMethod(SubtitleDeliveryMethod value)
	{
		Method = value;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("didlMode")] public string DidlMode {get;set;}
	private String DidlMode;
	public final String getDidlMode()
	{
		return DidlMode;
	}
	public final void setDidlMode(String value)
	{
		DidlMode = value;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlAttribute("language")] public string Language {get;set;}
	private String Language;
	public final String getLanguage()
	{
		return Language;
	}
	public final void setLanguage(String value)
	{
		Language = value;
	}

	public final java.util.ArrayList<String> GetLanguages()
	{
		java.util.ArrayList<String> list = new java.util.ArrayList<String>();
		for (String i : ((getLanguage() != null) ? getLanguage() : "").split("[,]", -1))
		{
			if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(i))
			{
				list.add(i);
			}
		}
		return list;
	}

	public final boolean SupportsLanguage(String language)
	{
		if (tangible.DotNetToJavaStringHelper.isNullOrEmpty(language))
		{
			language = "und";
		}

		java.util.ArrayList<String> languages = GetLanguages();
		return languages.isEmpty() || ListHelper.ContainsIgnoreCase(languages, language);
	}
}