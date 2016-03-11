package com.giovanniterlingen.windesheim;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * A scheduler app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
public class ScheduleFragment extends ListFragment implements SwipeRefreshLayout.OnRefreshListener {

    private static String componentId;
    private static int type;
    private Date date;
    private Context context;
    private ScheduleAdapter adapter;
    private DateFormat simpleDateFormat;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ViewGroup viewGroup;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        componentId = getArguments().getString("componentId");
        type = getArguments().getInt("type");
        date = (Date) getArguments().getSerializable("Date");
        context = getActivity();

        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Cursor scheduleDay = ApplicationLoader.scheduleDatabase.getLessons(simpleDateFormat.format(date), componentId);
        if (scheduleDay != null && scheduleDay.getCount() > 0) {
            adapter = new ScheduleAdapter(context, scheduleDay, 0);
            setListAdapter(adapter);
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            int month = calendar.get(Calendar.MONTH);
            String monthString = null;
            switch (month) {
                case 0:
                    monthString = "januari";
                    break;
                case 1:
                    monthString = "februari";
                    break;
                case 2:
                    monthString = "maart";
                    break;
                case 3:
                    monthString = "april";
                    break;
                case 4:
                    monthString = "mei";
                    break;
                case 5:
                    monthString = "juni";
                    break;
                case 6:
                    monthString = "juli";
                    break;
                case 7:
                    monthString = "augustus";
                    break;
                case 8:
                    monthString = "september";
                    break;
                case 9:
                    monthString = "oktober";
                    break;
                case 10:
                    monthString = "november";
                    break;
                case 11:
                    monthString = "december";
            }
            ((ScheduleActivity) context).getSupportActionBar().setTitle(simpleDateFormat.format(date) + " " + monthString);
            if (getListAdapter() == null) {
                new ScheduleFetcher().execute();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {
        viewGroup = (ViewGroup) inflater.inflate(R.layout.fragment_schedule, container, false);
        swipeRefreshLayout = (SwipeRefreshLayout) viewGroup.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimaryText, R.color.colorPrimary);
        if (getListAdapter() == null) {
            TextView emptyTextView = (TextView) viewGroup.findViewById(R.id.schedule_not_found);
            emptyTextView.setVisibility(View.VISIBLE);
        }
        return viewGroup;
    }

    private void alertConnectionProblem() {
        ApplicationLoader.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(context)
                        .setTitle("Probleem met verbinden!")
                        .setMessage("De gegevens konden niet worden opgevraagd. Controleer je internetverbinding en probeer het opnieuw.")
                        .setIcon(R.drawable.ic_launcher)
                        .setPositiveButton("Verbinden",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        new ScheduleFetcher().execute();
                                        dialog.cancel();
                                    }
                                })
                        .setNegativeButton("Annuleren", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        }).show();
            }
        });
    }

    @Override
    public void onRefresh() {
        new ScheduleFetcher().execute();
    }

    private class ScheduleFetcher extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (swipeRefreshLayout != null && adapter == null) {
                swipeRefreshLayout.setRefreshing(true);
            }
        }

        @Override
        protected Void doInBackground(Void... param) {
            try {
                ScheduleHandler.saveSchedule(ScheduleHandler.getScheduleFromServer(componentId, date, type), date, componentId, type);
            } catch (Exception e) {
                alertConnectionProblem();
            }
            final Cursor scheduleDay = ApplicationLoader.scheduleDatabase.getLessons(simpleDateFormat.format(date), componentId);
            if (adapter == null) {
                adapter = new ScheduleAdapter(context, scheduleDay, 0);
            } else {
                ApplicationLoader.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.changeCursor(scheduleDay);
                    }
                });
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void param) {
            super.onPostExecute(param);
            if (adapter != null && !adapter.isEmpty()) {
                TextView emptyTextView = (TextView) viewGroup.findViewById(R.id.schedule_not_found);
                emptyTextView.setVisibility(View.GONE);
                if (getListAdapter() == null) {
                    setListAdapter(adapter);
                }
            }
            if (swipeRefreshLayout != null && swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
        }
    }
}