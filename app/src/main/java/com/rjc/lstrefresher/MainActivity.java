package com.rjc.lstrefresher;

import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.rjc.lstrefresher.App.MyApplication;
import com.rjc.lstrefresher.Helper.Movie;
import com.rjc.lstrefresher.Helper.SwipeListAdapter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

public class MainActivity extends AppCompatActivity  implements SwipeRefreshLayout.OnRefreshListener{
    private String TAG = MainActivity.class.getSimpleName();

    private String URL_TOP_250 = "http://api.androidhive.info/json/imdb_top_250.php?offset=";

    private SwipeRefreshLayout swipeRefreshLayout;
    private ListView listView;
    private SwipeListAdapter adapter;
    private List<Movie> movieList;
    // initially offset will be 0, later will be updated while parsing the json
    private int offSet = 0;
    View vrow;
    private int xy = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        listView = (ListView) findViewById(R.id.listView);

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);

        movieList = new ArrayList<>();
        adapter = new SwipeListAdapter(this, movieList);
        listView.setAdapter(adapter);
        swipeRefreshLayout.setOnRefreshListener(this);
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {

            private int lastVisibleItem;
            private int totalItemCount;
            private boolean isEndOfList;
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                this.totalItemCount = totalItemCount;
                this.lastVisibleItem = firstVisibleItem + visibleItemCount - 1;
                // prevent checking on short lists
                if (totalItemCount > visibleItemCount)
                    checkEndOfList();
            }

            private synchronized void checkEndOfList() {
                // trigger after 2nd to last item
                if (lastVisibleItem >= (totalItemCount - 2)) {
                    if (!isEndOfList) {
                        // LOAD MORE ITEMS HERE!
                        vrow =   listView.getChildAt(listView.getChildCount()-2);
                        vrow.findViewById(R.id.rowSpinner).setVisibility(View.VISIBLE);
                        fetchMovies();
                    }
                    isEndOfList = true;
                } else {
                    isEndOfList = false;
                }
            }
        });

        /**
         * Showing Swipe Refresh animation on activity create
         * As animation won't start on onCreate, post runnable is used
         */
        swipeRefreshLayout.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        swipeRefreshLayout.setRefreshing(true);

                                        fetchMovies();
                                    }
                                }
        );

    }

    @Override
    public void onRefresh() {
           fetchMovies();
    }


    /**
     * Fetching movies json by making http call
     */
    private void fetchMovies() {

        // showing refresh animation before making http call
       // swipeRefreshLayout.setRefreshing(true);

        // appending offset to url
        String url = URL_TOP_250 + offSet;

        // Volley's json array request object
        final JsonArrayRequest req = new JsonArrayRequest(url,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        Log.d(TAG, response.toString());

                        if (response.length() > 0) {

                            // looping through json and adding to movies list
                            for (int i = 0; i < response.length(); i++) {
                                try {
                                    JSONObject movieObj = response.getJSONObject(i);

                                    int rank = movieObj.getInt("rank");
                                    String title = movieObj.getString("title");

                                    Movie m = new Movie(rank, title);

                                    movieList.add(m);

                                    // updating offset value to highest value
                                    if (rank >= offSet)
                                        offSet = rank;

                                } catch (JSONException e) {
                                    Log.e(TAG, "JSON Parsing error: " + e.getMessage());
                                }
                            }

                            adapter.notifyDataSetChanged();
                        }

                        // stopping swipe refresh
                        if(vrow!=null){
                            vrow.findViewById(R.id.rowSpinner).setVisibility(View.GONE);
                        }
                      if(xy==0){
                          swipeRefreshLayout.setRefreshing(false);
                          xy++;
                      }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Server Error: " + error.getMessage());

                Toast.makeText(getApplicationContext(), error.getMessage(), Toast.LENGTH_LONG).show();

                // stopping swipe refresh
               // swipeRefreshLayout.setRefreshing(false);
            }
        });

        // Adding request to request queue
        MyApplication.getInstance().addToRequestQueue(req);
    }

}
