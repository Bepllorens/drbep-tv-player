package com.drbep.tvplayer;

public final class ProgrammeMetadataUiModel {
    public final String key;
    public final ProgrammeMetadata initial;
    public final java.util.concurrent.Callable<ProgrammeMetadata> loader;
    ProgrammeMetadataUiModel(EpgRepository.EpgProgram p, boolean official, String providerCode) {
        key=TimelineProgrammeDetails.key(providerCode,p.startTime);
        ProgrammeMetadata base=p.metadata==null?new ProgrammeMetadata(p.category,"","","",p.description):p.metadata;
        ProgrammeMetadata cached=TimelineProgrammeDetails.peek(key);
        initial=cached==null?base:cached.merge(base);
        loader=official?()->TimelineProgrammeDetails.loadMetadata(providerCode,p.startTime,p.detailsUrl).merge(base):null;
    }
}
