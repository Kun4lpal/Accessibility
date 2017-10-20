package com.example.kupal.nodeinfoservice.Accessibility;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.example.kupal.nodeinfoservice.R;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kupaliwa on 8/27/17.
 */

public class BuildOverlay {
    public static int statusBarHeight = 0;
    public static WindowManager window_manager;
    public static ArrayList<View> list_overlays = new ArrayList<>();
    public static ArrayList<View> list_detail_overlays = new ArrayList<>();
    public static ImageReader image_reader;
    public static int compensate_width;


    public static int tab_num = 0;

    // this class represents an unlabeled overlay
    public static class unlabeledObject {
        public String id;
        public String overlay_content;
        public AccessibilityNodeInfo unlabeledItem;
        public Button generatedButton;
        public AccessibilityNodeInfo previous_node;
        public AccessibilityNodeInfo next_node;

        public unlabeledObject(Context context,
                               String alt_text,
                               AccessibilityNodeInfo info,
                               ArrayList<AccessibilityNodeInfo> list_nodes) {
            unlabeledItem = info;
            id = BuildOverlay.getIdOfNode(unlabeledItem);
            previous_node = BuildOverlay.getPrevNode(list_nodes, unlabeledItem);
            next_node = BuildOverlay.getNextNode(list_nodes, unlabeledItem);

            Rect bounds = new Rect();
            unlabeledItem.getBoundsInScreen(bounds);
            String content = unlabeledItem.getViewIdResourceName();
            if (content != null) {
                // ViewResourceId
                content = content.substring(content.lastIndexOf(":id/") + 4);
            } else {
                content = "No ID";
            }
            if (alt_text.length() > 0) content = alt_text;
            overlay_content = content;
            generatedButton = addOverlay(context, content, bounds);
        }
    }

    // this class represents an annotated overlay

    public static class annotationObject {
        public String overlay_content;
        public Button generatedButton;
        public AccessibilityNodeInfo previous_node;
        public AccessibilityNodeInfo next_node;

        public annotationObject(Context context,
                                AccessibilityNodeInfo anchor_node,
                                ArrayList<AccessibilityNodeInfo> list_nodes,
                                Rect bounds,
                                String content,
                                View.OnClickListener clickListener,
                                WindowManager windowManager) {
            overlay_content = content;
            previous_node = BuildOverlay.getPrevNode(list_nodes, anchor_node);
            next_node = anchor_node;
            generatedButton = addOverlay(context, content, bounds);
            generatedButton.setOnClickListener(clickListener);
        }
    }

    // remove the overlays

    public static void removeOverlays(Context context) {
        if (BuildOverlay.list_overlays.size() == 0) return;
        for (View overlay : BuildOverlay.list_overlays) {
            BuildOverlay.window_manager.removeView(overlay);
        }
        BuildOverlay.list_overlays = new ArrayList<>();
    }

    public static void removeDetailOverlays(Context context) {
        if (BuildOverlay.list_detail_overlays.size() == 0) return;
        for (View overlay : BuildOverlay.list_detail_overlays) {
            BuildOverlay.window_manager.removeView(overlay);
        }
        BuildOverlay.list_detail_overlays = new ArrayList<>();
    }

    // add button overlay

