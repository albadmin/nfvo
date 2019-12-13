package org.openbaton.nfvo.core.interfaces;

import java.io.IOException;
import java.util.List;
import org.openbaton.catalogue.nfvo.vibeswizzard.BaseSlice;
import org.openbaton.exceptions.BadRequestException;
import org.openbaton.exceptions.NotAllowedException;
import org.openbaton.exceptions.NotFoundException;

public interface SliceManagement {

  List<BaseSlice> retrieveFromLocalFile() throws NotFoundException, IOException;

  List<BaseSlice> retrieveFromService() throws IOException;

  /** @param slice */
  void delete(BaseSlice slice);

  /** @param new_slice */
  BaseSlice update(BaseSlice newSlice)
      throws NotAllowedException, BadRequestException, NotFoundException;

  /** */
  Iterable<BaseSlice> query();

  /** @param sliceName */
  BaseSlice queryByName(String sliceName) throws NotFoundException;

  /** @param slice */
  BaseSlice query(BaseSlice slice) throws NotFoundException;
}
