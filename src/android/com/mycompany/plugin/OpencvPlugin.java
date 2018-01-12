package com.mycompany.plugin;


import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import android.content.Context;
import android.widget.Toast;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.os.Build;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.core.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.core.KeyPoint;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * This class echoes a string called from JavaScript.
 */
public class OpencvPlugin extends CordovaPlugin {


    private static final String  TAG = "OpenCV::Activity";
    private static final int     REQUEST_CAMERA_PERMISSIONS = 133;
    private static final int CAMERA_ID_ANY   = -1;
    private static final int CAMERA_ID_BACK  = 99;
    private static final int CAMERA_ID_FRONT = 98;

    @SuppressWarnings("deprecation")
    private Camera               camera;
    private Activity             activity;
    private SurfaceHolder        surfaceHolder;
    private Mat                  mYuv;
    private Mat                  desc2;
    private FeatureDetector      orbDetector;
    private DescriptorExtractor  orbDescriptor;
    private MatOfKeyPoint        kp2;
    private MatOfDMatch          matches;
    private CallbackContext      cb;
    private Date                 last_time;
    private boolean processFrames = true, thread_over = true, debug = false,
            called_success_detection = false, called_failed_detection = true,
            previewing = false, save_files = false;
    private List<Integer> detection = new ArrayList<>();

    private List<Mat> triggers = new ArrayList<>();
    private List<MatOfKeyPoint> triggers_kps = new ArrayList<>();
    private List<Mat> triggers_descs = new ArrayList<>();
    private int trigger_size = -1, detected_index = -1;

    private double timeout = 0.0;
    private int cameraId = -1;
    private int mCameraIndex = CAMERA_ID_ANY;

    private BaseLoaderCallback mLoaderCallback;
    private FrameLayout cameraFrameLayout;

    private  int count = 0;
    private String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private String coords;
    private int screenWidth = 1, screenHeight = 1;



    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("cvtColor")) {
            String message = args.getString(0);
            this.cvtColor(message, callbackContext);
            return true;
        } else if ("echo".equals(action)) {
            echo(args.getString(0), callbackContext);
            return true;
        }
        return false;
    }


    private void echo(
            String msg,
            CallbackContext callbackContext
    ) {
        if (msg == null || msg.length() == 0) {
            callbackContext.error("Empty message!");
        } else {
            Toast.makeText(
                    webView.getContext(),
                    msg,
                    Toast.LENGTH_LONG
            ).show();
            callbackContext.success(msg);
        }
    }

    private void cvtColor(String message, CallbackContext callbackContext) {
        Log.d("zqc", "cvtColor");
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }



    //===========cordova callback event
    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        Log.i(TAG, "zqc initialize, start OpenCV loaded");
        activity = cordova.getActivity();


        super.initialize(cordova, webView);

        mLoaderCallback = new BaseLoaderCallback(activity) {
            @Override
            public void onManagerConnected(int status) {
                switch (status) {
                    case LoaderCallbackInterface.SUCCESS:
                    {
                        Log.i(TAG, "OpenCV loaded successfully");
                    } break;
                    default:
                    {
                        super.onManagerConnected(status);
                    } break;
                }
            }
        };
    }


    @Override
    public void onResume(boolean multitasking) {
        Log.i(TAG, "zqc onResume, start OpenCV loaded");
        super.onResume(multitasking);
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, activity, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        if (camera == null) {
            // openCamera();
        }
    }
}