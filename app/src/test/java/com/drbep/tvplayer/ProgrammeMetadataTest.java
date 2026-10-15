package com.drbep.tvplayer;
import org.junit.Test;
import org.json.JSONObject;
import static org.junit.Assert.*;

public class ProgrammeMetadataTest {
 @Test public void officialNestedMetadataHasNoInventedAgeOrScale()throws Exception {
  ProgrammeMetadata m=ProgrammeMetadata.official(new JSONObject("{\"Anno\":\"2020\",\"Genero\":{\"ComAntena\":\"Naturaleza\"},\"NivelMoral\":{\"Id\":\"+7\"},\"Valoracion\":{\"Valoracion\":3.0,\"Valoraciones\":148},\"Sinopsis\":\"Descripción\"}"));
  assertEquals("2020",m.year);assertEquals("+7",m.age);assertEquals("3,0/5",m.rating);assertEquals("Naturaleza",m.genre);assertEquals("Descripción",m.description);
 }
 @Test public void missingAndUnratedValuesAreNotShown()throws Exception {
  ProgrammeMetadata m=ProgrammeMetadata.official(new JSONObject("{\"Anno\":null,\"NivelMoral\":null,\"Valoracion\":null}"));
  assertEquals("",m.year);assertEquals("",m.age);assertEquals("",m.rating);assertEquals("",m.description);
  assertEquals("",ProgrammeMetadata.official(new JSONObject("{\"Valoracion\":{\"Valoracion\":0,\"Valoraciones\":0}}")).rating);
 }
 @Test public void serializedMetadataSurvivesSavedCatchupAndOldRecords()throws Exception {
  ProgrammeMetadata m=new ProgrammeMetadata("Drama","2020","+16","4,0/5","Sinopsis");
  EpgRepository.EpgProgram p=new EpgRepository.EpgProgram("1","Canal","LA2","Título","","Sinopsis","2026-10-14T10:00:00Z","2026-10-14T11:00:00Z","Drama",0,m,"https://ottcache.dof6.com/movistarplus/webplayer/contents/123/details");
  EpgRepository.EpgProgram decoded=U7dCatalogStore.decode(U7dCatalogStore.encode(p));
  assertEquals(m,decoded.metadata);assertEquals(p.detailsUrl,decoded.detailsUrl);assertEquals(p.tvgId,decoded.tvgId);
  assertNull(U7dCatalogStore.decode(new JSONObject("{\"title\":\"Old\"}")).metadata);
 }
 @Test public void incompleteDetailsPreserveGuideDataAndNullIsNotText()throws Exception {
  ProgrammeMetadata base=new ProgrammeMetadata("Naturaleza","","+7","","Texto");
  assertEquals(base,ProgrammeMetadata.EMPTY.merge(base));
  assertEquals("",ProgrammeMetadata.official(new JSONObject("{\"Descripcion\":null,\"Sinopsis\":null}")).description);
 }
 @Test public void displayedMetadataChangesInvalidateEpgContent() {
  EpgRepository.EpgProgram a=new EpgRepository.EpgProgram("1","C","LA2","T","","","2026-10-14T10:00:00Z","2026-10-14T11:00:00Z","Drama",0);
  EpgRepository.EpgProgram b=new EpgRepository.EpgProgram("1","C","LA2","T","","","2026-10-14T10:00:00Z","2026-10-14T11:00:00Z","Drama",0,new ProgrammeMetadata("Drama","2020","+7","",""),"");
  assertFalse(EpgRepository.sameProgramContent(a,b));
 }
 @Test public void presentationUsesConfirmedProviderCodeWithoutChangingProgramme() {
  EpgRepository.EpgProgram p=new EpgRepository.EpgProgram("1","C","legacy.es","T","","","2026-10-14T10:00:00Z","2026-10-14T11:00:00Z","Drama",0);
  ProgrammeMetadataUiModel model=new ProgrammeMetadataUiModel(p,true,"CPACC");
  assertEquals("CPACC|"+EpgTimeCodec.parseEpochMillis(p.startTime),model.key);
  assertEquals("legacy.es",p.tvgId);assertNotNull(model.loader);
  assertNull(new ProgrammeMetadataUiModel(p,false,"CPACC").loader);
 }
}
