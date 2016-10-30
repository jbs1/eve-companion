package com.jbs.evecompanion;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;

import javax.net.ssl.HttpsURLConnection;

public class oauth_login extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_oauth_login);

        Intent intent = getIntent();
        Uri data = intent.getData();

        if(data != null){
            new add_char_to_database().execute(data.getQueryParameter("code"));
        }
    }


}


class add_char_to_database extends AsyncTask<String, Void, JSONObject> {
    private Exception exception;
    protected JSONObject doInBackground(String... code) {
        try {
            String url = "https://login.eveonline.com/oauth/token";
            String charset = "UTF-8";
            String grant_type = "authorization_code";
            //encode id / secret
            String enc_auth = Base64.encodeToString((MainActivity.oauth_id+":"+MainActivity.oauth_sec).getBytes(), Base64.NO_WRAP);

            //create body
            String query = null;
            try {
                query = String.format("grant_type=%s&code=%s",
                        URLEncoder.encode(grant_type, charset),
                        URLEncoder.encode(code[0], charset));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            //build connection
            URL obj = new URL(url);
            HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

            con.setRequestMethod("POST");
            con.setRequestProperty("Accept-Charset", charset);
            con.setRequestProperty("Authorization","Basic "+enc_auth);
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=" + charset);

            //print request headers
            /**for (Map.Entry<String, List<String>> req_prop : con.getRequestProperties().entrySet()) {
             Log.i("eve_request_header",req_prop.getKey() + "=" + req_prop.getValue());
             }**/

            //write body
            con.setDoOutput(true);
            try (OutputStream output = con.getOutputStream()) {
                if (query != null) {
                    output.write(query.getBytes(charset));
                }
            }

            //check response headers
            /**for (Map.Entry<String, List<String>> header : con.getHeaderFields().entrySet()) {
             Log.i("eve_response_header",header.getKey() + "=" + header.getValue());
             }**/


            //check response body
            String result;
            StringBuilder sb = new StringBuilder();
            InputStream is;


            is = new BufferedInputStream(con.getInputStream());
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                sb.append(inputLine);
            }
            result = sb.toString();

            JSONObject json_result= new JSONObject(result);




            //get char id and name
            URL verify_obj = new URL("https://login.eveonline.com/oauth/verify");
            HttpsURLConnection verify_con = (HttpsURLConnection) verify_obj.openConnection();
            verify_con.setRequestMethod("GET");
            verify_con.setRequestProperty("Authorization","Bearer "+json_result.getString("access_token"));


            //get answer body

            String ver_res;
            StringBuilder ver_sb = new StringBuilder();
            InputStream ver_is;


            ver_is = new BufferedInputStream(verify_con.getInputStream());
            BufferedReader ver_br = new BufferedReader(new InputStreamReader(ver_is));
            String ver_inputLine;
            while ((ver_inputLine = ver_br.readLine()) != null) {
                ver_sb.append(ver_inputLine);
            }
            ver_res = ver_sb.toString();

            JSONObject verify_json= new JSONObject(ver_res);
            json_result.put("CharacterID",verify_json.getInt("CharacterID"));
            json_result.put("CharacterName",verify_json.getString("CharacterName"));

            return json_result;

        } catch (Exception e) {
            this.exception = e;
            return null;
        }

    }

    protected void onPostExecute(JSONObject response) {
        if(response==null){
            return;
        }

        //Log.i("eve_resp_body",response);

        String access_token;
        String token_type;
        int expires_in;
        String refresh_token;
        int char_id;
        String char_name;
        long valid_unitl;

        try {
            //JSONObject json_response = new JSONObject(response);

            access_token=response.getString("access_token");
            //token_type=response.getString("token_type");
            expires_in=response.getInt("expires_in");
            refresh_token=response.getString("refresh_token");
            char_id=response.getInt("CharacterID");
            char_name=response.getString("CharacterName");


            valid_unitl=System.currentTimeMillis()+(expires_in-10)*1000;

            //Log.i("eve_answer_json",response.toString(4));


            if(MainActivity.myDB.insert_char(access_token,refresh_token,valid_unitl,char_id,char_name)){
                Log.i("eve_chars", "Added char "+char_name+"("+char_id+") to database!");
            } else {
                Log.w("eve_chars", "Could NOT add "+char_name+"("+char_id+") to database!");
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }
}