package mediabrowser.model.dlna;

import mediabrowser.model.extensions.*;

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlRoot("Profile")] public class DeviceProfile
public class DeviceProfile
{
	/** 
	 Gets or sets the name.
	 
	 <value>The name.</value>
	*/
	private String Name;
	public final String getName()
	{
		return Name;
	}
	public final void setName(String value)
	{
		Name = value;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlIgnore] public string Id {get;set;}
	private String Id;
	public final String getId()
	{
		return Id;
	}
	public final void setId(String value)
	{
		Id = value;
	}

//C# TO JAVA CONVERTER TODO TASK: Java annotations will not correspond to .NET attributes:
//ORIGINAL LINE: [XmlIgnore] public DeviceProfileType ProfileType {get;set;}
	private DeviceProfileType ProfileType = DeviceProfileType.values()[0];
	public final DeviceProfileType getProfileType()
	{
		return ProfileType;
	}
	public final void setProfileType(DeviceProfileType value)
	{
		ProfileType = value;
	}

	/** 
	 Gets or sets the identification.
	 
	 <value>The identification.</value>
	*/
	private DeviceIdentification Identification;
	public final DeviceIdentification getIdentification()
	{
		return Identification;
	}
	public final void setIdentification(DeviceIdentification value)
	{
		Identification = value;
	}

	private String FriendlyName;
	public final String getFriendlyName()
	{
		return FriendlyName;
	}
	public final void setFriendlyName(String value)
	{
		FriendlyName = value;
	}
	private String Manufacturer;
	public final String getManufacturer()
	{
		return Manufacturer;
	}
	public final void setManufacturer(String value)
	{
		Manufacturer = value;
	}
	private String ManufacturerUrl;
	public final String getManufacturerUrl()
	{
		return ManufacturerUrl;
	}
	public final void setManufacturerUrl(String value)
	{
		ManufacturerUrl = value;
	}
	private String ModelName;
	public final String getModelName()
	{
		return ModelName;
	}
	public final void setModelName(String value)
	{
		ModelName = value;
	}
	private String ModelDescription;
	public final String getModelDescription()
	{
		return ModelDescription;
	}
	public final void setModelDescription(String value)
	{
		ModelDescription = value;
	}
	private String ModelNumber;
	public final String getModelNumber()
	{
		return ModelNumber;
	}
	public final void setModelNumber(String value)
	{
		ModelNumber = value;
	}
	private String ModelUrl;
	public final String getModelUrl()
	{
		return ModelUrl;
	}
	public final void setModelUrl(String value)
	{
		ModelUrl = value;
	}
	private String SerialNumber;
	public final String getSerialNumber()
	{
		return SerialNumber;
	}
	public final void setSerialNumber(String value)
	{
		SerialNumber = value;
	}
	private boolean IgnoreTranscodeByteRangeRequests;
	public final boolean getIgnoreTranscodeByteRangeRequests()
	{
		return IgnoreTranscodeByteRangeRequests;
	}
	public final void setIgnoreTranscodeByteRangeRequests(boolean value)
	{
		IgnoreTranscodeByteRangeRequests = value;
	}

	private boolean EnableAlbumArtInDidl;
	public final boolean getEnableAlbumArtInDidl()
	{
		return EnableAlbumArtInDidl;
	}
	public final void setEnableAlbumArtInDidl(boolean value)
	{
		EnableAlbumArtInDidl = value;
	}
	private boolean EnableSingleAlbumArtLimit;
	public final boolean getEnableSingleAlbumArtLimit()
	{
		return EnableSingleAlbumArtLimit;
	}
	public final void setEnableSingleAlbumArtLimit(boolean value)
	{
		EnableSingleAlbumArtLimit = value;
	}

	private String SupportedMediaTypes;
	public final String getSupportedMediaTypes()
	{
		return SupportedMediaTypes;
	}
	public final void setSupportedMediaTypes(String value)
	{
		SupportedMediaTypes = value;
	}

	private String UserId;
	public final String getUserId()
	{
		return UserId;
	}
	public final void setUserId(String value)
	{
		UserId = value;
	}

	private String AlbumArtPn;
	public final String getAlbumArtPn()
	{
		return AlbumArtPn;
	}
	public final void setAlbumArtPn(String value)
	{
		AlbumArtPn = value;
	}

