package com.nzoth.testgraph;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.nzoth.testgraph.models.GraphModel;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String DATA_TYPE = "x";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Gson gson = new Gson();
        try {
            JsonReader reader = gson.newJsonReader(new InputStreamReader(getAssets().open("chart_data.json")));
            GraphModel[] modelList = gson.fromJson(reader, GraphModel[].class);
            List<GraphItem> itemList = new ArrayList<>();

            for (GraphModel model : modelList) {
                GraphItem graphItem = new GraphItem();

                int counter = 0;
                int graphSize = model.names.size();
                String[] colors = new String[graphSize];
                String[] titles = new String[graphSize];
                long[][] graphs = new long[graphSize][];

                for (List<String> graphList : model.columns) {
                    String type = graphList.get(0);
                    long[] list = convertToLongList(graphList);
                    if (type.equalsIgnoreCase(DATA_TYPE)) {
                        graphItem.setDateList(list);
                    } else {
                        colors[counter] = model.colors.get(type);
                        titles[counter] = String.valueOf(model.names.get(type));
                        graphs[counter] = list;
                        counter++;
                    }
                }

                graphItem.setGraphColorList(colors);
                graphItem.setGraphTitleList(titles);
                graphItem.setGraphList(graphs);

                itemList.add(graphItem);
            }


            GraphView graphView = new GraphView(this, itemList.get(0));
            graphView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            setContentView(graphView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private long[] convertToLongList(List<String> stringList) {
        long[] result = new long[stringList.size() - 1];
        for (int i = result.length; i > 0; i--) {
            result[result.length - i] = Long.parseLong(stringList.get(i));
        }
        return result;
    }
}
