/*
 * Copyright (c) 2015-2018 Open Baton (http://openbaton.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openbaton.nfvo.api.admin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.validation.Valid;
import org.openbaton.catalogue.mano.descriptor.NetworkServiceDescriptor;
import org.openbaton.catalogue.mano.descriptor.VirtualNetworkFunctionDescriptor;
import org.openbaton.catalogue.mano.record.NetworkServiceRecord;
import org.openbaton.catalogue.nfvo.Configuration;
import org.openbaton.catalogue.nfvo.vibeswizzard.BaseSlice;
import org.openbaton.catalogue.nfvo.viminstances.BaseVimInstance;
import org.openbaton.exceptions.AlreadyExistingException;
import org.openbaton.exceptions.BadFormatException;
import org.openbaton.exceptions.BadRequestException;
import org.openbaton.exceptions.CyclicDependenciesException;
import org.openbaton.exceptions.EntityInUseException;
import org.openbaton.exceptions.EntityUnreachableException;
import org.openbaton.exceptions.IncompatibleVNFPackage;
import org.openbaton.exceptions.NetworkServiceIntegrityException;
import org.openbaton.exceptions.NotFoundException;
import org.openbaton.exceptions.PluginException;
import org.openbaton.exceptions.VimException;
import org.openbaton.exceptions.WrongStatusException;
import org.openbaton.nfvo.core.interfaces.NetworkServiceDescriptorManagement;
import org.openbaton.nfvo.core.interfaces.NetworkServiceRecordManagement;
import org.openbaton.nfvo.core.interfaces.SliceManagement;
import org.openbaton.nfvo.core.interfaces.VimManagement;
import org.openbaton.nfvo.core.interfaces.VirtualNetworkFunctionManagement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.thavam.util.concurrent.blockingMap.BlockingHashMap;

@RestController
@RequestMapping("/api/v1/wizzard")
public class RestVIBeSWizzard {

  private Logger log = LoggerFactory.getLogger(this.getClass());

  @Autowired private VimManagement vimManagement;
  @Autowired private SliceManagement sliceManagement;
  @Autowired private NetworkServiceDescriptorManagement networkServiceDescriptorManagement;
  @Autowired private VirtualNetworkFunctionManagement vnfdManagement;
  @Autowired private NetworkServiceRecordManagement networkServiceRecordManagement;

  @Autowired private Gson gson;

  // nsdId, vnfName, ip_addr
  private BlockingHashMap<String, BlockingHashMap<String, String>> ems =
      new BlockingHashMap<String, BlockingHashMap<String, String>>();

  /**
   * @param id of the NSD
   * @param projectId if of the project
   * @param body the body json is: { "vduVimInstances":{ "vduName1":[ "viminstancename" ],
   *     "vduName2":[ "viminstancename2" ] }, "keys":[ "keyname1", "keyname2" ], "configurations":{
   *     "vnfrName1":{ "name":"conf1", "configurationParameters":[ { "confKey":"key1",
   *     "value":"value1", "description":"description1" }, { "confKey":"key2", "value":"value2",
   *     "description":"description2" } ] }, "vnfrName2":{ "name":"conf1",
   *     "configurationParameters":[ { "confKey":"key1", "value":"value1",
   *     "description":"description1" }, { "confKey":"key2", "value":"value2",
   *     "description":"description2" } ] } }, "monitoringIp" : "192.168.0.1" }
   * @return the created NSR
   * @throws InterruptedException
   * @throws ExecutionException
   * @throws VimException
   * @throws NotFoundException
   * @throws BadFormatException
   * @throws PluginException
   */
  @ApiOperation(
      value = "Deploying a Network Service Record from an existing NSD",
      notes =
          "The Network Service Record is created from the Network Service Descriptor specified in the id of the URL")
  @RequestMapping(
      value = "deploy/{id}",
      method = RequestMethod.POST,
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.ACCEPTED)
  public NetworkServiceRecord deploy(
      @PathVariable("id") String id,
      @RequestHeader(value = "project-id") String projectId,
      @RequestBody(required = false) JsonObject body)
      throws InterruptedException, ExecutionException, VimException, NotFoundException,
          BadFormatException, PluginException, BadRequestException, IOException,
          AlreadyExistingException {
    String monitoringIp = null;
    List keys = null;
    Map<String, Set<String>> vduVimInstances = null;
    Map<String, Configuration> configurations = null;
    List<String> vLink = new ArrayList<>();

    String slice = null;
    List<String> pops = new ArrayList<>();
    boolean pepEnabled = false;

    if (body != null) {
      try {
        if (body.has("monitoringIp")) {
          monitoringIp = body.get("monitoringIp").getAsString();
        }
        if (body.has("keys")) {
          keys = gson.fromJson(body.getAsJsonArray("keys"), List.class);
        }
        if (body.has("vduVimInstances")) {
          Type mapType = new TypeToken<Map<String, Set<String>>>() {}.getType();
          vduVimInstances = gson.fromJson(body.getAsJsonObject("vduVimInstances"), mapType);
        }
        if (body.has("configurations")) {
          Type mapType = new TypeToken<Map<String, Configuration>>() {}.getType();
          configurations = gson.fromJson(body.get("configurations"), mapType);
        }
        if (body.has("slice")) {
          slice = body.get("slice").getAsString();
          log.info("The slice is: " + slice);
        }

        if (body.has("link")) {
          fillInVlinkConfig(body.get("link").getAsJsonArray(), vLink);
        }
        if (body.has("vnfpep")) {
          pepEnabled = body.get("vnfpep").getAsBoolean();
        }
      } catch (Exception e) {
        throw new BadRequestException("The passed request body is not correct.");
      }
    }

    return networkServiceRecordManagement.onboard(
        id, projectId, keys, vduVimInstances, configurations, monitoringIp, vLink);
  }

  /**
   * This operation allows submitting and validating a Network Service Descriptor (NSD), including
   * any related VNFFGD and VLD.
   *
   * @param networkServiceDescriptor : the Network Service Descriptor to be created
   * @return networkServiceDescriptor: the Network Service Descriptor filled with id and values from
   *     core
   */
  @RequestMapping(
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  @ApiOperation(
      value = "Adding a Network Service Descriptor",
      notes = "POST request with Network Service Descriptor as JSON content of the request body")
  public NetworkServiceDescriptor create(
      @RequestBody @Valid NetworkServiceDescriptor networkServiceDescriptor,
      @RequestHeader(value = "project-id") String projectId)
      throws NotFoundException, BadFormatException, NetworkServiceIntegrityException,
          CyclicDependenciesException, EntityInUseException, BadRequestException, IOException,
          AlreadyExistingException, PluginException, IncompatibleVNFPackage, VimException,
          InterruptedException, EntityUnreachableException {
    NetworkServiceDescriptor nsd;

    nsd = networkServiceDescriptorManagement.onboard(networkServiceDescriptor, projectId);
    return nsd;
  }

  /**
   * Removes a list Network Service Descriptor from the NSDs Repository
   *
   * @param ids: the list of the ids
   * @throws NotFoundException
   * @throws InterruptedException
   * @throws ExecutionException
   * @throws WrongStatusException
   * @throws VimException
   */
  @RequestMapping(
      value = "ns-descriptors/multipledelete",
      method = RequestMethod.POST,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  @ApiOperation(
      value = "Removing multiple Network Service Descriptors",
      notes = "Delete Request takes a list of Network Service Descriptor ids")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void multipleDelete(
      @RequestBody @Valid List<String> ids, @RequestHeader(value = "project-id") String projectId)
      throws WrongStatusException, NotFoundException, EntityInUseException, BadRequestException {

    for (String id : ids) networkServiceDescriptorManagement.delete(id, projectId);
  }

  /**
   * Returns the list of the VNF software virtualNetworkFunctionDescriptors available
   *
   * @return List<virtualNetworkFunctionDescriptor>: The list of VNF software
   *     virtualNetworkFunctionDescriptors available
   */
  @ApiOperation(value = "Retrieving all Virtual Network Function Descriptors", notes = "")
  @RequestMapping(
      value = "/vnf-descriptors",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public List<VirtualNetworkFunctionDescriptor> findAllVNFD(
      @RequestHeader(value = "project-id") String projectId) {
    return (List<VirtualNetworkFunctionDescriptor>) vnfdManagement.queryByProjectId(projectId);
  }

  /**
   * This operation returns the list of Network Service Descriptor (NSD)
   *
   * @return List<NetworkServiceDescriptor>: the list of Network Service Descriptor stored
   */
  @ApiOperation(
      value = "Get all NSDs from a project",
      notes =
          "Returns all Network Service Descriptors onboarded in the project with the specified id")
  @RequestMapping(
      value = "/ns-descriptors",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public List<NetworkServiceDescriptor> findAllNSD(
      @RequestHeader(value = "project-id") String projectId) {
    return (List<NetworkServiceDescriptor>)
        networkServiceDescriptorManagement.queryByProjectId(projectId);
  }

  @ApiOperation(
      value = "Retrieve all registered slices",
      notes = "Returns all registered configured slices")
  @RequestMapping(
      value = "/slices",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public List<BaseSlice> listSlices() {
    log.debug(this.getClass().getName() + " issued a call for all slices");
    List<BaseSlice> slices = new ArrayList<>();
    try {
      slices = sliceManagement.retrieveFromLocalFile();
    } catch (NotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return slices;
  }

  @ApiOperation(
      value = "Retrieving all active VIMs in the project",
      notes = "Specify the project id in the url")
  @RequestMapping(
      value = "/pops",
      method = RequestMethod.GET,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public List<BaseVimInstance> listPops(@RequestHeader(value = "project-id") String projectId) {

    List<BaseVimInstance> vimInstances = new ArrayList<>();
    System.err.println("Issuing query regarding vimInstances");
    Iterator<BaseVimInstance> it = vimManagement.queryByProjectId(projectId).iterator();
    // filter only those active ones.
    for (; it.hasNext(); ) {
      BaseVimInstance vim = it.next();
      if (vim.isActive()) {
        vimInstances.add(vim);
      }
    }
    return vimInstances;
  }

  /** */
  @ApiOperation(
      value = "Dependency declares its ip address in the host network",
      notes = "Dependency declares its ip address in the host network")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @RequestMapping(
      value = "dependency/{nsdId}/vnfName/{vnf}/ipAddr/{ip}",
      method = RequestMethod.POST)
  public void dependencyPOST(
      @PathVariable("nsdId") String nsdId,
      @PathVariable("vnf") String vnfName,
      @PathVariable("ip") String ipAddr,
      @RequestHeader(value = "project-id") String projectId)
      throws NotFoundException {
    log.info("Received dependency");
    // check whether nsd is present
    NetworkServiceDescriptor nsd = networkServiceDescriptorManagement.query(nsdId, projectId);
    if (nsd == null) throw new NotFoundException("No NSD found with ID " + nsdId);
    // check whether nsd has a function named vnfName
    boolean present = nsd.getVnfd().stream().anyMatch(vnfd -> vnfd.getName().equals(vnfName));
    if (!present)
      throw new NotFoundException("No function named " + vnfName + " found in nsdId: " + nsdId);

    if (!ems.containsKey(nsdId)) {
      BlockingHashMap<String, String> map = new BlockingHashMap<>();
      ems.put(nsdId, map);
    }
    ems.get(nsdId).put(vnfName, ipAddr);
  }

  /** dependecy/{nsdId}/vnfName/{vnfName} */
  @ApiOperation(
      value = "Dependant function queries for useful info about VNF",
      notes = "Dependant function queries for useful info about VNF")
  @RequestMapping(value = "dependency/{nsdId}/vnfName/{vnf}", method = RequestMethod.GET)
  public String dependencyGET(
      @PathVariable("nsdId") String nsdId,
      @PathVariable("vnf") String vnfName,
      @RequestHeader(value = "project-id") String projectId)
      throws NotFoundException {
    log.info("Resolving dependency");
    // check whether nsd is present
    NetworkServiceDescriptor nsd = networkServiceDescriptorManagement.query(nsdId, projectId);
    if (nsd == null) throw new NotFoundException("No NSD found with ID " + nsdId);
    // check whether nsd has a function named vnfName
    boolean present = nsd.getVnfd().stream().anyMatch(vnfd -> vnfd.getName().equals(vnfName));
    if (!present)
      throw new NotFoundException("No function named " + vnfName + " found in nsdId: " + nsdId);

    BlockingHashMap<String, String> nsdEntries = null;
    try {
      nsdEntries = ems.take(nsdId);
      return nsdEntries.take(vnfName);
    } catch (InterruptedException e) {
      throw new NotFoundException("Error while retrieving dependency entry");
    }
  }

  private void fillInVlinkConfig(JsonArray data, List<String> config) {
    Iterator<JsonElement> it = data.iterator();
    String entry = null;
    String[] value = null;
    while (it.hasNext()) {
      entry = it.next().getAsString();
      config.add(entry);
      log.info("vLink entry: " + entry);
    }
  }
}
