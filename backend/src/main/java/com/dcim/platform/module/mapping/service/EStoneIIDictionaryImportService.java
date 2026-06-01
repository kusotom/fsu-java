package com.dcim.platform.module.mapping.service;

import com.dcim.platform.module.mapping.entity.*;
import com.dcim.platform.module.mapping.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class EStoneIIDictionaryImportService {

    private final EStoneIITemplateDictionaryParser parser;
    private final EStoneIISignalDictionaryRepository signalRepository;
    private final EStoneIIEventDictionaryRepository eventRepository;
    private final EStoneIIControlReferenceRepository controlRepository;
    private final DeviceSignalCandidateRepository candidateRepository;

    public EStoneIIDictionaryImportService(EStoneIITemplateDictionaryParser parser,
                                           EStoneIISignalDictionaryRepository signalRepository,
                                           EStoneIIEventDictionaryRepository eventRepository,
                                           EStoneIIControlReferenceRepository controlRepository,
                                           DeviceSignalCandidateRepository candidateRepository) {
        this.parser = parser;
        this.signalRepository = signalRepository;
        this.eventRepository = eventRepository;
        this.controlRepository = controlRepository;
        this.candidateRepository = candidateRepository;
    }

    @Transactional
    public ImportSummary importIfNeeded() {
        if (signalRepository.count() >= 39 && eventRepository.count() >= 13
                && controlRepository.count() >= 4 && candidateRepository.count() >= 23) {
            return new ImportSummary(0, 0, 0, 0, false);
        }
        return importAll();
    }

    @Transactional
    public ImportSummary importAll() {
        EStoneIITemplateDictionaryParser.TemplateDictionaryData data = parser.parseAll();
        LocalDateTime now = LocalDateTime.now();
        int signals = 0;
        int events = 0;
        int controls = 0;
        int candidates = 0;

        for (var d : data.signals()) {
            EStoneIISignalDictionaryEntity e = signalRepository.findBySignalId(d.signalId())
                    .orElseGet(EStoneIISignalDictionaryEntity::new);
            if (e.getCreatedAt() == null) e.setCreatedAt(now);
            e.setSignalId(d.signalId());
            e.setSignalName(d.signalName());
            e.setSignalCategory(d.signalCategory());
            e.setSignalType(d.signalType());
            e.setChannelNo(d.channelNo());
            e.setChannelType(d.channelType());
            e.setUnit(d.unit());
            e.setBaseTypeId(d.baseTypeId());
            e.setSignalMeaningsRaw(d.signalMeaningsRaw());
            e.setSignalMeaningsJson(parser.meaningsToJson(d.signalMeaningsRaw()));
            e.setExpression(d.expression());
            e.setDisplayIndex(d.displayIndex());
            e.setTemplateVariant(d.templateVariant());
            e.setMappingConfidence(d.mappingConfidence());
            e.setNeedRealDataConfirm(d.needRealDataConfirm());
            e.setDerived(d.derived());
            e.setEnable(d.enable());
            e.setVisible(d.visible());
            e.setSourceFile(d.sourceFile());
            e.setUpdatedAt(now);
            signalRepository.save(e);
            signals++;
        }

        for (var d : data.events()) {
            EStoneIIEventDictionaryEntity e = eventRepository.findByEventId(d.eventId())
                    .orElseGet(EStoneIIEventDictionaryEntity::new);
            if (e.getCreatedAt() == null) e.setCreatedAt(now);
            e.setEventId(d.eventId());
            e.setEventName(d.eventName());
            e.setSignalId(d.signalId());
            e.setStartExpression(d.startExpression());
            e.setEventCategory(d.eventCategory());
            e.setEventSeverity(d.eventSeverity());
            e.setStartOperation(d.startOperation());
            e.setStartCompareValue(d.startCompareValue());
            e.setMeanings(d.meanings());
            e.setBaseTypeId(d.baseTypeId());
            e.setTemplateVariant(d.templateVariant());
            e.setNeedRealDataConfirm(d.needRealDataConfirm());
            e.setSourceFile(d.sourceFile());
            e.setEnable(d.enable());
            e.setVisible(d.visible());
            e.setUpdatedAt(now);
            eventRepository.save(e);
            events++;
        }

        for (var d : data.controls()) {
            EStoneIIControlReferenceEntity e = controlRepository.findByCommandId(d.commandId())
                    .orElseGet(EStoneIIControlReferenceEntity::new);
            if (e.getCreatedAt() == null) e.setCreatedAt(now);
            e.setCommandId(d.commandId());
            e.setCommandName(d.commandName());
            e.setCommandCategory(d.commandCategory());
            e.setCommandSeverity(d.commandSeverity());
            e.setCmdToken(d.cmdToken());
            e.setSignalId(d.signalId());
            e.setMeanings(d.meanings());
            e.setParameterName(null);
            e.setParameterValue(null);
            e.setSourceFile(d.sourceFile());
            e.setEnabledForControl(false);
            e.setControlAccess("disabled");
            e.setSource("template_reference_only");
            e.setUpdatedAt(now);
            controlRepository.save(e);
            controls++;
        }

        for (var d : data.candidates()) {
            DeviceSignalCandidateEntity e = candidateRepository
                    .findFirstByFsuIdAndDeviceIdAndSignalIdAndMappingSource(
                            d.fsuId(), d.deviceId(), d.signalId(), d.mappingSource())
                    .orElseGet(DeviceSignalCandidateEntity::new);
            if (e.getCreatedAt() == null) e.setCreatedAt(now);
            e.setFsuId(d.fsuId());
            e.setDeviceId(d.deviceId());
            e.setDeviceCode(d.deviceCode());
            e.setDeviceName(d.deviceName());
            e.setTowerCategoryId(d.towerCategoryId());
            e.setTowerDeviceType(d.towerDeviceType());
            e.setEmersonDeviceTypeId(d.emersonDeviceTypeId());
            e.setSignalId(d.signalId());
            e.setSignalName(d.signalName());
            e.setConfidence(d.confidence());
            e.setTemplateVariant(d.templateVariant());
            e.setNeedRealDataConfirm(d.needRealDataConfirm());
            e.setMappingSource(d.mappingSource());
            e.setVerifiedByRealData(false);
            e.setMappingStatus(d.mappingStatus());
            e.setUpdatedAt(now);
            candidateRepository.save(e);
            candidates++;
        }

        return new ImportSummary(signals, events, controls, candidates, true);
    }

    public record ImportSummary(int signals, int events, int controls, int candidates, boolean imported) {}
}
