//
//  City.h
//  LunaSolCal
//
//  Created by Volker Vöcking on 27.07.10.
//  Copyright 2010 VVSE. All rights reserved.
//

#import <Foundation/Foundation.h>


@interface City : NSObject {

    NSString *name;
    NSString *countryCode;
    double latitude;
    double longitude;
}

@property(nonatomic, strong) NSString *name;
@property(nonatomic, strong) NSString *countryCode;
@property(nonatomic) double latitude;
@property(nonatomic) double longitude;

- (City *) initWithName:(NSString *) n countryCode:(NSString *) cc latitude:(double) lat longitude:(double) lon;
- (NSString *) formattedCountry;

@end
