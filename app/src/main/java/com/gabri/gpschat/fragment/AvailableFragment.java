package com.gabri.gpschat.fragment;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gabri.gpschat.MainActivity;
import com.gabri.gpschat.R;
import com.gabri.gpschat.activity.ChatMainActivity;
import com.gabri.gpschat.activity.LoginActivity;
import com.gabri.gpschat.adapts.AvailableUserAdapter;
import com.gabri.gpschat.model.RecentModel;
import com.gabri.gpschat.model.UserModel;
import com.gabri.gpschat.utility.Constants;
import com.gabri.gpschat.utility.GPSTracker;
import com.gabri.gpschat.utility.Utils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import cc.cloudist.acplibrary.ACProgressConstant;
import cc.cloudist.acplibrary.ACProgressFlower;

/**
 * A simple {@link Fragment} subclass.
 */
public class AvailableFragment extends Fragment {

    View avaiable_view;
    TextView alert_textview;
    ListView availble_userlistview;
    AvailableUserAdapter adapter;
    private List<UserModel> userlist;
    ACProgressFlower dialog;
    String currentUserId,fragment_string,notification_string;
    GPSTracker gpsTracker;
    Location mycurrent_location;
    SharedPreferences cookies_string;
    SharedPreferences.Editor editor;
    public AvailableFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        avaiable_view=inflater.inflate(R.layout.fragment_available, container, false);
        cookies_string = getActivity().getSharedPreferences(Constants.KEY_COOKIES, Context.MODE_PRIVATE);
        editor = cookies_string.edit();
        fragment_string=Utils.getFromPref(Constants.KEY_FRAGMENTFLAG,getActivity());
        notification_string=Utils.getFromPref(Constants.KEY_SEND_COUND,getActivity());
        dialog = new ACProgressFlower.Builder(getActivity())
                .direction(ACProgressConstant.DIRECT_CLOCKWISE)
                .themeColor(Color.WHITE)
                .text("Reading")
                .fadeColor(Color.DKGRAY).build();

        currentUserId = Utils.getFromPref(Constants.USER_ID, getContext());
        availble_userlistview = (ListView)avaiable_view.findViewById(R.id.avuser_listView);
        load_user();
        return avaiable_view;
    }
    public void load_user(){

        gpsTracker=new GPSTracker(getActivity());
        mycurrent_location=new Location("A");
        mycurrent_location.setLatitude(gpsTracker.getLatitude());
        mycurrent_location.setLongitude(gpsTracker.getLongitude());

//        dialog.show();
        FirebaseDatabase.getInstance().getReference(Constants.USER_TABLE).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
//                dialog.dismiss();
                userlist = new ArrayList<UserModel>();
                for (DataSnapshot post : dataSnapshot.getChildren())
                {
                    UserModel model = post.getValue(UserModel.class);
                    Location other_locations=new Location("B");
                    other_locations.setLongitude(Double.parseDouble(model.getLongitude()));
                    other_locations.setLatitude(Double.parseDouble(model.getLatitude()));
                    float distance=mycurrent_location.distanceTo(other_locations);
                    Log.d("distance",Float.toString(distance));
                    if (distance<=300){
                        if (model.getObjectId().equals(cookies_string.getString(Constants.USER_ID,null)))
                        {

                        }
                        else {
                            userlist.add(model);
                        }

                    }
//                    userlist.add(model);

                }

                adapter = new AvailableUserAdapter(getActivity(), userlist);
                availble_userlistview.setAdapter(adapter);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(getActivity(), "User null", Toast.LENGTH_SHORT).show();
            }
        });

        availble_userlistview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                UserModel model = adapter.getItem(position);
//                Toast.makeText(getActivity(), model.getObjectId(), Toast.LENGTH_LONG).show();
                String uid = model.getObjectId();
                CheckExists(uid);
            }
        });




    }

    private void CheckExists(final String uid) {
//        String currentUserId = Utils.getFromPref(Constants.USER_ID, getContext());
        Log.d("current", currentUserId);
        if (currentUserId.equals("")) return;
        Query query = FirebaseDatabase.getInstance().getReference(Constants.RECENT_TABLE).orderByChild("userId").equalTo(currentUserId);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean ok = false;
                RecentModel tempModel = new RecentModel();
                for (DataSnapshot post : dataSnapshot.getChildren()) {
                    RecentModel model = post.getValue(RecentModel.class);

                    if (model != null && model.getOtherUser().equals(uid))
                    {
                        ok = true;
                        tempModel = model;
                        break;
                    }
                }
                if (ok == false)
                {
                    Log.d("ok", "null");
                    createChatRoom(uid);
                }
                else
                {
//                    Toast.makeText(getActivity(), "Already exists", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getActivity(), ChatMainActivity.class);
                    intent.putExtra("model", tempModel);
                    startActivity(intent);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("database", databaseError.toString());
            }
        });

    }

    private void createChatRoom(String uid) {

        String groupId = FirebaseDatabase.getInstance().getReference(Constants.MESSAGE_TABLE).push().getKey();
        String recentKey1 = FirebaseDatabase.getInstance().getReference(Constants.RECENT_TABLE).push().getKey();
        String recentKey2 = FirebaseDatabase.getInstance().getReference(Constants.RECENT_TABLE).push().getKey();
        String date = Calendar.getInstance().getTime().getTime() + "";
        RecentModel myModel = new RecentModel();
        myModel.setCreateAt(date);
        myModel.setUpdatedAt(date);
        myModel.setGroupId(groupId);
        myModel.setLastDate("");
        myModel.setLastMessage("");
        myModel.setObjectId(recentKey1);
        myModel.setOtherRecentId(recentKey2);
        myModel.setOtherUser(uid);
        myModel.setAvaiable_status("1");
        myModel.setUnread_count_message("0");
        myModel.setUserId(currentUserId);

        RecentModel otherModel = new RecentModel();
        otherModel.setCreateAt(date);
        otherModel.setUpdatedAt(date);
        otherModel.setGroupId(groupId);
        otherModel.setLastDate("");
        otherModel.setLastMessage("");
        otherModel.setObjectId(recentKey2);
        otherModel.setOtherRecentId(recentKey1);
        otherModel.setAvaiable_status("0");
        otherModel.setUnread_count_message("0");
        otherModel.setOtherUser(currentUserId);
        if (uid.equals(currentUserId))    otherModel.setUserId(uid + "_");
        else otherModel.setUserId(uid);

        FirebaseDatabase.getInstance().getReference(Constants.RECENT_TABLE).child(recentKey1).setValue(myModel.getHashMap());
        FirebaseDatabase.getInstance().getReference(Constants.RECENT_TABLE).child(recentKey2).setValue(otherModel.getHashMap());

        Intent intent = new Intent(getActivity(), ChatMainActivity.class);
        intent.putExtra("model", myModel);
        intent.putExtra("othermodel",otherModel);
        startActivity(intent);
    }

}
