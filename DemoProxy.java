package com.example.kupal.nodeinfoservice.Accessibility;


import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.kupal.nodeinfoservice.FrontEnd.GraphDesign;
import com.example.kupal.nodeinfoservice.FrontEnd.RecyclerViewDesign;
import com.example.kupal.nodeinfoservice.R;
import com.github.mikephil.charting.charts.BarChart;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by kupaliwa on 8/27/17.
 */

public class DemoProxy extends AccessibilityService {

    //<-------------------------------  private Data Members  ---------------------------------------->

    private FrameLayout overlay_permission;
    private TextToSpeech mTts;
    private int result;
    private WindowManager wm;
    private String appName = "";
    private boolean once_permission;
    private boolean once_button;
    private Button generatedButton;
    private BuildOverlay.annotationObject annotationObject;
    private ArrayList<BuildOverlay.annotationObject> listOfAnnotationObjects;
    private ArrayList<AccessibilityNodeInfo> listOfNodes;
    private AccessibilityNodeInfo current_overlay_previous_node;
    private AccessibilityNodeInfo current_overlay_next_node;
    private boolean annotation_overlay_exist = false;
    private boolean accessibility_rating_overlay_exist = false;
    private float offsetX;
    private float offsetY;
    private int originalXPos;
    private int originalYPos;
    private boolean moving;
    private boolean present;
    private TextView tv1;

    //<-------------------------------  onCreate method  ---------------------------------------->

    @Override
    public void onCreate() {
        super.onCreate();
        BuildOverlay.statusBarHeight = BuildOverlay.getStatusBarHeight(this);
        BuildOverlay.window_manager = (WindowManager) getSystemService(WINDOW_SERVICE);
        once_button = false;
        present = false;
    }

    //<-------------------------------  override onServiceConnected  ---------------------------------------->
    // instantiated AccessibilityInfo which is responsible for specificying the accessibility parameters
    // instantiated the textToSpeech object
    // Specified the event types that are allowed. Example: TYPE_WINDOW_CONTENT_CHANGED
    // Specified feedbackType as spoken ( can be made generic instead)
    // Specified notfication timeout as 100
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();

        info.eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED |
                AccessibilityEvent.TYPE_VIEW_FOCUSED | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
                | AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;

