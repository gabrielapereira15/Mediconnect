package com.example.mediconnect_android.model;

public class Patient {
    String id;
    String firstName;
    String lastName;
    String email;
    String phoneNumber;
    String clinicCode;
    String address;
    String birthdate;
    String gender;
    String document;

    public String getid() {
        return id;
    }

    public void setid(String id) {
        this.id = id;
    }

    public String getfirstName() {
        return firstName;
    }

    public void setfirstName(String firstName) {
        firstName = firstName;
    }

    public String getlastName() {
        return lastName;
    }

    public void setlastName(String lastName) {
        lastName = lastName;
    }

    public String getemail() {
        return email;
    }

    public void setemail(String email) {
        email = email;
    }

    public String getphoneNumber() {
        return phoneNumber;
    }

    public void setphoneNumber(String phoneNumber) {
        phoneNumber = phoneNumber;
    }

    public String getclinicCode() {
        return clinicCode;
    }

    public void setclinicCode(String clinicCode) {
        clinicCode = clinicCode;
    }

    public String getaddress() {
        return address;
    }

    public void setaddress(String address) {
        address = address;
    }

    public String getbirthdate() {
        return birthdate;
    }

    public void setbirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getgender() {
        return gender;
    }

    public void setgender(String gender) {
        gender = gender;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }
}
