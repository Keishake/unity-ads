//
//  UnityAdsViewController.m
//  UnityAdsExample
//
//  Created by bluesun on 7/30/12.
//  Copyright (c) 2012 Unity Technologies. All rights reserved.
//

#import "UnityAdsViewController.h"
#import <AVFoundation/AVFoundation.h>
#import <CoreMedia/CoreMedia.h>
#import <UnityAds/UnityAds.h>

@interface UnityAdsViewController () <UnityAdsDelegate, UITextFieldDelegate>
@end

@implementation UnityAdsViewController

@synthesize startButton;
@synthesize openButton;
@synthesize optionsButton;
@synthesize optionsView;
@synthesize developerId;
@synthesize optionsId;
@synthesize loadingImage;
@synthesize contentView;

- (void)viewDidLoad {
  [super viewDidLoad];

	[[UnityAds sharedInstance] setDelegate:self];

  [self.openButton addTarget:self action:@selector(openAds) forControlEvents:UIControlEventTouchUpInside];
  [self.startButton addTarget:self action:@selector(startAds) forControlEvents:UIControlEventTouchUpInside];
  [self.optionsButton addTarget:self action:@selector(openOptions) forControlEvents:UIControlEventTouchUpInside];

  [self.developerId setDelegate:self];
  [self.optionsId setDelegate:self];
}

- (void)viewDidAppear:(BOOL)animated {
  [super viewDidAppear:animated];
}

- (void)startAds {
  self.optionsButton.enabled = false;
  [self.optionsButton setAlpha:0.3f];
  self.startButton.enabled = false;
  self.startButton.hidden = true;
  self.openButton.hidden = false;
  self.loadingImage.hidden = false;
  self.optionsView.hidden = true;

  // TEST MODE: Do not use in production apps
  [[UnityAds sharedInstance] setDebugMode:YES];
  [[UnityAds sharedInstance] setTestMode:NO];
  [[UnityAds sharedInstance] setNetworks:@"video_ios_1,mobile"];

  if (self.developerId.text != nil) {
    NSLog(@"Setting developerId");
    // TEST STUFF, DO NOT USE IN PRODUCTION APPS
    if( [self.developerId.text length] > 0){
        [[UnityAds sharedInstance] setTestMode:YES];
    }
    [[UnityAds sharedInstance] setTestDeveloperId:self.developerId.text];
  }

  if (self.optionsId.text != nil) {
    NSLog(@"Setting optionsId");
    // TEST STUFF, DO NOT USE IN PRODUCTION APPS
    [[UnityAds sharedInstance] setTestOptionsId:self.optionsId.text];
  }

  // Initialize Unity Ads
	[[UnityAds sharedInstance] startWithGameId:@"17429" andViewController:self];
}

- (void)openOptions {
  if (self.optionsView.hidden) {
    self.optionsView.hidden = false;
  }
  else {
    self.optionsView.hidden = true;
  }
}

- (void)openAds {
	NSLog(@"canShow: %i",[[UnityAds sharedInstance] canShow]);
  if ([[UnityAds sharedInstance] canShow]) {
    [[UnityAds sharedInstance] setNetwork: @"mobile"];
    NSLog(@"show: %i", [[UnityAds sharedInstance] show:@{
                                                         kUnityAdsOptionNoOfferscreenKey:@true,
                                                         kUnityAdsOptionOpenAnimatedKey:@true,
                                                         kUnityAdsOptionGamerSIDKey:@"gom",
                                                         kUnityAdsOptionMuteVideoSounds:@false,
                                                         kUnityAdsOptionVideoUsesDeviceOrientation:@true
                                                         }]);
	}
	else {
    NSLog(@"Unity Ads cannot be shown.");
  }
}

- (BOOL)shouldAutorotateToInterfaceOrientation:(UIInterfaceOrientation)interfaceOrientation {
  return YES;
}

- (NSUInteger) supportedInterfaceOrientations {
  return UIInterfaceOrientationMaskAll;
}

- (BOOL)shouldAutorotate {
  return YES;
}


#pragma mark - UITextFieldDelegate

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
  NSLog(@"textFieldShouldReturn");
  [textField resignFirstResponder];
  return YES;
}

#pragma mark - UnityAdsDelegate

- (void)unityAdsFetchCompleted:(NSString *)network {
	NSLog(@"unityAdsFetchCompleted");
  [self.loadingImage setImage:[UIImage imageNamed:@"unityads_loaded"]];
	[self.openButton setEnabled:YES];
  [self.instructionsText setText:@"Press \"Open\" to show Unity Ads"];
}

- (void)unityAdsWillShow {
	NSLog(@"unityAdsWillShow");
}

- (void)unityAdsDidShow {
	NSLog(@"unityAdsDidShow");
}

- (void)unityAdsWillHide {
	NSLog(@"unityAdsWillHide");
}

- (void)unityAdsDidHide {
	NSLog(@"unityAdsDidHide");
}

- (void)unityAdsVideoStarted {
	NSLog(@"unityAdsVideoStarted");
}

- (void)unityAdsVideoCompleted:(NSString *)rewardItemKey skipped:(BOOL)skipped {
	NSLog(@"unityAdsVideoCompleted:rewardItemKey:skipped -- key: %@ -- skipped: %@", rewardItemKey, skipped ? @"true" : @"false");
}

@end
