
package com.macaronics;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Toast;

import com.facebook.*;
import com.facebook.widget.WebDialog;
import com.facebook.widget.WebDialog.OnCompleteListener;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.facebook.Session.NewPermissionsRequest;

import com.unity3d.player.UnityPlayer;
import com.macaronics.iab.overrideActivity;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.util.List;
import java.util.Arrays;
import java.util.Collection;

import android.view.Window;
import android.view.WindowManager;

public class fbWrapper
{
    private String myHash;
    private String mEventHandler;
    private Activity mActivity;
    private String mAppId;
    
    public fbWrapper(String packageName, String applicationId, String strEventHandler){

        mActivity =UnityPlayer.currentActivity;
        mEventHandler =strEventHandler;
        mAppId =applicationId;
        
        //--------------------
        //fetch keyhash
        try
        {
            PackageInfo info =mActivity.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            for (Signature sig : info.signatures){
                MessageDigest md =MessageDigest.getInstance("SHA");
                md.update(sig.toByteArray());
                myHash =Base64.encodeToString(md.digest(), Base64.DEFAULT);
            }
        }
        catch(NameNotFoundException e){
            Log.d("Unity::fbWrapper()", "error on fetch haskkey -name not found");
        }
        catch(NoSuchAlgorithmException e){
            Log.d("Unity::fbWrapper()", "error on fetch haskkey -no such algorithm");
        }

        
        //--------------------
        //register callback (onActivityResult)
        overrideActivity.registerOnActivityResultCBFunc(
            new overrideActivity.cbEvent(){
                public boolean cbEvent(int requestCode, int resultCode, Intent data)
                {
                    Session.getActiveSession().onActivityResult(mActivity, requestCode, resultCode, data);
                    return false;
                }
            });
        
        //--------------------
        //register callback (onSaveInstanceState)
        overrideActivity.registerOnSaveInstanceSataeCBFunc(
            new overrideActivity.cbSaveInstState(){
                public boolean cbSaveInstState(Bundle outState)
                {
                    Session session = Session.getActiveSession();
                    Session.saveSession(session, outState);
                    return false;
                }
            });
        
        //--------------------
		//facebook
        Bundle savedInstanceState =overrideActivity.getSavedInstanceState();
		Settings.addLoggingBehavior(LoggingBehavior.INCLUDE_ACCESS_TOKENS);
		Session session =Session.getActiveSession();
        if (session == null) {
            if (savedInstanceState != null) {
                session = Session.restoreSession(mActivity, null, statusCallback, savedInstanceState);
                Log.d("Unity::onCreate()", "Session.restoreSession()");
            }
            if (session == null) {
            	session = new Session.Builder(mActivity).setApplicationId(mAppId).build();
                Log.d("Unity::onCreate()", "new Session.Builder(this)");
            }
            
            Session.setActiveSession(session);
            if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
                session.openForRead(new Session.OpenRequest(mActivity).setCallback(statusCallback));
                Log.d("Unity::onCreate()", "openForRead()");
            }
        }        
    }

    public void dispose()
    {
        Log.d("Unity::dispose()", "dispose called");
    }
    
    public boolean isSessionOpened()
    {
        Session session =Session.getActiveSession();
        if (session !=null && session.isOpened()==true && session.getAccessToken() !=null)
        {
            return true;
        }
        
        return false;
    }

    public String getHash()
    {
        return myHash;
    }
    
    public void login()
    {
        Session session = Session.getActiveSession();
        if (!session.isOpened() && !session.isClosed()) {
            Log.d("Unity::onLogin()","session.openForRead()");
            session.openForRead(new Session.OpenRequest(mActivity).setCallback(statusCallback));
        }else if (session !=null){
            Log.d("Unity::onLogin()","Session.openActiveSession()");

            //regenerate session
            Session newsession = new Session.Builder(mActivity).setApplicationId(mAppId).build();
            Session.setActiveSession(newsession);
            newsession.openForRead(new Session.OpenRequest(mActivity).setCallback(statusCallback));
        }
        else
        {
        	Log.d("Unity", "error -session ==null");
        }
    }
    
    public void logout()
    {
    	Log.d("Unity::onLogout()","on logout called !");
        Session session = Session.getActiveSession();
        if (!session.isClosed()) {
            session.closeAndClearTokenInformation();
            Log.d("Unity::onLogout()","session.closeAndClearTokenInformation()");
        }
    }
    
    public void invokeQuery(String fqlQueryStr)
    {
    	Session session =Session.getActiveSession();
    	if (session.isOpened()==false)
    	{
    		Log.d("Unity::invokeQuery()", "invalid session");
    		return;
    	}

    	Bundle params =new Bundle();
    	params.putString("q", fqlQueryStr);

    	final Request request =new Request(session,
    			"/fql",
    			params,
    			HttpMethod.GET,
    			new Request.Callback(){
    				public void onCompleted(Response response){
                        GraphObject graphObject = response.getGraphObject();
                        if (graphObject !=null)
                        {
                            if (graphObject.getProperty("data")!=null &&
                                mEventHandler !=null)
                            {
                                try{
                                    String arr =graphObject.getProperty("data").toString();
                                    JSONArray pj =new JSONArray(arr);
                                    UnityPlayer.UnitySendMessage(mEventHandler, "msgReceiver", "{\"ret\":\"true\",\"dat\":"+pj.toString()+"}");
                                }
                                catch(Exception e)
                                {
                                    if (mEventHandler!=null)
                                        UnityPlayer.UnitySendMessage(mEventHandler, "msgReceiver", "{\"ret\":\"false\"}");
                                }
                                
                            }
                            else
                            {
                                if (mEventHandler!=null)
                                    UnityPlayer.UnitySendMessage(mEventHandler, "msgReceiver", "{\"ret\":\"false\"}");
                            }
                            
                        }
                        else
                        {
                            if (mEventHandler!=null)
                                UnityPlayer.UnitySendMessage(mEventHandler, "msgReceiver", "{\"ret\":\"false\"}");
                        }
                        
    				}

    			});

    	Log.d("Unity::invokeQuery()", "send fql request...");
        
        mActivity.runOnUiThread(new Runnable(){
            public void run(){
                Request.executeBatchAsync(request);                
            }
        });


    }

    public void sendRequest(String reqMessage)
    {
    	final Session session =Session.getActiveSession();
    	if (session.isOpened()==false)
    	{
    		Log.d("Unity::SendRequest()", "invalid session");
    		return;
    	}

    	final Bundle params =new Bundle();
    	params.putString("message", reqMessage);

    	final Activity act =mActivity;

        mActivity.runOnUiThread(new Runnable(){
            public void run(){
                
                WebDialog requestDialog =(
                    new WebDialog.RequestsDialogBuilder(act,
                        session.getActiveSession(),
                        params))
                        .setOnCompleteListener(new OnCompleteListener(){

                            @Override
                            public void onComplete(Bundle values,
                                    FacebookException error){
                                if (error !=null)
                                {
                                    if (error instanceof FacebookOperationCanceledException){
                                        Toast.makeText(act.getApplicationContext(),
                                        "Request cancelled",
                                        Toast.LENGTH_SHORT).show();
                                    }
                                    else
                                    {
                                        Toast.makeText(act.getApplicationContext(), "Network Error", Toast.LENGTH_SHORT).show();
                                    }
                                }
                                else
                                {
                                    final String requestId =values.getString("request");
                                    if (requestId !=null)
                                    {
                                        Toast.makeText(act.getApplicationContext(),
                                        "Request sent",
                                        Toast.LENGTH_SHORT).show();
                                    }
                                    else
                                    {
                                        Toast.makeText(act.getApplicationContext(),
                                        "Request cancelled",
                                        Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                    })
                    .build();
                requestDialog.show();
                
            }
        });

    }

    public void makeMeRequest()
    {
        final Session session =Session.getActiveSession();
        if (session.isOpened()==false)
        {
            Log.d("Unity::makeMeRequest()", "invalid session");
            return;
        }

        // Make an API call to get user data and define a  new callback to handle the response.
        final Request request = Request.newMeRequest(session, new Request.GraphUserCallback() {
            @Override
            public void onCompleted(GraphUser user, Response response) {
                // If the response is successful
                if (session == Session.getActiveSession()) {
                    if (user != null) {
                        // Set the id for the ProfilePictureView
                        // view that in turn displays the profile picture.
                        //profilePictureView.setProfileId(user.getId());
                        // Set the Textview's text to the user's name.
                        //userNameView.setText(user.getName());
                        Log.d("Unity::makeMeRequest()", "user.getId()="+user.getId());
                        Log.d("Unity::makeMeRequest()", "user.getName()="+user.getName());
                        Log.d("Unity::makeMeRequest()", "user.getFirstName()="+user.getFirstName());
                        Log.d("Unity::makeMeRequest()", "user.getMiddleName()="+user.getMiddleName());
                        Log.d("Unity::makeMeRequest()", "user.getLastName()="+user.getLastName());
                        Log.d("Unity::makeMeRequest()", "user.getLink()="+user.getLink());
                        Log.d("Unity::makeMeRequest()", "user.getUsername()="+user.getUsername());
                        Log.d("Unity::makeMeRequest()", "user.getBirthday()="+user.getBirthday());
                        Log.d("Unity::makeMeRequest()", "user.getLocation()="+user.getLocation());

                        
                        if (mEventHandler !=null)
                        {
                            String str ="[";

                            str+="{\"Id\":\""+user.getId()+"\"},";
                            str+="{\"Name\":\""+user.getName()+"\"},";
                            str+="{\"FirstName\":\""+user.getFirstName()+"\"},";
                            str+="{\"MiddleName\":\""+user.getMiddleName()+"\"},";
                            str+="{\"LastName\":\""+user.getLastName()+"\"},";
                            str+="{\"Link\":\""+user.getLink()+"\"},";
                            str+="{\"Username\":\""+user.getUsername()+"\"},";
                            str+="{\"Birthday\":\""+user.getBirthday()+"\"},";
                            str+="{\"Location\":\""+((user.getLocation()==null) ? "null":(user.getLocation().toString()))+"\"}";

                            str +="]";

                            UnityPlayer.UnitySendMessage(mEventHandler, "msgReceiver", "{\"ret\":\"true\",\"dat\":"+str+"}");
                        }

                    }
                }

                if (response.getError() != null) {
                    // Handle errors, will do so later.
                    Log.d("Unity::makeMeRequest()", "response.getError() !=null");
                }
            }
        });

        Log.d("Unity::invokeQuery()", "send newMeRequest request...");
        
        mActivity.runOnUiThread(new Runnable(){
            public void run(){        
                //request.executeAsync();
                Request.executeBatchAsync(request);
            }});
    }
    
    private Session.StatusCallback statusCallback = new SessionStatusCallback();
    private class SessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
            if (state.isOpened())
            {
                Log.d("Unity::onSessionStateChange()", "state.isOpened()");
            }
            else
            {
                Log.d("Unity::onSessionStateChange()", "state.isOpened() ===false");
            }
        }
    }

    //
    //ref:http://developers.facebook.com/docs/howtos/androidsdk/3.0/publish-to-feed/
    //
    private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
    private static final String PENDING_PUBLISH_KEY = "pendingPublishReauthorization";
    private boolean pendingPublishReauthorization = false;

    private boolean isSubsetOf(Collection<String> subset, Collection<String> superset) {
        for (String string : subset) {
            if (!superset.contains(string)) {
                return false;
            }
        }
        return true;
    }

    // Show a dialog (feed or request) without a notification bar (i.e. full screen)
    // Parameters of a WebDialog that should be displayed
    private WebDialog dialog = null;
    private String dialogAction = null;
    private Bundle dialogParams = null;    
    private void showDialogWithoutNotificationBar(String action, Bundle params) {
        // Create the dialog
        dialog = new WebDialog.Builder(mActivity, Session.getActiveSession(), action, params).setOnCompleteListener(
                new WebDialog.OnCompleteListener() {

            @Override
            public void onComplete(Bundle values, FacebookException error) {
                if (error != null && !(error instanceof FacebookOperationCanceledException)) {
                    //((HomeActivity)getActivity()).showError(getResources().getString(R.string.network_error), false);
                }
                dialog = null;
                dialogAction = null;
                dialogParams = null;
            }
        }).build();

        // Hide the notification bar and resize to full screen
        Window dialog_window = dialog.getWindow();
        dialog_window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        // Store the dialog information in attributes
        dialogAction = action;
        dialogParams = params;
        
        // Show the dialog
        dialog.show();
    }

    public void publishStory(final String name, final String caption, final String desc, final String link, final String imageUrl)
    {

        final Session session = Session.getActiveSession();

        if (session != null){

            mActivity.runOnUiThread(new Runnable(){
                public void run(){

                    // Check for publish permissions    
                    List<String> permissions = session.getPermissions();
                    if (!isSubsetOf(PERMISSIONS, permissions)) {
                        pendingPublishReauthorization = true;
                        Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(mActivity, PERMISSIONS);
                        session.requestNewPublishPermissions(newPermissionsRequest);
                        return;
                    }

                    Bundle postParams = new Bundle();
                    postParams.putString("name", name);
                    postParams.putString("caption", caption);
                    postParams.putString("description", desc);
                    postParams.putString("link", link);
                    postParams.putString("picture", imageUrl);

                    /*
                    Request.Callback callback= new Request.Callback() {
                        public void onCompleted(Response response) {
                            JSONObject graphResponse = response.getGraphObject().getInnerJSONObject();
                            String postId = null;
                            try {
                                postId = graphResponse.getString("id");
                            } catch (JSONException e) {
                                Log.i("Unity::publishStories", "JSON error "+ e.getMessage());
                            }
                            FacebookRequestError error = response.getError();
                            if (error != null) {
                                Toast.makeText(mActivity.getApplicationContext(), error.getErrorMessage(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(mActivity.getApplicationContext(), postId, Toast.LENGTH_LONG).show();
                            }
                        }
                    };

                    Request request = new Request(session, "me/feed", postParams, HttpMethod.POST, callback);
                    RequestAsyncTask task = new RequestAsyncTask(request);
                    task.execute();
                    */
                    showDialogWithoutNotificationBar("feed", postParams);
                }
            });

        }

    }

}


