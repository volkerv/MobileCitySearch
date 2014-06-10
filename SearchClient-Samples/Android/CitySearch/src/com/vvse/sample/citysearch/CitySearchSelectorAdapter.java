package com.vvse.sample.citysearch;

import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

public class CitySearchSelectorAdapter extends BaseAdapter implements Filterable
{
    private LayoutInflater mInflater;
    private Context mContext;
    private ArrayList<City> mSearchResults;
    private CityDatabase mCityDB;
    private GeoCoordinateFormatter mGeoCoordFormatter;

    public CitySearchSelectorAdapter(Context context, CityDatabase cityDB)
    {
        // Cache the LayoutInflate to avoid asking for a new one each time.
        mInflater = LayoutInflater.from( context );
        this.mContext = context;
        this.mCityDB = cityDB;
        this.mGeoCoordFormatter = new GeoCoordinateFormatter( context );
    }
    
    public void setSearchResults( ArrayList<City> mSearchResults2 )
    {
    	mSearchResults = mSearchResults2;
    }

    public View getView(final int position, View convertView, ViewGroup parent)
    {
    	convertView = mInflater.inflate( R.layout.city_search_result_cell, null );

    	TextView textLine1 = (TextView) convertView.findViewById( R.id.textLine1 );
        TextView textLine2 = (TextView) convertView.findViewById( R.id.textLine2 );
        TextView textLine3 = (TextView) convertView.findViewById( R.id.textLine3 );
        
        City c = mSearchResults.get(position);
        textLine1.setText( c.getName() );
        textLine2.setText( c.getCountry( mContext ) );
        textLine3.setText( mGeoCoordFormatter.formatLatitude( c.getLatitude( ), GeoCoordinateFormatter.Format.DMS) + "   " +
                           mGeoCoordFormatter.formatLongitude( c.getLongitude( ), GeoCoordinateFormatter.Format.DMS));
        
        convertView.setOnClickListener( new OnClickListener( )
        {
            private int pos = position;

            public void onClick(View v)
            {
                handleClick( v, pos );
            }
        } );

        return convertView;
    }

    private void handleClick(View v, int pos)
    {
    	City c = mSearchResults.get(pos);
    	StringBuilder tz = new StringBuilder();
    	boolean unique = mCityDB.timeZoneForCountryCode(c.getCountryCode(), tz);
    	Log.i("click", c.getName() + " " + tz.toString() + " " + Boolean.toString(unique));
    }

    public Filter getFilter()
    {
        // TODO Auto-generated method stub
        return null;
    }

    public long getItemId(int position)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getCount()
    {
        // TODO Auto-generated method stub
    	if ( mSearchResults != null )
    	{
    		return mSearchResults.size();
    	}
    	else
    	{
    		return 0;
    	}
    }

    public Object getItem(int position)
    {
        // TODO Auto-generated method stub
        return new String( "Blumenkohl" );
    }

}