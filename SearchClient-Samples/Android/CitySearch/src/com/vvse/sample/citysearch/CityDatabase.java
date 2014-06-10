package com.vvse.sample.citysearch;

import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

/**
 * Contains logic to return specific words from the dictionary, and
 * load the dictionary table when it needs to be created.
 */
public class CityDatabase extends SQLiteOpenHelper
{
	private SQLiteDatabase mSQLiteDatabase;
	private static final int DATABASE_VERSION = 1;
	private static String DATABASE_NAME = "city.db";
	private final Context mContext;
	private String mPath;
	private long mFreeSpaceRequired = 8 * 1024 * 1024;
	private ArrayList<String> mTimeZones;
	
	public enum Storage 
	{
	    INTERNAL, EXTERNAL, NOT_AVAILABLE
	};
	
	private Storage mStorage = Storage.NOT_AVAILABLE;

    /**
     * Constructor
     * @param context The Context within which to work, used to create the DB
     */
    public CityDatabase(Context context) 
    {
    	super(context, DATABASE_NAME, null, DATABASE_VERSION);
    	this.mContext = context;
    }
    
    public boolean init( boolean forceCopy )
    {
    	Log.i("init", "start");
    	boolean success = false;

    	loadTimeZones();
    	
    	mStorage = checkStorage();
    	Log.i("init", "mStorage: " + mStorage.toString());

    	if ( mStorage != Storage.NOT_AVAILABLE )
    	{
	    	this.getReadableDatabase();
	
	    	if ( !forceCopy && checkDataBase() ) 
	        {
	    		success = openDataBase();
	        } 
	        else
	        {
	            try 
	            {
	                copyDataBase();
	                this.close();
	                
	                success = openDataBase();
	            } 
	            catch (IOException e) 
	            {
	                throw new Error("Error copying database");
	            }
	        }
    	}
    	
    	Log.i("init", "done");
    	
    	return success;
    }
    
    public void close()
    {
    	if ( mSQLiteDatabase != null )
    	{
    		mSQLiteDatabase.close();
    	}
    	
    	super.close();
    }

    public Cursor searchCity( String searchString )
    {
    	Cursor cursor = null;
    	
    	if ( mSQLiteDatabase != null )
    	{
    	    try
    	    {
                searchString = searchString.replace("\"", "\\\"") + "*";
                searchString = DatabaseUtils.sqlEscapeString( searchString );
                cursor = mSQLiteDatabase.query( "cityname", null, "name MATCH ?",  new String[]{searchString}, null, null, "name" );
    	    }
            catch (SQLiteException e) 
            {
                Log.v("db log", "query failed: " + e.getMessage());
            }
    	}
    	
    	return cursor;
    }
    
    public Cursor getPosition( int rowId )
    {
        Cursor cursor = null;

        if ( mSQLiteDatabase != null )
        {
            cursor = mSQLiteDatabase.query( "citypos", null, "rowid = " + rowId , null, null, null, null );
        }

        return cursor;
    }
    
    private void copyDataBaseFile(String inputFileName, OutputStream myOutput) throws IOException
    {
        InputStream myInput = mContext.getAssets().open(inputFileName);

        byte[] buffer = new byte[102400];
        int length;
        while ((length = myInput.read(buffer))>0)
        {
            myOutput.write(buffer, 0, length);
        }

        myInput.close();
    }
    
    private void copyDataBase() throws IOException
    {
        FileOutputStream myOutput = mContext.openFileOutput(DATABASE_NAME, Context.MODE_PRIVATE);
        
        copyDataBaseFile( "city.aa", myOutput );
        copyDataBaseFile( "city.ab", myOutput );
        copyDataBaseFile( "city.ac", myOutput );
        
        myOutput.flush();
        myOutput.close();
    }
    
    private void removeDataBase()
    {
    	File file = new File( mPath + "/" + DATABASE_NAME );
    	file.delete();
    }

