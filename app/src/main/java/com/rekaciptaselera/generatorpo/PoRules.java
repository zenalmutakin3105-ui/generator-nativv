package com.rekaciptaselera.generatorpo;

import java.util.*;

/** Pure Java domain model and allocation engine — NO WebView and NO Android dependencies. */
public final class PoRules {
    private PoRules() {}
    public static final String[] SUPPLIERS={"Yamiku","Japfa","TMS","BPA","BSD","SREEYA","BIF","MBU"};
    public static final String[] SKU={"adpa","utuh","fillet"};
    public static final String[] SKU_NAMES={"Ayam Dada Paha Atas","Ayam Utuh","Ayam Fillet"};
    public static final String[] UOM={"Pack","Ekor","Pack"};
    public static final String[] OUTLETS={"Cisitu","Unisba","Widyatama","Sekeloa","Unpar","Unla","Antapani","Jatinangor","Metro Margahayu","Sukapura","UIN Bandung","Ujung Berung","Cihanjuang","Gerlong","Maranatha","Melong","Taman Kopo Indah","Unjani","Kelapa Dua","Dramaga","Kukusan UI","Unpam","UIN Jakarta","Binus","Trisakti","Mercubuana","Uhamka","Tebet","IKPN Bintaro"};
    public static final String[] ROUTES={"Rute Bandung","Rute Jakarta"};
    public static final String[] NOTES={"Request 00.00-06.00","No Lunch Time (11.00 - 13.00 WIB)","Drop DC Buahbatu"};
    public static class Stock { public String supplier,sku,warehouse;public double qty;
        public Stock(String supplier,String sku,String warehouse,double qty){this.supplier=supplier;this.sku=sku;this.warehouse=warehouse;this.qty=qty;}}
    public static class Demand { public String outlet,sku,preferred,request,ship;public double qty;
        public Demand(String outlet,String sku,String preferred,String request,String ship,double qty){this.outlet=outlet;this.sku=sku;this.preferred=preferred;this.request=request;this.ship=ship;this.qty=qty;}}
    public static class Allocation { public Demand demand;public String supplier;public double qty;public Allocation(Demand d,String s,double q){demand=d;supplier=s;qty=q;} }
    public static class Pending { public Demand demand;public double qty;public Pending(Demand d,double q){demand=d;qty=q;} }
    public static class Rounding { public Demand demand;public String supplier;public double requested,allocated,pending,difference;
        public Rounding(Demand d,String supplier,double requested,double allocated,double pending){this.demand=d;this.supplier=supplier;this.requested=requested;this.allocated=allocated;this.pending=pending;this.difference=Math.round((allocated+pending-requested)*100000000d)/100000000d;}}
    public static class Row { public String outlet,note;public double qty; public Row(String o,String n,double q){outlet=o;note=n;qty=q;} }
    public static class Table { public String supplier,sku,route,request,ship;public List<Row> rows=new ArrayList<>();
        public Table(String supplier,String sku,String route,String request,String ship){this.supplier=supplier;this.sku=sku;this.route=route;this.request=request;this.ship=ship;}
        public double total(){double q=0;for(Row r:rows)q+=r.qty;return q;} }
    public static class Result { public List<Table> tables=new ArrayList<>();public List<Pending> pending=new ArrayList<>();public List<Rounding> rounding=new ArrayList<>();public List<Allocation> allocations=new ArrayList<>();public Map<String,Double> stockLeft=new HashMap<>(); }
    public static class State { public List<Stock> stocks=new ArrayList<>();public List<Demand> demands=new ArrayList<>();
        public Map<String,Double> prices=new LinkedHashMap<>();public Map<String,String> routes=new LinkedHashMap<>();
        public Map<String,List<String>> restrictions=new LinkedHashMap<>();public Map<String,String> noteExceptions=new LinkedHashMap<>();
        public boolean restrictionsEnabled=false,allowFallback=false;
        public State(){for(int i=0;i<OUTLETS.length;i++)routes.put(OUTLETS[i],i<18?ROUTES[0]:ROUTES[1]);prices.put("Yamiku|utuh",29920d);} }
    public static String skuName(String sku){for(int i=0;i<SKU.length;i++)if(SKU[i].equals(sku))return SKU_NAMES[i];return sku;}
    public static String uom(String sku){for(int i=0;i<SKU.length;i++)if(SKU[i].equals(sku))return UOM[i];return "Pack";}
    public static int supplierIndex(String x){for(int i=0;i<SUPPLIERS.length;i++)if(SUPPLIERS[i].equals(x))return i;return 999;}
    public static int skuIndex(String x){for(int i=0;i<SKU.length;i++)if(SKU[i].equals(x))return i;return 999;}
    public static String route(State state,String outlet){return state.routes.getOrDefault(outlet,ROUTES[0]);}
    public static String warehouseForRoute(String route){return ROUTES[1].equals(route)?"Jakarta":"Bandung";}
    public static String invKey(String supplier,String sku,String wh){return supplier+"|"+sku+("BPA".equals(supplier)?"|"+(Arrays.asList("Bandung","Jakarta").contains(wh)?wh:"BELUM_DITENTUKAN"):"");}
    public static String demandKey(State state,String supplier,Demand d){return invKey(supplier,d.sku,"BPA".equals(supplier)?warehouseForRoute(route(state,d.outlet)):null);}
    public static int multiple(String supplier,String sku){
        if("utuh".equals(sku))return 25;
        if("adpa".equals(sku))return "Yamiku".equals(supplier)?25:20;
        if("fillet".equals(sku)&&"BPA".equals(supplier))return 20;
        return 1;
    }
    public static double toPoUom(double input,String sku){return "utuh".equals(sku)?input:input/10d;}
    public static double round(double quantity,int multiple){return Math.floor(quantity/multiple+0.5d)*multiple;}
    public static boolean restricted(State st,String supplier,String outlet){return st.restrictionsEnabled&&st.restrictions.getOrDefault(supplier,Collections.emptyList()).contains(outlet);}
    public static String note(State st,String supplier,String outlet){String rule=st.noteExceptions.get(outlet);return rule==null||rule.isEmpty()?("Yamiku".equals(supplier)||"BSD".equals(supplier)?NOTES[0]:NOTES[1]):rule;}
    public static String noted(String supplier,String sku){String code="";
        if("Yamiku".equals(supplier))code="adpa".equals(sku)?"Parting":"utuh".equals(sku)?"AU08":"";
        if("Japfa".equals(supplier))code="adpa".equals(sku)?"CU9":"utuh".equals(sku)?"CU2":"";
        if("BPA".equals(supplier))code="adpa".equals(sku)?"ATPS":"utuh".equals(sku)?"BKK":"fillet".equals(sku)?"BB Katsu":"";
        if("TMS".equals(supplier)&&"adpa".equals(sku))code="ATPS";
        return code.isEmpty()?"Noted":"Noted : "+code;
    }
    public static Result calculate(State st){
        Result result=new Result();Map<String,Double> inv=result.stockLeft;
        for(Stock s:st.stocks)if(s.qty>0){String k=invKey(s.supplier,s.sku,s.warehouse);inv.put(k,inv.getOrDefault(k,0d)+s.qty);}
        List<Demand> ds=new ArrayList<>(st.demands);
        ds.sort(Comparator.comparing((Demand d)->d.ship).thenComparing(d->d.request));
        for(Demand d:ds){
            if(d.qty<=0)continue;
            double requested=toPoUom(d.qty,d.sku),left=requested,allocated=0;String roundingSupplier=d.preferred;
            List<String> available=new ArrayList<>();
            for(String supplier:SUPPLIERS)if(inv.getOrDefault(demandKey(st,supplier,d),0d)>0&&!restricted(st,supplier,d.outlet))available.add(supplier);
            available.sort((a,b)->{
                Double ap=st.prices.get(a+"|"+d.sku),bp=st.prices.get(b+"|"+d.sku);
                if(ap==null&&bp!=null)return 1;if(ap!=null&&bp==null)return -1;
                int cmp=ap==null?0:Double.compare(ap,bp);return cmp!=0?cmp:Integer.compare(supplierIndex(a),supplierIndex(b));
            });
            if(d.preferred!=null&&!"auto".equals(d.preferred)&&!d.preferred.isEmpty()&&!restricted(st,d.preferred,d.outlet)){
                List<String> choose=new ArrayList<>();choose.add(d.preferred);
                if(st.allowFallback)for(String s:available)if(!s.equals(d.preferred))choose.add(s);
                available=choose;
            }
            for(String supplier:available){
                String key=demandKey(st,supplier,d);double stock=inv.getOrDefault(key,0d);if(stock<=0)continue;
                int mult=multiple(supplier,d.sku);double target=round(left,mult);
                if(target==0){roundingSupplier=supplier;left=0;break;}
                double whole=mult==1?stock:Math.floor(stock/mult)*mult;
                double taken=Math.min(target,whole);
                if(taken>0){
                    result.allocations.add(new Allocation(d,supplier,taken));allocated+=taken;inv.put(key,stock-taken);
                    if(taken==target)roundingSupplier=supplier;
                    left=taken==target?0:Math.max(0,left-taken);
                }
                if(left==0)break;
            }
            if(left>0)result.pending.add(new Pending(d,left));
            Rounding delta=new Rounding(d,roundingSupplier,requested,allocated,left);
            if(Math.abs(delta.difference)>0.0000001)result.rounding.add(delta);
        }
        Map<String,Table> grouped=new LinkedHashMap<>();
        for(Allocation a:result.allocations){
            Demand d=a.demand;String route=route(st,d.outlet),note=note(st,a.supplier,d.outlet);
            String key=a.supplier+"|"+d.sku+"|"+route+"|"+d.request+"|"+d.ship;
            Table t=grouped.computeIfAbsent(key,k->new Table(a.supplier,d.sku,route,d.request,d.ship));
            Row existing=null;for(Row r:t.rows)if(r.outlet.equals(d.outlet)&&r.note.equals(note)){existing=r;break;}
            if(existing==null)t.rows.add(new Row(d.outlet,note,a.qty));else existing.qty+=a.qty;
        }
        result.tables.addAll(grouped.values());
        for(Table t:result.tables)t.rows.sort(Comparator.comparing((Row r)->r.outlet,String.CASE_INSENSITIVE_ORDER));
        result.tables.sort(Comparator.comparingInt((Table t)->supplierIndex(t.supplier)).thenComparingInt(t->skuIndex(t.sku))
                .thenComparingInt(t->t.route.equals(ROUTES[0])?0:1).thenComparing(t->t.ship).thenComparing(t->t.request));
        return result;
    }
}
