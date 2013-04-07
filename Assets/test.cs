using UnityEngine;
using System.Collections;
using System.Collections.Generic;

public class test : MonoBehaviour {

    // Use this for initialization
    string myhash =null;
    void Start () {
        fbWrapper.inst.init("com.macaronics.fbonandroid", "FB_APP_ID");
        myhash =fbWrapper.inst.getHashKey();
    }

    void OnGUI()
    {
        if (GUI.Button(new Rect(10,10,100,100), "login"))
        {
            fbWrapper.inst.login();
            myhash =fbWrapper.inst.getHashKey();
        }

        if (GUI.Button(new Rect(10,120,100,100), "logout"))
        {
            fbWrapper.inst.logout();
            queryresult =null;
            myhash =null;
        }

        if (fbWrapper.inst.isSessionAvailable())
        {
            if (GUI.Button(new Rect(10,230,100,100), "invoke query"))
            {
                fbWrapper.inst.invokeQuery("SELECT uid, name, pic_square, is_app_user FROM user WHERE uid IN (SELECT uid2 FROM friend WHERE uid1 =me())", queryCB);
            }

            if (GUI.Button(new Rect(10,340,100,100), "send request"))
            {
                fbWrapper.inst.sendRequest("check out my app !");
            }
        }

        if (myhash !=null)
        {
            GUI.Label(new Rect(120, 10, Screen.width-120, 40), "Hash Key: "+myhash);
        }
        if (queryresult !=null)
        {
            GUI.Label(new Rect(120, 50, Screen.width-120, Screen.height-50), queryresult);
        }
    }

    string queryresult =null;
    void queryCB(bool ret, object data)
    {
        string tmpstr ="";
        List<object> OBJARR =(List<object>)data;
        for (int i=0;i<OBJARR.Count;++i)
        {
            
            Dictionary<string, object> items =(Dictionary<string, object>)OBJARR[i];
            foreach(KeyValuePair<string, object> item in items)
            {
                tmpstr +=(item.Key+":"+item.Value+"\n");
            }
        }

        queryresult =tmpstr;
    }
}
