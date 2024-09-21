package com.example.roomrental.model;
import java.io.Serializable;
import java.util.Random;

public class Property implements Serializable {
    private static final long serialVersionUID = 1L;
    private String roomName;
    private int noOfPersons;
    private String area;
    private float stars;
    private int noOfReviews;
    private String roomImage;
    private int id;
    private int starsSum;
    private int price;
    private byte[] image;

    public Property(String roomName, int noOfPersons, String area, int stars, int noOfReviews, String roomImage, int id, byte[] image) {
        this.roomName = roomName;
        this.noOfPersons = noOfPersons;
        this.area = area;
        this.roomImage = roomImage;
        this.id = id;
        if (noOfReviews == 0) {
            this.starsSum = 0;
            this.noOfReviews = 0;
            this.stars = 0;
        } else {
            this.noOfReviews = noOfReviews;
            this.starsSum = stars;
            this.stars = formatStars((float) starsSum / noOfReviews);
        }
        Random random = new Random();
        this.price = (int) (random.nextDouble() * 100);
        this.image=image;
    }

    // Getters
    public String getRoomName() {
        return roomName;
    }

    public int getNoOfPersons() {
        return noOfPersons;
    }

    public String getArea() {
        return area;
    }

    public float getStars() {
        if (noOfReviews == 0) return 0;
        System.out.println("GET STARS");
        System.out.println(stars);
        return stars;
    }

    public int getNoOfReviews() {
        return noOfReviews;
    }

    public String getRoomImage() {
        return roomImage;
    }

    public int getId() {
        return id;
    }

    public int getPrice() {
        return price;
    }

    // Setters
    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public void setNoOfPersons(int noOfPersons) {
        this.noOfPersons = noOfPersons;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public void setStars(float stars) {
        System.out.println("SET STARS");
        this.stars = formatStars(stars);
        System.out.println("stars");
    }

    public void setNoOfReviews(int noOfReviews) {
        this.noOfReviews = noOfReviews;
    }

    public void setRoomImage(String roomImage) {
        this.roomImage = roomImage;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void rate(int stars) {
        System.out.println("rating");
        System.out.println(stars);
        System.out.println(starsSum);
        System.out.println(noOfReviews);
        System.out.println(formatStars((float) starsSum / noOfReviews));
        System.out.println("end rating");

        starsSum = starsSum + stars;
        noOfReviews = noOfReviews + 1;
        this.stars = formatStars((float) starsSum / noOfReviews);

        System.out.println("rating");
        System.out.println(starsSum);
        System.out.println(noOfReviews);
        System.out.println(formatStars((float) starsSum / noOfReviews));
        System.out.println(this.stars);
        System.out.println("end rating");
    }

    private float formatStars(float stars) {
        return Math.round(stars * 10) / 10.0f;
    }

    public String toString() {
        return "Property{" +
                "roomName='" + roomName + '\'' +
                ", noOfPersons=" + noOfPersons +
                ", area='" + area + '\'' +
                ", stars=" + stars +
                ", noOfReviews=" + noOfReviews +
                ", roomImage='" + roomImage + '\'' +
                ", id=" + id +
                ", numberOfRatings=" + noOfReviews +
                ", starsSum=" + starsSum +
                ", price=" + price +
                "}\n";
    }

    public byte[] getImage() {
        return image;
    }
}
