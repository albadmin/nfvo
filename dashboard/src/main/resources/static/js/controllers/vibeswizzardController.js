/*
 * Copyright (c) 2016 Open Baton (http://www.openbaton.org)
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

var app = angular.module('app').controller('vibeswizzardCtrl', function ($scope, $http, $compile, $cookieStore, $filter, $routeParams, http, serviceAPI, topologiesAPI, AuthService, $location, $interval, NgTableParams) {
	var baseURL = $cookieStore.get('URL') + "/api/v1";
    var baseUrl = $cookieStore.get('URL') + "/api/v1/wizzard/";
	$scope.alerts = [];
    var urlSlices = baseUrl + 'slices/';
    $scope.slices = [];
    
    var urlInterPoPLinks = baseUrl + 'interPoPLinks/';
    $scope.interPoPLinks = [];
    $scope.links = [];//actually selected links   
    
    var urlPoPs = baseUrl + 'pops/';
	$scope.pops = [];

	$scope.selectionInterPoPLink = {};
	$scope.selectionInterPoPLink.ids = {};

	//call functs.
    $scope.selectionSlice = {};
    $scope.selectionSlice.ids = {};
	loadSlices();
	loadInterPoPLinks();
    loadPoPs();

    var urlNSD = baseUrl + 'ns-descriptors/';  
    var paginationNSD = [];
    $scope.selectionNSD = {};
    $scope.selectionNSD.ids = {};
    $scope.nsdescriptors = [];    
	loadNSDs();
    $scope.NSDInstanceJson = {};

    $scope.keys = [];    
	$scope.nsdToSend = {};
    var urlDeploy = baseUrl + 'deploy/';  
    $scope.textTopologyJson = '';
    $scope.file = '';	
    
	$scope.confOk = false;
    
    $scope.loadUploadNSDDialog = function () {
		$('#modalCreateNSDUploadjson').modal('show');
        $scope.NSDInstanceJson = response;
        $scope.selectionNSD = {};
    	$scope.selectionNSD.ids = {};
    };

    $('#modalCreateNSDUploadjson').on('hidden.bs.modal', function () {
        $(this).find("input,textarea,select").val('').end();
    });

    $scope.setFile = function (element) {
        $scope.$apply(function ($scope) {
            var f = element.files[0];
            if (f) {
                var r = new FileReader();
                r.onload = function (element) {
                    var contents = element.target.result;
                    $scope.file = contents;
                };
                r.readAsText(f);
            } else {
                alert("Failed to load file");
            }
        });
    };	
	
	
	$scope.sendFile = function (textTopologyJson) {

        $('.modal').modal('hide');
        var postNSD;
        var sendOk = true;
        var type = 'topology';
        if ($scope.file !== '') {
            postNSD = $scope.file;
            if (postNSD.charAt(0) === '<')
                type = 'definitions';
            document.getElementById("formJson").reset();
        }
        else if (textTopologyJson !== '') {
            postNSD = textTopologyJson;
        }

        else {
            alert('Problem with NSD');
            sendOk = false;

        }
        if (sendOk) {
            if (type === 'topology') {

                http.post(baseUrl, postNSD)
                    .success(function (response) {
                        showOk('Network Service Descriptors stored!');
                        loadNSDs();
                    })
                    .error(function (data, status) {
                        showError(data, status);
                    });
            }

            else {
                http.postXML('/api/rest/tosca/v2/definitions/', postNSD)
                    .success(function (response) {
                        showOk('Definition created!');
                        loadTable();
                        //window.setTimeout($scope.cleanModal(), 3000);
                    })
                    .error(function (data, status) {
                        showError(data, status);
                    });
            }
        }

        $scope.toggle = false;
        $scope.textTopologyJson = '';
        $scope.file = '';
        $scope.tableParamspaginationNSD.reload();
        $('#modalCreateNSDUploadjson').modal('hide');
    };
    

    function loadNSDs() {
		http.get(urlNSD)
        	.success(function (response, status) {
            	$scope.nsdescriptors = response;
                $scope.tableParamspaginationNSD.reload();
				//XXX: computed always - put this funct. in place where we first get the descriptors
				computeAssociativeDStructures();
            })
            .error(function (data, status) {
            	showError(data, status);
			});
    }

    $scope.nsd = {};    
	$scope.vnfdnames = [];
    $scope.loadNSD = function (nsdId) {
		http.get(urlNSD + nsdId)
        	.success(function (response, status) {
        		$scope.launchConfiguration = null;
        		$scope.launchConfiguration = {"configurations": {}};
				$scope.vnfdnames = [];    			
            	$scope.nsd = response;
            	console.log($scope.nsd);
        		$scope.nsd.vnfd.map(function (vnfd) {
            	$scope.vnfdnames.push(vnfd.name);
	            	if (vnfd.configurations === undefined || vnfd.configurations.length < 1) {
	                	$scope.launchConfiguration.configurations[vnfd.name] = {name: "", configurationParameters: []};
	            	} else {
	                	$scope.launchConfiguration.configurations[vnfd.name] = angular.copy(vnfd.configurations);
	            	}
        		});

        		$scope.loadVnfdTabs();       
            })
            .error(function (data, status) {
            	showError(data, status);
			});
    }    


	$scope.tabs = [];    
    $scope.loadVnfdTabs = function () {
        $scope.tabs = [];
        var i;
        for (i = 0; i < $scope.nsd.vnfd.length; i++) {
            newVNFD = {"vnfdname": $scope.nsd.vnfd[i].name, "vim": [], "vduLevel": false, "vdu": []};
            console.log(newVNFD);

            var tab = {};
            tab['id'] = i;
            tab['title'] = $scope.nsd.vnfd[i].name;
            tab['active'] = true;
            tab['disabled'] = false;
            tab['vnfd'] = $scope.nsd.vnfd[i];

            $scope.tabs.push(tab);
        }
    };
    
	$scope.nsdIdNameAssoc = [];
	$scope.nsdIdVNFDIdAssoc = [];	
	$scope.nsdIdVimAssoc = [];
	$scope.nfdIdNameAssoc = [];
	$scope.vnfdLinkAssoc = [];    

    function computeAssociativeDStructures() {
		var descriptors = JSON.parse(JSON.stringify( $scope.nsdescriptors));
		for(var nsd in descriptors) {	
			//create assoc: nsdId-nsdName
//			console.log(descriptors[nsd]);
			var id = descriptors[nsd]['id'];
//			console.log('current id: ' + id);
			$scope.nsdIdNameAssoc[id] = descriptors[nsd]['name'];
			var vnfd = descriptors[nsd]['vnfd'];
			//create assoc: vnfd-link
			for(var vnfd_nr=0; vnfd_nr < vnfd.length; vnfd_nr++) {
				var nf = descriptors[nsd]['vnfd'][vnfd_nr];
				var nfId = nf['id'];
				$scope.nfdIdNameAssoc[nfId] = nf['name'];
				$scope.nsdIdVNFDIdAssoc.push({
					nsdId: id,
					vnfdId: nfId
				});
				var vdu = nf['vdu'];
				//XXX: only single 'vdu' deployment supported
				var vimInstanceName = vdu[0]['vimInstanceName'];
				$scope.vnfdLinkAssoc[nfId] = vimInstanceName;
				$scope.nsdIdVimAssoc.push({
					nsdId: id,					
					vim: vimInstanceName
				});
			}
		}                    
    }
    
	function showError(data, status) {
		if(status === 500) {
            $scope.alerts.push({
                type: 'danger',
                msg: 'An error occurred and could not be handled properly, please, report to us and we will fix it as soon as possible'
            });
        } else {

            $scope.alerts.push({
                type: 'danger',
                msg: data.message + " Code: " + status
            });
        }

        $('.modal').modal('hide');
        if (status === 401) {
            console.log(status + ' Status unauthorized')
            AuthService.logout();
        }
    }

    function showOk(msg) {
        $scope.alerts.push({type: 'success', msg: msg});

        //loadTable();
        $('.modal').modal('hide');
    }

    $scope.closeAlert = function (index) {
        $scope.alerts.splice(index, 1);
    };

    function loadSlices() {
    	http.get(urlSlices)
        	.success(function (response) {
            	$scope.slices = response;
			})
			.error(function (data, status) {
                showError(status, data);
			});
	}
	
	function loadInterPoPLinks() {
    	http.get(urlInterPoPLinks)
        	.success(function (response) {
            	$scope.interPoPLinks = response;
			})
			.error(function (data, status) {
                showError(status, data);
			});
	}
    
    
    function loadPoPs() {
		http.get(urlPoPs)
        	.success(function (response, status) {
            	$scope.pops = response;

			})
            .error(function (data, status) {
            	showError(data, status);
			});
    }
    
	var paginationNSD = [];
    $scope.tableParamspaginationNSD = new NgTableParams({
            page: 1,
            count: 10,
            sorting: {
                name: 'asc'     // initial sorting
            },
            filter: {name: ""}
        },
        {
            counts: [],
            total: paginationNSD.length,
            getData: function (params) {
                paginationNSD = params.sorting() ? $filter('orderBy')($scope.nsdescriptors, params.orderBy()) : $scope.nsdescriptors;
                paginationNSD = params.filter() ? $filter('filter')(paginationNSD, params.filter()) : paginationNSD;
                $scope.tableParamspaginationNSD.total(paginationNSD.length);
                paginationNSD = paginationNSD.slice((params.page() - 1) * params.count(), params.page() * params.count());

                return paginationNSD;
            }
        });

        
    /* -- multiple delete functions Start -- */
    $scope.multipleDeleteReq = function () {
        var ids = [];
        angular.forEach($scope.selectionNSD.ids, function (value, k) {
            if (value) {
                ids.push(k);
            }
        });

        http.post(urlNSD + 'multipledelete', ids)
            .success(function (response) {
                showOk('Items with id: ' + ids.toString() + ' deleted.');
                loadNSDs();
            })
            .error(function (response, status) {
                showError(response, status);
            });

        $scope.multipleDelete = false;
        $scope.selectionNSD.ids = {};
        $scope.selectionNSD = {};
    };

    $scope.main = {checkbox: false};
    $scope.$watch('main', function (newValue, oldValue) {
        angular.forEach($scope.selectionNSD.ids, function (value, k) {
            $scope.selectionNSD.ids[k] = newValue.checkbox;
        });
    }, true);

    $scope.$watch('selectionNSD', function (newValue, oldValue) {

        var keepGoing = true;
        angular.forEach($scope.selectionNSD.ids, function (value, k) {
            if (keepGoing) {
                if ($scope.selectionNSD.ids[k]) {
                    $scope.multipleDelete = false;
                    keepGoing = false;
                }
                else {
                    $scope.multipleDelete = true;
                }
            }

        });
        if (keepGoing)
            $scope.mainCheckbox = false;
    }, true);

    $scope.multipleDelete = true;
    $scope.selectionNSD = {};
    $scope.selectionNSD.ids = {};

    /* -- multiple delete functions END -- */         

    /* -- deploy functions INIT  -- */        
     
	$scope.launchKeys = [];
	$scope.launchObj = {};
	$scope.launchPops = {};
	$scope.launchConfiguration = {"configurations": {}};
	$scope.monitoringIp = undefined;
        
    function removePriorConf() {
    	//selections
    	$scope.selectionNSD = {};
    	$scope.selectionNSD.ids = {};
    	$scope.selectionSlice = {};
    	$scope.selectionSlice.ids = {};   
		$scope.NSDInstanceJson = {};
    	$scope.selectionInterPoPLink = {};
		$scope.selectionInterPoPLink.ids = {};

		//configurations		
		$scope.launchKeys = [];
        $scope.launchObj = {};
        $scope.launchConfiguration = {"configurations": {}};
        $scope.monitoringIp = undefined;
		$scope.nsdToSend = undefined;
		$scope.links = [];
		
    	$scope.vimsOfInterest = {};
    	$scope.vimsOfInterest.vim = [];		
        $scope.confOk = false;
		$scope.tableParamspaginationNSD.reload();        
    }   

    function loadKeys() {
        http.get(baseURL + '/keys')
            .success(function (response) {
                $scope.keys = response;
            })
            .error(function (data, status) {
                showError(data, status);
            });

    }

	function computeSelectedNSD() {
        var nsdIds = [];
        angular.forEach($scope.selectionNSD.ids, function (truefalse, nsdId) {
            if (truefalse) {
				nsdIds = nsdId;	
            }
        });
                
        $scope.nsdToSend = nsdIds;	
	}    
			
    function env() {
    
		computeSelectedNSD();

        loadKeys();

		//monitoringIp configuration
        http.get($cookieStore.get('URL') + '/env')
            .success(function (response) {
                monitoringIp = response['applicationConfig: [file:/etc/openbaton/openbaton-nfvo.properties]']['nfvo.monitoring.ip'];
                if (monitoringIp !== undefined && monitoringIp.indexOf(':') > -1) {
                    $scope.monitoringIp = monitoringIp.split(":")[0];
                    $scope.monitoringPort = monitoringIp.split(":")[1];
                } else {
                    $scope.monitoringIp = monitoringIp;
                    $scope.monitoringPort = "";
                }
            })
            .error(function (response, status) {
                showError(response, status);
            });

        angular.forEach($scope.selectionSlice.ids, function (truefalse, name) {
            if (truefalse) {
				$scope.launchObj.slice = name;
            }else {
            }
        });		   
        
    }
    
	function confAllSelected() {
	
		var nsdSelected = false;
        angular.forEach($scope.selectionNSD.ids, function (truefalse, nsdId) {
            if (truefalse) {
				nsdSelected = true;
            }
        });
        	
		var sliceSelected = false;        	
        angular.forEach($scope.selectionSlice.ids, function (truefalse, name) {
            if (truefalse) {
				sliceSelected = true;
            }else {
            	//console.log('deploying on slice: ' + sliceName);
            }
        });		   
        
//		var interPoPLinkSelected = false;        	
//        angular.forEach($scope.selectionInterPoPLink.ids, function (truefalse, name) {
//            if (truefalse) {
//				interPoPLinkSelected = true;
//            }else {
//            }
//        });		           
        		
		$scope.confOk = nsdSelected && sliceSelected; // && interPoPLinkSelected;
	}
         
    $scope.deploy = function () {
		confAllSelected();
		if(!($scope.confOk)) {
			showError('400', 'Not all necessary parameters have been selected');
			return;
		}
		env();
		
		//add configured vlds to configuration envinronment
        $scope.launchObj.link = [];
        for (i = 0; i < $scope.links.length; i++) 
        {      
			$scope.launchObj.link.push(
				$scope.links[i]['netName']
			);
		}

		$scope.launchObj.configurations = {};
        $scope.launchObj.configurations = $scope.launchConfiguration.configurations;
		
        http.post(urlDeploy + $scope.nsdToSend, $scope.launchObj)
            .success(function (response) {
                showOk("Created Network Service Record from Descriptor with id: \<a href=\'\#nsrecords\'>" + $scope.nsdToSend + "<\/a>");
            })
            .error(function (data, status) {
                showError(data, status);
            });

        removePriorConf();
    };   
    
    /* -- deploy functions END  -- */            

	$scope.onClickNSD = function(nsdId) {
		$scope.nsdToSend = nsdId;//first time check, otherwise refined later
        angular.forEach($scope.selectionNSD.ids, function (truefalse, id) {
            if (nsdId !== id) {
				$scope.selectionNSD.ids[id] = false;
                $scope.tableParamspaginationNSD.reload();
            }else {
//            	console.log('deploying: ' + nsdId);
            }

		computeSelectedNSD();
        });
	}
	
	$scope.onClickSlice = function(sliceName) {
        angular.forEach($scope.selectionSlice.ids, function (truefalse, name) {
            if (sliceName !== name) {
				$scope.selectionSlice.ids[name] = false;	
            }else {
            	console.log('deploying on slice: ' + sliceName);
            }
        });
	}	

	$scope.onClickInterPoPLink = function(link) {
        angular.forEach($scope.selectionInterPoPLink.ids, function (truefalse, entry) 
        {
           	if(entry == link)//this triggered the event 
           	{
           		if(!truefalse) 
           		{//uncheck
					$scope.selectionInterPoPLink.ids[entry] = false;
					for (i = 0; i < $scope.links.length; i++) 
					{
						var inspect = $scope.links[i];
						if(inspect['netName'] == link) {							
							$scope.links.splice(i, 1);
						}
					}
				}else {//check
					$scope.links.push({
		    	        netName: link			
					});					
				}
			}
        });
		console.log('selected links are: ' + $scope.links);
	}		
});
