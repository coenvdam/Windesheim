package com.giovanniterlingen.windesheim;

import android.database.Cursor;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * A schedule app for Windesheim students
 *
 * @author Giovanni Terlingen
 */
class ScheduleHandler {

    public static String getListFromServer(int type) throws Exception {
        StringBuilder stringBuffer = new StringBuilder("");
        URL urlLink = new URL("https://roosters.windesheim.nl/WebUntis/Timetable.do?ajaxCommand=getPageConfig&type=" + type);
        HttpURLConnection connection = (HttpURLConnection) urlLink.openConnection();
        connection.setConnectTimeout(10000);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Cookie", "schoolname=\"_V2luZGVzaGVpbQ==\"");
        connection.setDoInput(true);
        connection.connect();

        InputStream inputStream = connection.getInputStream();

        BufferedReader rd = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = rd.readLine()) != null) {
            stringBuffer.append(line);
        }

        return stringBuffer.toString();

    }

    public static BufferedReader getScheduleFromServer(String id, Date date, int type) throws Exception {
        URL urlLink = new URL("https://roosters.windesheim.nl/WebUntis/lessoninfodlg.do?date=" + new SimpleDateFormat("yyyyMMdd").format(date) + "&starttime=0800&endtime=2300&elemid=" + id + "&elemtype=" + type);
        HttpURLConnection connection = (HttpURLConnection) urlLink.openConnection();
        connection.setConnectTimeout(10000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Cookie", "schoolname=\"_V2luZGVzaGVpbQ==\"");
        connection.setDoInput(true);
        connection.connect();
        InputStream inputStream = connection.getInputStream();
        return new BufferedReader(new InputStreamReader(inputStream));
    }

    public static void saveSchedule(BufferedReader reader, Date date, String componentId, int type) throws Exception {

        DateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        int countTd = 0;
        String module = "";
        String subject = "";
        String id = "";
        String start = "";
        String end = "";
        String component = "";
        String room = "";
        String line;

        List<String> list = new ArrayList<>();
        Cursor cursor = ApplicationLoader.scheduleDatabase.getFilteredLessons();
        while (cursor.moveToNext()) {
            list.add(cursor.getString(0));
        }
        cursor.close();

        ApplicationLoader.scheduleDatabase.clearScheduleData(simpleDateFormat.format(date));

        while ((line = reader.readLine()) != null) {
            if (line.contains("<td>")) {
                countTd++;
            }
            String les = line.replace("<td>", "").replace("</td>", "");
            switch (countTd) {
                case 1:
                    if (line.contains("<tooltip>")) {
                        module = line.replaceAll("\\s+", "").replace("<tooltip>", "").replace("</tooltip>", "");
                    }
                    break;
                case 2:
                    if (line.contains("</span>")) {
                        if (les.split("</span>")[0].split("\">").length != 2 && type == 2) {
                            component = "";
                            break;
                        } else {
                            if (type == 2) {
                                component = les.split("</span>")[0].split("\">")[1];
                            }
                            break;
                        }
                    }
                    break;
                case 4:
                    if (line.contains("</span>")) {
                        if (les.split("</span>")[0].split("\">").length != 2 && type == 1) {
                            component = "";
                            break;
                        } else {
                            if (type == 1) {
                                component = les.split("</span>")[0].split("\">")[1];
                            }
                            break;
                        }
                    }
                    break;
                case 5:
                    if (!les.contains(".")) {
                        break;
                    }
                    room = les.replaceAll("\t", "");
                    break;
                case 6:
                    subject = les;
                    break;
                case 7:
                    String[] startSplit = les.split(":");
                    if (startSplit[0].length() == 1) {
                        les = 0 + les;
                    }
                    start = les;
                    break;
                case 8:
                    String[] endSplit = les.split(":");
                    if (endSplit[0].length() == 1) {
                        les = 0 + les;
                    }
                    end = les;
                    break;
                case 9:
                    if (!les.matches(".*\\d.*")) {
                        break;
                    }
                    id = les.replaceAll("\t", "");
                    break;
                case 11:
                    // end reached, let's reset fields to prevent duplicates
                    String lesson = "";
                    if (module.equals("")) {
                        lesson = subject;
                    } else {
                        if (subject.equals("")) {
                            lesson = module;
                        } else {
                            lesson = module + " - " + subject;
                        }
                    }
                    ApplicationLoader.scheduleDatabase.saveScheduleData(id, simpleDateFormat.format(date), start, end, lesson, room, component, componentId, list.contains(id) ? 0 : 1);
                    countTd = 0;
                    module = "";
                    subject = "";
                    id = "";
                    start = "";
                    end = "";
                    component = "";
                    room = "";
                    break;
                default:
                    break;
            }
        }
    }
}