	private int MaxAlbumArtWidth;
	public final int getMaxAlbumArtWidth()
	{
		return MaxAlbumArtWidth;
	}
	public final void setMaxAlbumArtWidth(int value)
	{
		MaxAlbumArtWidth = value;
	}
	private int MaxAlbumArtHeight;
	public final int getMaxAlbumArtHeight()
	{
		return MaxAlbumArtHeight;
	}
	public final void setMaxAlbumArtHeight(int value)
	{
		MaxAlbumArtHeight = value;
	}

	private Integer MaxIconWidth = null;
	public final Integer getMaxIconWidth()
	{
		return MaxIconWidth;
	}
	public final void setMaxIconWidth(Integer value)
	{
		MaxIconWidth = value;
	}
	private Integer MaxIconHeight = null;
	public final Integer getMaxIconHeight()
	{
		return MaxIconHeight;
	}
	public final void setMaxIconHeight(Integer value)
	{
		MaxIconHeight = value;
	}

	private Integer MaxStreamingBitrate = null;
	public final Integer getMaxStreamingBitrate()
	{
		return MaxStreamingBitrate;
	}
	public final void setMaxStreamingBitrate(Integer value)
	{
		MaxStreamingBitrate = value;
	}
	private Integer MaxStaticBitrate = null;
	public final Integer getMaxStaticBitrate()
	{
		return MaxStaticBitrate;
	}
	public final void setMaxStaticBitrate(Integer value)
	{
		MaxStaticBitrate = value;
	}

	private Integer MusicStreamingTranscodingBitrate = null;
	public final Integer getMusicStreamingTranscodingBitrate()
	{
		return MusicStreamingTranscodingBitrate;
	}
	public final void setMusicStreamingTranscodingBitrate(Integer value)
	{
		MusicStreamingTranscodingBitrate = value;
	}
	private Integer MusicSyncBitrate = null;
	public final Integer getMusicSyncBitrate()
	{
		return MusicSyncBitrate;
	}
	public final void setMusicSyncBitrate(Integer value)
	{
		MusicSyncBitrate = value;
	}

	/** 
	 Controls the content of the X_DLNADOC element in the urn:schemas-dlna-org:device-1-0 namespace.
	*/
	private String XDlnaDoc;
	public final String getXDlnaDoc()
	{
		return XDlnaDoc;
	}
	public final void setXDlnaDoc(String value)
	{
		XDlnaDoc = value;
	}
	/** 
	 Controls the content of the X_DLNACAP element in the urn:schemas-dlna-org:device-1-0 namespace.
	*/
	private String XDlnaCap;
	public final String getXDlnaCap()
	{
		return XDlnaCap;
	}
	public final void setXDlnaCap(String value)
	{
		XDlnaCap = value;
	}
	/** 
	 Controls the content of the aggregationFlags element in the urn:schemas-sonycom:av namespace.
	*/
	private String SonyAggregationFlags;
	public final String getSonyAggregationFlags()
	{
		return SonyAggregationFlags;
	}
	public final void setSonyAggregationFlags(String value)
	{
		SonyAggregationFlags = value;
	}

	private String ProtocolInfo;
	public final String getProtocolInfo()
	{
		return ProtocolInfo;
	}
	public final void setProtocolInfo(String value)
	{
		ProtocolInfo = value;
	}

	private int TimelineOffsetSeconds;
	public final int getTimelineOffsetSeconds()
	{
		return TimelineOffsetSeconds;
	}
	public final void setTimelineOffsetSeconds(int value)
	{
		TimelineOffsetSeconds = value;
	}
	private boolean RequiresPlainVideoItems;
	public final boolean getRequiresPlainVideoItems()
	{
		return RequiresPlainVideoItems;
	}
	public final void setRequiresPlainVideoItems(boolean value)
	{
		RequiresPlainVideoItems = value;
	}
	private boolean RequiresPlainFolders;
	public final boolean getRequiresPlainFolders()
	{
		return RequiresPlainFolders;
	}
	public final void setRequiresPlainFolders(boolean value)
	{
		RequiresPlainFolders = value;
	}

	private boolean EnableMSMediaReceiverRegistrar;
	public final boolean getEnableMSMediaReceiverRegistrar()
	{
		return EnableMSMediaReceiverRegistrar;
	}
	public final void setEnableMSMediaReceiverRegistrar(boolean value)
	{
		EnableMSMediaReceiverRegistrar = value;
	}

	private XmlAttribute[] XmlRootAttributes;
	public final XmlAttribute[] getXmlRootAttributes()
	{
		return XmlRootAttributes;
	}
	public final void setXmlRootAttributes(XmlAttribute[] value)
	{
		XmlRootAttributes = value;
	}