    private static Button addOverlay(Context context, String contentDescription, Rect rect) {
        Log.d("Distance from above", String.valueOf(rect.top) + "    --     " + String.valueOf(rect.bottom));
        WindowManager.LayoutParams temp_params = new WindowManager.LayoutParams(
                rect.width(),
                rect.height(),
                rect.left,
                rect.top - statusBarHeight,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        temp_params.gravity = Gravity.TOP | Gravity.LEFT;
        Button temp_Button = new Button(context);
        temp_Button.setPadding(0, 0, 0, 0);
        temp_Button.setBackgroundColor(Color.argb(150, 0, 400, 0));
        temp_Button.setText(contentDescription);
        window_manager.addView(temp_Button, temp_params);
        list_overlays.add(temp_Button);

        return temp_Button;
    }


    // add image overlay
    public static View addImageViewOverlay(Context context, Rect bounds) {
        ImageView image_view = new ImageView(context);
        image_view.setImageResource(R.drawable.gesture);
        image_view.setBackgroundColor(Color.argb(200, 0, 180, 0));
        WindowManager.LayoutParams temp_params = new WindowManager.LayoutParams(
                bounds.width(),
                bounds.height(),
                bounds.left,
                bounds.top - statusBarHeight,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        temp_params.gravity = Gravity.TOP | Gravity.LEFT;
        BuildOverlay.window_manager.addView(image_view, temp_params);
        BuildOverlay.list_overlays.add(image_view);
        return image_view;
    }

    public static View addButtonsViewOverlay(Context context,
                                             ArrayList<Button> buttons,
                                             String overlay_content_description) {
        LinearLayout overlay = new LinearLayout(context);
        overlay.setBackgroundColor(Color.argb(200, 255, 0, 0));
        overlay.setContentDescription(overlay_content_description);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                0,
                0,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        for (Button button : buttons) {
            overlay.addView(button);
        }
        window_manager.addView(overlay, params);
        return overlay;
    }

    // get status bar height of the current window

    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    // click at position
    public static void clickAtPosition(int x, int y, AccessibilityNodeInfo node) {
        if (node == null) return;

        if (node.getChildCount() == 0) {
            Rect buttonRect = new Rect();
            node.getBoundsInScreen(buttonRect);
            if (buttonRect.contains(x, y)) {
                // Maybe we need to think if a large view covers item?
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.i("Xiaoyi", node.toString());
            }
        } else {
            Rect buttonRect = new Rect();
            node.getBoundsInScreen(buttonRect);
            if (buttonRect.contains(x, y)) {
                // Maybe we need to think if a large view covers item?
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.i("Xiaoyi", node.toString());
            }
            for (int i = 0; i < node.getChildCount(); i++) {
                clickAtPosition(x, y, node.getChild(i));
            }
        }
    }


    public static String getIdOfNode(AccessibilityNodeInfo info) {
        String s = info.toString();
        int start_id = s.indexOf("AccessibilityNodeInfo@") + 22;
        return s.substring(start_id, start_id + 8);
    }


    public static void refreshListOfNodes(ArrayList<AccessibilityNodeInfo> list_nodes,
                                          AccessibilityNodeInfo info) {
        if (info == null) return;
        if (info.getChildCount() == 0) {
            list_nodes.add(info);
        } else {
            list_nodes.add(info);
            for (int i = 0; i < info.getChildCount(); i++) {
                refreshListOfNodes(list_nodes, info.getChild(i));
            }
        }
    }


    public static void printAllNodes(AccessibilityNodeInfo root) {
        ArrayList<AccessibilityNodeInfo> list_nodes = new ArrayList<>();
        refreshListOfNodes(list_nodes, root);
        for (AccessibilityNodeInfo info : list_nodes) {
            Log.i("Xiaoyi", info.toString());
            //Log.i("Xiaoyi", info.getClassName().toString());
        }
    }


    public static int getNodeIndex(ArrayList<AccessibilityNodeInfo> list_nodes,
                                   AccessibilityNodeInfo info) {
        for (int i = 0; i < list_nodes.size(); i++) {
            if (list_nodes.get(i).equals(info)) return i;
        }
        return -1;
    }


    public static AccessibilityNodeInfo getPrevNode(ArrayList<AccessibilityNodeInfo> list_nodes,
                                                    AccessibilityNodeInfo info) {
        int node_index = getNodeIndex(list_nodes, info);
        if (node_index == -1) return null;
        if (node_index == 0) {
            return list_nodes.get(list_nodes.size() - 1);
        } else {
            return list_nodes.get(node_index - 1);
        }
    }


    public static AccessibilityNodeInfo getNextNode(ArrayList<AccessibilityNodeInfo> list_nodes,
                                                    AccessibilityNodeInfo info) {
        int node_index = getNodeIndex(list_nodes, info);
        if (node_index == -1) return null;
        if (node_index == list_nodes.size() - 1) {
            return list_nodes.get(0);
        } else {
            return list_nodes.get(node_index + 1);
        }
    }


    public static void findUnlabeledItems(Context context,
                                          AccessibilityNodeInfo info,
                                          ArrayList<AccessibilityNodeInfo> list_nodes,
                                          ArrayList unlabeledItems) {
        if (info == null) return;
        if (info.getChildCount() == 0) {
            if (info.getClassName().equals("android.view.View")) return;
            if (info.getText() == null && info.getContentDescription() == null) {
                String viewId = info.getViewIdResourceName();
                switch (tab_num) {
                    case 0:
                        unlabeledItems.add(new unlabeledObject(context, "Home", info, list_nodes));
                        break;
                    case 1:
                        unlabeledItems.add(new unlabeledObject(context, "Read", info, list_nodes));
                        break;
                    case 2:
                        unlabeledItems.add(new unlabeledObject(context, "Plan", info, list_nodes));
                        break;
                    case 3:
                        unlabeledItems.add(new unlabeledObject(context, "Me", info, list_nodes));
                        break;
                }
                tab_num++;
                if (viewId != null) {
                    viewId = viewId.substring(viewId.lastIndexOf(":id/") + 4);
                    if (viewId.equals("btn_settings"))
                        unlabeledItems.add(new unlabeledObject(context, "Settings", info, list_nodes));
                    if (viewId.equals("btn_image_share"))
                        unlabeledItems.add(new unlabeledObject(context, "Image Share", info, list_nodes));
                    if (viewId.equals("btn_share"))
                        unlabeledItems.add(new unlabeledObject(context, "Share", info, list_nodes));
                }
            }
        } else {
            for (int i = 0; i < info.getChildCount(); i++) {
                findUnlabeledItems(context, info.getChild(i), list_nodes, unlabeledItems);
            }
        }
    }

    public static AccessibilityNodeInfo findItemByClass(AccessibilityNodeInfo info,
                                                        String class_name) {
        ArrayList<AccessibilityNodeInfo> list_nodes = new ArrayList<>();
        refreshListOfNodes(list_nodes, info);
        for (AccessibilityNodeInfo node : list_nodes) {
            Log.i("itemByClass", node.getClassName().toString());
            if (node.getClassName().toString().equals(class_name)) {
                Log.i("itemByClass", node.getClassName().toString());
                return info;
            }
        }
        return null;
    }

    public static AccessibilityNodeInfo findItemByContentDescription(AccessibilityNodeInfo root,
                                                                     String contentDescription) {
        ArrayList<AccessibilityNodeInfo> list_nodes = new ArrayList<>();
        BuildOverlay.refreshListOfNodes(list_nodes, root);
        for (AccessibilityNodeInfo node : list_nodes) {
            if (node.getContentDescription() != null && node.getContentDescription().toString().equals(contentDescription)) {
                return node;
            }
        }
        return null;
    }


    public static class viewInfo {
        public String overlay_content;
        public Button generatedButton;
        public AccessibilityNodeInfo previous_node;
        public AccessibilityNodeInfo next_node;

        public viewInfo(Context context,
                        AccessibilityNodeInfo anchor_node,
                        ArrayList<AccessibilityNodeInfo> list_nodes,
                        Rect bounds,
                        String content,
                        View.OnClickListener clickListener) {
        }
    }


    public static Bitmap getScreenShot() {
        //Long starttime = System.currentTimeMillis();
        final Image image = BuildOverlay.image_reader.acquireLatestImage();
        if (image == null) return null;
        if (image.getPlanes() == null) return null;
        Image.Plane plane = image.getPlanes()[0];
        if (plane == null) return null;
        final ByteBuffer buffer = plane.getBuffer();
        final int width = image.getWidth();
        final int height = image.getHeight();
        final int pixelStride = plane.getPixelStride();
        final int rowStride = plane.getRowStride();
        final int rowPadding = rowStride - pixelStride * width;
        BuildOverlay.compensate_width = rowPadding / pixelStride;
        final Bitmap bmp = Bitmap.createBitmap(width + BuildOverlay.compensate_width, height, Bitmap.Config.ARGB_8888);
        bmp.copyPixelsFromBuffer(buffer);
//        final Bitmap bitmap = Bitmap.createScaledBitmap(bmp, bmp.getWidth() / 2, bmp.getHeight() / 2, false);
        // bmp.recycle();
        //Long endtime = System.currentTimeMillis();
        //Log.i("Xiaoyi", String.valueOf(endtime - starttime));
        image.close();
        return bmp;
    }

    public static AccessibilityNodeInfo getNodeByViewId(AccessibilityNodeInfo rootNode, String viewId) {
        if (rootNode == null) {
            return null;
        }
        List<AccessibilityNodeInfo> results = rootNode.findAccessibilityNodeInfosByViewId(viewId);
        if (results.size() == 0) {
            return null;
        }
        return results.get(0);
    }

    public void getVersion(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
        PackageInfo info = null;
        try {
            info = pm.getPackageInfo(packageName, 0);
            Log.i("Version:", "VersionName: " + info.versionName);
            Log.i("Version:", "VersionCode: " + info.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
