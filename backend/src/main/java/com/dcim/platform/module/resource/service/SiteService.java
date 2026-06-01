package com.dcim.platform.module.resource.service;

import com.dcim.platform.module.resource.entity.SiteEntity;
import com.dcim.platform.module.resource.repository.SiteRepository;
import com.dcim.platform.common.security.DataScopeService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SiteService {

    private final SiteRepository repository;
    private final DataScopeService dataScopeService;

    public SiteService(SiteRepository repository, DataScopeService dataScopeService) {
        this.repository = repository;
        this.dataScopeService = dataScopeService;
    }

    public List<SiteEntity> list() {
        List<SiteEntity> all = repository.findAll();
        return dataScopeService.filterByStationScope(all, SiteEntity::getId);
    }

    public SiteEntity getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Site not found: " + id));
    }

    public SiteEntity create(SiteEntity entity) {
        return repository.save(entity);
    }

    public SiteEntity update(Long id, SiteEntity entity) {
        SiteEntity existing = getById(id);
        existing.setSiteCode(entity.getSiteCode());
        existing.setSiteName(entity.getSiteName());
        existing.setSiteType(entity.getSiteType());
        existing.setRegion(entity.getRegion());
        existing.setAddress(entity.getAddress());
        existing.setLongitude(entity.getLongitude());
        existing.setLatitude(entity.getLatitude());
        existing.setContactPerson(entity.getContactPerson());
        existing.setContactPhone(entity.getContactPhone());
        existing.setStatus(entity.getStatus());
        existing.setDescription(entity.getDescription());
        return repository.save(existing);
    }

    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}