        mTts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    result = mTts.setLanguage(Locale.US);
                } else {
                    Toast.makeText(getApplicationContext(), "Feature not supported", Toast.LENGTH_SHORT).show();
                }
            }
        });
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_SPOKEN;
        info.notificationTimeout = 100;
        this.setServiceInfo(info);
    }

    //<-------------------------------  override onAccessibilityEvent  ---------------------------------------->
    // Here we specifiy the package names of the android app we want to access
    // dfs function is for building a log of all the nodes inside the current window
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        //dfs(getRootInActiveWindow(),0);

        //<---------------------- phone calling window has no package name (this avoids crash)---------------->
        if (event.getPackageName() == null) {
            return;
        }

        //<-------------------------------  remove all overlay after exiting playstore  ----------------------------->
        if (!event.getPackageName().equals("com.android.vending")) {
            if (overlay_permission != null) {
                wm.removeView(overlay_permission);
                overlay_permission = null;
                once_permission = false;
            }

            if (annotation_overlay_exist && once_button) {
                BuildOverlay.removeOverlays(this);
                annotation_overlay_exist = false;
                once_button = false;
            }
            return;
        }

        //<------------------------------- this checks if we are in system ui ---------------------------------------->

        if (event.getPackageName().equals("com.android.systemui")) {
            Toast.makeText(this, "testing UI", Toast.LENGTH_SHORT).show();
            return;
        }

        //<-------------------------------  add overlay to app store ---------------------------------------->
        // childcount is useful to get more information about the current window

        if (event.getPackageName().equals("com.android.vending")) {
            AccessibilityNodeInfo source = event.getSource();
            source.getChildCount();
            //Toast.makeText(this, "childCount: " + source.getChildCount(), Toast.LENGTH_SHORT).show();
            dfs(getRootInActiveWindow(), 0);
            Log.i("Event", event.toString() + "");
            Log.i("Source", source.toString() + "");


            //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

            final Context context = this;
            // Results list is empty if the current window does not contain an install button
            if(getRootInActiveWindow()!=null) {
                List<AccessibilityNodeInfo> results = getRootInActiveWindow().findAccessibilityNodeInfosByViewId("com.android.vending:id/buy_button");
                // Here we are adding a button on top of the install button if we are on the correct window
                if (results.size() > 0 && !once_button) {
                    final AccessibilityNodeInfo install_button = results.get(0);
                    wm = (WindowManager) getSystemService(WINDOW_SERVICE);
                    listOfNodes = new ArrayList<AccessibilityNodeInfo>();
                    BuildOverlay.refreshListOfNodes(listOfNodes, getRootInActiveWindow());
                    final Rect bounds = new Rect();
                    install_button.getBoundsInScreen(bounds);
                    Log.d("Distance from top", String.valueOf(bounds.top) + "    --     " + String.valueOf(bounds.bottom));
//                        bounds.top -= 150;
//                        bounds.bottom -= 150;
                    if (bounds.top > 800 && bounds.bottom > 900) {
                        listOfAnnotationObjects = new ArrayList<>();
                        View.OnClickListener clickListener = new View.OnClickListener() {
                            public void onClick(View v) {
                                if (!once_permission) {
                                    overlay_permission = new FrameLayout(context);
                                    WindowManager.LayoutParams lp_wish = new WindowManager.LayoutParams();
                                    overlay_permission.setBackgroundColor(Color.WHITE);
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                                        lp_wish.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
                                    }
                                    //lp_wish.format = PixelFormat.TRANSLUCENT;
                                    lp_wish.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
                                    lp_wish.width = WindowManager.LayoutParams.WRAP_CONTENT;
                                    lp_wish.height = WindowManager.LayoutParams.WRAP_CONTENT;
                                    lp_wish.gravity = Gravity.TOP;
                                    //lp_wish.alpha = 100;
                                    LayoutInflater inflater = LayoutInflater.from(context);
                                    inflater.inflate(R.layout.ui1, overlay_permission);
                                    configureOverlay();
                                    wm.addView(overlay_permission, lp_wish);
                                    once_permission = true;
                                } else {
                                    wm.removeView(overlay_permission);
                                    overlay_permission = null;
                                    once_permission = false;
                                }

                                new android.os.Handler().postDelayed(
                                        new Runnable() {
                                            public void run() {
                                                if (overlay_permission != null) {
                                                    overlay_permission.performAccessibilityAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS, null);
                                                }
                                            }
                                        },
                                        1000);
                            }
                        };
                        annotationObject = new BuildOverlay.annotationObject(context, install_button, listOfNodes,
                                bounds, "Install", clickListener,wm);
                        listOfAnnotationObjects.add(annotationObject);
                        generatedButton = annotationObject.generatedButton;
                        annotation_overlay_exist = true;
                        once_button = true;
                    }
                }

                // Here we are removing the Permission button if we are not in the app window
                if (results.size() == 0 && annotation_overlay_exist && once_button) {
                    BuildOverlay.removeOverlays(this);
                    annotation_overlay_exist = false;
                    once_button = false;
                }

                // Here we are removing the UI which is displayed when i click the Permission button
                if (overlay_permission != null && results.size() == 0) {
                    wm.removeView(overlay_permission);
                    overlay_permission = null;
                    once_permission = false;
                }
            }

            //xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx

            if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                // add event
            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                speakToUser("Scrolling");
                // this is the button overlay with swipe/scroll and remove button
            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_FOCUSED) {
                // here we can add an event or overlay
            } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {

            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                Toast.makeText(this, "test1", Toast.LENGTH_SHORT).show();
            } else if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED && getRootInActiveWindow() != null) {

                // a few test functions
            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_CONTEXT_CLICKED) {
                Toast.makeText(this, "test3", Toast.LENGTH_SHORT).show();
            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_LONG_CLICKED) {
                Toast.makeText(this, "test4", Toast.LENGTH_SHORT).show();
            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SELECTED) {
                Toast.makeText(this, "test5", Toast.LENGTH_SHORT).show();
            } else if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_HOVER_ENTER) {
                Toast.makeText(this, "test6", Toast.LENGTH_SHORT).show();
            }
        }
    }


    //<-------------------------------  text to speech helper function  ---------------------------------------->

    private void speakToUser(String eventText) {
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Toast.makeText(this, "Feature not supported", Toast.LENGTH_SHORT).show();
        } else {
            if (!eventText.contains("null")) {
                Toast.makeText(this, eventText, Toast.LENGTH_SHORT).show();
                mTts.speak(eventText, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    @Override
    public void onInterrupt() {
    }

    //<------------------------------- configure wish button ---------------------------------------->

    private void configureOverlay() {
        final Context context = this;
        Button removeButton = (Button) overlay_permission.findViewById(R.id.cancel);
        Button installButton = (Button) overlay_permission.findViewById(R.id.install);
        final Button graphButton = (Button) overlay_permission.findViewById(R.id.graphButton);
        final LinearLayout constructs = (LinearLayout) overlay_permission.findViewById(R.id.task1);
        final LinearLayout graphTask = (LinearLayout) overlay_permission.findViewById(R.id.taskgraph);
        graphTask.setVisibility(View.INVISIBLE);
        TextView appN = (TextView) overlay_permission.findViewById(R.id.appName);
        appN.setText(appName);
        final GraphDesign gm = new GraphDesign(getApplicationContext(), overlay_permission);
        RecyclerViewDesign recyclerViewDesign = new RecyclerViewDesign(getApplicationContext(),overlay_permission,
                appName,gm);
//        Permissiondetails pm = new Permissiondetails(getApplicationContext(), overlay_permission, appName,gm);
        removeButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void onClick(View view) {
                if (once_permission) {
                    wm.removeView(overlay_permission);
                    overlay_permission = null;
                    once_permission = false;
                }
            }
        });
        installButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<AccessibilityNodeInfo> results = getRootInActiveWindow().findAccessibilityNodeInfosByViewId("com.android.vending:id/buy_button");
                if (!results.isEmpty()) {
                    AccessibilityNodeInfo node = results.get(0);
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
            }
        });
        graphButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!present) {
                    present = true;
                    graphButton.setText("Get Privacy Constructs");
                    LinearLayout.LayoutParams constructsB = (LinearLayout.LayoutParams) constructs.getLayoutParams();
                    constructsB.height = 0;
                    constructs.setLayoutParams(constructsB);
                    BarChart bm = (BarChart) gm.getGraphView();
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) graphTask.getLayoutParams();
                    params.height = 540;
                    graphTask.setLayoutParams(params);
                    graphTask.setVisibility(View.VISIBLE);
                    ViewGroup.LayoutParams params2 = gm.getGraphView().getLayoutParams();
                    params2.height = 440;
                    bm.setLayoutParams(params2);
                    bm.setVisibility(View.VISIBLE);
                }else{
                    graphButton.setText("Get Riskiness Graph");
                    present = false;
                    LinearLayout.LayoutParams constructsB = (LinearLayout.LayoutParams) constructs.getLayoutParams();
                    constructsB.height = 540;
                    constructs.setLayoutParams(constructsB);
                    BarChart bm = (BarChart) gm.getGraphView();
                    LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) graphTask.getLayoutParams();
                    params.height = 0;
                    graphTask.setLayoutParams(params);
                }
            }
        });
    }

    //<--------------------- get all the node information or trigger an event ---------------------------------------->

    public void dfs(AccessibilityNodeInfo nodeInfo, final int depth) {

        if (nodeInfo == null) return;
        String spacerString = "";

        for (int i = 0; i < depth; ++i) {
            spacerString += '-';
        }

        if (nodeInfo.getText() != null) {    // able to get permissions with this function
            Log.d("logs", spacerString + nodeInfo.getText().toString() +
                    "------" + nodeInfo.getClassName().toString() + "------" + nodeInfo.getPackageName().toString());
            if (nodeInfo.getClassName().toString().contains("android.widget.TextView") && nodeInfo.getPackageName().toString().contains("com.android.vending")) {
                appName = nodeInfo.getText().toString();
                Log.d("appName", appName);
            }
        }

        for (int i = 0; i < nodeInfo.getChildCount(); ++i) {
            dfs(nodeInfo.getChild(i), depth + 1);
        }
    }
}
//<------------------------------------------------  END  ------------------------------------------------->

