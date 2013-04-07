
using UnityEngine;

using System.Collections;
using System.Collections.Generic;

public class fbWrapper : MonoBehaviour {

    AndroidJavaObject fbWrapperObj =null;
    public static fbWrapper inst =null;

	// Use this for initialization
	void Start () {
        inst =this;
	}

    void OnApplicationQuit(){
        dispose();
        inst =null;
    }

    public bool init(string unity_bundle_id, string fb_app_id)
    {
        dispose();
        fbWrapperObj =new AndroidJavaObject("com.macaronics.fbWrapper", new object[3]{unity_bundle_id, fb_app_id, "fbWrapper"});

        return true;
    }

    public void dispose()
    {
        if (fbWrapperObj !=null)
        {
            fbWrapperObj.Call("dispose");
            fbWrapperObj.Dispose();
            fbWrapperObj =null;
        }
    }

    void msgReceiver(string msg)
    {
        if (tmpCBFunc !=null)
        {
            Dictionary<string, object> cache =(Dictionary<string, object>)MiniJSON.Json.Deserialize(msg);
            if (cache.ContainsKey("ret")==true)
            {
                string ret =(string)cache["ret"];
                if (ret=="false")
                {
                    tmpCBFunc(false, null);
                }
                else
                {
                    if (cache.ContainsKey("dat")==true)
                    {
                        tmpCBFunc(true, cache["dat"]);
                    }
                }
            }
            else
            {
                tmpCBFunc(false, null);
            }
        }
    }

    //目前是否已登入
    public bool isSessionAvailable()
    {
        if(fbWrapperObj==null)
            return false;

        return fbWrapperObj.Call<bool>("isSessionOpened");
    }

    //登入
    public bool login()
    {
        if (fbWrapperObj !=null)
        {
            fbWrapperObj.Call("login");
            return true;
        }

        return false;
    }

    //登出
    public bool logout()
    {
        if (fbWrapperObj !=null)
        {
            fbWrapperObj.Call("logout");
            return true;
        }
        return false;
    }

    public string getHashKey()
    {
        string myhash =fbWrapperObj.Call<string>("getHash");
        if (myhash ==null)
            myhash ="package name not found";

        return myhash;
    }

    public delegate void queryCBFunc(bool ret, object data);
    protected queryCBFunc tmpCBFunc =null;
    public bool invokeQuery(string fql, queryCBFunc pcbfunc)
    {
        tmpCBFunc =pcbfunc;
        if (fbWrapperObj !=null)
        {
            fbWrapperObj.Call("invokeQuery", new object[1]{fql});
            return true;
        }
        return false;
    }

    //送出邀請
    public bool sendRequest(string message)
    {
        if (fbWrapperObj !=null)
        {
            fbWrapperObj.Call("sendRequest", new object[1]{message});
            return true;
        }
        return false;
    }

}
