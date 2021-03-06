package it.polito.mad.polilife.db.classes;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseRelation;
import com.parse.ParseUser;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created by luigi onSelectAppliedJobs 29/05/15.
 */
@ParseClassName("_User")
public class Student extends ParseUser {

    public static final String STUDENT_KEY = "studentInfo";
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String DOB = "birth";
    public static final String ADDRESS= "address";
    public static final String COUNTRY = "country";
    public static final String CITY= "city";
    public static final String CONTACT_PHONE = "contactPhone";
    public static final String ABOUT = "aboutMe";
    public static final String PHOTO = "photo";
    public static final String CHANNELS = "channels";
    public static final String LAST_LOGIN = "lastLogin";

    public static final String PUBLIC_CHANNEL = "public";

    public Date getLastLogin(){ return (Date) get(LAST_LOGIN); }
    public void setLastLogin(Date value){ put(LAST_LOGIN, value); }

    public StudentInfo getStudentInfo(){ return (StudentInfo) get(STUDENT_KEY); }
    public void setStudentInfo(StudentInfo value){ put(STUDENT_KEY, value); }

    public String getFirstName(){
        return (String) get(FIRST_NAME);
    }
    public void setFirstName(String value){
        put(FIRST_NAME, value);
    }

    public String getLastName(){
        return (String) get(LAST_NAME);
    }
    public void setLastName(String value){
        put(LAST_NAME, value);
    }

    public String getContactPhone(){
        return (String) get(CONTACT_PHONE);
    }
    public void setContactPhone(String contactName){
        put(CONTACT_PHONE, contactName);
    }

    public String getCity(){ return (String) get(CITY);  }
    public void setCity(String value) {   put(CITY, value);  }

    public String getAddress(){  return (String) get(ADDRESS);  }
    public void setAddress(String value) {   put(ADDRESS, value);  }

    public String getCountry(){
        return (String) get(COUNTRY);
    }
    public void setCountry(String value){
        put(COUNTRY, value);
    }

    public Date getBirthDate(){ return (Date) get(DOB); }
    public void setBirthDate(Date value){ put(DOB, value); }

    public ParseFile getPhoto(){ return (ParseFile) get(PHOTO); }
    public void setPhoto(ParseFile value){ put(PHOTO, value); }

    public String getAbout(){ return (String) get(ABOUT); }
    public void setAbout(String value){ put(ABOUT, value); }

    public List<String> getChannels(){
        List<String> chs = (List<String>) get(CHANNELS);
        chs.remove(PUBLIC_CHANNEL);
        return chs;
    }
    public void setChannels(List<String> values) {
        addUnique(CHANNELS, PUBLIC_CHANNEL);
        for (String ch : values)
            addUnique(CHANNELS, ch);
    }
    public void addChannel(String value){
        addUnique(CHANNELS, PUBLIC_CHANNEL);
        addUnique(CHANNELS, value);
    }
    public void removeChannels(Collection<String> values){
        if (values.contains(PUBLIC_CHANNEL))
            values.remove(PUBLIC_CHANNEL);
        removeAll(CHANNELS, values);
    }

    @Override
    public String toString() {
        return getUsername();
    }
}