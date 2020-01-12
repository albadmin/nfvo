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
import org.openbaton.catalogue.nfvo.vibeswizzard.BaseSlice;
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
public class SliceManagement implements org.openbaton.nfvo.core.interfaces.SliceManagement {

  @Value("${nfvo.slice.server.ip:localhost}")
  private String sliceServerIp;

  @Value("${nfvo.slice.server.ip:8080}")
  private String sliceServerPort;

  @Value("${nfvo.slice.localRepo:/home/albaadmin/Desktop/openbatonSrc/slices.json}")
  private String sliceLocalRepo;

  public List<BaseSlice> retrieveFromLocalFile() throws NotFoundException, IOException {

    Path sliceLocal = Paths.get(sliceLocalRepo);
    if (!Files.exists(sliceLocal)) {
      throw new NotFoundException("Slice local repo not found");
    }

    StringBuilder content = new StringBuilder();
    String line = null;
    try (BufferedReader in = Files.newBufferedReader(sliceLocal)) {
      while ((line = in.readLine()) != null) {
        content.append(line);
      }
    }

    List<BaseSlice> container = new ArrayList<>();
    JsonElement jsonElement = new JsonParser().parse(content.toString());
    if (jsonElement instanceof JsonObject) {
      JsonObject jobject = jsonElement.getAsJsonObject();
      container.add(parseSliceObject(jobject));
    } else if (jsonElement instanceof JsonArray) {
      JsonArray jarray = jsonElement.getAsJsonArray();
      Iterator<JsonElement> it = jarray.iterator();
      for (; it.hasNext(); ) {
        JsonElement elm = it.next();
        container.add(parseSliceObject(elm.getAsJsonObject()));
      }
    }

    return container;
  }

  public List<BaseSlice> retrieveFromService() throws IOException {
    String url = "http://" + sliceServerIp + ":" + sliceServerPort + "/api/v1/slices";

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
    List<BaseSlice> container = new ArrayList<>();
    Iterator<JsonElement> it = slices.iterator();

    for (; it.hasNext(); ) {
      JsonElement elm = it.next();
      container.add(parseSliceObject(elm.getAsJsonObject()));
    }
    return container;
  }

  private BaseSlice parseSliceObject(JsonObject slice) {
    String name = (slice.get("name") != null) ? slice.get("name").getAsString() : "N/A";
    String description =
        (slice.get("description") != null) ? slice.get("description").getAsString() : "N/A";
    String subnetwork =
        (slice.get("subnetwork") != null) ? slice.get("subnetwork").getAsString() : "N/A";

    return new BaseSlice(name, description, subnetwork);
  }

  /** @param user */
  public void delete(BaseSlice slice) {}

  /** @param new_user */
  public BaseSlice update(BaseSlice newSlice) throws NotAllowedException, BadRequestException {
    return null;
  }

  /** */
  public Iterable<BaseSlice> query() {
    return null;
  }

  /** @param username */
  public BaseSlice queryByName(String sliceName) {
    return null;
  }

  /** @param id */
  public BaseSlice query(BaseSlice slice) {
    return slice;
  }
}
