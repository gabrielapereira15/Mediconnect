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
    /** The provincial health card, and which province issued it. */
    String healthCardNumber;
    String healthCardProvince;

    public String getid() {
        return id;
    }

    public void setid(String id) {
        this.id = id;
    }

    public String getHealthCardNumber() {
        return healthCardNumber;
    }

    public void setHealthCardNumber(String healthCardNumber) {
        this.healthCardNumber = healthCardNumber;
    }

    public String getHealthCardProvince() {
        return healthCardProvince;
    }

    public void setHealthCardProvince(String healthCardProvince) {
        this.healthCardProvince = healthCardProvince;
    }

    public String getfirstName() {
        return firstName;
    }

    public void setfirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getlastName() {
        return lastName;
    }

    public void setlastName(String lastName) {
        this.lastName = lastName;
    }

    public String getemail() {
        return email;
    }

    public void setemail(String email) {
        this.email = email;
    }

    public String getphoneNumber() {
        return phoneNumber;
    }

    public void setphoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getclinicCode() {
        return clinicCode;
    }

    public void setclinicCode(String clinicCode) {
        this.clinicCode = clinicCode;
    }

    public String getaddress() {
        return address;
    }

    public void setaddress(String address) {
        this.address = address;
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
        this.gender = gender;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }
}