    public boolean openDataBase() throws SQLException 
    {
		Log.i( "CityDatabase", "openDataBase" );

        mSQLiteDatabase = SQLiteDatabase.openDatabase( mPath + "/" + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        
        return (mSQLiteDatabase != null);
    }

    private boolean checkDataBase( ) 
    {
		Log.i( "CityDatabase", "checkDataBase" );

		SQLiteDatabase checkDB = null;
        boolean exist = false;
        try 
        {
            checkDB = SQLiteDatabase.openDatabase( mPath + "/" + DATABASE_NAME, null, SQLiteDatabase.OPEN_READONLY | SQLiteDatabase.NO_LOCALIZED_COLLATORS);
        } 
        catch(SQLiteException e) 
        {
            Log.v("db log", "open database failed: " + e.getMessage());
        }

        if (checkDB != null) 
        {
            exist = true;
            checkDB.close();
        }
        return exist;
    }
    
    private Storage checkStorage()
    {
    	Storage storage = Storage.NOT_AVAILABLE;
    	
    	File internalStorage = mContext.getFilesDir();
    	if ( internalStorage != null )
    	{
    		mPath = internalStorage.getAbsolutePath();
    		
    		if ( checkDataBase( ) )
    		{
    			storage = Storage.INTERNAL;
    		}
    		else
    		{
    			if ( getFreeSpace( internalStorage ) > mFreeSpaceRequired )
    			{
    				storage = Storage.INTERNAL;
    			}
    			else
    			{
    				boolean externalStorageWriteable = false;
    				String state = Environment.getExternalStorageState();

    				if (Environment.MEDIA_MOUNTED.equals(state)) 
    				{
    				    // We can read and write the media
    				    externalStorageWriteable = true;
    				} 
    				else
    				{
    					if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) 
    					{
    						// We can only read the media
    						externalStorageWriteable = false;
    					} 
    					else 
    					{
    						// Something else is wrong. It may be one of many other states, but all we need
    						//  to know is we can neither read nor write
    						externalStorageWriteable = false;
    					}
    				}
    				
    				if ( externalStorageWriteable )
    				{
    					File externalStorage = Environment.getExternalStorageDirectory();
    					if ( externalStorage != null )
    					{
    						mPath = externalStorage.getAbsolutePath();
    			    		if ( checkDataBase( ) )
    			    		{
    			    			storage = Storage.EXTERNAL;
    			    		}
    			    		else
    			    		{
    			    			if ( getFreeSpace( externalStorage ) > mFreeSpaceRequired )
    			    			{
    			    				storage = Storage.EXTERNAL;
    			    			}
    			    			else
    			    			{	
    			    				storage = Storage.NOT_AVAILABLE;
    			    				mPath = "";
    			    			}
    			    		}
    					}
    				}
    			}
    		}
    	}
    	
    	return storage;
    }
    
    private long getFreeSpace( File storage )
    {
    	StatFs stat = new StatFs(storage.getPath());
    	
    	long blockSize = stat.getBlockSize();
    	long availableBlocks = stat.getAvailableBlocks();
    	
    	return blockSize * availableBlocks;
    }

	@Override
	public void onCreate(SQLiteDatabase db) 
	{
		Log.i( "CityDatabase", "onCreate" );
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
    	Log.i( "CityDatabase", "onUpgrade: oldVersion: " + oldVersion + " newVersion: " + newVersion );
    	removeDataBase();
	}
	
	private void loadTimeZones()
	{
        mTimeZones = new ArrayList<String>( 303 );

        try 
        {
            Resources res = mContext.getResources();
            InputStream in_s = res.openRawResource( R.raw.countrytotimezone );

            int bufferSize = in_s.available();
            byte[] tzBuffer = new byte[bufferSize];
            in_s.read( tzBuffer );
            
            int lineStart = 0;
            
            for ( int i = 0; i < bufferSize; i++ )
            {
            	if ( tzBuffer[i] == '\n' ) 
            	{
            		mTimeZones.add(new String( tzBuffer, lineStart, i-lineStart, "UTF-8"));
            		lineStart = i + 1;
            	}
            }
        } 
        catch (Exception e) 
        {
            // e.printStackTrace();
        }
	}
	
	public boolean timeZoneForCountryCode( String cc, StringBuilder resultTz )
	{
		String comps[] = cc.split("_");
		
		for ( String tz : mTimeZones )
		{
			String[] c = tz.split( "\\|" );
			if ( c[0].equals(comps[0]) || c[0].equals( cc ) ) 
			{
				resultTz.append( c[1] );
				return (Integer.parseInt(c[2]) == 0);
			}
		}
		
		return false;
	}
}
