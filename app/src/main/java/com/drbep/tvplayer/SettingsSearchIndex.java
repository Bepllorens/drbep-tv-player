package com.drbep.tvplayer;
import java.text.Normalizer;
import java.util.Locale;
final class SettingsSearchIndex {
 private static final java.util.regex.Pattern MARKS = java.util.regex.Pattern.compile("\\p{M}+");
 static String normalize(String text) {
  return MARKS.matcher(Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD))
    .replaceAll("").toLowerCase(Locale.ROOT).trim();
 }
 static String key(String title) { return normalize(title).replaceAll("[^a-z0-9]+", "-"); }
 static boolean matches(String query, String text) {
  String value = normalize(text);
  for (String word : normalize(query).split("\\s+")) if (!value.contains(word)) return false;
  return true;
 }
}
