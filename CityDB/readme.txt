Prerequisites
=============

- install protobuf (https://developers.google.com/protocol-buffers/docs/pythontutorial)
- install imposm.parser for python (https://pypi.python.org/pypi/imposm.parser)
- install osmosis (it is a good idea to increase the heap space available 
  for Java to 2GB by setting JAVACMD_OPTIONS=-Xmx2G in ˜/.osmosis)



Country shape/polygon files
===========================

Shapefiles with more or less precise border can be downloaded for each country
from http://gadm.org

Converting a shp file into a polyline (requires gdal and the gdal python bindings):
python ogr2poly.py input/BRA_adm0.shp

Alternatively country polygons are also available from Geofabrik:
http://download.geofabrik.de



Map data
========

- Map data in PBF format, e.g. continent files from download.geofabrik.de
- Processing a 12GB file with Europe's OSM data takes about 2.5 hours on a Mac mini (i5, 8GB RAM, hard disk)
- It is recommended to use a small data set (e.g. D-A-CH) to test if everything is set up correctly.



Database Model
==============

Two tables:

- "cityname" 
  * uses the sqlite full text search extension (fts3) 
  * has columns "name", "lang" and "rowid" 
  * "lang" is a comma separated list of all languages that use the value stored in column "name"; 
    an empty "lang" list entry exists when no language was specified in OSM, 
    this can be treated as a default name to be used when no language specific name was found
  * stores city names in all languages of interest
  * "rowid" is the foreign key used to retrieve the geo coodinate of the city

- "citypos" 
  * has columns "rowid" (primary key), "lat" , "lon" , "cc" 
  * stores the position (latitude, longitude) and country code of the country the city belongs to



Folder structure
================

CityDB
  |
  |-- input
  |     |-- country_codes.txt     - Maps country codes (ISO-3166) to country names 
  |     |                             (country names as used in the filenames of polygon files)
  |     |-- country_polygons    - Polygon files defining the country boundaries
  |     |-- OSM_data        - Put the source PBF file here 
  |
  |-- output 
  |      |-- cities.txt       - List of city names
  |      |-- city.sqlite        - Sqlite database
  |      |-- countries        - Here the country PBF files that only contain nodes place are put
  |
  |-- scripts
  |      |-- createCityDB.py    - Main script for creating the CityDB
  |      |-- ogr2poly.pl      - Helper script for creating polygon files from shp files (requires gdal)


Building the City DB
====================

- Update the createCityDB.py script with the names of the source PBF file
- Go to the scripts folder 
- Run "python createCityDB.py"


