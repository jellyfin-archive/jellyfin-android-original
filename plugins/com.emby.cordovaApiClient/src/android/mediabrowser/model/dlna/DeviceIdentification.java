package mediabrowser.model.dlna;

public class DeviceIdentification
{
	/** 
	 Gets or sets the name of the friendly.
	 
	 <value>The name of the friendly.</value>
	*/
	private String FriendlyName;
	public final String getFriendlyName()
	{
		return FriendlyName;
	}
	public final void setFriendlyName(String value)
	{
		FriendlyName = value;
	}
	/** 
	 Gets or sets the model number.
	 
	 <value>The model number.</value>
	*/
	private String ModelNumber;
	public final String getModelNumber()
	{
		return ModelNumber;
	}
	public final void setModelNumber(String value)
	{
		ModelNumber = value;
	}
	/** 
	 Gets or sets the serial number.
	 
	 <value>The serial number.</value>
	*/
	private String SerialNumber;
	public final String getSerialNumber()
	{
		return SerialNumber;
	}
	public final void setSerialNumber(String value)
	{
		SerialNumber = value;
	}
	/** 
	 Gets or sets the name of the model.
	 
	 <value>The name of the model.</value>
	*/
	private String ModelName;
	public final String getModelName()
	{
		return ModelName;
	}
	public final void setModelName(String value)
	{
		ModelName = value;
	}
	/** 
	 Gets or sets the model description.
	 
	 <value>The model description.</value>
	*/
	private String ModelDescription;
	public final String getModelDescription()
	{
		return ModelDescription;
	}
	public final void setModelDescription(String value)
	{
		ModelDescription = value;
	}
	/** 
	 Gets or sets the device description.
	 
	 <value>The device description.</value>
	*/
	private String DeviceDescription;
	public final String getDeviceDescription()
	{
		return DeviceDescription;
	}
	public final void setDeviceDescription(String value)
	{
		DeviceDescription = value;
	}
	/** 
	 Gets or sets the model URL.
	 
	 <value>The model URL.</value>
	*/
	private String ModelUrl;
	public final String getModelUrl()
	{
		return ModelUrl;
	}
	public final void setModelUrl(String value)
	{
		ModelUrl = value;
	}
	/** 
	 Gets or sets the manufacturer.
	 
	 <value>The manufacturer.</value>
	*/
	private String Manufacturer;
	public final String getManufacturer()
	{
		return Manufacturer;
	}
	public final void setManufacturer(String value)
	{
		Manufacturer = value;
	}
	/** 
	 Gets or sets the manufacturer URL.
	 
	 <value>The manufacturer URL.</value>
	*/
	private String ManufacturerUrl;
	public final String getManufacturerUrl()
	{
		return ManufacturerUrl;
	}
	public final void setManufacturerUrl(String value)
	{
		ManufacturerUrl = value;
	}
	/** 
	 Gets or sets the headers.
	 
	 <value>The headers.</value>
	*/
	private HttpHeaderInfo[] Headers;
	public final HttpHeaderInfo[] getHeaders()
	{
		return Headers;
	}
	public final void setHeaders(HttpHeaderInfo[] value)
	{
		Headers = value;
	}

	public DeviceIdentification()
	{
		setHeaders(new HttpHeaderInfo[] {});
	}
}