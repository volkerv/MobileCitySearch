//
//  ViewController.h
//  CitySearch
//
//  Created by Volker Voecking on 05.05.14.
//  Copyright (c) 2014 Volker Voecking Software Engineering. All rights reserved.
//

#import <UIKit/UIKit.h>
#import <sqlite3.h>

@interface ViewController : UIViewController {
    
    NSMutableArray *resultList;
    NSString *databaseName;
    NSString *databasePath;
    
    sqlite3 *cityDB;
}

@property (nonatomic, strong) IBOutlet UILabel *searchLabel;
@property (nonatomic, strong) IBOutlet UITextField *searchText;
@property (nonatomic, strong) IBOutlet UILabel *resultCountLabel;
@property (nonatomic, strong) IBOutlet UITableView *resultsTableView;
@property (nonatomic, strong) IBOutlet UILabel *OSMLabel;

@end
