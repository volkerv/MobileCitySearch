from datetime import datetime
from os import listdir
from os.path import isfile, join
from imposm.parser import OSMParser
import codecs
import locale
import os
import sets
import string
import shutil
import sqlite3

# -----------------------------------------------------------------------------
# This script creates a sqlite database containing cities described by their
# names and a geo coordinates
# -----------------------------------------------------------------------------

def cleanup( ):
	print "cleaning up"
	
	if os.path.exists( countries_dir ):
		shutil.rmtree( countries_dir )
	
	os.makedirs( countries_dir )

	print "done"


def extractPlacesCountries(pbf_input_file, countries_dir, polygon_files):
	print "extracting place nodes by country"

	extract_cmd = 'osmosis \\' + '\n'
	extract_cmd += '    --rb ' + pbf_input_file + ' \\' + '\n' 
	extract_cmd += '    --nk keyList="place" --tf reject-ways --tf reject-relations \\' + '\n'
	extract_cmd += '    --tee ' + str( len( polygon_files ) ) + ' \\' + '\n'

	for polygon_file in polygon_files:
		country = polygon_file.split( '/' )[-1].split( '.' )[0]
		extract_cmd += '    --bp file=' + polygon_file + ' \\' + '\n'
		extract_cmd += '    --wb ' + countries_dir + '/' + country + '.pbf \\' + '\n'
		extract_cmd += '    omitmetadata=true \\' + '\n'

	extract_cmd += '\n'

	print extract_cmd
	exit_stat = os.system( extract_cmd )	
	print "done extracting: " + str( exit_stat )
	print "done"


def extractCities( countries_dir, output_dir ):
	print "extracting cities"
	global CC
	global citiesFile

	if os.path.exists( cities_path ):
		os.remove( cities_path )

	citiesFile = codecs.open( cities_path, encoding='utf-8', mode='wb' ) 

	prepare_CC_map( )

	p = OSMParser( concurrency=4, nodes_callback = handle_nodes )

	for dirname, dirnames, filenames in os.walk( countries_dir ):
	#    for subdirname in dirnames:
		baseDirName = os.path.basename( dirname )
		basePath = os.path.dirname( dirname )

		for country_file in filenames:
			if country_file != ".DS_Store":
				country = os.path.splitext( os.path.basename( country_file ) )[0]
				CC = map_to_CC( country )
	#			print country + " " + CC
				inputFile = dirname + "/" + country_file
				print inputFile
				p.parse( inputFile )

	citiesFile.close()

	print "done"


def handle_nodes(nodes):
    # callback method for nodes
    global CC

    for osmid, tags, coords in nodes:
        if 'place' in tags:
        	place = tags['place']
#        	if place == 'village' or place == 'hamlet' or place == 'city' or place == 'town':
        	if place == 'city' or place == 'town':
    			lon = coords[0]
    			lat = coords[1]
        		for key, value in tags.iteritems():
        			has_name = False
        			if key == 'name':
        				lang = ""
        				has_name = True

        			if key.startswith('name:'):
        				name_comps = key.split(':')
        				lang = name_comps[1]
        				has_name = True

        			if has_name == True:
        				#print str( osmid ) + '|' + value + '|' +  lang + '|' + str( lat ) + '|' + str( lon ) + '|' + CC + '\n' 
	        			citiesFile.write( str( osmid ) + '|' + value + '|' +  lang + '|' + str( lat ) + '|' + str( lon ) + '|' + CC + '\n' )


def prepare_CC_map():
    inputFile = open( '../input/country_codes.txt', 'r' )

    for line in inputFile.readlines():
		comps = line.split('|')
		CCmap[comps[1].rstrip()] = comps[0]


def map_to_CC(country):
	if CCmap.has_key( country ):
		return CCmap[country]
	else:
		return "***"


def createCityDB( output_dir ):
	supported_langs = ( '', 'en', 'de', 'fr', 'es', 'it', 'ru' , 'zh', 'jp', 'he' )
	cityDB_file = output_dir + "/CityDB.sqlite"

	if os.path.exists( cityDB_file ):
		os.remove( cityDB_file )

	connection = sqlite3.connect( cityDB_file )
	cursor1 = connection.cursor( )
	cursor2 = connection.cursor( )

	cursor1.execute( "create virtual table cityname using fts3(name TEXT, lang TEXT, cityposrowid INT)" )
	cursor2.execute( "create table citypos (rowid INTEGER PRIMARY KEY, lat REAL, lon REAL, cc TEXT)" )

	cityFile = open( cities_path, 'r' )
	n = 0
	lastosmid = ""
	citynames = {}

	for c in cityFile.readlines():
		if n % 1000 == 0:
			print n
		n = n + 1

		namesDict = {}

		comps = c.split( "|" )
		if len( comps ) == 6:
			osmid = comps[0].lstrip()
			cc = comps[5].rstrip()
			name = comps[1].replace('"', '\'\'')
			lang = comps[2]
			lat = float( comps[3] ) 
			lon = float( comps[4] )

			if lastosmid != osmid:
				insertPos = "insert into citypos (lat, lon, cc) values (" + str( lat ) + ", " + str( lon ) + ", " + "\""+ cc + "\"" + ")"
				cursor2.execute( insertPos )

				for key in citynames:
					script = "insert into cityname (name, lang, cityposrowid) values (" + "\""+ key + "\", \""+ citynames[key] + "\", " + str( lastrowid ) + ")"
					cursor1.execute( script )

				lastrowid = cursor2.lastrowid
				lastosmid = osmid
				citynames = {}

			if lang in supported_langs:
				if name in citynames:
					citynames[name] = citynames[name] + "," + lang
				else:
					citynames[name] = lang	

		else:
			print "***" + c

	print n

	if len(citynames) > 0:
		for n in citynames:
			script = "insert into cityname (name, lang, cityposrowid) values (" + "\""+ n + "\", \""+ citynames[n] + "\", " + str( lastrowid ) + ")"
			cursor1.execute( script )

	connection.commit()
	cursor1.close()
	cursor2.close()
	connection.close()

# -----------------------------------------------------------------------------
	
print "started"
locale.setlocale( locale.LC_ALL, "" )
start = datetime.now( )

CCmap = { }
CC = ''

polygons_dir = "../input/country_polygons"
pbf_input_file = "../input/OSM_data/europe.osm.pbf"
output_dir = "../output"
countries_dir = output_dir + "/countries"
cities_path = output_dir + "/cities.txt"

cleanup( )

polygon_files = [ join( polygons_dir, f ) for f in listdir( polygons_dir ) if isfile( join( polygons_dir, f ) ) and f != ".DS_Store" ]

# get all places nodes and store them by country 
extractPlacesCountries( pbf_input_file, countries_dir, polygon_files )

# get all cities with all available city names and geo coordinates
extractCities( countries_dir, output_dir )

# create database and fill it with the city coordinates and the names of interest
createCityDB( output_dir )

# done
print "finished after: "
print datetime.now( ) - start
