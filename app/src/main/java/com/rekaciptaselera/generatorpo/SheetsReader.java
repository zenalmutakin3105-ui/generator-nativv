package com.rekaciptaselera.generatorpo;

import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;

/** Read Google Sheets privately with short-lived OAuth read-only bearer tokens. */
public final class SheetsReader {
    public static final String SOURCE_ID="1_Dyps9wFg5ZX4MZFqRptVzcK14CRF3lc1pGC7FWIn40";
    public static final String TAB="Resume Ayam";
    private static final String BASE="https://sheets.googleapis.com/v4/spreadsheets/"+SOURCE_ID;
    public static class Group {public final String date;public final int startCol;public final String label;
        public Group(String d,int c,String l){date=d;startCol=c;label=l;}public String toString(){return date+"  •  "+label;} }
    public static class QtyRow {public String outlet;public double dada,utuh,fillet;
        public QtyRow(String o,double a,double b,double c){outlet=o;dada=a;utuh=b;fillet=c;}}
    public static class Import {public Group group;public List<QtyRow> rows=new ArrayList<>();}
    public static List<Group> fetchDates(String token)throws Exception{
        String range="'"+TAB+"'!A1:ZZ3";
        String url=BASE+"?includeGridData=true&ranges="+enc(range)+"&fields="+enc("sheets(data(rowData(values(formattedValue,effectiveFormat(backgroundColor)))))");
        JSONObject root=get(url,token);JSONArray sheets=root.optJSONArray("sheets");if(sheets==null||sheets.length()==0)throw new Exception("Header Resume Ayam tidak tersedia.");
        JSONArray data=sheets.getJSONObject(0).optJSONArray("data");if(data==null||data.length()==0)throw new Exception("Header sumber kosong.");
        JSONArray rows=data.getJSONObject(0).optJSONArray("rowData");if(rows==null||rows.length()<3)throw new Exception("Baris 1–3 tidak terbaca.");
        JSONArray row1=vals(rows,0),row2=vals(rows,1),row3=vals(rows,2);
        List<Group> found=new ArrayList<>();int max=Math.max(row1.length(),row3.length());
        for(int c=0;c<max-2;c++){
            if(!norm(value(row1,c)).contains("roppic"))continue;
            if(!norm(value(row3,c)).contains("dada")||!norm(value(row3,c+1)).contains("utuh")||!norm(value(row3,c+2)).contains("fillet"))continue;
            int pink=0;for(int j=0;j<3;j++)if(isPink(cell(row3,c+j)))pink++;
            if(pink<2)continue; // Explicitly excludes yellow Pak Zaenal group.
            String label=value(row2,c),date=parseDate(label);
            if(date!=null)found.add(new Group(date,c,label));
        }
        Map<String,Group> byDate=new LinkedHashMap<>();for(Group g:found)byDate.put(g.date,g); // last valid rightmost wins
        List<Group> dates=new ArrayList<>(byDate.values());dates.sort(Comparator.comparing((Group g)->g.date));
        if(dates.isEmpty())throw new Exception("Blok RO–PPIC pink tidak ditemukan. Periksa baris 1–3 dan warna header.");
        return dates;
    }
    public static Import fetchRows(String token,Group group)throws Exception{
        String range="'"+TAB+"'!A4:ZZ120";
        String url=BASE+"/values/"+enc(range)+"?valueRenderOption=UNFORMATTED_VALUE";
        JSONObject json=get(url,token);JSONArray values=json.optJSONArray("values");if(values==null)throw new Exception("Nilai Qty kosong.");
        Map<String,QtyRow> matched=new HashMap<>();
        for(int r=0;r<values.length();r++){
            JSONArray line=values.optJSONArray(r);if(line==null)continue;
            String outlet=raw(line,1).trim();String canonical=null;
            for(String known:PoRules.OUTLETS)if(norm(known).equals(norm(outlet))){canonical=known;break;}
            if(canonical==null)continue;
            if(matched.containsKey(canonical))throw new Exception("Outlet ganda di sheet: "+canonical);
            double a=qty(line,group.startCol),b=qty(line,group.startCol+1),c=qty(line,group.startCol+2);
            matched.put(canonical,new QtyRow(canonical,a,b,c));
        }
        List<String> missing=new ArrayList<>();Import result=new Import();result.group=group;
        for(String outlet:PoRules.OUTLETS){QtyRow q=matched.get(outlet);if(q==null)missing.add(outlet);else result.rows.add(q);}
        if(!missing.isEmpty())throw new Exception("Outlet tidak ditemukan: "+String.join(", ",missing));
        if(result.rows.size()!=29)throw new Exception("Jumlah outlet tidak sama dengan master 29 outlet.");
        return result;
    }
    private static double qty(JSONArray row,int col)throws Exception{
        Object val=col>=row.length()?"":row.opt(col);
        if(val==null||val==JSONObject.NULL||String.valueOf(val).trim().isEmpty())throw new Exception("Qty kosong pada kolom "+(col+1)+". Isi 0 jika tidak diperlukan.");
        double n;try{n=val instanceof Number?((Number)val).doubleValue():Double.parseDouble(String.valueOf(val).replace(",",""));}catch(Exception e){throw new Exception("Qty bukan angka pada kolom "+(col+1));}
        if(!Double.isFinite(n)||n<0||Math.rint(n)!=n)throw new Exception("Qty harus bilangan bulat 0 atau lebih pada kolom "+(col+1));
        return n;
    }
    private static String raw(JSONArray arr,int col){return col>=arr.length()?"":String.valueOf(arr.opt(col));}
    private static JSONArray vals(JSONArray rows,int i){return rows.optJSONObject(i)!=null?rows.optJSONObject(i).optJSONArray("values")!=null?rows.optJSONObject(i).optJSONArray("values"):new JSONArray():new JSONArray();}
    private static JSONObject cell(JSONArray a,int i){return a.optJSONObject(i)==null?new JSONObject():a.optJSONObject(i);}
    private static String value(JSONArray a,int i){return cell(a,i).optString("formattedValue","");}
    private static boolean isPink(JSONObject v){JSONObject fmt=v.optJSONObject("effectiveFormat");if(fmt==null)return false;JSONObject color=fmt.optJSONObject("backgroundColor");if(color==null)return false;
        double r=color.optDouble("red",0),g=color.optDouble("green",0),b=color.optDouble("blue",0);
        return r>=0.74&&g>=0.40&&b>=0.40&&r-g>=0.04&&r-b>=0.04&&Math.abs(g-b)<0.30;
    }
    private static String norm(String s){return s==null?"":s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");}
    private static String parseDate(String raw){Matcher m=Pattern.compile("(\\d{1,2})[/.-](\\d{1,2})[/.-](\\d{2,4})").matcher(raw);
        if(m.find()){int d=Integer.parseInt(m.group(1)),mo=Integer.parseInt(m.group(2)),y=Integer.parseInt(m.group(3));if(y<100)y+=2000;
            if(d>=1&&d<=31&&mo>=1&&mo<=12)return String.format(Locale.US,"%04d-%02d-%02d",y,mo,d);}
        Matcher n=Pattern.compile("(\\d{1,2})\\s+(januari|februari|maret|april|mei|juni|juli|agustus|september|oktober|november|desember)\\s+(\\d{4})",Pattern.CASE_INSENSITIVE).matcher(raw);
        if(n.find()){List<String> ms=Arrays.asList("januari","februari","maret","april","mei","juni","juli","agustus","september","oktober","november","desember");return String.format(Locale.US,"%s-%02d-%02d",n.group(3),ms.indexOf(n.group(2).toLowerCase(Locale.ROOT))+1,Integer.parseInt(n.group(1)));}
        return null;
    }
    private static String enc(String s)throws Exception{return java.net.URLEncoder.encode(s,"UTF-8");}
    private static JSONObject get(String uri,String token)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(uri).openConnection();c.setRequestMethod("GET");c.setConnectTimeout(15000);c.setReadTimeout(22000);
        c.setRequestProperty("Authorization","Bearer "+token);c.setRequestProperty("Accept","application/json");
        try{int status=c.getResponseCode();InputStream stream=status<400?c.getInputStream():c.getErrorStream();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=stream.read(b))>0)out.write(b,0,n);
            String body=out.toString(StandardCharsets.UTF_8.name());if(status>=400)throw new Exception("Google Sheets API HTTP "+status+": "+body.substring(0,Math.min(180,body.length())));
            return new JSONObject(body);
        }finally{c.disconnect();}
    }
}
