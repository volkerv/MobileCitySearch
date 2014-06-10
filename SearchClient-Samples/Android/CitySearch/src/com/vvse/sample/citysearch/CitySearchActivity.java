package com.vvse.sample.citysearch;

import java.util.ArrayList;
import java.util.Locale;

import android.app.Activity;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class CitySearchActivity extends Activity implements TextWatcher
{
    private EditText mSearchString;
    private TextView mResultCountView;
    private ListView mResultList;
    private CitySearchSelectorAdapter mCitySearchSelectorAdapter;
    private ArrayList<City> mSearchResults;
    private int mResultCount;
    private CityDatabase mCityDB;

    @Override
    public void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        mResultList = (ListView) findViewById( R.id.resultList );
        mSearchString = (EditText) findViewById( R.id.searchString );
        mSearchString.addTextChangedListener(this);
        
        mResultCountView = (TextView) findViewById( R.id.resultCount );

        mCityDB = new CityDatabase( this );
        mCityDB.init( false );

        mCitySearchSelectorAdapter = new CitySearchSelectorAdapter( this, mCityDB );
        mResultList.setAdapter( mCitySearchSelectorAdapter );

        mResultList.setOnItemClickListener( new OnItemClickListener( )
        {
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id)
            {

                finish( );
            }
        } );

        mSearchResults = new ArrayList<City>( 100 );
        mCitySearchSelectorAdapter.setSearchResults( mSearchResults );
    }
    
    protected void onStop()
    {
    	Log.i("onStop", "start" );

    	super.onStop( );
    	
    	if ( mCityDB != null )
    	{
    		mCityDB.close();
    		mCityDB = null;
    	}

    	Log.i("onStop", "end" );
    }

    private void doSearch( String searchString )
    {
    	Log.i( "doSearch", searchString);
        mSearchResults.clear();

        Cursor c = mCityDB.searchCity(searchString);
        if ( c != null )
        {
            String uiLang = Locale.getDefault().getLanguage();
            c.moveToFirst();

            int resultsToRead = Math.min( 100, c.getCount());
            for (int i = 0; i < resultsToRead; i++ )
            {
                Cursor p =  mCityDB.getPosition( c.getInt(2));
                if ( p != null )
                {
                    p.moveToFirst();

                    String name = c.getString( 0 );
                    String lang = c.getString( 1 );
                    String languages[] = lang.split(",", -1);

                    for (int j = 0; j < languages.length; j++ )
                    {
                        String l = languages[ j ];
                        if ( ( l.length() == 0 ) || l.equals( uiLang ) || l.equals( "en") )
                        {
                            String cc = p.getString( 3 );
                            if ( !cc.equals( "GB_BI" ) )
                            {
                                double lat = p.getDouble( 1 );
                                double lon = p.getDouble( 2 );
    
                                mSearchResults.add( new City( name, cc,  lat, lon ) );
                                break;
                            }
                        }
                    }

                    p.close();
                }

                c.moveToNext();
            }

            c.close();
        }
        
        mResultCount = mSearchResults.size();
        
        mCitySearchSelectorAdapter.notifyDataSetChanged();

    	Log.i( "doSearch", "found " + mResultCount + " entries" );
    }
    
    public void beforeTextChanged(CharSequence s, int start, int count, int after) 
    {
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) 
    {
    }

    public void afterTextChanged(Editable s) 
    {
    	Log.i("onKey", mSearchString.getText().toString());
    	
    	if ( s.length() > 1 )
        {
        	doSearch( s.toString() );
        }
        else
        {
        	mSearchResults.clear();
        	mResultCount = 0;
        	mCitySearchSelectorAdapter.notifyDataSetChanged();
        }

    	int searchResultId = ( mResultCount > 100 ) ? R.string.searchResultCountLimit : R.string.searchResultCount;
        mResultCountView.setText( String.format( getString(searchResultId), mResultCount ) );
    }
    
    public boolean mustShowVirtualKeyboard()
    {
    	boolean showVirtualKeyboard = true;
    	
    	Configuration cfg = getResources().getConfiguration();
    	if ( cfg != null )
    	{
    		boolean hasHardwareKeyboard = (cfg.keyboard != Configuration.KEYBOARD_NOKEYS);
    		if ( hasHardwareKeyboard )
    		{
    			showVirtualKeyboard = ( cfg.hardKeyboardHidden == Configuration.KEYBOARDHIDDEN_YES );
    		}
    	}

    	return showVirtualKeyboard;
    }
}