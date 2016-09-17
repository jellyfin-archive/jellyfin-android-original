package nl.xservices.plugins.actionsheet;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Build;
import android.text.TextUtils;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Original excellent PR by: Brill Pappin
 * @author Mantainer of the code: Eddy Verbruggen
 */
public class ActionSheet extends CordovaPlugin {

  private AlertDialog dialog;

  public ActionSheet() {
    super();
  }

  private static final String ACTION_SHOW = "show";
  private static final String ACTION_HIDE = "hide";

  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

    if (ACTION_SHOW.equals(action)) {
      JSONObject options = args.optJSONObject(0);

      String title = options.optString("title");
      String subtitle = options.optString("subtitle");
      int theme = options.optInt("androidTheme", 1);
      JSONArray buttons = options.optJSONArray("buttonLabels");

      boolean androidEnableCancelButton = options.optBoolean("androidEnableCancelButton", false);
      boolean destructiveButtonLast = options.optBoolean("destructiveButtonLast", false);

      String addCancelButtonWithLabel = options.optString("addCancelButtonWithLabel");
      String addDestructiveButtonWithLabel = options.optString("addDestructiveButtonWithLabel");

      this.show(title, subtitle, buttons, addCancelButtonWithLabel, androidEnableCancelButton,
          addDestructiveButtonWithLabel, destructiveButtonLast,
          theme, callbackContext);
      // need to return as this call is async.
      return true;

    } else if (ACTION_HIDE.equals(action)) {
      if (dialog != null && dialog.isShowing()) {
        dialog.dismiss();
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, -1));
      }
      return true;
    }

    return false;
  }

  public synchronized void show(final String title,
                                final String subtitle,
                                final JSONArray buttonLabels,
                                final String addCancelButtonWithLabel,
                                final boolean androidEnableCancelButton,
                                final String addDestructiveButtonWithLabel,
                                final boolean destructiveButtonLast,
                                final int theme,
                                final CallbackContext callbackContext) {

    final CordovaInterface cordova = this.cordova;

    Runnable runnable = new Runnable() {
      public void run() {

        final AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
          builder = new AlertDialog.Builder(cordova.getActivity(), theme);
        } else {
          builder = new AlertDialog.Builder(cordova.getActivity());
        }

        builder
            .setTitle(title)
            .setCancelable(true);


        // Although there is not really anything technically wrong
        // with adding a cancel button, Android typically doesn't use
        // one for this kind of list dialog.
        // We'll allow the user to override the "smart" option and
        // include it if they insist anyway.

        if (androidEnableCancelButton && !TextUtils.isEmpty(addCancelButtonWithLabel)) {
          builder.setNegativeButton(addCancelButtonWithLabel,
              new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                  dialog.cancel();
                  // We catch the cancel event and return
                  // the index then.
                }
              });
        }

        // So what do we do with the iOS destructive button?
        // Android doesn't really have the concept, so we're going to
        // ignore it until we have a situation where we can come up with
        // a good way to implement it. Most likely adding an image
        // or some other indicator.
//        if (!TextUtils.isEmpty(addDestructiveButtonWithLabel)) {
//          builder.setPositiveButton(addDestructiveButtonWithLabel,
//              new OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                  dialog.dismiss();
//                  callbackContext
//                      .sendPluginResult(new PluginResult(
//                          PluginResult.Status.OK, 0));
//                }
//              });
//        }

        final String[] buttons = getStringArray(
            buttonLabels,
            destructiveButtonLast,
            (TextUtils.isEmpty(addDestructiveButtonWithLabel) ? null
                : addDestructiveButtonWithLabel));

        builder.setItems(buttons, new OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            // java 0 based index converted to cordova 1 based
            // index, so we don't confuse the webbies.
            callbackContext.sendPluginResult(new PluginResult(
                PluginResult.Status.OK, which + 1));
          }
        });

        builder.setOnCancelListener(new AlertDialog.OnCancelListener() {
          public void onCancel(DialogInterface dialog) {
            // Match the way the iOS plugin works. Cancel is
            // always the last index and destructive is always the
            // first, if it exists. Even though we don't handle the
            // destructive button, we want the selected index to
            // match.
            int cancelButtonIndex = buttons.length + 1;
            callbackContext.sendPluginResult(new PluginResult(
                PluginResult.Status.OK, cancelButtonIndex));
          }
        });

        dialog = builder.create();
        dialog.show();
      }
    };
    this.cordova.getActivity().runOnUiThread(runnable);
  }

  private String[] getStringArray(JSONArray jsonArray, boolean append, String... additionalButtons) {

    List<String> btns = new ArrayList<String>();

    // Add prefix items
    if (!append) {
      for (String btn : additionalButtons) {
        if (!TextUtils.isEmpty(btn)) {
          btns.add(btn);
        }
      }
    }

    // add the rest of the buttons from the list.
    if (jsonArray != null) {
      for (int i = 0; i < jsonArray.length(); i++) {
        btns.add(jsonArray.optString(i));
      }
    }

    // Add postfix items
    if (append) {
      for (String btn : additionalButtons) {
        if (!TextUtils.isEmpty(btn)) {
          btns.add(btn);
        }
      }
    }
    return btns.toArray(new String[btns.size()]);
  }
}