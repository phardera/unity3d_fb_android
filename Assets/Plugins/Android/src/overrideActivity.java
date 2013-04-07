
package com.macaronics.iab;

import com.unity3d.player.UnityPlayerActivity;
import android.os.Bundle;
import android.util.Log;
import android.content.Intent;

public class overrideActivity extends UnityPlayerActivity {
    public interface cbEvent{
        public boolean cbEvent(int requestCode, int resultCode, Intent data);
    }
    
    public interface cbSaveInstState{
        public boolean cbSaveInstState(Bundle outState);
    }

    protected cbEvent ie;
    protected cbSaveInstState isis;
    protected Bundle mSavedInstState;

    static protected overrideActivity inst;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        inst =this;
        mSavedInstState =savedInstanceState;
        
        // print debug message to logcat
        Log.d("Unity::overrideActivity", "onCreate called!");
    }

    @Override
	public void onDestroy(){
        super.onDestroy();
        inst =null;
        Log.d("Unity::overrideActivity", "onDestroy called!");
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        Log.d("Unity::overrideActivity", "onActivityResult called!");
        
        boolean ret =false;
        if (ie !=null){
            try{
                ret =ie.cbEvent(requestCode, resultCode, data);
            }
            catch(Exception e){
                ret =false;
            }
        }

        if (ret ==false){
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.d("Unity::overrideActivity", "onSaveInstanceState called!");
    
        boolean ret =false;
        
        if (isis !=null)
        {
            try{
                ret =isis.cbSaveInstState(outState);
            }
            catch(Exception e){
                ret =false;
            }
        }
        
        if (ret ==false){
            super.onSaveInstanceState(outState);
        }
    }
    
    static public void registerOnActivityResultCBFunc(final cbEvent pcbfunc){
        if (inst !=null)
            inst.ie =pcbfunc;
    }
    
    static public void registerOnSaveInstanceSataeCBFunc(final cbSaveInstState pcbfunc){
        if (inst !=null)
            inst.isis =pcbfunc;
    }
    
    static public Bundle getSavedInstanceState()
    {
        if (inst !=null)
            return inst.mSavedInstState;
        
        return null;
    }
} 