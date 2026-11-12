import com.rekaciptaselera.generatorpo.PoRules;
import java.util.*;
public class EngineTest {
    static void yes(boolean ok,String m){if(!ok)throw new RuntimeException("FAIL: "+m);}
    static PoRules.State base(String supplier,String sku,double input,double available){PoRules.State s=new PoRules.State();s.stocks.add(new PoRules.Stock(supplier,sku,supplier.equals("BPA")?"Bandung":"",available));s.demands.add(new PoRules.Demand("Cisitu",sku,supplier,"2026-11-12","2026-11-13",input));return s;}
    public static void main(String[] x){
      yes(PoRules.calculate(base("Yamiku","adpa",100,1000)).tables.isEmpty(),"Yamiku 100 PCS=10 Pack to 0 Pack");
      yes(PoRules.calculate(base("Yamiku","adpa",380,1000)).tables.get(0).total()==50,"Yamiku 380 PCS to 50 Pack");
      yes(PoRules.calculate(base("Japfa","adpa",100,1000)).tables.get(0).total()==20,"Japfa 100 PCS to 20 Pack");
      yes(PoRules.calculate(base("BPA","fillet",260,1000)).tables.get(0).total()==20,"BPA fillet 260 PCS to 20 Pack");
      yes(PoRules.calculate(base("Yamiku","utuh",35,1000)).tables.get(0).total()==25,"Ayam utuh 35 ekor to 25");
      PoRules.State cross=base("BPA","adpa",400,0);cross.stocks.clear();cross.stocks.add(new PoRules.Stock("BPA","adpa","Jakarta",1000));
      yes(PoRules.calculate(cross).tables.isEmpty(),"BPA Jakarta not for Bandung");
      PoRules.State limit=base("Yamiku","utuh",50,1000);limit.restrictionsEnabled=true;limit.restrictions.put("Yamiku",Collections.singletonList("Cisitu"));
      yes(PoRules.calculate(limit).tables.isEmpty(),"Supplier restriction");
      PoRules.State up=base("Yamiku","adpa",380,1000);yes(PoRules.calculate(up).rounding.get(0).difference==12,"rounding +12");
      PoRules.State down=base("Yamiku","adpa",350,1000);yes(PoRules.calculate(down).rounding.get(0).difference==-10,"rounding -10");
      PoRules.State noStock=base("Yamiku","adpa",500,0);yes(PoRules.calculate(noStock).pending.get(0).qty==50,"pending no stock");
      PoRules.State multi=new PoRules.State();multi.stocks.add(new PoRules.Stock("Yamiku","utuh","",100));multi.demands.add(new PoRules.Demand("Unisba","utuh","auto","2026-11-12","2026-11-13",25));multi.demands.add(new PoRules.Demand("Cisitu","utuh","auto","2026-11-12","2026-11-13",25));
      yes(PoRules.calculate(multi).tables.get(0).rows.get(0).outlet.equals("Cisitu"),"alphabetical outlets");
      yes(PoRules.noted("BPA","fillet").equals("Noted : BB Katsu"),"Noted code");
      System.out.println("ALL 12 NATIVE ENGINE TESTS PASS");
    }
}
