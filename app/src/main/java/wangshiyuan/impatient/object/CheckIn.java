package wangshiyuan.impatient.object;

import java.io.Serializable;

/**
 * Created by wangshiyuan on 11/15/15.
 */



public class CheckIn implements Serializable, Comparable<CheckIn>{

    private static final long serialVersionUID = 1L;
    String objectId;
    String firstName;
    String lastName;
    String sex;
    String userID;
    String appointmentID;
    int checkInTime;


    public String getObjectId() {
        return objectId;
    }
    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public String getFirstName() {
        return firstName;
    }
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }
    public String getLastName() {
        return lastName;
    }
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
    public String getSex() {
        return sex;
    }
    public void setSex(String sex) {
        this.sex = sex;
    }
    public String getUserID() {
        return userID;
    }
    public void setUserID(String userID) {
        this.userID = userID;
    }
    public String getAppointmentID() {
        return appointmentID;
    }
    public void setAppointmentID(String appointmentID) {
        this.appointmentID = appointmentID;
    }
    public int getCheckInTime() {
        return checkInTime;
    }
    public void setCheckInTime(int checkInTime) {
        this.checkInTime = checkInTime;
    }

    @Override
    public boolean equals(Object other){
        return this.getUserID().equals(((CheckIn) other).getUserID());
    }

    @Override
    public int compareTo(CheckIn other) {
        return this.getCheckInTime()-other.getCheckInTime();
    }

    public String toUserString(){
        return "Name: "+this.firstName+" "+this.lastName+", ID:"+this.userID;

    }


}
