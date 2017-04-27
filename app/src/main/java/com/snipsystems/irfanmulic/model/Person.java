package com.snipsystems.irfanmulic.model;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by irfan1 on 2/19/17.
 */


public class Person {
    String name;
    Double lon;
    Double lat;
    String place;

    public Person(String name, Double lat, Double lon, String place) {
        this.name = name;
        this.lon = lon;
        this.lat = lat;
        this.place = place;
    }

    public String getName() {
        return name;
    }

    public Double getLon() {
        return lon;
    }

    public Double getLat() {
        return lat;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLon(Double lon) {
        this.lon = lon;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", lon='" + lon + '\'' +
                ", lat='" + lat + '\'' +
                ", place='" + place + '\'' +
                '}';
    }

    private static int lastUserId = 0;
    
    public static ArrayList<Person> createUserList(int numUsers){
        ArrayList<Person> contacts = new ArrayList<Person>();

        for (int i = 1; i <= numUsers; i++) {
            Person p = new Person("Person " + ++lastUserId, 40*Math.random(), 90*Math.random(), "Place" );
            contacts.add(p);
        }

        return contacts;
    }
}
