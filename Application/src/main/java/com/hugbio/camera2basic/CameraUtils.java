package com.hugbio.camera2basic;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * 由 HBOK 在 2015/12/23 创建.
 */
public class CameraUtils {

    Activity context;
    Camera.Size pictureSize = null;
    int w;
    int h;
    int previewRotation = -1;
    public Camera camera;
    private Camera.Parameters parameters = null;
    private SurfaceView surfaceView;
    private CameraCreatedListener listener;
    private SurfaceCallback surfaceCallback;
    Camera.PictureCallback pictureCallback = null;
    Camera.Size previewSize = null;

    public CameraUtils(Activity activity, SurfaceView sv, CameraCreatedListener listener) {
        context = activity;
        surfaceView = sv;
        this.listener = listener;
        pictureCallback = null;
    }

    public void init() {
        surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceView.getHolder().setKeepScreenOn(true);// 屏幕常亮
        if (surfaceCallback != null) {
            surfaceView.getHolder().removeCallback(surfaceCallback);
        }
        surfaceCallback = new SurfaceCallback();
        surfaceView.getHolder().addCallback(surfaceCallback);// 为SurfaceView的句柄添加一个回调函数
        pictureCallback = null;
    }

    public void ForciblyCreated() {
        if (surfaceView != null) {
            pictureCallback = null;
            surfaceCallback.surfaceCreated(surfaceView.getHolder());
        }
    }

    public void stopPreviewAndRelease() {
//        surfaceView.getHolder().removeCallback(surfaceCallback);
        previewSize = null;
        if (camera != null) {
            camera.stopPreview();
            camera.release(); // 释放照相机
        }
        camera = null;
        pictureCallback = null;
    }

    public boolean isCameraAvailable() {
        return camera != null;
    }

    public void takePicture(Camera.ShutterCallback shutter, Camera.PictureCallback raw,
                            Camera.PictureCallback jpeg) {
        if (camera != null && surfaceCallback!= null) {
//            camera.takePicture(shutter, raw, jpeg);
            pictureCallback = jpeg;
            try {
                camera.setOneShotPreviewCallback(surfaceCallback);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    private final class SurfaceCallback implements SurfaceHolder.Callback, Camera.PreviewCallback {

        // 拍照状态变化时调用该方法
        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if ((w == 0 || h == 0) && pictureSize != null) {
                double pw = pictureSize.width;
                double ph = pictureSize.height;
                Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(0, cameraInfo);
//                int orientation = cameraInfo.orientation; // 摄像头的默认方向。影响需要设置的摄像头旋转角度
                if (previewRotation == 0 || previewRotation == 180) { //
                    w = (int) (height * (pw / ph));
                    h = height;
                } else {
                    w = width;
                    h =  (int) (width * (pw / ph));
                }
//                if (DeviceInfo.CameraDirection == 0 || DeviceInfo.CameraDirection == 180) { //
//                    if (width > height) {
//                        w = width;
//                        h = (int) (width * (ph / pw));
//                    } else {
//                        w = (int) (height * (pw / ph));
//                        h = height;
//                    }
//                } else {
//                    if (width > height) {
//                        w = (int) (height * (ph / pw));
//                        h = height;
//                    } else {
//                        w = width;
//                        h = (int) (width * (pw / ph));
//                    }
//                }
                ViewGroup.LayoutParams layoutParams = surfaceView.getLayoutParams();
                layoutParams.width = w;
                layoutParams.height = h;
                surfaceView.setLayoutParams(layoutParams);
            }
        }

        // 开始拍照时调用该方法
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                if (camera == null) {
                    camera = Camera.open(0);// 取得第一个摄像头
                }
                //设置参数
                parameters = camera.getParameters(); // 获取各项参数
                parameters.setPictureFormat(PixelFormat.JPEG); // 设置图片格式
                if (parameters.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                    parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE); // 设置对焦模式
                }
                // 这里设置预览大小。照片大小，和surfaceView大小。只要保证三者宽高比例相同就好。
                // 注意设置的摄像头旋转角度。如果为90、270，则照片宽高对应预览高宽。如果为0、180，则照片宽高对应预览宽高
                if (pictureSize == null) {
                    List<Camera.Size> pictureSizes = parameters.getSupportedPictureSizes();
                    pictureSize = CameraUtils.getCurrentScreenSize(pictureSizes, 640, 480);

                }
                if (pictureSize != null) {
                    parameters.setPictureSize(pictureSize.width, pictureSize.height);
                }
                List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
                previewSize = CameraUtils.getCurrentScreenSize(previewSizes, pictureSize.width, pictureSize.height);
                parameters.setPreviewSize(previewSize.width, previewSize.height);
//                List<int[]> supportedPreviewFpsRange = parameters.getSupportedPreviewFpsRange();  //5000-60000
//                parameters.setPreviewFpsRange(5, 10);
//                parameters.set("jpen-quality", 85);
                parameters.setPictureFormat(ImageFormat.JPEG);
                parameters.setJpegQuality(80); // 设置照片质量
                parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
                try {
                    camera.setParameters(parameters);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                camera.setPreviewDisplay(holder); // 设置用于显示拍照影像的SurfaceHolder对象
                previewRotation = CameraUtils.getPreviewDegree(context);
                camera.setDisplayOrientation(previewRotation);
//                camera.stopPreview();

//                int bufferSize = previewSize.width * previewSize.height;
//                bufferSize = bufferSize * ImageFormat.getBitsPerPixel(parameters.getPreviewFormat()) / 8;
//                byte[] gBuffer = new byte[bufferSize];
//                camera.setPreviewCallbackWithBuffer(this);
//                camera.addCallbackBuffer(gBuffer);

                camera.startPreview(); // 开始预览
                if (listener != null) {
                    listener.onCreatedSucceed(camera, holder);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (listener != null) {
                    listener.onCreatedErr(camera, e);
                }
            }

        }

        // 停止拍照时调用该方法
        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            stopPreviewAndRelease();
            if (listener != null) {
                listener.onSurfaceDestroyed(holder);
            }
        }


        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if(camera != null){
                if (pictureCallback != null) {
                    byte[] bytes = decodeToByte(data, camera);
                    if (bytes != null) {
                        pictureCallback.onPictureTaken(bytes, camera);
                    }
                    pictureCallback = null;
                }
//                camera.addCallbackBuffer(data);
            }
        }

