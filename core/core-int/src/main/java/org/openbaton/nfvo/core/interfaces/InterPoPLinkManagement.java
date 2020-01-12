package org.openbaton.nfvo.core.interfaces;

import java.io.IOException;
import java.util.List;
import org.openbaton.catalogue.nfvo.vibeswizzard.BaseInterPoPLink;
import org.openbaton.exceptions.BadRequestException;
import org.openbaton.exceptions.NotAllowedException;
import org.openbaton.exceptions.NotFoundException;

public interface InterPoPLinkManagement {

  List<BaseInterPoPLink> retrieveFromLocalFile() throws NotFoundException, IOException;

  List<BaseInterPoPLink> retrieveFromService() throws IOException;

  /** @param link */
  void delete(BaseInterPoPLink link);

  /** @param new_link */
  BaseInterPoPLink update(BaseInterPoPLink newLink)
      throws NotAllowedException, BadRequestException, NotFoundException;

  /** */
  Iterable<BaseInterPoPLink> query();

  /** @param linkName */
  BaseInterPoPLink queryByName(String linkName) throws NotFoundException;

  /** @param slice */
  BaseInterPoPLink query(BaseInterPoPLink link) throws NotFoundException;
}
