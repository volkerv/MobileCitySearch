package com.vvse.sample.citysearch;

import java.text.DecimalFormat;
import java.util.Locale;

import android.content.Context;

public class GeoCoordinateFormatter
{
    public enum Format
    {
        DMS,
        MinDec,
        DegDec
    };
    
    private Context mContext;
    private Double[] mComponents = new Double[ 3 ];
    private StringBuffer mDirection = new StringBuffer( );

    public GeoCoordinateFormatter(Context ctx)
    {
        mContext = ctx;
    }

    public String formatLatitude( double lat, Format fmt )
    {
        return formatGeoCoordComponent( lat, true, fmt );
    }

    public String formatLongitude( double lon, Format fmt )
    {
        return formatGeoCoordComponent( lon, false, fmt );
    }

    public Double[] getComponents()
    {
        return mComponents;
    }

    public StringBuffer getDirection()
    {
        return mDirection;
    }

    public void splitLatitude(double lat)
    {
        splitGeoCoordComponent( lat, true );
    }

    public void splitLongitude(double lon)
    {
        splitGeoCoordComponent( lon, false );
    }

    private String formatGeoCoordComponent( double value, boolean isLat, Format fmt )
    {
        splitGeoCoordComponent( value, isLat );
        
        switch ( fmt )
        {
            case DMS:
                return String.format( Locale.getDefault(), "%d°%d'%d''%s", mComponents[ 0 ].intValue(), mComponents[ 1 ].intValue(), mComponents[ 2 ].intValue(), mDirection);
                
            case MinDec:
            	return String.format( Locale.getDefault(), "%d°%s'%s", mComponents[ 0 ].intValue(), formatDouble( (Math.abs(value) - mComponents[ 0 ]) * 60.0 ), mDirection );

            case DegDec:
                return String.format( Locale.getDefault(), "%s°%s", formatDouble( Math.abs( value )), mDirection );

            default:
                break;
        }

        return "";
    }
    
    static public String formatDouble( double d )
    {
    	return new DecimalFormat("###.########").format(d);    	
    }

    private void splitGeoCoordComponent(double value, boolean isLat)
    {
        double absValue = Math.abs( value );

        // degrees
        mComponents[ 0 ] = Math.floor( absValue );

        // minutes
        mComponents[ 1 ] = Math.floor( ( absValue - mComponents[ 0 ] ) * 60 );

        // seconds
        mComponents[ 2 ] = Double.valueOf(
                Math.round( ( ( ( absValue - mComponents[ 0 ] ) * 60 ) - mComponents[ 1 ] ) * 60 ) );

        int stringRes = 0;
        if ( value < 0 )
        {
            stringRes = isLat ? R.string.south : R.string.west;
        }
        else
        {
            stringRes = isLat ? R.string.north : R.string.east;
        }

        mDirection.delete( 0, mDirection.length( ) );
        mDirection.append( mContext.getString( stringRes ) );
    }

}
