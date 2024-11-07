Project Description:
This is a proof of concept app that uses the Android camera feed and TensorFlow mobilenet library to give a single object’s approximate size based on a reference phone within the same frame from approx 30 centimetres away. 

The phone running the app should be held approximately 30 centimetres away from the test objects. The app looks for a Mobile Phone with approximate dimensions of 7cm x 14cm as a reference object. This is the average size of a standard modern iPhone, Google Pixel 3 etc. 
Specifically, the app is looking for an object that is detected as a ‘cell phone’ in the mobile net library, within certain approximate dimensions. 

When a Mobile Phone within the appropriate size range is detected, the app will use that to approximate the dimensions of another object within the frame. These dimensions are given in centimetres, based off the above reference phone dimensions, and scale, which is a decimal width/height of the object compared to the reference object. Ie, the detected object may be 1.2 x the width of the phone and 0.7 x the height.

The approximate size is based off the current measurements of the detected phone. 

This app is based heavily on the example app “Object Detection” on the TensorFlow Github, available here: https://github.com/tensorflow/examples/blob/master/lite/examples/object_detection/android/


Setup and build instructions:
This app was created with Android Studio Koala Feature Drop | 2024.1.2 Patch 1, on OSX 15.0 and has been tested on physical hardware Google Pixel 3A and 6. 
The reference phone object was tested with iPhone 12, 15 and Google Pixel 3A. 
This app was configured with default settings for gradle, sdk and language versions etc. 

Build Steps:
- Clone source code from Github
- Using Android Studio, go to File -> Open Project and open the folder containing this project. 
- Connect a physical Android device to your computer and ensure that Android Studio can see it. 
- Press the green play button in Android Studio to run the app on the phone.

Run Steps:
- When the app loads, please accept the popup for camera permissions.

Notes:
- This app assumes the presence of a back facing camera, so may not run correctly on emulated devices depending on their configuration. 


Usage guide:
Place the reference phone object standing upright, and then hold the phone running the app approximately 30 centimetres away. The app will display when it detects the reference object and whether the dimensions of it are within tolerance. Add an additional object, the app will then display its approximate size. 

The additional objects can be removed and swapped out, the app will continue running.

Assumptions and limitations:
This app will only detect 2 objects at the same time. Adding more than 2 objects in the same frame will produce undefined behaviour, depending on which objects the detection library returns to the app. 

This app works only with a reference phone of the approximate dimensions from 30 centimetres away. Any other phones or distance will affect the accuracy of the estimated dimensions. 

The app does not comprehensively handle permissions. Please accept the Camera permissions. If permissions are not accepted, the app may need to be reinstalled in order to allow them to be accepted.


Potential enhancements:
The dimensions calculations are very simple and very specific. More work and data needs to be collated in order to make these more accurate. 

More testing also needs to be done on the ability of the UI to scale for differing phone sizes and whether that affects the accuracy of the detection. 

TensorFlow was renamed in October to TFLite for Android. The Android tutorials and support for the new libraries are not fully available yet, but the general functionality should be ported to the new libraries when available. 


Code Architecture:
This app is written in Kotlin and was started as a default Android Studio project with no options selected. Much of the setup, theming etc are the default values which would need to be changed for a commercial app. 

This app has been architectured to use both classic Android xml and Compose for UI, to show both of them working in tandem. The Compose UI makes use of LiveData and ViewModels, which are created and handled as part of the Android Lifecycle. This app also uses ViewBinding for the fragment to communicate with the UI fields. 

As this is a sample app, all of the code is located in the same package. Ideally these would be separated out based on functionality. 
