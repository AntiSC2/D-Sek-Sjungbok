package com.sjung.sjungbok;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import android.widget.PopupWindow;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

public class JSONDownloader extends AsyncTask<Void, String, String> {
    PopupWindow progressWindow;
    Context context;
    private int songListSizeBefore = 0;
    private ArrayList<Song> tempList;
    private static final String TAG = "JSONDownloader";

    public JSONDownloader(Context context) {
        this.context = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progressWindow = new PopupWindow(300, 300);
        progressWindow.showAsDropDown(null, 0, 0);
    }

    @Override
    protected String doInBackground(Void... params) {
        String result = "";

        URL url = null;
        HttpURLConnection urlConnection = null;

        try {
            url = new URL("https://dsek.se/arkiv/sanger/api.php?showAll");
            urlConnection = (HttpURLConnection) url.openConnection();

            InputStream in = urlConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder sb = new StringBuilder();

            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }

            result = sb.toString();
            Log.d(TAG, result);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        JSONObject jObject = null;
        Map<String, JSONObject> map = new HashMap<String, JSONObject>();

        try {
            jObject = new JSONObject(result);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (jObject == null) {
            return null;
        }

        Iterator iter = jObject.keys();

        ArrayList<String> favoriteTitles = new ArrayList<String>();
        HashSet<String> melodiesIncluded = new HashSet<String>();
        ArrayList<Song> snuskigaVisor = new ArrayList<Song>();

        SongListWrapper.songList.addAll(MainActivity.snuskigaVisor);

        for (int i = 0; i < SongListWrapper.songList.size(); i++) {
            if (SongListWrapper.songList.get(i).isFavorite()) {
                favoriteTitles.add(SongListWrapper.songList.get(i).getTitle());
            }
            if (!SongListWrapper.songList.get(i).getMidFile().equals("")
                    && !SongListWrapper.songList.get(i).getMidFile().equals("null")) {
                melodiesIncluded.add(SongListWrapper.songList.get(i).getMidFile());
                if (SongListWrapper.songList.get(i).getMidFile().contains("obla")) {
                    Log.d(TAG, "'" + SongListWrapper.songList.get(i).getMidFile() + "'");
                    Log.d(TAG, SongListWrapper.songList.get(i).getTitle());
                }
            }
            if (!SongListWrapper.songList.get(i).forAll()) {
                snuskigaVisor.add(SongListWrapper.songList.get(i));
            }
        }
        songListSizeBefore = SongListWrapper.songList.size();
        tempList = new ArrayList<Song>();
        SongListWrapper.songList = new ArrayList<Song>();
        long lastModified;
        String category = "";
        String melodyFile = "";
        boolean favorite = false;
        String melodyURL;
        String tempMod;
        while (iter.hasNext()) {
            String key = (String) iter.next();
            JSONObject value = null;
            try {
                value = jObject.getJSONObject(key);
                favorite = favoriteTitles.contains(getCorrectSwedishLetters(value.getString("title")).trim());

                lastModified = value.getLong("modified");
                if (lastModified == 0) {
                    lastModified = value.getLong("created");
                }
                category = value.getString("categoryTitle");
                if (category.equals("null")) {
                    category = "Andra visor";
                }
                melodyFile = value.getString("melodySoundclip");
                if (!melodyFile.equals("") && !melodyFile.equals("null") && melodyFile.contains(".mid")) {
                    tempMod = melodyFile;
                    tempMod = tempMod.replace(".mid", "");
                    tempMod += "__downloaded.mid";
                    if (!melodiesIncluded.contains(melodyFile) && !melodiesIncluded.contains(tempMod)) {
                        melodyURL = melodyFile;
                        melodyFile = melodyFile.replace(".mid", "");
                        melodyFile += "__downloaded.mid";
                        Log.d(TAG, "meolodyURL: " + melodyURL);
                        Log.d(TAG, "meolodyFILE: " + melodyFile);
                        downloadFile(melodyURL, context.getFilesDir() + File.separator + melodyFile);
                        melodiesIncluded.add(melodyFile);
                    }
                }
                // SongListWrapper.songList.add(new
                // Song(getCorrectSwedishLetters(value.getString("title")).trim(),
                // getCorrectSwedishLetters(value.getString("melodyTitle")),
                // value.getString("lyrics"), favorite, category, lastModified,
                // melodyFile,true));
                tempList.add(new Song(getCorrectSwedishLetters(value.getString("title")).trim(),
                        getCorrectSwedishLetters(value.getString("melodyTitle")), value.getString("lyrics"), favorite,
                        category, lastModified, melodyFile, true));

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        tempList.addAll(snuskigaVisor);

        Collections.sort(tempList, new Comparator<Song>() {
            @Override
            public int compare(Song a, Song b) {
                return a.compareTo(b);

            }
        });

        BufferedWriter bufferedWriter;
        try {

            bufferedWriter = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(context.getFilesDir() + File.separator + "Songs.txt"),
                            StandardCharsets.UTF_8));
            for (int i = 0; i < tempList.size(); i++)
                bufferedWriter.write(tempList.get(i).writeToFileFormat());
            bufferedWriter.close();
        } catch (IOException e) {

            e.printStackTrace();
        }

        try {
            bufferedWriter = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(context.getFilesDir() + File.separator + "History.txt"),
                            StandardCharsets.UTF_8));
            bufferedWriter.write("");
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        SharedPreferences settings = context.getSharedPreferences("SETTINGS", 0);
        boolean loggedIn = settings.getBoolean("LoggedIn", false);
        for (int i = 0; i < tempList.size(); i++) {
            Song temp = tempList.get(i);
            if (loggedIn || temp.forAll()) {
                SongListWrapper.songList.add(temp);
            }
        }

        return null;
    }

