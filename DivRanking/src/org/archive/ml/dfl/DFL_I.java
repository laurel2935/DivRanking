package org.archive.ml.dfl;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.archive.ml.ufl.CFL.UFLMode;

/**
 * I: implicit, i.e, Customer and Facility are the same set of elements
 * **/

public class DFL_I extends DFL {
	// for the case of C is the same as F    
    private Integer _cfID = 0;
    protected Map<String, Integer> _cfIDMapper = new TreeMap<String, Integer>();
    protected Map<Integer, String> _cfIDRevMapper = new TreeMap<Integer, String>();
    
    
    ////message variables
    //Eta_{MM}
    //V_{1M}
    //Alpha_{MM}

    ////
    protected Integer getCustomerID(String cName) {
    	if(_cfIDMapper.containsKey(cName)){
			return _cfIDMapper.get(cName);
		}else{
			Integer id = _cfID;
			_cfIDMapper.put(cName, id);
			_cfIDRevMapper.put(id, cName);
			_cfID++;
			return id;
		}       
    }
    protected String getCustomerName(Integer cID){
    	return this._cfIDRevMapper.get(cID);
    }
    protected Integer getFacilityID(String fName){
    	if(_cfIDMapper.containsKey(fName)){
			return _cfIDMapper.get(fName);
		}else{
			Integer id = _cfID;
			_cfIDMapper.put(fName, id);
			_cfIDRevMapper.put(id, fName);
			_cfID++;
			return id;
		}   	
    }
    protected String getFacilityName(Integer fID){
    	return this._cfIDRevMapper.get(fID);
    }
}
