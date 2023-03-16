package me.mraxetv.beastcore.filemanager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.HashMap;

public class JsonConfig {
    private JavaPlugin pl;
    private File file;
    private JSONObject json; // org.json.simple
    private JSONParser parser = new JSONParser();

    private HashMap<String, Object> defaults = new HashMap<String, Object>();

    //TreeMap<String, Object> treeMap = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);


    public JsonConfig(JavaPlugin pl, String name) {
        this.file = new File(pl.getDataFolder(), name);
        reload();
        //addDefault();
        //save();
    }

    public void reload() {

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        try {
            if (!file.exists()) {
                PrintWriter pw = new PrintWriter(file, "UTF-8");
                pw.print("{");
                pw.print("}");
                pw.flush();
                pw.close();
            }

            json = (JSONObject) parser.parse(new InputStreamReader(new FileInputStream(file), "UTF-8"));

            rewrite();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


    private void rewrite(){
        if(json != null) return;
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(file, "UTF-8");
            pw.print("{");
            pw.print("}");
            pw.flush();
            pw.close();
            json = (JSONObject) parser.parse(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        } catch (FileNotFoundException e) {e.printStackTrace();}
        catch (UnsupportedEncodingException e) {e.printStackTrace();}
        catch (IOException e) {e.printStackTrace();}
        catch (ParseException e) {e.printStackTrace();}


    }

    public boolean save() {
        try {


            Gson g = new GsonBuilder().setPrettyPrinting().create();
            //String prettyJsonString = g.toJson(treeMap);
            String prettyJsonString = g.toJson(json);

            FileWriter fw = new FileWriter(file);
            fw.write(prettyJsonString);
            fw.flush();
            fw.close();

            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public void addShop(String shop){

     if(json.get(shop) != null) return;
        JSONObject jsonObject = new JSONObject();
    jsonObject.put("InStockItems",new JSONObject());
    json.put(shop,jsonObject);

    }

    public void addShopSections(String shop){

        JSONObject jsonShop = (JSONObject) json.get(shop);

        if(!jsonShop.containsKey("InStockItems")){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("InStockItems",new JSONObject());
            json.put(shop,jsonObject);
        }


    }

    public String getRawData(String key) {
        return json.containsKey(key) ? json.get(key).toString() : (defaults.containsKey(key) ? defaults.get(key).toString() : key);
    }


    public String getString(String key) {
        return ChatColor.translateAlternateColorCodes('&', getRawData(key));
    }

    public boolean getBoolean(String key) {
        return Boolean.valueOf(getRawData(key));
    }

    public double getDouble(String key) {
        try {
            return Double.parseDouble(getRawData(key));
        } catch (Exception ex) { }
        return -1;
    }

    public double getInteger(String key) {
        try {
            return Integer.parseInt(getRawData(key));
        } catch (Exception ex) { }
        return -1;
    }

    /*public JSONObject getObject(String key) {
        return json.containsKey(key) ? (JSONObject) json.get(key) : (defaults.containsKey(key) ? (JSONObject) defaults.get(key) : new JSONObject());
    }*/
    public JSONObject getObject(String key) {
        return json.containsKey(key) ? (JSONObject) json.get(key) : null;
    }

    public JSONArray getArray(String key) {
        return json.containsKey(key) ? (JSONArray) json.get(key)
                : (defaults.containsKey(key) ? (JSONArray) defaults.get(key) : new JSONArray());
    }




}
