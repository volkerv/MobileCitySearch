//
//  City.m
//  LunaSolCal
//
//  Created by Volker Vöcking on 27.07.10.
//  Copyright 2010 VVSE. All rights reserved.
//

#import "City.h"


@implementation City

@synthesize name;
@synthesize countryCode;
@synthesize latitude;
@synthesize longitude;

- (City *) initWithName:(NSString *) n countryCode:(NSString *) cc latitude:(double) lat longitude:(double) lon {
    
    self.name = n;
    self.countryCode = cc;
    self.latitude = lat;    
    self.longitude = lon;
    
    return self;
}

- (NSString *) formattedCountry {
    
    return NSLocalizedStringFromTable( countryCode, @"CountryNames", @"");
}


@end
