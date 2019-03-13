package com.nzoth.testgraph;

public class GraphItem {
    private long[] dateList;
    private long[][] graphList;
    private String[] graphColorList;
    private String[] graphTitleList;

    public long[] getDateList() {
        return dateList;
    }

    public void setDateList(long[] dateList) {
        this.dateList = dateList;
    }

    public long[][] getGraphList() {
        return graphList;
    }

    public void setGraphList(long[][] graphList) {
        this.graphList = graphList;
    }

    public String[] getGraphColorList() {
        return graphColorList;
    }

    public void setGraphColorList(String[] graphColorList) {
        this.graphColorList = graphColorList;
    }

    public String[] getGraphTitleList() {
        return graphTitleList;
    }

    public void setGraphTitleList(String[] graphTitleList) {
        this.graphTitleList = graphTitleList;
    }
}