    protected void onPostExecute(String result) {
        progressWindow.dismiss();
        Toast.makeText(context, tempList.size() - songListSizeBefore + " nya sånger har lagts till", Toast.LENGTH_LONG)
                .show();
        Log.d(TAG, "AsyncTask DONE!");
    }

    private String getCorrectSwedishLetters(String text) {
        text = text.replaceAll("&#039;", "'");
        text = text.replaceAll("&aring;", "å");
        text = text.replaceAll("&Aring;", "Å");
        text = text.replaceAll("&auml;", "ä");
        text = text.replaceAll("&Auml;", "Ä");
        text = text.replaceAll("&ouml;", "ö");
        text = text.replaceAll("&Ouml;", "Ö");
        text = text.replaceAll("&amp;", "&");
        text = text.replaceAll("&quot;", "\"");
        text = text.replaceAll("&#8221;", "\"");
        text = text.replaceAll("&#180;", "'");
        text = text.replaceAll("&#8217;", "’");
        text = text.replaceAll("&#8230;", "...");
        text = text.replaceAll("&#8220;", "\"");
        text = text.replaceAll("&#8221;", "\"");
        text = text.replaceAll("&#65533;", "e");

        return text;
    }

    private void downloadFile(String url, String outputFilePath) {
        Log.d(TAG, "Downloading file" + url + ", output filepath: " + outputFilePath);
        url = "http://www.dsek.se/arkiv/sanger/ljud/" + url;
        File outputFile = new File(outputFilePath);
        try {
            URL u = new URL(url);
            URLConnection conn = u.openConnection();
            int contentLength = conn.getContentLength();

            DataInputStream stream = new DataInputStream(u.openStream());

            byte[] buffer = new byte[contentLength];
            stream.readFully(buffer);
            stream.close();

            DataOutputStream fos = new DataOutputStream(new FileOutputStream(outputFile));
            fos.write(buffer);
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, outputFilePath);
            e.printStackTrace();
            Log.e(TAG, "File could not be downloaded: FileNotFoundException");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "File could not be downloaded: IOException");
        }
    }
}
