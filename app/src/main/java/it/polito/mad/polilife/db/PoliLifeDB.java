package it.polito.mad.polilife.db;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.parse.*;

import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import it.polito.mad.polilife.MainActivity;
import it.polito.mad.polilife.db.classes.*;
import it.polito.mad.polilife.db.DBCallbacks.*;
import it.polito.mad.polilife.db.parcel.PStudentData;
import it.polito.mad.polilife.db.parcel.PUserData;

/**
 * Created by Luigi onSelectAppliedJobs 27/10/2015.
 */
public class PoliLifeDB {

    private PoliLifeDB(){}

    private static final String TAG = "ParseDB";

    private static final String APPLICATION_ID = "UWm5ltXVvvea4XrTKFFYX63GbuOX90WS4ec8hZg5";
    private static final String CLIENT_KEY = "4HPCLcygPRGkVEImYdsDYUFi8A0zXeZF80pPEYtf";

    public static final String STUDENT_KEY = "student";

    public static final String NOTICES = "notices";

    public static void initialize(final Context context, Class<? extends ParseObject>... classesToRegister) {
        if (context == null) {
            throw new RuntimeException("context must be non null");
        }
        for (Class cl : classesToRegister) {
            ParseObject.registerSubclass(cl);
        }
        Parse.enableLocalDatastore(context);
        Parse.initialize(context, APPLICATION_ID, CLIENT_KEY);
        ParseInstallation.getCurrentInstallation().saveInBackground();
    }

    public static void signUpStudent(final PUserData userData,
                                     final PStudentData studentData,
                                     final StudentSignUpCallback listener) {
        final Student newUser = userData.build();
        newUser.signUpInBackground(new SignUpCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    if (listener != null) listener.onStudentSignUpException(e);
                    return;
                }
                if (studentData == null) {
                    if (listener != null) {
                        listener.onStudentSignUpSuccess(newUser);
                    }
                    return;
                }
                final StudentInfo info = studentData.build();
                info.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if (e != null) {
                            if (listener != null) {
                                listener.onStudentSignUpException(e);
                            }
                            return;
                        }
                        newUser.setStudentInfo(info);
                        newUser.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e != null) {
                                    if (listener != null) {
                                        listener.onStudentSignUpException(e);
                                    }
                                    return;
                                }
                                listener.onStudentSignUpSuccess(newUser);
                            }
                        });
                    }
                });
            }
        });
    }


    public static ParseUser tryLocalLogin() {
        ParseUser current = ParseUser.getCurrentUser();
        if (current == null || !(current instanceof Student)){
            return null;
        }
        Student student = (Student) current;
        if (student.getStudentInfo() == null){
            return null;
        }
        StudentInfo info = student.getStudentInfo();
        try {
            info.fetchFromLocalDatastore();
        } catch (ParseException e) {
            return null;
        }
        student.setLastLogin(new Date());
        student.saveEventually();
        return current;
    }

    public static void remoteLogIn(final String username, final String password,
                                   final UserLoginCallback listener) {
        ParseUser.logInInBackground(username, password, new LogInCallback() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                if (parseUser == null || !(parseUser instanceof Student) ||
                        ((Student) parseUser).getStudentInfo() == null) {
                    if (listener != null) {
                        listener.onLoginError((e != null) ? e : new ParseException(-1, "No login"));
                    }
                    return;
                }
                Student student = (Student) parseUser;
                student.setLastLogin(new Date());
                student.saveEventually();
                ParseObject studentInfo = student.getStudentInfo();
                //prova
                ParseInstallation.getCurrentInstallation().put("user", student);
                ParseInstallation.getCurrentInstallation().saveEventually();
                //fine prova
                try {
                    student.fetchIfNeeded();
                    studentInfo.fetchIfNeeded();
                    student.pin();
                } catch (ParseException ex) {
                    if (listener != null) {
                        listener.onLoginError(ex);
                    }
                    return;
                }
                if (listener != null) {
                    listener.onStudentLoginSuccess(parseUser);
                }
            }
        });
    }

    public static void logOut(final UserLogoutCallback listener) {
        ParseUser current = ParseUser.getCurrentUser();
        if (current == null){
            if (listener != null){
                listener.onLogoutError(new ParseException(-1, "No current parse user"));
            }
            return;
        }
        try {
            current.unpin();
        }
        catch (ParseException e) {
            if (listener != null) {
                listener.onLogoutError(e);
            }
        }
        ParseUser.logOutInBackground(new LogOutCallback() {
            @Override
            public void done(ParseException e) {
                if (listener != null) {
                    if (e != null) {
                        listener.onLogoutError(e);
                    } else {
                        ParseInstallation.getCurrentInstallation().put("user", JSONObject.NULL);
                        ParseInstallation.getCurrentInstallation().saveInBackground();
                        listener.onLogoutSuccess();
                    }
                }
            }
        });
    }


    /**

    public static void updateStudentData(PStudentData data, final UpdateCallback<StudentInfo> listener){
        ParseObject po = (ParseObject) ParseUser.getCurrentUser().get(STUDENT_KEY);
        if (po == null || !(po instanceof StudentInfo)) throw new AssertionError();
        final StudentInfo currentStudent = (StudentInfo) po;
        if (data.getAvailableFrom() != null) currentStudent.setAvailableStart(data.getAvailableFrom());
        if (data.getAvailableTo() != null) currentStudent.setAvailableEnd(data.getAvailableTo());
        currentStudent.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    if (listener != null) {
                        listener.onUpdateError(e);
                    }
                    return;
                }
                if (listener != null) {
                    listener.onUpdateSuccess(currentStudent);
                }
            }
        });
    }
    */

    public static void searchClassrooms(String param, boolean exactMatch,
                                        final GetListCallback<Classroom> listener){
        ParseQuery<Classroom> query = ParseQuery.getQuery(Classroom.class);
        if (exactMatch) query.whereEqualTo(Classroom.NAME, "Aula "+param);
        else query.whereContains(Classroom.NAME, param);
        query.findInBackground(new FindCallback<Classroom>() {
            @Override
            public void done(List<Classroom> list, ParseException e) {
                if (e != null) {
                    if (listener != null) {
                        listener.onFetchError(e);
                    }
                    return;
                }
                if (listener != null) {
                    listener.onFetchSuccess(list);
                }
            }
        });
    }

    public static <T extends ParseObject> void getAllObjects(
            Class<T> klass, final GetListCallback<T> listener){
        ParseQuery<T> query = ParseQuery.getQuery(klass);
        query.findInBackground(new FindCallback<T>() {
            @Override
            public void done(List<T> list, ParseException e) {
                if (e != null) {
                    if (listener != null) listener.onFetchError(e);
                    return;
                }
                if (listener != null) listener.onFetchSuccess(list);
            }
        });
    }
