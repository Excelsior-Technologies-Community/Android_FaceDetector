# **Android Face Detector App**


---
An Android application built with Kotlin, CameraX, and Google ML Kit that performs real-time face detection using the device camera .

---

## ✨ **Features**

- ✅ Real-time face detection using CameraX

- ✅ Accurate face bounding box / wireframe overlay

- ✅ Displays number of detected faces

- ✅ Handles image rotation (EXIF) correctly

- ✅ Clean and modular code structure

  ---

# **Preview**
---
<p align="center">
  <img src="https://github.com/S13reya/Android_FaceDetector/blob/stages/app/src/main/assets/demovideo.gif" height="320"/>




</p>


## ⚡ **Installation**

**Step 1:** Add JitPack repository to your root build.gradle:

```gradle
maven { url = uri("https://jitpack.io") }
```

**Step 2:** Add the dependency in your app `build.gradle` (example if hosted on JitPack):  

```gradle
dependencies {
	        implementation 'com.github.Excelsior-Technologies-Community:Android_FaceDetector:1.0.0'

}
```
## ⚡ **Dependencies**

```

// ML Kit Face Detection
    implementation("com.google.mlkit:face-detection:16.1.5")
// CameraX
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")


```

## ⚡ **Permissions**

```

  <!-- ML Kit Face Detection Model -->
        <meta-data
            android:name="com.google.mlkit.vision.DEPENDENCIES"
            android:value="face" />
 <uses-feature
        android:name="android.hardware.camera"
        android:required="false" />
    <uses-feature android:name="android.hardware.camera.front" android:required="false" />

    <uses-permission android:name="android.permission.CAMERA" />

```

## ⚡ **Usage**

1. Add in XML

```
  <com.ext.android_face_detector.FaceDetectorView
        android:id="@+id/faceDetectorView"
        android:layout_width="0dp"
        android:layout_height="0dp"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintBottom_toTopOf="@+id/controlPanel"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintEnd_toEndOf="parent"/>

```





## **📄 License**

**MIT License**  
```
Copyright (c) 2025 Excelsior Technologies

Permission is hereby granted, free of charge, to any person obtaining a copy  
of this software and associated documentation files (the "Software"), to deal  
in the Software without restriction, including without limitation the rights  
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell  
copies of the Software, and to permit persons to whom the Software is  
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all  
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED **"AS IS"**, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR  
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,  
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
```



  
