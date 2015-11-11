package org.archive.ml.dfl;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * E: explicit, i.e, Customer and Facility are not the same set of elements
 * **/

public class DFL_E extends DFL {
	//set of node identifier, i.e., names
	protected Collection<String> _cNodeNames;
	protected Collection<String> _fNodeNames; 
	
    //for recording inner id
	protected Integer _customerID = 0;
    protected Map<String, Integer> _customerIDMapper = new TreeMap<String, Integer>();
    protected Map<Integer, String> _customerIDRevMapper = new TreeMap<Integer, String>();
    
    protected Integer _facilityID = 0;
    protected Map<String, Integer> _facilityIDMapper = new TreeMap<String, Integer>();
    protected Map<Integer, String> _facilityIDRevMapper = new TreeMap<Integer, String>();
    
    
    ////message variables
    //N: number of customers	0<=i<N
  	//M: number of potential facilities	0<=j<M
    //Eta_{NM}
    //V_{1M}
    //Alpha_{NM}
    
    ////
    protected Integer getCustomerID(String cName) {
    	if (_customerIDMapper.containsKey(cName)) {
            return _customerIDMapper.get(cName);
        } else {
            Integer id = _customerID;
            _customerIDMapper.put(cName, id);
            _customerIDRevMapper.put(id, cName);
            _customerID++;
            return id;
        }       
    }
    protected String getCustomerName(Integer cID){
    	return this._customerIDRevMapper.get(cID);
    }
    protected Integer getFacilityID(String fName){
    	if(_facilityIDMapper.containsKey(fName)){
    		return _facilityIDMapper.get(fName);
    	}else{
    		Integer id = _facilityID;
    		_facilityIDMapper.put(fName, id);
    		_facilityIDRevMapper.put(id, fName);
    		_facilityID++;
    		return id;
    	}    	
    }
    protected String getFacilityName(Integer fID){
    	return this._facilityIDRevMapper.get(fID);
    }

}
