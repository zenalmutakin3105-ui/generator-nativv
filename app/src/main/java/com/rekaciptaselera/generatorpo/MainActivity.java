package com.rekaciptaselera.generatorpo;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.text.InputType;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.Scope;
import java.io.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/** Fully native Android controls; deliberately contains no WebView, HTML or JavaScript. */
public final class MainActivity extends Activity {
    private static final int NAVY=0xff17324e,GOLD=0xfffbbc04,BG=0xfff5f7fa,WHITE=Color.WHITE,LINE=0xffd8e2ea,GRAY=0xff5e7183,RED=0xffb32629,GREEN=0xff16784e;
    private static final int RC_LOGIN=91,RC_BACKUP=92,RC_RESTORE=93,RC_PDF=94;
    private StateStore storage;private PoRules.State state;private PoRules.Result lastResult;
    private LinearLayout page,header,nav;private int active=0;private String requestDate=StateStore.today(),shipDate=StateStore.today();
    private List<SheetsReader.Group> dates=new ArrayList<>();private SheetsReader.Import preview;
    private final ExecutorService io=Executors.newSingleThreadExecutor();private Consumer<String> waitingToken;
    private String quickSku="adpa",quickSupplier="auto",quickRoute="All Rute";
    private Map<String,EditText> quickInputs=new LinkedHashMap<>();

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(NAVY);getWindow().setNavigationBarColor(NAVY);
        storage=new StateStore(this);state=storage.load();lastResult=PoRules.calculate(state);render(0);}
    @Override protected void onDestroy(){io.shutdownNow();super.onDestroy();}
    private int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int fill,int radius,int border){GradientDrawable d=new GradientDrawable();d.setColor(fill);d.setCornerRadius(dp(radius));if(border!=0)d.setStroke(dp(1),border);return d;}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
    private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
    private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    private void space(LinearLayout l,int h){View v=new View(this);l.addView(v,new LinearLayout.LayoutParams(1,dp(h)));}
    private void addText(LinearLayout c,String value,int size,int color,boolean bold){TextView t=text(value,size,color,bold);c.addView(t);}
    private void addLine(LinearLayout parent){View v=new View(this);v.setBackgroundColor(LINE);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(1));p.topMargin=dp(6);p.bottomMargin=dp(6);parent.addView(v,p);}
    private LinearLayout card(LinearLayout parent){LinearLayout v=column();v.setPadding(dp(14),dp(14),dp(14),dp(14));v.setBackground(bg(WHITE,14,LINE));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(dp(10),dp(6),dp(10),dp(6));parent.addView(v,lp);return v;}
    private TextView button(String value,Runnable action,boolean primary){TextView b=text(value,13,primary?WHITE:NAVY,true);b.setGravity(Gravity.CENTER);b.setPadding(dp(13),dp(12),dp(13),dp(12));b.setBackground(bg(primary?NAVY:0xffecf2f7,10,primary?0:LINE));b.setOnClickListener(v->action.run());return b;}
    private void action(LinearLayout c,String title,Runnable r){TextView b=button(title,r,true);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(7);c.addView(b,lp);}
    private void secondary(LinearLayout c,String title,Runnable r){TextView b=button(title,r,false);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(7);c.addView(b,lp);}
    private EditText field(String value,String hint,boolean number){EditText e=new EditText(this);e.setSingleLine(true);e.setTextSize(14);e.setPadding(dp(10),dp(7),dp(10),dp(7));e.setBackground(bg(WHITE,9,LINE));e.setHint(hint);if(number)e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);e.setText(value);return e;}
    private void fieldFull(LinearLayout parent,EditText e){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(45));p.topMargin=dp(5);parent.addView(e,p);}
    private Spinner spinner(String[] values,int selected){Spinner s=new Spinner(this);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,values);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);s.setAdapter(a);s.setSelection(Math.max(0,Math.min(selected,values.length-1)));s.setBackground(bg(0xfff4f7fa,8,LINE));s.setPadding(dp(8),dp(3),dp(6),dp(3));return s;}
    private String pick(Spinner s){return String.valueOf(s.getSelectedItem());}
    private int index(String[] array,String val){for(int i=0;i<array.length;i++)if(array[i].equals(val))return i;return 0;}
    private String[] names(String[] sku){String[] x=new String[sku.length];for(int i=0;i<sku.length;i++)x[i]=PoRules.skuName(sku[i])+" ("+PoRules.uom(sku[i])+")";return x;}
    private LinearLayout expand(LinearLayout parent,String title,boolean initiallyOpen){LinearLayout section=card(parent);TextView head=text((initiallyOpen?"▼  ":"▶  ")+title,14,NAVY,true);section.addView(head);
        LinearLayout body=column();body.setVisibility(initiallyOpen?View.VISIBLE:View.GONE);section.addView(body);head.setOnClickListener(v->{boolean open=body.getVisibility()!=View.VISIBLE;body.setVisibility(open?View.VISIBLE:View.GONE);head.setText((open?"▼  ":"▶  ")+title);});return body;}
    private void title(LinearLayout parent,String name){TextView t=text(name,18,NAVY,true);t.setPadding(dp(14),dp(10),dp(12),dp(4));parent.addView(t);}
    private void label(LinearLayout c,String text){TextView t=text(text,12,GRAY,true);t.setPadding(0,dp(8),0,0);c.addView(t);}
    private void notifyUser(String m){Toast.makeText(this,m,Toast.LENGTH_LONG).show();}
    private void persist(){try{storage.save(state);lastResult=PoRules.calculate(state);notifyUser("Data tersimpan");}catch(Exception e){err(e);} }
    private void err(Exception e){new AlertDialog.Builder(this).setTitle("Generator PO").setMessage(e.getMessage()==null?e.toString():e.getMessage()).setPositiveButton("OK",null).show();}
    private void confirm(String message,Runnable yes){new AlertDialog.Builder(this).setTitle("Konfirmasi").setMessage(message).setNegativeButton("Batal",null).setPositiveButton("Lanjut",(d,w)->yes.run()).show();}
    private static double number(String s){try{return Double.parseDouble(s.trim().replace(",","."));}catch(Exception e){return -1;}}
    private static boolean whole(double n){return Double.isFinite(n)&&n>=0&&Math.rint(n)==n;}

    private void render(int current){active=current;LinearLayout root=column();root.setBackgroundColor(BG);setContentView(root);
        if(android.os.Build.VERSION.SDK_INT>=35){root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets sys=insets.getInsets(android.view.WindowInsets.Type.systemBars());v.setPadding(0,sys.top,0,sys.bottom);return insets;});}
        header=column();header.setPadding(dp(18),dp(15),dp(12),dp(12));header.setBackgroundColor(NAVY);
        addText(header,"GENERATOR PO",21,WHITE,true);addText(header,"Android Native Beta 1.0  ·  SCM Reka Cipta Selera",11,0xffc0cedb,false);root.addView(header);
        HorizontalScrollView tabs=new HorizontalScrollView(this);tabs.setHorizontalScrollBarEnabled(false);tabs.setBackgroundColor(WHITE);nav=row();nav.setPadding(dp(7),dp(7),dp(7),dp(7));tabs.addView(nav);root.addView(tabs);
        String[] labels={"Dashboard","Stok","Kebutuhan","Master","Hasil"};
        for(int i=0;i<labels.length;i++){final int chosen=i;TextView b=button(labels[i],()->render(chosen),i==current);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.setMargins(dp(3),0,dp(3),0);nav.addView(b,p);}
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));page=column();page.setPadding(0,dp(4),0,dp(30));scroll.addView(page);
        switch(current){case 0:dashboard();break;case 1:stockScreen();break;case 2:demandScreen();break;case 3:masterScreen();break;case 4:resultScreen();break;}
    }
    private void metric(LinearLayout l,String name,String value){LinearLayout r=row();TextView a=text(name,13,GRAY,false);r.addView(a,new LinearLayout.LayoutParams(0,-2,1));TextView b=text(value,15,NAVY,true);r.addView(b);l.addView(r);space(l,8);}
    private void dashboard(){title(page,"Dashboard Generator PO");LinearLayout box=card(page);
        metric(box,"Supplier tersedia",String.valueOf(PoRules.SUPPLIERS.length));metric(box,"Outlet master",String.valueOf(PoRules.OUTLETS.length));
        metric(box,"Stok tersimpan",String.valueOf(state.stocks.size()));metric(box,"Kebutuhan tercatat",String.valueOf(state.demands.size()));metric(box,"Tabel alokasi",String.valueOf(lastResult.tables.size()));
        action(box,"Hitung Ulang Alokasi",()->{lastResult=PoRules.calculate(state);render(4);});
        LinearLayout cloud=card(page);addText(cloud,"SINKRONISASI GOOGLE SHEETS",14,NAVY,true);
        addText(cloud,"Sumber: Resume Ayam • Hanya blok pink RO–PPIC berdasarkan tanggal kirim.",12,GRAY,false);
        secondary(cloud,"1. Ambil Daftar Tanggal (Login Google)",this::requestDates);
        if(!dates.isEmpty()){
            secondary(cloud,"2. Pilih Tanggal Kirim",this::chooseDate);
            if(preview!=null){addText(cloud,"Dipilih: "+preview.group.date+" • 29 outlet × 3 SKU",12,GREEN,true);secondary(cloud,"3. Lihat Preview & Konfirmasi",this::confirmImport);}
        }
        secondary(cloud,"Pulihkan Data Sebelum Sinkronisasi",this::rollback);
        LinearLayout files=card(page);addText(files,"BACKUP & DOKUMEN",14,NAVY,true);
        secondary(files,"Unduh Backup JSON",()->createFile(RC_BACKUP,"application/json","PO_Ayam_"+requestDate+".json"));
        secondary(files,"Muat Backup JSON",()->openFile(RC_RESTORE,"application/json"));
        secondary(files,"Cetak / Simpan PDF PO",()->createFile(RC_PDF,"application/pdf","Generator_PO_"+requestDate+".pdf"));
        TextView reset=button("Bersihkan Data Input (Master tetap)",()->confirm("Semua stok dan kebutuhan akan dihapus. Harga, rute, pembatasan, dan catatan master tetap disimpan.",()->{state.stocks.clear();state.demands.clear();requestDate=StateStore.today();shipDate=requestDate;persist();render(0);}),false);
        files.addView(reset);addText(files,"Data disimpan lokal di perangkat. Backup JSON sebelum pembaruan aplikasi.",11,GRAY,false);
        secondary(files,"Putuskan Izin Google Sheets",()->{
            Identity.getAuthorizationClient(this).revokeAccess(com.google.android.gms.auth.api.identity.RevokeAccessRequest.builder().setScopes(Collections.singletonList(new Scope("https://www.googleapis.com/auth/spreadsheets.readonly"))).build())
                .addOnSuccessListener(x->notifyUser("Akses Google Sheets dicabut")).addOnFailureListener(e->notifyUser("Gagal mencabut akses: "+e.getMessage()));
        });
    }

    private void stockScreen(){title(page,"Stok Supplier (Pack/Ekor)");LinearLayout form=card(page);
        label(form,"Supplier");Spinner supplier=spinner(PoRules.SUPPLIERS,0);fieldFullSpinner(form,supplier);
        label(form,"SKU");Spinner sku=spinner(names(PoRules.SKU),0);fieldFullSpinner(form,sku);
        label(form,"Gudang BPA (hanya berlaku untuk BPA)");Spinner wh=spinner(new String[]{"Bandung","Jakarta"},0);fieldFullSpinner(form,wh);
        label(form,"Qty Stok");EditText qty=field("","Masukkan Qty stok Pack/Ekor",true);fieldFull(form,qty);
        action(form,"+ Tambah Stok",()->{double n=number(qty.getText().toString());if(!whole(n)||n<=0){notifyUser("Qty stok harus bilangan bulat positif");return;}String s=pick(supplier),sk=PoRules.SKU[sku.getSelectedItemPosition()],w="BPA".equals(s)?pick(wh):"";
            PoRules.Stock existing=null;for(PoRules.Stock x:state.stocks)if(x.supplier.equals(s)&&x.sku.equals(sk)&&x.warehouse.equals(w)){existing=x;break;}
            if(existing!=null)existing.qty=n;else state.stocks.add(new PoRules.Stock(s,sk,w,n));persist();render(1);
        });
        LinearLayout list=expand(page,"Daftar Stok Supplier ("+state.stocks.size()+" data)",false);
        for(PoRules.Stock s:new ArrayList<>(state.stocks)){
            addLine(list);LinearLayout r=row();LinearLayout meta=column();addText(meta,s.supplier+" — "+PoRules.skuName(s.sku),13,NAVY,true);
            addText(meta,("BPA".equals(s.supplier)?"Gudang "+s.warehouse+" · ":"")+PoRules.uom(s.sku)+" · "+PdfExporter.number(s.qty),11,GRAY,false);
            r.addView(meta,new LinearLayout.LayoutParams(0,-2,1));TextView b=button("Edit",()->editStock(s),false);r.addView(b);list.addView(r);
        }
    }
    private void fieldFullSpinner(LinearLayout p,Spinner s){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(44));lp.topMargin=dp(5);p.addView(s,lp);}
    private void editStock(PoRules.Stock stock){LinearLayout c=column();c.setPadding(dp(18),0,dp(18),0);
        EditText qty=field(PdfExporter.number(stock.qty).replace(".",""),"Qty",true);fieldFull(c,qty);
        Spinner wh=spinner(new String[]{"Bandung","Jakarta"},"Jakarta".equals(stock.warehouse)?1:0);
        if("BPA".equals(stock.supplier)){label(c,"Gudang");fieldFullSpinner(c,wh);}
        new AlertDialog.Builder(this).setTitle(stock.supplier+" · "+PoRules.skuName(stock.sku)).setView(c)
            .setNegativeButton("Batal",null).setNeutralButton("Hapus",(d,w)->{state.stocks.remove(stock);persist();render(1);})
            .setPositiveButton("Simpan",(d,w)->{double v=number(qty.getText().toString());if(!whole(v)){notifyUser("Qty tidak valid");return;}
                stock.qty=v;if("BPA".equals(stock.supplier))stock.warehouse=pick(wh);persist();render(1);}).show();
    }

    private void demandScreen(){title(page,"Input Cepat Kebutuhan PO");LinearLayout form=card(page);
        addText(form,"Dada dan Fillet: input PCS → Pack (÷10). Ayam Utuh: Ekor.",12,GRAY,false);
        label(form,"SKU");Spinner sku=spinner(new String[]{"Ayam Dada (PCS)","Ayam Utuh (Ekor)","Ayam Fillet (PCS)"},index(PoRules.SKU,quickSku));fieldFullSpinner(form,sku);
        label(form,"Supplier pilihan");String[] suppliers=new String[PoRules.SUPPLIERS.length+1];suppliers[0]="Otomatis (stok tersedia)";System.arraycopy(PoRules.SUPPLIERS,0,suppliers,1,PoRules.SUPPLIERS.length);
        Spinner supplier=spinner(suppliers,"auto".equals(quickSupplier)?0:1+index(PoRules.SUPPLIERS,quickSupplier));fieldFullSpinner(form,supplier);
        label(form,"Rute");Spinner route=spinner(new String[]{"All Rute","Rute Bandung","Rute Jakarta"},index(new String[]{"All Rute","Rute Bandung","Rute Jakarta"},quickRoute));fieldFullSpinner(form,route);
        label(form,"Tanggal Request");dateControl(form,true);label(form,"Tanggal Kirim");dateControl(form,false);
        Spinner[] selections={sku,supplier,route};
        action(form,"Tampilkan Daftar Outlet",()->{quickSku=PoRules.SKU[sku.getSelectedItemPosition()];quickSupplier=supplier.getSelectedItemPosition()==0?"auto":PoRules.SUPPLIERS[supplier.getSelectedItemPosition()-1];quickRoute=pick(route);render(2);});
        label(form,"Paste satu kolom Qty (29 outlet, satu angka per baris)");EditText pasted=field("","Contoh: 500 lalu enter 400 lalu enter 0 ...",false);pasted.setSingleLine(false);pasted.setMinLines(3);pasted.setGravity(Gravity.TOP);pasted.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        form.addView(pasted,new LinearLayout.LayoutParams(-1,dp(100)));
        secondary(form,"Terapkan Qty dari Clipboard / Paste",()->{String raw=pasted.getText().toString().replace("\r\n","\n").replace('\r','\n').replace('\t','\n');
            if(raw.endsWith("\n"))raw=raw.substring(0,raw.length()-1);
            String[] lines=raw.split("\n",-1);
            List<String> visible=visibleOutlets();if(lines.length!=visible.size()){notifyUser("Jumlah angka "+lines.length+" tidak sama dengan "+visible.size()+" outlet yang tampil.");return;}
            for(int i=0;i<lines.length;i++){double n=number(lines[i]);if(!whole(n)){notifyUser("Qty tidak valid pada baris "+(i+1));return;}}
            for(int i=0;i<lines.length;i++)quickInputs.get(visible.get(i)).setText(lines[i].trim());notifyUser("Qty terisi sesuai urutan outlet");
        });
        action(form,"+ Simpan Semua Qty Terisi",()->{
            String selectedSku=PoRules.SKU[sku.getSelectedItemPosition()];
            String selectedSupplier=supplier.getSelectedItemPosition()==0?"auto":PoRules.SUPPLIERS[supplier.getSelectedItemPosition()-1];
            if(!selectedSku.equals(quickSku)||!selectedSupplier.equals(quickSupplier)||!pick(route).equals(quickRoute)){
                notifyUser("Klik Tampilkan Daftar Outlet setelah mengubah SKU, supplier atau rute.");return;
            }
            saveQuick();
        });
        secondary(form,"Kosongkan Qty Cepat",()->{for(EditText e:quickInputs.values())e.setText("");pasted.setText("");});
        addText(form,"Qty 0 tetap disimpan tetapi tidak ditampilkan pada tabel PO.",11,GRAY,false);
        LinearLayout list=expand(page,"Daftar Outlet — Input Qty ("+visibleOutlets().size()+" outlet)",false);quickInputs.clear();
        int index=0;for(String outlet:visibleOutlets()){
            LinearLayout r=row();TextView num=text(String.valueOf(++index)+". ",11,GRAY,false);r.addView(num);
            TextView name=text(outlet,12,NAVY,false);r.addView(name,new LinearLayout.LayoutParams(0,-2,1));EditText q=field("","—",true);
            LinearLayout.LayoutParams size=new LinearLayout.LayoutParams(dp(95),dp(40));r.addView(q,size);quickInputs.put(outlet,q);
            list.addView(r);space(list,5);
        }
        for(String sk:PoRules.SKU){List<PoRules.Demand> selected=new ArrayList<>();for(PoRules.Demand d:state.demands)if(sk.equals(d.sku))selected.add(d);
            LinearLayout history=expand(page,PoRules.skuName(sk)+" — "+selected.size()+" kebutuhan",false);
            for(PoRules.Demand d:selected){addLine(history);LinearLayout r=row();LinearLayout info=column();addText(info,d.outlet+"  •  "+PdfExporter.number(d.qty)+" "+("utuh".equals(sk)?"Ekor":"PCS"),12,NAVY,true);
                addText(info,d.ship+"  •  "+d.preferred,10,GRAY,false);r.addView(info,new LinearLayout.LayoutParams(0,-2,1));r.addView(button("Edit",()->editDemand(d),false));history.addView(r);}
        }
    }
    private void dateControl(LinearLayout parent,boolean request){String date=request?requestDate:shipDate;TextView b=button(date+"  ▾",()->{
        String iso=request?requestDate:shipDate;LocalDate dt;try{dt=LocalDate.parse(iso);}catch(Exception e){dt=LocalDate.now();}
        new DatePickerDialog(this,(v,y,m,d)->{String chosen=String.format(Locale.US,"%04d-%02d-%02d",y,m+1,d);
            if(request)requestDate=chosen;else shipDate=chosen;render(2);},dt.getYear(),dt.getMonthValue()-1,dt.getDayOfMonth()).show();},false);
        parent.addView(b,new LinearLayout.LayoutParams(-1,dp(43)));
    }
    private List<String> visibleOutlets(){List<String> list=new ArrayList<>();for(String outlet:PoRules.OUTLETS)if(quickRoute.equals("All Rute")||quickRoute.equals(PoRules.route(state,outlet)))list.add(outlet);return list;}
    private void saveQuick(){int count=0;String supplier=quickSupplier;
        if(shipDate.compareTo(requestDate)<0){notifyUser("Tanggal kirim tidak boleh sebelum tanggal request");return;}
        for(Map.Entry<String,EditText> e:quickInputs.entrySet()){String str=e.getValue().getText().toString().trim();if(str.isEmpty())continue;double q=number(str);if(!whole(q)){notifyUser("Qty tidak valid untuk "+e.getKey());return;}count++;}
        if(count==0){notifyUser("Isi minimal 1 Qty");return;}
        for(Map.Entry<String,EditText> e:quickInputs.entrySet()){String str=e.getValue().getText().toString().trim();if(str.isEmpty())continue;double qty=number(str);
            PoRules.Demand same=null;for(PoRules.Demand d:state.demands)if(d.outlet.equals(e.getKey())&&d.sku.equals(quickSku)&&d.preferred.equals(supplier)&&d.request.equals(requestDate)&&d.ship.equals(shipDate)){same=d;break;}
            if(same!=null)same.qty=qty;else state.demands.add(new PoRules.Demand(e.getKey(),quickSku,supplier,requestDate,shipDate,qty));
        }
        persist();render(2);notifyUser(count+" data kebutuhan tersimpan");
    }
    private void editDemand(PoRules.Demand d){EditText e=field(String.valueOf((long)d.qty),"Qty",true);
        new AlertDialog.Builder(this).setTitle(d.outlet+" · "+PoRules.skuName(d.sku)).setView(e)
            .setNegativeButton("Batal",null).setNeutralButton("Hapus",(x,w)->{state.demands.remove(d);persist();render(2);})
            .setPositiveButton("Simpan",(x,w)->{double n=number(e.getText().toString());if(!whole(n)){notifyUser("Qty tidak valid");return;}d.qty=n;persist();render(2);}).show();
    }

    private void masterScreen(){title(page,"Informasi Master");LinearLayout price=expand(page,"Update Harga SKU per Supplier",false);
        label(price,"Supplier");Spinner supplier=spinner(PoRules.SUPPLIERS,0);fieldFullSpinner(price,supplier);
        EditText[] inputs=new EditText[3];LinearLayout inputArea=column();price.addView(inputArea);
        Runnable refreshPrices=()->{inputArea.removeAllViews();for(int j=0;j<3;j++){String k=PoRules.SKU[j],s=pick(supplier);label(inputArea,PoRules.skuName(k)+" / "+PoRules.uom(k));Double v=state.prices.get(s+"|"+k);inputs[j]=field(v==null?"":String.valueOf(v.longValue()),"Harga Rp",true);fieldFull(inputArea,inputs[j]);}};
        supplier.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){refreshPrices.run();}public void onNothingSelected(AdapterView<?> p){}});
        refreshPrices.run();action(price,"Simpan Harga Supplier",()->{for(int j=0;j<3;j++){String raw=inputs[j].getText().toString().trim(),key=pick(supplier)+"|"+PoRules.SKU[j];
                if(raw.isEmpty()){state.prices.remove(key);continue;}double n=number(raw);if(n<0||!Double.isFinite(n)){notifyUser("Harga harus 0 atau lebih");return;}state.prices.put(key,n);
            }persist();});
        LinearLayout restrict=expand(page,"Aturan Supplier — Tidak Bisa Kirim ke Outlet",false);
        Switch toggle=new Switch(this);toggle.setText("Aktifkan pembatasan supplier");toggle.setChecked(state.restrictionsEnabled);restrict.addView(toggle);toggle.setOnCheckedChangeListener((v,c)->{state.restrictionsEnabled=c;persist();});
        label(restrict,"Pilih 1 Supplier");Spinner rs=spinner(PoRules.SUPPLIERS,0);fieldFullSpinner(restrict,rs);
        LinearLayout checkArea=column();restrict.addView(checkArea);Map<String,CheckBox> checks=new LinkedHashMap<>();
        Runnable updateChecks=()->{checkArea.removeAllViews();checks.clear();List<String> forbidden=state.restrictions.getOrDefault(pick(rs),Collections.emptyList());
            for(String outlet:PoRules.OUTLETS){CheckBox cb=new CheckBox(this);cb.setText(outlet);cb.setTextSize(12);cb.setChecked(forbidden.contains(outlet));checkArea.addView(cb);checks.put(outlet,cb);}};
        rs.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){updateChecks.run();}public void onNothingSelected(AdapterView<?> p){}});
        updateChecks.run();action(restrict,"Simpan Pembatasan",()->{List<String> forbidden=new ArrayList<>();for(Map.Entry<String,CheckBox> e:checks.entrySet())if(e.getValue().isChecked())forbidden.add(e.getKey());state.restrictions.put(pick(rs),forbidden);persist();});
        LinearLayout notes=expand(page,"Master Pengecualian Catatan Default per Outlet",false);
        label(notes,"Outlet");Spinner no=spinner(PoRules.OUTLETS,0);fieldFullSpinner(notes,no);label(notes,"Catatan Khusus");Spinner note=spinner(PoRules.NOTES,0);fieldFullSpinner(notes,note);
        action(notes,"Simpan Catatan Khusus",()->{state.noteExceptions.put(pick(no),pick(note));persist();render(3);});
        for(Map.Entry<String,String> en:new LinkedHashMap<>(state.noteExceptions).entrySet()){
            LinearLayout r=row();TextView name=text(en.getKey()+" : "+en.getValue(),11,GRAY,false);r.addView(name,new LinearLayout.LayoutParams(0,-2,1));r.addView(button("Hapus",()->{state.noteExceptions.remove(en.getKey());persist();render(3);},false));notes.addView(r);
        }
        LinearLayout routes=expand(page,"Master Outlet & Penetapan Rute (29 outlet)",false);
        for(String outlet:PoRules.OUTLETS){LinearLayout r=row();TextView name=text(outlet,12,NAVY,false);r.addView(name,new LinearLayout.LayoutParams(0,-2,1));Spinner sp=spinner(PoRules.ROUTES,PoRules.ROUTES[1].equals(PoRules.route(state,outlet))?1:0);r.addView(sp,new LinearLayout.LayoutParams(dp(132),dp(40)));
            sp.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int i,long id){String newRoute=PoRules.ROUTES[i];if(!newRoute.equals(state.routes.get(outlet))){state.routes.put(outlet,newRoute);persist();}}public void onNothingSelected(AdapterView<?> p){}});
            routes.addView(r);space(routes,4);
        }
        LinearLayout other=card(page);Switch fallback=new Switch(this);fallback.setText("Supplier pilihan boleh fallback ke supplier lain");fallback.setChecked(state.allowFallback);other.addView(fallback);fallback.setOnCheckedChangeListener((v,b)->{state.allowFallback=b;persist();});
        addText(other,"Default OFF. Pengecualian supplier yang diblokir tetap mencari alternatif yang diizinkan.",11,GRAY,false);
    }

    private void resultScreen(){title(page,"Hasil Alokasi PO");lastResult=PoRules.calculate(state);
        LinearLayout summary=card(page);addText(summary,"Ringkasan Hasil Alokasi PO",15,NAVY,true);metric(summary,"Jumlah tabel PO",String.valueOf(lastResult.tables.size()));
        for(String sk:PoRules.SKU){double req=0,allocated=0,pending=0;for(PoRules.Demand d:state.demands)if(d.sku.equals(sk))req+=PoRules.toPoUom(d.qty,sk);
            for(PoRules.Table t:lastResult.tables)if(t.sku.equals(sk))allocated+=t.total();for(PoRules.Pending p:lastResult.pending)if(p.demand.sku.equals(sk))pending+=p.qty;
            LinearLayout r=column();addText(r,PoRules.skuName(sk)+" ("+PoRules.uom(sk)+")",12,NAVY,true);
            addText(r,"Diminta "+PdfExporter.number(req)+"  •  Dialokasi "+PdfExporter.number(allocated)+"  •  Pending "+PdfExporter.number(pending),12,GRAY,false);summary.addView(r);space(summary,10);
        }
        secondary(summary,"Simpan / Cetak PDF",()->createFile(RC_PDF,"application/pdf","Generator_PO_"+requestDate+".pdf"));
        secondary(summary,"Salin Hasil Tabel PO (teks)",this::copyResultText);
        LinearLayout rounding=expand(page,"Rincian Selisih Pembulatan ±  ("+lastResult.rounding.size()+" outlet)",false);
        if(lastResult.rounding.isEmpty())addText(rounding,"Tidak ada selisih pembulatan",12,GREEN,false);
        else for(PoRules.Rounding r:lastResult.rounding){addLine(rounding);addText(rounding,PoRules.skuName(r.demand.sku)+" · "+r.demand.outlet+" · "+r.supplier,12,NAVY,true);
            addText(rounding,"Diminta "+PdfExporter.number(r.requested)+" → Dialokasi "+PdfExporter.number(r.allocated)+" | Pending "+PdfExporter.number(r.pending)+" | "+(r.difference>0?"+":"")+PdfExporter.number(r.difference),12,r.difference>0?GREEN:RED,true);
        }
        LinearLayout finance=expand(page,"Rangkuman Qty & Nilai PO per Supplier dan SKU",false);
        Map<String,Double> qtyBySku=new LinkedHashMap<>();for(PoRules.Table t:lastResult.tables){String key=t.supplier+"|"+t.sku;qtyBySku.put(key,qtyBySku.getOrDefault(key,0d)+t.total());}
        double totalKnown=0;boolean unknown=false;
        for(String supplier:PoRules.SUPPLIERS){double subtotal=0;boolean has=false;for(String sku:PoRules.SKU){String key=supplier+"|"+sku;Double q=qtyBySku.get(key);if(q==null)continue;has=true;Double price=state.prices.get(key);
                addText(finance,supplier+" · "+PoRules.skuName(sku),12,NAVY,true);
                addText(finance,PdfExporter.number(q)+" "+PoRules.uom(sku)+" × "+(price==null?"Harga belum diisi":("Rp"+PdfExporter.number(price)))+(price==null?"":" = Rp"+PdfExporter.number(q*price)),11,GRAY,false);
                if(price==null)unknown=true;else{subtotal+=q*price;totalKnown+=q*price;}
            }if(has){addText(finance,"Subtotal diketahui " +supplier+": Rp"+PdfExporter.number(subtotal),12,GREEN,true);addLine(finance);}}
        addText(finance,"Total nilai PO "+(unknown?"(parsial): ":": ")+"Rp"+PdfExporter.number(totalKnown),14,NAVY,true);
        if(!lastResult.pending.isEmpty()){LinearLayout pend=expand(page,"Pending Kebutuhan ("+lastResult.pending.size()+")",false);
            for(PoRules.Pending p:lastResult.pending)addText(pend,p.demand.outlet+" · "+PoRules.skuName(p.demand.sku)+" : "+PdfExporter.number(p.qty)+" "+PoRules.uom(p.demand.sku),12,RED,false);
        }
        if(lastResult.tables.isEmpty()){LinearLayout empty=card(page);addText(empty,"Belum ada tabel PO. Tambah stok dan kebutuhan terlebih dahulu.",13,GRAY,false);return;}
        String supplier="";for(PoRules.Table t:lastResult.tables){if(!supplier.equals(t.supplier)){supplier=t.supplier;title(page,"Supplier " +supplier);}
            drawNativeTable(t);
        }
    }
    private void copyResultText(){
        PoRules.Result res=PoRules.calculate(state);
        if(res.tables.isEmpty()){notifyUser("Belum ada tabel hasil PO");return;}
        StringBuilder out=new StringBuilder("GENERATOR PO\n\n");
        for(PoRules.Table t:res.tables){
            out.append("Kiriman ").append(t.ship).append(" | ").append(t.supplier)
               .append(" | ").append(PoRules.skuName(t.sku)).append(" | ").append(t.route).append("\n")
               .append("Request: ").append(t.request).append(" | ").append(PoRules.noted(t.supplier,t.sku)).append("\n");
            int no=0;for(PoRules.Row r:t.rows)out.append(++no).append(". ").append(r.outlet).append(" : ")
                .append(PdfExporter.number(r.qty)).append(" ").append(PoRules.uom(t.sku)).append(" | ").append(r.note).append("\n");
            out.append("TOTAL: ").append(PdfExporter.number(t.total())).append(" ").append(PoRules.uom(t.sku)).append("\n\n");
        }
        android.content.ClipboardManager clipboard=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Generator PO",out.toString()));
        notifyUser("Hasil tabel PO disalin ke clipboard Android");
    }

    private void drawNativeTable(PoRules.Table t){LinearLayout content=card(page);int color="adpa".equals(t.sku)?0xffffff00:"utuh".equals(t.sku)?0xfffbbc04:0xff00ffff;
        LinearLayout head=row();head.setPadding(dp(8),dp(7),dp(8),dp(7));head.setBackgroundColor(color);
        TextView left=text("Kiriman "+t.ship+" · "+t.supplier,12,Color.BLACK,true);head.addView(left,new LinearLayout.LayoutParams(0,-2,1));
        Double pr=state.prices.get(t.supplier+"|"+t.sku);head.addView(text(pr==null?"Harga belum diisi":"Rp"+PdfExporter.number(pr),11,Color.BLACK,true));content.addView(head);
        TextView sub=text(t.request+" • "+PoRules.uom(t.sku)+" • "+PoRules.noted(t.supplier,t.sku)+" • "+t.route,11,Color.BLACK,true);
        sub.setPadding(dp(8),dp(7),dp(8),dp(7));sub.setBackgroundColor(color);content.addView(sub);
        int i=0;for(PoRules.Row r:t.rows){LinearLayout line=row();line.setPadding(dp(4),dp(7),dp(4),dp(7));line.addView(text(String.valueOf(++i),11,GRAY,false),new LinearLayout.LayoutParams(dp(25),-2));
            line.addView(text(r.outlet,12,NAVY,false),new LinearLayout.LayoutParams(0,-2,1));line.addView(text(PdfExporter.number(r.qty),12,NAVY,true),new LinearLayout.LayoutParams(dp(65),-2));
            TextView note=text(r.note,10,Color.BLACK,false);note.setPadding(dp(4),dp(4),dp(4),dp(4));note.setBackgroundColor(PoRules.NOTES[0].equals(r.note)?color:WHITE);
            line.addView(note,new LinearLayout.LayoutParams(dp(135),-2));content.addView(line);addLine(content);
        }
        addText(content,"Total : "+PdfExporter.number(t.total())+" "+PoRules.uom(t.sku),13,NAVY,true);
    }

    private void authorize(Consumer<String> after){waitingToken=after;
        AuthorizationRequest req=AuthorizationRequest.builder().setRequestedScopes(Collections.singletonList(new Scope("https://www.googleapis.com/auth/spreadsheets.readonly"))).build();
        Identity.getAuthorizationClient(this).authorize(req).addOnSuccessListener(result->{
            if(result.hasResolution()){try{startIntentSenderForResult(result.getPendingIntent().getIntentSender(),RC_LOGIN,null,0,0,0);}catch(Exception e){err(e);}}
            else if(result.getAccessToken()!=null){Consumer<String> f=waitingToken;waitingToken=null;f.accept(result.getAccessToken());}
            else notifyUser("Token Google tidak tersedia.");
        }).addOnFailureListener(e->err(new Exception("Google login gagal. Periksa Google Play Services dan OAuth Android client: "+e.getMessage())));
    }
    private void backgroundTask(String status,CallableAction action){ProgressDialog dialog=ProgressDialog.show(this,"Generator PO",status,true,false);
        io.execute(()->{try{Object result=action.run();runOnUiThread(()->{dialog.dismiss();onTaskCompleted(result);});}catch(Exception e){runOnUiThread(()->{dialog.dismiss();err(e);});}});
    }
    private interface CallableAction {Object run()throws Exception;}
    private void onTaskCompleted(Object result){
        if(result instanceof List){@SuppressWarnings("unchecked") List<SheetsReader.Group> found=(List<SheetsReader.Group>)result;dates=found;preview=null;render(0);chooseDate();}
        else if(result instanceof SheetsReader.Import){preview=(SheetsReader.Import)result;render(0);confirmImport();}
    }
    private void requestDates(){authorize(token->backgroundTask("Mengambil tanggal kirim RO–PPIC pink...",()->SheetsReader.fetchDates(token)));}
    private void chooseDate(){if(dates.isEmpty()){notifyUser("Ambil daftar tanggal terlebih dahulu.");return;}
        String[] options=new String[dates.size()];for(int i=0;i<dates.size();i++)options[i]=dates.get(i).toString();
        new AlertDialog.Builder(this).setTitle("Pilih Tanggal Kirim").setItems(options,(d,n)->{
            SheetsReader.Group group=dates.get(n);authorize(token->backgroundTask("Memeriksa 29 outlet dan 3 SKU...",()->SheetsReader.fetchRows(token,group)));
        }).setNegativeButton("Batal",null).show();
    }
    private void confirmImport(){if(preview==null)return;SheetsReader.Import data=preview;StringBuilder sb=new StringBuilder("Tanggal kirim: "+data.group.date+"\n29 outlet × 3 SKU = 87 kebutuhan.\n\n");
        for(int i=0;i<Math.min(5,data.rows.size());i++){SheetsReader.QtyRow q=data.rows.get(i);sb.append(q.outlet).append(" : ").append((long)q.dada).append(" / ").append((long)q.utuh).append(" / ").append((long)q.fillet).append("\n");}
        sb.append("\nData kebutuhan untuk tanggal kirim ini akan diganti. Snapshot rollback dibuat terlebih dahulu. Stok dan master tidak berubah.");
        new AlertDialog.Builder(this).setTitle("Preview Qty RO–PPIC Pink").setMessage(sb.toString()).setNegativeButton("Batal",null)
            .setPositiveButton("Impor 87 Qty",(d,w)->{
                try{
                    storage.snapshot(state);
                    state.demands.removeIf(x->x.ship.equals(data.group.date));
                    for(SheetsReader.QtyRow q:data.rows){state.demands.add(new PoRules.Demand(q.outlet,"adpa","auto",requestDate,data.group.date,q.dada));
                        state.demands.add(new PoRules.Demand(q.outlet,"utuh","auto",requestDate,data.group.date,q.utuh));
                        state.demands.add(new PoRules.Demand(q.outlet,"fillet","auto",requestDate,data.group.date,q.fillet));}
                    shipDate=data.group.date;persist();render(2);notifyUser("87 Qty berhasil diimpor, rollback tersimpan.");
                }catch(Exception e){err(e);}
            }).show();
    }
    private void rollback(){if(!storage.hasRollback()){notifyUser("Belum ada snapshot sebelum sinkronisasi");return;}
        confirm("Pulihkan seluruh kondisi data sebelum impor terakhir (stok, kebutuhan, dan master pada saat itu)?",()->{
            try{state=storage.rollback();lastResult=PoRules.calculate(state);render(0);notifyUser("Snapshot berhasil dipulihkan");}catch(Exception e){err(e);}
        });
    }
    private void createFile(int request,String type,String name){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(type);i.putExtra(Intent.EXTRA_TITLE,name);startActivityForResult(i,request);}
    private void openFile(int request,String type){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(type);startActivityForResult(i,request);}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(result!=RESULT_OK||data==null)return;
        if(req==RC_LOGIN){try{AuthorizationResult r=Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(data);
            if(r.getAccessToken()!=null&&waitingToken!=null){Consumer<String> f=waitingToken;waitingToken=null;f.accept(r.getAccessToken());}
        }catch(Exception e){err(e);}return;}
        Uri uri=data.getData();if(uri==null)return;
        try{
            if(req==RC_BACKUP){try(OutputStream out=getContentResolver().openOutputStream(uri)){out.write(storage.export(state).getBytes(java.nio.charset.StandardCharsets.UTF_8));}notifyUser("Backup JSON berhasil disimpan");}
            else if(req==RC_PDF){lastResult=PoRules.calculate(state);try(OutputStream out=getContentResolver().openOutputStream(uri)){PdfExporter.write(state,lastResult,out);}notifyUser("PDF berhasil dibuat");}
            else if(req==RC_RESTORE){ByteArrayOutputStream buffer=new ByteArrayOutputStream();try(InputStream in=getContentResolver().openInputStream(uri)){byte[] b=new byte[8192];int n;while((n=in.read(b))>0)buffer.write(b,0,n);}
                String json=buffer.toString("UTF-8");confirm("Muat backup JSON dan ganti seluruh data lokal (Master + Stok + Kebutuhan)?",()->{
                    try{storage.snapshot(state);state=storage.importBackup(json);lastResult=PoRules.calculate(state);render(0);notifyUser("Restore selesai");}catch(Exception e){err(e);}
                });}
        }catch(Exception e){err(e);}
    }
}