/*
    public static void publishNewNotice(final PNoticeData data,
                                        final UpdateCallback<Notice> listener){
        final ParseUser currentUser = ParseUser.getCurrentUser();
        ParseObject po = (ParseObject) currentUser.get(STUDENT_KEY);
        if (po == null || !(po instanceof StudentInfo)) throw new AssertionError();
        final Notice built = data.build();

        new AsyncTask<Void,Void,ParseException>() {
            @Override
            protected ParseException doInBackground(Void[] params) {
                List<ParseFile> photoFiles = new LinkedList<>();
                for (PFileData fileData : data.getPhotos()){
                    ParseFile file = fileData.build();
                    try{
                        file.save();
                    }catch(ParseException e){
                        Log.e("pe", e.getMessage());
                        return e;
                    }
                    photoFiles.add(file);
                }
                built.addPhotos(photoFiles);
                return null;
            }
            @Override
            protected void onPostExecute(ParseException e) {
                super.onPostExecute(e);
                if (e == null){
                    built.setOwner(currentUser);
                    built.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e != null){
                                if (listener != null) listener.onUpdateError(e);
                                return;
                            }
                            currentUser.getRelation(NOTICES).add(built);
                            currentUser.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e != null){
                                        if (listener != null) listener.onUpdateError(e);
                                        return;
                                    }
                                    if (listener != null) listener.onUpdateSuccess(built);
                                }
                            });
                        }
                    });
                }
            }
        }.execute();
    }
    */

    public static void advancedNoticeFilter(final Notice.Filter filter,
                                            final boolean fromLocalDataStore,
                                            final GetListCallback<Notice> listener){
        ParseQuery<Notice> query = ParseQuery.getQuery(Notice.class);
        ParseQuery<Notice> finalQuery = null;
        if (filter != null) {
            query.whereLessThanOrEqualTo(Notice.PRICE, filter.maxPrice)
                    .whereGreaterThanOrEqualTo(Notice.PRICE, filter.minPrice)
                    .whereLessThanOrEqualTo(Notice.SIZE, filter.maxSize)
                    .whereGreaterThanOrEqualTo(Notice.SIZE, filter.minSize);
            if (filter.daysAgo > 0) {
                long DAY_IN_MS = 1000 * 60 * 60 * 24;
                Date limit = new Date(System.currentTimeMillis() - (filter.daysAgo * DAY_IN_MS));
                query.whereGreaterThanOrEqualTo(Notice.PUBLICATED_AT, limit);
            }
            if (filter.type != null){
                query.whereEqualTo(Notice.TYPE, filter.type);
            }
            if (filter.title != null){
                query.whereContains(Notice.TITLE, filter.title);
            }
            if (filter.location != null) {
                query.whereContains(Notice.LOCATION_STRING, filter.location);
            }
            if (filter.latitude != null && filter.longitude != null){
                ParseGeoPoint point = new ParseGeoPoint(filter.latitude, filter.longitude);
                query.whereWithinKilometers(Notice.LOCATION_POINT, point, filter.within);
            }
            if (filter.contractType != null) {
                query.whereEqualTo(Notice.CONTRACT_TYPE, filter.contractType);
            }
            if (filter.propertyType != null) {
                query.whereEqualTo(Notice.PROPERTY_TYPE, filter.propertyType);
            }
            if (filter.tags != null && !filter.tags.isEmpty()) {
                query.whereContainsAll(Notice.TAGS, filter.tags);
            }
            List<ParseQuery<Notice>> res = new LinkedList<>();
            res.add(query);
            for (String tag : filter.tags) {
                List<ParseQuery<Notice>> queries = new LinkedList<>();
                queries.add(ParseQuery.getQuery(Notice.class).whereContains(Notice.TITLE, tag));
                queries.add(ParseQuery.getQuery(Notice.class).whereContains(Notice.DESCRIPTION, tag));
                ParseQuery<Notice> union = ParseQuery.or(queries);
                res.add(union);
            }
            finalQuery = ParseQuery.or(res);
        }
        else{
            finalQuery = query;
        }
        if (fromLocalDataStore){
            finalQuery.fromLocalDatastore();
        }
        finalQuery.addDescendingOrder(Notice.PUBLICATED_AT);
        finalQuery.findInBackground(new FindCallback<Notice>() {
            @Override
            public void done(List<Notice> list, ParseException e) {
                if (e != null) {
                    if (listener != null) listener.onFetchError(e);
                    return;
                }
                if (!fromLocalDataStore) {
                    ParseObject.unpinAllInBackground("cachedNotices");
                    ParseObject.pinAllInBackground("cachedNotices", list);
                }
                if (listener != null) listener.onFetchSuccess(list);
            }
        });
    }

    public static void getNewestDidacticalNotices(final GetListCallback<Notice> listener){
        ParseQuery<Notice> query = ParseQuery.getQuery(Notice.class);
        query.whereEqualTo(Notice.TYPE, Notice.DIDACTICAL_TYPE);
        Date lastLogin = ((Student) ParseUser.getCurrentUser()).getLastLogin();
        query.whereGreaterThanOrEqualTo("createdAt", lastLogin);
        query.findInBackground(new FindCallback<Notice>() {
            @Override
            public void done(List<Notice> list, ParseException e) {
                if (e != null) {
                    if (listener != null) listener.onFetchError(e);
                    return;
                }
                if (listener != null) listener.onFetchSuccess(list);
            }
        });
    }

    public static void advancedJobsFilter(final Job.Filter filter,
                                          final boolean fromLocalDataStore,
                                          final GetListCallback<Job> listener) {
        ParseQuery<Job> query = ParseQuery.getQuery(Job.class);
        if (filter != null){
            if (filter.name != null){
                query.whereContains(Job.NAME, filter.name);
            }
            if (filter.typeOfContract != null){
                query.whereEqualTo(Job.TYPE_OF_CONTRACT, filter.typeOfContract);
            }
            if (filter.typeOfDegree != null){
                query.whereEqualTo(Job.TYPE_OF_DEGREE, filter.typeOfDegree);
            }
            if (filter.city != null){
                query.whereEqualTo(Job.CITY, filter.city);
            }
            if (filter.startDate != null){
                query.whereGreaterThanOrEqualTo(Job.START_DATE, filter.startDate);
            }
        }
        query.addDescendingOrder(Job.START_DATE);
        if (fromLocalDataStore){
            query.fromLocalDatastore();
        }
        query.findInBackground(new FindCallback<Job>() {
            @Override
            public void done(List<Job> list, ParseException e) {
                if (e != null) {
                    if (listener != null) listener.onFetchError(e);
                    return;
                }
                if (!fromLocalDataStore) {
                    ParseObject.unpinAllInBackground(getPinName(Job.class));
                    ParseObject.pinAllInBackground(getPinName(Job.class), list);
                }
                if (listener != null) listener.onFetchSuccess(list);
            }
        });
    }

    public static void getRecentNoticesAndPositions(final int daysAgo,
            final boolean fromLocalDataStore, final GetListCallback<ParseObject> listener){
        final List<ParseObject> objs = new LinkedList<>();
        Notice.Filter filter = new Notice.Filter();
        filter.daysAgo = daysAgo;
        advancedNoticeFilter(filter, fromLocalDataStore, new GetListCallback<Notice>() {
            @Override
            public void onFetchSuccess(List<Notice> result) {
                objs.addAll(result);
                ParseQuery<Job> query = ParseQuery.getQuery(Job.class);
                if (daysAgo > 0) {
                    long DAY_IN_MS = 1000 * 60 * 60 * 24;
                    Date limit = new Date(System.currentTimeMillis() - (daysAgo * DAY_IN_MS));
                    query.whereGreaterThanOrEqualTo(Job.START_DATE, limit);
                }
                if (fromLocalDataStore) {
                    query.fromLocalDatastore();
                }
                query.findInBackground(new FindCallback<Job>() {
                    @Override
                    public void done(List<Job> list, ParseException e) {
                        if (e != null) {
                            if (listener != null) listener.onFetchError(e);
                            return;
                        }
                        objs.addAll(list);
                        if (listener != null) listener.onFetchSuccess(objs);
                    }
                });
            }

            @Override
            public void onFetchError(Exception exception) {
                if (listener != null) listener.onFetchError(exception);
            }
        });
    }

    public static <T extends ParseObject> void retrieveObject(final String id, final Class<T> klass,
            final GetOneCallback<T> listener){
        final T obj = ParseObject.createWithoutData(klass, id);
        obj.fetchFromLocalDatastoreInBackground(new GetCallback<T>() {
            @Override
            public void done(T parseObject, ParseException e) {
                if (e != null) {
                    obj.fetchInBackground(new GetCallback<T>() {
                        @Override
                        public void done(T parseObject, ParseException e) {
                            if (e != null) {
                                if (listener != null) listener.onFetchError(e);
                                return;
                            }
                            Log.d(TAG, "Found " + klass.getSimpleName() + "[" + id + "] online");
                            parseObject.pinInBackground(getPinName(klass));
                            if (listener != null) listener.onFetchSuccess(parseObject);
                        }
                    });
                    return;
                }
                Log.d(TAG, "Found " + klass.getSimpleName() + "[" + id + "] onSelectAppliedJobs local data store");
                if (listener != null) listener.onFetchSuccess(parseObject);
            }
        });
    }

    public static void getUsersByName(String name, final GetListCallback<ParseUser> listener){
        ParseQuery<ParseUser> query = ParseQuery.getQuery(ParseUser.class);
        query.whereContains("username", name);
        query.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> list, ParseException e) {
                if (e != null) {
                    if (listener != null) listener.onFetchError(e);
                    return;
                }
                if (listener != null) listener.onFetchSuccess(list);
            }
        });
    }

    public static void apply(Job job, final UpdateCallback<StudentInfo> listener) {
        final StudentInfo info = ((Student) ParseUser.getCurrentUser()).getStudentInfo();
        info.addAppliedJob(job);
        info.saveEventually(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e != null) {
                    if (listener != null) listener.onUpdateError(e);
                    return;
                }
                if (listener != null) listener.onUpdateSuccess(info);
            }
        });
    }

    public static void getAppliedJobs(final GetListCallback<Job> listener){
        StudentInfo info = ((Student) ParseUser.getCurrentUser()).getStudentInfo();
        info.getAppliedJobs().getQuery().findInBackground(new FindCallback<Job>() {
            @Override
            public void done(List<Job> list, ParseException e) {
                if (e != null) {
                    if (listener != null) listener.onFetchError(e);
                    return;
                }
                if (listener != null) listener.onFetchSuccess(list);
            }
        });
    }

    public static void getCachedJobs(final GetListCallback<Job> listener){
        ParseQuery.getQuery(Job.class).fromPin(getPinName(Job.class))
                .findInBackground(new FindCallback<Job>() {
            @Override
            public void done(List<Job> list, ParseException e) {
                if (e != null) {
                    if (listener != null) listener.onFetchError(e);
                    return;
                }
                if (listener != null) listener.onFetchSuccess(list);
            }
        });
    }

    private static <T extends ParseObject> String getPinName(Class<T> klass){
        return "cache" + klass.getSimpleName(); 
    }

}
