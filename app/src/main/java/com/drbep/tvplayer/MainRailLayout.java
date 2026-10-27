package com.drbep.tvplayer;

import org.json.*;
import java.util.*;

/** Local, portable navigation preference. IDs never depend on translated labels or catalogue counts. */
final class MainRailLayout {
    static final String PREF="ui_main_rail_v1";
    static final String SPORTS="sports",PALANTIR="palantir",SEARCH="search",HOME="home",LIVE="live",LIBRARY="library",
            MOVIES="movies",SERIES="series",GUIDE="guide",CATCHUP="catchup";
    static final List<String> IDS=Collections.unmodifiableList(Arrays.asList(
            SEARCH,HOME,LIVE,LIBRARY,MOVIES,SERIES,GUIDE,CATCHUP,PALANTIR,SPORTS));
    private static final MainRailLayout DEFAULT=new MainRailLayout(IDS,Collections.emptySet());
    final List<String> order;
    final Set<String> hidden;
    private MainRailLayout(Collection<String> requested,Collection<String> hidden) {
        LinkedHashSet<String> ids=new LinkedHashSet<>();
        for(String id:requested)if(IDS.contains(id))ids.add(id);
        ids.addAll(IDS);
        this.order=Collections.unmodifiableList(new ArrayList<>(ids));
        LinkedHashSet<String> excluded=new LinkedHashSet<>();
        for(String id:hidden)if(IDS.contains(id)&&!HOME.equals(id))excluded.add(id);
        this.hidden=Collections.unmodifiableSet(excluded);
    }
    static MainRailLayout defaults(){return DEFAULT;}
    static MainRailLayout decode(String raw) {
        if(raw==null||raw.isEmpty()||raw.length()>4096)return DEFAULT;
        try {
            JSONObject json=new JSONObject(raw);
            if(json.optInt("version")!=1)return DEFAULT;
            return new MainRailLayout(strings(json.optJSONArray("order")),strings(json.optJSONArray("hidden")));
        } catch(Exception invalid){return DEFAULT;}
    }
    private static List<String> strings(JSONArray values) {
        List<String> result=new ArrayList<>();
        if(values!=null)for(int i=0;i<Math.min(values.length(),32);i++)
            if(values.opt(i) instanceof String)result.add(values.optString(i));
        return result;
    }
    String encode() throws JSONException {
        return new JSONObject().put("version",1).put("order",new JSONArray(order))
                .put("hidden",new JSONArray(hidden)).toString();
    }
    static boolean validBackup(String raw) {
        if(raw==null||raw.length()>4096)return false;
        try {
            JSONObject json=new JSONObject(raw);
            Set<String> fields=new HashSet<>();
            Iterator<String> names=json.keys();while(names.hasNext())fields.add(names.next());
            if(!(json.opt("version") instanceof Integer)||json.getInt("version")!=1
                    ||!fields.equals(new HashSet<>(Arrays.asList("version","order","hidden"))))return false;
            for(String field:Arrays.asList("order","hidden")) {
                if(!(json.opt(field) instanceof JSONArray))return false;
                JSONArray values=json.getJSONArray(field);
                if(values.length()>IDS.size())return false;
                Set<String> seen=new HashSet<>();
                for(int i=0;i<values.length();i++){
                    Object item=values.get(i);
                    if(!(item instanceof String)||!IDS.contains(item)||!seen.add((String)item)
                            ||(field.equals("hidden")&&HOME.equals(item)))return false;
                }
            }
            return true;
        } catch(Exception invalid){return false;}
    }
    boolean visible(String id){return IDS.contains(id)&&(!hidden.contains(id)||HOME.equals(id));}
    List<String> visibleOrder(Collection<String> available) {
        List<String> result=new ArrayList<>();
        for(String id:order)if(visible(id)&&(HOME.equals(id)||available.contains(id)))result.add(id);
        return result;
    }
    MainRailLayout move(String id,int delta) {
        int from=order.indexOf(id);
        if(from<0||delta==0)return this;
        int to=from+Integer.signum(delta);
        if(to<0||to>=order.size())return this;
        List<String> next=new ArrayList<>(order);
        Collections.swap(next,from,to);
        return new MainRailLayout(next,hidden);
    }
    MainRailLayout withVisible(String id,boolean value) {
        if(!IDS.contains(id)||HOME.equals(id))return this;
        Set<String> next=new LinkedHashSet<>(hidden);
        if(value)next.remove(id);else next.add(id);
        return new MainRailLayout(order,next);
    }
    static String sectionId(String section) {
        if("En directo".equals(section))return LIVE;
        if("Mi biblioteca".equals(section))return LIBRARY;
        return HOME;
    }
    static String label(String id) {
        switch(id){
            case SEARCH:return "Buscar";
            case HOME:return "Inicio";
            case LIVE:return "En directo";
            case LIBRARY:return "Mi biblioteca";
            case MOVIES:return "Películas";
            case SERIES:return "Series";
            case GUIDE:return "Guía";
            case CATCHUP:return "Últimos 7 días";
 case PALANTIR:return "Palantir";
 case SPORTS:return "Agenda deportiva";
            default:return "";
        }
    }
}
