package com.rekaciptaselera.generatorpo;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Native local persistence. No browser storage or HTML; snapshot before each Sheet import. */
public final class StateStore {
    private final File stateFile,rollbackFile;
    public StateStore(Context c){stateFile=new File(c.getFilesDir(),"generator_po_state.json");rollbackFile=new File(c.getFilesDir(),"before_import.json");}
    public PoRules.State load(){try{return parse(new JSONObject(read(stateFile)));}catch(Exception e){return new PoRules.State();}}
    public void save(PoRules.State state)throws Exception{write(stateFile,serialize(state).toString());}
    public void snapshot(PoRules.State state)throws Exception{write(rollbackFile,serialize(state).toString());}
    public boolean hasRollback(){return rollbackFile.exists();}
    public PoRules.State rollback()throws Exception{if(!hasRollback())throw new IOException("Snapshot rollback belum tersedia.");PoRules.State s=parse(new JSONObject(read(rollbackFile)));save(s);return s;}
    public String export(PoRules.State s) {
        JSONObject x = serialize(s);
        try {
            x.put("format", "GeneratorPO-Native-v1");
            x.put("exportedAt", new Date().toString());
            return x.toString(2);
        } catch (org.json.JSONException e) {
            return x.toString();
        }
    }
    public PoRules.State importBackup(String json)throws Exception{JSONObject root=new JSONObject(json);PoRules.State s=parse(root.has("state")?root.getJSONObject("state"):root);save(s);return s;}
    private static String read(File f)throws Exception{try(FileInputStream in=new FileInputStream(f)){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))>=0)b.write(buf,0,n);return b.toString(StandardCharsets.UTF_8.name());}}
    private static void write(File file,String value)throws Exception{File tmp=new File(file.getParentFile(),file.getName()+".tmp");try(FileOutputStream out=new FileOutputStream(tmp)){out.write(value.getBytes(StandardCharsets.UTF_8));out.getFD().sync();}if(file.exists()&&!file.delete())throw new IOException("Tidak dapat memperbarui data lama");if(!tmp.renameTo(file))throw new IOException("Gagal menyimpan data lokal");}
    public static JSONObject serialize(PoRules.State s){JSONObject root=new JSONObject();try{
        root.put("inputQtyMode","pcs_for_adpa_fillet_v1");root.put("allowFallback",s.allowFallback);root.put("supplierRestrictionsEnabled",s.restrictionsEnabled);
        root.put("prices",new JSONObject(s.prices));root.put("routes",new JSONObject(s.routes));root.put("noteExceptions",new JSONObject(s.noteExceptions));
        JSONObject rules=new JSONObject();for(Map.Entry<String,List<String>> e:s.restrictions.entrySet())rules.put(e.getKey(),new JSONArray(e.getValue()));root.put("supplierRestrictions",rules);
        JSONArray stocks=new JSONArray();for(PoRules.Stock x:s.stocks)stocks.put(new JSONObject().put("supplier",x.supplier).put("sku",x.sku).put("warehouse",x.warehouse==null?"":x.warehouse).put("qty",x.qty));root.put("stocks",stocks);
        JSONArray demands=new JSONArray();for(PoRules.Demand x:s.demands)demands.put(new JSONObject().put("outlet",x.outlet).put("sku",x.sku).put("preferred",x.preferred).put("request",x.request).put("ship",x.ship).put("qty",x.qty));root.put("demands",demands);
    }catch(Exception e){throw new IllegalStateException(e);}return root;}
    public static PoRules.State parse(JSONObject root){PoRules.State st=new PoRules.State();
        st.allowFallback=root.optBoolean("allowFallback",false);st.restrictionsEnabled=root.optBoolean("supplierRestrictionsEnabled",false);
        JSONObject prices=root.optJSONObject("prices");if(prices!=null){for(String k:keys(prices)){double p=prices.optDouble(k,Double.NaN);if(!Double.isNaN(p)&&p>=0)st.prices.put(k,p);}}
        JSONObject routes=root.optJSONObject("routes");if(routes!=null)for(String k:keys(routes))if(Arrays.asList(PoRules.ROUTES).contains(routes.optString(k)))st.routes.put(k,routes.optString(k));
        JSONObject notes=root.optJSONObject("noteExceptions");if(notes!=null)for(String k:keys(notes))if(Arrays.asList(PoRules.NOTES).contains(notes.optString(k)))st.noteExceptions.put(k,notes.optString(k));
        JSONObject restricted=root.optJSONObject("supplierRestrictions");if(restricted!=null)for(String k:keys(restricted)){JSONArray a=restricted.optJSONArray(k);if(a!=null){List<String> list=new ArrayList<>();for(int j=0;j<a.length();j++)list.add(a.optString(j));st.restrictions.put(k,list);}}
        JSONArray ss=root.optJSONArray("stocks");if(ss!=null)for(int i=0;i<ss.length();i++){JSONObject x=ss.optJSONObject(i);if(x==null)continue;String s=x.optString("supplier"),sku=x.optString("sku");double q=x.optDouble("qty",0);if(Arrays.asList(PoRules.SUPPLIERS).contains(s)&&Arrays.asList(PoRules.SKU).contains(sku)&&q>=0){String w=x.optString("warehouse","");st.stocks.add(new PoRules.Stock(s,sku,w,q));}}
        boolean oldPack=!root.has("inputQtyMode")||!"pcs_for_adpa_fillet_v1".equals(root.optString("inputQtyMode"));
        JSONArray ds=root.optJSONArray("demands");if(ds!=null)for(int i=0;i<ds.length();i++){JSONObject x=ds.optJSONObject(i);if(x==null)continue;String o=x.optString("outlet"),sku=x.optString("sku");double q=x.optDouble("qty",0);
            if(Arrays.asList(PoRules.OUTLETS).contains(o)&&Arrays.asList(PoRules.SKU).contains(sku)&&q>=0){if(oldPack&&!"utuh".equals(sku))q*=10;String p=x.optString("preferred","auto");if(p.isEmpty())p="auto";String req=x.optString("request",today()),ship=x.optString("ship",today());st.demands.add(new PoRules.Demand(o,sku,p,req,ship,q));}}
        return st;
    }
    private static List<String> keys(JSONObject o){List<String> l=new ArrayList<>();Iterator<String> it=o.keys();while(it.hasNext())l.add(it.next());return l;}
    public static String today(){return new java.text.SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
}
