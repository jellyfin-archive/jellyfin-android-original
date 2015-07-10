package net.spantree.searchbar;

import android.animation.AnimatorInflater;
import android.animation.LayoutTransition;
import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import com.fg.feengo.R;

/**
 * Spantree SearchBar Plugin.
 *
 * Android equivalent of iOS UISearchBar widget.
 * Allows for use of native components for consuming user input.
 *
 * @author alliecurry
 * @since 2.0
 */
public class SearchBar extends CordovaPlugin implements TextView.OnEditorActionListener {
    private static final String SEARCH_EVENT = "searchEvent";
    private static final String ACTION_SHOW = "show";
    private static final String ACTION_HIDE = "hide";
    private static final int ANIMATION_DURATION = 2000;
    private static final boolean RESIZE_WEBVIEW = true;

    private SearchView searchView;
    private boolean isShowing = false;

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArray of arguments for the plugin.
     * @param callbackContext   The callback context used when calling back into JavaScript.
     * @return                  True when the action was valid, false otherwise.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equalsIgnoreCase(ACTION_SHOW)) {
            // Show the search bar
            showSearchBar();
        } else if (action.equalsIgnoreCase(ACTION_HIDE)) {
            // Hide the search bar
            hideSearchBar();
        } else {
            // Unknown action
            return false;
        }
        callbackContext.success();
        return true;
    }

    private void initAnimations() {
        LayoutTransition l = new LayoutTransition();
        l.setDuration(ANIMATION_DURATION);
        l.setInterpolator(LayoutTransition.APPEARING, new AccelerateDecelerateInterpolator());
        l.setInterpolator(LayoutTransition.DISAPPEARING, new AccelerateInterpolator());
        l.setAnimator(LayoutTransition.APPEARING, AnimatorInflater.loadAnimator(cordova.getActivity(), R.animator.slide_in));
        l.setAnimator(LayoutTransition.DISAPPEARING, AnimatorInflater.loadAnimator(cordova.getActivity(), R.animator.slide_out));
        webView.setLayoutTransition(l);
    }

    private void initView() {
        searchView = new SearchView(cordova.getActivity());
        searchView.getInput().setOnEditorActionListener(this); // Handles keyboard search button
        searchView.getInput().setOnTouchListener(drawableListener); // Handles inline search button
        initAnimations();
    }

    /** The given View will be overlap on top or display above the local WebView. */
    private void overlayView(final View view) {
        if (RESIZE_WEBVIEW) {
            // Add above webview
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            ((ViewGroup) webView.getParent()).addView(view, 0, params);
            return;
        }
        // Add on top of WebView
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        webView.addView(view, params);
        view.bringToFront();
    }

    private void showSearchBar() {
        if (isShowing) {
            return;
        } else if (searchView == null) {
            initView();
        }
        isShowing = true;
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                overlayView(searchView);
            }
        });
    }

    private void hideSearchBar() {
        if (searchView == null || !isShowing) {
            return;
        }
        isShowing = false;
        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.removeView(searchView);
            }
        });
    }

    /** Method called when users requests to send a search.c*/
    private void onSearchAction() {
        search(searchView.getInputText());
        searchView.getInput().clearFocus();
        webView.requestFocus();
        closeKeyboard();
    }

    /** Sends the given search term to the Cordova WebView. */
    private void search(final String query) {
        webView.loadUrl(String.format("javascript:cordova.fireDocumentEvent('%s', {text:'%s'});", SEARCH_EVENT, query));
    }

    /** Attempts to close the Android Soft Keyboard. */
    private void closeKeyboard() {
        if (searchView == null) {
            return;
        }

        InputMethodManager imm = (InputMethodManager) cordova.getActivity()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchView.getInput().getWindowToken(), 0);
    }

    /** Handles key events from the soft keyboard. */
    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == KeyEvent.KEYCODE_SEARCH || actionId == KeyEvent.KEYCODE_ENTER) {
            onSearchAction();
            return true;
        }
        return false;
    }

    private View.OnTouchListener drawableListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int DRAWABLE_LEFT = 0;
            final int DRAWABLE_TOP = 1;
            final int DRAWABLE_RIGHT = 2;
            final int DRAWABLE_BOTTOM = 3;

            if (event.getAction() == MotionEvent.ACTION_UP) {
                if (event.getX() >= (searchView.getInput().getRight() -
                        searchView.getInput().getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width() - 
                        searchView.getPaddingInPx() * 2 )) {
                    onSearchAction();
                }
            }
            return false;
        }
    };

    /** Custom Widget for mimicking the iOS UISearchBar. */
    private static class SearchView extends LinearLayout {
        private static final int SEARCH_ICON = android.R.drawable.ic_menu_search;
        private static final int BACKGROUND_COLOR = Color.parseColor("#EE4F4084");
        private static final int PADDING_DP = 8;
        private EditText input;

        public SearchView(Context context) {
            super(context);
            initView();
            initChildViews(context);
        }

        /** Initializes default layout parameters and basic appearance. */
        private void initView() {
            final int padding = (int) dpToPx(PADDING_DP);
            setPadding(padding, (int) (padding + (padding * 0.5)), padding, padding);
            setOrientation(HORIZONTAL);
            setBackgroundColor(BACKGROUND_COLOR);
        }

        /** Initializes default child Views. */
        private void initChildViews(final Context context) {
            input = new EditText(context);
            input.setLayoutParams(new TableRow.LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
            input.setSingleLine();
            input.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
            input.setCompoundDrawablesWithIntrinsicBounds(null, null, getResources().getDrawable(SEARCH_ICON), null);
            addView(input);
        }

        /** @return String representation of the current user input. */
        public String getInputText() {
            return input.getText().toString();
        }

        /** @return the local EditText child View. */
        public EditText getInput() {
            return input;
        }

        /** @return padding used in the view */
        public int getPaddingInPx() {
            return (int) dpToPx(PADDING_DP);
        }

        /** @return the given density-pixel unit in exact pixels. */
        private float dpToPx(final int dp) {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
        }
    }
}
