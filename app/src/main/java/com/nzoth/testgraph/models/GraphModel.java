package com.nzoth.testgraph.models;

import java.util.HashMap;
import java.util.List;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class GraphModel {

    @SerializedName("columns")
    @Expose
    public List<List<String>> columns = null;

    @SerializedName("types")
    @Expose
    public HashMap<String, String> types;

    @SerializedName("names")
    @Expose
    public HashMap<String, String>  names;

    @SerializedName("colors")
    @Expose
    public HashMap<String, String>  colors;

}
