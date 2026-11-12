package com.rekaciptaselera.generatorpo;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Native A4 landscape PDF: 1 supplier / page, max 4 tables; fill left column top-bottom first. */
public final class PdfExporter {
    private PdfExporter(){}
    private static final int W=842,H=595;private static final int INK=Color.rgb(20,20,20);
    public static void write(PoRules.State state,PoRules.Result result,OutputStream target)throws Exception{
        PdfDocument pdf=new PdfDocument();int pageNumber=0;
        try{
            Map<String,List<PoRules.Table>> supplierTables=new LinkedHashMap<>();
            for(PoRules.Table t:result.tables)supplierTables.computeIfAbsent(t.supplier,k->new ArrayList<>()).add(t);
            for(String supplier:PoRules.SUPPLIERS){List<PoRules.Table> ts=supplierTables.get(supplier);if(ts==null||ts.isEmpty())continue;
                for(int offset=0;offset<ts.size();offset+=4){
                    PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(W,H,++pageNumber).create());Canvas canvas=page.getCanvas();canvas.drawColor(Color.WHITE);
                    Paint p=new Paint(3);p.setColor(INK);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(12);
                    canvas.drawText("GENERATOR PO  ·  "+supplier,18,22,p);p.setTypeface(Typeface.DEFAULT);p.setTextSize(8);canvas.drawText("Reka Cipta Selera  •  A4 Landscape",W-185,20,p);
                    for(int j=0;j<4&&offset+j<ts.size();j++){
                        int column=j/2,row=j%2;float x=18+column*411,y=36+row*269;
                        drawTable(canvas,state,ts.get(offset+j),x,y,395,257);
                    }
                    p.setColor(Color.rgb(100,110,120));p.setTextSize(7);canvas.drawText("Halaman "+pageNumber+" • 1 supplier per halaman • Qty UOM utama",18,H-8,p);
                    pdf.finishPage(page);
                }
            }
            if(pageNumber==0){PdfDocument.Page p=pdf.startPage(new PdfDocument.PageInfo.Builder(W,H,1).create());Paint text=new Paint(3);text.setColor(INK);text.setTextSize(16);p.getCanvas().drawText("Belum ada Qty alokasi untuk dicetak.",50,100,text);pdf.finishPage(p);}
            pdf.writeTo(target);
        }finally{pdf.close();}
    }
    private static void drawTable(Canvas c,PoRules.State state,PoRules.Table table,float x,float y,float w,float h){
        Paint p=new Paint(3);int skuColor=color(table.sku);float no=22,name=133,qty=38,notes=w-no-name-qty;
        List<PoRules.Row> rows=table.rows;float rh=Math.min(17f,(h-44f)/Math.max(8,rows.size()+1));float font=Math.max(6f,Math.min(8.3f,rh-2));
        rect(c,p,x,y,w,24,Color.WHITE);rect(c,p,x+w-94,y,94,12,skuColor);
        rect(c,p,x,y+12,w,12,skuColor);
        p.setTextSize(8);p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setColor(INK);
        txt(c,p,"Kiriman "+formatted(table.ship),x+4,y+9,w-140);
        txt(c,p,table.supplier,x+w-155,y+9,55);
        String price="";Double pr=state.prices.get(table.supplier+"|"+table.sku);
        if(pr!=null)price="Rp"+number(pr);txt(c,p,price,x+w-91,y+9,88);
        txt(c,p,formatted(table.request),x+4,y+22,138);txt(c,p,PoRules.uom(table.sku),x+no+name+2,y+22,qty-2);txt(c,p,PoRules.noted(table.supplier,table.sku),x+no+name+qty+2,y+22,notes-5);
        line(c,p,x,y,x+w,y);line(c,p,x,y+12,x+w,y+12);line(c,p,x,y+24,x+w,y+24);
        float tableY=y+24;
        int minimum=Math.max(8,rows.size());
        for(int i=0;i<minimum;i++){
            float ry=tableY+i*rh;
            if(i<rows.size()){
                PoRules.Row row=rows.get(i);if(PoRules.NOTES[0].equals(row.note))rect(c,p,x+no+name+qty,ry,notes,rh,skuColor);
                p.setTextSize(font);p.setTypeface(Typeface.DEFAULT);p.setColor(INK);
                txt(c,p,String.valueOf(i+1),x+4,ry+rh-3,no-5);
                txt(c,p,row.outlet,x+no+3,ry+rh-3,name-5);
                txt(c,p,number(row.qty),x+no+name+3,ry+rh-3,qty-4);
                txt(c,p,row.note,x+no+name+qty+3,ry+rh-3,notes-5);
            }
            line(c,p,x,ry+rh,x+w,ry+rh);
        }
        float lastY=tableY+minimum*rh;
        p.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));p.setTextSize(Math.max(7,font));p.setColor(INK);
        txt(c,p,"Total",x+no+3,lastY+rh-3,name-5);txt(c,p,number(table.total()),x+no+name+3,lastY+rh-3,qty-4);
        line(c,p,x,lastY+rh,x+w,lastY+rh);
        float end=lastY+rh;
        for(float xx:new float[]{x,x+no,x+no+name,x+no+name+qty,x+w})line(c,p,xx,y+24,xx,end);
        line(c,p,x,y,x,y+24);line(c,p,x+w,y,x+w,y+24);
        p.setColor(Color.rgb(88,102,115));p.setTextSize(7);p.setTypeface(Typeface.DEFAULT);
        txt(c,p,table.route+("BPA".equals(table.supplier)?" · Gudang "+PoRules.warehouseForRoute(table.route):""),x+2,Math.min(y+h-2,end+11),w-10);
    }
    private static void rect(Canvas c,Paint p,float x,float y,float w,float h,int color){p.setStyle(Paint.Style.FILL);p.setColor(color);c.drawRect(x,y,x+w,y+h,p);}
    private static void line(Canvas c,Paint p,float x1,float y1,float x2,float y2){p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(0.7f);p.setColor(INK);c.drawLine(x1,y1,x2,y2,p);p.setStyle(Paint.Style.FILL);}
    private static void txt(Canvas c,Paint p,String s,float x,float y,float max){
        if(s==null)return;String v=s;
        while(v.length()>0&&p.measureText(v)>max)v=v.substring(0,v.length()-1);
        if(!v.equals(s)&&v.length()>2)v=v.substring(0,v.length()-2)+"…";
        c.drawText(v,x,y,p);
    }
    private static String formatted(String iso){try{return LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("EEEE dd MMMM yyyy",new Locale("id","ID")));}catch(Exception e){return iso;}}
    private static int color(String sku){return "adpa".equals(sku)?0xffffff00:"utuh".equals(sku)?0xfffbbc04:0xff00ffff;}
    public static String number(double value){return Math.abs(value-Math.rint(value))<0.000001?String.format(new Locale("id","ID"),"%,.0f",value):String.format(new Locale("id","ID"),"%,.2f",value);}
}
