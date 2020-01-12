package org.openbaton.nfvo.core.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.openbaton.catalogue.nfvo.vibeswizzard.BaseInterPoPLink;
import org.openbaton.exceptions.BadRequestException;
import org.openbaton.exceptions.NotAllowedException;
import org.openbaton.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

@Service
@Scope
@ConfigurationProperties
@EnableAsync
public class InterPoPLinkManagement
    implements org.openbaton.nfvo.core.interfaces.InterPoPLinkManagement {

  @Value("${nfvo.interPoPLink.server.ip:localhost}")
  private String serverIp;

  @Value("${nfvo.interPoPLink.server.ip:8080}")
  private String serverPort;

  @Value("${nfvo.interPoPLink.localRepo:/home/albaadmin/Desktop/openbatonSrc/interPoPLink.json}")
  private String interPoPLinkLocalRepo;

  public List<BaseInterPoPLink> retrieveFromLocalFile() throws NotFoundException, IOException {

    Path sliceLocal = Paths.get(interPoPLinkLocalRepo);
    if (!Files.exists(sliceLocal)) {
      throw new NotFoundException("InterPoPLink local repo not found");
    }

    StringBuilder content = new StringBuilder();
    String line = null;
    try (BufferedReader in = Files.newBufferedReader(sliceLocal)) {
      while ((line = in.readLine()) != null) {
        content.append(line);
      }
    }

    List<BaseInterPoPLink> container = new ArrayList<>();
    JsonElement jsonElement = new JsonParser().parse(content.toString());
    if (jsonElement instanceof JsonObject) {
      JsonObject jobject = jsonElement.getAsJsonObject();
      container.add(parseInterPoPLinkObject(jobject));
    } else if (jsonElement instanceof JsonArray) {
      JsonArray jarray = jsonElement.getAsJsonArray();
      Iterator<JsonElement> it = jarray.iterator();
      for (; it.hasNext(); ) {
        JsonElement elm = it.next();
        container.add(parseInterPoPLinkObject(elm.getAsJsonObject()));
      }
    }

    return container;
  }

  public List<BaseInterPoPLink> retrieveFromService() throws IOException {
    String url = "http://" + serverIp + ":" + serverPort + "/api/v1/slices";

    URL sliceAPIURL = new URL(url);
    URLConnection conn = sliceAPIURL.openConnection();
    // retrieve a JSON formatted file
    StringBuilder content = new StringBuilder();
    String line = null;
    try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
      while ((line = in.readLine()) != null) {
        content.append(line);
      }
    }

    JsonObject jsonObject = new JsonParser().parse(content.toString()).getAsJsonObject();
    JsonArray slices = jsonObject.getAsJsonArray();
    // Iterate over employee array
    List<BaseInterPoPLink> container = new ArrayList<>();
    Iterator<JsonElement> it = slices.iterator();

    for (; it.hasNext(); ) {
      JsonElement elm = it.next();
      container.add(parseInterPoPLinkObject(elm.getAsJsonObject()));
    }
    return container;
  }

  private BaseInterPoPLink parseInterPoPLinkObject(JsonObject interPoPLink) {
    String name =
        (interPoPLink.get("name") != null) ? interPoPLink.get("name").getAsString() : "N/A";
    String description =
        (interPoPLink.get("description") != null)
            ? interPoPLink.get("description").getAsString()
            : "N/A";
    String subnetwork =
        (interPoPLink.get("subnetwork") != null)
            ? interPoPLink.get("subnetwork").getAsString()
            : "N/A";

    return new BaseInterPoPLink(name, description, subnetwork);
  }

  /** @param user */
  public void delete(BaseInterPoPLink link) {}

  /** @param new_link */
  public BaseInterPoPLink update(BaseInterPoPLink newLInk)
      throws NotAllowedException, BadRequestException {
    return null;
  }

  /** */
  public Iterable<BaseInterPoPLink> query() {
    return null;
  }

  /** @param username */
  public BaseInterPoPLink queryByName(String linkName) {
    return null;
  }

  /** @param id */
  public BaseInterPoPLink query(BaseInterPoPLink link) {
    return link;
  }
}
