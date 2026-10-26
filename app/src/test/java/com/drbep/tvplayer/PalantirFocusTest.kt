package com.drbep.tvplayer
import org.junit.Assert.assertEquals
import org.junit.Test
class PalantirFocusTest {
 @Test fun removedSelectionFallsBackToFirstResult() {
  assertEquals("new",palantirFocusTarget(listOf("new","other"),"old"))
 }
 @Test fun returningFromDetailKeepsExistingSelection() {
  assertEquals("other",palantirFocusTarget(listOf("new","other"),"other"))
 }
 @Test fun emptyResultsHaveNoPosterFocus() {
  assertEquals("",palantirFocusTarget(emptyList(),"old"))
 }
}
