package com.drbep.tvplayer;

import java.io.Serializable;
import java.util.Objects;
import org.json.JSONObject;

/** Descriptive metadata only; never participates in identity, permissions or playback. */
public final class ProgrammeMetadata implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final java.util.regex.Pattern YEAR=java.util.regex.Pattern.compile("[12][0-9]{3}");
    private static final java.util.regex.Pattern AGE=java.util.regex.Pattern.compile("(?i)TP|SC|\\+?(7|12|16|18)");
    public static final ProgrammeMetadata EMPTY = new ProgrammeMetadata("", "", "", "", "");
    public final String genre, year, age, rating, description;
    public ProgrammeMetadata(String genre, String year, String age, String rating, String description) {
        this.genre=clean(genre); this.year=clean(year); this.age=clean(age);
        this.rating=clean(rating); this.description=clean(description);
    }
    private static String clean(String s) { return s==null||"null".equals(s)?"":s.trim(); }
    static ProgrammeMetadata official(JSONObject p) {
        JSONObject genre=p.optJSONObject("Genero"), age=p.optJSONObject("NivelMoral"), rating=p.optJSONObject("Valoracion");
        String score="";
        if(rating!=null && rating.optInt("Valoraciones",0)>0) {
            double value=rating.optDouble("Valoracion",Double.NaN);
            if(Double.isFinite(value) && value>=0 && value<=5) score=String.format(java.util.Locale.forLanguageTag("es"),"%.1f/5",value);
        }
        String year=clean(p.optString("Anno"));
        if(!YEAR.matcher(year).matches())year="";
        String parental=age==null?"":clean(age.optString("Id"));
        if(!AGE.matcher(parental).matches())parental="";
        return new ProgrammeMetadata(genre==null?p.optString("GeneroComAntena"):genre.optString("ComAntena"),year,parental,score,TimelineProgrammeDetails.description(p));
    }
    JSONObject json() throws org.json.JSONException {
        return new JSONObject().put("genre",genre).put("year",year).put("age",age).put("rating",rating).put("description",description);
    }
    static ProgrammeMetadata stored(JSONObject p) {
        return p==null?null:new ProgrammeMetadata(p.optString("genre"),p.optString("year"),p.optString("age"),p.optString("rating"),p.optString("description"));
    }
    public ProgrammeMetadata merge(ProgrammeMetadata fallback) {
        if(fallback==null)return this;
        return new ProgrammeMetadata(genre.isEmpty()?fallback.genre:genre,year.isEmpty()?fallback.year:year,
            age.isEmpty()?fallback.age:age,rating.isEmpty()?fallback.rating:rating,description.isEmpty()?fallback.description:description);
    }
    public boolean equals(Object other) {
        if(!(other instanceof ProgrammeMetadata))return false; ProgrammeMetadata m=(ProgrammeMetadata)other;
        return genre.equals(m.genre)&&year.equals(m.year)&&age.equals(m.age)&&rating.equals(m.rating)&&description.equals(m.description);
    }
    public int hashCode(){return Objects.hash(genre,year,age,rating,description);}
}
