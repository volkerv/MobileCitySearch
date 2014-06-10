//
//  ViewController.m
//  CitySearch
//
//  Created by Volker Voecking on 05.05.14.
//  Copyright (c) 2014 Volker Voecking Software Engineering. All rights reserved.
//

#import "ViewController.h"
#import "City.h"

#include <math.h>

@interface ViewController ()

@end

@implementation ViewController

static NSString *DB_NAME = @"CityDB_Europe.sqlite";
static NSString *searchString = nil;

- (id)initWithCoder:(NSCoder *)decoder {
    
    self = [super initWithCoder:decoder];
    if ( self != nil ) {
        
        databaseName = DB_NAME;
        
        NSArray *documentPaths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, YES);
        NSString *documentsDir = [documentPaths objectAtIndex:0];
        databasePath = [documentsDir stringByAppendingPathComponent:databaseName];
        
        [self checkAndCreateDatabase];
    }
    
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.
    
    self.searchLabel.text = NSLocalizedString( @"FIND_CITY", @"" );
    self.OSMLabel.text = NSLocalizedString( @"OSM", @"" );
    [self.searchText becomeFirstResponder];
}

- (void) viewWillAppear:(BOOL)animated {
    
    [super viewWillAppear:animated];
    
    if ( sqlite3_open( [databasePath UTF8String], &cityDB ) != SQLITE_OK ) {
        
        [[[UIAlertView alloc]initWithTitle:@"Missing"
                                   message:@"Database file not found"
                                  delegate:nil
                         cancelButtonTitle:@"OK"
                         otherButtonTitles:nil, nil]show];
    }
}

- (void) viewWillDisappear:(BOOL)animated {
    
    [super viewWillDisappear:animated];

    if ( cityDB != NULL ) {
        
        sqlite3_close( cityDB );
    }
}

-(BOOL) textFieldShouldReturn:(UITextField*) textField {
    
    [self.searchText resignFirstResponder];
    
    return YES;
}

#pragma mark -
#pragma mark Search stuff

- (IBAction)doSearch:(id)sender {
    
    searchString = [[(UITextField *) sender text] lowercaseString];
    if ( ( searchString != nil ) && ( [searchString length] > 1 ) && ( cityDB != NULL ) ) {
        
        //        double start = [[NSDate date] timeIntervalSince1970];
        
        NSLog( @"Searching for %@", searchString );
        
        resultList = [[NSMutableArray alloc] init];
        if ( resultList != nil ) {
            
            // Create the query statement to get all persons
            NSString *queryStatementName = [NSString stringWithFormat:@"SELECT * FROM cityname WHERE name MATCH '%@*' order by name", searchString];

            sqlite3_stmt *nameStatement;
            int result = sqlite3_prepare_v2( cityDB, [queryStatementName UTF8String], -1, &nameStatement, NULL );
            if ( result == SQLITE_OK ) 
            {
                int resultCount = 0;

                // Iterate over all returned rows
                while (sqlite3_step(nameStatement) == SQLITE_ROW) {
                    
                    // Get associated address of the current person row
                    int posRowId = sqlite3_column_int(nameStatement, 2);
                    NSString *queryStatementPos = [NSString stringWithFormat:@"SELECT * FROM citypos WHERE rowid=%d", posRowId];
                    sqlite3_stmt *posStatement;
                    int posQueryResult = sqlite3_prepare_v2( cityDB, [queryStatementPos UTF8String], -1, &posStatement, NULL );
                    if ( posQueryResult == SQLITE_OK ) {
                        
                        NSString *cityName = [NSString stringWithUTF8String:(char*) sqlite3_column_text( nameStatement, 0 )];
                        NSString *lang = [NSString stringWithUTF8String:(char*) sqlite3_column_text( nameStatement, 1 )];
                        
                        NSArray *languages = [lang componentsSeparatedByString: @","];
                        NSString *uiLang = [[NSLocale preferredLanguages] objectAtIndex:0];
                        
                        for (int j = 0; j < languages.count; j++ )
                        {
                            NSString *l = (NSString *) languages[ j ];
                            if ( ( l.length == 0 ) ||
                                ( [ l compare:uiLang ] == NSOrderedSame ) ||
                                ( [ l compare:@"en" ] == NSOrderedSame ) )
                            {
                                if ( sqlite3_step( posStatement ) == SQLITE_ROW ) {

                                    NSString *cc = [NSString stringWithUTF8String:(char*) sqlite3_column_text( posStatement, 3)];
                                    if ( [ cc compare:@"GB_BI"] != NSOrderedSame ) {
                                 
                                        resultCount++;
                                        
                                        if ( resultCount <= 100 ) {
                                            
                                            double lat = sqlite3_column_double( posStatement, 1 );
                                            double lon = sqlite3_column_double( posStatement, 2 );
                                            
                                            [resultList addObject:[[City alloc] initWithName:cityName
                                                                                 countryCode:[cc stringByTrimmingCharactersInSet:[NSCharacterSet newlineCharacterSet]]
                                                                                    latitude:lat
                                                                                   longitude:lon]];
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                sqlite3_finalize(nameStatement);
                
                self.resultsTableView.hidden = ( resultCount > 0 ) ? NO : YES;
                
                [self.resultCountLabel setText:[NSString stringWithFormat:NSLocalizedString( ( resultCount >= 100 ) ? @"SEARCH_RESULT_COUNT_LIMIT" : @"SEARCH_RESULT_COUNT", @""), resultCount ]];
            }
        }
    }
    else {
        
        [self.resultCountLabel setText:@""];
        resultList = nil;
    }
    
    [self.resultsTableView reloadData];
}
                                                      
- (void) checkAndCreateDatabase {

    NSFileManager *fileManager = [NSFileManager defaultManager];
    
    // Check if the database has already been created in the users filesystem
    BOOL databaseExists = [fileManager fileExistsAtPath:databasePath];
    if ( databaseExists == NO ) {
        
        NSString *databasePathFromApp = [[[NSBundle mainBundle] resourcePath] stringByAppendingPathComponent:databaseName];
        
        NSError *error = 0;
        [fileManager copyItemAtPath:databasePathFromApp toPath:databasePath error:&error];
    }
}

#pragma mark -
#pragma mark Table view data source

- (NSInteger)numberOfSectionsInTableView:(UITableView *)tableView {
    // Return the number of sections.
    return 1;
}


- (NSInteger)tableView:(UITableView *)tableView numberOfRowsInSection:(NSInteger)section {
    
    return ( resultList == nil ) ?  0 : [resultList count];
}


// Customize the appearance of table view cells.
- (UITableViewCell *)tableView:(UITableView *)tableView cellForRowAtIndexPath:(NSIndexPath *)indexPath {
    
    static NSString *CellIdentifier = @"Cell";
    
    UITableViewCell *cell = [tableView dequeueReusableCellWithIdentifier:CellIdentifier];
    if (cell == nil) {
        cell = [[UITableViewCell alloc] initWithStyle:UITableViewCellStyleSubtitle reuseIdentifier:CellIdentifier];
    }
    
    cell.textLabel.text = [[resultList objectAtIndex:indexPath.row] name];
    cell.detailTextLabel.text = [[resultList objectAtIndex:indexPath.row] formattedCountry];
    
    return cell;
}


#pragma mark -
#pragma mark Table view delegate

- (void)tableView:(UITableView *)tableView didSelectRowAtIndexPath:(NSIndexPath *)indexPath {

}


- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

@end