        public byte[] decodeToByte(byte[] data, Camera camera) {
            if (previewSize == null) {
                return null;
            }
            byte[] bytes = null;
            try {
                Camera.Size p = camera.getParameters().getPreviewSize();
                YuvImage image = new YuvImage(data, ImageFormat.NV21, p.width, p.height, null);
                if (image != null) {
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    image.compressToJpeg(new Rect(0, 0, p.width, p.height), 100, stream);
                    bytes = stream.toByteArray();
                    stream.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return bytes;
        }

    }

    // 提供一个静态方法，用于根据手机方向获得相机预览画面旋转的角度
    public static int getPreviewDegree(Activity activity) {
        // 获得手机的方向
        int result;
        int rotation = activity.getWindowManager()
                .getDefaultDisplay().getRotation();
        int degree = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degree = 0;
                break;
            case Surface.ROTATION_90:
                degree = 90;
                break;
            case Surface.ROTATION_180:
                degree = 180;
                break;
            case Surface.ROTATION_270:
                degree = 270;
                break;
        }

        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(0, info);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degree) % 360;
            result = (360 - result) % 360;
        } else {
            result = (info.orientation - degree + 360) % 360;
        }
        return result;
    }

    /**
     * 获取合适的预览图和照片的大小
     */
    public static Camera.Size getCurrentScreenSize(List<Camera.Size> sizeList, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        if (sizeList == null)
            return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // 尝试找到一个匹配宽高比的大小
        for (Camera.Size size : sizeList) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        // 如果找不到匹配的宽高比，忽略此要求
        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizeList) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    public interface CameraCreatedListener {
        void onCreatedSucceed(Camera camera, SurfaceHolder holder);

        void onCreatedErr(Camera camera, Exception e);

        void onSurfaceDestroyed(SurfaceHolder holder);
    }
}
