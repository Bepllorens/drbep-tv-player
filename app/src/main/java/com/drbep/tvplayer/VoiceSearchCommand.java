package com.drbep.tvplayer;

import java.text.Normalizer;
import java.util.Locale;

/** Local intent parsing only. Never sends the catalogue to an external assistant. */
final class VoiceSearchCommand {
    enum Kind { SEARCH, PLAY, GUIDE, RECOMMEND, SIMILAR }
    final Kind kind;
    final String query;
    VoiceSearchCommand(Kind kind, String query) { this.kind=kind; this.query=query; }
    static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("[¿?¡!]", "").trim().replaceAll("\\s+", " ");
    }
    static VoiceSearchCommand parse(String text) {
        String s=normalize(text);
        String[][] prefixes={{"que estan echando en ","GUIDE"},{"que echan en ","GUIDE"},{"que dan en ","GUIDE"},
            {"pon el canal ","PLAY"},{"pon ","PLAY"},{"reproduce ","PLAY"},
            {"peliculas parecidas a ","SIMILAR"},{"parecidas a ","SIMILAR"},{"similares a ","SIMILAR"},
            {"recomiendame algo de ","RECOMMEND"},{"algo de ","RECOMMEND"},{"peliculas de ","RECOMMEND"},
            {"busca ","SEARCH"},{"buscar ","SEARCH"}};
        for(String[] prefix:prefixes) if(s.startsWith(prefix[0]) && s.length()>prefix[0].length()) {
            String q=s.substring(prefix[0].length()).trim();
            if(prefix[1].equals("RECOMMEND")) {
                if(q.equals("amor") || q.equals("romanticas")) q="Romance";
                else if(q.equals("miedo")) q="Terror";
                else if(q.equals("risa")) q="Comedia";
                else if(q.equals("accion")) q="Acción";
                else if(q.equals("ciencia ficcion")) q="Ciencia ficción";
                else if(q.equals("animacion")) q="Animación";
            }
            return new VoiceSearchCommand(Kind.valueOf(prefix[1]),q);
        }
        return new VoiceSearchCommand(Kind.SEARCH,text == null ? "" : text.trim());
    }
}
