package org.openbaton.nfvo.repositories;

import java.util.List;
import org.openbaton.catalogue.nfvo.vibeswizzard.BaseSlice;
import org.springframework.data.repository.CrudRepository;

public interface SliceRepository extends CrudRepository<BaseSlice, String> {

  List<BaseSlice> findByProjectId(String projectId);

  BaseSlice findByProjectIdAndName(String projectId, String name);
}