	/** 
	 Gets or sets the direct play profiles.
	 
	 <value>The direct play profiles.</value>
	*/
	private DirectPlayProfile[] DirectPlayProfiles;
	public final DirectPlayProfile[] getDirectPlayProfiles()
	{
		return DirectPlayProfiles;
	}
	public final void setDirectPlayProfiles(DirectPlayProfile[] value)
	{
		DirectPlayProfiles = value;
	}

	/** 
	 Gets or sets the transcoding profiles.
	 
	 <value>The transcoding profiles.</value>
	*/
	private TranscodingProfile[] TranscodingProfiles;
	public final TranscodingProfile[] getTranscodingProfiles()
	{
		return TranscodingProfiles;
	}
	public final void setTranscodingProfiles(TranscodingProfile[] value)
	{
		TranscodingProfiles = value;
	}

	private ContainerProfile[] ContainerProfiles;
	public final ContainerProfile[] getContainerProfiles()
	{
		return ContainerProfiles;
	}
	public final void setContainerProfiles(ContainerProfile[] value)
	{
		ContainerProfiles = value;
	}

	private CodecProfile[] CodecProfiles;
	public final CodecProfile[] getCodecProfiles()
	{
		return CodecProfiles;
	}
	public final void setCodecProfiles(CodecProfile[] value)
	{
		CodecProfiles = value;
	}
	private ResponseProfile[] ResponseProfiles;
	public final ResponseProfile[] getResponseProfiles()
	{
		return ResponseProfiles;
	}
	public final void setResponseProfiles(ResponseProfile[] value)
	{
		ResponseProfiles = value;
	}

	private SubtitleProfile[] SubtitleProfiles;
	public final SubtitleProfile[] getSubtitleProfiles()
	{
		return SubtitleProfiles;
	}
	public final void setSubtitleProfiles(SubtitleProfile[] value)
	{
		SubtitleProfiles = value;
	}

	public DeviceProfile()
	{
		setDirectPlayProfiles(new DirectPlayProfile[] { });
		setTranscodingProfiles(new TranscodingProfile[] { });
		setResponseProfiles(new ResponseProfile[] { });
		setCodecProfiles(new CodecProfile[] { });
		setContainerProfiles(new ContainerProfile[] { });
		setSubtitleProfiles(new SubtitleProfile[] { });

		setXmlRootAttributes(new XmlAttribute[] { });

		setSupportedMediaTypes("Audio,Photo,Video");
		setMaxStreamingBitrate(8000000);
		setMaxStaticBitrate(8000000);
		setMusicStreamingTranscodingBitrate(128000);
		setMusicSyncBitrate(128000);
	}

	public final java.util.ArrayList<String> GetSupportedMediaTypes()
	{
		java.util.ArrayList<String> list = new java.util.ArrayList<String>();
		for (String i : ((getSupportedMediaTypes() != null) ? getSupportedMediaTypes() : "").split("[,]", -1))
		{
			if (!tangible.DotNetToJavaStringHelper.isNullOrEmpty(i))
			{
				list.add(i);
			}
		}
		return list;
	}

	public final TranscodingProfile GetAudioTranscodingProfile(String container, String audioCodec)
	{
		container = StringHelper.TrimStart(((container != null) ? container : ""), '.');

		for (TranscodingProfile i : getTranscodingProfiles())
		{
			if (i.getType() != DlnaProfileType.Audio)
			{
				continue;
			}

			if (!StringHelper.EqualsIgnoreCase(container, i.getContainer()))
			{
				continue;
			}

			if (!ListHelper.ContainsIgnoreCase(i.GetAudioCodecs(), (audioCodec != null) ? audioCodec : ""))
			{
				continue;
			}

			return i;
		}
		return null;
	}

	public final TranscodingProfile GetVideoTranscodingProfile(String container, String audioCodec, String videoCodec)
	{
		container = StringHelper.TrimStart(((container != null) ? container : ""), '.');

		for (TranscodingProfile i : getTranscodingProfiles())
		{
			if (i.getType() != DlnaProfileType.Video)
			{
				continue;
			}

			if (!StringHelper.EqualsIgnoreCase(container, i.getContainer()))
			{
				continue;
			}

			if (!ListHelper.ContainsIgnoreCase(i.GetAudioCodecs(), (audioCodec != null) ? audioCodec : ""))
			{
				continue;
			}

			String tempVar = i.getVideoCodec();
			if (!StringHelper.EqualsIgnoreCase(videoCodec, (tempVar != null) ? tempVar : ""))
			{
				continue;
			}

			return i;
		}
		return null;
	}

}