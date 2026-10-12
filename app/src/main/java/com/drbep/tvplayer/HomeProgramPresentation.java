package com.drbep.tvplayer;

/** Display-only shortening; the original EPG and playback metadata stay intact. */
final class HomeProgramPresentation {
    final String title, metadata;
    private HomeProgramPresentation(String title,String metadata){this.title=title;this.metadata=metadata;}
    static HomeProgramPresentation of(String input){
        String text=input==null?"":input.trim().replaceFirst("(?i)^DIRECTO\\s+","");
        java.util.regex.Matcher round=java.util.regex.Pattern.compile("(?i)^Jornada\\s+(\\d+)\\s*:\\s*(.+)$").matcher(text);
        String detail="";
        if(round.matches()){detail="Jornada "+round.group(1);text=round.group(2);}
        java.util.regex.Matcher season=java.util.regex.Pattern.compile("^(.*?)\\s+T(\\d{2}/\\d{2})(?:\\s*·\\s*(.*))?$").matcher(text);
        if(season.matches()){
            text=season.group(1);
            String league=season.group(3)==null?"":season.group(3).trim();
            detail=(league.isEmpty()?"":league+" · ")+detail+(detail.isEmpty()?"":" · ")+"T"+season.group(2);
        }
        return new HomeProgramPresentation(text,detail);
    }
}
