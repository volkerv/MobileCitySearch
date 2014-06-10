package com.vvse.sample.citysearch;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

public class City 
{
	private String mName;
	private String mCountryCode;
	private double mLatitude;
	private double mLongitude;

	public City( String name, String cc, double lat, double lon )
	{
		mName = name;
		mCountryCode = cc;
		mLatitude = lat;
		mLongitude = lon;
	}
	
	public String getName()
	{
		return mName;
	}
	
	public String getCountryCode()
	{
		return mCountryCode;
	}
	
	public double getLatitude()
	{
		return mLatitude;
	}
	
	public double getLongitude()
	{
		return mLongitude;
	}
	
	public String getCountry( Context context )
	{
		Resources res = context.getResources();

		try
		{
			return context.getString( res.getIdentifier("ctry_" + mCountryCode, "string", context.getPackageName() ));
		}
		catch ( Exception e )
		{
			Log.e("Exception", "Failed to retrieve country name");
		}
		
		return "";
	}
}
